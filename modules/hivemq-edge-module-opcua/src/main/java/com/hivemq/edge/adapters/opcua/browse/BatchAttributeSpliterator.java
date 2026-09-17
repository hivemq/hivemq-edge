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

import static com.hivemq.edge.adapters.opcua.browse.OperationLimits.ATTRIBUTES_PER_NODE;
import static com.hivemq.edge.adapters.opcua.browse.ServiceFaults.isTooManyOperations;
import static com.hivemq.edge.adapters.opcua.browse.ServiceFaults.statusOf;

import com.hivemq.edge.adapters.browse.BrowsedNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Phase 2. Lazily batch-reads OPC UA attributes and produces {@link BrowsedNode} records. Each
 * {@link #tryAdvance} call consumes one {@link BrowsedNode} from the current batch and, when a batch is
 * exhausted, waits for the already-in-flight next batch before firing the one after. Prefetching keeps the
 * wire busy while the HTTP serializer drains the current batch — the attribute-read round-trip for batch
 * N+1 overlaps with the serialization of batch N.
 *
 * <p>Milo's channel is strictly serial, so prefetching does not add concurrent server load: the next read is
 * dispatched only after the previous response has landed on the client, but before the client has finished
 * emitting the current batch. For a typical browse the two costs (serialization + transfer vs attribute read
 * round-trip) are close to equal, so prefetching roughly halves Phase 2 wall time on large address spaces.
 *
 * <p>The batch size starts from the server's advertised {@code MaxNodesPerRead} and is halved whenever a
 * read comes back {@code Bad_TooManyOperations}, so a server that enforces a tighter limit than it advertises
 * still gets browsed instead of failing the stream (EDG-1034).
 */
final class BatchAttributeSpliterator implements Spliterator<BrowsedNode> {

    private static final @NotNull Logger log = LoggerFactory.getLogger(BatchAttributeSpliterator.class);

    private final @NotNull List<DiscoveredVariable> variables;
    private final @NotNull List<String> tagNameDefaults;
    private final @NotNull OpcUaClient client;
    private final @NotNull AttributeResolver resolver;
    private final @NotNull String adapterId;
    private final long timeoutSeconds;

    private int globalOffset;
    private @Nullable List<BrowsedNode> currentBatch;
    private int batchIndex;
    private @Nullable CompletableFuture<List<BrowsedNode>> nextBatchFuture;
    // Size of the prefetched batch that globalOffset has already been advanced past.
    // Tracked so estimateSize() can correctly count the in-flight batch as remaining,
    // which is required by the SIZED characteristic contract.
    private int pendingBatchSize;
    // Variables per read; halved on rejection, never regrown.
    private int batchSize;
    // Offset of the in-flight batch, so a rejected read can be re-issued for the same slice.
    private int inFlightStart;

    /**
     * @param variables       the sorted Phase 1 result
     * @param tagNameDefaults one per variable, same order
     * @param timeoutSeconds  budget of each read
     * @param batchSize       variables per read to start with, see {@link OperationLimits#readBatchSize()}
     */
    BatchAttributeSpliterator(
            final @NotNull List<DiscoveredVariable> variables,
            final @NotNull List<String> tagNameDefaults,
            final @NotNull OpcUaClient client,
            final @NotNull AttributeResolver resolver,
            final @NotNull String adapterId,
            final long timeoutSeconds,
            final int batchSize) {
        this.variables = variables;
        this.tagNameDefaults = tagNameDefaults;
        this.client = client;
        this.resolver = resolver;
        this.adapterId = adapterId;
        this.timeoutSeconds = timeoutSeconds;
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
        // consumption of the batch we just received. A batch is only ever fired for at least one
        // variable, so the one just received is never empty.
        currentBatch = await(nextBatchFuture);
        batchIndex = 0;
        nextBatchFuture = firePrefetch();
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
            readValueIds.add(new ReadValueId(var.nodeId(), AttributeId.DataType.uid(), null, null));
            readValueIds.add(new ReadValueId(var.nodeId(), AttributeId.AccessLevel.uid(), null, null));
            readValueIds.add(new ReadValueId(var.nodeId(), AttributeId.Description.uid(), null, null));
        }

        return client.readAsync(0.0, TimestampsToReturn.Neither, readValueIds)
                .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .thenApply(response -> buildBatch(batch, batchStart, response.getResults()));
    }

    private @NotNull List<BrowsedNode> buildBatch(
            final @NotNull List<DiscoveredVariable> batch, final int batchStart, final @Nullable DataValue[] values) {
        // One result per ReadValueId is the service contract; fewer is a server we cannot trust the
        // alignment of, not a batch with some attributes missing.
        final int expected = batch.size() * ATTRIBUTES_PER_NODE;
        if (values == null || values.length < expected) {
            throw new UncheckedBrowseException(
                    "Attribute read returned " + (values == null ? 0 : values.length) + " results for " + expected
                            + " attributes",
                    null);
        }
        final List<BrowsedNode> result = new ArrayList<>(batch.size());
        for (int i = 0; i < batch.size(); i++) {
            final DiscoveredVariable var = batch.get(i);
            final String dataType = resolver.dataTypeName(values[i * ATTRIBUTES_PER_NODE]);
            final String accessLevel = resolver.accessLevel(values[i * ATTRIBUTES_PER_NODE + 1]);
            final String description = resolver.description(values[i * ATTRIBUTES_PER_NODE + 2]);

            result.add(new BrowsedNode(
                    var.path(),
                    var.namespaceUri(),
                    var.namespaceIndex(),
                    var.nodeId().toParseableString(),
                    dataType,
                    accessLevel,
                    description,
                    tagNameDefaults.get(batchStart + i),
                    description,
                    TagDefaults.northboundTopic(adapterId, var.path()),
                    TagDefaults.southboundTopic(adapterId, var.path())));
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
                if (cause instanceof final UncheckedBrowseException browseFailure) {
                    throw browseFailure;
                }
                if (cause instanceof TimeoutException) {
                    throw new UncheckedBrowseException(
                            "Attribute read timed out after " + timeoutSeconds + " seconds", cause);
                }
                // Halve the slice that was actually rejected (the last slice can be shorter than batchSize).
                if (isTooManyOperations(cause) && pendingBatchSize > 1) {
                    final int rejected = pendingBatchSize;
                    batchSize = rejected / 2;
                    log.info(
                            "OPC UA server rejected a read of {} variables ({} attributes) for adapter '{}' ({}), retrying with {} variables per read",
                            rejected,
                            rejected * ATTRIBUTES_PER_NODE,
                            adapterId,
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
