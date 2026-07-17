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
package com.hivemq.edge.adapters.opcua.conformance;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.schema.ScalarSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.v2.model.BrowseContinuation;
import com.hivemq.adapter.sdk.api.v2.model.BrowseFilter;
import com.hivemq.adapter.sdk.api.v2.model.BrowseNode;
import com.hivemq.adapter.sdk.api.v2.model.ErrorScope;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterOutput;
import com.hivemq.adapter.sdk.api.v2.model.ResolvedAttributes;
import com.hivemq.adapter.sdk.api.v2.model.VerifyOutcome;
import com.hivemq.adapter.sdk.api.v2.model.WriteEntry;
import com.hivemq.adapter.sdk.api.v2.node.AccessTriState;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import util.EmbeddedOpcUaServerExtension;

/**
 * EDG-737 — OPC-UA conformance of the SDK v2 foundation model. Drives a real OPC-UA adapter
 * ({@link OpcUaConformanceAdapter}) built on {@link com.hivemq.adapter.sdk.api.v2.template.AbstractProtocolAdapter}
 * against an embedded Milo server, through the SDK v2 command/event contract, asserting the model carries real
 * OPC-UA interactions: connect/verify/poll (1), write (2), subscribe (3), browse (4).
 */
class OpcUaFoundationConformanceTest {

    // Embedded TestNamespace nodes register at ns=1 (see TestNamespace#addDynamicNodes).
    private static final @NotNull ConformanceNode INT32_NODE = new ConformanceNode("ns=1;i=11");
    private static final @NotNull ConformanceNode DOUBLE_NODE = new ConformanceNode("ns=1;i=13");
    // the standard OPC-UA Objects folder — the browse root.
    private static final @NotNull ConformanceNode OBJECTS_FOLDER = new ConformanceNode("ns=0;i=85");

    @RegisterExtension
    final @NotNull EmbeddedOpcUaServerExtension server = new EmbeddedOpcUaServerExtension();

    private final @NotNull DataPointFactory dataPointFactory = ConformanceHarness.dataPointFactory();

    @Test
    void connectVerifyPoll_carryThroughTheFoundationModel() {
        final DrainOnCallDispatcher dispatcher = new DrainOnCallDispatcher();
        final RecordingOutput output = new RecordingOutput();
        final OpcUaConformanceAdapter adapter = connectedAdapter(dispatcher, output);
        final List<Node> nodes = List.of(INT32_NODE, DOUBLE_NODE);

        adapter.verifyBatch(nodes);
        dispatcher.drainAll();
        assertThat(output.verifyOutcomes.keySet()).containsExactlyInAnyOrderElementsOf(nodes);
        assertThat(output.verifyOutcomes.values())
                .as("every declared node verifies as Success against the device")
                .allMatch(outcome -> outcome instanceof VerifyOutcome.Success);

        adapter.pollBatch(nodes);
        dispatcher.drainAll();
        assertThat(output.nodeErrors).as("no poll errors").isEmpty();
        assertThat(output.dataPoints.keySet()).containsExactlyInAnyOrderElementsOf(nodes);
        assertThat(output.dataPoints.get(INT32_NODE).getTagValue()).isInstanceOf(Number.class);
        assertThat(output.dataPoints.get(DOUBLE_NODE).getTagValue()).isInstanceOf(Number.class);

        adapter.disconnect();
        adapter.stop();
        dispatcher.drainAll();
        assertThat(output.disconnected).as("disconnected()").isTrue();
        assertThat(output.stopped).as("stopped() after stop()").isTrue();
    }

    @Test
    void write_carriesSouthboundThroughTheFoundationModel() {
        final DrainOnCallDispatcher dispatcher = new DrainOnCallDispatcher();
        final RecordingOutput output = new RecordingOutput();
        final OpcUaConformanceAdapter adapter = connectedAdapter(dispatcher, output);

        adapter.writeBatch(List.of(new WriteEntry(INT32_NODE, dataPointFactory.create("write", 4242))));
        dispatcher.drainAll();

        // Conformance is the southbound round-trip: write command -> device -> writeResult flows through the
        // model. The embedded TestNamespace nodes are getValue-callback backed and grant no UserAccessLevel to
        // the anonymous session, so the device itself declines the write — correct device behaviour, asserted
        // here as a returned result rather than a forced success (a read-back cannot reflect a write on these
        // synthetic nodes either, since reads return a random callback value).
        assertThat(output.writeResults)
                .as("southbound write round-trips to a writeResult through the foundation model")
                .containsKey(INT32_NODE);
    }

