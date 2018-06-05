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

import java.util.List;

abstract class Packet {

  static PingPacket createPing(Endpoint from, Endpoint to, long expiration, KeyPair keyPair) {
    return new PingPacket(expiration, keyPair, from, to);
  }

  static PongPacket createPong(Endpoint to, Bytes pingHash, long expiration, KeyPair keyPair) {
    return new PongPacket(expiration, keyPair, to, pingHash);
  }

  static FindNeighborsPacket createFindNeighbors(Bytes target, long expiration, KeyPair keyPair) {
    return new FindNeighborsPacket(expiration, keyPair, target);
  }

  static NeighborsPacket createNeighbors(List<NeighborsPacket.Neighbor> neighbors, long expiration, KeyPair keyPair) {
    return new NeighborsPacket(expiration, keyPair, neighbors);
  }

  private PacketHeader header;
  private Bytes bytes;
  private KeyPair keyPair;
  private final long expiration;
  private Bytes cachedBytes;

  long expiration() {
    return expiration;
  }

  protected abstract Bytes createPayloadBytes();

  Bytes payloadBytes() {
    if (cachedBytes == null) {
      cachedBytes = createPayloadBytes();
    }
    return cachedBytes;
  }

  Packet(long expiration, KeyPair keyPair) {
    this.expiration = expiration;
    this.keyPair = keyPair;
  }

  Packet(long expiration, PacketHeader packetHeader) {
    this.expiration = expiration;
    this.header = packetHeader;
  }

  PacketHeader header() {
    PacketHeader header = this.header;
    if (header == null) {
      header = new PacketHeader(keyPair, getPacketType(), payloadBytes());
      this.header = header;
    }
    return header;
  }

  protected abstract byte getPacketType();

  Bytes toBytes() {
    Bytes packetBytes = bytes;
    if (packetBytes == null) {
      packetBytes = Bytes.concatenate(
          header().hash(),
          header().signature().encodedBytes(),
          Bytes.of(header().packetType()),
          payloadBytes());
      bytes = packetBytes;
    }
    return packetBytes;
  }

  @Override
  public String toString() {
    return "Packet{" + "packetType=" + getPacketType() + "}";
  }
}
