/*
 * Copyright 2018, ConsenSys Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.consensys.cava.trie.experimental

import net.consensys.cava.bytes.Bytes

internal interface NodeVisitor<V> {

  suspend fun visit(extensionNode: ExtensionNode<V>, path: Bytes): Node<V>

  suspend fun visit(branchNode: BranchNode<V>, path: Bytes): Node<V>

  suspend fun visit(leafNode: LeafNode<V>, path: Bytes): Node<V>

  suspend fun visit(nullNode: NullNode<V>, path: Bytes): Node<V>
}
