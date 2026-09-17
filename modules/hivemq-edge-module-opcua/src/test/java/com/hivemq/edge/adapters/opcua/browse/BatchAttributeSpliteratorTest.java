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

import static com.hivemq.edge.adapters.opcua.browse.FakeOpcUaServer.discovered;
import static com.hivemq.edge.adapters.opcua.browse.FakeOpcUaServer.nodeIds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hivemq.edge.adapters.browse.BrowsedNode;
import com.hivemq.edge.adapters.opcua.browse.FakeOpcUaServer.FakeReadServer;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.UaServiceFaultException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * {@link BatchAttributeSpliterator}: Phase 2 read batching, prefetch, halving on rejection, and the stream
 * contract. Driven directly over the read fake with a pre-built list of discovered variables.
 */
class BatchAttributeSpliteratorTest {

    private static final @NotNull String ADAPTER = "adapter";

    private static @NotNull BatchAttributeSpliterator spliterator(
            final @NotNull FakeReadServer server, final int count, final int batchSize) {
        final OpcUaClient client = mock(OpcUaClient.class);
        server.install(client);
        final List<DiscoveredVariable> variables = discovered(count);
        return new BatchAttributeSpliterator(
                variables,
                TagDefaults.tagNames(variables),
                client,
                new AttributeResolver(null),
                ADAPTER,
                120,
                batchSize);
    }

    /** The stream Phase 2 hands out, batch size derived from the fake's advertised limit as the browser does. */
    private static @NotNull Stream<BrowsedNode> stream(final @NotNull FakeReadServer server, final int count) {
        final int batchSize = new OperationLimits(server.advertised, 0, 0).readBatchSize();
        return StreamSupport.stream(spliterator(server, count, batchSize), false);
    }

    private static @NotNull List<BrowsedNode> read(final @NotNull FakeReadServer server, final int count) {
        return stream(server, count).collect(Collectors.toList());
    }

    // --- batch sizing against the server's limit ---

    @Test
    void read_advertisedLimit100_neverSendsMoreThan100ReadValueIds() {
        // The WAGO PFC200 case: MaxNodesPerRead = 100, enforced on the ReadValueId count. 82 variables x 3
        // attributes must be read in requests of at most 100 ids, and every variable must still come out once,
        // in path order.
        final FakeReadServer server = new FakeReadServer(100, 100);

        final List<BrowsedNode> nodes = read(server, 82);

        assertThat(server.attributeReadSizes)
                .as("ReadValueIds per attribute read")
                .containsExactly(99, 99, 48);
        assertThat(nodes).extracting(BrowsedNode::nodeId).containsExactlyElementsOf(nodeIds(82));
    }

    @Test
    void read_limitNotAdvertised_serverRejectsRead_halvesBatchAndRereadsSameSlice() {
        // A server that advertises nothing but still enforces 100 operations: the default batch covers all 82
        // variables (246 ids) and is rejected, so is half of it (41 variables, 123 ids); 20 variables (60 ids)
        // go through and stay the batch size. The rejected slice is re-read from its start, so nothing is
        // skipped or emitted twice.
        final FakeReadServer server = new FakeReadServer(0, 100);

        final List<BrowsedNode> nodes = read(server, 82);

        assertThat(server.attributeReadSizes).containsExactly(246, 123, 60, 60, 60, 60, 6);
        assertThat(server.attributeReadOffsets)
                .as("the rejected reads are retried for the same slice before moving on")
                .containsExactly(0, 0, 0, 20, 40, 60, 80);
        assertThat(nodes).extracting(BrowsedNode::nodeId).containsExactlyElementsOf(nodeIds(82));
    }

    @Test
    void read_serverLiesAboutLimit_stillHalvesDownToWhatItAccepts() {
        // Advertised 1000 (default batch), enforced 100: same recovery as when nothing is advertised.
        final FakeReadServer server = new FakeReadServer(1000, 100);

        final List<BrowsedNode> nodes = read(server, 40);

        assertThat(server.attributeReadSizes).containsExactly(120, 60, 60);
        assertThat(nodes).extracting(BrowsedNode::nodeId).containsExactlyElementsOf(nodeIds(40));
    }

