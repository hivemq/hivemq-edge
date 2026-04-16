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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
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

/**
 * Browses an OPC-UA address space and collects variable nodes with their attributes.
 * Builds {@link BrowsedNode} records with informational fields and generated defaults.
 *
 * <p>The browse is two-phase: (1) async recursive traversal collects variable node references,
 * bounded by a concurrency semaphore to avoid overwhelming the server; (2) batch attribute reads
 * (DataType, AccessLevel, Description) resolve each variable's metadata. Data type names are
 * resolved via Milo's {@link DataTypeTree}, which handles both built-in and server-defined types.
 */
public class OpcUaNodeBrowser {

    private static final long TIMEOUT_SECONDS = 120;
    private static final int READ_BATCH_SIZE = 100;
    // Serialise all browse operations (including continuation-point requests) to avoid
    // server-side throttling on resource-constrained PLCs (e.g. S7-1500). Even at
    // concurrency 4 the S7-1500 produced non-deterministic results because browseNext
    // requests bypassed the semaphore and overlapped with new browseAsync calls.
    private static final int MAX_CONCURRENT_BROWSES = 1;

    private final @NotNull OpcUaClient client;
    private final @NotNull String adapterId;
    private final int maxReferencesPerNode;
    private final @Nullable DataTypeTree providedDataTypeTree;

    public OpcUaNodeBrowser(final @NotNull OpcUaClient client, final @NotNull String adapterId) {
        this(client, adapterId, 0, null);
    }

    /**
     * @param maxReferencesPerNode maximum references the server should return per browse request.
     *                             0 means server-decides (default). A low value forces the server
     *                             to paginate via continuation points.
     */
    public OpcUaNodeBrowser(
            final @NotNull OpcUaClient client, final @NotNull String adapterId, final int maxReferencesPerNode) {
        this(client, adapterId, maxReferencesPerNode, null);
    }

    /**
     * @param dataTypeTree a pre-built {@link DataTypeTree} to reuse across browses, or {@code null}
     *                     to build one lazily for each browse. The tree walk is non-trivial on most
     *                     servers; caching it at the adapter level saves a round-trip per browse.
     */
    public OpcUaNodeBrowser(
            final @NotNull OpcUaClient client,
            final @NotNull String adapterId,
            final int maxReferencesPerNode,
            final @Nullable DataTypeTree dataTypeTree) {
        this.client = client;
        this.adapterId = adapterId;
        this.maxReferencesPerNode = maxReferencesPerNode;
        this.providedDataTypeTree = dataTypeTree;
    }

