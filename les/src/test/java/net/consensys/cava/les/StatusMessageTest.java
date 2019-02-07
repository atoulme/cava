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

import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.units.bigints.UInt256;

import org.junit.jupiter.api.Test;

class StatusMessageTest {

  @Test
  void testStatusMessageRoundtrip() {
    StatusMessage message = new StatusMessage(
        2,
        1,
        UInt256.valueOf(23),
        Bytes32.random(),
        UInt256.valueOf(3443),
        Bytes32.random(),
        null,
        UInt256.valueOf(333),
        UInt256.valueOf(453),
        true,
        UInt256.valueOf(3),
        UInt256.valueOf(4),
        UInt256.valueOf(5),
        1);
    StatusMessage read = StatusMessage.read(message.toBytes());
    assertEquals(message, read);
  }
}
