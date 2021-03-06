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
package net.consensys.cava.toml;

import java.util.List;

/**
 * The result from parsing a TOML document.
 */
public interface TomlParseResult extends TomlTable {

  /**
   * @return <tt>true</tt> if the TOML document contained errors.
   */
  default boolean hasErrors() {
    return !(errors().isEmpty());
  }

  /**
   * The errors that occurred during parsing.
   *
   * @return A list of errors.
   */
  List<TomlParseError> errors();
}