    @Test
    void subscribe_incrementalAdd_deliversPushedValuesForEachNode() throws Exception {
        final DrainOnCallDispatcher dispatcher = new DrainOnCallDispatcher();
        final RecordingOutput output = new RecordingOutput();
        final OpcUaConformanceAdapter adapter = connectedAdapter(dispatcher, output);

        // incremental add: two separate batches; the second must not reset the first
        adapter.addSubscriptionBatch(List.of(INT32_NODE));
        dispatcher.drainAll();
        adapter.addSubscriptionBatch(List.of(DOUBLE_NODE));
        dispatcher.drainAll();

        assertThat(output.awaitDataPoint(INT32_NODE, TimeUnit.SECONDS.toMillis(10)))
                .as("a pushed value arrives for the first-subscribed node")
                .isTrue();
        assertThat(output.awaitDataPoint(DOUBLE_NODE, TimeUnit.SECONDS.toMillis(10)))
                .as("the second incremental subscription also delivers — the first was not reset")
                .isTrue();
        assertThat(output.dataPoints.get(INT32_NODE).getTagValue()).isInstanceOf(Number.class);

        adapter.disconnect();
        adapter.stop();
        dispatcher.drainAll();
        assertThat(output.disconnected).as("disconnected()").isTrue();
        assertThat(output.stopped).as("stopped() after stop()").isTrue();
    }

    @Test
    void browse_paginates_assemblingAllVariablesAcrossPages() {
        final DrainOnCallDispatcher dispatcher = new DrainOnCallDispatcher();
        final RecordingOutput output = new RecordingOutput();
        final OpcUaConformanceAdapter adapter = connectedAdapter(dispatcher, output);
        adapter.pageDelay(15); // simulate a slow device — pages must still assemble correctly

        // discover the namespace-1 folders directly under Objects (one page, server-decided size)
        final List<Node> testFolders = browseAllPages(adapter, dispatcher, output, OBJECTS_FOLDER, 0).stream()
                .filter(entry -> !entry.selectable()) // folders are Objects, not selectable variables
                .map(BrowseNode::node)
                .filter(node -> node.nodeId().startsWith("ns=1;"))
                .toList();
        assertThat(testFolders)
                .as("the test-namespace folder is discoverable under Objects")
                .isNotEmpty();

        // browse each ns=1 folder with maxReferences=1 -> forces continuation-point pagination
        final int pagesBeforeVariables = output.browsePageCount();
        final Set<String> variableNodeIds = new HashSet<>();
        for (final Node folder : testFolders) {
            for (final BrowseNode entry : browseAllPages(adapter, dispatcher, output, folder, 1)) {
                if (entry.selectable()) {
                    variableNodeIds.add(entry.node().nodeId());
                }
            }
        }

        assertThat(variableNodeIds)
                .as("the paginated browse assembled the test variables across pages")
                .contains(INT32_NODE.nodeId(), DOUBLE_NODE.nodeId());
        assertThat(output.browsePageCount() - pagesBeforeVariables)
                .as("maxReferences=1 forced multi-page pagination (continuation points)")
                .isGreaterThan(1);

        adapter.disconnect();
        adapter.stop();
        dispatcher.drainAll();
        assertThat(output.stopped).as("stopped() after stop()").isTrue();
    }