    /**
     * Browse the OPC-UA address space starting from the given root node.
     *
     * <p>Phase 1 collects all variable node references via async recursive traversal.
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
            // Phase 1: Browse and collect variable node references with their paths.
            // CopyOnWriteArrayList is safe for concurrent adds from async browse callbacks.
            // The visited set deduplicates nodes reachable via multiple paths in the OPC UA graph.
            final List<DiscoveredVariable> variables = new CopyOnWriteArrayList<>();
            final Set<NodeId> visited = ConcurrentHashMap.newKeySet();
            final Semaphore concurrency = new Semaphore(MAX_CONCURRENT_BROWSES);
            browseRecursive(
                            browseRoot,
                            "",
                            maxDepth == 0 ? Integer.MAX_VALUE : maxDepth,
                            variables,
                            visited,
                            concurrency)
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (variables.isEmpty()) {
                return Stream.empty();
            }

            // Sort by path early (DiscoveredVariable is small) so the output stream is ordered
            // without needing to materialize the full List<BrowsedNode>.
            variables.sort(Comparator.comparing(DiscoveredVariable::path));

            // Pre-compute unique tag name defaults. Multiple nodes can share the same browse
            // path (e.g. Prosys simulation instances), so we append a numeric suffix on collision.
            final List<String> tagNameDefaults = deduplicateTagNameDefaults(variables);

            // Phase 2: Return a stream that lazily batch-reads attributes as it is consumed.
            // Reuse the caller-provided DataTypeTree if one was supplied (avoids a round-trip
            // per browse); otherwise build one now.
            final DataTypeTree dataTypeTree = providedDataTypeTree != null ? providedDataTypeTree : getDataTypeTree();
            return StreamSupport.stream(
                    new BatchAttributeSpliterator(variables, tagNameDefaults, client, dataTypeTree, this), false);
        } catch (final ExecutionException e) {
            throw new BrowseException("Browse operation failed", e.getCause());
        } catch (final TimeoutException e) {
            throw new BrowseException("Browse operation timed out after " + TIMEOUT_SECONDS + " seconds", e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BrowseException("Browse operation interrupted", e);
        }
    }

    private @NotNull CompletableFuture<Void> browseRecursive(
            final @NotNull NodeId browseRoot,
            final @NotNull String currentPath,
            final int remainingDepth,
            final @NotNull List<DiscoveredVariable> variables,
            final @NotNull Set<NodeId> visited,
            final @NotNull Semaphore concurrency) {
        // Skip already-visited nodes to deduplicate and prevent cycles in the OPC UA graph.
        if (!visited.add(browseRoot)) {
            return CompletableFuture.completedFuture(null);
        }
        final BrowseDescription browseDescription = new BrowseDescription(
                browseRoot,
                BrowseDirection.Forward,
                NodeIds.HierarchicalReferences,
                true,
                uint(0),
                uint(BrowseResultMask.All.getValue()));
        return CompletableFuture.runAsync(concurrency::acquireUninterruptibly)
                .thenCompose(ignored -> {
                    if (maxReferencesPerNode > 0) {
                        final var viewDescription = new ViewDescription(NodeId.NULL_VALUE, DateTime.MIN_VALUE, uint(0));
                        return client.browseAsync(
                                        viewDescription, uint(maxReferencesPerNode), List.of(browseDescription))
                                .thenApply(response -> response.getResults()[0]);
                    }
                    return client.browseAsync(browseDescription);
                })
                .whenComplete((result, error) -> concurrency.release())
                .thenCompose(browseResult ->
                        handleBrowseResult(browseResult, currentPath, remainingDepth, variables, visited, concurrency));
    }

    private @NotNull CompletableFuture<Void> handleBrowseResult(
            final @NotNull BrowseResult browseResult,
            final @NotNull String currentPath,
            final int remainingDepth,
            final @NotNull List<DiscoveredVariable> variables,
            final @NotNull Set<NodeId> visited,
            final @NotNull Semaphore concurrency) {
        // Fail loudly on non-Good status. Under high concurrency the server may throttle
        // individual browse operations (e.g. BadTooManyOperations), returning no references
        // and no continuation point. Without this check, the entire subtree under the
        // throttled node is silently missing from the results.
        if (browseResult.getStatusCode() != null
                && !browseResult.getStatusCode().isGood()) {
            throw new UncheckedBrowseException(
                    "Browse at path '" + currentPath + "' returned non-Good status: " + browseResult.getStatusCode(),
                    null);
        }

        final var references = new ArrayList<ReferenceDescription>();

        if (browseResult.getReferences() != null) {
            Collections.addAll(references, browseResult.getReferences());
        }

        // Drain all continuation pages BEFORE starting child recursive browses.
        // Continuation points are server-side cursors with a limited lifetime — resource-
        // constrained devices (e.g. S7-1500) expire them quickly. If continuation follow-ups
        // compete with recursive browses for the semaphore, the recursive browses may run
        // first and the continuation point expires -> Bad_ContinuationPointInvalid.
        // Continuation pages bypass the semaphore because they are part of the same logical
        // browse operation that already acquired and released the semaphore.
        final CompletableFuture<Void> continuationFuture = drainContinuationPages(
                browseResult, currentPath, remainingDepth, references, variables, visited, concurrency);

        // After all continuation pages are drained, process all collected references and
        // start recursive browses for child nodes.
        return continuationFuture.thenCompose(ignored -> {
            final var childFutures = new ArrayList<CompletableFuture<Void>>();
            final NamespaceTable nsTable = client.getNamespaceTable();

            for (final ReferenceDescription rd : references) {
                final String browseName =
                        rd.getBrowseName() != null && rd.getBrowseName().getName() != null
                                ? rd.getBrowseName().getName()
                                : "";
                final String childPath = currentPath + "/" + browseName;

                final Optional<NodeId> resolvedNodeId = rd.getNodeId().toNodeId(nsTable);
                if (resolvedNodeId.isEmpty()) {
                    continue;
                }
                final NodeId nodeId = resolvedNodeId.get();

                if (rd.getNodeClass() == NodeClass.Variable) {
                    if (visited.add(nodeId)) {
                        final int nsIndex = nodeId.getNamespaceIndex().intValue();
                        final String nsUri =
                                nsIndex < nsTable.toArray().length ? nsTable.get(nsIndex) : String.valueOf(nsIndex);
                        variables.add(new DiscoveredVariable(
                                nodeId, childPath, nsUri != null ? nsUri : "", nsIndex, browseName));
                    }
                }

                if (remainingDepth > 1) {
                    childFutures.add(
                            browseRecursive(nodeId, childPath, remainingDepth - 1, variables, visited, concurrency));
                }
            }

            return CompletableFuture.allOf(childFutures.toArray(CompletableFuture[]::new));
        });
    }

    /**
     * Drain all continuation pages for a browse result, appending references to the shared list.
     * Continuation pages bypass the browse semaphore because they are part of the same logical
     * browse operation and must be consumed promptly before the server expires them.
     */
    private @NotNull CompletableFuture<Void> drainContinuationPages(
            final @NotNull BrowseResult browseResult,
            final @NotNull String currentPath,
            final int remainingDepth,
            final @NotNull List<ReferenceDescription> references,
            final @NotNull List<DiscoveredVariable> variables,
            final @NotNull Set<NodeId> visited,
            final @NotNull Semaphore concurrency) {
        if (browseResult.getContinuationPoint() == null
                || browseResult.getContinuationPoint().bytes() == null
                || browseResult.getContinuationPoint().bytes().length == 0) {
            return CompletableFuture.completedFuture(null);
        }
        return client.browseNextAsync(false, List.of(browseResult.getContinuationPoint()))
                .thenCompose(nextResult -> {
                    if (nextResult.getResults() != null) {
                        for (final BrowseResult result : nextResult.getResults()) {
                            if (result != null) {
                                if (result.getStatusCode() != null
                                        && !result.getStatusCode().isGood()) {
                                    throw new UncheckedBrowseException(
                                            "Browse continuation at path '" + currentPath
                                                    + "' returned non-Good status: " + result.getStatusCode(),
                                            null);
                                }
                                if (result.getReferences() != null) {
                                    Collections.addAll(references, result.getReferences());
                                }
                                // Recursively drain further continuation pages.
                                return drainContinuationPages(
                                        result,
                                        currentPath,
                                        remainingDepth,
                                        references,
                                        variables,
                                        visited,
                                        concurrency);
                            }
                        }
                    }
                    return CompletableFuture.completedFuture(null);
                });
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

        private final @NotNull List<String> tagNameDefaults;

        BatchAttributeSpliterator(
                final @NotNull List<DiscoveredVariable> variables,
                final @NotNull List<String> tagNameDefaults,
                final @NotNull OpcUaClient client,
                final @Nullable DataTypeTree dataTypeTree,
                final @NotNull OpcUaNodeBrowser browser) {
            this.variables = variables;
            this.tagNameDefaults = tagNameDefaults;
            this.client = client;
            this.dataTypeTree = dataTypeTree;
            this.browser = browser;
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
            final int end = Math.min(globalOffset + READ_BATCH_SIZE, variables.size());
            // Snapshot the slice so later globalOffset updates can't mutate the view used by
            // the async callback.
            final List<DiscoveredVariable> batch = List.copyOf(variables.subList(globalOffset, end));
            pendingBatchSize = end - batchStart;
            globalOffset = end;

            final List<ReadValueId> readValueIds = new ArrayList<>(batch.size() * 3);
            for (final DiscoveredVariable var : batch) {
                readValueIds.add(new ReadValueId(var.nodeId, AttributeId.DataType.uid(), null, null));
                readValueIds.add(new ReadValueId(var.nodeId, AttributeId.AccessLevel.uid(), null, null));
                readValueIds.add(new ReadValueId(var.nodeId, AttributeId.Description.uid(), null, null));
            }

            return client.readAsync(0.0, TimestampsToReturn.Neither, readValueIds)
                    .orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
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

        private @NotNull List<BrowsedNode> await(final @NotNull CompletableFuture<List<BrowsedNode>> future) {
            try {
                return future.get();
            } catch (final ExecutionException e) {
                final Throwable cause = e.getCause();
                if (cause instanceof TimeoutException) {
                    throw new UncheckedBrowseException(
                            "Attribute read timed out after " + TIMEOUT_SECONDS + " seconds", cause);
                }
                throw new UncheckedBrowseException("Failed to read node attributes", cause);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new UncheckedBrowseException("Attribute read interrupted", e);
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
        final String[] segments = stripped.split("/");
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
        final String[] segments = stripped.split("/");
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
