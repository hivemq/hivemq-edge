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

import static com.hivemq.edge.adapters.opcua.browse.FakeOpcUaServer.client;
import static com.hivemq.edge.adapters.opcua.browse.FakeOpcUaServer.folders;
import static com.hivemq.edge.adapters.opcua.browse.FakeOpcUaServer.object;
import static com.hivemq.edge.adapters.opcua.browse.FakeOpcUaServer.variable;
import static com.hivemq.edge.adapters.opcua.browse.FakeOpcUaServer.wideFolders;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.hivemq.edge.adapters.opcua.browse.FakeOpcUaServer.FakeBrowseServer;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaServiceFaultException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * {@link AddressSpaceWalker}: the level-wise traversal — what is collected, in which requests, and how the
 * server's refusals are retried. Driven directly, with a real {@link ChunkBrowser} over the fake server.
 */
class AddressSpaceWalkerTest {

    private static final @NotNull Deadline FAR = Deadline.after(120);

    private static @NotNull List<DiscoveredVariable> walk(
            final @NotNull FakeBrowseServer server, final @NotNull OperationLimits limits, final int maxDepth)
            throws ExecutionException, InterruptedException, TimeoutException {
        final OpcUaClient client = client(server);
        final ChunkBrowser chunkBrowser = new ChunkBrowser(client, "adapter", 0, new Semaphore(1), limits);
        return new AddressSpaceWalker(client, "adapter", chunkBrowser, limits)
                .walk(NodeIds.ObjectsFolder, maxDepth == 0 ? Integer.MAX_VALUE : maxDepth, FAR);
    }

    private static @NotNull List<DiscoveredVariable> walk(final @NotNull FakeBrowseServer server)
            throws ExecutionException, InterruptedException, TimeoutException {
        return walk(server, OperationLimits.NONE, 0);
    }

    private static @NotNull String id(final @NotNull DiscoveredVariable variable) {
        return variable.nodeId().toParseableString();
    }

    // --- what is collected ---

    @Test
    void walk_variablesUnderVariables_areDiscovered() throws Exception {
        // Struct members, array elements and properties are Variables whose parent is a Variable. Before
        // EDG-1034 none of them was returned because the traversal marked a Variable visited when it recorded
        // it and then refused to browse it.
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

        assertThat(walk(server))
                .extracting(AddressSpaceWalkerTest::id, DiscoveredVariable::path)
                .containsExactly(
                        tuple("ns=2;s=DB1.Struct", "/DB1/Struct"),
                        tuple("ns=2;s=DB1.Struct.A", "/DB1/Struct/A"),
                        tuple("ns=2;s=DB1.Struct.Arr", "/DB1/Struct/Arr"),
                        tuple("ns=2;s=DB1.Struct.Arr[0]", "/DB1/Struct/Arr/0"),
                        tuple("ns=2;s=DB1.Struct.Arr[1]", "/DB1/Struct/Arr/1"));
    }

    @Test
    void walk_recordsNamespaceAndBrowseName() throws Exception {
        final FakeBrowseServer server =
                new FakeBrowseServer().children(NodeIds.ObjectsFolder, variable("ns=2;s=Motor Speed", "Motor Speed"));

        assertThat(walk(server))
                .extracting(
                        DiscoveredVariable::namespaceUri,
                        DiscoveredVariable::namespaceIndex,
                        DiscoveredVariable::browseName)
                .containsExactly(tuple("urn:test:plc", 2, "Motor Speed"));
    }

    @Test
    void walk_referenceWithoutBrowseName_getsAnEmptySegment() throws Exception {
        final ReferenceDescription nameless = new ReferenceDescription(
                NodeIds.HasComponent,
                true,
                ExpandedNodeId.parse("ns=2;s=X"),
                QualifiedName.NULL_VALUE,
                LocalizedText.NULL_VALUE,
                NodeClass.Variable,
                ExpandedNodeId.NULL_VALUE);
        final FakeBrowseServer server = new FakeBrowseServer().children(NodeIds.ObjectsFolder, nameless);

        assertThat(walk(server)).extracting(DiscoveredVariable::path).containsExactly("/");
    }

    @Test
    void walk_referenceWithNoQualifiedNameAtAll_getsAnEmptySegment() throws Exception {
        final ReferenceDescription nameless = new ReferenceDescription(
                NodeIds.HasComponent,
                true,
                ExpandedNodeId.parse("ns=2;s=X"),
                null,
                LocalizedText.NULL_VALUE,
                NodeClass.Variable,
                ExpandedNodeId.NULL_VALUE);
        final FakeBrowseServer server = new FakeBrowseServer().children(NodeIds.ObjectsFolder, nameless);

        assertThat(walk(server)).extracting(DiscoveredVariable::path).containsExactly("/");
    }