    @Test
    void browse_thenResolve_resolvesDeclaredAttributesOfDiscoveredVariables() {
        final DrainOnCallDispatcher dispatcher = new DrainOnCallDispatcher();
        final RecordingOutput output = new RecordingOutput();
        final OpcUaConformanceAdapter adapter = connectedAdapter(dispatcher, output);

        // DISCOVER: walk the ns=1 folders under Objects and collect their selectable variables
        final List<Node> variables = new ArrayList<>();
        for (final BrowseNode folder : browseAllPages(adapter, dispatcher, output, OBJECTS_FOLDER, 0)) {
            if (folder.selectable() || !folder.node().nodeId().startsWith("ns=1;")) {
                continue; // only the test-namespace folders
            }
            for (final BrowseNode entry : browseAllPages(adapter, dispatcher, output, folder.node(), 0)) {
                if (entry.selectable()) {
                    variables.add(entry.node());
                }
            }
        }
        assertThat(variables)
                .as("browse discovered the test variables to resolve")
                .isNotEmpty();

        // RESOLVE: one batched round-trip reads every discovered variable's attributes, reported once
        adapter.readNodeAttributes(7, variables);
        dispatcher.drainAll();

        final List<ResolvedAttributes> resolved = output.resolvedAttributes();
        assertThat(resolved)
                .as("one ResolvedAttributes per discovered variable, reported once for the batch")
                .hasSize(variables.size());
        assertThat(resolved)
                .as("every variable resolves a non-empty datatype and is readable")
                .allSatisfy(attr -> {
                    assertThat(attr.dataType()).isNotEmpty();
                    assertThat(attr.access().readable()).isEqualTo(AccessTriState.YES);
                });

        // conformance: the known Int32 / Double nodes resolve to their declared OPC-UA datatype ids
        final Map<String, String> dataTypeByNodeId = resolved.stream()
                .collect(Collectors.toMap(attr -> attr.node().nodeId(), ResolvedAttributes::dataType, (a, _) -> a));
        assertThat(dataTypeByNodeId.get(INT32_NODE.nodeId()))
                .as("the Int32 variable resolves the Int32 datatype")
                .isEqualTo(NodeIds.Int32.toParseableString());
        assertThat(dataTypeByNodeId.get(DOUBLE_NODE.nodeId()))
                .as("the Double variable resolves the Double datatype")
                .isEqualTo(NodeIds.Double.toParseableString());

        // EDG-737 P4: the resolved OPC-UA datatype id round-trips into a v1 Schema — i.e. the foundation
        // carries enough for the import layer to build a typed tag, not just an opaque datatype string.
        assertThat(schemaFor(dataTypeByNodeId.get(INT32_NODE.nodeId())))
                .as("the Int32 datatype maps to a v1 scalar schema")
                .isInstanceOfSatisfying(
                        ScalarSchema.class, schema -> assertThat(schema.type()).isEqualTo(ScalarType.LONG));
        assertThat(schemaFor(dataTypeByNodeId.get(DOUBLE_NODE.nodeId())))
                .as("the Double datatype maps to a v1 scalar schema")
                .isInstanceOfSatisfying(
                        ScalarSchema.class, schema -> assertThat(schema.type()).isEqualTo(ScalarType.DOUBLE));

        adapter.disconnect();
        adapter.stop();
        dispatcher.drainAll();
        assertThat(output.stopped).as("stopped() after stop()").isTrue();
    }

    /**
     * Maps a resolved OPC-UA datatype id to the v1 {@link Schema} the import layer would build for it — the
     * minimal proof (EDG-737 P4) that the resolved attribute string is sufficient to produce a typed tag.
     */
    private static @NotNull Schema schemaFor(final @NotNull String opcuaDataTypeId) {
        final ScalarType type;
        if (NodeIds.Int32.toParseableString().equals(opcuaDataTypeId)) {
            type = ScalarType.LONG;
        } else if (NodeIds.Double.toParseableString().equals(opcuaDataTypeId)) {
            type = ScalarType.DOUBLE;
        } else {
            type = ScalarType.STRING;
        }
        return new ScalarSchema(type, null, null, null, null, false, true, false);
    }

