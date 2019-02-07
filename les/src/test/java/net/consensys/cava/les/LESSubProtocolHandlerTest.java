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

import static net.consensys.cava.les.LESSubprotocol.LES_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.crypto.SECP256K1;
import net.consensys.cava.eth.Address;
import net.consensys.cava.eth.Block;
import net.consensys.cava.eth.BlockBody;
import net.consensys.cava.eth.BlockHeader;
import net.consensys.cava.eth.Hash;
import net.consensys.cava.eth.Transaction;
import net.consensys.cava.eth.repository.BlockchainIndex;
import net.consensys.cava.eth.repository.BlockchainRepository;
import net.consensys.cava.junit.BouncyCastleExtension;
import net.consensys.cava.junit.LuceneIndexWriter;
import net.consensys.cava.junit.LuceneIndexWriterExtension;
import net.consensys.cava.junit.VertxExtension;
import net.consensys.cava.kv.MapKeyValueStore;
import net.consensys.cava.rlpx.RLPxService;
import net.consensys.cava.rlpx.WireConnectionRepository;
import net.consensys.cava.rlpx.wire.DisconnectReason;
import net.consensys.cava.rlpx.wire.SubProtocolIdentifier;
import net.consensys.cava.rlpx.wire.WireConnection;
import net.consensys.cava.units.bigints.UInt256;
import net.consensys.cava.units.ethereum.Gas;
import net.consensys.cava.units.ethereum.Wei;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;

import org.apache.lucene.index.IndexWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith({BouncyCastleExtension.class, VertxExtension.class, LuceneIndexWriterExtension.class})
class LESSubProtocolHandlerTest {

  LESSubProtocolHandlerTest() throws IOException {}

  private static class MyWireConnection implements WireConnection {

    @Override
    public String id() {
      return "abc";
    }
  }

  private static class MyRLPxService implements RLPxService {

    public Bytes message;
    public DisconnectReason disconnectReason;

    @Override
    public AsyncCompletion connectTo(SECP256K1.PublicKey peerPublicKey, InetSocketAddress peerAddress) {
      return null;
    }

    @Override
    public AsyncCompletion start() {
      return null;
    }

    @Override
    public AsyncCompletion stop() {
      return null;
    }

    @Override
    public void send(SubProtocolIdentifier subProtocolIdentifier, int messageType, String connectionId, Bytes message) {
      this.message = message;
    }

    @Override
    public void broadcast(SubProtocolIdentifier subProtocolIdentifier, int messageType, Bytes message) {

    }

    @Override
    public void disconnect(String connectionId, DisconnectReason reason) {
      this.disconnectReason = reason;
    }

    @Override
    public WireConnectionRepository repository() {
      return null;
    }
  }

