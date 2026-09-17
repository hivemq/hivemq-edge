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
package com.hivemq.edge.adapters.opcua.browse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hivemq.edge.adapters.opcua.browse.FakeOpcUaServer.FakeReadServer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** {@link OperationLimits}: the limits read and every request size derived from them. */
class OperationLimitsTest {

    // --- reading the limits ---

    @Test
    void read_advertisedLimits_inOneRequestOfTheThreeNodes() throws Exception {
        final OpcUaClient client = mock(OpcUaClient.class);
        final FakeReadServer server = new FakeReadServer(1000, Integer.MAX_VALUE, 1000);
        server.advertisedContinuationPoints = 5; // UInt16 on the wire
        server.install(client);

        final OperationLimits limits = OperationLimits.read(client, 120).get(5, TimeUnit.SECONDS);

        assertThat(limits).isEqualTo(new OperationLimits(1000, 1000, 5));
        assertThat(server.limitReads).isEqualTo(1);
    }

    @Test
    void read_requestsTheValueAttributeOfExactlyTheThreeLimitNodes() {
        final OpcUaClient client = mock(OpcUaClient.class);
        final java.util.concurrent.atomic.AtomicReference<List<ReadValueId>> requested =
                new java.util.concurrent.atomic.AtomicReference<>();
        when(client.readAsync(anyDouble(), any(), anyList())).thenAnswer(invocation -> {
            requested.set(invocation.getArgument(2));
            return new CompletableFuture<>();
        });

        final CompletableFuture<OperationLimits> pending = OperationLimits.read(client, 120);

        assertThat(pending).isNotDone();
        assertThat(requested.get())
                .extracting(ReadValueId::getNodeId, ReadValueId::getAttributeId)
                .containsExactly(
                        tuple(
                                NodeIds.Server_ServerCapabilities_OperationLimits_MaxNodesPerRead,
                                AttributeId.Value.uid()),
                        tuple(
                                NodeIds.Server_ServerCapabilities_OperationLimits_MaxNodesPerBrowse,
                                AttributeId.Value.uid()),
                        tuple(NodeIds.Server_ServerCapabilities_MaxBrowseContinuationPoints, AttributeId.Value.uid()));
    }

    @Test
    void read_nothingAdvertised_isNone() throws Exception {
        final OpcUaClient client = mock(OpcUaClient.class);
        new FakeReadServer(0, Integer.MAX_VALUE).install(client);

        assertThat(OperationLimits.read(client, 120).get(5, TimeUnit.SECONDS)).isEqualTo(OperationLimits.NONE);
    }

    @Test
    void read_fails_isNoneNotExceptional() throws Exception {
        // The OperationLimits nodes are optional. A failed read of them must not fail the browse.
        final OpcUaClient client = mock(OpcUaClient.class);
        final FakeReadServer server = new FakeReadServer(0, Integer.MAX_VALUE);
        server.limitReadFails = true;
        server.install(client);

        assertThat(OperationLimits.read(client, 120).get(5, TimeUnit.SECONDS)).isEqualTo(OperationLimits.NONE);
    }

    @Test
    void read_neverAnswered_isNoneAfterTheTimeout() throws Exception {
        final OpcUaClient client = mock(OpcUaClient.class);
        when(client.readAsync(anyDouble(), any(), anyList())).thenReturn(new CompletableFuture<>());

        assertThat(OperationLimits.read(client, 1).get(5, TimeUnit.SECONDS)).isEqualTo(OperationLimits.NONE);
    }

    @Test
    void read_shortResponse_isNone() throws Exception {
        final OpcUaClient client = mock(OpcUaClient.class);
        when(client.readAsync(anyDouble(), any(), anyList()))
                .thenReturn(CompletableFuture.completedFuture(
                        new ReadResponse(null, new DataValue[] {new DataValue(new Variant(uint(7)))}, null)));

        assertThat(OperationLimits.read(client, 120).get(5, TimeUnit.SECONDS)).isEqualTo(OperationLimits.NONE);
    }

    @Test
    void read_nullResults_isNone() throws Exception {
        final OpcUaClient client = mock(OpcUaClient.class);
        when(client.readAsync(anyDouble(), any(), anyList()))
                .thenReturn(CompletableFuture.completedFuture(new ReadResponse(null, null, null)));

        assertThat(OperationLimits.read(client, 120).get(5, TimeUnit.SECONDS)).isEqualTo(OperationLimits.NONE);
    }

