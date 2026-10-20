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

import com.hivemq.edge.adapters.opcua.browse.ServiceFaults;
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
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
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
 * ceiling (EDG-1125). 100 is the Browse batch Edge's own browser uses for the same case. A server that cannot take
 * 100 refuses the batch with {@code Bad_TooManyOperations}, and Milo replays it one operation at a time; a server
 * that refuses it for its message size instead gets the same replay (see {@link #browse(List)} and
 * {@link #read(double, TimestampsToReturn, List)}).
 * <p>
 * A batch can also run out of continuation points, or be refused node by node: see {@link #browse(List)} and
 * {@link #read(double, TimestampsToReturn, List)}.
 * <p>
 * When Edge moves to Milo 1.2, which can override the limits in its client configuration, remove
 * {@link #getOperationLimits()}; {@link #browse(List)} and {@link #read(double, TimestampsToReturn, List)} stay as
 * long as Milo reads a refused operation as an answer about the node.
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
     * A server pages a large result behind a continuation point, and holds only a few of them per session. In a batch,
     * the nodes past that pool answer {@code Bad_NoContinuationPoints}, and a server may free a point it handed out to
     * serve a later request, which then answers {@code Bad_ContinuationPointInvalid}. A server may also answer a batch
     * it will not serve with a service result on every result instead of refusing the request: too many operations, a
     * response too large, no resources, a busy server, a timeout (see {@link #refused(StatusCode)}). Milo's type-tree
     * builds read any of them as a node without references, so the cached data-type tree silently misses types. Here
     * every continuation point of the batch is drained first, which frees the pool, and each refused node is then
     * browsed again on its own. A node refused even then for a reason about the request, not the node (see {@link
     * #refusedForTheBatch(StatusCode)}), or whose continuation is refused, fails the call, so the build fails instead
     * of caching a partial tree; any other refusal of the node on its own goes back to Milo as the node's answer, as
     * Milo's own client gets it. A batch refused as a whole for its size is refused as too many operations, which Milo
     * replays one operation at a time.
     * <p>
     * The results come back drained, without continuation points. A call abandoned while a request is in flight
     * (interrupted at a deadline) still gives back the continuation points its late answer brings.
     */
    @Override
    public @NotNull List<BrowseResult> browse(final @NotNull List<BrowseDescription> nodesToBrowse) throws UaException {
        final List<BrowseResult> results;
        try {
            results = new ArrayList<>(awaitBrowse(nodesToBrowse));
        } catch (final UaException e) {
            throw tooManyOperationsIfTooLarge(nodesToBrowse.size(), e);
        }
        if (results.size() != nodesToBrowse.size()) {
            releaseAll(results); // Milo rejects the mismatch itself; the points that did arrive would stay allocated
            return results;
        }
        for (int i = 0; i < results.size(); i++) {
            try {
                results.set(i, drained(results.get(i), false));
            } catch (final UaException e) {
                releaseAll(results.subList(i + 1, results.size()));
                throw e;
            }
        }
        for (int i = 0; i < results.size(); i++) {
            if (refused(results.get(i).getStatusCode())) {
                final BrowseResult alone = drained(browseAlone(nodesToBrowse.get(i)), true);
                if (refusedForTheBatch(alone.getStatusCode())) {
                    throw new UaException(
                            alone.getStatusCode(),
                            "Browse of " + nodesToBrowse.get(i).getNodeId() + " was refused even on its own");
                }
                results.set(i, alone);
            }
        }
        return results;
    }

    /**
     * Milo's Read, with the same treatment as {@link #browse(List)}: an operation refused because of the batch is read
     * again on its own, one refused even then for a reason about the request fails the call, and a batch refused as a
     * whole for its size is refused as too many operations. Milo's type-tree builds, the only callers of this method
     * (Edge reads with {@code readAsync}), read a refused {@code IsAbstract} as concrete and a refused {@code
     * DataTypeDefinition} as none, and cache the type that way.
     */
    @Override
    public @NotNull ReadResponse read(
            final double maxAge,
            final @NotNull TimestampsToReturn timestampsToReturn,
            final @NotNull List<ReadValueId> readValueIds)
            throws UaException {
        final ReadResponse response;
        try {
            response = super.read(maxAge, timestampsToReturn, readValueIds);
        } catch (final UaException e) {
            throw tooManyOperationsIfTooLarge(readValueIds.size(), e);
        }
        final DataValue[] values = response.getResults();
        if (values == null || values.length != readValueIds.size()) {
            return response; // Milo rejects the mismatch itself
        }
        DataValue[] completed = null;
        for (int i = 0; i < values.length; i++) {
            if (!refused(values[i].getStatusCode())) {
                continue;
            }
            final DataValue alone =
                    readValueIds.size() == 1 ? values[i] : readAlone(maxAge, timestampsToReturn, readValueIds.get(i));
            if (refusedForTheBatch(alone.getStatusCode())) {
                throw new UaException(
                        requireNonNullElse(alone.getStatusCode(), StatusCode.BAD),
                        "Read of " + readValueIds.get(i).getNodeId() + " was refused even on its own");
            }
            if (completed == null) {
                completed = values.clone();
            }
            completed[i] = alone;
        }
        return completed == null
                ? response
                : new ReadResponse(response.getResponseHeader(), completed, response.getDiagnosticInfos());
    }

    /**
     * A status that may say the server did not serve the operation in this request, rather than answer about the
     * node: the common service results may also be returned per operation (OPC UA Part 4, 7.38.2). Such an operation
     * is sent again on its own.
     */
    private static boolean refused(final @Nullable StatusCode status) {
        if (status == null) {
            return false;
        }
        final long value = status.value();
        return refusedForTheBatch(status)
                || value == StatusCodes.Bad_ResponseTooLarge
                || value == StatusCodes.Bad_EncodingLimitsExceeded
                || value == StatusCodes.Bad_ResourceUnavailable
                || value == StatusCodes.Bad_OutOfMemory
                || value == StatusCodes.Bad_Timeout
                || value == StatusCodes.Bad_ServerTooBusy;
    }

    /**
     * A refusal that says nothing about the node, only about the request it came in: one sent on its own cannot get
     * it, so getting it then fails the call. The other refusals, on their own, may be the node's answer (a definition
     * too large to encode, a backend that is down) and go back to Milo as they came, as Milo's own client would get
     * them; failing the call instead would fail every build of the tree on that server.
     */
    private static boolean refusedForTheBatch(final @Nullable StatusCode status) {
        if (status == null) {
            return false;
        }
        final long value = status.value();
        return value == StatusCodes.Bad_NoContinuationPoints
                || value == StatusCodes.Bad_ContinuationPointInvalid
                || value == StatusCodes.Bad_TooManyOperations;
    }

    /**
     * Milo replays a batch one operation at a time only when the server refuses it with {@code Bad_TooManyOperations};
     * a server may refuse it for the size of the request or response instead, which Edge's own browser answers the
     * same way.
     */
    private static @NotNull UaException tooManyOperationsIfTooLarge(
            final int operations, final @NotNull UaException e) {
        if (operations > 1
                && e.getStatusCode().value() != StatusCodes.Bad_TooManyOperations
                && ServiceFaults.isTooManyOperations(e)) {
            return new UaException(
                    StatusCodes.Bad_TooManyOperations,
                    "A request of " + operations + " operations was refused for its size: " + e.getStatusCode(),
                    e);
        }
        return e;
    }

    private @NotNull DataValue readAlone(
            final double maxAge,
            final @NotNull TimestampsToReturn timestampsToReturn,
            final @NotNull ReadValueId readValueId)
            throws UaException {
        final DataValue[] values = requireNonNullElse(
                super.read(maxAge, timestampsToReturn, List.of(readValueId)).getResults(), new DataValue[0]);
        if (values.length != 1) {
            throw new UaException(StatusCodes.Bad_UnexpectedError, "Read returned no single result");
        }
        return values[0];
    }

    private @NotNull BrowseResult browseAlone(final @NotNull BrowseDescription node) throws UaException {
        final List<BrowseResult> results = awaitBrowse(List.of(node));
        if (results.size() != 1) {
            releaseAll(results);
            throw new UaException(StatusCodes.Bad_UnexpectedError, "Browse returned no single result");
        }
        return results.get(0);
    }

    private static boolean pending(final @Nullable ByteString continuationPoint) {
        // An empty continuation point ends the result as a null one does; a BrowseNext with it is refused.
        return continuationPoint != null && !continuationPoint.isNullOrEmpty();
    }

    /**
     * {@code result} with every page of its references, or, when the server refuses a page, a result carrying that
     * refusal for the caller to browse the node again on its own; the node was already browsed on its own when
     * {@code alone} is set, and the refusal then fails the call: it is about the continuation, not the node, whose own
     * answer was a page of references.
     */
    private @NotNull BrowseResult drained(final @NotNull BrowseResult result, final boolean alone) throws UaException {
        ByteString continuationPoint = result.getContinuationPoint();
        if (!result.getStatusCode().isGood()) {
            return result;
        }
        if (!pending(continuationPoint)) {
            // Milo pages on any non-null continuation point, an empty one included.
            return continuationPoint == null || continuationPoint.isNull()
                    ? result
                    : new BrowseResult(result.getStatusCode(), ByteString.NULL_VALUE, result.getReferences());
        }
        final List<ReferenceDescription> references = new ArrayList<>(references(result));
        int calls = 0;
        while (pending(continuationPoint)) {
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
            final StatusCode status = next.get(0).getStatusCode();
            if (!status.isGood()) {
                release(List.of(continuationPoint));
                if (refused(status) && !alone) {
                    // A point the server freed for another request, or a page it would not serve now: the caller
                    // browses the node again on its own.
                    return new BrowseResult(status, ByteString.NULL_VALUE, null);
                }
                if (refused(status)) {
                    throw new UaException(status, "BrowseNext was refused even on its own");
                }
                throw new UaException(status, "BrowseNext failed");
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
            if (pending(continuationPoint)) {
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
     * One request per continuation point: a BrowseNext may carry no more of them than the server's
     * {@code MaxNodesPerBrowse} (OPC UA Part 5, 6.3.11), and Milo's own server refuses more than its pool holds, while
     * a batch can be handed one more than that.
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
