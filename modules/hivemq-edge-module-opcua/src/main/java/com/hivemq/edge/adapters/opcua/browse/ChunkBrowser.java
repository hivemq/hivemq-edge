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

import static com.hivemq.edge.adapters.opcua.browse.ServiceFaults.continuationPointsOf;
import static com.hivemq.edge.adapters.opcua.browse.ServiceFaults.hasContinuationPoint;
import static com.hivemq.edge.adapters.opcua.browse.ServiceFaults.isNoContinuationPoints;
import static com.hivemq.edge.adapters.opcua.browse.ServiceFaults.isTooManyOperations;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseNextResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ViewDescription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One {@code Browse} request for a chunk of nodes, its continuation pages, and the accounting of every
 * server-side cursor that request hands out. The whole thing runs under the browse permit, so no other browse
 * against the same device runs before the chunk's cursors are consumed — resource-constrained servers
 * (S7-1500: five cursors per session) expire them quickly, see EDG-465.
 *
 * <p>The rules, in one place:
 * <ul>
 *   <li>The wait for the permit counts against the browse deadline and stays interruptible, so a browse
 *       queued behind another one can neither outlive its timeout nor ignore cancellation.</li>
 *   <li>Every cursor of a response is tracked <em>before</em> any status is judged; whatever fails afterwards
 *       — a bad sibling, a bad page, a missing result, a timeout — releases every cursor still open before
 *       the failure propagates.</li>
 *   <li>A BrowseNext carries at most the advertised {@code MaxBrowseContinuationPoints} cursors and is halved
 *       when refused as too many operations; a release request likewise.</li>
 *   <li>A response that lands after the wait for it ended is not lost: the cursors it hands out are released
 *       when it arrives.</li>
 *   <li>{@code Bad_NoContinuationPoints} is not a failure but a node the server could not page now; the
 *       caller browses it again in a smaller chunk.</li>
 * </ul>
 */
