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
package net.consensys.cava.io;

import static net.consensys.cava.io.Streams.enumerationStream;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class StreamsTest {

  @Test
  void shouldStreamAnEnumeration() {
    Enumeration<String> enumeration = Collections.enumeration(Arrays.asList("RED", "BLUE", "GREEN"));
    List<String> result = enumerationStream(enumeration).map(String::toLowerCase).collect(Collectors.toList());
    assertEquals(Arrays.asList("red", "blue", "green"), result);
  }
}
