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

import net.consensys.cava.crypto.SECP256K1.KeyPair;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.security.Security;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import io.vertx.core.Vertx;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.logl.Level;
import org.logl.Logger;
import org.logl.logl.SimpleLogger;

public class SimpleDiscoveryShowcaseApp {

  public static void main(String[] args) throws Exception {
    Security.addProvider(new BouncyCastleProvider());
    Vertx vertx = Vertx.vertx();

    PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out, UTF_8)), true);
    Logger logger = SimpleLogger.withLogLevel(Level.DEBUG).toPrintWriter(writer).getLogger("app");

    PeerRepository peerRepository = new PeerRepository();
    List<Peer> activePeers = new ArrayList<>();
    peerRepository.observePeerActive(activePeers::add);
    List<String> bootstrapPeers = new ArrayList<>();
    bootstrapPeers.add(
        "enode://eed65ccfbfcfb996a4bd1c7a1df159d305e740c29337ffc61de73bd65f4b388029f490b1c695fced65221c18e1bb8b3dde4988875256f33d16526a45684cca36@10.120.59.49:30305");
    InetSocketAddress address = new InetSocketAddress(30303);
    VertxDiscoveryService service = new VertxDiscoveryService(
        vertx,
        peerRepository,
        bootstrapPeers,
        new InetSocketAddress(30303),
        address,
        new SimplePeerRoutingTable(),
        KeyPair.random(),
        30303,
        logger,
        () -> Instant.now().toEpochMilli());
    service.start();
    while (true) {
      Thread.sleep(5000);
      System.out.println("Peers as of " + System.currentTimeMillis());
      for (Peer peer : activePeers) {
        System.out.println(peer);
      }
    }
  }

}
