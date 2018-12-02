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

import static net.consensys.cava.bytes.Bytes.concatenate;
import static net.consensys.cava.crypto.Hash.keccak256;
import static net.consensys.cava.crypto.SECP256K1.Parameters.CURVE;
import static net.consensys.cava.crypto.SECP256K1.calculateKeyAgreement;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.concurrent.AsyncResult;
import net.consensys.cava.crypto.SECP256K1;
import net.consensys.cava.crypto.SECP256K1.KeyPair;
import net.consensys.cava.crypto.SECP256K1.PublicKey;
import net.consensys.cava.crypto.SECP256K1.SecretKey;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.annotations.VisibleForTesting;
import org.bouncycastle.crypto.BasicAgreement;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.modes.SICBlockCipher;
import org.bouncycastle.crypto.params.*;
import org.bouncycastle.util.BigIntegers;

/**
 * Factory creating RLPxConnection, either from initiating a handshake or responding to a handshake request.
 */
public final class RLPxConnectionFactory {

  private static final SecureRandom random = new SecureRandom();

  public static Bytes32 createRandomHash() {
    Bytes32 nonce = Bytes32.wrap(new byte[32]);
    random.nextBytes(nonce.toArrayUnsafe());
    return nonce;
  }

  public static AsyncResult<RLPxConnection> createHandshake(
      KeyPair keyPair,
      PublicKey remotePublicKey,
      Function<Bytes, AsyncResult<Bytes>> initAndResponse) {

    Bytes32 nonce = createRandomHash();
    KeyPair ephemeralKeyPair = KeyPair.random();
    Bytes initHandshakeMessage = init(keyPair, remotePublicKey, ephemeralKeyPair, nonce);
    AsyncResult<Bytes> response = initAndResponse.apply(initHandshakeMessage);

    return response.thenApply(responseBytes -> {
      ResponderHandshakeMessage responseMessage = readResponse(responseBytes, keyPair.secretKey());
      return createConnection(
          true,
          initHandshakeMessage,
          responseBytes,
          ephemeralKeyPair.secretKey(),
          responseMessage.ephemeralPublicKey(),
          nonce,
          responseMessage.nonce());
    });
  }

  public static RLPxConnection respondToHandshake(
      Bytes initiatorMessageBytes,
      SecretKey privateKey,
      Consumer<Bytes> responseHandler) {
    InitiatorHandshakeMessage initiatorHandshakeMessage = read(initiatorMessageBytes, privateKey);
    Bytes32 nonce = Bytes32.wrap(new byte[32]);
    random.nextBytes(nonce.toArrayUnsafe());
    KeyPair ephemeralKeyPair = KeyPair.random();

    PublicKey initiatorPublicKey = initiatorHandshakeMessage.publicKey();

    ResponderHandshakeMessage responderMessage = ResponderHandshakeMessage.create(ephemeralKeyPair.publicKey(), nonce);
    Bytes responseBytes = encryptMessage(responderMessage.encode(), initiatorPublicKey);
    responseHandler.accept(responseBytes);

    return createConnection(
        false,
        initiatorMessageBytes,
        responseBytes,
        ephemeralKeyPair.secretKey(),
        initiatorHandshakeMessage.ephemeralPublicKey(),
        initiatorHandshakeMessage.nonce(),
        nonce);
  }

  @VisibleForTesting
  public static Bytes init(
      KeyPair keyPair,
      PublicKey remotePublicKey,
      KeyPair ephemeralKeyPair,
      Bytes32 initiatorNonce) {
    Bytes32 sharedSecret = calculateKeyAgreement(keyPair.secretKey(), remotePublicKey);
    InitiatorHandshakeMessage message =
        InitiatorHandshakeMessage.create(keyPair.publicKey(), ephemeralKeyPair, sharedSecret, initiatorNonce);
    return encryptMessage(message.encode(), remotePublicKey);
  }

  @VisibleForTesting
  static InitiatorHandshakeMessage read(Bytes payload, SecretKey privateKey) {
    return InitiatorHandshakeMessage.decode(decryptMessage(payload, privateKey), privateKey);
  }

  @VisibleForTesting
  public static ResponderHandshakeMessage readResponse(Bytes response, SecretKey privateKey) {
    return ResponderHandshakeMessage.decode(decryptMessage(response, privateKey));
  }

  public static RLPxConnection createConnection(
      boolean initiator,
      Bytes initiatorMessage,
      Bytes responderMessage,
      SecretKey ourEphemeralPrivateKey,
      PublicKey peerEphemeralPublicKey,
      Bytes32 initiatorNonce,
      Bytes32 responderNonce) {

    Bytes agreedSecret = SECP256K1.calculateKeyAgreement(ourEphemeralPrivateKey, peerEphemeralPublicKey);
    Bytes sharedSecret = keccak256(concatenate(agreedSecret, keccak256(concatenate(responderNonce, initiatorNonce))));

    Bytes32 aesSecret = keccak256(concatenate(agreedSecret, sharedSecret));
    Bytes32 macSecret = keccak256(concatenate(agreedSecret, aesSecret));
    Bytes32 token = keccak256(sharedSecret);

    Bytes initiatorMac = concatenate(macSecret.xor(responderNonce), initiatorMessage);
    Bytes responderMac = concatenate(macSecret.xor(initiatorNonce), responderMessage);

    if (initiator) {
      return new RLPxConnection(aesSecret, macSecret, token, initiatorMac, responderMac);
    } else {
      return new RLPxConnection(aesSecret, macSecret, token, responderMac, initiatorMac);
    }
  }