    @Test
    void walk_namespaceIndexBeyondTheClientsTable_isReportedByNumber() throws Exception {
        // The client's table has three entries (0..2); a reference into ns=7 still resolves to a NodeId.
        final FakeBrowseServer server =
                new FakeBrowseServer().children(NodeIds.ObjectsFolder, variable("ns=7;s=Far", "Far"));

        assertThat(walk(server))
                .extracting(DiscoveredVariable::namespaceUri, DiscoveredVariable::namespaceIndex)
                .containsExactly(tuple("7", 7));
    }

    @Test
    void walk_referenceIntoAnUnknownNamespace_isSkipped() throws Exception {
        // ExpandedNodeId with a namespace URI the client's table does not know cannot be turned into a NodeId.
        final ReferenceDescription foreign = new ReferenceDescription(
                NodeIds.HasComponent,
                true,
                new ExpandedNodeId(
                        ExpandedNodeId.ServerReference.of(0),
                        ExpandedNodeId.NamespaceReference.of("urn:elsewhere"),
                        "X"),
                new QualifiedName(2, "X"),
                LocalizedText.english("X"),
                NodeClass.Variable,
                ExpandedNodeId.NULL_VALUE);
        final FakeBrowseServer server =
                new FakeBrowseServer().children(NodeIds.ObjectsFolder, foreign, variable("ns=2;s=Y", "Y"));

        assertThat(walk(server)).extracting(AddressSpaceWalkerTest::id).containsExactly("ns=2;s=Y");
    }

    @Test
    void walk_objectsAreTraversedButNotRecorded() throws Exception {
        final FakeBrowseServer server = folders(3);

        assertThat(walk(server)).extracting(DiscoveredVariable::path).containsExactly("/F000/V", "/F001/V", "/F002/V");
    }

    @Test
    void walk_maxDepth_countsVariableLevelsToo() throws Exception {
        final FakeBrowseServer server = new FakeBrowseServer()
                .children(NodeIds.ObjectsFolder, object("ns=2;s=DB1", "DB1"))
                .children("ns=2;s=DB1", variable("ns=2;s=DB1.Struct", "Struct"))
                .children("ns=2;s=DB1.Struct", variable("ns=2;s=DB1.Struct.A", "A"));

        assertThat(walk(server, OperationLimits.NONE, 1)).isEmpty();
        assertThat(walk(server, OperationLimits.NONE, 2))
                .extracting(AddressSpaceWalkerTest::id)
                .containsExactly("ns=2;s=DB1.Struct");
        assertThat(walk(server, OperationLimits.NONE, 3))
                .extracting(AddressSpaceWalkerTest::id)
                .containsExactly("ns=2;s=DB1.Struct", "ns=2;s=DB1.Struct.A");
    }

    @Test
    void walk_nodeReachableThroughTwoPaths_emittedOnceUnderTheFirstPath() throws Exception {
        // Shared sits under both A and B. Breadth-first, A is browsed before B, so /A/Shared is the path it is
        // recorded under; the second sighting neither re-records nor re-traverses it.
        final FakeBrowseServer server = new FakeBrowseServer()
                .children(NodeIds.ObjectsFolder, object("ns=2;s=A", "A"), object("ns=2;s=B", "B"))
                .children("ns=2;s=A", variable("ns=2;s=Shared", "Shared"))
                .children("ns=2;s=B", variable("ns=2;s=Shared", "Shared"))
                .children("ns=2;s=Shared", variable("ns=2;s=Shared.Child", "Child"));

        assertThat(walk(server))
                .extracting(AddressSpaceWalkerTest::id, DiscoveredVariable::path)
                .containsExactly(tuple("ns=2;s=Shared", "/A/Shared"), tuple("ns=2;s=Shared.Child", "/A/Shared/Child"));
        assertThat(server.browsedNodes).containsOnlyOnce("ns=2;s=Shared");
    }

    @Test
    void walk_cycleBackToTheRoot_terminates() throws Exception {
        final FakeBrowseServer server = new FakeBrowseServer()
                .children(NodeIds.ObjectsFolder, object("ns=2;s=A", "A"))
                .children(
                        "ns=2;s=A",
                        object(NodeIds.ObjectsFolder.toParseableString(), "Objects"),
                        variable("ns=2;s=V", "V"));

        assertThat(walk(server)).extracting(DiscoveredVariable::path).containsExactly("/A/V");
        assertThat(server.browsedNodes).containsOnlyOnce(NodeIds.ObjectsFolder.toParseableString());
    }