final class ChunkBrowser {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ChunkBrowser.class);

    /**
     * References per node of a chunk, aligned with the chunk ({@code null} for a node listed in
     * {@code exhausted}), plus the nodes whose result was {@code Bad_NoContinuationPoints} and must be
     * browsed again in a smaller chunk.
     */
    record ChunkResult(
            @NotNull List<@Nullable List<ReferenceDescription>> references,
            @NotNull List<PendingNode> exhausted) {}

    private final @NotNull OpcUaClient client;
    private final @NotNull String adapterId;
    private final int maxReferencesPerNode;
    private final @NotNull Semaphore permit;
    private final @NotNull OperationLimits limits;

    /**
     * @param maxReferencesPerNode maximum references the server should return per node and request, 0 =
     *                             server decides; a low value forces the server to page via continuation points
     * @param permit               serialises every Browse and BrowseNext against the device; shared by all
     *                             browses of one adapter (EDG-576)
     */
    ChunkBrowser(
            final @NotNull OpcUaClient client,
            final @NotNull String adapterId,
            final int maxReferencesPerNode,
            final @NotNull Semaphore permit,
            final @NotNull OperationLimits limits) {
        this.client = client;
        this.adapterId = adapterId;
        this.maxReferencesPerNode = maxReferencesPerNode;
        this.permit = permit;
        this.limits = limits;
    }

    /**
     * Browses {@code chunk} in a single request and drains all its continuation points, holding the permit
     * throughout.
     *
     * @throws ExecutionException      the Browse or a BrowseNext failed as a whole (service fault, connection)
     * @throws TimeoutException        the permit or a response did not come within the deadline
     * @throws InterruptedException    the calling thread was interrupted while waiting
     * @throws UncheckedBrowseException a result or page was missing or non-Good, with the node's path
     */
    @NotNull
    ChunkResult browse(final @NotNull List<PendingNode> chunk, final @NotNull Deadline deadline)
            throws ExecutionException, InterruptedException, TimeoutException {
        final List<BrowseDescription> descriptions = new ArrayList<>(chunk.size());
        for (final PendingNode node : chunk) {
            descriptions.add(new BrowseDescription(
                    node.nodeId(),
                    BrowseDirection.Forward,
                    NodeIds.HierarchicalReferences,
                    true,
                    uint(0),
                    uint(BrowseResultMask.All.getValue())));
        }
        final var viewDescription = new ViewDescription(NodeId.NULL_VALUE, DateTime.MIN_VALUE, uint(0));
        if (!deadline.tryAcquire(permit)) {
            throw new TimeoutException("Timed out waiting for the browse permit of adapter '" + adapterId + "'");
        }
        try {
            final BrowseResult[] results = awaitResults(
                    client.browseAsync(viewDescription, uint(maxReferencesPerNode), descriptions),
                    BrowseResponse::getResults,
                    deadline);
            // Every cursor still open on the server: the ones not drained yet plus, while a BrowseNext is in
            // flight, the ones it carries. Tracked before any status is judged, so a failure on one node
            // releases its siblings' cursors instead of leaking them.
            final List<ByteString> open = new ArrayList<>(continuationPointsOf(results));
            try {
                final List<List<ReferenceDescription>> references = new ArrayList<>(chunk.size());
                final List<PendingNode> exhausted = new ArrayList<>();
                // Continuation points still to drain, with the chunk index they belong to.
                final List<ByteString> pendingPoints = new ArrayList<>();
                final List<Integer> pendingOwners = new ArrayList<>();
                for (int i = 0; i < chunk.size(); i++) {
                    final BrowseResult result = results != null && i < results.length ? results[i] : null;
                    final List<ReferenceDescription> refs = new ArrayList<>();
                    references.add(refs);
                    if (result == null) {
                        // One result per description is the service contract; a missing one is not "no
                        // children", it is a subtree we know nothing about.
                        throw new UncheckedBrowseException(
                                "Browse at path '" + chunk.get(i).path() + "' returned no result", null);
                    }
                    // A server out of continuation points cannot page this node's children now; a smaller
                    // chunk, or a moment later, will. Whether a chunk of one that still gets the fault is a
                    // real failure is decided by the caller, which bounds the retries.
                    if (isNoContinuationPoints(result.getStatusCode())) {
                        references.set(i, null);
                        exhausted.add(chunk.get(i));
                        continue;
                    }
                    // Fail loudly on non-Good status. Under load the server may throttle individual browse
                    // operations, returning no references and no continuation point; without this check the
                    // entire subtree under the throttled node is silently missing from the results.
                    if (result.getStatusCode() != null
                            && !result.getStatusCode().isGood()) {
                        throw new UncheckedBrowseException(
                                "Browse at path '" + chunk.get(i).path() + "' returned non-Good status: "
                                        + result.getStatusCode(),
                                null);
                    }
                    if (result.getReferences() != null) {
                        Collections.addAll(refs, result.getReferences());
                    }
                    if (hasContinuationPoint(result)) {
                        pendingPoints.add(result.getContinuationPoint());
                        pendingOwners.add(i);
                    }
                }
                // Drain every continuation page of this chunk before returning (and releasing the permit). A
                // BrowseNext carries at most the server's continuation-point capacity — Milo hands out more
                // cursors per Browse than it accepts per BrowseNext — and is halved on Bad_TooManyOperations.
                int pointsPerRequest = limits.continuationPointsPerBrowseNext(pendingPoints.size());
                while (!pendingPoints.isEmpty()) {
                    final int n = Math.max(1, Math.min(pointsPerRequest, pendingPoints.size()));
                    final List<ByteString> batch = List.copyOf(pendingPoints.subList(0, n));
                    final List<Integer> batchOwners = List.copyOf(pendingOwners.subList(0, n));
                    final BrowseResult[] pages;
                    try {
                        pages = awaitResults(
                                client.browseNextAsync(false, batch), BrowseNextResponse::getResults, deadline);
                    } catch (final ExecutionException e) {
                        if (isTooManyOperations(e.getCause()) && n > 1) {
                            pointsPerRequest = Math.max(1, n / 2);
                            continue;
                        }
                        throw e;
                    }
                    // The batch's cursors are consumed; whatever the pages carry is open now.
                    pendingPoints.subList(0, n).clear();
                    pendingOwners.subList(0, n).clear();
                    open.clear();
                    open.addAll(pendingPoints);
                    open.addAll(continuationPointsOf(pages));
                    for (int i = 0; i < batch.size(); i++) {
                        final int owner = batchOwners.get(i);
                        final BrowseResult page = pages != null && i < pages.length ? pages[i] : null;
                        if (page == null) {
                            throw new UncheckedBrowseException(
                                    "Browse continuation at path '"
                                            + chunk.get(owner).path() + "' returned no result",
                                    null);
                        }
                        if (page.getStatusCode() != null
                                && !page.getStatusCode().isGood()) {
                            throw new UncheckedBrowseException(
                                    "Browse continuation at path '"
                                            + chunk.get(owner).path() + "' returned non-Good status: "
                                            + page.getStatusCode(),
                                    null);
                        }
                        if (page.getReferences() != null) {
                            Collections.addAll(references.get(owner), page.getReferences());
                        }
                        if (hasContinuationPoint(page)) {
                            pendingPoints.add(page.getContinuationPoint());
                            pendingOwners.add(owner);
                        }
                    }
                }
                return new ChunkResult(references, exhausted);
            } catch (final Exception e) {
                releaseContinuationPoints(open);
                throw e;
            }
        } finally {
            permit.release();
        }
    }

    /**
     * The results of a Browse or BrowseNext, waited for within the deadline. When the wait ends before the
     * response does — timeout or interrupt — the request is still on the wire and its late answer may hand
     * out continuation points nobody will ever drain: those are released the moment the answer lands. The
     * future is deliberately not cancelled, cancelling it would drop that answer.
     */
    private <R> @Nullable BrowseResult[] awaitResults(
            final @NotNull CompletableFuture<R> response,
            final @NotNull Function<R, @Nullable BrowseResult[]> results,
            final @NotNull Deadline deadline)
            throws ExecutionException, InterruptedException, TimeoutException {
        try {
            return results.apply(deadline.await(response));
        } catch (final TimeoutException | InterruptedException e) {
            response.thenAccept(late -> releaseContinuationPoints(continuationPointsOf(results.apply(late))))
                    .exceptionally(error -> {
                        // The late answer was a failure: no cursors were handed out, nothing to give back.
                        log.debug("Browse request of adapter '{}' failed after its wait ended", adapterId, error);
                        return null;
                    });
            throw e;
        }
    }

    /**
     * Best-effort release of cursors the browse will not drain ({@code BrowseNext} with
     * {@code releaseContinuationPoints = true}). Fire-and-forget: the browse is already failing, possibly by
     * timeout or interrupt, so nothing waits on the answer and a failure is only logged.
     */
    private void releaseContinuationPoints(final @NotNull List<ByteString> points) {
        if (points.isEmpty()) {
            return;
        }
        // A BrowseNext above the server's capacity is refused as a whole, which would leak the very cursors
        // this is meant to free: batch to the advertised capacity, one per request when none is advertised.
        final int perRequest = limits.continuationPointsPerRelease();
        for (int start = 0; start < points.size(); start += perRequest) {
            release(List.copyOf(points.subList(start, Math.min(start + perRequest, points.size()))));
        }
    }

    /**
     * One release request. Refused as too many operations — the advertised capacity was a lie, or nothing
     * was advertised and the server's real limit is lower still — nothing was released, so the batch is
     * halved and sent again, like the drain does, down to one cursor per request.
     */
    private void release(final @NotNull List<ByteString> batch) {
        client.browseNextAsync(true, batch).exceptionally(error -> {
            if (isTooManyOperations(error) && batch.size() > 1) {
                final int half = batch.size() / 2;
                release(batch.subList(0, half));
                release(batch.subList(half, batch.size()));
                return null;
            }
            log.debug(
                    "Could not release {} continuation point(s) after a failed browse for adapter '{}'",
                    batch.size(),
                    adapterId,
                    error);
            return null;
        });
    }
}