    @Test
    void read_responseTooLarge_halvesTheAttributeBatch() {
        final FakeReadServer server = new FakeReadServer(0, 100);
        server.rejectStatus = StatusCodes.Bad_ResponseTooLarge;

        final List<BrowsedNode> nodes = read(server, 82);

        assertThat(server.attributeReadSizes).startsWith(246, 123, 60);
        assertThat(nodes).extracting(BrowsedNode::nodeId).containsExactlyElementsOf(nodeIds(82));
    }

    @Test
    void read_serverRejectsEvenASingleVariable_failsTheStream() {
        // Halving stops at one variable per read (3 ids). If the server rejects that too, the stream fails with
        // the fault as the cause instead of spinning.
        final FakeReadServer server = new FakeReadServer(0, 2);
        final Stream<BrowsedNode> stream = stream(server, 5);

        assertThatThrownBy(() -> stream.collect(Collectors.toList()))
                .isInstanceOf(UncheckedBrowseException.class)
                .hasMessage("Failed to read node attributes")
                .cause()
                .isInstanceOf(UaServiceFaultException.class);
        assertThat(server.attributeReadSizes).containsExactly(15, 6, 3);
    }

    @Test
    void read_otherFault_failsTheStreamWithoutHalving() {
        final FakeReadServer server = new FakeReadServer(0, 2);
        server.rejectStatus = StatusCodes.Bad_ConnectionClosed;
        final Stream<BrowsedNode> stream = stream(server, 5);

        assertThatThrownBy(() -> stream.collect(Collectors.toList()))
                .isInstanceOf(UncheckedBrowseException.class)
                .hasMessage("Failed to read node attributes");
        assertThat(server.attributeReadSizes).containsExactly(15);
    }

    @Test
    void read_returnsFewerResultsThanAttributes_failsInsteadOfMisaligningAttributes() {
        final FakeReadServer server = new FakeReadServer(0, Integer.MAX_VALUE);
        server.dropLastValue = true;
        final Stream<BrowsedNode> stream = stream(server, 5);

        assertThatThrownBy(() -> stream.collect(Collectors.toList()))
                .isInstanceOf(UncheckedBrowseException.class)
                .hasMessage("Attribute read returned 14 results for 15 attributes");
    }

    @Test
    void read_returnsNoResultsArray_failsLikeAShortResponse() {
        final OpcUaClient client = mock(OpcUaClient.class);
        when(client.readAsync(anyDouble(), any(), anyList()))
                .thenReturn(CompletableFuture.completedFuture(new ReadResponse(null, null, null)));
        final List<DiscoveredVariable> variables = discovered(1);
        final BatchAttributeSpliterator spliterator = new BatchAttributeSpliterator(
                variables, TagDefaults.tagNames(variables), client, new AttributeResolver(null), ADAPTER, 120, 100);

        assertThatThrownBy(() -> spliterator.tryAdvance(node -> {}))
                .isInstanceOf(UncheckedBrowseException.class)
                .hasMessage("Attribute read returned 0 results for 3 attributes");
    }

    @Test
    void read_timesOut_failsTheStreamWithTheBudget() {
        final OpcUaClient client = mock(OpcUaClient.class);
        when(client.readAsync(anyDouble(), any(), anyList())).thenReturn(new CompletableFuture<>());
        final List<DiscoveredVariable> variables = discovered(1);
        final BatchAttributeSpliterator spliterator = new BatchAttributeSpliterator(
                variables, TagDefaults.tagNames(variables), client, new AttributeResolver(null), ADAPTER, 1, 100);

        assertThatThrownBy(() -> spliterator.tryAdvance(node -> {}))
                .isInstanceOf(UncheckedBrowseException.class)
                .hasMessage("Attribute read timed out after 1 seconds")
                .cause()
                .isInstanceOf(TimeoutException.class);
    }

