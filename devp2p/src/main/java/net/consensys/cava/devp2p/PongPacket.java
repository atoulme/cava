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

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.crypto.SECP256K1.KeyPair;
import net.consensys.cava.rlp.RLP;

import com.google.common.base.Objects;

final class PongPacket extends Packet {

  static PongPacket decode(Bytes payloadBytes, PacketHeader packetHeader) {
    return RLP.decodeList(payloadBytes, (listReader) -> {
      Endpoint to = Endpoint.readFrom(listReader);
      Bytes pingHash = listReader.readValue();
      long expiration = listReader.readLong();
      return new PongPacket(expiration, packetHeader, to, pingHash);
    });
  }

  private final Endpoint to;
  private final Bytes pingHash;

  PongPacket(long expiration, KeyPair keyPair, Endpoint to, Bytes pingHash) {
    super(expiration, keyPair);
    this.to = to;
    this.pingHash = pingHash;
  }

  PongPacket(long expiration, PacketHeader packetHeader, Endpoint to, Bytes pingHash) {
    super(expiration, packetHeader);
    this.to = to;
    this.pingHash = pingHash;
  }

  Bytes pingHash() {
    return pingHash;
  }

  Endpoint to() {
    return to;
  }

  @Override
  public Bytes createPayloadBytes() {
    return RLP.encodeList(writer -> {
      writer.writeList(to::writeTo);
      writer.writeValue(pingHash);
      writer.writeLong(expiration());
    });
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PongPacket)) {
      return false;
    }
    PongPacket that = (PongPacket) o;
    return Objects.equal(to, that.to) && Objects.equal(pingHash, that.pingHash) && expiration() == that.expiration();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(to, pingHash, expiration());
  }

  @Override
  public String toString() {
    return "PongPayload{" + "to=" + to + ", pingHash=" + pingHash + '}';
  }

  @Override
  protected byte getPacketType() {
    return 0x02;
  }
}
