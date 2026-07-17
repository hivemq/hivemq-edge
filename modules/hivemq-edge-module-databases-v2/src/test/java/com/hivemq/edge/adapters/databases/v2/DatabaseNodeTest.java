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
package com.hivemq.edge.adapters.databases.v2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.v2.node.NodeProperty;
import org.junit.jupiter.api.Test;

class DatabaseNodeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesFromItsNodeStringViaAPlainObjectMapper() throws Exception {
        final String nodeString = "{\"query\":\"SELECT * FROM products\",\"spiltLinesInIndividualMessages\":true}";

        final DatabaseNode node = objectMapper.readValue(nodeString, DatabaseNode.class);

        assertThat(node.query()).isEqualTo("SELECT * FROM products");
        assertThat(node.spiltLinesInIndividualMessages()).isTrue();
        assertThat(node.nodeId()).isEqualTo("SELECT * FROM products");
    }

    @Test
    void theMisspelledSplitLinesKeyIsPreservedForConfigCompatibility() throws Exception {
        final DatabaseNode node = new DatabaseNode("SELECT 1", true);

        assertThat(node.nodeString()).contains("\"spiltLinesInIndividualMessages\":true");
        final DatabaseNode reparsed = objectMapper.readValue(node.nodeString(), DatabaseNode.class);
        assertThat(reparsed.spiltLinesInIndividualMessages()).isTrue();
    }

    @Test
    void anAbsentSplitLinesFlagDefaultsToFalse() throws Exception {
        final DatabaseNode node = objectMapper.readValue("{\"query\":\"SELECT 1\"}", DatabaseNode.class);

        assertThat(node.spiltLinesInIndividualMessages()).isFalse();
    }

    @Test
    void isTypedButNotUnique() {
        // Two tags may carry the identical query text, so a query does not pin a unique source.
        final DatabaseNode node = new DatabaseNode("SELECT * FROM products", false);

        assertThat(node.properties()).containsExactly(NodeProperty.TYPED);
        assertThat(node.is(NodeProperty.UNIQUE)).isFalse();
    }

    @Test
    void rejectsANullQuery() {
        assertThatNullPointerException()
                .isThrownBy(() -> new DatabaseNode(null, false))
                .withMessage("query must not be null");
    }

    @Test
    void nodeStringRoundTripsIncludingAQueryThatNeedsJsonEscaping() throws Exception {
        final DatabaseNode node = new DatabaseNode("SELECT \"name\" FROM products WHERE name = 'a\\b'", false);

        final DatabaseNode reparsed = objectMapper.readValue(node.nodeString(), DatabaseNode.class);

        assertThat(reparsed.query()).isEqualTo(node.query());
        assertThat(reparsed.spiltLinesInIndividualMessages()).isFalse();
    }

    @Test
    void ignoresUnknownFieldsWhenTheMapperIsLenient() throws Exception {
        final ObjectMapper lenient =
                new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        final String nodeString = "{\"query\":\"SELECT 1\",\"legacyExtra\":true}";

        final DatabaseNode node = lenient.readValue(nodeString, DatabaseNode.class);

        assertThat(node.query()).isEqualTo("SELECT 1");
    }
}
