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
package net.consensys.cava.scuttlebutt.feed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.scuttlebutt.Identity;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;

import org.junit.jupiter.api.Test;

class FeedMessageTest {

  private LinkedHashMap<String, Object> singletonMap(String key, Object value) {
    LinkedHashMap<String, Object> map = new LinkedHashMap<>();
    map.put(key, value);
    return map;
  }

  @Test
  void noTypeKey() {
    assertThrows(IllegalArgumentException.class, () -> {
      new FeedMessage(null, Identity.random(), new LinkedHashMap<>());
    });
  }

  @Test
  void withPrevious() {
    Identity author = Identity.random();
    SignedFeedMessage msg = new SignedFeedMessage(null, author, singletonMap("type", "post"));
    SignedFeedMessage childMsg = new SignedFeedMessage(msg, author, singletonMap("type", "post2"));
    assertTrue(childMsg.verify());
    assertEquals(2, childMsg.sequence());

  }

  @Test
  void signAndVerify() {
    Identity author = Identity.random();
    SignedFeedMessage msg = new SignedFeedMessage(null, author, singletonMap("type", "post"));
    assertTrue(msg.verify());
  }

  @Test
  void verifyNotMatching() {
    Identity author = Identity.random();
    SignedFeedMessage msg = new SignedFeedMessage(null, author, 33, singletonMap("type", "post"), "wontmatch");
    assertFalse(msg.verify());
  }

  @Test
  void prettyJsonMatching() throws Exception {
    // @formatter:off
        String json = "{\n" +
                "  \"previous\": \"%XphMUkWQtomKjXQvFGfsGYpt69sgEY7Y4Vou9cEuJho=.sha256\",\n" +
                "  \"author\": \"@FCX/tsDLpubCPKKfIrw4gc+SQkHcaD17s7GI6i/ziWY=.ed25519\",\n" +
                "  \"sequence\": 2,\n" +
                "  \"timestamp\": 1514517078157,\n" +
                "  \"hash\": \"sha256\",\n" +
                "  \"content\": {\n" +
                "    \"type\": \"post\",\n" +
                "    \"text\": \"Second post!\",\n" +
                "    \"emptyArray\": [],\n" +
                "    \"emptyObject\": {},\n" +
                "    \"arrayContents\": [\n" +
                "      \"apple\",\n" +
                "      \"pear\"\n" +
                "    ],\n" +
                "    \"objectContents\": {\n" +
                "      \"ids\": [\n" +
                "        1,\n" +
                "        2,\n" +
                "        3\n" +
                "      ],\n" +
                "      \"name\": \"bob\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        // @formatter:on
    LinkedHashMap<String, Object> objectContents = new LinkedHashMap<>();
    objectContents.put("ids", Arrays.asList(1, 2, 3));
    objectContents.put("name", "bob");
    LinkedHashMap<String, Object> content = new LinkedHashMap<>();
    content.put("type", "post");
    content.put("text", "Second post!");
    content.put("emptyArray", Collections.emptyList());
    content.put("emptyObject", Collections.emptyMap());
    content.put("arrayContents", Arrays.asList("apple", "pear"));
    content.put("objectContents", objectContents);
    FeedMessage message = new FeedMessage(
        "%XphMUkWQtomKjXQvFGfsGYpt69sgEY7Y4Vou9cEuJho=.sha256",
        2,
        Identity.fromCanonicalForm("@FCX/tsDLpubCPKKfIrw4gc+SQkHcaD17s7GI6i/ziWY=.ed25519"),
        1514517078157L,
        content);
    String encoded =
        FeedMessage.mapper.writerFor(FeedMessage.class).withDefaultPrettyPrinter().writeValueAsString(message);
    assertEquals(json, encoded);
  }
}
