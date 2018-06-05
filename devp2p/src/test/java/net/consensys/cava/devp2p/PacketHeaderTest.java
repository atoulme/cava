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
package net.consensys.cava.devp2p;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.MutableBytes;
import net.consensys.cava.crypto.SECP256K1.KeyPair;
import net.consensys.cava.crypto.SECP256K1.Signature;
import net.consensys.cava.junit.BouncyCastleExtension;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class PacketHeaderTest {

  private KeyPair keyPair = KeyPair.random();
  private Signature sig = Signature.create((byte) 16, BigInteger.valueOf(34), BigInteger.valueOf(62));

  @Test
  void noKeyPair() {
    Exception exception =
        assertThrows(IllegalArgumentException.class, () -> new PacketHeader(null, (byte) 0x01, Bytes.EMPTY));
    assertEquals("keyPair cannot be null", exception.getMessage());
  }

  @Test
  void noPayload() {
    Exception exception =
        assertThrows(IllegalArgumentException.class, () -> new PacketHeader(keyPair, (byte) 0x02, null));
    assertEquals("payload cannot be null", exception.getMessage());
  }

  @Test
  void noHash() {
    Exception exception = assertThrows(
        IllegalArgumentException.class,
        () -> new PacketHeader(null, keyPair.getPublicKey(), sig, (byte) 0x02));
    assertEquals("hash cannot be null", exception.getMessage());
  }

  @Test
  void hashNot32Bytes() {
    Exception exception = assertThrows(
        IllegalArgumentException.class,
        () -> new PacketHeader(Bytes.EMPTY, keyPair.getPublicKey(), sig, (byte) 0x02));
    assertEquals("hash should be 32 bytes long, got 0 instead", exception.getMessage());
  }

  @Test
  void noPublicKey() {
    Exception exception = assertThrows(
        IllegalArgumentException.class,
        () -> new PacketHeader(Bytes.wrap(new byte[32]), null, sig, (byte) 0x02));
    assertEquals("publicKey cannot be null", exception.getMessage());
  }

  @Test
  void noSignature() {
    Exception exception = assertThrows(
        IllegalArgumentException.class,
        () -> new PacketHeader(Bytes.wrap(new byte[32]), keyPair.getPublicKey(), null, (byte) 0x02));
    assertEquals("signature cannot be null", exception.getMessage());
  }

  @Test
  void hashMismatch() {
    Exception e = assertThrows(PeerDiscoveryPacketDecodingException.class, () -> {
      FindNeighborsPacket packet = new FindNeighborsPacket(20, keyPair, Bytes.of(3));
      MutableBytes encoded = packet.toBytes().mutableCopy();
      encoded.set(3, (byte) 0xFF);
      PacketHeader.decode(encoded);
    });
    assertEquals("Hash does not match content", e.getMessage());
  }

  @Test
  void invalidSignature() {
    Exception e = assertThrows(PeerDiscoveryPacketDecodingException.class, () -> {
      FindNeighborsPacket packet = new FindNeighborsPacket(20, keyPair, Bytes.of(3));
      MutableBytes encoded = packet.toBytes().mutableCopy();
      encoded.mutableSlice(32, 65).fill((byte) 0x03);
      PacketHeader.decode(encoded);
    });
    assertEquals("Could not retrieve the public key from the signature and signed data", e.getMessage());
  }

  @Test
  void testEqualsForCreatedPacketHeaders() {
    FindNeighborsPacket packet = new FindNeighborsPacket(20, keyPair, Bytes.of(3));
    FindNeighborsPacket packet2 = new FindNeighborsPacket(20, keyPair, Bytes.of(3));
    assertEquals(packet.header(), packet2.header());
  }

  @Test
  void testEqualsForReceivedPacketHeaders() {
    FindNeighborsPacket packet = new FindNeighborsPacket(20, keyPair, Bytes.of(3));
    PacketHeader receivedHeader = PacketHeader.decode(packet.toBytes());
    PacketHeader receivedHeader2 = PacketHeader.decode(packet.toBytes());
    assertEquals(receivedHeader, receivedHeader2);
  }
}