    @Test
    void controlCommand_firedMidBrowse_preemptsTheQueuedNextPage() {
        final DrainOnCallDispatcher dispatcher = new DrainOnCallDispatcher();
        final RecordingOutput output = new RecordingOutput();
        final OpcUaConformanceAdapter adapter = connectedAdapter(dispatcher, output);

        // take the first page of a paginated browse — it leaves a continuation (more pages pending)
        adapter.browse(9, new BrowseFilter(OBJECTS_FOLDER), 1);
        dispatcher.drainAll();
        final BrowseContinuation continuation = output.lastContinuation();
        assertThat(continuation).as("first page left a continuation").isNotNull();

        // queue the next page (DATA band) AND a disconnect (CONTROL band) together, then drain once
        adapter.browseNext(9, requireNonNull(continuation)); // DATA
        adapter.disconnect(); // CONTROL
        dispatcher.drainAll();

        // CONTROL outranks DATA in the priority mailbox: the disconnect runs before the queued browse page
        // (which then fails because the client is gone) — pagination created the interleave point.
        assertThat(output.eventLog())
                .as("a CONTROL command fired mid-browse preempts the queued next page")
                .containsSubsequence("disconnected", "browseError");
    }

    @Test
    void browseCancel_releasesTheOpenContinuationPoint() {
        final DrainOnCallDispatcher dispatcher = new DrainOnCallDispatcher();
        final RecordingOutput output = new RecordingOutput();
        final OpcUaConformanceAdapter adapter = connectedAdapter(dispatcher, output);

        // the first page of a paginated browse leaves a continuation point open on the server
        adapter.browse(11, new BrowseFilter(OBJECTS_FOLDER), 1);
        dispatcher.drainAll();
        final BrowseContinuation continuation = output.lastContinuation();
        assertThat(continuation).as("first page left a continuation").isNotNull();

        // abandon the browse: browseCancel releases the continuation point (OPC-UA ReleaseContinuationPoints)
        adapter.browseCancel(11);
        dispatcher.drainAll();

        // resuming the now-released point is rejected by the server -> browseError, never a real next page
        adapter.browseNext(11, requireNonNull(continuation));
        dispatcher.drainAll();
        assertThat(output.eventLog())
                .as("resuming the released continuation fails rather than returning a page")
                .containsSubsequence("browsePage", "browseError");
        assertThat(output.eventLog())
                .as("the released point yields no second page — only the original first page")
                .filteredOn("browsePage"::equals)
                .hasSize(1);
    }

    /**
     * Drive a full paginated browse of one node to completion (browse + browseNext until no continuation),
     * returning every entry assembled across its pages — the orchestration the framework's PAW will own.
     */
    private static @NotNull List<BrowseNode> browseAllPages(
            final @NotNull OpcUaConformanceAdapter adapter,
            final @NotNull DrainOnCallDispatcher dispatcher,
            final @NotNull RecordingOutput output,
            final @NotNull Node node,
            final int maxReferences) {
        final int firstPageIndex = output.browsePageCount();
        adapter.browse(1, new BrowseFilter(node), maxReferences);
        dispatcher.drainAll();
        BrowseContinuation continuation = output.lastContinuation();
        while (continuation != null) {
            adapter.browseNext(1, continuation);
            dispatcher.drainAll();
            continuation = output.lastContinuation();
        }
        final List<BrowseNode> entries = new ArrayList<>();
        output.pagesFrom(firstPageIndex).forEach(entries::addAll);
        return entries;
    }

    private @NotNull OpcUaConformanceAdapter connectedAdapter(
            final @NotNull DrainOnCallDispatcher dispatcher, final @NotNull RecordingOutput output) {
        final ProtocolAdapterInput input = ConformanceHarness.input(dispatcher);
        final OpcUaConformanceAdapter adapter = new OpcUaConformanceAdapter(input, output, server.getServerUri());
        adapter.start();
        dispatcher.drainAll();
        assertThat(output.started).as("started()").isTrue();
        adapter.connect();
        dispatcher.drainAll();
        assertThat(output.errors).as("no connection error").isEmpty();
        assertThat(output.connected).as("connected()").isTrue();
        return adapter;
    }