  private BlockHeader header = new BlockHeader(
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
  private BlockBody body = new BlockBody(
      Collections.singletonList(
          new Transaction(
              UInt256.valueOf(1),
              Wei.valueOf(2),
              Gas.valueOf(2),
              Address.fromBytes(Bytes.random(20)),
              Wei.valueOf(2),
              Bytes.random(12),
              SECP256K1.KeyPair.random())),
      Collections.emptyList());
  private Block block = new Block(header, body);

  @Test
  void sendStatusOnNewConnection(@LuceneIndexWriter IndexWriter writer) throws Exception {

    MyRLPxService service = new MyRLPxService();
    Block block = new Block(header, body);
    BlockchainRepository repo = BlockchainRepository
        .init(
            new MapKeyValueStore(),
            new MapKeyValueStore(),
            new MapKeyValueStore(),
            new BlockchainIndex(writer),
            block)
        .get();
    LESSubProtocolHandler handler = new LESSubProtocolHandler(
        service,
        LES_ID,
        1,
        false,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        repo);
    handler.handleNewPeerConnection("abc").join();
    Thread.sleep(1000);
    StatusMessage message = StatusMessage.read(service.message);
    assertNotNull(message);
    assertEquals(2, message.protocolVersion());
    assertEquals(UInt256.ZERO, message.flowControlBufferLimit());
    assertEquals(block.header().hash().toBytes(), message.genesisHash());
  }

  @Test
  void receiveStatusTwice(@LuceneIndexWriter IndexWriter writer) throws Exception {
    Bytes status = new StatusMessage(
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
        0).toBytes();
    MyRLPxService service = new MyRLPxService();

    BlockchainRepository repo = BlockchainRepository
        .init(
            new MapKeyValueStore(),
            new MapKeyValueStore(),
            new MapKeyValueStore(),
            new BlockchainIndex(writer),
            block)
        .get();
    LESSubProtocolHandler handler = new LESSubProtocolHandler(
        service,
        LES_ID,
        1,
        false,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        repo);
    handler.handleNewPeerConnection("abc").join();
    handler.handle("abc", 0, status).join();
    handler.handle("abc", 0, status).join();

    assertEquals(DisconnectReason.PROTOCOL_BREACH, service.disconnectReason);
  }

  @Test
  void receiveOtherMessageBeforeStatus(@LuceneIndexWriter IndexWriter writer) throws Exception {
    MyRLPxService service = new MyRLPxService();
    BlockchainRepository repo = new BlockchainRepository(
        new MapKeyValueStore(),
        new MapKeyValueStore(),
        new MapKeyValueStore(),
        new BlockchainIndex(writer));
    LESSubProtocolHandler handler = new LESSubProtocolHandler(
        service,
        LES_ID,
        1,
        false,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        repo);
    handler.handle("abc", 2, Bytes.random(2));
    assertEquals(DisconnectReason.PROTOCOL_BREACH, service.disconnectReason);
  }

  @Test
  void receivedGetBlockHeadersMessage(@LuceneIndexWriter IndexWriter writer) throws Exception {
    MyRLPxService service = new MyRLPxService();
    BlockchainRepository repo = BlockchainRepository
        .init(
            new MapKeyValueStore(),
            new MapKeyValueStore(),
            new MapKeyValueStore(),
            new BlockchainIndex(writer),
            block)
        .get();
    LESSubProtocolHandler handler = new LESSubProtocolHandler(
        service,
        LES_ID,
        1,
        false,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        repo);
    Bytes status = new StatusMessage(
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
        0).toBytes();
    handler.handleNewPeerConnection("abc");
    handler.handle("abc", 0, status);

    handler.handle(
        "abc",
        2,
        new GetBlockHeadersMessage(
            1,
            Arrays.asList(
                new GetBlockHeadersMessage.BlockHeaderQuery(
                    Bytes32.random(),
                    UInt256.valueOf(3),
                    UInt256.valueOf(0),
                    GetBlockHeadersMessage.BlockHeaderQuery.Direction.BACKWARDS))).toBytes());
    Bytes received = service.message;
    BlockHeadersMessage messageReceived = BlockHeadersMessage.read(received);
    assertTrue(messageReceived.blockHeaders().isEmpty());
  }

  @Test
  void receivedBlockHeadersMessage(@LuceneIndexWriter IndexWriter writer) throws Exception {
    MyRLPxService service = new MyRLPxService();
    BlockchainRepository repo = BlockchainRepository
        .init(
            new MapKeyValueStore(),
            new MapKeyValueStore(),
            new MapKeyValueStore(),
            new BlockchainIndex(writer),
            block)
        .get();
    LESSubProtocolHandler handler = new LESSubProtocolHandler(
        service,
        LES_ID,
        1,
        false,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        repo);
    Bytes status = new StatusMessage(
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
        0).toBytes();

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

    handler.handleNewPeerConnection("abc").join();
    handler.handle("abc", 0, status).join();

    handler.handle("abc", 3, new BlockHeadersMessage(1, 2, Arrays.asList(header)).toBytes()).join();

    BlockHeader retrieved = repo.retrieveBlockHeader(header.hash()).get();
    assertEquals(header, retrieved);
  }

  @Test
  void receivedGetBlockBodiesMessage(@LuceneIndexWriter IndexWriter writer) throws Exception {
    MyRLPxService service = new MyRLPxService();
    BlockchainRepository repo = BlockchainRepository
        .init(
            new MapKeyValueStore(),
            new MapKeyValueStore(),
            new MapKeyValueStore(),
            new BlockchainIndex(writer),
            block)
        .get();
    LESSubProtocolHandler handler = new LESSubProtocolHandler(
        service,
        LES_ID,
        1,
        false,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        repo);
    Bytes status = new StatusMessage(
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
        0).toBytes();
    handler.handleNewPeerConnection("abc").join();
    handler.handle("abc", 0, status).join();

    handler
        .handle("abc", 4, new GetBlockBodiesMessage(1, Arrays.asList(Hash.fromBytes(Bytes32.random()))).toBytes())
        .join();
    Bytes received = service.message;
    BlockBodiesMessage messageReceived = BlockBodiesMessage.read(received);
    assertTrue(messageReceived.blockBodies().isEmpty());
  }
}
