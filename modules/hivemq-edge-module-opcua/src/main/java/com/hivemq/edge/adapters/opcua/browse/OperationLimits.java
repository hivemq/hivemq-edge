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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The server's advertised operation limits and the request sizes derived from them. 0 means not advertised /
 * unlimited, and the defaults apply. An advertised value is a starting point, not a contract: the callers
 * halve their requests when the server refuses what it advertised (EDG-1034).
 *
 * @param maxNodesPerRead             {@code MaxNodesPerRead} (i=11705), counted in ReadValueIds
 * @param maxNodesPerBrowse           {@code MaxNodesPerBrowse} (i=11710), counted in BrowseDescriptions
 * @param maxBrowseContinuationPoints {@code MaxBrowseContinuationPoints} (i=2735), cursors per session
 */
record OperationLimits(int maxNodesPerRead, int maxNodesPerBrowse, int maxBrowseContinuationPoints) {

    static final @NotNull OperationLimits NONE = new OperationLimits(0, 0, 0);

    /** Variables per Phase 2 read when the server advertises no limit or a larger one. */
    static final int READ_BATCH_SIZE = 100;
    /** Nodes per Browse request when the server advertises no limit or a larger one. */
    static final int BROWSE_CHUNK_SIZE = 100;
    // Attributes read per variable in Phase 2 (DataType, AccessLevel, Description). A server's MaxNodesPerRead
    // limit counts ReadValueIds, not distinct nodes, so a batch of N variables is N * ATTRIBUTES_PER_NODE
    // operations (EDG-1034).
    static final int ATTRIBUTES_PER_NODE = 3;

    /**
     * Reads the three limits in one request. A limit resolves to 0 when the node is missing, the value is not
     * a number, or the read fails — never exceptionally, so a server without operation limits browses with the
     * defaults. The future is fired here and joined by the caller when a phase needs it, so its round trip
     * overlaps the start of the traversal.
     */
    static @NotNull CompletableFuture<OperationLimits> read(
            final @NotNull OpcUaClient client, final long timeoutSeconds) {
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
                        return NONE;
                    }
                    return new OperationLimits(limitValue(results[0]), limitValue(results[1]), limitValue(results[2]));
                })
                .exceptionally(error -> NONE);
    }

    static int limitValue(final @Nullable DataValue value) {
        if (value == null || value.getValue() == null) {
            return 0;
        }
        // MaxNodesPerRead / MaxNodesPerBrowse are UInt32, MaxBrowseContinuationPoints is UInt16.
        return value.getValue().getValue() instanceof final Number limit ? limit.intValue() : 0;
    }

    /**
     * Variables per Phase 2 read so that {@code variables * ATTRIBUTES_PER_NODE} stays within
     * {@code MaxNodesPerRead}. Never below 1. WAGO PFC200 / Codesys servers advertise 100 and enforce it on the
     * ReadValueId count, so they get 33 variables per read instead of the default 100.
     */
    int readBatchSize() {
        if (maxNodesPerRead <= 0) {
            return READ_BATCH_SIZE;
        }
        return Math.max(1, Math.min(READ_BATCH_SIZE, maxNodesPerRead / ATTRIBUTES_PER_NODE));
    }

    /** Nodes per Browse request: the default, or fewer if {@code MaxNodesPerBrowse} is smaller. Never below 1. */
    int browseChunkSize() {
        if (maxNodesPerBrowse <= 0) {
            return BROWSE_CHUNK_SIZE;
        }
        return Math.max(1, Math.min(BROWSE_CHUNK_SIZE, maxNodesPerBrowse));
    }

    /**
     * Chunk size for re-browsing nodes that got {@code Bad_NoContinuationPoints}: the advertised
     * {@code MaxBrowseContinuationPoints} when the server has one and it is smaller than what was just tried,
     * otherwise half of what was just tried. Never below 1; at 1 a repeat of the fault fails the browse.
     */
    int retryChunkSize(final int tried) {
        if (maxBrowseContinuationPoints > 0 && maxBrowseContinuationPoints < tried) {
            return maxBrowseContinuationPoints;
        }
        return Math.max(1, tried / 2);
    }

    /**
     * Cursors per BrowseNext when draining: the advertised capacity — Milo hands out more cursors per Browse
     * than it accepts per BrowseNext — or all {@code pending} ones when none is advertised.
     */
    int continuationPointsPerBrowseNext(final int pending) {
        return maxBrowseContinuationPoints > 0 ? maxBrowseContinuationPoints : pending;
    }

    /**
     * Cursors per release request: the advertised capacity, one when none is advertised. A release above the
     * server's capacity is refused as a whole and would leak the very cursors it is meant to free.
     */
    int continuationPointsPerRelease() {
        return maxBrowseContinuationPoints > 0 ? maxBrowseContinuationPoints : 1;
    }
}
