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
package com.hivemq.edge.adapters.opcua.client;

import static java.util.Objects.requireNonNullElse;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import org.eclipse.milo.opcua.sdk.client.DiscoveryClient;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.sdk.client.OperationLimits;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.eclipse.milo.opcua.stack.transport.client.tcp.OpcTcpClientTransport;
import org.eclipse.milo.opcua.stack.transport.client.tcp.OpcTcpClientTransportConfig;
import org.eclipse.milo.opcua.stack.transport.client.tcp.OpcTcpClientTransportConfigBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Milo's client, except that a server advertising no Read or Browse limit (absent or 0) gets batches of
 * {@link #DEFAULT_OPERATIONS_PER_REQUEST} operations instead of one per request.
 * <p>
 * Milo 1.1.7 sends one operation per request in that case, so building the data-type tree takes one round trip per
 * type and step: on the CAS-QS-3110-F gateway, over a 95 ms link, 103 s instead of 14 s, past the connect-time
 * ceiling (EDG-1125). 100 is what Edge's own browser uses for the same case. A server that cannot take 100 refuses
 * the batch with {@code Bad_TooManyOperations}, and Milo replays it one operation at a time.
 * <p>
 * A batch can also run out of continuation points, or be refused node by node: see {@link #browse(List)}.
 * <p>
 * When Edge moves to Milo 1.2, which can override the limits in its client configuration, remove
 * {@link #getOperationLimits()}; {@link #browse(List)} stays as long as Milo reads a refused node as one without
 * references.
 */
public class EdgeOpcUaClient extends OpcUaClient {

    static final int DEFAULT_OPERATIONS_PER_REQUEST = 100;

    /** Milo's own bound on the BrowseNext calls that complete one node's result. */
    static final int MAX_BROWSE_NEXT_CALLS = 1000;

    public EdgeOpcUaClient(final @NotNull OpcUaClientConfig config, final @NotNull OpcTcpClientTransport transport) {
        super(config, transport);
    }

    /**
     * {@link OpcUaClient#create(String, Function, Consumer, Consumer)}, step for step, returning this class: Milo's
     * factory constructs its own.
     */
    public static @NotNull EdgeOpcUaClient create(
            final @NotNull String endpointUrl,
            final @NotNull Function<List<EndpointDescription>, Optional<EndpointDescription>> selectEndpoint,
            final @NotNull Consumer<OpcTcpClientTransportConfigBuilder> configureTransport,
            final @NotNull Consumer<OpcUaClientConfigBuilder> configureClient)
            throws UaException {
        try {
            final List<EndpointDescription> endpoints = DiscoveryClient.getEndpoints(endpointUrl, configureTransport)
                    .get();
            final EndpointDescription endpoint = selectEndpoint
                    .apply(endpoints)
                    .orElseThrow(() -> new UaException(StatusCodes.Bad_ConfigurationError, "no endpoint selected"));

            final OpcTcpClientTransportConfigBuilder transportConfig = OpcTcpClientTransportConfig.newBuilder();
            configureTransport.accept(transportConfig);

            final OpcUaClientConfigBuilder clientConfig = OpcUaClientConfig.builder();
            clientConfig.setEndpoint(endpoint);
            clientConfig.setDiscoveryEndpoints(endpoints);
            clientConfig.setSessionEndpointValidationEnabled(false);
            configureClient.accept(clientConfig);

            return new EdgeOpcUaClient(clientConfig.build(), new OpcTcpClientTransport(transportConfig.build()));
        } catch (final @NotNull ExecutionException e) {
            if (endpointUrl.endsWith("/discovery")) {
                throw new UaException(e.getCause());
            }
            final String discoveryUrl =
                    endpointUrl.endsWith("/") ? endpointUrl + "discovery" : endpointUrl + "/discovery";
            return create(discoveryUrl, selectEndpoint, configureTransport, configureClient);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UaException(StatusCodes.Bad_UnexpectedError, e);
        }
    }

    /**
     * Milo's batched Browse, except that no node comes back refused because of the batch.
     * <p>
     * A server pages a large result behind a continuation point, and holds only a few of them per session. In a
     * batch, the nodes past that pool answer {@code Bad_NoContinuationPoints}; some servers also answer a batch they
     * will not serve with {@code Bad_TooManyOperations} on every result instead of refusing the request. Milo's
     * type-tree builds read either as a node without references, so the cached data-type tree silently misses types.
     * Here every continuation point of the batch is drained first, which frees the pool, and each refused node is
     * then browsed again on its own. A node refused even then fails the call, so the build fails instead of caching a
     * partial tree.
     * <p>
     * The results come back complete, without continuation points. A call abandoned while a request is in flight
     * (interrupted at a deadline) still gives back the continuation points its late answer brings.
     */
    @Override
    public @NotNull List<BrowseResult> browse(final @NotNull List<BrowseDescription> nodesToBrowse) throws UaException {
        final List<BrowseResult> results = new ArrayList<>(awaitBrowse(nodesToBrowse));
        if (results.size() != nodesToBrowse.size()) {
            releaseAll(results); // Milo rejects the mismatch itself; the points that did arrive would stay allocated
            return results;
        }
        for (int i = 0; i < results.size(); i++) {
            try {
                results.set(i, drained(results.get(i)));
            } catch (final UaException e) {
                releaseAll(results.subList(i + 1, results.size()));
                throw e;
            }
        }
        for (int i = 0; i < results.size(); i++) {
            if (refused(results.get(i))) {
                final BrowseResult alone =
                        awaitBrowse(List.of(nodesToBrowse.get(i))).get(0);
                if (refused(alone)) {
                    throw new UaException(
                            alone.getStatusCode(),
                            "Browse of " + nodesToBrowse.get(i).getNodeId() + " was refused even on its own");
                }
                results.set(i, drained(alone));
            }
        }
        return results;
    }

    private static boolean refused(final @NotNull BrowseResult result) {
        final long status = result.getStatusCode().value();
        return status == StatusCodes.Bad_NoContinuationPoints || status == StatusCodes.Bad_TooManyOperations;
    }

    private @NotNull BrowseResult drained(final @NotNull BrowseResult result) throws UaException {
        ByteString continuationPoint = result.getContinuationPoint();
        if (!result.getStatusCode().isGood() || continuationPoint == null || continuationPoint.isNull()) {
            return result;
        }
        final List<ReferenceDescription> references = new ArrayList<>(references(result));
        int calls = 0;
        while (continuationPoint != null && continuationPoint.isNotNull()) {
            if (++calls > MAX_BROWSE_NEXT_CALLS) {
                release(List.of(continuationPoint));
                throw new UaException(
                        StatusCodes.Bad_UnexpectedError,
                        "BrowseNext did not complete after " + MAX_BROWSE_NEXT_CALLS + " calls");
            }
            // A BrowseNext that fails, times out or answers badly may leave the continuation point allocated.
            final List<BrowseResult> next;
            try {
                next = awaitBrowseNext(continuationPoint);
            } catch (final UaException e) {
                release(List.of(continuationPoint));
                throw e;
            }
            if (next.size() != 1) {
                release(List.of(continuationPoint));
                throw new UaException(StatusCodes.Bad_UnexpectedError, "BrowseNext returned no single result");
            }
            if (!next.get(0).getStatusCode().isGood()) {
                release(List.of(continuationPoint));
                throw new UaException(next.get(0).getStatusCode(), "BrowseNext failed");
            }
            references.addAll(references(next.get(0)));
            continuationPoint = next.get(0).getContinuationPoint();
        }
        return new BrowseResult(
                result.getStatusCode(), ByteString.NULL_VALUE, references.toArray(ReferenceDescription[]::new));
    }

    private static @NotNull List<ReferenceDescription> references(final @NotNull BrowseResult result) {
        return Arrays.asList(requireNonNullElse(result.getReferences(), new ReferenceDescription[0]));
    }

    private void releaseAll(final @NotNull List<BrowseResult> results) {
        final List<ByteString> continuationPoints = new ArrayList<>();
        for (final BrowseResult result : results) {
            final ByteString continuationPoint = result.getContinuationPoint();
            if (continuationPoint != null && continuationPoint.isNotNull()) {
                continuationPoints.add(continuationPoint);
            }
        }
        if (!continuationPoints.isEmpty()) {
            release(continuationPoints);
        }
    }

    /**
     * Best effort, and not waited for: the call is failing already, and a wait may be what just failed. A
     * continuation point that is never released stays allocated until the session closes.
     * <p>
     * One request per continuation point: a server may refuse a BrowseNext carrying more of them than its pool holds,
     * and a batch can be handed one more than that.
     */
    private void release(final @NotNull List<ByteString> continuationPoints) {
        for (final ByteString continuationPoint : continuationPoints) {
            browseNextAsync(true, List.of(continuationPoint)).exceptionally(ignored -> null);
        }
    }

    private @NotNull List<BrowseResult> awaitBrowse(final @NotNull List<BrowseDescription> nodesToBrowse)
            throws UaException {
        return await(browseAsync(nodesToBrowse), results -> results);
    }

    private @NotNull List<BrowseResult> awaitBrowseNext(final @NotNull ByteString continuationPoint)
            throws UaException {
        return await(
                browseNextAsync(false, List.of(continuationPoint)),
                response -> Arrays.asList(requireNonNullElse(response.getResults(), new BrowseResult[0])));
    }

    /**
     * {@link OpcUaClient#browse(List)}'s wait, except that an interrupted wait does not stop the answer: it still
     * arrives and may bring continuation points nobody will drain, so they are given back when it does.
     */
    private <R> @NotNull List<BrowseResult> await(
            final @NotNull CompletableFuture<R> response, final @NotNull Function<R, List<BrowseResult>> results)
            throws UaException {
        try {
            return results.apply(response.get());
        } catch (final ExecutionException e) {
            throw new UaException(e.getCause());
        } catch (final InterruptedException e) {
            response.thenAccept(late -> releaseAll(results.apply(late)));
            Thread.currentThread().interrupt();
            throw new UaException(StatusCodes.Bad_UnexpectedError, e);
        }
    }

    @Override
    public @NotNull OperationLimits getOperationLimits() throws UaException {
        return withDefaultBatchSizes(super.getOperationLimits());
    }

    static @NotNull OperationLimits withDefaultBatchSizes(final @NotNull OperationLimits limits) {
        return new OperationLimits(
                defaulted(limits.maxNodesPerRead().orElse(null)),
                limits.maxNodesPerWrite().orElse(null),
                limits.maxNodesPerMethodCall().orElse(null),
                defaulted(limits.maxNodesPerBrowse().orElse(null)),
                limits.maxNodesPerRegisterNodes().orElse(null),
                limits.maxNodesPerTranslateBrowsePathsToNodeIds().orElse(null),
                limits.maxNodesPerNodeManagement().orElse(null),
                limits.maxMonitoredItemsPerCall().orElse(null),
                limits.maxNodesPerHistoryReadData().orElse(null),
                limits.maxNodesPerHistoryReadEvents().orElse(null),
                limits.maxNodesPerHistoryUpdateData().orElse(null),
                limits.maxNodesPerHistoryUpdateEvents().orElse(null));
    }

    private static @NotNull UInteger defaulted(final @Nullable UInteger limit) {
        return limit == null || limit.longValue() == 0 ? uint(DEFAULT_OPERATIONS_PER_REQUEST) : limit;
    }
}
