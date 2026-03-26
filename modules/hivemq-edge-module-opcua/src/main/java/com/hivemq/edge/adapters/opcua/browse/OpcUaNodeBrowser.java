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
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
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
    private static final int MAX_CONCURRENT_BROWSES = 32;

    private final @NotNull OpcUaClient client;
    private final @NotNull String adapterId;

    public OpcUaNodeBrowser(final @NotNull OpcUaClient client, final @NotNull String adapterId) {
        this.client = client;
        this.adapterId = adapterId;
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
            final List<DiscoveredVariable> variables = new CopyOnWriteArrayList<>();
            final Semaphore concurrency = new Semaphore(MAX_CONCURRENT_BROWSES);
            browseRecursive(browseRoot, "", maxDepth == 0 ? Integer.MAX_VALUE : maxDepth, variables, concurrency)
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (variables.isEmpty()) {
                return Stream.empty();
            }

            // Sort by path early (DiscoveredVariable is small) so the output stream is ordered
            // without needing to materialize the full List<BrowsedNode>.
            variables.sort(Comparator.comparing(DiscoveredVariable::path));

            // Phase 2: Return a stream that lazily batch-reads attributes as it is consumed.
            final DataTypeTree dataTypeTree = getDataTypeTree();
            return StreamSupport.stream(new BatchAttributeSpliterator(variables, client, dataTypeTree, this), false);
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
            final @NotNull Semaphore concurrency) {
        return CompletableFuture.runAsync(concurrency::acquireUninterruptibly)
                .thenCompose(ignored -> client.browseAsync(new BrowseDescription(
                        browseRoot,
                        BrowseDirection.Forward,
                        NodeIds.HierarchicalReferences,
                        true,
                        uint(0),
                        uint(BrowseResultMask.All.getValue()))))
                .whenComplete((result, error) -> concurrency.release())
                .thenCompose(browseResult ->
                        handleBrowseResult(browseResult, currentPath, remainingDepth, variables, concurrency));
    }

    private @NotNull CompletableFuture<Void> handleBrowseResult(
            final @NotNull BrowseResult browseResult,
            final @NotNull String currentPath,
            final int remainingDepth,
            final @NotNull List<DiscoveredVariable> variables,
            final @NotNull Semaphore concurrency) {
        final var childFutures = new ArrayList<CompletableFuture<Void>>();
        final var references = new ArrayList<ReferenceDescription>();

        if (browseResult.getReferences() != null) {
            Collections.addAll(references, browseResult.getReferences());
        }

        final NamespaceTable nsTable = client.getNamespaceTable();

        for (final ReferenceDescription rd : references) {
            final String browseName =
                    rd.getBrowseName() != null ? rd.getBrowseName().getName() : "";
            final String childPath = currentPath + "/" + (browseName != null ? browseName : "");

            final Optional<NodeId> resolvedNodeId = rd.getNodeId().toNodeId(nsTable);
            if (resolvedNodeId.isEmpty()) {
                continue;
            }
            final NodeId nodeId = resolvedNodeId.get();

            if (rd.getNodeClass() == NodeClass.Variable) {
                final int nsIndex = nodeId.getNamespaceIndex().intValue();
                final String nsUri =
                        nsIndex < nsTable.toArray().length ? nsTable.get(nsIndex) : String.valueOf(nsIndex);
                variables.add(new DiscoveredVariable(
                        nodeId, childPath, nsUri != null ? nsUri : "", nsIndex, browseName != null ? browseName : ""));
            }

            if (remainingDepth > 1) {
                childFutures.add(browseRecursive(nodeId, childPath, remainingDepth - 1, variables, concurrency));
            }
        }

        // Handle continuation points
        if (browseResult.getContinuationPoint() != null
                && browseResult.getContinuationPoint().bytes() != null) {
            childFutures.add(client.browseNextAsync(false, List.of(browseResult.getContinuationPoint()))
                    .thenCompose(nextResult -> {
                        final var continuationFutures = new ArrayList<CompletableFuture<Void>>();
                        if (nextResult.getResults() != null) {
                            for (final BrowseResult result : nextResult.getResults()) {
                                if (result != null) {
                                    continuationFutures.add(handleBrowseResult(
                                            result, currentPath, remainingDepth, variables, concurrency));
                                }
                            }
                        }
                        return CompletableFuture.allOf(continuationFutures.toArray(CompletableFuture[]::new));
                    }));
        }

        return CompletableFuture.allOf(childFutures.toArray(CompletableFuture[]::new));
    }

    /**
     * Spliterator that lazily batch-reads OPC-UA attributes and produces {@link BrowsedNode} records.
     * Each {@link #tryAdvance} call reads one batch of up to {@link #READ_BATCH_SIZE} nodes from the
     * OPC-UA server, resolves attributes, and emits the resulting {@link BrowsedNode} records one at
     * a time. This keeps at most one batch of {@code BrowsedNode} objects alive at any point.
     */
    private static final class BatchAttributeSpliterator implements Spliterator<BrowsedNode> {

        private final @NotNull List<DiscoveredVariable> variables;
        private final @NotNull OpcUaClient client;
        private final @Nullable DataTypeTree dataTypeTree;
        private final @NotNull OpcUaNodeBrowser browser;

        private int globalOffset;
        private @Nullable List<BrowsedNode> currentBatch;
        private int batchIndex;

        BatchAttributeSpliterator(
                final @NotNull List<DiscoveredVariable> variables,
                final @NotNull OpcUaClient client,
                final @Nullable DataTypeTree dataTypeTree,
                final @NotNull OpcUaNodeBrowser browser) {
            this.variables = variables;
            this.client = client;
            this.dataTypeTree = dataTypeTree;
            this.browser = browser;
            this.globalOffset = 0;
            this.currentBatch = null;
            this.batchIndex = 0;
        }

        @Override
        public boolean tryAdvance(final @NotNull Consumer<? super BrowsedNode> action) {
            // Serve from current batch if available
            if (currentBatch != null && batchIndex < currentBatch.size()) {
                action.accept(currentBatch.get(batchIndex++));
                return true;
            }
            // Load next batch if variables remain
            if (globalOffset >= variables.size()) {
                return false;
            }
            currentBatch = readNextBatch();
            batchIndex = 0;
            if (currentBatch.isEmpty()) {
                return false;
            }
            action.accept(currentBatch.get(batchIndex++));
            return true;
        }

        private @NotNull List<BrowsedNode> readNextBatch() {
            final int end = Math.min(globalOffset + READ_BATCH_SIZE, variables.size());
            final List<DiscoveredVariable> batch = variables.subList(globalOffset, end);
            globalOffset = end;

            final List<ReadValueId> readValueIds = new ArrayList<>(batch.size() * 3);
            for (final DiscoveredVariable var : batch) {
                readValueIds.add(new ReadValueId(var.nodeId, AttributeId.DataType.uid(), null, null));
                readValueIds.add(new ReadValueId(var.nodeId, AttributeId.AccessLevel.uid(), null, null));
                readValueIds.add(new ReadValueId(var.nodeId, AttributeId.Description.uid(), null, null));
            }

            final DataValue[] values;
            try {
                values = client.readAsync(0.0, TimestampsToReturn.Neither, readValueIds)
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .getResults();
            } catch (final ExecutionException e) {
                throw new UncheckedBrowseException("Failed to read node attributes", e.getCause());
            } catch (final TimeoutException e) {
                throw new UncheckedBrowseException("Attribute read timed out after " + TIMEOUT_SECONDS + " seconds", e);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new UncheckedBrowseException("Attribute read interrupted", e);
            }
            assert values != null;

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
                        browser.generateTagNameDefault(var.browseName),
                        description,
                        browser.generateNorthboundTopicDefault(var.path),
                        browser.generateSouthboundTopicDefault(var.path)));
            }
            return result;
        }

        @Override
        public @Nullable Spliterator<BrowsedNode> trySplit() {
            return null; // sequential only
        }

        @Override
        public long estimateSize() {
            return variables.size() - globalOffset + (currentBatch != null ? currentBatch.size() - batchIndex : 0);
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

    @NotNull
    String generateTagNameDefault(final @NotNull String browseName) {
        return sanitize(browseName);
    }

    @NotNull
    String generateNorthboundTopicDefault(final @NotNull String path) {
        return adapterId + "/" + sanitizePath(path);
    }

    @NotNull
    String generateSouthboundTopicDefault(final @NotNull String path) {
        return adapterId + "/write/" + sanitizePath(path);
    }

    static @NotNull String sanitize(final @NotNull String input) {
        return input.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
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
