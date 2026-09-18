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
import static com.hivemq.edge.adapters.opcua.browse.FakeOpcUaServer.folderChunk;
import static com.hivemq.edge.adapters.opcua.browse.FakeOpcUaServer.wideFolders;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hivemq.edge.adapters.opcua.browse.FakeOpcUaServer.FakeBrowseServer;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaServiceFaultException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * {@link ChunkBrowser}: one Browse for a chunk of folder nodes, its pages, and the cursor accounting. Every
 * test drives the class directly with a chunk of {@code F000..} folders whose children the fake pages.
 */
class ChunkBrowserTest {

    private static final @NotNull Deadline FAR = Deadline.after(120);
    private static final @NotNull OperationLimits NO_LIMITS = OperationLimits.NONE;

    private static @NotNull OperationLimits continuationPoints(final int advertised) {
        return new OperationLimits(0, 0, advertised);
    }

    private static @NotNull ChunkBrowser browser(
            final @NotNull FakeBrowseServer server, final @NotNull OperationLimits limits) {
        return new ChunkBrowser(client(server), "adapter", 0, new Semaphore(1), limits);
    }

    private static @NotNull BrowseResult bad(final long status) {
        return new BrowseResult(new StatusCode(status), ByteString.NULL_VALUE, new ReferenceDescription[0]);
    }

    private static int references(final @NotNull ChunkBrowser.ChunkResult result) {
        return result.references().stream()
                .mapToInt(refs -> refs == null ? 0 : refs.size())
                .sum();
    }

    // --- the happy path: request, pages, permit ---

    @Test
    void browse_pagedNodes_drainsEveryContinuationPageUnderThePermit() throws Exception {
        final FakeBrowseServer server = wideFolders(2, 7).pageSize(3);
        final Semaphore permit = new Semaphore(1);
        final ChunkBrowser browser = new ChunkBrowser(client(server), "adapter", 0, permit, NO_LIMITS);

        final ChunkBrowser.ChunkResult result = browser.browse(folderChunk(2), FAR);

        assertThat(result.references()).allSatisfy(refs -> assertThat(refs).hasSize(7));
        assertThat(result.exhausted()).isEmpty();
        // 7 references at 3 a page: Browse, then two BrowseNext rounds carrying both cursors
        assertThat(server.calls).containsExactly("browse[2]", "next[2]", "next[2]");
        assertThat(server.openContinuationPoints()).isZero();
        assertThat(permit.availablePermits()).as("permit returned").isEqualTo(1);
    }

    @Test
    void browse_maxReferencesPerNode_isPassedToTheServer() throws Exception {
        final FakeBrowseServer server = wideFolders(1, 4);
        final ChunkBrowser browser = new ChunkBrowser(client(server), "adapter", 7, new Semaphore(1), NO_LIMITS);

        browser.browse(folderChunk(1), FAR);

        assertThat(server.requestedMaxReferences).containsExactly(7);
    }

    @Test
    void browse_successfulChunk_releasesNothing() throws Exception {
        final FakeBrowseServer server = wideFolders(2, 7).pageSize(3);

        browser(server, NO_LIMITS).browse(folderChunk(2), FAR);

        assertThat(server.calls).noneMatch(c -> c.startsWith("release"));
        assertThat(server.openContinuationPoints()).isZero();
    }

    // --- capacity: how many cursors a BrowseNext may carry ---

    @Test
    void browse_serverHandsOutMoreCursorsThanBrowseNextAccepts_drainsInBatchesOfTheAdvertisedCapacity()
            throws Exception {
        // Milo (and who knows which PLC) hands out six cursors in one Browse response but refuses a BrowseNext
        // carrying more than its advertised five. Found by OpcUaNodeBrowserServerLimitsIT: the six-point
        // BrowseNext was refused, the release of six was refused too, and the leaked cursors starved every
        // later browse. Drain — and release — in batches of the advertised capacity.
        final FakeBrowseServer server = wideFolders(6, 4).pageSize(3).continuationCapacity(6);
        server.maxPointsPerBrowseNext = 5;

        final ChunkBrowser.ChunkResult result =
                browser(server, continuationPoints(5)).browse(folderChunk(6), FAR);

        assertThat(references(result)).isEqualTo(24);
        assertThat(server.calls).containsExactly("browse[6]", "next[5]", "next[1]");
        assertThat(server.openContinuationPoints()).isZero();
    }