  private static Bytes encryptMessage(Bytes message, PublicKey remoteKey) {
    byte[] ivb = new byte[16];
    random.nextBytes(ivb);
    Bytes iv = Bytes.wrap(ivb);
    KeyPair ephemeralKeyPair = KeyPair.random();
    Bytes bytes = addPadding(message);
    int size = bytes.size() + 32 + 65 + 16 + 3;
    Bytes sizePrefix = Bytes.of((byte) (size >>> 8), (byte) size);
    EthereumIESEncryptionEngine engine = forEncryption(remoteKey, iv, sizePrefix, ephemeralKeyPair);
    byte[] encrypted;
    try {
      encrypted = engine.processBlock(bytes.toArrayUnsafe(), 0, bytes.size());
    } catch (InvalidCipherTextException e) {
      throw new IllegalArgumentException(e);
    }
    // Create the output message by concatenating the ephemeral public key (prefixed with
    // 0x04 to designate uncompressed), IV, and encrypted bytes.
    return concatenate(
        Bytes.of(sizePrefix.get(0), sizePrefix.get(1), (byte) 0x04),
        ephemeralKeyPair.publicKey().bytes(),
        iv,
        Bytes.wrap(encrypted));
  }

  private static EthereumIESEncryptionEngine forEncryption(
      PublicKey pubKey,
      Bytes iv,
      Bytes commonMac,
      KeyPair ephemeralKeyPair) {
    CipherParameters pubParam = new ECPublicKeyParameters(pubKey.asEcPoint(), CURVE);
    CipherParameters privParam =
        new ECPrivateKeyParameters(ephemeralKeyPair.secretKey().bytes().toUnsignedBigInteger(), CURVE);

    BasicAgreement agree = new ECDHBasicAgreement();
    agree.init(privParam);
    BigInteger z = agree.calculateAgreement(pubParam);
    byte[] zbytes = BigIntegers.asUnsignedByteArray(agree.getFieldSize(), z);

    IESWithCipherParameters iesWithCipherParameters = new IESWithCipherParameters(new byte[0], new byte[0], 128, 128);

    // Initialise the KDF.
    EthereumIESEncryptionEngine.ECIESHandshakeKDFFunction kdf =
        new EthereumIESEncryptionEngine.ECIESHandshakeKDFFunction(1, new SHA256Digest());
    kdf.init(new KDFParameters(zbytes, iesWithCipherParameters.getDerivationV()));
    EthereumIESEncryptionEngine engine = new EthereumIESEncryptionEngine(
        agree,
        kdf,
        new HMac(new SHA256Digest()),
        commonMac.toArrayUnsafe(),
        new BufferedBlockCipher(new SICBlockCipher(new AESEngine())));
    ParametersWithIV cipherParameters = new ParametersWithIV(iesWithCipherParameters, iv.toArrayUnsafe());
    engine.init(true, privParam, pubParam, cipherParameters);

    return engine;
  }

  private static Bytes decryptMessage(Bytes msgBytes, SecretKey ourKey) {
    PublicKey ephemeralPublicKey = PublicKey.fromBytes(msgBytes.slice(3, 64));

    // Strip off the IV to use.
    Bytes commonMac = msgBytes.slice(0, 2);
    Bytes iv = msgBytes.slice(67, 16);

    // Extract the encrypted payload.
    Bytes encrypted = msgBytes.slice(83);

    EthereumIESEncryptionEngine decryptor = forDecryption(ourKey, ephemeralPublicKey, iv, commonMac);
    byte[] result;
    try {
      result = decryptor.processBlock(encrypted.toArrayUnsafe(), 0, encrypted.size());
    } catch (InvalidCipherTextException e) {
      throw new IllegalArgumentException(e);
    }
    return Bytes.wrap(result);
  }

  private static Bytes addPadding(final Bytes message) {
    final int padding = 100 + random.nextInt(200);
    final byte[] paddingBytes = new byte[padding];
    random.nextBytes(paddingBytes);
    return concatenate(message, Bytes.wrap(paddingBytes));
  }

  private static EthereumIESEncryptionEngine forDecryption(
      SecretKey privateKey,
      PublicKey ephemeralPublicKey,
      Bytes iv,
      Bytes commonMac) {
    CipherParameters pubParam = new ECPublicKeyParameters(ephemeralPublicKey.asEcPoint(), CURVE);
    CipherParameters privParam = new ECPrivateKeyParameters(privateKey.bytes().toUnsignedBigInteger(), CURVE);

    BasicAgreement agreement = new ECDHBasicAgreement();
    agreement.init(privParam);
    byte[] agreementValue =
        BigIntegers.asUnsignedByteArray(agreement.getFieldSize(), agreement.calculateAgreement(pubParam));

    IESWithCipherParameters iesWithCipherParameters = new IESWithCipherParameters(new byte[0], new byte[0], 128, 128);

    EthereumIESEncryptionEngine.ECIESHandshakeKDFFunction kdf =
        new EthereumIESEncryptionEngine.ECIESHandshakeKDFFunction(1, new SHA256Digest());
    kdf.init(new KDFParameters(agreementValue, iesWithCipherParameters.getDerivationV()));
    EthereumIESEncryptionEngine engine = new EthereumIESEncryptionEngine(
        agreement,
        kdf,
        new HMac(new SHA256Digest()),
        commonMac.toArrayUnsafe(),
        new BufferedBlockCipher(new SICBlockCipher(new AESEngine())));
    ParametersWithIV cipherParameters = new ParametersWithIV(iesWithCipherParameters, iv.toArrayUnsafe());
    engine.init(false, privParam, pubParam, cipherParameters);
    return engine;
  }
}
