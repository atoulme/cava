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

import net.consensys.cava.eth.repository.BlockchainIndex;
import net.consensys.cava.eth.repository.BlockchainRepository;
import net.consensys.cava.kv.KeyValueStore;
import net.consensys.cava.rlpx.RLPxService;
import net.consensys.cava.rlpx.wire.SubProtocol;
import net.consensys.cava.rlpx.wire.SubProtocolHandler;
import net.consensys.cava.rlpx.wire.SubProtocolIdentifier;
import net.consensys.cava.units.bigints.UInt256;

/**
 * The LES subprotocol entry point class, to be used in conjunction with RLPxService
 * <p>
 * This subprotocol is implemented after the specification presented on the *
 * <a href="https://github.com/ethereum/wiki/wiki/Light-client-protocol">Ethereum wiki.</a>
 *
 * @see net.consensys.cava.rlpx.RLPxService
 *
 */
public final class LESSubprotocol implements SubProtocol {

  static final SubProtocolIdentifier LES_ID = SubProtocolIdentifier.of("les", 2);

  private final int networkId;

  private final KeyValueStore blockStore;
  private final KeyValueStore blockHeaderStore;
  private final KeyValueStore chainMetadataStore;

  private final boolean serveHeaders;
  private final UInt256 serveChainSince;
  private final UInt256 serveStateSince;
  private final UInt256 flowControlBufferLimit;
  private final UInt256 flowControlMaximumRequestCostTable;
  private final UInt256 flowControlMinimumRateOfRecharge;
  private final BlockchainRepository repo;


  /**
   * Default constructor.
   * 
   * @param networkId the identifier, as an integer of the chain to connect to. 0 for testnet, 1 for mainnet.
   * @param blockStore the key-value store for blocks
   * @param blockHeaderStore the key-value store for block headers
   */
  public LESSubprotocol(
      int networkId,
      boolean serveHeaders,
      UInt256 serveChainSince,
      UInt256 serveStateSince,
      UInt256 flowControlBufferLimit,
      UInt256 flowControlMaximumRequestCostTable,
      UInt256 flowControlMinimumRateOfRecharge,
      KeyValueStore blockStore,
      KeyValueStore blockHeaderStore,
      KeyValueStore chainMetadataStore,
      BlockchainIndex blockchainIndex) {
    this.networkId = networkId;
    this.blockStore = blockStore;
    this.blockHeaderStore = blockHeaderStore;
    this.chainMetadataStore = chainMetadataStore;
    this.repo = new BlockchainRepository(blockStore, blockHeaderStore, chainMetadataStore, blockchainIndex);
    this.serveHeaders = serveHeaders;
    this.serveChainSince = serveChainSince;
    this.serveStateSince = serveStateSince;
    this.flowControlBufferLimit = flowControlBufferLimit;
    this.flowControlMinimumRateOfRecharge = flowControlMinimumRateOfRecharge;
    this.flowControlMaximumRequestCostTable = flowControlMaximumRequestCostTable;
  }

  @Override
  public SubProtocolIdentifier id() {
    return LES_ID;
  }

  @Override
  public boolean supports(SubProtocolIdentifier subProtocolIdentifier) {
    return "les".equals(subProtocolIdentifier.name()) && subProtocolIdentifier.version() == 2;
  }

  @Override
  public int versionRange(int version) {
    return 21;
  }

  @Override
  public SubProtocolHandler createHandler(RLPxService service) {
    return new LESSubProtocolHandler(
        service,
        LES_ID,
        networkId,
        serveHeaders,
        serveChainSince,
        serveStateSince,
        flowControlBufferLimit,
        flowControlMaximumRequestCostTable,
        flowControlMinimumRateOfRecharge,
        repo);
  }
}