    @Test
    void walk_emptyRoot_returnsNothing() throws Exception {
        final FakeBrowseServer server = new FakeBrowseServer()
                .status(
                        NodeIds.ObjectsFolder,
                        new BrowseResult(StatusCode.GOOD, ByteString.NULL_VALUE, new ReferenceDescription[0]));

        assertThat(walk(server)).isEmpty();
    }

    // --- how the requests are shaped ---

    @Test
    void walk_levelIsBrowsedInOneRequest_chunkedToMaxNodesPerBrowse() throws Exception {
        // 250 folders under Objects, each holding one variable. The folder level must go out in requests of at
        // most the advertised MaxNodesPerBrowse (here 60, below the default 100), not one request per folder.
        final FakeBrowseServer server = folders(250);

        final List<DiscoveredVariable> variables = walk(server, new OperationLimits(0, 60, 0), 0);

        assertThat(server.browseSizes)
                .as("nodes per Browse request")
                .containsExactly(1, 60, 60, 60, 60, 10, 60, 60, 60, 60, 10);
        assertThat(variables).hasSize(250);
    }

    @Test
    void walk_limitNotAdvertised_serverRejectsBrowse_halvesChunkAndRetries() throws Exception {
        // Nothing advertised, so the whole 250-node level goes in one request, capped at the default 100 per
        // request; the server accepts at most 40. Rejected chunks are halved and re-issued, nothing is lost.
        final FakeBrowseServer server = folders(250).enforce(40);

        final List<DiscoveredVariable> variables = walk(server);

        assertThat(server.browseSizes).startsWith(1, 100, 50, 25).allSatisfy(n -> assertThat(n)
                .isLessThanOrEqualTo(100));
        assertThat(server.browseSizes.stream().filter(n -> n > 40).count())
                .as("rejected requests")
                .isEqualTo(2);
        assertThat(variables).hasSize(250);
        assertThat(variables).extracting(AddressSpaceWalkerTest::id).doesNotHaveDuplicates();
    }

    @Test
    void walk_responseTooLargeForTheChannel_halvesTheChunkLikeTooManyOperations() throws Exception {
        // A folder level of 250 nodes whose Browse response does not fit the negotiated message size: the
        // server answers Bad_ResponseTooLarge (or Bad_RequestTooLarge / Bad_EncodingLimitsExceeded /
        // Bad_TcpMessageTooLarge) — the cure is the same as for too many operations: send less per request.
        final FakeBrowseServer server = folders(250).enforce(40);
        server.rejectStatus = StatusCodes.Bad_ResponseTooLarge;

        final List<DiscoveredVariable> variables = walk(server);

        assertThat(server.browseSizes).startsWith(1, 100, 50, 25);
        assertThat(variables).hasSize(250);
    }

    @Test
    void walk_serverRejectsASingleNodeBrowse_fails() {
        final FakeBrowseServer server = folders(1).enforce(0);

        assertThatThrownBy(() -> walk(server))
                .isInstanceOf(ExecutionException.class)
                .cause()
                .isInstanceOf(UaServiceFaultException.class);
        assertThat(server.browseSizes).containsExactly(1);
    }

    @Test
    void walk_otherFaults_propagateWithoutHalving() {
        final FakeBrowseServer server = folders(10).enforce(5);
        server.rejectStatus = StatusCodes.Bad_ConnectionClosed;

        assertThatThrownBy(() -> walk(server)).isInstanceOf(ExecutionException.class);
        assertThat(server.browseSizes).containsExactly(1, 10);
    }

    @Test
    void walk_continuationPoints_drainedBeforeTheNextLevelIsRequested() throws Exception {
        // Server pages every result at 3 references. The folder level (5 folders) needs a BrowseNext; the
        // variable level must not be requested until that page has been drained (EDG-465: the S7-1500 expires
        // continuation points that are left waiting behind other browses).
        final FakeBrowseServer server = folders(5).pageSize(3);

        final List<DiscoveredVariable> variables = walk(server);

        // root page 1 -> its continuation -> the 5 folders in one request -> their 5 variables (leaves)
        assertThat(server.calls).containsExactly("browse[1]", "next[1]", "browse[5]", "browse[5]");
        assertThat(variables).hasSize(5);
    }

    // --- nodes the server could not page ---

