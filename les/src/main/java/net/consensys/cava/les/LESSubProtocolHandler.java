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
import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.eth.BlockBody;
import net.consensys.cava.eth.BlockHeader;
import net.consensys.cava.eth.Hash;
import net.consensys.cava.eth.repository.BlockchainRepository;
import net.consensys.cava.rlpx.RLPxService;
import net.consensys.cava.rlpx.wire.DisconnectReason;
import net.consensys.cava.rlpx.wire.SubProtocolHandler;
import net.consensys.cava.rlpx.wire.SubProtocolIdentifier;
import net.consensys.cava.units.bigints.UInt256;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

final class LESSubProtocolHandler implements SubProtocolHandler {

  private final RLPxService service;
  private final SubProtocolIdentifier subProtocolIdentifier;
  private final int networkId;
  private final BlockchainRepository repo;
  private final boolean serveHeaders;
  private final UInt256 serveChainSince;
  private final UInt256 serveStateSince;
  private final UInt256 flowControlBufferLimit;
  private final UInt256 flowControlMaximumRequestCostTable;
  private final UInt256 flowControlMinimumRateOfRecharge;

  private final Map<String, LESPeerState> peerStateMap = new ConcurrentHashMap<>();

  LESSubProtocolHandler(
      RLPxService service,
      SubProtocolIdentifier subProtocolIdentifier,
      int networkId,
      boolean serveHeaders,
      UInt256 serveChainSince,
      UInt256 serveStateSince,
      UInt256 flowControlBufferLimit,
      UInt256 flowControlMaximumRequestCostTable,
      UInt256 flowControlMinimumRateOfRecharge,
      BlockchainRepository repository) {
    this.service = service;
    this.subProtocolIdentifier = subProtocolIdentifier;
    this.networkId = networkId;
    this.repo = repository;
    this.serveHeaders = serveHeaders;
    this.serveChainSince = serveChainSince;
    this.serveStateSince = serveStateSince;
    this.flowControlBufferLimit = flowControlBufferLimit;
    this.flowControlMaximumRequestCostTable = flowControlMaximumRequestCostTable;
    this.flowControlMinimumRateOfRecharge = flowControlMinimumRateOfRecharge;
  }

  @Override
  public AsyncCompletion handle(String connectionId, int messageType, Bytes message) {
    LESPeerState state = peerStateMap.computeIfAbsent(connectionId, (connId) -> new LESPeerState());
    if (messageType == 0) {
      if (state.handshakeComplete()) {
        service.disconnect(connectionId, DisconnectReason.PROTOCOL_BREACH);
        return AsyncCompletion.completed();
      }
      state.setPeerStatusMessage(StatusMessage.read(message));
      return AsyncCompletion.completed();
    } else {
      if (!state.handshakeComplete()) {
        service.disconnect(connectionId, DisconnectReason.PROTOCOL_BREACH);
        return AsyncCompletion.completed();
      }
      if (messageType == 1) {
        throw new UnsupportedOperationException();
      } else if (messageType == 2) {
        GetBlockHeadersMessage getBlockHeadersMessage = GetBlockHeadersMessage.read(message);
        return handleGetBlockHeaders(connectionId, getBlockHeadersMessage);
      } else if (messageType == 3) {
        BlockHeadersMessage blockHeadersMessage = BlockHeadersMessage.read(message);
        return handleBlockHeadersMessage(blockHeadersMessage);
      } else if (messageType == 4) {
        GetBlockBodiesMessage blockBodiesMessage = GetBlockBodiesMessage.read(message);
        return handleGetBlockBodiesMessage(connectionId, blockBodiesMessage);
      } else {
        throw new UnsupportedOperationException();
      }
    }

  }

  private AsyncCompletion handleGetBlockBodiesMessage(String connectionId, GetBlockBodiesMessage blockBodiesMessage) {
    List<BlockBody> bodies = new ArrayList<>();
    List<AsyncCompletion> retrievals = new ArrayList<>();
    for (Hash blockHash : blockBodiesMessage.blockHashes()) {
      retrievals.add(repo.retrieveBlock(blockHash).thenAccept(block -> {
        if (block != null) {
          bodies.add(block.body());
        }
      }));
    }
    return AsyncCompletion.allOf(retrievals).thenRun(
        () -> service.send(
            subProtocolIdentifier,
            5,
            connectionId,
            new BlockBodiesMessage(blockBodiesMessage.reqID(), 0, bodies).toBytes()));
  }

  private AsyncCompletion handleBlockHeadersMessage(BlockHeadersMessage blockHeadersMessage) {
    List<AsyncCompletion> completions = new ArrayList<>();
    for (BlockHeader header : blockHeadersMessage.blockHeaders()) {
      completions.add(repo.storeBlockHeader(header));
    }
    return AsyncCompletion.allOf(completions);
  }

  private AsyncCompletion handleGetBlockHeaders(String connectionId, GetBlockHeadersMessage getBlockHeadersMessage) {
    Set<BlockHeader> headersFound = new TreeSet<>();
    List<AsyncCompletion> retrievals = new ArrayList<>();
    for (GetBlockHeadersMessage.BlockHeaderQuery query : getBlockHeadersMessage.queries()) {
      List<Hash> hashes = repo.findBlockByHashOrNumber(query.blockNumberOrBlockHash());
      for (Hash h : hashes) {
        retrievals.add(repo.retrieveBlockHeader(h).thenAccept(header -> {
          if (header != null) {
            headersFound.add(header);
          }
        }));
      }
    }
    return AsyncCompletion.allOf(retrievals).thenRun(
        () -> service.send(
            subProtocolIdentifier,
            3,
            connectionId,
            new BlockHeadersMessage(getBlockHeadersMessage.reqID(), 0L, new ArrayList<>(headersFound)).toBytes()));


  }

  @Override
  public AsyncCompletion handleNewPeerConnection(String connectionId) {
    return repo.retrieveChainHead().thenAcceptBoth(repo.retrieveGenesisBlock(), (head, genesis) -> {
      UInt256 headTd = head.header().difficulty();
      Hash headHash = head.header().hash();
      LESPeerState state = peerStateMap.computeIfAbsent(connectionId, (connId) -> new LESPeerState());
      state.setOurStatusMessage(
          new StatusMessage(
              subProtocolIdentifier.version(),
              networkId,
              headTd,
              headHash.toBytes(),
              head.header().number(),
              genesis.header().hash().toBytes(),
              serveHeaders,
              serveChainSince,
              serveStateSince,
              false,
              flowControlBufferLimit,
              flowControlMaximumRequestCostTable,
              flowControlMinimumRateOfRecharge,
              0));
      service.send(subProtocolIdentifier, 0, connectionId, state.ourStatusMessage().toBytes());
    });
  }

  @Override
  public AsyncCompletion stop() {
    return AsyncCompletion.completed();
  }
}
