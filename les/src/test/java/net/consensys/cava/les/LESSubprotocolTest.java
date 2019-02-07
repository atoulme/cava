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
package net.consensys.cava.les;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.eth.repository.BlockchainIndex;
import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.cava.kv.MapKeyValueStore;
import net.consensys.cava.rlpx.wire.SubProtocolIdentifier;
import net.consensys.cava.units.bigints.UInt256;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LESSubprotocolTest {

  private IndexWriter writer;

  @BeforeAll
  void setUp(@TempDirectory Path tempDirectory) throws IOException {
    Directory index = new MMapDirectory(tempDirectory.resolve("blockHIndex"));

    StandardAnalyzer analyzer = new StandardAnalyzer();
    IndexWriterConfig config = new IndexWriterConfig(analyzer);
    writer = new IndexWriter(index, config);
  }

  @AfterAll
  void tearDown() throws Exception {
    writer.close();
  }

  @Test
  void supportsLESv2() throws Exception {

    LESSubprotocol sp = new LESSubprotocol(
        1,
        false,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        new MapKeyValueStore(),
        new MapKeyValueStore(),
        new MapKeyValueStore(),
        new BlockchainIndex(writer));
    assertTrue(sp.supports(SubProtocolIdentifier.of("les", 2)));
  }

  @Test
  void noSupportForv3(@TempDirectory Path tempDirectory) throws Exception {


    LESSubprotocol sp = new LESSubprotocol(
        1,
        false,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        new MapKeyValueStore(),
        new MapKeyValueStore(),
        new MapKeyValueStore(),

        new BlockchainIndex(writer));
    assertFalse(sp.supports(SubProtocolIdentifier.of("les", 3)));
  }

  @Test
  void noSupportForETH(@TempDirectory Path tempDirectory) throws Exception {
    LESSubprotocol sp = new LESSubprotocol(
        1,
        false,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        new MapKeyValueStore(),
        new MapKeyValueStore(),
        new MapKeyValueStore(),

        new BlockchainIndex(writer));
    assertFalse(sp.supports(SubProtocolIdentifier.of("eth", 2)));
  }
}