    @Test
    void limitValue_acceptsAnyNumber_elseZero() {
        assertThat(OperationLimits.limitValue(new DataValue(new Variant(uint(100)))))
                .isEqualTo(100);
        assertThat(OperationLimits.limitValue(new DataValue(new Variant(ushort(5)))))
                .isEqualTo(5);
        assertThat(OperationLimits.limitValue(new DataValue(new Variant(42L)))).isEqualTo(42);
        assertThat(OperationLimits.limitValue(new DataValue(new Variant("100"))))
                .isZero();
        assertThat(OperationLimits.limitValue(new DataValue(Variant.NULL_VALUE)))
                .isZero();
        assertThat(OperationLimits.limitValue(new DataValue(null, null, null))).isZero();
        assertThat(OperationLimits.limitValue(null)).isZero();
        assertThat(OperationLimits.limitValue(new DataValue(
                        new Variant(uint(3)),
                        new org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode(StatusCodes.Bad_NodeIdUnknown),
                        null)))
                .as("the value is taken as read; a bad status with a value is the server's problem")
                .isEqualTo(3);
    }

    @Test
    void read_uaExceptionFromTheClient_isNone() throws Exception {
        final OpcUaClient client = mock(OpcUaClient.class);
        when(client.readAsync(anyDouble(), any(), anyList()))
                .thenReturn(CompletableFuture.failedFuture(new UaException(StatusCodes.Bad_SessionClosed)));

        assertThat(OperationLimits.read(client, 120).get(5, TimeUnit.SECONDS)).isEqualTo(OperationLimits.NONE);
    }

    // --- request sizes ---

    @ParameterizedTest(name = "MaxNodesPerRead {0} -> {1} variables per read")
    @CsvSource({
        "0, 100", // not advertised / unlimited: default
        "-1, 100",
        "1000, 100", // S7-1500: default fits
        "10000, 100", // Prosys
        "300, 100", // exactly three attributes per default batch
        "299, 99",
        "100, 33", // WAGO PFC200 / Codesys: 33 * 3 = 99 <= 100
        "3, 1",
        "2, 1", // below one variable's worth of attributes: still read one at a time
        "1, 1",
    })
    void readBatchSize_keepsAttributeCountWithinAdvertisedLimit(final int maxNodesPerRead, final int expected) {
        assertThat(new OperationLimits(maxNodesPerRead, 0, 0).readBatchSize()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "MaxNodesPerBrowse {0} -> {1} nodes per browse")
    @CsvSource({"0, 100", "-1, 100", "1000, 100", "100, 100", "60, 60", "1, 1"})
    void browseChunkSize_isTheDefaultOrTheSmallerAdvertisedLimit(final int maxNodesPerBrowse, final int expected) {
        assertThat(new OperationLimits(0, maxNodesPerBrowse, 0).browseChunkSize())
                .isEqualTo(expected);
    }

    @ParameterizedTest(name = "tried {0}, advertised {1} -> {2}")
    @CsvSource({"100, 5, 5", "100, 0, 50", "4, 5, 2", "5, 5, 2", "1, 5, 1", "1, 0, 1", "2, 0, 1", "3, 0, 1"})
    void retryChunkSize(final int tried, final int advertised, final int expected) {
        assertThat(new OperationLimits(0, 0, advertised).retryChunkSize(tried)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "advertised {0}, pending {1} -> {2} per BrowseNext")
    @CsvSource({"5, 6, 5", "5, 2, 5", "0, 6, 6", "0, 0, 0", "-1, 4, 4"})
    void continuationPointsPerBrowseNext(final int advertised, final int pending, final int expected) {
        assertThat(new OperationLimits(0, 0, advertised).continuationPointsPerBrowseNext(pending))
                .isEqualTo(expected);
    }

    @ParameterizedTest(name = "advertised {0} -> {1} per release")
    @CsvSource({"5, 5", "50, 50", "0, 1", "-1, 1"})
    void continuationPointsPerRelease(final int advertised, final int expected) {
        assertThat(new OperationLimits(0, 0, advertised).continuationPointsPerRelease())
                .isEqualTo(expected);
    }

    @Test
    void none_hasNoLimits() {
        assertThat(OperationLimits.NONE).isEqualTo(new OperationLimits(0, 0, 0));
        assertThat(OperationLimits.NONE.readBatchSize()).isEqualTo(OperationLimits.READ_BATCH_SIZE);
        assertThat(OperationLimits.NONE.browseChunkSize()).isEqualTo(OperationLimits.BROWSE_CHUNK_SIZE);
        assertThat(OperationLimits.ATTRIBUTES_PER_NODE).isEqualTo(3);
    }
}
