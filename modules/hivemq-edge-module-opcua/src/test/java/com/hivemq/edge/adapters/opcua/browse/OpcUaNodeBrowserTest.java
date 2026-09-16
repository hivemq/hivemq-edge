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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hivemq.edge.adapters.browse.BrowseException;
import com.hivemq.edge.adapters.browse.BrowsedNode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.UaServiceFaultException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseNextResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ResponseHeader;
import org.eclipse.milo.opcua.stack.core.types.structured.ServiceFault;
import org.eclipse.milo.opcua.stack.core.types.structured.ViewDescription;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OpcUaNodeBrowserTest {

    // --- Browse with non-Good status code ---

    @Test
    void browse_nonGoodStatusCode_throwsBrowseException() {
        final OpcUaClient client = mock(OpcUaClient.class);
        // Server returns BadTooManyOperations (simulates server-side throttling under concurrent browse load)
        final BrowseResult badResult = new BrowseResult(
                new StatusCode(StatusCodes.Bad_TooManyOperations), ByteString.NULL_VALUE, new ReferenceDescription[0]);
        new FakeBrowseServer().status(NodeIds.ObjectsFolder, badResult).install(client);
        new FakeReadServer(0, Integer.MAX_VALUE).install(client);

        final OpcUaNodeBrowser browser = new OpcUaNodeBrowser(client, "test-adapter");

        assertThatThrownBy(() -> browser.browse(null, 0))
                .isInstanceOf(BrowseException.class)
                .cause()
                .hasMessageContaining("non-Good status")
                .hasMessageContaining("Bad_TooManyOperations");
    }

    @Test
    void browse_goodStatusCode_emptyResult_succeeds() throws BrowseException {
        final OpcUaClient client = mock(OpcUaClient.class);
        // Good status but no references (empty node)
        final BrowseResult goodResult =
                new BrowseResult(StatusCode.GOOD, ByteString.NULL_VALUE, new ReferenceDescription[0]);
        new FakeBrowseServer().status(NodeIds.ObjectsFolder, goodResult).install(client);
        new FakeReadServer(0, Integer.MAX_VALUE).install(client);

        final OpcUaNodeBrowser browser = new OpcUaNodeBrowser(client, "test-adapter");

        assertThat(browser.browse(null, 0).count()).isEqualTo(0);
    }

    // --- deterministic collision suffixes ---

    @Test
    void browse_samePathNodes_tagNameDefaultSuffixIndependentOfArrivalOrder() throws BrowseException {
        // Two variables under the same browse path collide on tagNameDefault and get "-2" appended to one of
        // them. The order the async browse callbacks deliver them in varies between browses, so the suffix
        // must be decided by a stable key (the NodeId), not by arrival order — otherwise a CSV exported from
        // one browse names a different node than the next browse would.
        final ReferenceDescription first = variable("ns=2;s=Sim1/Max", "Max Value");
        final ReferenceDescription second = variable("ns=2;s=Sim2/Max", "Max Value");

        final Map<String, String> forward = tagNameDefaultsByNodeId(browse(first, second));
        final Map<String, String> reversed = tagNameDefaultsByNodeId(browse(second, first));

        assertThat(forward)
                .containsEntry("ns=2;s=Sim1/Max", "max-value")
                .containsEntry("ns=2;s=Sim2/Max", "max-value-2");
        assertThat(reversed)
                .as("the same node keeps the same default whichever order the server delivered the references")
                .isEqualTo(forward);
    }

    private static @NotNull ReferenceDescription variable(final @NotNull String nodeId, final @NotNull String name) {
        return new ReferenceDescription(
                NodeIds.HasComponent,
                true,
                ExpandedNodeId.parse(nodeId),
                new QualifiedName(2, name),
                LocalizedText.english(name),
                NodeClass.Variable,
                ExpandedNodeId.NULL_VALUE);
    }

    /** Browse a root whose only children are {@code refs}; every other node is a leaf and reads answer null. */
    private static @NotNull List<BrowsedNode> browse(final @NotNull ReferenceDescription... refs)
            throws BrowseException {
        final OpcUaClient client = mock(OpcUaClient.class);
        final NamespaceTable nsTable = new NamespaceTable();
        nsTable.add("urn:test");
        nsTable.add("urn:test:sim");
        when(client.getNamespaceTable()).thenReturn(nsTable);
        new FakeBrowseServer().children(NodeIds.ObjectsFolder, refs).install(client);
        new FakeReadServer(0, Integer.MAX_VALUE).install(client);
        return new OpcUaNodeBrowser(client, "adapter").browse(null, 0).collect(Collectors.toList());
    }

    private static @NotNull Map<String, String> tagNameDefaultsByNodeId(final @NotNull List<BrowsedNode> nodes) {
        return nodes.stream().collect(Collectors.toMap(BrowsedNode::nodeId, BrowsedNode::tagNameDefault));
    }

    // --- adapter-scoped browse serialisation (EDG-576) ---

    @Test
    void browse_sharedSemaphore_serialisesBrowseAsyncAcrossBrowsers() throws Exception {
        // EDG-576: two distinct browsers sharing the adapter-owned permit must never have their browseAsync
        // calls overlap on the shared client. The peak in-flight count therefore stays at 1.
        final OpcUaClient client = mock(OpcUaClient.class);
        final AtomicInteger inFlight = new AtomicInteger();
        final AtomicInteger peak = new AtomicInteger();
        final BrowseResult empty =
                new BrowseResult(StatusCode.GOOD, ByteString.NULL_VALUE, new ReferenceDescription[0]);
        when(client.browseAsync(any(ViewDescription.class), any(UInteger.class), anyList()))
                .thenAnswer(invocation -> {
                    peak.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
                    try {
                        Thread.sleep(100); // hold the permit long enough that an overlap would be observable
                    } finally {
                        inFlight.decrementAndGet();
                    }
                    final List<BrowseDescription> descriptions = invocation.getArgument(2);
                    final BrowseResult[] results = new BrowseResult[descriptions.size()];
                    Arrays.fill(results, empty);
                    return CompletableFuture.completedFuture(new BrowseResponse(null, results, null));
                });
        new FakeReadServer(0, Integer.MAX_VALUE).install(client);

        final Semaphore shared = new Semaphore(1);
        final OpcUaNodeBrowser first = new OpcUaNodeBrowser(client, "adapter", 0, shared);
        final OpcUaNodeBrowser second = new OpcUaNodeBrowser(client, "adapter", 0, shared);

        final ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            final var f1 = pool.submit(() -> first.browse(null, 0).count());
            final var f2 = pool.submit(() -> second.browse(null, 0).count());
            f1.get(10, TimeUnit.SECONDS);
            f2.get(10, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        assertThat(peak.get())
                .as("the shared adapter-scoped permit serialises browseAsync across concurrent browsers")
                .isEqualTo(1);
    }

    // --- Phase 1 traversal: nested variables, batched Browse, continuation points (EDG-1034) ---

    @Test
    void browse_variablesUnderVariables_areDiscovered() throws BrowseException {
        // Struct members, array elements and properties are Variables whose parent is a Variable. On the lab
        // S7-1500 that is 393 of the 795 PLC variables; before EDG-1034 none of them was returned because the
        // traversal marked a Variable visited when it recorded it and then refused to browse it.
        final FakeBrowseServer server = new FakeBrowseServer()
                .children(NodeIds.ObjectsFolder, object("ns=2;s=DB1", "DB1"))
                .children("ns=2;s=DB1", variable("ns=2;s=DB1.Struct", "Struct"))
                .children(
                        "ns=2;s=DB1.Struct",
                        variable("ns=2;s=DB1.Struct.A", "A"),
                        variable("ns=2;s=DB1.Struct.Arr", "Arr"))
                .children(
                        "ns=2;s=DB1.Struct.Arr",
                        variable("ns=2;s=DB1.Struct.Arr[0]", "0"),
                        variable("ns=2;s=DB1.Struct.Arr[1]", "1"));

        final List<BrowsedNode> nodes = browse(server, 0);

        assertThat(nodes)
                .extracting(BrowsedNode::nodeId, BrowsedNode::nodePath)
                .containsExactly(
                        tuple("ns=2;s=DB1.Struct", "/DB1/Struct"),
                        tuple("ns=2;s=DB1.Struct.A", "/DB1/Struct/A"),
                        tuple("ns=2;s=DB1.Struct.Arr", "/DB1/Struct/Arr"),
                        tuple("ns=2;s=DB1.Struct.Arr[0]", "/DB1/Struct/Arr/0"),
                        tuple("ns=2;s=DB1.Struct.Arr[1]", "/DB1/Struct/Arr/1"));
    }

    @Test
    void browse_maxDepth_countsVariableLevelsToo() throws BrowseException {
        final FakeBrowseServer server = new FakeBrowseServer()
                .children(NodeIds.ObjectsFolder, object("ns=2;s=DB1", "DB1"))
                .children("ns=2;s=DB1", variable("ns=2;s=DB1.Struct", "Struct"))
                .children("ns=2;s=DB1.Struct", variable("ns=2;s=DB1.Struct.A", "A"));

        assertThat(browse(server, 1)).isEmpty();
        assertThat(browse(server, 2)).extracting(BrowsedNode::nodeId).containsExactly("ns=2;s=DB1.Struct");
        assertThat(browse(server, 3))
                .extracting(BrowsedNode::nodeId)
                .containsExactly("ns=2;s=DB1.Struct", "ns=2;s=DB1.Struct.A");
    }

    @Test
    void browse_levelIsBrowsedInOneRequest_chunkedToMaxNodesPerBrowse() throws BrowseException {
        // 250 folders under Objects, each holding one variable. The folder level must go out in requests of at
        // most the advertised MaxNodesPerBrowse (here 60, below the default 100), not one request per folder.
        final FakeBrowseServer server = folders(250);
        final OpcUaClient client = client(server, new FakeReadServer(0, Integer.MAX_VALUE, 60));

        final List<BrowsedNode> nodes =
                new OpcUaNodeBrowser(client, "adapter").browse(null, 0).toList();

        assertThat(server.browseSizes)
                .as("nodes per Browse request")
                .containsExactly(1, 60, 60, 60, 60, 10, 60, 60, 60, 60, 10);
        assertThat(nodes).hasSize(250);
    }

    @Test
    void browse_limitNotAdvertised_serverRejectsBrowse_halvesChunkAndRetries() throws BrowseException {
        // Nothing advertised, so the whole 250-node level goes in one request, capped at the default 100 per
        // request; the server accepts at most 40. Rejected chunks are halved and re-issued, nothing is lost.
        final FakeBrowseServer server = folders(250).enforce(40);
        final OpcUaClient client = client(server, new FakeReadServer(0, Integer.MAX_VALUE, 0));

        final List<BrowsedNode> nodes =
                new OpcUaNodeBrowser(client, "adapter").browse(null, 0).toList();

        assertThat(server.browseSizes).startsWith(1, 100, 50, 25).allSatisfy(n -> assertThat(n)
                .isLessThanOrEqualTo(100));
        assertThat(server.browseSizes.stream().filter(n -> n > 40).count())
                .as("rejected requests")
                .isEqualTo(2);
        assertThat(nodes).hasSize(250);
        assertThat(nodes).extracting(BrowsedNode::nodeId).doesNotHaveDuplicates();
    }

    @Test
    void browse_continuationPoints_drainedBeforeTheNextLevelIsRequested() throws BrowseException {
        // Server pages every result at 3 references. The folder level (5 folders) needs a BrowseNext; the
        // variable level must not be requested until that page has been drained (EDG-465: the S7-1500 expires
        // continuation points that are left waiting behind other browses).
        final FakeBrowseServer server = folders(5).pageSize(3);
        final OpcUaClient client = client(server, new FakeReadServer(0, Integer.MAX_VALUE, 0));

        final List<BrowsedNode> nodes =
                new OpcUaNodeBrowser(client, "adapter").browse(null, 0).toList();

        // root page 1 -> its continuation -> the 5 folders in one request -> their 5 variables (leaves)
        assertThat(server.calls).containsExactly("browse[1]", "next[1]", "browse[5]", "browse[5]");
        assertThat(nodes).hasSize(5);
    }

    @Test
    void browse_serverOutOfContinuationPoints_rebrowsesThoseNodesInSmallerChunks() throws BrowseException {
        // The lab S7-1500 advertises MaxBrowseContinuationPoints = 5 and pages results: a level chunk of 100
        // folders with more than five of them overflowing answers Bad_NoContinuationPoints for the rest. Those
        // nodes are browsed again five at a time; nothing is lost and nothing is browsed twice successfully.
        final FakeBrowseServer server = wideFolders(12, 4).pageSize(3).continuationCapacity(5);
        final FakeReadServer read = new FakeReadServer(0, Integer.MAX_VALUE, 0);
        read.advertisedContinuationPoints = 5;
        final OpcUaClient client = client(server, read);

        final List<BrowsedNode> nodes =
                new OpcUaNodeBrowser(client, "adapter").browse(null, 0).toList();

        assertThat(nodes).hasSize(12 * 4);
        assertThat(nodes).extracting(BrowsedNode::nodeId).doesNotHaveDuplicates();
        // root, then the 12 folders in one request (5 paged, 7 refused), then the 7 again as 5 + 2, then leaves
        assertThat(server.browseSizes).startsWith(1, 12, 5, 2);
        assertThat(server.exhaustedPerRequest).startsWith(0, 7, 0, 0);
    }

    @Test
    void browse_serverOutOfContinuationPoints_noCapacityAdvertised_halvesTheChunk() throws BrowseException {
        final FakeBrowseServer server = wideFolders(12, 4).pageSize(3).continuationCapacity(5);
        final OpcUaClient client = client(server, new FakeReadServer(0, Integer.MAX_VALUE, 0));

        final List<BrowsedNode> nodes =
                new OpcUaNodeBrowser(client, "adapter").browse(null, 0).toList();

        assertThat(nodes).hasSize(12 * 4);
        // 12 -> 7 refused -> retried 6 at a time (5 + 1 refused) -> the one again in a chunk of 3
        assertThat(server.browseSizes).startsWith(1, 12, 6, 1, 1);
    }

    @Test
    void browse_singleNodeStillOutOfContinuationPoints_failsWithThePath() {
        final FakeBrowseServer server = wideFolders(2, 4).pageSize(3).continuationCapacity(0);
        final OpcUaClient client = client(server, new FakeReadServer(0, Integer.MAX_VALUE, 0));

        assertThatThrownBy(() -> new OpcUaNodeBrowser(client, "adapter").browse(null, 0))
                .isInstanceOf(BrowseException.class)
                .cause()
                .hasMessageContaining("Browse at path '/F000'")
                .hasMessageContaining("Bad_NoContinuationPoints");
    }

    @ParameterizedTest(name = "tried {0}, advertised {1} -> {2}")
    @CsvSource({"100, 5, 5", "100, 0, 50", "4, 5, 2", "5, 5, 2", "1, 5, 1", "1, 0, 1"})
    void retryChunkSize(final int tried, final int advertised, final int expected) {
        assertThat(OpcUaNodeBrowser.retryChunkSize(tried, advertised)).isEqualTo(expected);
    }

    /** {@code folders} folders under Objects, each with {@code width} variables. */
    private static @NotNull FakeBrowseServer wideFolders(final int folders, final int width) {
        final FakeBrowseServer server = new FakeBrowseServer();
        final ReferenceDescription[] refs = new ReferenceDescription[folders];
        for (int i = 0; i < folders; i++) {
            final String id = "ns=2;s=F" + String.format("%03d", i);
            refs[i] = object(id, "F" + String.format("%03d", i));
            final ReferenceDescription[] vars = new ReferenceDescription[width];
            for (int v = 0; v < width; v++) {
                vars[v] = variable(id + ".V" + v, "V" + v);
            }
            server.children(id, vars);
        }
        return server.children(NodeIds.ObjectsFolder, refs);
    }

    @Test
    void browse_nonGoodContinuationPage_failsWithThePath() {
        final FakeBrowseServer server = folders(5).pageSize(3);
        server.nextStatus = new StatusCode(StatusCodes.Bad_ContinuationPointInvalid);
        final OpcUaClient client = client(server, new FakeReadServer(0, Integer.MAX_VALUE, 0));

        assertThatThrownBy(() -> new OpcUaNodeBrowser(client, "adapter").browse(null, 0))
                .isInstanceOf(BrowseException.class)
                .cause()
                .hasMessageContaining("Browse continuation at path ''")
                .hasMessageContaining("Bad_ContinuationPointInvalid");
    }

    @Test
    void browse_nodeReachableThroughTwoPaths_emittedOnceUnderTheFirstPath() throws BrowseException {
        final FakeBrowseServer server = new FakeBrowseServer()
                .children(NodeIds.ObjectsFolder, object("ns=2;s=A", "A"), object("ns=2;s=B", "B"))
                .children("ns=2;s=A", variable("ns=2;s=Shared", "Shared"))
                .children("ns=2;s=B", variable("ns=2;s=Shared", "Shared"))
                .children("ns=2;s=Shared", variable("ns=2;s=Shared.Child", "Child"));

        final List<BrowsedNode> nodes = browse(server, 0);

        assertThat(nodes)
                .extracting(BrowsedNode::nodeId, BrowsedNode::nodePath)
                .containsExactly(tuple("ns=2;s=Shared", "/A/Shared"), tuple("ns=2;s=Shared.Child", "/A/Shared/Child"));
        assertThat(server.browsedNodes.stream().filter("ns=2;s=Shared"::equals).count())
                .as("a node reachable twice is browsed once")
                .isEqualTo(1);
    }

    @Test
    void browse_maxReferencesPerNode_isPassedToTheServer() throws BrowseException {
        final FakeBrowseServer server = folders(2);
        final OpcUaClient client = client(server, new FakeReadServer(0, Integer.MAX_VALUE, 0));

        final List<BrowsedNode> nodes =
                new OpcUaNodeBrowser(client, "adapter", 7).browse(null, 0).toList();

        assertThat(nodes).hasSize(2);
        assertThat(server.requestedMaxReferences).containsOnly(7);
    }

    private static @NotNull FakeBrowseServer folders(final int count) {
        final FakeBrowseServer server = new FakeBrowseServer();
        final ReferenceDescription[] folders = new ReferenceDescription[count];
        for (int i = 0; i < count; i++) {
            final String id = "ns=2;s=F" + String.format("%03d", i);
            folders[i] = object(id, "F" + String.format("%03d", i));
            server.children(id, variable(id + ".V", "V"));
        }
        return server.children(NodeIds.ObjectsFolder, folders);
    }

    private static @NotNull List<BrowsedNode> browse(final @NotNull FakeBrowseServer server, final int maxDepth)
            throws BrowseException {
        final OpcUaClient client = client(server, new FakeReadServer(0, Integer.MAX_VALUE));
        return new OpcUaNodeBrowser(client, "adapter").browse(null, maxDepth).toList();
    }

    private static @NotNull OpcUaClient client(
            final @NotNull FakeBrowseServer browse, final @NotNull FakeReadServer read) {
        final OpcUaClient client = mock(OpcUaClient.class);
        final NamespaceTable nsTable = new NamespaceTable();
        nsTable.add("urn:test");
        nsTable.add("urn:test:plc");
        when(client.getNamespaceTable()).thenReturn(nsTable);
        browse.install(client);
        read.install(client);
        return client;
    }

    private static @NotNull ReferenceDescription object(final @NotNull String nodeId, final @NotNull String name) {
        return new ReferenceDescription(
                NodeIds.Organizes,
                true,
                ExpandedNodeId.parse(nodeId),
                new QualifiedName(2, name),
                LocalizedText.english(name),
                NodeClass.Object,
                ExpandedNodeId.NULL_VALUE);
    }

    /**
     * Stands in for the server's Browse and BrowseNext services over a fixed address space: answers each node
     * with its configured children (paged at {@code pageSize} references through continuation points), rejects
     * requests of more than {@code enforced} nodes with Bad_TooManyOperations, and records every call.
     */
    private static final class FakeBrowseServer {
        private final Map<NodeId, ReferenceDescription[]> children = new java.util.HashMap<>();
        private final Map<NodeId, BrowseResult> fixedResults = new java.util.HashMap<>();
        private final Map<ByteString, List<ReferenceDescription>> continuations = new java.util.HashMap<>();
        private int enforced = Integer.MAX_VALUE;
        private int pageSize = Integer.MAX_VALUE;
        private int continuationCapacity = Integer.MAX_VALUE;
        final List<Integer> exhaustedPerRequest = new java.util.ArrayList<>();
        private int nextContinuation;

        @org.jetbrains.annotations.Nullable
        StatusCode nextStatus;

        final List<Integer> browseSizes = new java.util.ArrayList<>();
        final List<String> calls = new java.util.ArrayList<>();
        final List<String> browsedNodes = new java.util.ArrayList<>();
        final List<Integer> requestedMaxReferences = new java.util.ArrayList<>();

        @NotNull
        FakeBrowseServer children(final @NotNull NodeId parent, final @NotNull ReferenceDescription... refs) {
            children.put(parent, refs);
            return this;
        }

        @NotNull
        FakeBrowseServer children(final @NotNull String parent, final @NotNull ReferenceDescription... refs) {
            return children(NodeId.parse(parent), refs);
        }

        @NotNull
        FakeBrowseServer status(final @NotNull NodeId node, final @NotNull BrowseResult result) {
            fixedResults.put(node, result);
            return this;
        }

        @NotNull
        FakeBrowseServer enforce(final int maxNodesPerBrowse) {
            this.enforced = maxNodesPerBrowse;
            return this;
        }

        @NotNull
        FakeBrowseServer pageSize(final int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /** Continuation points the server can hold at once; results beyond that get Bad_NoContinuationPoints. */
        FakeBrowseServer continuationCapacity(final int capacity) {
            this.continuationCapacity = capacity;
            return this;
        }

        void install(final @NotNull OpcUaClient client) {
            when(client.browseAsync(any(ViewDescription.class), any(UInteger.class), anyList()))
                    .thenAnswer(invocation -> {
                        final UInteger maxReferences = invocation.getArgument(1);
                        final List<BrowseDescription> descriptions = invocation.getArgument(2);
                        requestedMaxReferences.add(maxReferences.intValue());
                        browseSizes.add(descriptions.size());
                        calls.add("browse[" + descriptions.size() + "]");
                        if (descriptions.size() > enforced) {
                            return CompletableFuture.failedFuture(tooManyOperations());
                        }
                        final BrowseResult[] results = new BrowseResult[descriptions.size()];
                        int pointsInUse = continuations.size();
                        int exhausted = 0;
                        for (int i = 0; i < results.length; i++) {
                            final NodeId node = descriptions.get(i).getNodeId();
                            browsedNodes.add(node.toParseableString());
                            final BrowseResult fixed = fixedResults.get(node);
                            if (fixed != null) {
                                results[i] = fixed;
                                continue;
                            }
                            final ReferenceDescription[] refs =
                                    children.getOrDefault(node, new ReferenceDescription[0]);
                            if (refs.length > pageSize && pointsInUse >= continuationCapacity) {
                                results[i] = new BrowseResult(
                                        new StatusCode(StatusCodes.Bad_NoContinuationPoints),
                                        ByteString.NULL_VALUE,
                                        new ReferenceDescription[0]);
                                exhausted++;
                                continue;
                            }
                            results[i] = page(refs);
                            if (refs.length > pageSize) {
                                pointsInUse++;
                            }
                        }
                        exhaustedPerRequest.add(exhausted);
                        return CompletableFuture.completedFuture(new BrowseResponse(null, results, null));
                    });
            when(client.browseNextAsync(anyBoolean(), anyList())).thenAnswer(invocation -> {
                final List<ByteString> points = invocation.getArgument(1);
                calls.add("next[" + points.size() + "]");
                final BrowseResult[] results = new BrowseResult[points.size()];
                for (int i = 0; i < results.length; i++) {
                    final List<ReferenceDescription> rest = continuations.remove(points.get(i));
                    results[i] = nextStatus != null
                            ? new BrowseResult(nextStatus, ByteString.NULL_VALUE, new ReferenceDescription[0])
                            : page(rest.toArray(ReferenceDescription[]::new));
                }
                return CompletableFuture.completedFuture(new BrowseNextResponse(null, results, null));
            });
        }

        private @NotNull BrowseResult page(final @NotNull ReferenceDescription[] refs) {
            if (refs.length <= pageSize) {
                return new BrowseResult(StatusCode.GOOD, ByteString.NULL_VALUE, refs);
            }
            final ByteString point = ByteString.of(new byte[] {(byte) ++nextContinuation});
            continuations.put(point, List.of(refs).subList(pageSize, refs.length));
            return new BrowseResult(StatusCode.GOOD, point, Arrays.copyOf(refs, pageSize));
        }
    }

    private static @NotNull UaServiceFaultException tooManyOperations() {
        final ResponseHeader header = new ResponseHeader(
                DateTime.now(), uint(0), new StatusCode(StatusCodes.Bad_TooManyOperations), null, null, null);
        return new UaServiceFaultException(new ServiceFault(header));
    }

    // --- Phase 2 read batching against the server's MaxNodesPerRead (EDG-1034) ---

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
    void initialBatchSize_keepsAttributeCountWithinAdvertisedLimit(final int maxNodesPerRead, final int expected) {
        assertThat(OpcUaNodeBrowser.initialBatchSize(maxNodesPerRead)).isEqualTo(expected);
    }

    @Test
    void browse_advertisedLimit100_neverSendsMoreThan100ReadValueIds() throws BrowseException {
        // The WAGO PFC200 case: MaxNodesPerRead = 100, enforced on the ReadValueId count. 82 variables x 3
        // attributes must be read in requests of at most 100 ids, and every variable must still come out once,
        // in path order.
        final FakeReadServer server = new FakeReadServer(100, 100);
        final List<BrowsedNode> nodes = browseVariables(server, 82);

        assertThat(server.attributeReadSizes)
                .as("ReadValueIds per attribute read")
                .containsExactly(99, 99, 48);
        assertThat(server.attributeReadSizes)
                .allSatisfy(size -> assertThat(size).isLessThanOrEqualTo(100));
        assertThat(nodes).extracting(BrowsedNode::nodeId).containsExactlyElementsOf(nodeIds(82));
    }

    @Test
    void browse_limitNotAdvertised_serverRejectsRead_halvesBatchAndRereadsSameSlice() throws BrowseException {
        // A server that advertises nothing but still enforces 100 operations: the default batch covers all 82
        // variables (246 ids) and is rejected, so is half of it (41 variables, 123 ids); 20 variables (60 ids)
        // go through and stay the batch size. The rejected slice is re-read from its start, so nothing is
        // skipped or emitted twice.
        final FakeReadServer server = new FakeReadServer(0, 100);
        final List<BrowsedNode> nodes = browseVariables(server, 82);

        assertThat(server.attributeReadSizes).containsExactly(246, 123, 60, 60, 60, 60, 6);
        assertThat(server.attributeReadOffsets)
                .as("the rejected reads are retried for the same slice before moving on")
                .containsExactly(0, 0, 0, 20, 40, 60, 80);
        assertThat(nodes).extracting(BrowsedNode::nodeId).containsExactlyElementsOf(nodeIds(82));
    }

    @Test
    void browse_serverLiesAboutLimit_stillHalvesDownToWhatItAccepts() throws BrowseException {
        // Advertised 1000 (default batch), enforced 100: same recovery as when nothing is advertised.
        final FakeReadServer server = new FakeReadServer(1000, 100);
        final List<BrowsedNode> nodes = browseVariables(server, 40);

        assertThat(server.attributeReadSizes).containsExactly(120, 60, 60);
        assertThat(nodes).extracting(BrowsedNode::nodeId).containsExactlyElementsOf(nodeIds(40));
    }

    @Test
    void browse_serverRejectsEvenASingleVariable_failsTheStream() throws BrowseException {
        // Halving stops at one variable per read (3 ids). If the server rejects that too, the stream fails with
        // the fault as the cause instead of spinning.
        final FakeReadServer server = new FakeReadServer(0, 2);
        final var stream = browse(server, 5);

        assertThatThrownBy(() -> stream.collect(Collectors.toList()))
                .isInstanceOf(OpcUaNodeBrowser.UncheckedBrowseException.class)
                .hasMessage("Failed to read node attributes")
                .cause()
                .isInstanceOf(UaServiceFaultException.class);
        assertThat(server.attributeReadSizes).containsExactly(15, 6, 3);
    }

    @Test
    void browse_limitReadFails_usesDefaultBatch() throws BrowseException {
        // The OperationLimits node is optional. A failed read of it must not fail the browse — the default
        // batch applies.
        final FakeReadServer server = new FakeReadServer(0, Integer.MAX_VALUE);
        server.limitReadFails = true;
        final List<BrowsedNode> nodes = browseVariables(server, 150);

        assertThat(server.attributeReadSizes).containsExactly(300, 150);
        assertThat(nodes).hasSize(150);
    }

    @Test
    void browse_readsTheLimitOncePerBrowse_beforeTheAttributeReads() throws BrowseException {
        final FakeReadServer server = new FakeReadServer(100, 100);
        browseVariables(server, 10);

        assertThat(server.limitReads).isEqualTo(1);
    }

    /** Browse a root with {@code count} variable children through {@code server}, fully consumed. */
    private static @NotNull List<BrowsedNode> browseVariables(final @NotNull FakeReadServer server, final int count)
            throws BrowseException {
        return browse(server, count).collect(Collectors.toList());
    }

    private static @NotNull java.util.stream.Stream<BrowsedNode> browse(
            final @NotNull FakeReadServer server, final int count) throws BrowseException {
        final OpcUaClient client = mock(OpcUaClient.class);
        final NamespaceTable nsTable = new NamespaceTable();
        nsTable.add("urn:test");
        nsTable.add("urn:test:plc");
        when(client.getNamespaceTable()).thenReturn(nsTable);
        final ReferenceDescription[] refs = new ReferenceDescription[count];
        for (int i = 0; i < count; i++) {
            refs[i] = variable(nodeId(i), "Var" + String.format("%03d", i));
        }
        new FakeBrowseServer().children(NodeIds.ObjectsFolder, refs).install(client);
        server.install(client);
        return new OpcUaNodeBrowser(client, "adapter").browse(null, 0);
    }

    /** {@code ns=2;s=Var000} .. — zero-padded so path order equals index order. */
    private static @NotNull String nodeId(final int i) {
        return "ns=2;s=Var" + String.format("%03d", i);
    }

    private static @NotNull List<String> nodeIds(final int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(OpcUaNodeBrowserTest::nodeId)
                .toList();
    }

    /**
     * Stands in for the server's Read service: answers the MaxNodesPerRead read with {@code advertised} (0 = the
     * node reads as null), rejects any attribute read with more than {@code enforced} ReadValueIds with
     * Bad_TooManyOperations, and answers everything else with null values. Records what it was asked.
     */
    private static final class FakeReadServer {
        final int advertised;
        final int enforced;
        boolean limitReadFails;
        int limitReads;
        final List<Integer> attributeReadSizes = new java.util.ArrayList<>();
        /** Index (within the browse's variable list) of the first variable of each attribute read. */
        final List<Integer> attributeReadOffsets = new java.util.ArrayList<>();

        final int advertisedBrowse;
        int advertisedContinuationPoints;

        FakeReadServer(final int advertised, final int enforced) {
            this(advertised, enforced, 0);
        }

        FakeReadServer(final int advertised, final int enforced, final int advertisedBrowse) {
            this.advertised = advertised;
            this.enforced = enforced;
            this.advertisedBrowse = advertisedBrowse;
        }

        void install(final @NotNull OpcUaClient client) {
            when(client.readAsync(anyDouble(), any(), anyList())).thenAnswer(invocation -> {
                final List<ReadValueId> ids = invocation.getArgument(2);
                if (ids.size() == 3
                        && NodeIds.Server_ServerCapabilities_OperationLimits_MaxNodesPerRead.equals(
                                ids.get(0).getNodeId())
                        && NodeIds.Server_ServerCapabilities_OperationLimits_MaxNodesPerBrowse.equals(
                                ids.get(1).getNodeId())
                        && NodeIds.Server_ServerCapabilities_MaxBrowseContinuationPoints.equals(
                                ids.get(2).getNodeId())) {
                    limitReads++;
                    if (limitReadFails) {
                        return CompletableFuture.failedFuture(new UaException(StatusCodes.Bad_NodeIdUnknown));
                    }
                    final Variant read = advertised > 0 ? new Variant(uint(advertised)) : Variant.NULL_VALUE;
                    final Variant browse =
                            advertisedBrowse > 0 ? new Variant(uint(advertisedBrowse)) : Variant.NULL_VALUE;
                    // UInt16 on the wire, unlike the two UInt32 operation limits
                    final Variant points = advertisedContinuationPoints > 0
                            ? new Variant(ushort(advertisedContinuationPoints))
                            : Variant.NULL_VALUE;
                    return CompletableFuture.completedFuture(new ReadResponse(
                            null,
                            new DataValue[] {new DataValue(read), new DataValue(browse), new DataValue(points)},
                            null));
                }
                attributeReadSizes.add(ids.size());
                // Only the browseVariables() fixtures use the zero-padded "VarNNN" ids this decodes.
                final String first = ids.get(0).getNodeId().toParseableString();
                if (first.matches(".*Var\\d{3}$")) {
                    attributeReadOffsets.add(Integer.parseInt(first.substring(first.length() - 3)));
                }
                if (ids.size() > enforced) {
                    return CompletableFuture.failedFuture(tooManyOperations());
                }
                final DataValue[] values = new DataValue[ids.size()];
                Arrays.fill(values, new DataValue(Variant.NULL_VALUE));
                return CompletableFuture.completedFuture(new ReadResponse(null, values, null));
            });
        }
    }

    // --- sanitize() ---

    @Test
    void sanitize_lowercasesInput() {
        assertThat(OpcUaNodeBrowser.sanitize("Int32Node")).isEqualTo("int32node");
    }

    @Test
    void sanitize_replacesNonAlphanumericWithDash() {
        assertThat(OpcUaNodeBrowser.sanitize("My Node!@#")).isEqualTo("my-node");
    }

    @Test
    void sanitize_collapsesConsecutiveDashes() {
        assertThat(OpcUaNodeBrowser.sanitize("a---b")).isEqualTo("a-b");
    }

    @Test
    void sanitize_stripsLeadingAndTrailingDashes() {
        assertThat(OpcUaNodeBrowser.sanitize("-test-")).isEqualTo("test");
    }

    @Test
    void sanitize_mixedSpecialChars() {
        assertThat(OpcUaNodeBrowser.sanitize("CamelCase_Node.Name")).isEqualTo("camelcase-node-name");
    }

    @Test
    void sanitize_allDigits() {
        assertThat(OpcUaNodeBrowser.sanitize("12345")).isEqualTo("12345");
    }

    @Test
    void sanitize_emptyInput() {
        assertThat(OpcUaNodeBrowser.sanitize("")).isEqualTo("");
    }

    @Test
    void sanitize_onlySpecialChars() {
        assertThat(OpcUaNodeBrowser.sanitize("!@#$%")).isEqualTo("");
    }

    @Test
    void sanitize_leadingAndTrailingSpecialChars_produceNoDashes() {
        // Runs of non-alphanumeric characters at either edge collapse away entirely.
        assertThat(OpcUaNodeBrowser.sanitize("!!!ABC!!!")).isEqualTo("abc");
    }

    @Test
    void sanitize_singleCharacterInputs() {
        assertThat(OpcUaNodeBrowser.sanitize("A")).isEqualTo("a");
        assertThat(OpcUaNodeBrowser.sanitize("1")).isEqualTo("1");
        assertThat(OpcUaNodeBrowser.sanitize("-")).isEqualTo("");
    }

    @Test
    void sanitize_internalDashesCollapse() {
        // Multiple kinds of non-alphanumeric runs collapse into a single dash.
        assertThat(OpcUaNodeBrowser.sanitize("foo !!!   bar")).isEqualTo("foo-bar");
    }

    @Test
    void sanitize_alreadyKebabCase_isIdempotent() {
        assertThat(OpcUaNodeBrowser.sanitize("already-kebab-case")).isEqualTo("already-kebab-case");
    }

    @Test
    void sanitize_unicodeFallsBackToDashes() {
        // Non-ASCII alphanumeric characters are not preserved; they collapse like punctuation.
        assertThat(OpcUaNodeBrowser.sanitize("caf\u00e9-con-leche")).isEqualTo("caf-con-leche");
    }

    // --- sanitizePath() ---

    @Test
    void sanitizePath_stripsLeadingSlash() {
        assertThat(OpcUaNodeBrowser.sanitizePath("/Data/Static/Int32")).isEqualTo("data/static/int32");
    }

    @Test
    void sanitizePath_handlesNoLeadingSlash() {
        assertThat(OpcUaNodeBrowser.sanitizePath("Data/Static/Int32")).isEqualTo("data/static/int32");
    }

    @Test
    void sanitizePath_sanitizesEachSegment() {
        assertThat(OpcUaNodeBrowser.sanitizePath("/My Folder/Node Name!")).isEqualTo("my-folder/node-name");
    }

    @Test
    void sanitizePath_emptyPath() {
        assertThat(OpcUaNodeBrowser.sanitizePath("")).isEqualTo("");
    }

    @Test
    void sanitizePath_singleSegment() {
        assertThat(OpcUaNodeBrowser.sanitizePath("/Objects")).isEqualTo("objects");
    }

    // --- generateTagNameDefault (full path) ---

    @ParameterizedTest
    @CsvSource({
        "/Data/Static/Int32Node,                                  data-static-int32node",
        "/S7-1500/DataBlocksGlobal/Icon,                          s7-1500-datablocksglobal-icon",
        "/S7-1500/DataBlocksInstance/Icon,                        s7-1500-datablocksinstance-icon",
        "/Objects/My Node,                                        objects-my-node",
        "/Aliases/FindAlias/InputArguments,                       aliases-findalias-inputarguments",
        "/Aliases/TagVariables/FindAlias/InputArguments,          aliases-tagvariables-findalias-inputarguments",
    })
    void generateTagNameDefault_usesFullPath(final String path, final String expected) {
        final OpcUaNodeBrowser browser = new OpcUaNodeBrowser(null, "any");
        assertThat(browser.generateTagNameDefault(path)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "/Int32Node,    int32node",
        "/Variable,     variable",
    })
    void generateTagNameDefault_singleSegment(final String path, final String expected) {
        final OpcUaNodeBrowser browser = new OpcUaNodeBrowser(null, "any");
        assertThat(browser.generateTagNameDefault(path)).isEqualTo(expected);
    }

    @Test
    void generateTagNameDefault_emptyPath() {
        final OpcUaNodeBrowser browser = new OpcUaNodeBrowser(null, "any");
        assertThat(browser.generateTagNameDefault("")).isEqualTo("");
        assertThat(browser.generateTagNameDefault("/")).isEqualTo("");
    }

    @Test
    void generateTagNameDefault_duplicateDisplayNames_disambiguated() {
        final OpcUaNodeBrowser browser = new OpcUaNodeBrowser(null, "s7");
        // Same display name "Icon" in different folders → different tag_name_default
        assertThat(browser.generateTagNameDefault("/S7-1500/DataBlocksGlobal/Icon"))
                .isEqualTo("s7-1500-datablocksglobal-icon");
        assertThat(browser.generateTagNameDefault("/S7-1500/DataBlocksInstance/Icon"))
                .isEqualTo("s7-1500-datablocksinstance-icon");
        assertThat(browser.generateTagNameDefault("/S7-1500/TechnologicalObjects/Icon"))
                .isEqualTo("s7-1500-technologicalobjects-icon");

        // All three are unique
        assertThat(browser.generateTagNameDefault("/S7-1500/DataBlocksGlobal/Icon"))
                .isNotEqualTo(browser.generateTagNameDefault("/S7-1500/DataBlocksInstance/Icon"));
    }

    @Test
    void generateTagNameDefault_deepNesting_disambiguated() {
        final OpcUaNodeBrowser browser = new OpcUaNodeBrowser(null, "opc");
        // Same parent folder + same name, but different ancestors → unique
        assertThat(browser.generateTagNameDefault("/Aliases/FindAlias/InputArguments"))
                .isNotEqualTo(browser.generateTagNameDefault("/Aliases/TagVariables/FindAlias/InputArguments"));
    }

    @Test
    void generateTagNameDefault_specialCharsInSegments() {
        final OpcUaNodeBrowser browser = new OpcUaNodeBrowser(null, "opc");
        assertThat(browser.generateTagNameDefault("/My Folder/Sub.Folder/Node Name!"))
                .isEqualTo("my-folder-sub-folder-node-name");
    }

    // --- deduplicateDefaults ---

    @Test
    void deduplicateDefaults_noDuplicates_unchanged() {
        final List<String> input = List.of("alpha", "beta", "gamma");
        assertThat(OpcUaNodeBrowser.deduplicateDefaults(input)).containsExactly("alpha", "beta", "gamma");
    }

    @Test
    void deduplicateDefaults_allDuplicates_appendsSuffix() {
        final List<String> input = List.of("tag", "tag", "tag");
        assertThat(OpcUaNodeBrowser.deduplicateDefaults(input)).containsExactly("tag", "tag-2", "tag-3");
    }

    @Test
    void deduplicateDefaults_mixedDuplicates() {
        final List<String> input = List.of("alpha", "beta", "alpha", "gamma", "beta", "alpha");
        assertThat(OpcUaNodeBrowser.deduplicateDefaults(input))
                .containsExactly("alpha", "beta", "alpha-2", "gamma", "beta-2", "alpha-3");
    }

    @Test
    void deduplicateDefaults_emptyList() {
        assertThat(OpcUaNodeBrowser.deduplicateDefaults(List.of())).isEmpty();
    }

    @Test
    void deduplicateDefaults_singleElement() {
        assertThat(OpcUaNodeBrowser.deduplicateDefaults(List.of("only"))).containsExactly("only");
    }

    @Test
    void deduplicateDefaults_prosysSimulationScenario() {
        // Simulates the real-world case: 6 simulation instances with same path produce same default
        final List<String> input = List.of(
                "server-valuesimulations-valuesimulation-max-value",
                "server-valuesimulations-valuesimulation-max-value",
                "server-valuesimulations-valuesimulation-max-value",
                "server-valuesimulations-valuesimulation-max-value",
                "server-valuesimulations-valuesimulation-max-value",
                "server-valuesimulations-valuesimulation-max-value");
        final List<String> result = OpcUaNodeBrowser.deduplicateDefaults(input);
        assertThat(result).hasSize(6);
        assertThat(result.get(0)).isEqualTo("server-valuesimulations-valuesimulation-max-value");
        assertThat(result.get(1)).isEqualTo("server-valuesimulations-valuesimulation-max-value-2");
        assertThat(result.get(5)).isEqualTo("server-valuesimulations-valuesimulation-max-value-6");
        // All unique
        assertThat(result).doesNotHaveDuplicates();
    }

    // --- generateNorthboundTopicDefault / generateSouthboundTopicDefault ---

    @ParameterizedTest
    @CsvSource({"my-opcua, /Data/Static/Int32, my-opcua/data/static/int32", "adapter1, /Objects, adapter1/objects"})
    void generateNorthboundTopicDefault(final String adapterId, final String path, final String expected) {
        final OpcUaNodeBrowser browser = new OpcUaNodeBrowser(null, adapterId);
        assertThat(browser.generateNorthboundTopicDefault(path)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "my-opcua, /Data/Static/Int32, my-opcua/write/data/static/int32",
        "adapter1, /Objects, adapter1/write/objects"
    })
    void generateSouthboundTopicDefault(final String adapterId, final String path, final String expected) {
        final OpcUaNodeBrowser browser = new OpcUaNodeBrowser(null, adapterId);
        assertThat(browser.generateSouthboundTopicDefault(path)).isEqualTo(expected);
    }
}
