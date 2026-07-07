/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.file.v2;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.v2.node.NodeProperty;
import org.junit.jupiter.api.Test;

class FileNodeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesFromItsNodeStringViaAPlainObjectMapper() throws Exception {
        final String nodeString = "{\"filePath\":\"/data/reading.json\",\"contentType\":\"TEXT_JSON\"}";

        final FileNode node = objectMapper.readValue(nodeString, FileNode.class);

        assertThat(node.filePath()).isEqualTo("/data/reading.json");
        assertThat(node.contentType()).isEqualTo(FileContentType.TEXT_JSON);
        assertThat(node.nodeId()).isEqualTo("/data/reading.json");
    }

    @Test
    void isUniqueAndTyped() {
        final FileNode node = new FileNode("/data/reading.csv", FileContentType.TEXT_CSV);

        assertThat(node.properties()).containsExactlyInAnyOrder(NodeProperty.UNIQUE, NodeProperty.TYPED);
    }

    @Test
    void nodeStringRoundTripsIncludingAPathThatNeedsJsonEscaping() throws Exception {
        final FileNode node = new FileNode("C:\\data\\reading \"1\".json", FileContentType.BINARY);

        final FileNode reparsed = objectMapper.readValue(node.nodeString(), FileNode.class);

        assertThat(reparsed.filePath()).isEqualTo(node.filePath());
        assertThat(reparsed.contentType()).isEqualTo(FileContentType.BINARY);
    }

    @Test
    void ignoresUnknownFieldsWhenTheMapperIsLenient() throws Exception {
        final ObjectMapper lenient =
                new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        final String nodeString =
                "{\"filePath\":\"/data/reading.txt\",\"contentType\":\"TEXT_PLAIN\",\"legacyExtra\":true}";

        final FileNode node = lenient.readValue(nodeString, FileNode.class);

        assertThat(node.filePath()).isEqualTo("/data/reading.txt");
        assertThat(node.contentType()).isEqualTo(FileContentType.TEXT_PLAIN);
    }
}
