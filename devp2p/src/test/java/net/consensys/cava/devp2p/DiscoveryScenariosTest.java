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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.crypto.SECP256K1.KeyPair;
import net.consensys.cava.junit.BouncyCastleExtension;
import net.consensys.cava.junit.VertxExtension;
import net.consensys.cava.junit.VertxInstance;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.logl.Level;
import org.logl.Logger;
import org.logl.LoggerProvider;
import org.logl.logl.SimpleLogger;

@ExtendWith({BouncyCastleExtension.class, VertxExtension.class})
class DiscoveryScenariosTest {

  private static final LoggerProvider loggerProvider = SimpleLogger.withLogLevel(Level.DEBUG).toPrintWriter(
      new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out, UTF_8)), true));

  private static final Logger logger = loggerProvider.getLogger("service");
  private static final Logger otherLogger = loggerProvider.getLogger("otherService");


  @Test
  void discoverOnePeer(@VertxInstance Vertx vertx) throws Exception {
    PeerRepository peerRepository = new PeerRepository();
    InetSocketAddress address = new InetSocketAddress(30303);
    InetSocketAddress otherAddress = new InetSocketAddress(30304);
    KeyPair keyPair = KeyPair.random();
    VertxDiscoveryService service = new VertxDiscoveryService(
        vertx,
        peerRepository,
        Collections.emptyList(),
        address,
        address,
        new SimplePeerRoutingTable(),
        keyPair,
        30303,
        logger);

    PeerRepository otherPeerRepository = new PeerRepository();

    VertxDiscoveryService otherService = new VertxDiscoveryService(
        vertx,
        otherPeerRepository,
        Collections.singletonList(
            "enode://" + keyPair.getPublicKey().encodedBytes().toHexString().substring(2) + "@127.0.0.1:30303"),
        otherAddress,
        otherAddress,
        new SimplePeerRoutingTable(),
        KeyPair.random(),
        30304,
        otherLogger);

    List<Peer> activePeers = new ArrayList<>();
    peerRepository.observePeerActive(activePeers::add);
    service.start().join();
    otherService.start().join();
    try {
      Thread.sleep(5000);
    } finally {
      service.stop().join();
      otherService.stop().join();
    }
    assertEquals(1, activePeers.size());
    assertEquals("0.0.0.0", activePeers.get(0).endpoint().address());
    assertEquals(30304, activePeers.get(0).endpoint().tcpPort());
    assertEquals(30304, activePeers.get(0).endpoint().udpPort());
  }
}
