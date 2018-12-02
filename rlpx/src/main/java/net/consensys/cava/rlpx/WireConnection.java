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
package net.consensys.cava.rlpx;

import net.consensys.cava.bytes.Bytes;

import java.util.List;
import java.util.function.Consumer;

public class WireConnection {

  private final Consumer<WireProtocolMessage> writer;
  private final Runnable disconnectHandler;
  private final List<Capability> capabilities;

  private HelloMessage myHelloMessage;
  private HelloMessage peerHelloMessage;

  public WireConnection(
      Consumer<WireProtocolMessage> writer,
      Runnable disconnectHandler,
      List<Capability> capabilities) {
    this.writer = writer;
    this.disconnectHandler = disconnectHandler;
    this.capabilities = capabilities;
  }

  public void messageReceived(WireProtocolMessage message) {
    if (message.type() == 0) {
      peerHelloMessage = (HelloMessage) message;
      if (myHelloMessage == null) {
        sendHello();
      }
    } else if (message.type() == 1) {
      disconnectHandler.run();
    }
  }

  public void disconnect() {
    writer.accept(new DisconnectMessage(8)); // TODO reason hardcoded for now.
  }

  void sendHello() {
    myHelloMessage = new HelloMessage(Bytes.of(1), 0, "abc", 1, capabilities);
    writer.accept(myHelloMessage);
  }
}
