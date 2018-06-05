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

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.crypto.SECP256K1.KeyPair;
import net.consensys.cava.junit.BouncyCastleExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class PacketTest {

  private Endpoint to = new Endpoint("127.0.0.1", 7654, 8765);;
  private Endpoint from = new Endpoint("127.0.0.2", 7644, 2765);

  @Test
  void toBytesIsCached() {
    PingPacket ping = Packet.createPing(from, to, 10L, KeyPair.random());
    assertSame(ping.toBytes(), ping.toBytes());
  }

  @Test
  void createPacketAndReadItBack() {
    FindNeighborsPacket packet = new FindNeighborsPacket(20, KeyPair.random(), Bytes.of(3));
    Bytes encoded = packet.toBytes();
    FindNeighborsPacket readPacket = FindNeighborsPacket.decode(encoded.slice(98), PacketHeader.decode(encoded));
    assertEquals(readPacket, packet);
  }

}
