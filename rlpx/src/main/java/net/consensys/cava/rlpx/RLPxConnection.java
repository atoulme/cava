/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.rlpx;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.bytes.MutableBytes;
import net.consensys.cava.rlp.RLP;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import org.bouncycastle.crypto.digests.KeccakDigest;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.SICBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.xerial.snappy.Snappy;

public final class RLPxConnection {

  private static Bytes32 snapshot(KeccakDigest digest) {
    byte[] out = new byte[32];
    new KeccakDigest(digest).doFinal(out, 0);
    return Bytes32.wrap(out);
  }

  private final Bytes32 aesSecret;
  private final Bytes32 macSecret;
  private final Bytes32 token;
  private final KeccakDigest egressMac = new KeccakDigest(Bytes32.SIZE * 8);
  private final KeccakDigest ingressMac = new KeccakDigest(Bytes32.SIZE * 8);
  private final SICBlockCipher encryptionCipher;
  private final SICBlockCipher decryptionCipher;
  private final AESEngine macEncryptionEngine;

  private boolean applySnappyCompression = false;

  RLPxConnection(Bytes32 aesSecret, Bytes32 macSecret, Bytes32 token, Bytes egressMac, Bytes ingressMac) {
    this.aesSecret = aesSecret;
    this.macSecret = macSecret;
    this.token = token;

    KeyParameter aesKey = new KeyParameter(aesSecret.toArrayUnsafe());
    KeyParameter macKey = new KeyParameter(macSecret.toArrayUnsafe());

    byte[] IV = new byte[16];
    Arrays.fill(IV, (byte) 0);

    encryptionCipher = new SICBlockCipher(new AESEngine());
    encryptionCipher.init(true, new ParametersWithIV(aesKey, IV));

    decryptionCipher = new SICBlockCipher(new AESEngine());
    decryptionCipher.init(false, new ParametersWithIV(aesKey, IV));

    macEncryptionEngine = new AESEngine();
    macEncryptionEngine.init(true, macKey);

    updateEgress(egressMac);
    updateIngress(ingressMac);
  }

  Bytes32 updateEgress(Bytes bytes) {
    egressMac.update(bytes.toArrayUnsafe(), 0, bytes.size());
    return snapshot(egressMac);
  }

  Bytes32 updateIngress(Bytes bytes) {
    ingressMac.update(bytes.toArrayUnsafe(), 0, bytes.size());
    return snapshot(ingressMac);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    RLPxConnection that = (RLPxConnection) obj;
    return Objects.equals(aesSecret, that.aesSecret)
        && Objects.equals(macSecret, that.macSecret)
        && Objects.equals(token, that.token)
        && Objects.equals(snapshot(egressMac), snapshot(that.egressMac))
        && Objects.equals(snapshot(ingressMac), snapshot(that.ingressMac));
  }

  boolean isComplementedBy(RLPxConnection conn) {
    return Objects.equals(aesSecret, conn.aesSecret)
        && Objects.equals(macSecret, conn.macSecret)
        && Objects.equals(token, conn.token)
        && Objects.equals(snapshot(egressMac), snapshot(conn.ingressMac))
        && Objects.equals(snapshot(ingressMac), snapshot(conn.egressMac));
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(Objects.hashCode(aesSecret), Objects.hashCode(macSecret), Objects.hashCode(token), egressMac, ingressMac);
  }