    @Test
    void read_interrupted_failsTheStreamAndKeepsTheFlag() throws Exception {
        final OpcUaClient client = mock(OpcUaClient.class);
        when(client.readAsync(anyDouble(), any(), anyList())).thenReturn(new CompletableFuture<>());
        final List<DiscoveredVariable> variables = discovered(1);
        final BatchAttributeSpliterator spliterator = new BatchAttributeSpliterator(
                variables, TagDefaults.tagNames(variables), client, new AttributeResolver(null), ADAPTER, 120, 100);

        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final AtomicReference<Boolean> interrupted = new AtomicReference<>(false);
        final Thread reader = new Thread(() -> {
            try {
                spliterator.tryAdvance(node -> {});
            } catch (final Throwable t) {
                failure.set(t);
                interrupted.set(Thread.currentThread().isInterrupted());
            }
        });
        reader.start();
        Thread.sleep(200);
        reader.interrupt();
        reader.join(5_000);

        assertThat(reader.isAlive()).isFalse();
        assertThat(failure.get()).isInstanceOf(UncheckedBrowseException.class).hasMessage("Attribute read interrupted");
        assertThat(interrupted.get()).as("interrupt flag restored").isTrue();
    }

    // --- prefetch and the stream contract ---

    @Test
    void firstBatch_isInFlightBeforeTheFirstTryAdvance() {
        final FakeReadServer server = new FakeReadServer(0, Integer.MAX_VALUE);

        spliterator(server, 5, 100);

        assertThat(server.attributeReadSizes).as("fired from the constructor").containsExactly(15);
    }

    @Test
    void nextBatch_isFiredWhenTheCurrentOneIsReceived_notWhenItIsExhausted() {
        final FakeReadServer server = new FakeReadServer(0, Integer.MAX_VALUE);
        final BatchAttributeSpliterator spliterator = spliterator(server, 5, 2);

        assertThat(spliterator.tryAdvance(node -> {})).isTrue();
        assertThat(server.attributeReadSizes)
                .as("batch 0 received, batch 1 already on the wire")
                .containsExactly(6, 6);
        assertThat(spliterator.tryAdvance(node -> {})).isTrue();
        assertThat(server.attributeReadSizes).as("still emitting batch 0").containsExactly(6, 6);
        assertThat(spliterator.tryAdvance(node -> {})).isTrue();
        assertThat(server.attributeReadSizes).containsExactly(6, 6, 3);
    }

    @Test
    void estimateSize_isExactThroughoutTheStream() {
        final FakeReadServer server = new FakeReadServer(0, Integer.MAX_VALUE);
        final BatchAttributeSpliterator spliterator = spliterator(server, 5, 2);

        assertThat(spliterator.estimateSize()).isEqualTo(5);
        for (int emitted = 1; emitted <= 5; emitted++) {
            assertThat(spliterator.tryAdvance(node -> {})).isTrue();
            assertThat(spliterator.estimateSize())
                    .as("after %d emitted", emitted)
                    .isEqualTo(5 - emitted);
        }
        assertThat(spliterator.tryAdvance(node -> {})).isFalse();
        assertThat(spliterator.estimateSize()).isZero();
    }

    @Test
    void estimateSize_staysExactWhileABatchIsHalved() {
        final FakeReadServer server = new FakeReadServer(0, 100);
        final BatchAttributeSpliterator spliterator = spliterator(server, 82, 100);

        assertThat(spliterator.estimateSize()).isEqualTo(82);
        assertThat(spliterator.tryAdvance(node -> {})).isTrue();
        assertThat(spliterator.estimateSize()).isEqualTo(81);
    }

    @Test
    void characteristics_orderedSizedNonNull_andNoSplitting() {
        final BatchAttributeSpliterator spliterator = spliterator(new FakeReadServer(0, Integer.MAX_VALUE), 1, 100);

        assertThat(spliterator.characteristics())
                .isEqualTo(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.NONNULL);
        assertThat(spliterator.trySplit()).isNull();
    }

