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

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import com.hivemq.edge.adapters.browse.BrowseException;
import com.hivemq.edge.adapters.browse.BrowsedNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.core.typetree.DataType;
import org.eclipse.milo.opcua.sdk.core.typetree.DataTypeTree;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.AccessLevelType;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ViewDescription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Browses an OPC-UA address space and collects variable nodes with their attributes.
 * Builds {@link BrowsedNode} records with informational fields and generated defaults.
 *
 * <p>The browse is two-phase: (1) a level-wise traversal collects variable node references, one
 * batched {@code Browse} request per level chunk, serialised by a concurrency semaphore so browses
 * never overlap on the device; (2) batch attribute reads (DataType, AccessLevel, Description)
 * resolve each variable's metadata, sized to the server's advertised operation limits. Data type
 * names are resolved via Milo's {@link DataTypeTree}, which handles both built-in and server-defined types.
 */
public class OpcUaNodeBrowser {

    private static final @NotNull Logger log = LoggerFactory.getLogger(OpcUaNodeBrowser.class);

    private static final long TIMEOUT_SECONDS = 120;
    private static final int READ_BATCH_SIZE = 100;
    private static final int BROWSE_CHUNK_SIZE = 100;
    // A node refused with Bad_NoContinuationPoints even when browsed alone is retried this often, pausing in
    // between, before the browse fails: the pool is per session and another client may be draining it.
    private static final int SINGLE_NODE_CONTINUATION_RETRIES = 3;
    private static final long CONTINUATION_RETRY_PAUSE_MILLIS = 500;
    // Attributes read per variable in Phase 2 (DataType, AccessLevel, Description). A server's MaxNodesPerRead
    // limit counts ReadValueIds, not distinct nodes, so a batch of N variables is N * ATTRIBUTES_PER_NODE
    // operations (EDG-1034).
    static final int ATTRIBUTES_PER_NODE = 3;

    private final @NotNull OpcUaClient client;
    private final @NotNull String adapterId;
    private final int maxReferencesPerNode;
    // Serialise all browse operations (including continuation-point requests) so they never overlap on the
    // shared client. In production this permit is owned by the OpcUaProtocolAdapter and shared across every
    // browse call against the device (EDG-576); standalone/test callers get their own single permit.
    private final @NotNull Semaphore concurrency;
    // Overall budget for one browse call: Phase 1 including the wait for the permit, and each Phase 2 read.
    private final long timeoutSeconds;

    public OpcUaNodeBrowser(final @NotNull OpcUaClient client, final @NotNull String adapterId) {
        this(client, adapterId, 0);
    }

    /**
     * @param maxReferencesPerNode maximum references the server should return per browse request.
     *                             0 means server-decides (default). A low value forces the server
     *                             to paginate via continuation points.
     */
    public OpcUaNodeBrowser(
            final @NotNull OpcUaClient client, final @NotNull String adapterId, final int maxReferencesPerNode) {
        // Standalone default: one permit private to this browser. Production uses the constructor below to share
        // the adapter-scoped permit so concurrent browses against the same device are serialised too (EDG-576).
        this(client, adapterId, maxReferencesPerNode, new Semaphore(1));
    }

    /**
     * @param maxReferencesPerNode maximum references the server should return per browse request (0 =
     *                             server-decides).
     * @param concurrency          permit that serialises browse operations; pass the adapter-owned semaphore to
     *                             serialise across concurrent browse calls sharing one client (EDG-576).
     */
    public OpcUaNodeBrowser(
            final @NotNull OpcUaClient client,
            final @NotNull String adapterId,
            final int maxReferencesPerNode,
            final @NotNull Semaphore concurrency) {
        this(client, adapterId, maxReferencesPerNode, concurrency, TIMEOUT_SECONDS);
    }

