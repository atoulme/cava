/*
 * Copyright 2018, ConsenSys Inc.
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
package net.consensys.cava.net.tls;

import static net.consensys.cava.crypto.Hash.sha2_256;
import static net.consensys.cava.net.tls.SecurityTestUtils.DUMMY_FINGERPRINT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.cava.junit.VertxExtension;
import net.consensys.cava.junit.VertxInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import io.netty.util.internal.StringUtil;
import io.vertx.core.Vertx;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.SelfSignedCertificate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
@ExtendWith(VertxExtension.class)
class ServerTofaTest {

  private static String caFingerprint;
  private static HttpClient caClient;
  private static String fooFingerprint;
  private static HttpClient fooClient;
  private static HttpClient foobarClient;

  private Path knownClientsFile;
  private HttpServer httpServer;

  @BeforeAll
  static void setupClients(@TempDirectory Path tempDir, @VertxInstance Vertx vertx) throws Exception {
    SelfSignedCertificate caClientCert = SelfSignedCertificate.create("example.com");
    caFingerprint = StringUtil
        .toHexStringPadded(sha2_256(SecurityTestUtils.loadPEM(Paths.get(caClientCert.keyCertOptions().getCertPath()))));
    SecurityTestUtils.configureJDKTrustStore(tempDir, caClientCert);
    caClient = vertx.createHttpClient(
        new HttpClientOptions().setTrustOptions(InsecureTrustOptions.INSTANCE).setSsl(true).setKeyCertOptions(
            caClientCert.keyCertOptions()));

    SelfSignedCertificate fooCert = SelfSignedCertificate.create("foo.com");
    fooFingerprint = StringUtil
        .toHexStringPadded(sha2_256(SecurityTestUtils.loadPEM(Paths.get(fooCert.keyCertOptions().getCertPath()))));
    HttpClientOptions fooClientOptions = new HttpClientOptions();
    fooClientOptions
        .setSsl(true)
        .setKeyCertOptions(fooCert.keyCertOptions())
        .setTrustOptions(InsecureTrustOptions.INSTANCE)
        .setConnectTimeout(1500)
        .setReuseAddress(true)
        .setReusePort(true);
    fooClient = vertx.createHttpClient(fooClientOptions);

    SelfSignedCertificate foobarCert = SelfSignedCertificate.create("foobar.com");
    HttpClientOptions foobarClientOptions = new HttpClientOptions();
    foobarClientOptions
        .setSsl(true)
        .setKeyCertOptions(foobarCert.keyCertOptions())
        .setTrustOptions(InsecureTrustOptions.INSTANCE)
        .setConnectTimeout(1500)
        .setReuseAddress(true)
        .setReusePort(true);
    foobarClient = vertx.createHttpClient(foobarClientOptions);
  }

  @BeforeEach
  void startServer(@TempDirectory Path tempDir, @VertxInstance Vertx vertx) throws Exception {
    knownClientsFile = tempDir.resolve("known-clients.txt");
    Files.write(knownClientsFile, Arrays.asList("#First line", "foobar.com " + DUMMY_FINGERPRINT));

    SelfSignedCertificate serverCert = SelfSignedCertificate.create();
    HttpServerOptions options = new HttpServerOptions();
    options
        .setSsl(true)
        .setClientAuth(ClientAuth.REQUIRED)
        .setPemKeyCertOptions(serverCert.keyCertOptions())
        .setTrustOptions(VertxTrustOptions.trustClientOnFirstAccess(knownClientsFile, false))
        .setIdleTimeout(1500)
        .setReuseAddress(true)
        .setReusePort(true);
    httpServer = vertx.createHttpServer(options);
    SecurityTestUtils.configureAndStartTestServer(httpServer);
  }

  @AfterEach
  void stopServer() {
    httpServer.close();
  }

  @AfterAll
  static void cleanupClients() {
    caClient.close();
    fooClient.close();
    foobarClient.close();
  }

  @Test
  void shouldNotValidateUsingCertificate() throws Exception {
    HttpClientRequest req = caClient.get(httpServer.actualPort(), "localhost", "/upcheck");
    CompletableFuture<HttpClientResponse> respFuture = new CompletableFuture<>();
    req.handler(respFuture::complete).exceptionHandler(respFuture::completeExceptionally).end();
    HttpClientResponse resp = respFuture.join();
    assertEquals(200, resp.statusCode());

    List<String> knownClients = Files.readAllLines(knownClientsFile);
    assertEquals(3, knownClients.size());
    assertEquals("#First line", knownClients.get(0));
    assertEquals("foobar.com " + DUMMY_FINGERPRINT, knownClients.get(1));
    assertEquals("example.com " + caFingerprint, knownClients.get(2));
  }

  @Test
  void shouldValidateOnFirstUse() throws Exception {
    HttpClientRequest req = fooClient.get(httpServer.actualPort(), "localhost", "/upcheck");
    CompletableFuture<HttpClientResponse> respFuture = new CompletableFuture<>();
    req.handler(respFuture::complete).exceptionHandler(respFuture::completeExceptionally).end();
    HttpClientResponse resp = respFuture.join();
    assertEquals(200, resp.statusCode());

    List<String> knownClients = Files.readAllLines(knownClientsFile);
    assertEquals(3, knownClients.size());
    assertEquals("#First line", knownClients.get(0));
    assertEquals("foobar.com " + DUMMY_FINGERPRINT, knownClients.get(1));
    assertEquals("foo.com " + fooFingerprint, knownClients.get(2));
  }

  @Test
  void shouldRejectDifferentCertificate() {
    HttpClientRequest req = foobarClient.get(httpServer.actualPort(), "localhost", "/upcheck");
    CompletableFuture<HttpClientResponse> respFuture = new CompletableFuture<>();
    req.handler(respFuture::complete).exceptionHandler(respFuture::completeExceptionally).end();
    Throwable e = assertThrows(CompletionException.class, respFuture::join);
    e = e.getCause().getCause();
    assertTrue(e.getMessage().contains("certificate_unknown"));
  }
}
