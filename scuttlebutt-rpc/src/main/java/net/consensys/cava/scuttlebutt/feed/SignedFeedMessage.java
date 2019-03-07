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

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.crypto.Hash;
import net.consensys.cava.io.Base64;
import net.consensys.cava.scuttlebutt.Identity;

import java.util.LinkedHashMap;
import java.util.Objects;
import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;

@JsonPropertyOrder({"previous", "author", "sequence", "timestamp", "hash", "content", "signature"})
public class SignedFeedMessage extends FeedMessage {

  private String signature;

  public SignedFeedMessage(
      @Nullable SignedFeedMessage previous,
      Identity author,
      long timestamp,
      LinkedHashMap<String, Object> content,
      @Nullable String signature) {
    super(previous, author, timestamp, content);
    this.signature = signature;
  }

  public SignedFeedMessage(
      @Nullable SignedFeedMessage previous,
      Identity author,
      LinkedHashMap<String, Object> content) {
    super(previous, author, content);
    this.signature = computeSignature();
  }

  protected String messageId() {
    return "%" + Base64.encode(Hash.sha2_256(json())) + ".sha256";
  }

  protected Bytes json() {
    try {
      return Bytes.wrap(mapper.writerFor(SignedFeedMessage.class).writeValueAsBytes(this));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @JsonProperty
  public String signature() {
    if (signature != null) {
      return signature;
    }
    this.signature = computeSignature();
    return signature;
  }

  private String computeSignature() {
    try {
      byte[] data = mapper.writerFor(FeedMessage.class).withDefaultPrettyPrinter().writeValueAsBytes(this);
      return Base64.encode(author.sign(Bytes.wrap(data))) + ".sig.ed25519";
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean verify() {
    return Objects.equals(signature, computeSignature());
  }
}
