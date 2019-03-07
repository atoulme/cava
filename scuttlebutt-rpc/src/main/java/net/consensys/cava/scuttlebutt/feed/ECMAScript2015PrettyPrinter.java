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

import java.io.IOException;
import java.util.Arrays;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.PrettyPrinter;

final class ECMAScript2015PrettyPrinter implements PrettyPrinter {

  private int indent = 0;

  @Override
  public void writeRootValueSeparator(JsonGenerator g) throws IOException {
    g.writeRaw('\n');
  }

  @Override
  public void writeStartObject(JsonGenerator g) throws IOException {
    g.writeRaw('{');
    indent++;
  }

  @Override
  public void beforeObjectEntries(JsonGenerator g) throws IOException {
    g.writeRaw('\n');
    writeIndent(g);
  }

  /**
   * Method called after an object field has been output, but before the value is output.
   * <p>
   * Default handling will just output a single colon to separate the two, without additional spaces.
   */
  @Override
  public void writeObjectFieldValueSeparator(JsonGenerator g) throws IOException {
    g.writeRaw(": ");
  }

  /**
   * Method called after an object entry (field:value) has been completely output, and before another value is to be
   * output.
   * <p>
   * Default handling (without pretty-printing) will output a single comma to separate the two.
   */
  @Override
  public void writeObjectEntrySeparator(JsonGenerator g) throws IOException {
    g.writeRaw(",\n");
    writeIndent(g);
  }

  @Override
  public void writeEndObject(JsonGenerator g, int nrOfEntries) throws IOException {
    indent--;
    if (nrOfEntries > 0) {
      g.writeRaw('\n');
      writeIndent(g);
    }
    g.writeRaw('}');
  }

  @Override
  public void writeStartArray(JsonGenerator g) throws IOException {
    g.writeRaw('[');
    indent++;
  }

  @Override
  public void beforeArrayValues(JsonGenerator g) throws IOException {
    g.writeRaw('\n');
    writeIndent(g);
  }

  @Override
  public void writeArrayValueSeparator(JsonGenerator g) throws IOException {
    g.writeRaw(",\n");
    writeIndent(g);
  }

  @Override
  public void writeEndArray(JsonGenerator g, int nrOfValues) throws IOException {
    indent--;
    if (nrOfValues > 0) {
      g.writeRaw('\n');
      writeIndent(g);
    }
    g.writeRaw(']');
  }

  private void writeIndent(JsonGenerator g) throws IOException {
    char[] spaces = new char[indent * 2];
    Arrays.fill(spaces, ' ');
    g.writeRaw(spaces, 0, spaces.length);
  }
}
