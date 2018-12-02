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

import static org.junit.jupiter.api.Assertions.*;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.concurrent.AsyncResult;
import net.consensys.cava.crypto.SECP256K1;
import net.consensys.cava.crypto.SECP256K1.KeyPair;
import net.consensys.cava.crypto.SECP256K1.SecretKey;
import net.consensys.cava.junit.BouncyCastleExtension;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class RLPxConnectionFactoryTest {

  @Test
  void roundtripPayload() {
    KeyPair exampleKeyPair = SECP256K1.KeyPair.fromSecretKey(
        SecretKey
            .fromBytes(Bytes32.fromHexString("0xEE647A774DF811AB577BA5F397D56BE6567DA58AF7A65368F01DD7A8313812D8")));

    Bytes payload = Bytes.fromHexString(
        "0xF8A7B84135A22239600070940908090D5F051B2C597981B090E386360B87163A8AF1EDF0434A84AA31A582DF93A0396D4CC2E3574C919B0E8D47DCEA095647446C88B36D01B840B8497006E23B1F35BD6E988EC53EE759BC852162049972F777B92B5E029B840BE8BE93F513DA55B81AEE463254930EE30667825B0B6FE30938FFFA7024A03C5AA02B310D67A36F599EAB6B8D03FECB9D782CC7A0EB12FECBFF454A4094557A2EB704");
    InitiatorHandshakeMessage initial = InitiatorHandshakeMessage.decode(payload, exampleKeyPair.secretKey());
    Bytes encoded = initial.encode();
    assertEquals(payload, encoded);
  }

  @Test
  void roundtripInitiatorHandshakeBytes() {
    KeyPair keyPair = KeyPair.random();
    KeyPair peerKeyPair = KeyPair.random();
    byte[] nonce = new byte[32];
    new SecureRandom().nextBytes(nonce);

    Bytes payload = RLPxConnectionFactory.init(keyPair, peerKeyPair.publicKey(), KeyPair.random(), Bytes32.wrap(nonce));
    InitiatorHandshakeMessage init = RLPxConnectionFactory.read(payload, peerKeyPair.secretKey());
    assertEquals(keyPair.publicKey(), init.publicKey());
    assertEquals(Bytes.wrap(nonce), init.nonce());
  }

  @Test
  void roundtripResponseHandshakeBytes() {
    KeyPair keyPair = KeyPair.random();
    KeyPair peerKeyPair = KeyPair.random();
    byte[] nonce = new byte[32];
    new SecureRandom().nextBytes(nonce);

    Bytes payload = RLPxConnectionFactory.init(keyPair, peerKeyPair.publicKey(), KeyPair.random(), Bytes32.wrap(nonce));

    AtomicReference<Bytes> ref = new AtomicReference<>();
    RLPxConnectionFactory.respondToHandshake(payload, peerKeyPair.secretKey(), ref::set);
    ResponderHandshakeMessage responder = RLPxConnectionFactory.readResponse(ref.get(), keyPair.secretKey());
    assertNotNull(responder);
  }

  @Test
  void createHandshake() {
    KeyPair keyPair = KeyPair.random();
    KeyPair peerKeyPair = KeyPair.random();
    byte[] nonce = new byte[32];
    new SecureRandom().nextBytes(nonce);

    KeyPair ephemeralKeyPair = KeyPair.random();

    Bytes payload = RLPxConnectionFactory.init(keyPair, peerKeyPair.publicKey(), ephemeralKeyPair, Bytes32.wrap(nonce));

    AtomicReference<Bytes> ref = new AtomicReference<>();
    RLPxConnection conn = RLPxConnectionFactory.respondToHandshake(payload, peerKeyPair.secretKey(), ref::set);
    ResponderHandshakeMessage responder = RLPxConnectionFactory.readResponse(ref.get(), keyPair.secretKey());

    assertNotNull(conn);
    assertNotNull(responder);
  }

  @Test
  void createHandshakeAsync() throws TimeoutException, InterruptedException {
    KeyPair keyPair = KeyPair.random();
    KeyPair peerKeyPair = KeyPair.random();

    AtomicReference<RLPxConnection> peerConnectionReference = new AtomicReference<>();
    Function<Bytes, AsyncResult<Bytes>> wireBytes = (bytes) -> {
      AtomicReference<Bytes> responseReference = new AtomicReference<>();
      peerConnectionReference
          .set(RLPxConnectionFactory.respondToHandshake(bytes, peerKeyPair.secretKey(), responseReference::set));
      return AsyncResult.completed(responseReference.get());
    };
    AsyncResult<RLPxConnection> futureConn =
        RLPxConnectionFactory.createHandshake(keyPair, peerKeyPair.publicKey(), wireBytes);

    RLPxConnection conn = futureConn.get(1, TimeUnit.SECONDS);
    assertNotNull(conn);
    assertTrue(conn.isComplementedBy(peerConnectionReference.get()));
  }

}
