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

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.crypto.SECP256K1.KeyPair;

import org.junit.jupiter.api.Test;

class FindNeighborsPacketTest {

  @Test
  void testToBytesAndBack() {
    KeyPair keyPair = KeyPair.random();
    FindNeighborsPacket payload = new FindNeighborsPacket(20L, keyPair, Bytes.of(1, 2, 3));
    FindNeighborsPacket read = FindNeighborsPacket
        .decode(payload.payloadBytes(), new PacketHeader(keyPair, (byte) 0x03, payload.payloadBytes()));
    assertEquals(payload, read);
  }
}
