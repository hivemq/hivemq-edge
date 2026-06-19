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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageDispatcher;
import com.hivemq.adapter.sdk.api.v2.model.BrowseContinuation;
import com.hivemq.adapter.sdk.api.v2.model.BrowseResultEntry;
import com.hivemq.adapter.sdk.api.v2.model.ErrorScope;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterOutput;
import com.hivemq.adapter.sdk.api.v2.model.ResolvedAttributes;
import com.hivemq.adapter.sdk.api.v2.model.VerifyOutcome;
import com.hivemq.adapter.sdk.api.v2.node.AccessTriState;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.node.NodeProperty;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.adapter.sdk.api.v2.services.ProtocolAdapterService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import util.EmbeddedOpcUaServerExtension;
import util.TestNamespace;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * EDG-737 — drives the {@link ReferenceBrowseEngine} (the traversal policy the framework's PAW will own)
 * through its DISCOVER → RESOLVE phases against a <b>large, deep, branching</b> embedded OPC-UA address space,
 * to prove the {@code browse}/{@code browseNext}/{@code readNodeAttributes} contract is sufficient to assemble
 * a complete, deduped, typed result — the real end-to-end check that the SDK v2 browse changes are sound.
 * <p>
 * The tree (built by {@link TestNamespace#growLargeTree}) deliberately seeds a variable shared by two folders
 * and a reference cycle. Size is parameterized by system properties ({@code edg737.browse.*}) — small by
 * default for CI, scalable for a soak run (e.g. {@code -Dedg737.browse.depth=6 -Dedg737.browse.varsPerFolder=40}).
 * Determinism comes for free: the synchronous {@link DrainOnCallDispatcher} makes one {@code drainAll()} run the
 * entire walk with no timing dependence.
 */
class OpcUaBrowseEngineConformanceTest {

    @RegisterExtension
    final @NotNull EmbeddedOpcUaServerExtension server = new EmbeddedOpcUaServerExtension();

    @Test
    void browseEngine_walksMassiveTree_completelyDedupedTypedAndPaginated() {
        final int breadth = Integer.getInteger("edg737.browse.breadth", 3);
        final int depth = Integer.getInteger("edg737.browse.depth", 4);
        final int varsPerFolder = Integer.getInteger("edg737.browse.varsPerFolder", 12);
        final int maxReferences = Integer.getInteger("edg737.browse.maxRefs", 5);

        final TestNamespace.LargeTree tree =
                requireNonNull(server.getTestNamespace()).growLargeTree(breadth, depth, varsPerFolder);

        final DrainOnCallDispatcher dispatcher = new DrainOnCallDispatcher();
        final ReferenceBrowseEngine engine = new ReferenceBrowseEngine();
        final EngineOutput output = new EngineOutput(engine);
        final OpcUaConformanceAdapter adapter = connectedAdapter(dispatcher, output);

        // walk the model through DISCOVER -> RESOLVE; with the synchronous dispatcher one drain runs it all
        final ReferenceBrowseEngine.BrowseOutcome outcome =
                engine.start(adapter, new OpcUaConformanceNode(tree.rootNodeId()), maxReferences, 0);
        dispatcher.drainAll();

        assertThat(outcome.isDone()).as("the walk terminated — the reference cycle did not loop").isTrue();
        assertThat(outcome.isOk()).as("the walk succeeded: %s", outcome.failure()).isTrue();

        final List<ReferenceBrowseEngine.BrowsedTag> tags = outcome.result();
        final List<String> resolvedIds = tags.stream().map(tag -> tag.attributes().node().nodeId()).toList();
        final List<String> expectedIds = tree.variables().stream().map(TestNamespace.ExpectedVar::nodeId).toList();

        // completeness + dedup: exactly the expected unique variables — none missing, none duplicated
        assertThat(resolvedIds).as("every variable discovered exactly once (shared node deduped, cycle broken)")
                .containsExactlyInAnyOrderElementsOf(expectedIds)
                .doesNotHaveDuplicates();
        assertThat(resolvedIds).as("the variable shared by two folders was discovered exactly once")
                .filteredOn(tree.sharedNodeId()::equals).hasSize(1);

        // RESOLVE correctness: each variable's datatype matches what the generator declared, and it is readable
        final Map<String, String> expectedType = tree.variables().stream()
                .collect(Collectors.toMap(TestNamespace.ExpectedVar::nodeId, TestNamespace.ExpectedVar::dataTypeId));
        assertThat(tags).allSatisfy(tag -> {
            final String id = tag.attributes().node().nodeId();
            assertThat(tag.attributes().dataType()).as("datatype of %s", id).isEqualTo(expectedType.get(id));
            assertThat(tag.attributes().access().readable()).as("readable %s", id).isEqualTo(AccessTriState.YES);
        });

        // path assembly from browse names (the fix): each variable's path matches the generated hierarchy
        final Map<String, String> expectedPath = tree.variables().stream()
                .collect(Collectors.toMap(TestNamespace.ExpectedVar::nodeId, TestNamespace.ExpectedVar::path));
        assertThat(tags).allSatisfy(tag -> assertThat(tag.path())
                .as("path of %s", tag.attributes().node().nodeId())
                .isEqualTo(expectedPath.get(tag.attributes().node().nodeId())));

        // default tag names derived from the path: non-empty, unique, and the expected value for the shared node
        assertThat(tags.stream().map(ReferenceBrowseEngine.BrowsedTag::tagName).toList())
                .as("every variable got a non-empty, unique default tag name").doesNotContain("").doesNotHaveDuplicates();
        assertThat(tags).filteredOn(tag -> tag.attributes().node().nodeId().equals(tree.sharedNodeId()))
                .singleElement()
                .satisfies(tag -> assertThat(tag.tagName()).as("tag name of the shared node").isEqualTo("shared"));

        // pagination + termination: each folder browsed exactly once, small maxReferences forced continuations
        assertThat(engine.browseCommands).as("each folder browsed exactly once — the cycle caused no re-walk")
                .isEqualTo(tree.folderCount());
        assertThat(engine.browseNextCommands)
                .as("maxReferences=%d forced continuation-point pagination", maxReferences).isGreaterThan(0);
        assertThat(output.browsePages).as("exactly one page event per browse and per browseNext")
                .isEqualTo(engine.browseCommands + engine.browseNextCommands);

        // RESOLVE batching: variables attribute-read in batches of RESOLVE_BATCH
        final int expectedBatches = (expectedIds.size() + ReferenceBrowseEngine.RESOLVE_BATCH - 1) /
                ReferenceBrowseEngine.RESOLVE_BATCH;
        assertThat(engine.resolveBatches)
                .as("attribute reads batched at %d", ReferenceBrowseEngine.RESOLVE_BATCH).isEqualTo(expectedBatches);

        adapter.disconnect();
        adapter.stop();
        dispatcher.drainAll();
        assertThat(output.stopped).as("stopped() after stop()").isTrue();
    }

    private @NotNull OpcUaConformanceAdapter connectedAdapter(
            final @NotNull DrainOnCallDispatcher dispatcher, final @NotNull EngineOutput output) {
        final ProtocolAdapterInput input = ConformanceInput.create(dispatcher);
        final OpcUaConformanceAdapter adapter = new OpcUaConformanceAdapter(input, output, server.getServerUri());
        adapter.start();
        dispatcher.drainAll();
        assertThat(output.started).as("started()").isTrue();
        adapter.connect();
        dispatcher.drainAll();
        assertThat(output.connected).as("connected()").isTrue();
        return adapter;
    }

    /** Minimal {@link Node}: identity is its parseable OPC-UA node id — enough to seed a browse root. */
    private static final class OpcUaConformanceNode extends Node {
        private final @NotNull String parseableNodeId;

        private OpcUaConformanceNode(final @NotNull String parseableNodeId) {
            this.parseableNodeId = parseableNodeId;
        }

        @Override
        public @NotNull String nodeId() {
            return parseableNodeId;
        }

        @Override
        public @NotNull String nodeString() {
            return "{\"nodeId\":\"" + parseableNodeId + "\"}";
        }

        @Override
        public @NotNull EnumSet<NodeProperty> properties() {
            return EnumSet.of(NodeProperty.UNIQUE, NodeProperty.TYPED);
        }
    }

    /**
     * A {@link ProtocolAdapterOutput} that routes browse events into the {@link ReferenceBrowseEngine} (as the
     * PAW will) and tracks the lifecycle acks the harness asserts. All callbacks run on the single dispatch
     * thread, so no synchronization is needed.
     */
    private static final class EngineOutput implements ProtocolAdapterOutput {
        private final @NotNull ReferenceBrowseEngine engine;
        private boolean started;
        private boolean connected;
        private boolean stopped;
        private int browsePages;

        private EngineOutput(final @NotNull ReferenceBrowseEngine engine) {
            this.engine = engine;
        }

        @Override
        public void started() {
            started = true;
        }

        @Override
        public void stopped() {
            stopped = true;
        }

        @Override
        public void connected() {
            connected = true;
        }

        @Override
        public void disconnected() {
        }

        @Override
        public void error(final @NotNull ErrorScope scope, final @NotNull String reason) {
        }

        @Override
        public void verifyResult(final @NotNull Node node, final @NotNull VerifyOutcome outcome) {
        }

        @Override
        public void dataPoint(final @NotNull Node node, final @NotNull DataPoint value) {
        }

        @Override
        public void nodeError(final @NotNull Node node, final @NotNull String reason, final boolean spontaneous) {
        }

        @Override
        public void writeResult(final @NotNull Node node, final boolean success, final @Nullable String reason) {
        }

        @Override
        public void browsePage(
                final int requestId,
                final @NotNull List<BrowseResultEntry> entries,
                final @Nullable BrowseContinuation continuation) {
            browsePages++;
            engine.onBrowsePage(requestId, entries, continuation);
        }

        @Override
        public void readAttributesResult(
                final int requestId, final @NotNull List<ResolvedAttributes> attributes) {
            engine.onReadAttributesResult(requestId, attributes);
        }

        @Override
        public void browseError(final int requestId, final @NotNull String reason) {
            engine.onBrowseError(requestId, reason);
        }
    }

    private record ConformanceInput(@NotNull String adapterId, @NotNull DataPoint adapterConfig,
                                    @NotNull List<NodeTagPair> nodes, @NotNull ProtocolAdapterService services)
            implements ProtocolAdapterInput {

        static @NotNull ConformanceInput create(final @NotNull MessageDispatcher dispatcher) {
            final ConformanceDataPointFactory factory = new ConformanceDataPointFactory();
            return new ConformanceInput("opcua-browse-engine-conformance",
                    factory.createJsonDataPoint("adapterConfig", JsonNodeFactory.instance.objectNode()),
                    List.of(),
                    new ConformanceService(factory, dispatcher));
        }
    }

    private record ConformanceService(@NotNull DataPointFactory dataPointFactory, @NotNull MessageDispatcher dispatcher)
            implements ProtocolAdapterService {
    }

    private static final class ConformanceDataPointFactory implements DataPointFactory {
        @Override
        public @NotNull DataPoint create(final @NotNull String tagName, final @NotNull Object tagValue) {
            return new SimpleDataPoint(tagName, tagValue, false);
        }

        @Override
        public @NotNull DataPoint createJsonDataPoint(final @NotNull String tagName, final @NotNull Object tagValue) {
            return new SimpleDataPoint(tagName, tagValue, true);
        }

        private record SimpleDataPoint(@NotNull String tagName, @NotNull Object tagValue, boolean json)
                implements DataPoint {
            @Override
            public @NotNull Object getTagValue() {
                return tagValue;
            }

            @Override
            public boolean treatTagValueAsJson() {
                return json;
            }

            @Override
            public @NotNull String getTagName() {
                return tagName;
            }
        }
    }
}
