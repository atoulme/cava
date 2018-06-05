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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.crypto.SECP256K1.KeyPair;
import net.consensys.cava.junit.BouncyCastleExtension;
import net.consensys.cava.rlp.RLP;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class PingPacketTest {

  private Endpoint to = new Endpoint("127.0.0.1", 7654, 8765);;
  private Endpoint from = new Endpoint("127.0.0.2", 7644, 2765);

  @Test
  void testToBytesAndBack() {
    KeyPair keyPair = KeyPair.random();
    PingPacket payload = new PingPacket(20L, keyPair, from, to);
    PingPacket read =
        PingPacket.decode(payload.payloadBytes(), new PacketHeader(keyPair, (byte) 0x01, payload.payloadBytes()));
    assertEquals(payload, read);
  }

  @Test
  void toBytesCached() {
    PingPacket payload = new PingPacket(20L, KeyPair.random(), from, to);
    assertSame(payload.toBytes(), payload.toBytes());
  }

  @Test
  void invalidVersion() {
    KeyPair keyPair = KeyPair.random();
    assertThrows(PeerDiscoveryPacketDecodingException.class, () -> {
      PingPacket
          .decode(RLP.encodeList(writer -> writer.writeInt(3)), new PacketHeader(keyPair, (byte) 0x01, Bytes.EMPTY));
    });
  }

}