    @Test
    void walk_serverOutOfContinuationPoints_rebrowsesThoseNodesInSmallerChunks() throws Exception {
        // The lab S7-1500 advertises MaxBrowseContinuationPoints = 5 and pages results: a level chunk of 100
        // folders with more than five of them overflowing answers Bad_NoContinuationPoints for the rest. Those
        // nodes are browsed again five at a time; nothing is lost and nothing is browsed twice successfully.
        final FakeBrowseServer server = wideFolders(12, 4).pageSize(3).continuationCapacity(5);

        final List<DiscoveredVariable> variables = walk(server, new OperationLimits(0, 0, 5), 0);

        assertThat(variables).hasSize(12 * 4);
        assertThat(variables).extracting(AddressSpaceWalkerTest::id).doesNotHaveDuplicates();
        // root, then the 12 folders in one request (5 paged, 7 refused), then the 7 again as 5 + 2, then leaves
        assertThat(server.browseSizes).startsWith(1, 12, 5, 2);
        assertThat(server.exhaustedPerRequest).startsWith(0, 7, 0, 0);
    }

    @Test
    void walk_serverOutOfContinuationPoints_noCapacityAdvertised_halvesTheChunk() throws Exception {
        final FakeBrowseServer server = wideFolders(12, 4).pageSize(3).continuationCapacity(5);

        final List<DiscoveredVariable> variables = walk(server);

        assertThat(variables).hasSize(12 * 4);
        // 12 -> 7 refused -> retried 6 at a time (5 + 1 refused) -> the one again in a chunk of 3
        assertThat(server.browseSizes).startsWith(1, 12, 6, 1, 1);
    }

    @Test
    void walk_anotherClientHoldsAllContinuationPoints_retriesAloneAndSucceeds() throws Exception {
        // Two paged folders; the server refuses continuation points for the first three requests as if another
        // session held the whole pool, then frees up. The browse must wait it out, not fail.
        final FakeBrowseServer server = wideFolders(2, 4).pageSize(3);
        server.refuseContinuationsForRequests = 3; // root browse counts too: the level and one retry round

        final List<DiscoveredVariable> variables = walk(server);

        assertThat(variables).hasSize(8);
        assertThat(variables).extracting(AddressSpaceWalkerTest::id).doesNotHaveDuplicates();
    }

    @Test
    void walk_singleNodeStillOutOfContinuationPoints_failsWithThePath() {
        final FakeBrowseServer server = wideFolders(2, 4).pageSize(3).continuationCapacity(0);

        assertThatThrownBy(() -> walk(server))
                .isInstanceOf(UncheckedBrowseException.class)
                .hasMessageContaining("Browse at path '/F000'")
                .hasMessageContaining("Bad_NoContinuationPoints after 3 retries");
        // the level of two, then both alone: once straight away, then the three paused retries
        assertThat(server.browseSizes).containsExactly(1, 2, 1, 1, 1, 1, 1, 1, 1, 1);
    }

    @Test
    void walk_singleNodeRetries_pauseIsBoundedByTheDeadline() {
        // With no time left the pauses collapse to zero and the retries still stop after three.
        final FakeBrowseServer server = wideFolders(1, 4).pageSize(3).continuationCapacity(0);
        final OpcUaClient client = client(server);
        final ChunkBrowser chunkBrowser =
                new ChunkBrowser(client, "adapter", 0, new Semaphore(1), OperationLimits.NONE);
        final Deadline now = new Deadline(System.nanoTime());

        final long start = System.nanoTime();
        assertThatThrownBy(() -> new AddressSpaceWalker(client, "adapter", chunkBrowser, OperationLimits.NONE)
                        .walk(NodeIds.ObjectsFolder, Integer.MAX_VALUE, now))
                .isInstanceOf(UncheckedBrowseException.class)
                .hasMessageContaining("after 3 retries");
        assertThat(System.nanoTime() - start).isLessThan(TimeUnit.SECONDS.toNanos(1));
    }

    @Test
    void walk_nonGoodRootStatus_failsWithThePath() {
        final FakeBrowseServer server = new FakeBrowseServer()
                .status(
                        NodeIds.ObjectsFolder,
                        new BrowseResult(
                                new StatusCode(StatusCodes.Bad_TooManyOperations),
                                ByteString.NULL_VALUE,
                                new ReferenceDescription[0]));

        assertThatThrownBy(() -> walk(server))
                .isInstanceOf(UncheckedBrowseException.class)
                .hasMessageContaining("Browse at path '' returned non-Good status")
                .hasMessageContaining("Bad_TooManyOperations");
    }
}
