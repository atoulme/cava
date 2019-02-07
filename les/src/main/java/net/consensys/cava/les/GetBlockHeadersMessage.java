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
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.rlp.RLP;
import net.consensys.cava.units.bigints.UInt256;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class GetBlockHeadersMessage {

  final static class BlockHeaderQuery {

    enum Direction {
      BACKWARDS, FORWARD
    }

    private final Bytes32 blockNumberOrBlockHash;
    private final UInt256 maxHeaders;
    private final UInt256 skip;
    private final Direction direction;

    BlockHeaderQuery(Bytes32 blockNumberOrBlockHash, UInt256 maxHeaders, UInt256 skip, Direction direction) {
      this.blockNumberOrBlockHash = blockNumberOrBlockHash;
      this.maxHeaders = maxHeaders;
      this.skip = skip;
      this.direction = direction;
    }

    Bytes32 blockNumberOrBlockHash() {
      return blockNumberOrBlockHash;
    }

    UInt256 maxHeaders() {
      return maxHeaders;
    }

    UInt256 skip() {
      return skip;
    }

    Direction direction() {
      return direction;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      BlockHeaderQuery query = (BlockHeaderQuery) o;
      return direction == query.direction
          && Objects.equals(blockNumberOrBlockHash, query.blockNumberOrBlockHash)
          && Objects.equals(maxHeaders, query.maxHeaders)
          && Objects.equals(skip, query.skip);
    }

    @Override
    public int hashCode() {
      return Objects.hash(blockNumberOrBlockHash, maxHeaders, skip, direction);
    }
  }

  static GetBlockHeadersMessage read(Bytes bytes) {
    return RLP.decodeList(bytes, reader -> {
      long reqId = reader.readLong();
      List<BlockHeaderQuery> queries = new ArrayList<>();
      while (!reader.isComplete()) {
        queries.add(
            reader.readList(
                queryReader -> new BlockHeaderQuery(
                    Bytes32.wrap(queryReader.readValue()),
                    queryReader.readUInt256(),
                    queryReader.readUInt256(),
                    queryReader.readInt() == 1 ? BlockHeaderQuery.Direction.BACKWARDS
                        : BlockHeaderQuery.Direction.FORWARD)));
      }
      return new GetBlockHeadersMessage(reqId, queries);
    });
  }

  private final long reqID;
  private final List<BlockHeaderQuery> queries;

  GetBlockHeadersMessage(long reqID, List<BlockHeaderQuery> queries) {
    this.reqID = reqID;
    this.queries = queries;
  }

  long reqID() {
    return reqID;
  }

  List<BlockHeaderQuery> queries() {
    return queries;
  }

  Bytes toBytes() {
    return RLP.encodeList(writer -> {
      writer.writeLong(reqID);
      for (BlockHeaderQuery query : queries) {
        writer.writeList(queryWriter -> {
          queryWriter.writeValue(query.blockNumberOrBlockHash());
          queryWriter.writeUInt256(query.maxHeaders());
          queryWriter.writeUInt256(query.skip());
          queryWriter.writeInt(query.direction() == BlockHeaderQuery.Direction.BACKWARDS ? 1 : 0);
        });
      }
    });
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    GetBlockHeadersMessage message = (GetBlockHeadersMessage) o;
    return Objects.equals(reqID, message.reqID) && Objects.equals(queries, message.queries);
  }

  @Override
  public int hashCode() {
    return Objects.hash(reqID, queries);
  }
}
