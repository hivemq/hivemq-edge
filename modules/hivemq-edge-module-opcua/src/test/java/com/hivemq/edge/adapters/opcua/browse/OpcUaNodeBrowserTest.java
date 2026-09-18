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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hivemq.edge.adapters.browse.BrowseException;
import com.hivemq.edge.adapters.browse.BrowsedNode;
import com.hivemq.edge.adapters.opcua.browse.FakeOpcUaServer.FakeBrowseServer;
import com.hivemq.edge.adapters.opcua.browse.FakeOpcUaServer.FakeReadServer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaServiceFaultException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ViewDescription;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * {@link OpcUaNodeBrowser} end to end over the fakes: root handling, the wiring of the two phases, ordering
 * and defaults of the output, the failure mapping, and one PLC-shaped scenario that exercises every part.
 * The parts themselves are tested in their own classes.
 */
class OpcUaNodeBrowserTest {

    private static @NotNull List<BrowsedNode> browse(final @NotNull FakeBrowseServer server, final int maxDepth)
            throws BrowseException {
        return new OpcUaNodeBrowser(client(server), "adapter")
                .browse(null, maxDepth)
                .toList();
    }

    // --- root ---

    @Test
    void browse_nullOrBlankRoot_startsAtObjectsFolder() throws BrowseException {
        final FakeBrowseServer server = folders(1);
        final OpcUaNodeBrowser browser = new OpcUaNodeBrowser(client(server), "adapter");

        assertThat(browser.browse(null, 0).toList()).hasSize(1);
        assertThat(browser.browse("  ", 0).toList()).hasSize(1);
        assertThat(server.browsedNodes).startsWith(NodeIds.ObjectsFolder.toParseableString());
    }

    @Test
    void browse_explicitRoot_startsThere() throws BrowseException {
        final FakeBrowseServer server = wideFolders(2, 2);

        final List<BrowsedNode> nodes = new OpcUaNodeBrowser(client(server), "adapter")
                .browse("ns=2;s=F001", 0)
                .toList();

        assertThat(nodes).extracting(BrowsedNode::nodePath).containsExactly("/V0", "/V1");
        assertThat(server.browsedNodes).startsWith("ns=2;s=F001");
    }

    @Test
    void browse_maxDepth_limitsTheTraversal() throws BrowseException {
        final FakeBrowseServer server = new FakeBrowseServer()
                .children(NodeIds.ObjectsFolder, object("ns=2;s=DB1", "DB1"))
                .children("ns=2;s=DB1", variable("ns=2;s=DB1.Struct", "Struct"))
                .children("ns=2;s=DB1.Struct", variable("ns=2;s=DB1.Struct.A", "A"));

        assertThat(browse(server, 2)).extracting(BrowsedNode::nodePath).containsExactly("/DB1/Struct");
        assertThat(browse(server, 0)).extracting(BrowsedNode::nodePath).containsExactly("/DB1/Struct", "/DB1/Struct/A");
    }

    @Test
    void browse_invalidRoot_failsBeforeTouchingTheServer() {
        final FakeBrowseServer server = folders(1);
        final OpcUaNodeBrowser browser = new OpcUaNodeBrowser(client(server), "adapter");

        assertThatThrownBy(() -> browser.browse("not a node id", 0))
                .isInstanceOf(BrowseException.class)
                .hasMessage("Invalid OPC-UA node ID: 'not a node id'");
        assertThat(server.calls).isEmpty();
    }

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
                .hasMessage("Browse operation failed")
                .cause()
                .isInstanceOf(UncheckedBrowseException.class)
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
        final FakeReadServer read = new FakeReadServer(0, Integer.MAX_VALUE);
        read.install(client);