    /** Test seam: like the constructor above, with the browse timeout in seconds instead of the default. */
    OpcUaNodeBrowser(
            final @NotNull OpcUaClient client,
            final @NotNull String adapterId,
            final int maxReferencesPerNode,
            final @NotNull Semaphore concurrency,
            final long timeoutSeconds) {
        this.client = client;
        this.adapterId = adapterId;
        this.maxReferencesPerNode = maxReferencesPerNode;
        this.concurrency = concurrency;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Browse the OPC-UA address space starting from the given root node.
     *
     * <p>Phase 1 collects all variable node references via a level-wise batched traversal.
     * The discovered variables are then sorted by path so that the returned stream
     * is ordered without requiring the final {@link BrowsedNode} list to be materialized.
     * Phase 2 lazily batch-reads attributes (DataType, AccessLevel, Description) as the
     * stream is consumed, keeping only one batch of {@link BrowsedNode} objects alive at a time.
     *
     * @param rootId the OPC-UA node ID to start from, or null for ObjectsFolder (i=85)
     * @param maxDepth   maximum depth (0 = unlimited)
     * @return a stream of discovered variable nodes, ordered by node path
     * @throws BrowseException if the browse phase fails
     */
    public @NotNull Stream<BrowsedNode> browse(final @Nullable String rootId, final int maxDepth)
            throws BrowseException {
        final NodeId browseRoot;
        if (rootId == null || rootId.isBlank()) {
            browseRoot = NodeIds.ObjectsFolder;
        } else {
            final Optional<NodeId> parsed = NodeId.parseSafe(rootId);
            if (parsed.isEmpty()) {
                throw new BrowseException("Invalid OPC-UA node ID: '" + rootId + "'");
            }
            browseRoot = parsed.get();
        }

        try {
            // Fired before Phase 1 so their round-trips overlap the traversal; joined when each phase needs them.
            final CompletableFuture<OperationLimits> limits = readOperationLimits();

            // Phase 1: level-wise traversal collecting every variable reachable from the root with its path.
            final List<DiscoveredVariable> variables =
                    browseLevels(browseRoot, maxDepth == 0 ? Integer.MAX_VALUE : maxDepth, limits);

            if (variables.isEmpty()) {
                return Stream.empty();
            }

            // Sort by path early (DiscoveredVariable is small) so the output stream is ordered
            // without needing to materialize the full List<BrowsedNode>. Nodes sharing a path
            // (e.g. Prosys simulation instances) are tie-broken on the NodeId so the collision
            // suffixes in tagNameDefaults never depend on the order the server listed them in.
            variables.sort(
                    Comparator.comparing(DiscoveredVariable::path).thenComparing(v -> v.nodeId.toParseableString()));

            // Pre-compute unique tag name defaults. Multiple nodes can share the same browse
            // path (e.g. Prosys simulation instances), so we append a numeric suffix on collision.
            final List<String> tagNameDefaults = deduplicateTagNameDefaults(variables);

            // Phase 2: Return a stream that lazily batch-reads attributes as it is consumed.
            final DataTypeTree dataTypeTree = getDataTypeTree();
            final int batchSize = initialBatchSize(
                    limits.get(timeoutSeconds, TimeUnit.SECONDS).maxNodesPerRead());
            return StreamSupport.stream(
                    new BatchAttributeSpliterator(variables, tagNameDefaults, client, dataTypeTree, this, batchSize),
                    false);
        } catch (final ExecutionException e) {
            throw new BrowseException("Browse operation failed", e.getCause());
        } catch (final UncheckedBrowseException e) {
            throw new BrowseException("Browse operation failed", e);
        } catch (final TimeoutException e) {
            throw new BrowseException("Browse operation timed out after " + timeoutSeconds + " seconds", e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BrowseException("Browse operation interrupted", e);
        }
    }

    /** The server's advertised operation limits; 0 = not advertised / unlimited. */
    record OperationLimits(int maxNodesPerRead, int maxNodesPerBrowse, int maxBrowseContinuationPoints) {
        static final @NotNull OperationLimits NONE = new OperationLimits(0, 0, 0);
    }

    /**
     * Reads the server's advertised {@code MaxNodesPerRead}, {@code MaxNodesPerBrowse} and
     * {@code MaxBrowseContinuationPoints} limits in one request. A limit resolves to 0 (= not advertised / unlimited) when the node is missing, the value is
     * not a UInt32, or the read fails — never exceptionally, so a server without operation limits browses with
     * the defaults (EDG-1034).
     */
    private @NotNull CompletableFuture<OperationLimits> readOperationLimits() {
        final List<ReadValueId> ids = List.of(
                new ReadValueId(
                        NodeIds.Server_ServerCapabilities_OperationLimits_MaxNodesPerRead,
                        AttributeId.Value.uid(),
                        null,
                        null),
                new ReadValueId(
                        NodeIds.Server_ServerCapabilities_OperationLimits_MaxNodesPerBrowse,
                        AttributeId.Value.uid(),
                        null,
                        null),
                new ReadValueId(
                        NodeIds.Server_ServerCapabilities_MaxBrowseContinuationPoints,
                        AttributeId.Value.uid(),
                        null,
                        null));
        return client.readAsync(0.0, TimestampsToReturn.Neither, ids)
                .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .thenApply(response -> {
                    final DataValue[] results = response.getResults();
                    if (results == null || results.length < ids.size()) {
                        return OperationLimits.NONE;
                    }
                    return new OperationLimits(limitValue(results[0]), limitValue(results[1]), limitValue(results[2]));
                })
                .exceptionally(error -> OperationLimits.NONE);
    }

    private static int limitValue(final @Nullable DataValue value) {
        if (value == null || value.getValue() == null) {
            return 0;
        }
        // MaxNodesPerRead / MaxNodesPerBrowse are UInt32, MaxBrowseContinuationPoints is UInt16.
        return value.getValue().getValue() instanceof final Number limit ? limit.intValue() : 0;
    }

    /**
     * Number of variables per Phase 2 read so that {@code variables * ATTRIBUTES_PER_NODE} stays within the
     * server's advertised {@code MaxNodesPerRead}; 0 or below means the server did not advertise a limit and
     * the default applies. Never below 1. WAGO PFC200 / Codesys servers advertise 100 and enforce it on the
     * ReadValueId count, so they get 33 variables per read instead of the default 100 (EDG-1034).
     */
    static int initialBatchSize(final int maxNodesPerRead) {
        if (maxNodesPerRead <= 0) {
            return READ_BATCH_SIZE;
        }
        return Math.max(1, Math.min(READ_BATCH_SIZE, maxNodesPerRead / ATTRIBUTES_PER_NODE));
    }

    /** A node whose children are still to be browsed. */
    private record PendingNode(
            @NotNull NodeId nodeId, @NotNull String path, int remainingDepth) {}

    /**
     * Phase 1. Breadth-first: every node of a level is browsed in as few {@code Browse} requests as the server's
     * {@code MaxNodesPerBrowse} allows, and every continuation point of a level is drained before the next level
     * is requested. One request per node was the previous shape; on a real PLC over a WAN link (95 ms RTT to
     * the lab S7-1500) that put a full-depth browse near the 120 s timeout once nested variables were included.
     *
     * <p>Variables are collected in a map keyed by NodeId so a node reachable through several paths is emitted
     * once, under the first (shallowest) path met. The visited set only guards the traversal. Keeping the two
     * apart is what lets a Variable's own children be browsed: struct members, array elements and properties are
     * Variables under a Variable, and were silently missing when one set did both jobs (EDG-1034).
     *
     * <p>Continuation points are drained while the browse permit is held, so no other browse against the same
     * device can run before the server-side cursor is consumed — resource-constrained servers (S7-1500) expire
     * them quickly, see EDG-465.
     */
    private @NotNull List<DiscoveredVariable> browseLevels(
            final @NotNull NodeId browseRoot,
            final int maxDepth,
            final @NotNull CompletableFuture<OperationLimits> limits)
            throws ExecutionException, InterruptedException, TimeoutException {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        final Map<NodeId, DiscoveredVariable> variables = new LinkedHashMap<>();
        final Set<NodeId> visited = new HashSet<>();
        visited.add(browseRoot);
        List<PendingNode> level = List.of(new PendingNode(browseRoot, "", maxDepth));
        final OperationLimits serverLimits = limits.get(remaining(deadline), TimeUnit.NANOSECONDS);
        int chunkSize = initialBrowseChunkSize(serverLimits.maxNodesPerBrowse());

        while (!level.isEmpty()) {
            final List<PendingNode> next = new ArrayList<>();
            int offset = 0;
            while (offset < level.size()) {
                final List<PendingNode> chunk = level.subList(offset, Math.min(offset + chunkSize, level.size()));
                final ChunkResult result;
                try {
                    result = browseChunk(chunk, deadline, serverLimits.maxBrowseContinuationPoints());
                } catch (final ExecutionException e) {
                    if (isTooManyOperations(e.getCause()) && chunk.size() > 1) {
                        final int rejected = chunk.size();
                        chunkSize = Math.max(1, rejected / 2);
                        log.info(
                                "OPC UA server rejected a browse of {} nodes for adapter '{}' ({}), retrying with {} nodes per browse",
                                rejected,
                                adapterId,
                                statusOf(e.getCause()),
                                chunkSize);
                        continue;
                    }
                    throw e;
                }
                for (int i = 0; i < chunk.size(); i++) {
                    if (result.references.get(i) != null) {
                        collectReferences(chunk.get(i), result.references.get(i), variables, visited, next);
                    }
                }
                offset += chunk.size();
                // Nodes the server could not page because it ran out of continuation points: browse them again
                // in chunks small enough that every node of a chunk can hold a point at once. The S7-1500
                // advertises 5, so a level chunk of 100 nodes with six overflowing folders fails six times over.
                List<PendingNode> exhausted = result.exhausted;
                int retrySize = chunk.size();
                int singleNodeAttempts = 0;
                while (!exhausted.isEmpty()) {
                    if (retrySize == 1) {
                        // Even one node at a time is refused: another client is holding the server's whole
                        // pool (UaExpert, TIA Portal, a second adapter). Give it a moment, a few times.
                        if (++singleNodeAttempts > SINGLE_NODE_CONTINUATION_RETRIES) {
                            throw new UncheckedBrowseException(
                                    "Browse at path '" + exhausted.get(0).path
                                            + "' returned non-Good status: Bad_NoContinuationPoints after "
                                            + SINGLE_NODE_CONTINUATION_RETRIES + " retries",
                                    null);
                        }
                        Thread.sleep(Math.min(
                                CONTINUATION_RETRY_PAUSE_MILLIS, TimeUnit.NANOSECONDS.toMillis(remaining(deadline))));
                    }
                    retrySize = retryChunkSize(retrySize, serverLimits.maxBrowseContinuationPoints());
                    log.info(
                            "OPC UA server ran out of continuation points for {} of {} browsed nodes for adapter '{}', re-browsing them {} at a time",
                            exhausted.size(),
                            chunk.size(),
                            adapterId,
                            retrySize);
                    final List<PendingNode> stillExhausted = new ArrayList<>();
                    for (int start = 0; start < exhausted.size(); start += retrySize) {
                        final List<PendingNode> retry =
                                exhausted.subList(start, Math.min(start + retrySize, exhausted.size()));
                        final ChunkResult retried =
                                browseChunk(retry, deadline, serverLimits.maxBrowseContinuationPoints());
                        for (int i = 0; i < retry.size(); i++) {
                            if (retried.references.get(i) != null) {
                                collectReferences(retry.get(i), retried.references.get(i), variables, visited, next);
                            }
                        }
                        stillExhausted.addAll(retried.exhausted);
                    }
                    exhausted = stillExhausted;
                }
            }
            level = next;
        }
        return new ArrayList<>(variables.values());
    }

    /**
     * Chunk size for re-browsing nodes that got {@code Bad_NoContinuationPoints}: the advertised
     * {@code MaxBrowseContinuationPoints} when the server has one and it is smaller than what was just tried,
     * otherwise half of what was just tried. Never below 1; at 1 a repeat of the fault fails the browse.
     */
    static int retryChunkSize(final int tried, final int maxBrowseContinuationPoints) {
        if (maxBrowseContinuationPoints > 0 && maxBrowseContinuationPoints < tried) {
            return maxBrowseContinuationPoints;
        }
        return Math.max(1, tried / 2);
    }

    /**
     * Nodes per {@code Browse} request: the default, or fewer if the server advertises a smaller
     * {@code MaxNodesPerBrowse}; 0 or below means not advertised. Never below 1.
     */
    static int initialBrowseChunkSize(final int maxNodesPerBrowse) {
        if (maxNodesPerBrowse <= 0) {
            return BROWSE_CHUNK_SIZE;
        }
        return Math.max(1, Math.min(BROWSE_CHUNK_SIZE, maxNodesPerBrowse));
    }

    /**
     * References per node of a chunk, aligned with the chunk ({@code null} for a node listed in
     * {@code exhausted}), plus the nodes whose result was {@code Bad_NoContinuationPoints} and must be
     * browsed again in a smaller chunk.
     */
    private record ChunkResult(
            @NotNull List<@Nullable List<ReferenceDescription>> references,
            @NotNull List<PendingNode> exhausted) {}

    /**
     * Browses one chunk of nodes in a single request and drains all continuation points, holding the browse
     * permit throughout. The wait for the permit counts against the browse deadline and stays interruptible,
     * so a browse queued behind another one on the same adapter can neither outlive the timeout nor ignore
     * cancellation. Whatever fails after the server handed out continuation points, every cursor still open
     * on the server is released before the failure propagates — the S7-1500 has five per session.
     */
    private @NotNull ChunkResult browseChunk(
            final @NotNull List<PendingNode> chunk, final long deadline, final int maxBrowseContinuationPoints)
            throws ExecutionException, InterruptedException, TimeoutException {
        final List<BrowseDescription> descriptions = new ArrayList<>(chunk.size());
        for (final PendingNode node : chunk) {
            descriptions.add(new BrowseDescription(
                    node.nodeId,
                    BrowseDirection.Forward,
                    NodeIds.HierarchicalReferences,
                    true,
                    uint(0),
                    uint(BrowseResultMask.All.getValue())));
        }
        final var viewDescription = new ViewDescription(NodeId.NULL_VALUE, DateTime.MIN_VALUE, uint(0));
        if (!concurrency.tryAcquire(remaining(deadline), TimeUnit.NANOSECONDS)) {
            throw new TimeoutException("Timed out waiting for the browse permit of adapter '" + adapterId + "'");
        }
        try {
            final BrowseResult[] results = client.browseAsync(viewDescription, uint(maxReferencesPerNode), descriptions)
                    .get(remaining(deadline), TimeUnit.NANOSECONDS)
                    .getResults();
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
                                "Browse at path '" + chunk.get(i).path + "' returned no result", null);
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
                                "Browse at path '" + chunk.get(i).path + "' returned non-Good status: "
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
                int pointsPerRequest =
                        maxBrowseContinuationPoints > 0 ? maxBrowseContinuationPoints : pendingPoints.size();
                while (!pendingPoints.isEmpty()) {
                    final int n = Math.max(1, Math.min(pointsPerRequest, pendingPoints.size()));
                    final List<ByteString> batch = List.copyOf(pendingPoints.subList(0, n));
                    final List<Integer> batchOwners = List.copyOf(pendingOwners.subList(0, n));
                    final BrowseResult[] pages;
                    try {
                        pages = client.browseNextAsync(false, batch)
                                .get(remaining(deadline), TimeUnit.NANOSECONDS)
                                .getResults();
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
                                    "Browse continuation at path '" + chunk.get(owner).path + "' returned no result",
                                    null);
                        }
                        if (page.getStatusCode() != null
                                && !page.getStatusCode().isGood()) {
                            throw new UncheckedBrowseException(
                                    "Browse continuation at path '" + chunk.get(owner).path
                                            + "' returned non-Good status: " + page.getStatusCode(),
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
                releaseContinuationPoints(open, maxBrowseContinuationPoints);
                throw e;
            }
        } finally {
            concurrency.release();
        }
    }

    /** All continuation points present in {@code results}, whatever each result's status. */
    private static @NotNull List<ByteString> continuationPointsOf(final @Nullable BrowseResult[] results) {
        if (results == null) {
            return List.of();
        }
        final List<ByteString> points = new ArrayList<>();
        for (final BrowseResult result : results) {
            if (result != null && hasContinuationPoint(result)) {
                points.add(result.getContinuationPoint());
            }
        }
        return points;
    }

    /**
     * Best-effort release of cursors the browse will not drain ({@code BrowseNext} with
     * {@code releaseContinuationPoints = true}). Fire-and-forget: the browse is already failing, possibly by
     * timeout or interrupt, so nothing waits on the answer and a failure is only logged.
     */
    private void releaseContinuationPoints(
            final @NotNull List<ByteString> points, final int maxBrowseContinuationPoints) {
        if (points.isEmpty()) {
            return;
        }
        // A BrowseNext above the server's capacity is refused as a whole, which would leak the very cursors
        // this is meant to free: batch to the advertised capacity, one per request when none is advertised.
        final int perRequest = maxBrowseContinuationPoints > 0 ? maxBrowseContinuationPoints : 1;
        for (int start = 0; start < points.size(); start += perRequest) {
            final List<ByteString> batch =
                    List.copyOf(points.subList(start, Math.min(start + perRequest, points.size())));
            client.browseNextAsync(true, batch).exceptionally(error -> {
                log.debug(
                        "Could not release {} continuation point(s) after a failed browse for adapter '{}'",
                        batch.size(),
                        adapterId,
                        error);
                return null;
            });
        }
    }

    private static boolean isNoContinuationPoints(final @Nullable StatusCode status) {
        return status != null && status.getValue() == StatusCodes.Bad_NoContinuationPoints;
    }

    private static boolean hasContinuationPoint(final @NotNull BrowseResult result) {
        return result.getContinuationPoint() != null
                && result.getContinuationPoint().bytes() != null
                && result.getContinuationPoint().bytes().length > 0;
    }

    /** Records the variables among {@code references} and queues every unvisited child for the next level. */
    private void collectReferences(
            final @NotNull PendingNode parent,
            final @NotNull List<ReferenceDescription> references,
            final @NotNull Map<NodeId, DiscoveredVariable> variables,
            final @NotNull Set<NodeId> visited,
            final @NotNull List<PendingNode> next) {
        final NamespaceTable nsTable = client.getNamespaceTable();
        for (final ReferenceDescription rd : references) {
            final String browseName =
                    rd.getBrowseName() != null && rd.getBrowseName().getName() != null
                            ? rd.getBrowseName().getName()
                            : "";
            final String childPath = parent.path + "/" + browseName;

            final Optional<NodeId> resolvedNodeId = rd.getNodeId().toNodeId(nsTable);
            if (resolvedNodeId.isEmpty()) {
                continue;
            }
            final NodeId nodeId = resolvedNodeId.get();

            if (rd.getNodeClass() == NodeClass.Variable && !variables.containsKey(nodeId)) {
                final int nsIndex = nodeId.getNamespaceIndex().intValue();
                final String nsUri =
                        nsIndex < nsTable.toArray().length ? nsTable.get(nsIndex) : String.valueOf(nsIndex);
                variables.put(
                        nodeId,
                        new DiscoveredVariable(nodeId, childPath, nsUri != null ? nsUri : "", nsIndex, browseName));
            }

            if (parent.remainingDepth > 1 && visited.add(nodeId)) {
                next.add(new PendingNode(nodeId, childPath, parent.remainingDepth - 1));
            }
        }
    }

    private static long remaining(final long deadline) {
        return Math.max(1, deadline - System.nanoTime());
    }

    /** The status of a Milo fault, for log lines; empty when the cause is not one. */
    static @NotNull String statusOf(final @Nullable Throwable cause) {
        return cause instanceof final UaException ua ? String.valueOf(ua.getStatusCode()) : "";
    }

    /**
     * A service fault that says the request was too big for the server in one way or another: too many
     * operations, or a request/response the negotiated message size cannot carry. All of them are answered by
     * sending less per request.
     */
    static boolean isTooManyOperations(final @Nullable Throwable cause) {
        if (!(cause instanceof final UaException ua)) {
            return false;
        }
        final long status = ua.getStatusCode().getValue();
        return status == StatusCodes.Bad_TooManyOperations
                || status == StatusCodes.Bad_ResponseTooLarge
                || status == StatusCodes.Bad_RequestTooLarge
                || status == StatusCodes.Bad_EncodingLimitsExceeded
                || status == StatusCodes.Bad_TcpMessageTooLarge;
    }

    /**
     * Spliterator that lazily batch-reads OPC-UA attributes and produces {@link BrowsedNode} records.
     * Each {@link #tryAdvance} call consumes one {@link BrowsedNode} from the current batch and,
     * when a batch is exhausted, waits for the already-in-flight next batch before firing the
     * one after. Prefetching keeps the wire busy while the HTTP serializer drains the current
     * batch — the attribute-read round-trip for batch N+1 overlaps with the serialization of
     * batch N.
     *
     * <p>Milo's channel is strictly serial, so prefetching does not add concurrent server load:
     * the next read is dispatched only after the previous response has landed on the client,
     * but before the client has finished emitting the current batch. For a typical browse the
     * two costs (serialization + transfer vs attribute read round-trip) are close to equal, so
     * prefetching roughly halves Phase 2 wall time on large address spaces.
     */
    private static final class BatchAttributeSpliterator implements Spliterator<BrowsedNode> {

        private final @NotNull List<DiscoveredVariable> variables;
        private final @NotNull OpcUaClient client;
        private final @Nullable DataTypeTree dataTypeTree;
        private final @NotNull OpcUaNodeBrowser browser;

        private int globalOffset;
        private @Nullable List<BrowsedNode> currentBatch;
        private int batchIndex;
        private @Nullable CompletableFuture<List<BrowsedNode>> nextBatchFuture;
        // Size of the prefetched batch that globalOffset has already been advanced past.
        // Tracked so estimateSize() can correctly count the in-flight batch as remaining,
        // which is required by the SIZED characteristic contract.
        private int pendingBatchSize;
        // Variables per read. Starts from the server's advertised MaxNodesPerRead and is halved
        // whenever a read comes back Bad_TooManyOperations, so a server that enforces a tighter
        // limit than it advertises still gets browsed instead of failing the stream (EDG-1034).
        private int batchSize;
        // Offset of the in-flight batch, so a rejected read can be re-issued for the same slice.
        private int inFlightStart;

        private final @NotNull List<String> tagNameDefaults;

        BatchAttributeSpliterator(
                final @NotNull List<DiscoveredVariable> variables,
                final @NotNull List<String> tagNameDefaults,
                final @NotNull OpcUaClient client,
                final @Nullable DataTypeTree dataTypeTree,
                final @NotNull OpcUaNodeBrowser browser,
                final int batchSize) {
            this.variables = variables;
            this.tagNameDefaults = tagNameDefaults;
            this.client = client;
            this.dataTypeTree = dataTypeTree;
            this.browser = browser;
            this.batchSize = batchSize;
            this.globalOffset = 0;
            this.currentBatch = null;
            this.batchIndex = 0;
            this.pendingBatchSize = 0;
            // Prime the pipeline: fire the first batch eagerly so it is in flight before the
            // first tryAdvance() call.
            this.nextBatchFuture = firePrefetch();
        }

        @Override
        public boolean tryAdvance(final @NotNull Consumer<? super BrowsedNode> action) {
            // Serve from current batch if available.
            if (currentBatch != null && batchIndex < currentBatch.size()) {
                action.accept(currentBatch.get(batchIndex++));
                return true;
            }
            // No prefetched batch left — we're done.
            if (nextBatchFuture == null) {
                return false;
            }
            // Wait for the prefetched batch, then fire the next one so it overlaps with the
            // consumption of the batch we just received.
            currentBatch = await(nextBatchFuture);
            batchIndex = 0;
            nextBatchFuture = firePrefetch();
            if (currentBatch.isEmpty()) {
                return false;
            }
            action.accept(currentBatch.get(batchIndex++));
            return true;
        }

        /**
         * Schedule the next attribute-read batch if there are still variables to process.
         * Returns {@code null} when the end of the variable list has been reached. Updates
         * {@code pendingBatchSize} so {@link #estimateSize()} can count the in-flight batch.
         */
        private @Nullable CompletableFuture<List<BrowsedNode>> firePrefetch() {
            if (globalOffset >= variables.size()) {
                pendingBatchSize = 0;
                return null;
            }
            final int batchStart = globalOffset;
            final int end = Math.min(globalOffset + batchSize, variables.size());
            // Snapshot the slice so later globalOffset updates can't mutate the view used by
            // the async callback.
            final List<DiscoveredVariable> batch = List.copyOf(variables.subList(globalOffset, end));
            pendingBatchSize = end - batchStart;
            inFlightStart = batchStart;
            globalOffset = end;

            final List<ReadValueId> readValueIds = new ArrayList<>(batch.size() * ATTRIBUTES_PER_NODE);
            for (final DiscoveredVariable var : batch) {
                readValueIds.add(new ReadValueId(var.nodeId, AttributeId.DataType.uid(), null, null));
                readValueIds.add(new ReadValueId(var.nodeId, AttributeId.AccessLevel.uid(), null, null));
                readValueIds.add(new ReadValueId(var.nodeId, AttributeId.Description.uid(), null, null));
            }

            return client.readAsync(0.0, TimestampsToReturn.Neither, readValueIds)
                    .orTimeout(browser.timeoutSeconds, TimeUnit.SECONDS)
                    .thenApply(response -> buildBatch(batch, batchStart, response.getResults()));
        }

        private @NotNull List<BrowsedNode> buildBatch(
                final @NotNull List<DiscoveredVariable> batch,
                final int batchStart,
                final @NotNull DataValue[] values) {
            final List<BrowsedNode> result = new ArrayList<>(batch.size());
            for (int i = 0; i < batch.size(); i++) {
                final DiscoveredVariable var = batch.get(i);
                final String dataType = browser.resolveDataTypeName(values[i * 3], dataTypeTree);
                final String accessLevel = browser.resolveAccessLevel(values[i * 3 + 1]);
                final String description = browser.resolveDescription(values[i * 3 + 2]);

                result.add(new BrowsedNode(
                        var.path,
                        var.namespaceUri,
                        var.namespaceIndex,
                        var.nodeId.toParseableString(),
                        dataType,
                        accessLevel,
                        description,
                        tagNameDefaults.get(batchStart + i),
                        description,
                        browser.generateNorthboundTopicDefault(var.path),
                        browser.generateSouthboundTopicDefault(var.path)));
            }
            return result;
        }

        /**
         * Waits for the in-flight batch. A {@code Bad_TooManyOperations} service fault means the server enforces
         * a smaller read limit than it advertised (or none was advertised): the batch size is halved and the
         * same slice re-read, until a single variable per read is rejected — only then is the stream failed.
         */
        private @NotNull List<BrowsedNode> await(final @NotNull CompletableFuture<List<BrowsedNode>> future) {
            CompletableFuture<List<BrowsedNode>> pending = future;
            while (true) {
                try {
                    return pending.get();
                } catch (final ExecutionException e) {
                    final Throwable cause = e.getCause();
                    if (cause instanceof TimeoutException) {
                        throw new UncheckedBrowseException(
                                "Attribute read timed out after " + browser.timeoutSeconds + " seconds", cause);
                    }
                    // Halve the slice that was actually rejected (the last slice can be shorter than batchSize).
                    if (isTooManyOperations(cause) && pendingBatchSize > 1) {
                        final int rejected = pendingBatchSize;
                        batchSize = rejected / 2;
                        log.info(
                                "OPC UA server rejected a read of {} variables ({} attributes) for adapter '{}' ({}), retrying with {} variables per read",
                                rejected,
                                rejected * ATTRIBUTES_PER_NODE,
                                browser.adapterId,
                                statusOf(cause),
                                batchSize);
                        globalOffset = inFlightStart;
                        // inFlightStart < variables.size(), so there is always a batch to re-issue.
                        pending = Objects.requireNonNull(firePrefetch());
                        continue;
                    }
                    throw new UncheckedBrowseException("Failed to read node attributes", cause);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new UncheckedBrowseException("Attribute read interrupted", e);
                }
            }
        }

        @Override
        public @Nullable Spliterator<BrowsedNode> trySplit() {
            return null; // sequential only
        }

        @Override
        public long estimateSize() {
            // Must be exact to satisfy the SIZED characteristic contract. Three sources of
            // yet-to-emit items: (a) tail of the currentBatch we're iterating, (b) the
            // prefetched batch that's already been scheduled but not yet received, (c) the
            // tail of the variables list that we haven't fired a read for yet.
            final int currentRemaining = currentBatch != null ? currentBatch.size() - batchIndex : 0;
            return (long) currentRemaining + pendingBatchSize + (variables.size() - globalOffset);
        }

        @Override
        public int characteristics() {
            return ORDERED | SIZED | NONNULL;
        }
    }

    /**
     * Unchecked wrapper for {@link BrowseException} thrown from within a {@link Spliterator}.
     * Stream consumers should catch this when consuming the browse stream.
     */
    static final class UncheckedBrowseException extends RuntimeException {
        UncheckedBrowseException(final @NotNull String message, final @Nullable Throwable cause) {
            super(message, cause);
        }
    }

    private @Nullable DataTypeTree getDataTypeTree() {
        try {
            return client.getDataTypeTree();
        } catch (final UaException e) {
            return null;
        }
    }

    // --- Attribute resolution ---

    private @NotNull String resolveDataTypeName(
            final @NotNull DataValue dataTypeValue, final @Nullable DataTypeTree dataTypeTree) {
        if (!(dataTypeValue.getValue().getValue() instanceof final NodeId dataTypeNodeId)) {
            return "Unknown";
        }
        if (dataTypeTree != null) {
            final DataType dataType = dataTypeTree.getDataType(dataTypeNodeId);
            if (dataType != null
                    && dataType.getBrowseName() != null
                    && dataType.getBrowseName().getName() != null) {
                return dataType.getBrowseName().getName();
            }
        }
        return dataTypeNodeId.toParseableString();
    }

    private @NotNull String resolveAccessLevel(final @NotNull DataValue accessLevelValue) {
        if (accessLevelValue.getValue().getValue() instanceof final UByte accessByte) {
            final AccessLevelType accessLevel = new AccessLevelType(accessByte);
            final boolean readable = accessLevel.getCurrentRead();
            final boolean writable = accessLevel.getCurrentWrite();
            if (readable && writable) {
                return "READ_WRITE";
            }
            if (readable) {
                return "READ";
            }
            if (writable) {
                return "WRITE";
            }
            return "NONE";
        }
        return "READ";
    }

    private @Nullable String resolveDescription(final @NotNull DataValue descriptionValue) {
        if (descriptionValue.getValue().getValue() instanceof final LocalizedText text) {
            return text.getText();
        }
        return null;
    }

    // --- Default generation ---

    /**
     * Builds a list of unique tag name defaults from base defaults. When duplicates are detected,
     * a numeric suffix is appended: {@code name}, {@code name-2}, {@code name-3}, etc.
     */
    static @NotNull List<String> deduplicateDefaults(final @NotNull List<String> baseDefaults) {
        final List<String> result = new ArrayList<>(baseDefaults.size());
        final Map<String, Integer> seen = new HashMap<>();
        for (final String base : baseDefaults) {
            final int count = seen.merge(base, 1, Integer::sum);
            result.add(count == 1 ? base : base + "-" + count);
        }
        return result;
    }

    private @NotNull List<String> deduplicateTagNameDefaults(final @NotNull List<DiscoveredVariable> variables) {
        final List<String> baseDefaults = new ArrayList<>(variables.size());
        for (final DiscoveredVariable var : variables) {
            baseDefaults.add(generateTagNameDefault(var.path));
        }
        return deduplicateDefaults(baseDefaults);
    }

    @NotNull
    String generateTagNameDefault(final @NotNull String path) {
        // Use the full path to guarantee uniqueness — paths are unique in the address space.
        // e.g. "/Aliases/FindAlias/InputArguments" → "aliases-findalias-inputarguments"
        final String stripped = path.startsWith("/") ? path.substring(1) : path;
        if (stripped.isEmpty()) {
            return "";
        }
        final String[] segments = stripped.split("/", -1);
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                sb.append('-');
            }
            sb.append(sanitize(segments[i]));
        }
        return sb.toString();
    }

