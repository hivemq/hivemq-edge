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

import com.hivemq.edge.adapters.browse.BrowseException;
import com.hivemq.edge.adapters.browse.BrowsedNode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Browses an OPC-UA address space and collects variable nodes with their attributes.
 * Builds {@link BrowsedNode} records with informational fields and generated defaults.
 *
 * <p>The browse is two-phase: (1) {@link AddressSpaceWalker} collects variable node references level by
 * level, one batched {@code Browse} request per level chunk, each chunk browsed and drained by
 * {@link ChunkBrowser} under a permit so browses never overlap on the device; (2)
 * {@link BatchAttributeSpliterator} batch-reads DataType, AccessLevel and Description lazily as the stream
 * is consumed. Both phases size their requests to the server's {@link OperationLimits}. This class parses
 * the root, wires the two phases together and maps every failure to a {@link BrowseException}.
 */
public class OpcUaNodeBrowser {

    private static final long TIMEOUT_SECONDS = 120;

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
            final Deadline deadline = Deadline.after(timeoutSeconds);
            // Fired before Phase 1 so its round-trip overlaps the start of the traversal.
            final CompletableFuture<OperationLimits> limitsRead = OperationLimits.read(client, timeoutSeconds);
            final OperationLimits limits = deadline.await(limitsRead);

            // Phase 1: level-wise traversal collecting every variable reachable from the root with its path.
            final ChunkBrowser chunkBrowser =
                    new ChunkBrowser(client, adapterId, maxReferencesPerNode, concurrency, limits);
            final List<DiscoveredVariable> variables = new AddressSpaceWalker(client, adapterId, chunkBrowser, limits)
                    .walk(browseRoot, maxDepth == 0 ? Integer.MAX_VALUE : maxDepth, deadline);

            if (variables.isEmpty()) {
                return Stream.empty();
            }

            // Sort by path early (DiscoveredVariable is small) so the output stream is ordered
            // without needing to materialize the full List<BrowsedNode>. Nodes sharing a path
            // (e.g. Prosys simulation instances) are tie-broken on the NodeId so the collision
            // suffixes in tagNameDefaults never depend on the order the server listed them in.
            variables.sort(Comparator.comparing(DiscoveredVariable::path)
                    .thenComparing(v -> v.nodeId().toParseableString()));

            // Pre-compute unique tag name defaults. Multiple nodes can share the same browse
            // path (e.g. Prosys simulation instances), so we append a numeric suffix on collision.
            final List<String> tagNameDefaults = TagDefaults.tagNames(variables);

            // Phase 2: Return a stream that lazily batch-reads attributes as it is consumed.
            return StreamSupport.stream(
                    new BatchAttributeSpliterator(
                            variables,
                            tagNameDefaults,
                            client,
                            AttributeResolver.forClient(client),
                            adapterId,
                            timeoutSeconds,
                            limits.readBatchSize()),
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
}
