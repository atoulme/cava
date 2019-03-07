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

import static com.google.common.base.Preconditions.checkArgument;

import net.consensys.cava.scuttlebutt.Identity;

import java.time.Instant;
import java.util.LinkedHashMap;
import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A message in a Secure Scuttlebutt feed.
 *
 * Messages are chained, except for the first one.
 */
@JsonPropertyOrder({"previous", "author", "sequence", "timestamp", "hash", "content"})
public class FeedMessage {

  final static ObjectMapper mapper = new ObjectMapper().setDefaultPrettyPrinter(new ECMAScript2015PrettyPrinter());

  private final String previous;

  private final long sequence;

  private final long timestamp;

  final Identity author;

  private final LinkedHashMap<String, Object> content;

  public FeedMessage(@Nullable SignedFeedMessage previous, Identity author, LinkedHashMap<String, Object> content) {
    this(previous, author, Instant.now().toEpochMilli(), content);
  }

  public FeedMessage(
      @Nullable SignedFeedMessage previous,
      Identity author,
      long timestamp,
      LinkedHashMap<String, Object> content) {
    this(
        previous != null ? previous.messageId() : null,
        previous != null ? previous.sequence() + 1 : 1,
        author,
        timestamp,
        content);
  }

  public FeedMessage(
      @Nullable String previous,
      long sequence,
      Identity author,
      long timestamp,
      LinkedHashMap<String, Object> content) {
    checkArgument(content.get("type") != null, "Content must have a type key");
    if (previous != null) {
      checkArgument(sequence > 1, "Sequence must be greater than 1");
    } else {
      checkArgument(sequence == 1L, "Sequence must equal 1 if no previous message is present");
    }
    this.previous = previous;
    this.sequence = sequence;
    this.author = author;
    this.content = content;
    this.timestamp = timestamp;
  }

  @JsonProperty
  public String previous() {
    return previous;
  }

  @JsonProperty
  public String author() {
    return author.toCanonicalForm();
  }

  @JsonProperty
  public long sequence() {
    return sequence;
  }

  @JsonProperty
  public long timestamp() {
    return timestamp;
  }

  @JsonProperty
  public String hash() {
    return "sha256";
  }

  @JsonProperty
  public LinkedHashMap<String, Object> content() {
    return content;
  }


}
