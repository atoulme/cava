/*
 * Copyright 2019 ConsenSys AG.
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
package net.consensys.cava.scuttlebutt;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.crypto.SECP256K1;
import net.consensys.cava.crypto.sodium.Signature;
import net.consensys.cava.io.Base64;

import java.util.List;

import com.google.common.base.Splitter;

/**
 * A Scuttlebutt identity, backed by a public key.
 *
 * Currently supported: Ed25519 and SECP256K1.
 */
public interface Identity {

  /**
   * Curves supported by those identities.
   */
  public enum Curve {
    Ed25519("ed25519"), SECP256K1("secp256k1");

    public final String name;

    Curve(String name) {
      this.name = name;
    }

    /**
     * Provides the curve associated with the curve name, or null if none match the name.
     *
     * @param name the name of the curve
     * @return the curve, or null if no curve is supported by that name
     */
    static Curve fromName(String name) {
      if (SECP256K1.name.equals(name)) {
        return SECP256K1;
      } else if (Ed25519.name.equals(name)) {
        return Ed25519;
      }
      return null;
    }
  }

  /**
   * Creates a new Ed25519 identity backed by this key pair.
   *
   * @param keyPair the key pair of the identity
   * @return a new Scuttlebutt identity
   */
  static Identity fromKeyPair(Signature.KeyPair keyPair) {
    return new Ed25519KeyPairIdentity(keyPair);
  }

  /**
   * Creates a new SECP256K1 identity backed by this key pair.
   *
   * @param keyPair the key pair of the identity
   * @return a new Scuttlebutt identity
   */
  static Identity fromKeyPair(SECP256K1.KeyPair keyPair) {
    return new SECP256K1KeyPairIdentity(keyPair);
  }

  /**
   * Creates a new Ed25519 identity backed by this secret key.
   *
   * @param secretKey the secret key of the identity
   * @return a new Scuttlebutt identity
   */
  static Identity fromSecretKey(Signature.SecretKey secretKey) {
    return fromKeyPair(Signature.KeyPair.forSecretKey(secretKey));
  }

  /**
   * Creates a new SECP256K1 identity backed by this secret key.
   *
   * @param secretKey the secret key of the identity
   * @return a new Scuttlebutt identity
   */
  static Identity fromSecretKey(SECP256K1.SecretKey secretKey) {
    return fromKeyPair(SECP256K1.KeyPair.fromSecretKey(secretKey));
  }

  /**
   * Creates a new random Ed25519 identity.
   *
   * @return a new Scuttlebutt identity
   */
  static Identity random() {
    return randomEd25519();
  }

  /**
   * Creates a new random Ed25519 identity.
   *
   * @return a new Scuttlebutt identity
   */
  static Identity randomEd25519() {
    return new Ed25519KeyPairIdentity(Signature.KeyPair.random());
  }


  /**
   * Creates a new random secp251k1 identity.
   *
   * @return a new Scuttlebutt identity
   */
  static Identity randomSECP256K1() {
    return new SECP256K1KeyPairIdentity(SECP256K1.KeyPair.random());
  }

  /**
   * Creates a new SECP256K1 identity backed by this secret key.
   *
   * @param publicKey the secret key of the identity
   * @return a new Scuttlebutt identity
   */
  static Identity fromPublicKey(SECP256K1.PublicKey publicKey) {
    return new SECP256K1PublicKeyIdentity(publicKey);
  }

  /**
   * Creates a new Ed25519 identity backed by this secret key.
   *
   * @param publicKey the secret key of the identity
   * @return a new Scuttlebutt identity
   */
  static Identity fromPublicKey(Signature.PublicKey publicKey) {
    return new Ed25519PublicKeyIdentity(publicKey);
  }

  /**
   * Creates a new identity from its canonical form.
   *
   * @param id the identity in a canonical form
   * @return the identity, decoded
   */
  static Identity fromCanonicalForm(String id) {
    if (!id.startsWith("@")) {
      throw new IllegalArgumentException("The canonical form should start with @");
    }
    List<String> segments = Splitter.on('.').splitToList(id.substring(1));
    if (segments.size() != 2) {
      throw new IllegalArgumentException("The canonical form should be of the form @base64-encoded public key.curve");
    }
    Curve curve = Curve.fromName(segments.get(1));
    if (curve == null) {
      throw new IllegalArgumentException("Unsupported curve " + segments.get(1));
    }
    Bytes pubKey = Base64.decode(segments.get(0));
    if (curve == Curve.SECP256K1) {
      return fromPublicKey(SECP256K1.PublicKey.fromBytes(pubKey));
    } else if (curve == Curve.Ed25519) {
      return fromPublicKey(Signature.PublicKey.fromBytes(pubKey));
    } else {
      throw new UnsupportedOperationException("Unsupported identity " + curve);
    }
  }

  /**
   * Hashes data using the secret key of the identity.
   *
   * @param message the message to sign
   * @return the signature
   * @throws UnsupportedOperationException if the identity doesn't contain a secret key
   */
  Bytes sign(Bytes message);

  /**
   * Verifies a signature matches a message according to the public key of the identity.
   * 
   * @param signature the signature to test
   * @param message the data that was signed by the signature
   * @return true if the signature matches the message according to the public key of the identity
   */
  boolean verify(Bytes signature, Bytes message);

  /**
   * Provides the base64 encoded representation of the public key of the identity
   *
   * @return the base64 encoded representation of the public key of the identity
   */
  String publicKeyAsBase64String();

  /**
   * Provides the curve associated with this identity
   *
   * @return the curve associated with this identity
   */
  Curve curve();

  /**
   * Provides the name of the curve associated with this identity
   * 
   * @return the name of the curve associated with this identity
   */
  default String curveName() {
    return curve().name;
  }

  /**
   * Provides the identity's associated Ed25519 public key.
   * 
   * @return the identity's associated Ed25519 public key
   * @throws UnsupportedOperationException if the identity does not use the Ed25519 algorithm.
   */
  Signature.PublicKey ed25519PublicKey();

  /**
   * Provides the identity's associated SECP256K1 public key.
   * 
   * @return the identity's associated SECP256K1 public key
   * @throws UnsupportedOperationException if the identity does not use the SECP256K1 algorithm.
   */
  SECP256K1.PublicKey secp256k1PublicKey();

  /**
   * Encodes the identity into a canonical Scuttlebutt identity string
   *
   * @return the identity, as a Scuttlebutt identity string representation
   */
  default String toCanonicalForm() {
    return "@" + publicKeyAsBase64String() + "." + curveName();
  }
}