    @Test
    void browse_browseNextRefusedAsTooManyOperations_halvesThePointBatch() throws Exception {
        // Nothing advertised, so the first BrowseNext carries all six cursors; the server refuses it and the
        // batch is halved until it goes through. No cursor is lost.
        final FakeBrowseServer server = wideFolders(6, 4).pageSize(3).continuationCapacity(6);
        server.maxPointsPerBrowseNext = 5;

        final ChunkBrowser.ChunkResult result = browser(server, NO_LIMITS).browse(folderChunk(6), FAR);

        assertThat(references(result)).isEqualTo(24);
        assertThat(server.calls).containsExactly("browse[6]", "next[6]", "next[3]", "next[3]");
        assertThat(server.openContinuationPoints()).isZero();
    }

    @Test
    void browse_singleCursorBrowseNextRefused_failsWithTheFault() {
        final FakeBrowseServer server = wideFolders(1, 4).pageSize(3);
        server.maxPointsPerBrowseNext = 0;

        assertThatThrownBy(() -> browser(server, NO_LIMITS).browse(folderChunk(1), FAR))
                .isInstanceOf(ExecutionException.class)
                .cause()
                .isInstanceOf(UaServiceFaultException.class);
        assertThat(server.calls).containsExactly("browse[1]", "next[1]", "release[1]");
    }

    // --- Bad_NoContinuationPoints is reported, not thrown ---

    @Test
    void browse_serverOutOfContinuationPoints_reportsThoseNodesAsExhausted() throws Exception {
        // Five cursors in the pool, six paging folders: the sixth gets Bad_NoContinuationPoints. It comes back
        // in exhausted with a null reference list; the other five are drained normally.
        final FakeBrowseServer server = wideFolders(6, 4).pageSize(3).continuationCapacity(5);

        final ChunkBrowser.ChunkResult result =
                browser(server, continuationPoints(5)).browse(folderChunk(6), FAR);

        assertThat(result.exhausted()).extracting(PendingNode::path).containsExactly("/F005");
        assertThat(result.references().get(5)).isNull();
        assertThat(references(result)).isEqualTo(20);
        assertThat(server.openContinuationPoints()).isZero();
    }

    // --- failures release every cursor still open ---

    @Test
    void browse_badSiblingInAChunk_releasesTheOtherNodesContinuationPoints() {
        // F000 pages (the server hands out a cursor), F001 answers Bad_NodeIdUnknown in the same response. The
        // browse fails — and must hand F000's cursor back (BrowseNext with releaseContinuationPoints) instead of
        // leaving it for the server to time out; the S7-1500 has five of them per session.
        final FakeBrowseServer server = wideFolders(2, 4).pageSize(3);
        server.status(NodeId.parse("ns=2;s=F001"), bad(StatusCodes.Bad_NodeIdUnknown));

        assertThatThrownBy(() -> browser(server, NO_LIMITS).browse(folderChunk(2), FAR))
                .isInstanceOf(UncheckedBrowseException.class)
                .hasMessageContaining("Browse at path '/F001'")
                .hasMessageContaining("Bad_NodeIdUnknown");
        assertThat(server.calls).containsExactly("browse[2]", "release[1]");
        assertThat(server.openContinuationPoints())
                .as("no cursor left open on the server")
                .isZero();
    }

