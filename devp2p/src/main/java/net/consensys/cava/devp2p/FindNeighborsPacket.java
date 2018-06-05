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

final class FindNeighborsPacket extends Packet {

  static FindNeighborsPacket decode(Bytes payloadBytes, PacketHeader packetHeader) {
    return RLP.decodeList(payloadBytes, (listReader) -> {
      Bytes target = listReader.readValue();
      long expiration = listReader.readLong();
      return new FindNeighborsPacket(expiration, packetHeader, target);
    });
  }

  private final Bytes target;

  FindNeighborsPacket(long expiration, KeyPair keyPair, Bytes target) {
    super(expiration, keyPair);
    this.target = target;
  }

  private FindNeighborsPacket(long expiration, PacketHeader packetHeader, Bytes target) {
    super(expiration, packetHeader);
    this.target = target;
  }

  Bytes target() {
    return target;
  }

  @Override
  protected Bytes createPayloadBytes() {
    return RLP.encodeList(writer -> {
      writer.writeValue(target);
      writer.writeLong(expiration());
    });
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FindNeighborsPacket)) {
      return false;
    }
    FindNeighborsPacket that = (FindNeighborsPacket) o;
    return expiration() == that.expiration() && Objects.equal(target, that.target);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(target, expiration());
  }

  @Override
  public String toString() {
    return "FindNeighborsPacket{" + "target=" + target + '}';
  }

  @Override
  protected byte getPacketType() {
    return 0x03;
  }
}