  public WireProtocolMessage read(Bytes messageFrame) {
    Bytes macBytes = messageFrame.slice(16, 16);
    Bytes headerBytes = messageFrame.slice(0, 16);

    Bytes expectedMac = Bytes.wrap(new byte[16]);

    macEncryptionEngine
        .processBlock(snapshot(ingressMac).slice(0, 16).toArrayUnsafe(), 0, expectedMac.toArrayUnsafe(), 0);
    expectedMac = expectedMac.xor(headerBytes);
    expectedMac = updateIngress(expectedMac).slice(0, 16);

    if (!macBytes.equals(expectedMac)) {
      throw new IllegalArgumentException(
          String.format(
              "Header MAC did not match expected MAC; expected: %s, received: %s",
              expectedMac.toHexString(),
              macBytes.toHexString()));
    }

    Bytes decryptedHeader = Bytes.wrap(new byte[16]);
    decryptionCipher.processBytes(headerBytes.toArrayUnsafe(), 0, 16, decryptedHeader.toArrayUnsafe(), 0);

    int frameSize = decryptedHeader.get(0) & 0xff;
    frameSize = (frameSize << 8) + (decryptedHeader.get(1) & 0xff);
    frameSize = (frameSize << 8) + (decryptedHeader.get(2) & 0xff);
    int pad = frameSize % 16 == 0 ? 0 : 16 - frameSize % 16;

    Bytes frameData = messageFrame.slice(32, frameSize);
    Bytes frameMac = messageFrame.slice(32 + frameSize + pad, 16);

    Bytes newFrameMac = Bytes.wrap(new byte[16]);
    Bytes frameMacSeed = updateIngress(messageFrame.slice(32, frameSize + pad));
    macEncryptionEngine.processBlock(frameMacSeed.toArrayUnsafe(), 0, newFrameMac.toArrayUnsafe(), 0);
    Bytes expectedFrameMac = updateIngress(newFrameMac.xor(frameMacSeed.slice(0, 16))).slice(0, 16);
    if (!expectedFrameMac.equals(frameMac)) {
      throw new IllegalArgumentException(
          String.format(
              "Frame MAC did not match expected MAC; expected: %s, received: %s",
              expectedFrameMac.toHexString(),
              frameMac.toHexString()));
    }

    Bytes decryptedFrameData = Bytes.wrap(new byte[frameData.size()]);
    decryptionCipher
        .processBytes(frameData.toArrayUnsafe(), 0, frameData.size(), decryptedFrameData.toArrayUnsafe(), 0);

    int messageType = RLP.decodeInt(decryptedFrameData.slice(0, 1));

    Bytes messageData = decryptedFrameData.slice(1);
    if (applySnappyCompression) {
      try {
        messageData = Bytes.wrap(Snappy.uncompress(messageData.toArrayUnsafe()));
      } catch (IOException e) {
        throw new IllegalArgumentException(e);
      }
    }

    switch (messageType) {
      case 0:
        return HelloMessage.read(messageData);
      case 1:
        return DisconnectMessage.read(messageData);
      case 2:
        return PingMessage.read(messageData);
      case 3:
        return PongMessage.read(messageData);
      default:
        throw new UnsupportedOperationException("Unsupported message type");
    }
  }

  /**
   * Frames a message for sending to an RLPx peer, encrypting it and calculating the appropriate MACs.
   *
   * @param message The message to frame.
   * @return The framed message, as byte buffer.
   */
  public Bytes write(WireProtocolMessage message) {
    // Compress message
    Bytes messageData = message.toBytes();
    if (applySnappyCompression) {
      try {
        messageData = Bytes.wrap(Snappy.compress(messageData.toArrayUnsafe()));
      } catch (IOException e) {
        throw new IllegalArgumentException(e);
      }
    }

    int frameSize = messageData.size() + 1;
    int pad = frameSize % 16 == 0 ? 0 : 16 - frameSize % 16;

    // Generate the header data.
    MutableBytes frameSizeBytes = MutableBytes.create(3);
    frameSizeBytes.set(0, (byte) ((frameSize >> 16) & 0xff));
    frameSizeBytes.set(1, (byte) ((frameSize >> 8) & 0xff));
    frameSizeBytes.set(2, (byte) (frameSize & 0xff));
    Bytes protocolHeader = RLP.encodeList(writer -> {
      writer.writeValue(Bytes.EMPTY);
      writer.writeValue(Bytes.EMPTY);
    });
    byte[] zeros = new byte[16 - frameSizeBytes.size() - protocolHeader.size()];
    Arrays.fill(zeros, (byte) 0x00);
    Bytes headerBytes = Bytes.concatenate(frameSizeBytes, protocolHeader, Bytes.wrap(zeros));
    encryptionCipher.processBytes(headerBytes.toArrayUnsafe(), 0, 16, headerBytes.toArrayUnsafe(), 0);
    // Generate the header MAC.
    Bytes headerMac = Bytes.wrap(new byte[16]);
    macEncryptionEngine.processBlock(snapshot(egressMac).toArrayUnsafe(), 0, headerMac.toArrayUnsafe(), 0);
    headerMac = updateEgress(headerBytes.xor(headerMac)).slice(0, 16);

    Bytes idBytes = RLP.encodeInt(message.type());
    assert idBytes.size() == 1;

    Bytes encryptedPayload = Bytes.wrap(new byte[idBytes.size() + messageData.size() + pad]);
    encryptionCipher.processBytes(
        Bytes.concatenate(idBytes, messageData, Bytes.wrap(new byte[pad])).toArrayUnsafe(),
        0,
        encryptedPayload.size(),
        encryptedPayload.toArrayUnsafe(),
        0);

    // Calculate the frame MAC.
    Bytes payloadMacSeed = updateEgress(encryptedPayload).slice(0, 16);
    Bytes payloadMac = Bytes.wrap(new byte[16]);
    macEncryptionEngine.processBlock(payloadMacSeed.toArrayUnsafe(), 0, payloadMac.toArrayUnsafe(), 0);
    payloadMac = updateEgress(payloadMacSeed.xor(payloadMac)).slice(0, 16);

    return Bytes.concatenate(headerBytes, headerMac, encryptedPayload, payloadMac);
  }
}