    @Test
    void browse_badContinuationPage_releasesTheOtherNodesRemainingCursors() {
        // Two paged folders; F001's continuation page fails while F000 still has a page to go. F000's fresh
        // cursor from the same BrowseNext response must be released.
        final FakeBrowseServer server = wideFolders(2, 7).pageSize(3);
        server.nextStatus = new StatusCode(StatusCodes.Bad_ContinuationPointInvalid);
        server.nextStatusOwnerPrefix = "ns=2;s=F001";

        assertThatThrownBy(() -> browser(server, NO_LIMITS).browse(folderChunk(2), FAR))
                .isInstanceOf(UncheckedBrowseException.class)
                .hasMessageContaining("Browse continuation at path '/F001'")
                .hasMessageContaining("Bad_ContinuationPointInvalid");
        assertThat(server.calls).containsExactly("browse[2]", "next[2]", "release[1]");
        assertThat(server.openContinuationPoints()).isZero();
    }

    @Test
    void browse_serverReturnsFewerResultsThanRequested_failsInsteadOfAssumingNoChildren() {
        final FakeBrowseServer server = wideFolders(2, 4).pageSize(3);
        server.dropLastResult = true;

        assertThatThrownBy(() -> browser(server, NO_LIMITS).browse(folderChunk(2), FAR))
                .isInstanceOf(UncheckedBrowseException.class)
                .hasMessage("Browse at path '/F001' returned no result");
        // F000's cursor from the truncated response is released; the one the server allocated for the result
        // it then dropped is invisible to the client and stays the server's problem
        assertThat(server.calls).containsExactly("browse[2]", "release[1]");
        assertThat(server.openContinuationPoints()).isEqualTo(1);
    }

    @Test
    void browse_continuationPageMissing_failsWithThePath() {
        final FakeBrowseServer server = wideFolders(2, 7).pageSize(3);
        server.dropLastNextResult = true;

        assertThatThrownBy(() -> browser(server, NO_LIMITS).browse(folderChunk(2), FAR))
                .isInstanceOf(UncheckedBrowseException.class)
                .hasMessage("Browse continuation at path '/F001' returned no result");
        // F000's fresh cursor from the truncated page is released; F001's, dropped with its page, cannot be
        assertThat(server.calls).containsExactly("browse[2]", "next[2]", "release[1]");
        assertThat(server.openContinuationPoints()).isEqualTo(1);
    }

    @Test
    void browse_releaseOnFailure_isBatchedToTheAdvertisedCapacity() {
        // Six open cursors, a bad sibling, capacity five: the release must go out as 5 + 1 — a single request
        // of six would be refused and leak all of them.
        final FakeBrowseServer server = wideFolders(7, 4).pageSize(3).continuationCapacity(6);
        server.maxPointsPerBrowseNext = 5;
        server.status(NodeId.parse("ns=2;s=F006"), bad(StatusCodes.Bad_NodeIdUnknown));

        assertThatThrownBy(() -> browser(server, continuationPoints(5)).browse(folderChunk(7), FAR))
                .isInstanceOf(UncheckedBrowseException.class);
        assertThat(server.calls).containsExactly("browse[7]", "release[5]", "release[1]");
        assertThat(server.openContinuationPoints()).isZero();
    }

    @Test
    void browse_releaseOnFailure_noCapacityAdvertised_releasesOnePerRequest() {
        final FakeBrowseServer server = wideFolders(4, 4).pageSize(3).continuationCapacity(6);
        server.maxPointsPerBrowseNext = 2;
        server.status(NodeId.parse("ns=2;s=F003"), bad(StatusCodes.Bad_NodeIdUnknown));

        assertThatThrownBy(() -> browser(server, NO_LIMITS).browse(folderChunk(4), FAR))
                .isInstanceOf(UncheckedBrowseException.class);
        assertThat(server.calls).containsExactly("browse[4]", "release[1]", "release[1]", "release[1]");
        assertThat(server.openContinuationPoints()).isZero();
    }

