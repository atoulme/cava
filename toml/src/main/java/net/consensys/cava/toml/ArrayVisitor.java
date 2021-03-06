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

import net.consensys.cava.toml.internal.TomlParser.ArrayValueContext;
import net.consensys.cava.toml.internal.TomlParser.ValContext;
import net.consensys.cava.toml.internal.TomlParserBaseVisitor;

final class ArrayVisitor extends TomlParserBaseVisitor<MutableTomlArray> {

  private final MutableTomlArray array = new MutableTomlArray(true);

  @Override
  public MutableTomlArray visitArrayValue(ArrayValueContext ctx) {
    ValContext valContext = ctx.val();
    if (valContext != null) {
      Object value = valContext.accept(new ValueVisitor());
      if (value != null) {
        TomlPosition position = new TomlPosition(ctx);
        try {
          array.append(value, position);
        } catch (TomlInvalidTypeException e) {
          throw new TomlParseError(e.getMessage(), position);
        }
      }
    }
    return array;
  }

  @Override
  protected MutableTomlArray aggregateResult(MutableTomlArray aggregate, MutableTomlArray nextResult) {
    return aggregate == null ? null : nextResult;
  }

  @Override
  protected MutableTomlArray defaultResult() {
    return array;
  }
}