        assertThat(new OpcUaNodeBrowser(client, "test-adapter").browse(null, 0).count())
                .isEqualTo(0);
        assertThat(read.attributeReadSizes).as("nothing to read").isEmpty();
    }

    // --- the output: order and defaults ---

    @Test
    void browse_outputIsSortedByPath() throws BrowseException {
        final FakeBrowseServer server = new FakeBrowseServer()
                .children(NodeIds.ObjectsFolder, object("ns=2;s=B", "B"), object("ns=2;s=A", "A"))
                .children("ns=2;s=B", variable("ns=2;s=B.Z", "Z"), variable("ns=2;s=B.A", "A"))
                .children("ns=2;s=A", variable("ns=2;s=A.M", "M"));

        assertThat(browse(server, 0)).extracting(BrowsedNode::nodePath).containsExactly("/A/M", "/B/A", "/B/Z");
    }

    @Test
    void browse_samePathNodes_tagNameDefaultSuffixIndependentOfArrivalOrder() throws BrowseException {
        // Two variables under the same browse path collide on tagNameDefault and get "-2" appended to one of
        // them. The order the server delivers them in may vary between browses, so the suffix must be decided
        // by a stable key (the NodeId), not by arrival order — otherwise a CSV exported from one browse names
        // a different node than the next browse would.
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

    @Test
    void browse_defaultsCarryTheAdapterId() throws BrowseException {
        final FakeBrowseServer server = new FakeBrowseServer()
                .children(NodeIds.ObjectsFolder, object("ns=2;s=Motor", "Motor"))
                .children("ns=2;s=Motor", variable("ns=2;s=Motor.Speed", "Speed"));

        assertThat(new OpcUaNodeBrowser(client(server), "plc-7").browse(null, 0).toList())
                .extracting(
                        BrowsedNode::tagNameDefault,
                        BrowsedNode::northboundTopicDefault,
                        BrowsedNode::southboundTopicDefault)
                .containsExactly(tuple("motor-speed", "plc-7/motor/speed", "plc-7/write/motor/speed"));
    }

    // --- the two phases wired together ---

    @Test
    void browse_readsTheLimitsOnce_beforeAnyBrowse_andSizesBothPhasesFromThem() throws BrowseException {
        // Codesys/WAGO shape: 100 per read, 100 per browse, 50 continuation points.
        final FakeBrowseServer server = folders(150);
        final FakeReadServer read = new FakeReadServer(100, 100, 100);
        read.advertisedContinuationPoints = 50;

        final List<BrowsedNode> nodes = new OpcUaNodeBrowser(client(server, read), "adapter")
                .browse(null, 0)
                .toList();

        assertThat(read.limitReads).isEqualTo(1);
        assertThat(server.browseSizes).containsExactly(1, 100, 50, 100, 50);
        assertThat(read.attributeReadSizes).allSatisfy(size -> assertThat(size).isLessThanOrEqualTo(100));
        assertThat(read.attributeReadSizes).containsExactly(99, 99, 99, 99, 54);
        assertThat(nodes).hasSize(150);
    }

    @Test
    void browse_limitReadFails_usesTheDefaults() throws BrowseException {
        // The OperationLimits nodes are optional. A failed read of them must not fail the browse — the default
        // batch applies.
        final FakeBrowseServer server = folders(150);
        final FakeReadServer read = new FakeReadServer(0, Integer.MAX_VALUE);
        read.limitReadFails = true;

        final List<BrowsedNode> nodes = new OpcUaNodeBrowser(client(server, read), "adapter")
                .browse(null, 0)
                .toList();

        assertThat(server.browseSizes).containsExactly(1, 100, 50, 100, 50);
        assertThat(read.attributeReadSizes).containsExactly(300, 150);
        assertThat(nodes).hasSize(150);
    }

    @Test
    void browse_phase2IsLazy_readsHappenAsTheStreamIsConsumed() throws BrowseException {
        final FakeBrowseServer server = folders(250);
        final FakeReadServer read = new FakeReadServer(0, Integer.MAX_VALUE);

        final var stream = new OpcUaNodeBrowser(client(server, read), "adapter").browse(null, 0);

        assertThat(read.attributeReadSizes)
                .as("batch 0 prefetched when the stream is handed out")
                .containsExactly(300);
        assertThat(stream.limit(1).toList()).hasSize(1);
        assertThat(read.attributeReadSizes)
                .as("batch 1 fired when batch 0 landed, nothing more")
                .containsExactly(300, 300);
    }

    @Test
    void browse_s7Shape_limitsPagingExhaustionAndNestedVariables_allTogether() throws BrowseException {
        // The lab S7-1500 in one fake: 1000 per read and browse, five continuation points, results paged, a
        // wide level of folders that overflows the pool, and struct members under Variables.
        final FakeBrowseServer server = wideFolders(12, 4).pageSize(3).continuationCapacity(5);
        server.children("ns=2;s=F000.V0", variable("ns=2;s=F000.V0.A", "A"), variable("ns=2;s=F000.V0.B", "B"));
        final FakeReadServer read = new FakeReadServer(1000, 1000, 1000);
        read.advertisedContinuationPoints = 5;

        final List<BrowsedNode> nodes =
                new OpcUaNodeBrowser(client(server, read), "s7").browse(null, 0).toList();

        assertThat(nodes).hasSize(12 * 4 + 2);
        assertThat(nodes).extracting(BrowsedNode::nodeId).doesNotHaveDuplicates();
        assertThat(nodes).extracting(BrowsedNode::nodePath).isSorted();
        assertThat(nodes)
                .extracting(BrowsedNode::nodePath)
                .contains("/F000/V0", "/F000/V0/A", "/F000/V0/B", "/F011/V3");
        assertThat(server.openContinuationPoints()).isZero();
        assertThat(server.exhaustedPerRequest).contains(7);
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

    @Test
    void browse_maxReferencesPerNode_reachesTheServer() throws BrowseException {
        final FakeBrowseServer server = folders(1);

        final List<BrowsedNode> nodes = new OpcUaNodeBrowser(client(server), "adapter", 7)
                .browse(null, 0)
                .toList();

        assertThat(nodes).hasSize(1);
        assertThat(server.requestedMaxReferences).containsOnly(7);
    }

    // --- failure mapping ---

    @Test
    void browse_serviceFault_isBrowseOperationFailedWithTheFaultAsCause() {
        final FakeBrowseServer server = folders(1).enforce(0);

        assertThatThrownBy(() -> browse(server, 0))
                .isInstanceOf(BrowseException.class)
                .hasMessage("Browse operation failed")
                .cause()
                .isInstanceOf(UaServiceFaultException.class);
    }

    @Test
    void browse_phase1Timeout_isBrowseOperationTimedOut() {
        final FakeBrowseServer server = folders(1);
        server.deferCall = 1;
        final OpcUaNodeBrowser browser = new OpcUaNodeBrowser(client(server), "adapter", 0, new Semaphore(1), 1);

        assertThatThrownBy(() -> browser.browse(null, 0))
                .isInstanceOf(BrowseException.class)
                .hasMessage("Browse operation timed out after 1 seconds")
                .cause()
                .isInstanceOf(TimeoutException.class);
    }

    @Test
    void browse_permitHeldPastTheBudget_isBrowseOperationTimedOut() throws Exception {
        final Semaphore held = new Semaphore(1);
        held.acquire();
        final OpcUaNodeBrowser browser = new OpcUaNodeBrowser(client(folders(1)), "adapter", 0, held, 1);

        assertThatThrownBy(() -> browser.browse(null, 0))
                .isInstanceOf(BrowseException.class)
                .hasMessage("Browse operation timed out after 1 seconds");
    }

    @Test
    void browse_interrupted_isBrowseOperationInterrupted_andTheFlagIsRestored() throws Exception {
        final FakeBrowseServer server = folders(1);
        server.deferCall = 1;
        final OpcUaNodeBrowser browser = new OpcUaNodeBrowser(client(server), "adapter");

        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final AtomicReference<Boolean> interrupted = new AtomicReference<>(false);
        final Thread browsing = new Thread(() -> {
            try {
                browser.browse(null, 0);
            } catch (final Throwable t) {
                failure.set(t);
                interrupted.set(Thread.currentThread().isInterrupted());
            }
        });
        browsing.start();
        Thread.sleep(300);
        browsing.interrupt();
        browsing.join(5_000);

        assertThat(browsing.isAlive()).isFalse();
        assertThat(failure.get())
                .isInstanceOf(BrowseException.class)
                .hasMessage("Browse operation interrupted")
                .cause()
                .isInstanceOf(InterruptedException.class);
        assertThat(interrupted.get()).isTrue();
    }

    @Test
    void browse_phase1Failure_isWrappedOnce_withThePathInTheCause() {
        final FakeBrowseServer server = wideFolders(2, 4).pageSize(3).continuationCapacity(0);

        assertThatThrownBy(() -> browse(server, 0))
                .isInstanceOf(BrowseException.class)
                .hasMessage("Browse operation failed")
                .cause()
                .isInstanceOf(UncheckedBrowseException.class)
                .hasMessageContaining("Browse at path '/F000'");
    }

    @Test
    void browse_phase2Failure_escapesTheStreamUnchecked() throws BrowseException {
        // Phase 2 runs inside the consumer's stream pipeline; its failure cannot be a checked BrowseException.
        // The REST layer recognises the exception by simple name.
        final FakeReadServer read = new FakeReadServer(0, 2);
        final var stream = new OpcUaNodeBrowser(client(folders(3), read), "adapter").browse(null, 0);

        assertThatThrownBy(stream::toList)
                .isInstanceOf(UncheckedBrowseException.class)
                .hasMessage("Failed to read node attributes");
        assertThat(UncheckedBrowseException.class.getSimpleName()).isEqualTo("UncheckedBrowseException");
    }

    @Test
    void browse_executionExceptionCause_isUnwrapped() {
        final FakeBrowseServer server = folders(10).enforce(5);
        server.rejectStatus = StatusCodes.Bad_ConnectionClosed;

        assertThatThrownBy(() -> browse(server, 0))
                .isInstanceOf(BrowseException.class)
                .cause()
                .isNotInstanceOf(ExecutionException.class)
                .isInstanceOf(UaServiceFaultException.class);
    }
}