    @NotNull
    String generateNorthboundTopicDefault(final @NotNull String path) {
        return adapterId + "/" + sanitizePath(path);
    }

    @NotNull
    String generateSouthboundTopicDefault(final @NotNull String path) {
        return adapterId + "/write/" + sanitizePath(path);
    }

    /**
     * Produces a kebab-case-safe identifier: lowercases, replaces runs of non-alphanumeric
     * characters with a single dash, strips leading/trailing dashes. Single-pass character
     * walk — no regex allocation or intermediate strings. Equivalent to the three-regex
     * formulation previously used, but measurably faster in the Phase 2 hot path.
     */
    static @NotNull String sanitize(final @NotNull String input) {
        final int len = input.length();
        final StringBuilder sb = new StringBuilder(len);
        boolean lastWasDash = false;
        for (int i = 0; i < len; i++) {
            final char lower = Character.toLowerCase(input.charAt(i));
            if ((lower >= 'a' && lower <= 'z') || (lower >= '0' && lower <= '9')) {
                sb.append(lower);
                lastWasDash = false;
            } else if (!lastWasDash && sb.length() > 0) {
                // Collapse runs of non-alphanumeric characters and drop leading dashes.
                sb.append('-');
                lastWasDash = true;
            }
        }
        // Strip trailing dash.
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '-') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    static @NotNull String sanitizePath(final @NotNull String path) {
        final String stripped = path.startsWith("/") ? path.substring(1) : path;
        if (stripped.isEmpty()) {
            return "";
        }
        final String[] segments = stripped.split("/", -1);
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                sb.append('/');
            }
            sb.append(sanitize(segments[i]));
        }
        return sb.toString();
    }

    private record DiscoveredVariable(
            @NotNull NodeId nodeId,
            @NotNull String path,
            @NotNull String namespaceUri,
            int namespaceIndex,
            @NotNull String browseName) {}
}