    private static final class RecordingOutput implements ProtocolAdapterOutput {
        private final @NotNull Object lock = new Object();
        private final @NotNull List<String> errors = new ArrayList<>();
        private final @NotNull List<String> nodeErrors = new ArrayList<>();
        private final @NotNull Map<Node, VerifyOutcome> verifyOutcomes = new LinkedHashMap<>();
        private final @NotNull Map<Node, DataPoint> dataPoints = new LinkedHashMap<>();
        private final @NotNull Map<Node, Boolean> writeResults = new LinkedHashMap<>();
        private final @NotNull List<String> eventLog = new ArrayList<>();
        private final @NotNull List<List<BrowseNode>> browsePages = new ArrayList<>();
        private final @NotNull List<ResolvedAttributes> resolvedAttributes = new ArrayList<>();
        private volatile boolean started;
        private volatile boolean stopped;
        private volatile boolean connected;
        private volatile boolean disconnected;
        private volatile @Nullable BrowseContinuation lastContinuation;

        private void event(final @NotNull String name) {
            synchronized (lock) {
                eventLog.add(name);
            }
        }

        @Override
        public void started() {
            started = true;
            event("started");
        }

        @Override
        public void stopped() {
            stopped = true;
            event("stopped");
        }

        @Override
        public void connected() {
            connected = true;
            event("connected");
        }

        @Override
        public void disconnected() {
            disconnected = true;
            event("disconnected");
        }

        @Override
        public void error(final @NotNull ErrorScope scope, final @NotNull String reason) {
            synchronized (lock) {
                errors.add(scope + ": " + reason);
            }
        }

        @Override
        public void verifyResult(final @NotNull Node node, final @NotNull VerifyOutcome outcome) {
            synchronized (lock) {
                verifyOutcomes.put(node, outcome);
            }
        }

        @Override
        public void dataPoint(final @NotNull Node node, final @NotNull DataPoint value) {
            synchronized (lock) {
                dataPoints.put(node, value);
                lock.notifyAll();
            }
        }

        @Override
        public void pollComplete(final @NotNull Node node) {
            // Poll cadence is the framework's concern; the conformance rig only inspects values and errors.
        }

        @Override
        public void nodeError(final @NotNull Node node, final @NotNull String reason, final boolean spontaneous) {
            synchronized (lock) {
                nodeErrors.add(node.nodeId() + ": " + reason);
            }
        }

        @Override
        public void writeResult(final @NotNull Node node, final boolean success, final @Nullable String reason) {
            synchronized (lock) {
                writeResults.put(node, success);
            }
        }

        @Override
        public void browsePage(
                final int requestId,
                final @NotNull List<BrowseNode> entries,
                final @Nullable BrowseContinuation continuation) {
            synchronized (lock) {
                browsePages.add(entries);
                lastContinuation = continuation;
                eventLog.add("browsePage");
            }
        }

        @Override
        public void readAttributesResult(final int requestId, final @NotNull List<ResolvedAttributes> attributes) {
            synchronized (lock) {
                resolvedAttributes.addAll(attributes);
                eventLog.add("readAttributesResult");
            }
        }

        @Override
        public void browseError(final int requestId, final @NotNull String reason) {
            event("browseError");
        }

        @NotNull
        List<ResolvedAttributes> resolvedAttributes() {
            synchronized (lock) {
                return List.copyOf(resolvedAttributes);
            }
        }

        @NotNull
        List<String> eventLog() {
            synchronized (lock) {
                return List.copyOf(eventLog);
            }
        }

        @Nullable
        BrowseContinuation lastContinuation() {
            return lastContinuation;
        }

        int browsePageCount() {
            synchronized (lock) {
                return browsePages.size();
            }
        }

        @NotNull
        List<List<BrowseNode>> pagesFrom(final int fromIndex) {
            synchronized (lock) {
                return List.copyOf(browsePages.subList(fromIndex, browsePages.size()));
            }
        }

        boolean awaitDataPoint(final @NotNull Node node, final long timeoutMillis) throws InterruptedException {
            final long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
            synchronized (lock) {
                while (!dataPoints.containsKey(node)) {
                    final long remainingMillis = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
                    if (remainingMillis <= 0) {
                        return false;
                    }
                    lock.wait(remainingMillis);
                }
                return true;
            }
        }
    }
}
