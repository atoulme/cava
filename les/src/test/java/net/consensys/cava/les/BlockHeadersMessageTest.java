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
package net.consensys.cava.les;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.eth.Address;
import net.consensys.cava.eth.BlockHeader;
import net.consensys.cava.eth.Hash;
import net.consensys.cava.units.bigints.UInt256;
import net.consensys.cava.units.ethereum.Gas;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class BlockHeadersMessageTest {

  @Test
  void roundtripRLP() {
    BlockHeader header = new BlockHeader(
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Address.fromBytes(Bytes.random(20)),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random(),
        UInt256.fromBytes(Bytes32.random()),
        UInt256.fromBytes(Bytes32.random()),
        Gas.valueOf(3),
        Gas.valueOf(2),
        Instant.now().truncatedTo(ChronoUnit.SECONDS),
        Bytes.of(2, 3, 4),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random());
    BlockHeadersMessage message = new BlockHeadersMessage(3L, 2L, Arrays.asList(header));
    Bytes bytes = message.toBytes();
    assertEquals(message, BlockHeadersMessage.read(bytes));
  }
}
