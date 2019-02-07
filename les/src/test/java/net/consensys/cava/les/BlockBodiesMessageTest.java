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
import net.consensys.cava.crypto.SECP256K1;
import net.consensys.cava.eth.Address;
import net.consensys.cava.eth.BlockBody;
import net.consensys.cava.eth.Transaction;
import net.consensys.cava.junit.BouncyCastleExtension;
import net.consensys.cava.units.bigints.UInt256;
import net.consensys.cava.units.ethereum.Gas;
import net.consensys.cava.units.ethereum.Wei;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class BlockBodiesMessageTest {

  @Test
  void roundtripRLP() {
    BlockBodiesMessage message = new BlockBodiesMessage(
        3,
        2,
        Arrays.asList(
            new BlockBody(
                Collections.singletonList(
                    new Transaction(
                        UInt256.valueOf(1),
                        Wei.valueOf(2),
                        Gas.valueOf(2),
                        Address.fromBytes(Bytes.random(20)),
                        Wei.valueOf(2),
                        Bytes.random(12),
                        SECP256K1.KeyPair.random())),
                Collections.emptyList())));
    Bytes rlp = message.toBytes();
    BlockBodiesMessage read = BlockBodiesMessage.read(rlp);
    assertEquals(message, read);
  }
}
