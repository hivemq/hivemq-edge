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

import static com.hivemq.edge.adapters.opcua.browse.ServiceFaults.isTooManyOperations;
import static com.hivemq.edge.adapters.opcua.browse.ServiceFaults.statusOf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * <p>Requests the server refuses as too big are halved and retried; nodes it cannot page for lack of
 * continuation points are browsed again in chunks small enough for its pool. The I/O of a chunk — the
 * request, its pages, the cursor accounting — is {@link ChunkBrowser}'s.
 */
final class AddressSpaceWalker {

    private static final @NotNull Logger log = LoggerFactory.getLogger(AddressSpaceWalker.class);

    // A node refused with Bad_NoContinuationPoints even when browsed alone is retried this often, pausing in
    // between, before the browse fails: the pool is per session and another client may be draining it.
    static final int SINGLE_NODE_CONTINUATION_RETRIES = 3;
    static final long CONTINUATION_RETRY_PAUSE_MILLIS = 500;

    private final @NotNull OpcUaClient client;
    private final @NotNull String adapterId;
    private final @NotNull ChunkBrowser chunkBrowser;
    private final @NotNull OperationLimits limits;

    AddressSpaceWalker(
            final @NotNull OpcUaClient client,
            final @NotNull String adapterId,
            final @NotNull ChunkBrowser chunkBrowser,
            final @NotNull OperationLimits limits) {
        this.client = client;
        this.adapterId = adapterId;
        this.chunkBrowser = chunkBrowser;
        this.limits = limits;
    }

    /**
     * Every Variable reachable from {@code root} within {@code maxDepth} levels, in discovery order.
     *
     * @param maxDepth levels to descend; {@link Integer#MAX_VALUE} for unlimited
     */
    @NotNull
    List<DiscoveredVariable> walk(final @NotNull NodeId root, final int maxDepth, final @NotNull Deadline deadline)
            throws ExecutionException, InterruptedException, TimeoutException {
        final Map<NodeId, DiscoveredVariable> variables = new LinkedHashMap<>();
        final Set<NodeId> visited = new HashSet<>();
        visited.add(root);
        List<PendingNode> level = List.of(new PendingNode(root, "", maxDepth));
        int chunkSize = limits.browseChunkSize();

        while (!level.isEmpty()) {
            final List<PendingNode> next = new ArrayList<>();
            int offset = 0;
            while (offset < level.size()) {
                final List<PendingNode> chunk = level.subList(offset, Math.min(offset + chunkSize, level.size()));
                final ChunkBrowser.ChunkResult result;
                try {
                    result = chunkBrowser.browse(chunk, deadline);
                } catch (final ExecutionException e) {
                    if (isTooManyOperations(e.getCause()) && chunk.size() > 1) {
                        final int rejected = chunk.size();
                        chunkSize = rejected / 2;
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
                collect(chunk, result, variables, visited, next);
                offset += chunk.size();
                rebrowseExhausted(chunk, result.exhausted(), deadline, variables, visited, next);
            }
            level = next;
        }
        return new ArrayList<>(variables.values());
    }

    /**
     * Nodes the server could not page because it ran out of continuation points: browse them again in chunks
     * small enough that every node of a chunk can hold a point at once. The S7-1500 advertises 5, so a level
     * chunk of 100 nodes with six overflowing folders fails six times over.
     */
    private void rebrowseExhausted(
            final @NotNull List<PendingNode> chunk,
            final @NotNull List<PendingNode> firstExhausted,
            final @NotNull Deadline deadline,
            final @NotNull Map<NodeId, DiscoveredVariable> variables,
            final @NotNull Set<NodeId> visited,
            final @NotNull List<PendingNode> next)
            throws ExecutionException, InterruptedException, TimeoutException {
        List<PendingNode> exhausted = firstExhausted;
        int retrySize = chunk.size();
        int singleNodeAttempts = 0;
        while (!exhausted.isEmpty()) {
            if (retrySize == 1) {
                // Even one node at a time is refused: another client is holding the server's whole pool
                // (UaExpert, TIA Portal, a second adapter). Give it a moment, a few times.
                if (++singleNodeAttempts > SINGLE_NODE_CONTINUATION_RETRIES) {
                    throw new UncheckedBrowseException(
                            "Browse at path '" + exhausted.getFirst().path()
                                    + "' returned non-Good status: Bad_NoContinuationPoints after "
                                    + SINGLE_NODE_CONTINUATION_RETRIES + " retries",
                            null);
                }
                Thread.sleep(Math.min(CONTINUATION_RETRY_PAUSE_MILLIS, deadline.remainingMillis()));
            }
            retrySize = limits.retryChunkSize(retrySize);
            log.info(
                    "OPC UA server ran out of continuation points for {} of {} browsed nodes for adapter '{}', re-browsing them {} at a time",
                    exhausted.size(),
                    chunk.size(),
                    adapterId,
                    retrySize);
            final List<PendingNode> stillExhausted = new ArrayList<>();
            for (int start = 0; start < exhausted.size(); start += retrySize) {
                final List<PendingNode> retry = exhausted.subList(start, Math.min(start + retrySize, exhausted.size()));
                final ChunkBrowser.ChunkResult retried = chunkBrowser.browse(retry, deadline);
                collect(retry, retried, variables, visited, next);
                stillExhausted.addAll(retried.exhausted());
            }
            exhausted = stillExhausted;
        }
    }

    private void collect(
            final @NotNull List<PendingNode> chunk,
            final @NotNull ChunkBrowser.ChunkResult result,
            final @NotNull Map<NodeId, DiscoveredVariable> variables,
            final @NotNull Set<NodeId> visited,
            final @NotNull List<PendingNode> next) {
        for (int i = 0; i < chunk.size(); i++) {
            final List<ReferenceDescription> references = result.references().get(i);
            if (references != null) {
                collectReferences(chunk.get(i), references, variables, visited, next);
            }
        }
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
            final String childPath = parent.path() + "/" + browseName;

            final Optional<NodeId> resolvedNodeId = rd.getNodeId().toNodeId(nsTable);
            if (resolvedNodeId.isEmpty()) {
                continue;
            }
            final NodeId nodeId = resolvedNodeId.get();

            if (rd.getNodeClass() == NodeClass.Variable && !variables.containsKey(nodeId)) {
                final int nsIndex = nodeId.getNamespaceIndex().intValue();
                // An index the client's table does not know is reported by number.
                final String nsUri = nsTable.get(nsIndex);
                variables.put(
                        nodeId,
                        new DiscoveredVariable(
                                nodeId,
                                childPath,
                                nsUri != null ? nsUri : String.valueOf(nsIndex),
                                nsIndex,
                                browseName));
            }

            if (parent.remainingDepth() > 1 && visited.add(nodeId)) {
                next.add(new PendingNode(nodeId, childPath, parent.remainingDepth() - 1));
            }
        }
    }
}
