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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 *
 * Inform a peer of the sender's current LES state. This message should be sent after the initial handshake and prior to
 * any LES related messages. The following keys should be present (except the optional ones) in order to be accepted by
 * a LES/1 node: (value types are noted after the key string)
 *
 * @link https://github.com/ethereum/wiki/wiki/Light-client-protocol
 */
final class StatusMessage {

  /**
   * Reads a status message from bytes, and associates it with a connection ID.
   *
   * @param bytes the bytes of the message
   * @return a new StatusMessage built from the bytes
   */
  static StatusMessage read(Bytes bytes) {
    return RLP.decode(bytes, reader -> {
      Map<String, Object> parameters = new HashMap<>();
      while (!reader.isComplete()) {
        reader.readList(eltReader -> {
          String key = eltReader.readString();

          if ("protocolVersion".equals(key) || "networkId".equals(key) || "announceType".equals(key)) {
            parameters.put(key, eltReader.readInt());
          } else if ("headHash".equals(key) || "genesisHash".equals(key)) {
            parameters.put(key, Bytes32.wrap(eltReader.readValue()));
          } else if ("headTd".equals(key)
              || "headNum".equals(key)
              || "serveChainSince".equals(key)
              || "serveStateSince".equals(key)
              || "flowControl/BL".equals(key)
              || "flowControl/MRC".equals(key)
              || "flowControl/MRR".equals(key)) {
            parameters.put(key, eltReader.readUInt256());
          } else if ("serveHeaders".equals(key) || "txRelay".equals(key)) {
            parameters.put(key, true);
          }
          return null;
        });
      }

      return new StatusMessage(
          (int) parameters.get("protocolVersion"),
          (int) parameters.get("networkId"),
          (UInt256) parameters.get("headTd"),
          (Bytes32) parameters.get("headHash"),
          (UInt256) parameters.get("headNum"),
          (Bytes32) parameters.get("genesisHash"),
          (Boolean) parameters.get("serveHeaders"),
          (UInt256) parameters.get("serveChainSince"),
          (UInt256) parameters.get("serveStateSince"),
          (Boolean) parameters.get("txRelay"),
          (UInt256) parameters.get("flowControl/BL"),
          (UInt256) parameters.get("flowControl/MRC"),
          (UInt256) parameters.get("flowControl/MRR"),
          (int) parameters.get("announceType"));
    });
  }

  private final int protocolVersion;
  private final int networkId;
  private final UInt256 headTd;
  private final Bytes32 headHash;
  private final UInt256 headNum;
  private final Bytes32 genesisHash;
  private final Boolean serveHeaders;
  private final UInt256 serveChainSince;
  private final UInt256 serveStateSince;
  private final Boolean txRelay;
  private final UInt256 flowControlBufferLimit;
  private final UInt256 flowControlMaximumRequestCostTable;
  private final UInt256 flowControlMinimumRateOfRecharge;
  private final int announceType;

  StatusMessage(
      int protocolVersion,
      int networkId,
      UInt256 headTd,
      Bytes32 headHash,
      UInt256 headNum,
      Bytes32 genesisHash,
      @Nullable Boolean serveHeaders,
      @Nullable UInt256 serveChainSince,
      @Nullable UInt256 serveStateSince,
      @Nullable Boolean txRelay,
      UInt256 flowControlBufferLimit,
      UInt256 flowControlMaximumRequestCostTable,
      UInt256 flowControlMinimumRateOfRecharge,
      int announceType) {
    this.protocolVersion = protocolVersion;
    this.networkId = networkId;
    this.headTd = headTd;
    this.headHash = headHash;
    this.headNum = headNum;
    this.genesisHash = genesisHash;
    this.serveHeaders = serveHeaders;
    this.serveChainSince = serveChainSince;
    this.serveStateSince = serveStateSince;
    this.txRelay = txRelay;
    this.flowControlBufferLimit = flowControlBufferLimit;
    this.flowControlMaximumRequestCostTable = flowControlMaximumRequestCostTable;
    this.flowControlMinimumRateOfRecharge = flowControlMinimumRateOfRecharge;
    this.announceType = announceType;
  }