    @Test
    void browse_releaseRefusedAsTooManyOperations_isHalvedUntilItGoesThrough() {
        // Six open cursors, advertised capacity five, real capacity two: the release of five is refused as a
        // whole. Trusting the advertisement would leave five cursors allocated on the server.
        final FakeBrowseServer server = wideFolders(7, 4).pageSize(3).continuationCapacity(6);
        server.maxPointsPerBrowseNext = 2;
        server.status(NodeId.parse("ns=2;s=F006"), bad(StatusCodes.Bad_NodeIdUnknown));

        assertThatThrownBy(() -> browser(server, continuationPoints(5)).browse(folderChunk(7), FAR))
                .isInstanceOf(UncheckedBrowseException.class);
        // 5 refused -> 2 + 3; 3 refused -> 1 + 2; then the sixth on its own
        assertThat(server.calls)
                .containsExactly(
                        "browse[7]",
                        "release[5]",
                        "release[2]",
                        "release[3]",
                        "release[1]",
                        "release[2]",
                        "release[1]");
        assertThat(server.openContinuationPoints()).isZero();
    }

    @Test
    void browse_releaseOfASingleCursorRefused_isNotRetried() {
        // One cursor per request is as small as it gets; a server refusing even that is logged, not looped on.
        final FakeBrowseServer server = wideFolders(2, 4).pageSize(3);
        server.maxPointsPerBrowseNext = 0;
        server.status(NodeId.parse("ns=2;s=F001"), bad(StatusCodes.Bad_NodeIdUnknown));

        assertThatThrownBy(() -> browser(server, NO_LIMITS).browse(folderChunk(2), FAR))
                .isInstanceOf(UncheckedBrowseException.class);
        assertThat(server.calls).containsExactly("browse[2]", "release[1]");
    }

