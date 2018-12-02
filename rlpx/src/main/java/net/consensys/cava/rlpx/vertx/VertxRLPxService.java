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
package net.consensys.cava.rlpx.vertx;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.concurrent.CompletableAsyncCompletion;
import net.consensys.cava.crypto.SECP256K1.KeyPair;
import net.consensys.cava.crypto.SECP256K1.PublicKey;
import net.consensys.cava.rlpx.Capability;
import net.consensys.cava.rlpx.RLPxConnection;
import net.consensys.cava.rlpx.RLPxConnectionFactory;
import net.consensys.cava.rlpx.RLPxService;
import net.consensys.cava.rlpx.ResponderHandshakeMessage;
import net.consensys.cava.rlpx.WireConnection;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;

public final class VertxRLPxService implements RLPxService {

  private final Vertx vertx;
  private final int listenPort;
  private final KeyPair keyPair;
  private final List<Capability> capabilities;

  private NetClient client;
  private NetServer server;

  private final List<WireConnection> wireConnections = new ArrayList<>();

  public VertxRLPxService(Vertx vertx, int listenPort, KeyPair identityKeyPair, List<Capability> capabilities) {
    if (listenPort < 0 || listenPort > 65536) {
      throw new IllegalArgumentException("Invalid port: " + listenPort);
    }
    this.vertx = vertx;
    this.listenPort = listenPort;
    this.keyPair = identityKeyPair;
    this.capabilities = capabilities;
  }

  public AsyncCompletion start() {
    client = vertx.createNetClient(new NetClientOptions());
    server = vertx.createNetServer(new NetServerOptions().setPort(listenPort)).connectHandler(this::receiveMessage);
    CompletableAsyncCompletion complete = AsyncCompletion.incomplete();
    server.listen(res -> {
      if (res.succeeded()) {
        complete.complete();
      } else {
        complete.completeExceptionally(res.cause());
      }
    });
    return complete;
  }

  private void receiveMessage(NetSocket netSocket) {
    netSocket.handler(new Handler<Buffer>() {

      private RLPxConnection conn;

      private WireConnection wireConnection;

      public void handle(Buffer buffer) {
        if (conn == null) {
          conn = RLPxConnectionFactory.respondToHandshake(
              Bytes.wrapBuffer(buffer),
              keyPair.secretKey(),
              bytes -> netSocket.write(Buffer.buffer(bytes.toArrayUnsafe())));
          if (wireConnection == null) {
            wireConnection = new WireConnection(
                message -> netSocket.write(Buffer.buffer(conn.write(message).toArrayUnsafe())),
                netSocket::end,
                capabilities);
            wireConnections.add(wireConnection);
          }
        } else {
          wireConnection.messageReceived(conn.read(Bytes.wrapBuffer(buffer)));
        }
      }
    });
  }

  public void stop() {
    if (client == null) {
      throw new IllegalStateException("The service was not started");
    }
    for (WireConnection conn : wireConnections) {
      conn.disconnect();
    }
    client.close();
    server.close();
  }

  public int actualPort() {
    return server.actualPort();
  }

  public void connectTo(PublicKey peerPublicKey, InetSocketAddress peerAddress) {
    client.connect(
        peerAddress.getPort(),
        peerAddress.getHostString(),
        netSocketFuture -> netSocketFuture.map(netSocket -> {
          Bytes32 nonce = RLPxConnectionFactory.createRandomHash();
          KeyPair ephemeralKeyPair = KeyPair.random();
          Bytes initHandshakeMessage = RLPxConnectionFactory.init(keyPair, peerPublicKey, ephemeralKeyPair, nonce);
          netSocket.write(Buffer.buffer(initHandshakeMessage.toArrayUnsafe()));

          netSocket.handler(new Handler<Buffer>() {

            private RLPxConnection conn;

            private WireConnection wireConnection;

            public void handle(Buffer buffer) {
              if (conn == null) {
                Bytes responseBytes = Bytes.wrapBuffer(buffer);
                ResponderHandshakeMessage responseMessage =
                    RLPxConnectionFactory.readResponse(responseBytes, keyPair.secretKey());
                conn = RLPxConnectionFactory.createConnection(
                    true,
                    initHandshakeMessage,
                    responseBytes,
                    ephemeralKeyPair.secretKey(),
                    responseMessage.ephemeralPublicKey(),
                    nonce,
                    responseMessage.nonce());
                wireConnection = new WireConnection(
                    message -> netSocket.write(Buffer.buffer(conn.write(message).toArrayUnsafe())),
                    netSocket::end,
                    capabilities);
                wireConnections.add(wireConnection);
              } else {
                wireConnection.messageReceived(conn.read(Bytes.wrapBuffer(buffer)));
              }
            }
          });
          return null;
        }));
  }

  List<WireConnection> getWireConnections() {
    return wireConnections;
  }
}