  Bytes toBytes() {
    return RLP.encode(writer -> {
      writer.writeList(listWriter -> {
        listWriter.writeString("protocolVersion");
        listWriter.writeInt(protocolVersion);
      });
      writer.writeList(listWriter -> {
        listWriter.writeString("networkId");
        listWriter.writeInt(networkId);
      });
      writer.writeList(listWriter -> {
        listWriter.writeString("headTd");
        listWriter.writeUInt256(headTd);
      });
      writer.writeList(listWriter -> {
        listWriter.writeString("headHash");
        listWriter.writeValue(headHash);
      });
      writer.writeList(listWriter -> {
        listWriter.writeString("headNum");
        listWriter.writeUInt256(headNum);
      });
      writer.writeList(listWriter -> {
        listWriter.writeString("genesisHash");
        listWriter.writeValue(genesisHash);
      });
      if (serveHeaders != null && serveHeaders) {
        writer.writeList(listWriter -> listWriter.writeString("serveHeaders"));
      }
      if (serveChainSince != null) {
        writer.writeList(listWriter -> {
          listWriter.writeString("serveChainSince");
          listWriter.writeUInt256(serveChainSince);
        });
      }
      if (serveStateSince != null) {
        writer.writeList(listWriter -> {
          listWriter.writeString("serveStateSince");
          listWriter.writeUInt256(serveStateSince);
        });
      }
      if (txRelay != null && txRelay) {
        writer.writeList(listWriter -> listWriter.writeString("txRelay"));
      }
      writer.writeList(listWriter -> {
        listWriter.writeString("flowControl/BL");
        listWriter.writeUInt256(flowControlBufferLimit);
      });
      writer.writeList(listWriter -> {
        listWriter.writeString("flowControl/MRC");
        listWriter.writeUInt256(flowControlMaximumRequestCostTable);
      });
      writer.writeList(listWriter -> {
        listWriter.writeString("flowControl/MRR");
        listWriter.writeUInt256(flowControlMinimumRateOfRecharge);
      });
      writer.writeList(listWriter -> {
        listWriter.writeString("announceType");
        listWriter.writeInt(announceType);
      });

    });
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    StatusMessage message = (StatusMessage) o;
    return protocolVersion == message.protocolVersion
        && networkId == message.networkId
        && headTd.equals(message.headTd)
        && headHash.equals(message.headHash)
        && headNum.equals(message.headNum)
        && genesisHash.equals(message.genesisHash)
        && Objects.equals(serveHeaders, message.serveHeaders)
        && Objects.equals(serveChainSince, message.serveChainSince)
        && Objects.equals(serveStateSince, message.serveStateSince)
        && Objects.equals(txRelay, message.txRelay)
        && Objects.equals(flowControlBufferLimit, message.flowControlBufferLimit)
        && Objects.equals(flowControlMaximumRequestCostTable, message.flowControlMaximumRequestCostTable)
        && Objects.equals(flowControlMinimumRateOfRecharge, message.flowControlMinimumRateOfRecharge)
        && announceType == message.announceType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        protocolVersion,
        networkId,
        headTd,
        headHash,
        headNum,
        genesisHash,
        serveHeaders,
        serveChainSince,
        serveStateSince,
        txRelay,
        flowControlBufferLimit,
        flowControlMaximumRequestCostTable,
        flowControlMinimumRateOfRecharge,
        announceType);
  }

  @Override
  public String toString() {
    return "StatusMessage{"
        + '\''
        + ", protocolVersion="
        + protocolVersion
        + ", networkId="
        + networkId
        + ", headTd="
        + headTd
        + ", headHash="
        + headHash
        + ", headNum="
        + headNum
        + ", genesisHash="
        + genesisHash
        + ", serveHeaders="
        + serveHeaders
        + ", serveChainSince="
        + serveChainSince
        + ", serveStateSince="
        + serveStateSince
        + ", txRelay="
        + txRelay
        + ", flowControlBufferLimit="
        + flowControlBufferLimit
        + ", flowControlMaximumRequestCostTable="
        + flowControlMaximumRequestCostTable
        + ", flowControlMinimumRateOfRecharge="
        + flowControlMinimumRateOfRecharge
        + ", announceType="
        + announceType
        + '}';
  }

  int protocolVersion() {
    return protocolVersion;
  }

  int networkId() {
    return networkId;
  }

  UInt256 headTd() {
    return headTd;
  }

  Bytes32 headHash() {
    return headHash;
  }

  UInt256 headNum() {
    return headNum;
  }

  Bytes32 genesisHash() {
    return genesisHash;
  }

  Boolean serveHeaders() {
    return serveHeaders;
  }

  UInt256 serveChainSince() {
    return serveChainSince;
  }

  UInt256 serveStateSince() {
    return serveStateSince;
  }

  Boolean txRelay() {
    return txRelay;
  }

  UInt256 flowControlBufferLimit() {
    return flowControlBufferLimit;
  }

  UInt256 flowControlMaximumRequestCostTable() {
    return flowControlMaximumRequestCostTable;
  }

  UInt256 flowControlMinimumRateOfRecharge() {
    return flowControlMinimumRateOfRecharge;
  }
}
