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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.v2.node.NodeProperty;
import org.junit.jupiter.api.Test;

class DatabaseNodeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesFromItsNodeStringViaAPlainObjectMapper() throws Exception {
        final String nodeString = "{\"query\":\"SELECT * FROM products\",\"splitMode\":\"OnePerRow\",\"batchSize\":50}";

        final DatabaseNode node = objectMapper.readValue(nodeString, DatabaseNode.class);

        assertThat(node.query()).isEqualTo("SELECT * FROM products");
        assertThat(node.splitMode()).isEqualTo(SplitMode.ONE_PER_ROW);
        assertThat(node.batchSize()).isEqualTo(50);
        assertThat(node.nodeId()).isEqualTo("SELECT * FROM products");
    }

    @Test
    void theSplitModeSerializesAsItsPascalCaseNameAndRoundTrips() throws Exception {
        final DatabaseNode node = new DatabaseNode("SELECT 1", SplitMode.ONE_PER_BATCH, 25);

        assertThat(node.nodeString()).contains("\"splitMode\":\"OnePerBatch\"").contains("\"batchSize\":25");
        final DatabaseNode reparsed = objectMapper.readValue(node.nodeString(), DatabaseNode.class);
        assertThat(reparsed.splitMode()).isEqualTo(SplitMode.ONE_PER_BATCH);
        assertThat(reparsed.batchSize()).isEqualTo(25);
    }

    @Test
    void everySplitModeRoundTripsThroughItsJsonValue() throws Exception {
        for (final SplitMode mode : SplitMode.values()) {
            final DatabaseNode reparsed =
                    objectMapper.readValue(new DatabaseNode("SELECT 1", mode, 100).nodeString(), DatabaseNode.class);
            assertThat(reparsed.splitMode()).isEqualTo(mode);
        }
    }

    @Test
    void anAbsentSplitModeDefaultsToAllInOne() throws Exception {
        final DatabaseNode node = objectMapper.readValue("{\"query\":\"SELECT 1\"}", DatabaseNode.class);

        assertThat(node.splitMode()).isEqualTo(SplitMode.ALL_IN_ONE);
    }

    @Test
    void anAbsentBatchSizeDefaultsTo100() throws Exception {
        final DatabaseNode node =
                objectMapper.readValue("{\"query\":\"SELECT 1\",\"splitMode\":\"OnePerBatch\"}", DatabaseNode.class);

        assertThat(node.batchSize()).isEqualTo(100);
    }

    @Test
    void theBatchSizeIsClampedToItsRange() {
        assertThat(new DatabaseNode("SELECT 1", SplitMode.ONE_PER_BATCH, 0).batchSize())
                .isEqualTo(DatabaseNode.MIN_BATCH_SIZE);
        assertThat(new DatabaseNode("SELECT 1", SplitMode.ONE_PER_BATCH, 10_000).batchSize())
                .isEqualTo(DatabaseNode.MAX_BATCH_SIZE);
    }

    @Test
    void anUnknownSplitModeIsRejectedRatherThanSilentlyDefaulted() {
        // Jackson wraps the creator's IllegalArgumentException, but the unrecognized name still surfaces in the
        // message — a typo fails loudly instead of silently becoming AllInOne.
        assertThatThrownBy(() -> objectMapper.readValue(
                        "{\"query\":\"SELECT 1\",\"splitMode\":\"Sideways\"}", DatabaseNode.class))
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sideways");
    }

    @Test
    void isTypedButNotUnique() {
        // Two tags may carry the identical query text, so a query does not pin a unique source.
        final DatabaseNode node = new DatabaseNode("SELECT * FROM products", SplitMode.ALL_IN_ONE, 100);

        assertThat(node.properties()).containsExactly(NodeProperty.TYPED);
        assertThat(node.is(NodeProperty.UNIQUE)).isFalse();
    }

    @Test
    void rejectsANullQuery() {
        assertThatNullPointerException()
                .isThrownBy(() -> new DatabaseNode(null, SplitMode.ALL_IN_ONE, 100))
                .withMessage("query must not be null");
    }

    @Test
    void nodeStringRoundTripsIncludingAQueryThatNeedsJsonEscaping() throws Exception {
        final DatabaseNode node =
                new DatabaseNode("SELECT \"name\" FROM products WHERE name = 'a\\b'", SplitMode.ALL_IN_ONE, 100);

        final DatabaseNode reparsed = objectMapper.readValue(node.nodeString(), DatabaseNode.class);

        assertThat(reparsed.query()).isEqualTo(node.query());
        assertThat(reparsed.splitMode()).isEqualTo(SplitMode.ALL_IN_ONE);
    }

    @Test
    void rejectsAnUnknownFieldEvenWhenTheMapperIsLenient() {
        // The node enforces its own additionalProperties=false contract, so a stray field fails loudly even under a
        // mapper that would otherwise ignore unknown properties — a typo can never be silently dropped.
        final ObjectMapper lenient =
                new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        assertThatThrownBy(() -> lenient.readValue("{\"query\":\"SELECT 1\",\"legacyExtra\":true}", DatabaseNode.class))
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("legacyExtra");
    }

    @Test
    void rejectsTheLegacySplitLinesFieldInsteadOfSilentlyDefaultingToAllInOne() {
        // Migrating from the v1 boolean (or guessing a plausible splitLines): the stale key must fail loudly rather
        // than be dropped, which would silently leave the tag on AllInOne and lose the intended per-row shaping.
        assertThatThrownBy(() -> objectMapper.readValue(
                        "{\"query\":\"SELECT 1\",\"spiltLinesInIndividualMessages\":true}", DatabaseNode.class))
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("spiltLinesInIndividualMessages");
    }
}