    @Test
    void emptyVariableList_emitsNothingAndReadsNothing() {
        final FakeReadServer server = new FakeReadServer(0, Integer.MAX_VALUE);
        final BatchAttributeSpliterator spliterator = spliterator(server, 0, 100);

        assertThat(spliterator.tryAdvance(node -> {})).isFalse();
        assertThat(spliterator.estimateSize()).isZero();
        assertThat(server.attributeReadSizes).isEmpty();
    }

    // --- what a BrowsedNode carries ---

    @Test
    void browsedNode_carriesResolvedAttributesAndDefaults() {
        final OpcUaClient client = mock(OpcUaClient.class);
        when(client.readAsync(anyDouble(), any(), anyList())).thenAnswer(invocation -> {
            final List<ReadValueId> ids = invocation.getArgument(2);
            assertThat(ids).hasSize(3);
            return CompletableFuture.completedFuture(new ReadResponse(
                    null,
                    new DataValue[] {
                        new DataValue(new Variant(new NodeId(0, 6))), // Int32
                        new DataValue(new Variant(Unsigned.ubyte(3))), // CurrentRead | CurrentWrite
                        new DataValue(new Variant(LocalizedText.english("the motor speed")))
                    },
                    null));
        });
        final List<DiscoveredVariable> variables =
                List.of(new DiscoveredVariable(NodeId.parse("ns=2;s=Speed"), "/Motor/Speed", "urn:plc", 2, "Speed"));
        final Stream<BrowsedNode> stream = StreamSupport.stream(
                new BatchAttributeSpliterator(
                        variables, List.of("motor-speed"), client, new AttributeResolver(null), "opc", 120, 100),
                false);

        assertThat(stream.toList()).singleElement().satisfies(node -> {
            assertThat(node.nodePath()).isEqualTo("/Motor/Speed");
            assertThat(node.namespaceUri()).isEqualTo("urn:plc");
            assertThat(node.namespaceIndex()).isEqualTo(2);
            assertThat(node.nodeId()).isEqualTo("ns=2;s=Speed");
            assertThat(node.dataType()).as("no tree: the type's node id").isEqualTo("i=6");
            assertThat(node.accessLevel()).isEqualTo("READ_WRITE");
            assertThat(node.nodeDescription()).isEqualTo("the motor speed");
            assertThat(node.tagDescription()).isEqualTo("the motor speed");
            assertThat(node.tagNameDefault()).isEqualTo("motor-speed");
            assertThat(node.northboundTopicDefault()).isEqualTo("opc/motor/speed");
            assertThat(node.southboundTopicDefault()).isEqualTo("opc/write/motor/speed");
        });
    }

    @Test
    void read_limitReadFailure_isNotThisClassesConcern_defaultBatchReadsEverything() {
        // The spliterator never reads the limits itself; a caller that got none uses the default batch.
        final FakeReadServer server = new FakeReadServer(0, Integer.MAX_VALUE);
        server.limitReadFails = true;

        final List<BrowsedNode> nodes = read(server, 150);

        assertThat(server.attributeReadSizes).containsExactly(300, 150);
        assertThat(nodes).hasSize(150);
        assertThat(server.limitReads).isZero();
    }

    @Test
    void read_uaExceptionThatIsNotAServiceFault_failsTheStream() {
        final OpcUaClient client = mock(OpcUaClient.class);
        when(client.readAsync(anyDouble(), any(), anyList()))
                .thenReturn(CompletableFuture.failedFuture(new UaException(StatusCodes.Bad_SessionClosed)));
        final List<DiscoveredVariable> variables = discovered(2);
        final Stream<BrowsedNode> stream = StreamSupport.stream(
                new BatchAttributeSpliterator(
                        variables,
                        TagDefaults.tagNames(variables),
                        client,
                        new AttributeResolver(null),
                        ADAPTER,
                        120,
                        100),
                false);

        assertThatThrownBy(stream::toList)
                .isInstanceOf(UncheckedBrowseException.class)
                .hasMessage("Failed to read node attributes")
                .cause()
                .isInstanceOf(UaException.class);
    }
}
