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
import net.consensys.cava.units.bigints.UInt256;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class GetBlockHeadersMessageTest {

  @Test
  void roundtripBytes() {
    GetBlockHeadersMessage message = new GetBlockHeadersMessage(
        344,
        Arrays.asList(
            new GetBlockHeadersMessage.BlockHeaderQuery(
                Bytes32.random(),
                UInt256.valueOf(32),
                UInt256.valueOf(64),
                GetBlockHeadersMessage.BlockHeaderQuery.Direction.BACKWARDS),
            new GetBlockHeadersMessage.BlockHeaderQuery(
                Bytes32.random(),
                UInt256.valueOf(32),
                UInt256.valueOf(64),
                GetBlockHeadersMessage.BlockHeaderQuery.Direction.FORWARD),
            new GetBlockHeadersMessage.BlockHeaderQuery(
                Bytes32.random(),
                UInt256.valueOf(32),
                UInt256.valueOf(64),
                GetBlockHeadersMessage.BlockHeaderQuery.Direction.BACKWARDS)));

    Bytes bytes = message.toBytes();
    assertEquals(message, GetBlockHeadersMessage.read(bytes));
  }
}