    // --- the permit wait is bounded by the deadline ---

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD) // an unbounded wait would hang here
    void browse_waitingForThePermit_countsAgainstTheDeadline() throws Exception {
        // Another browse on the same adapter holds the permit for longer than this browse's timeout. The
        // waiting browse must fail with the timeout, not sit on the permit until the other one is done.
        final Semaphore shared = new Semaphore(1);
        shared.acquire(); // held by "another browse" for the whole test
        final FakeBrowseServer server = wideFolders(1, 1);
        final ChunkBrowser browser = new ChunkBrowser(client(server), "adapter", 0, shared, NO_LIMITS);

        final long start = System.nanoTime();
        assertThatThrownBy(() -> browser.browse(folderChunk(1), Deadline.after(1)))
                .isInstanceOf(TimeoutException.class)
                .hasMessage("Timed out waiting for the browse permit of adapter 'adapter'");
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)).isLessThan(5_000);
        assertThat(shared.availablePermits())
                .as("the waiter never took the permit")
                .isZero();
        assertThat(server.calls).isEmpty();
    }

    @Test
    void browse_waitingForThePermit_isInterruptible() throws Exception {
        final Semaphore shared = new Semaphore(1);
        shared.acquire();
        final ChunkBrowser browser = new ChunkBrowser(client(wideFolders(1, 1)), "adapter", 0, shared, NO_LIMITS);

        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final Thread waiter = new Thread(() -> {
            try {
                browser.browse(folderChunk(1), FAR);
            } catch (final Throwable t) {
                failure.set(t);
            }
        });
        waiter.start();
        Thread.sleep(300); // let it block on the permit
        waiter.interrupt();
        waiter.join(5_000);

        assertThat(waiter.isAlive())
                .as("an interrupted waiter gives up promptly")
                .isFalse();
        assertThat(failure.get()).isInstanceOf(InterruptedException.class);
    }

    // --- responses that land after the wait for them ended ---

    @Test
    void browse_browseResponseArrivesAfterTheTimeout_itsCursorsAreReleasedWhenItLands() {
        // The Browse for F000/F001 hands out two cursors but its response only lands after the browse gave
        // up on it. Nothing waits for that response any more — it must still hand the cursors back.
        final FakeBrowseServer server = wideFolders(2, 4).pageSize(3);
        server.deferCall = 1;
        final Semaphore permit = new Semaphore(1);
        final ChunkBrowser browser = new ChunkBrowser(client(server), "adapter", 0, permit, NO_LIMITS);

        assertThatThrownBy(() -> browser.browse(folderChunk(2), Deadline.after(1)))
                .isInstanceOf(TimeoutException.class);
        assertThat(server.calls).containsExactly("browse[2]");
        assertThat(server.openContinuationPoints())
                .as("allocated by the server, response still in flight")
                .isEqualTo(2);
        assertThat(permit.availablePermits())
                .as("the permit is not held for a response nobody waits for")
                .isEqualTo(1);

        server.completeDeferred();

        assertThat(server.calls).containsExactly("browse[2]", "release[1]", "release[1]");
        assertThat(server.openContinuationPoints()).isZero();
    }

    @Test
    void browse_browseNextResponseArrivesAfterTheTimeout_itsFreshCursorsAreReleasedWhenItLands() {
        // Two folders of seven, paged by three: the first BrowseNext consumes both cursors and hands out two
        // fresh ones, but only after the browse timed out. The stale two are released at once (the request may
        // never have reached the server), the fresh two when the late response shows them.
        final FakeBrowseServer server = wideFolders(2, 7).pageSize(3);
        server.deferCall = 2; // browse[2], next[2]
        final ChunkBrowser browser = browser(server, NO_LIMITS);

        assertThatThrownBy(() -> browser.browse(folderChunk(2), Deadline.after(1)))
                .isInstanceOf(TimeoutException.class);
        assertThat(server.calls).containsExactly("browse[2]", "next[2]", "release[1]", "release[1]");
        assertThat(server.openContinuationPoints())
                .as("the fresh cursors of the in-flight BrowseNext")
                .isEqualTo(2);

        server.completeDeferred();

        assertThat(server.calls)
                .containsExactly("browse[2]", "next[2]", "release[1]", "release[1]", "release[1]", "release[1]");
        assertThat(server.openContinuationPoints()).isZero();
    }

    @Test
    void browse_interruptedWhileWaitingForABrowseResponse_itsCursorsAreReleasedWhenItLands() throws Exception {
        final FakeBrowseServer server = wideFolders(2, 4).pageSize(3);
        server.deferCall = 1;
        final ChunkBrowser browser = browser(server, NO_LIMITS);

        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final Thread browsing = new Thread(() -> {
            try {
                browser.browse(folderChunk(2), FAR);
            } catch (final Throwable t) {
                failure.set(t);
            }
        });
        browsing.start();
        Thread.sleep(300); // let it block on the deferred response
        browsing.interrupt();
        browsing.join(5_000);

        assertThat(browsing.isAlive()).isFalse();
        assertThat(failure.get()).isInstanceOf(InterruptedException.class);
        assertThat(server.calls).containsExactly("browse[2]");

        server.completeDeferred();

        assertThat(server.calls).containsExactly("browse[2]", "release[1]", "release[1]");
        assertThat(server.openContinuationPoints()).isZero();
    }

    @Test
    void browse_lateResponseIsAFailure_nothingToRelease() {
        // The deferred Browse completes with a fault after the timeout: no cursors were handed out.
        final FakeBrowseServer server = wideFolders(1, 1);
        server.deferCall = 1;
        server.deferredFault = StatusCodes.Bad_ConnectionClosed;
        final ChunkBrowser browser = browser(server, NO_LIMITS);

        assertThatThrownBy(() -> browser.browse(folderChunk(1), Deadline.after(1)))
                .isInstanceOf(TimeoutException.class);

        server.completeDeferred();

        assertThat(server.calls).containsExactly("browse[1]");
    }

    @Test
    void browse_browseFailsAsAWhole_propagatesTheFault() {
        final FakeBrowseServer server = wideFolders(2, 1).enforce(1);

        assertThatThrownBy(() -> browser(server, NO_LIMITS).browse(folderChunk(2), FAR))
                .isInstanceOf(ExecutionException.class)
                .cause()
                .isInstanceOf(UaServiceFaultException.class);
        assertThat(server.calls).containsExactly("browse[2]");
    }

    // --- what a server may leave out of a response ---

    @Test
    void browse_nullResultsArray_failsLikeAMissingResult() {
        final FakeBrowseServer server = wideFolders(1, 1);
        server.nullResultsFromCall = 1;

        assertThatThrownBy(() -> browser(server, NO_LIMITS).browse(folderChunk(1), FAR))
                .isInstanceOf(UncheckedBrowseException.class)
                .hasMessage("Browse at path '/F000' returned no result");
    }

    @Test
    void browse_nullResultsOnAContinuationPage_failsWithThePath() {
        final FakeBrowseServer server = wideFolders(1, 4).pageSize(3);
        server.nullResultsFromCall = 2; // the Browse answers normally, the BrowseNext's results array is null

        assertThatThrownBy(() -> browser(server, NO_LIMITS).browse(folderChunk(1), FAR))
                .isInstanceOf(UncheckedBrowseException.class)
                .hasMessage("Browse continuation at path '/F000' returned no result");
    }

    @Test
    void browse_resultsWithoutAStatusCode_areTakenAsGood() throws Exception {
        final FakeBrowseServer server = wideFolders(2, 7).pageSize(3);
        server.nullStatus = true;

        final ChunkBrowser.ChunkResult result = browser(server, NO_LIMITS).browse(folderChunk(2), FAR);

        assertThat(references(result)).isEqualTo(14);
        assertThat(server.openContinuationPoints()).isZero();
    }

    @Test
    void browse_resultsWithoutAReferenceArray_areTakenAsNoChildren() throws Exception {
        final FakeBrowseServer server = new FakeBrowseServer();
        server.nullReferencesForLeaves = true;

        final ChunkBrowser.ChunkResult result = browser(server, NO_LIMITS).browse(folderChunk(2), FAR);

        assertThat(result.references()).allSatisfy(refs -> assertThat(refs).isEmpty());
        assertThat(result.exhausted()).isEmpty();
    }

    @Test
    void browse_continuationPageWithoutAReferenceArray_isTakenAsTheEnd() throws Exception {
        final FakeBrowseServer server = wideFolders(1, 4).pageSize(3);
        server.nullReferencesOnPages = true;

        final ChunkBrowser.ChunkResult result = browser(server, NO_LIMITS).browse(folderChunk(1), FAR);

        assertThat(references(result)).as("the first page only").isEqualTo(3);
        assertThat(server.calls).containsExactly("browse[1]", "next[1]");
    }

    @Test
    void browse_browseNextFailsWithAnotherFault_propagatesAndReleasesTheRest() {
        final FakeBrowseServer server = wideFolders(2, 7).pageSize(3);
        server.maxPointsPerBrowseNext = 1;
        server.nextFault = StatusCodes.Bad_ConnectionClosed;

        assertThatThrownBy(() -> browser(server, continuationPoints(1)).browse(folderChunk(2), FAR))
                .isInstanceOf(ExecutionException.class)
                .cause()
                .isInstanceOf(UaServiceFaultException.class);
        // the first single-cursor BrowseNext fails; both cursors are still open and get released one by one
        assertThat(server.calls).containsExactly("browse[2]", "next[1]", "release[1]", "release[1]");
    }

    @Test
    void browse_releaseRefusedWithAnotherFault_isLoggedNotHalved() {
        final FakeBrowseServer server = wideFolders(7, 4).pageSize(3).continuationCapacity(6);
        server.releaseFault = StatusCodes.Bad_ConnectionClosed;
        server.status(NodeId.parse("ns=2;s=F006"), bad(StatusCodes.Bad_NodeIdUnknown));

        assertThatThrownBy(() -> browser(server, continuationPoints(5)).browse(folderChunk(7), FAR))
                .isInstanceOf(UncheckedBrowseException.class);
        assertThat(server.calls).containsExactly("browse[7]", "release[5]", "release[1]");
    }

    @Test
    void chunkResult_carriesReferencesAlignedWithTheChunk() throws Exception {
        final FakeBrowseServer server = wideFolders(3, 2);

        final ChunkBrowser.ChunkResult result = browser(server, NO_LIMITS).browse(folderChunk(3), FAR);

        assertThat(result.references()).hasSize(3);
        for (int i = 0; i < 3; i++) {
            final List<ReferenceDescription> refs = result.references().get(i);
            assertThat(refs).isNotNull();
            assertThat(refs).extracting(r -> r.getBrowseName().getName()).containsExactly("V0", "V1");
        }
    }
}
