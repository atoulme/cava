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

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.eth.BlockBody;
import net.consensys.cava.rlp.RLP;

import java.util.List;
import java.util.Objects;

final class BlockBodiesMessage {

  static BlockBodiesMessage read(Bytes bytes) {
    return RLP.decodeList(
        bytes,
        reader -> new BlockBodiesMessage(
            reader.readLong(),
            reader.readLong(),
            reader.readListContents(BlockBody::readFrom)));
  }

  private final long reqID;
  private final long bufferValue;
  private final List<BlockBody> blockBodies;

  BlockBodiesMessage(long reqID, long bufferValue, List<BlockBody> blockBodies) {
    this.reqID = reqID;
    this.bufferValue = bufferValue;
    this.blockBodies = blockBodies;
  }

  Bytes toBytes() {
    return RLP.encodeList(writer -> {
      writer.writeLong(reqID);
      writer.writeLong(bufferValue);
      writer.writeList(blockBodies, (eltWriter, blockBody) -> blockBody.writeTo(eltWriter));
    });
  }

  long reqID() {
    return reqID;
  }

  long bufferValue() {
    return bufferValue;
  }

  List<BlockBody> blockBodies() {
    return blockBodies;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    BlockBodiesMessage that = (BlockBodiesMessage) o;
    return reqID == that.reqID && bufferValue == that.bufferValue && Objects.equals(blockBodies, that.blockBodies);
  }

  @Override
  public int hashCode() {
    return Objects.hash(reqID, bufferValue, blockBodies);
  }
}
