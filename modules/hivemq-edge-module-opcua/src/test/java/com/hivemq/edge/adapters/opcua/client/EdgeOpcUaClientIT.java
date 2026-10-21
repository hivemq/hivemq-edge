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

import static com.hivemq.edge.adapters.opcua.client.EdgeOpcUaClient.DEFAULT_OPERATIONS_PER_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.milo.opcua.sdk.client.DiscoveryClient;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.OpcUaServerConfigLimits;
import org.eclipse.milo.opcua.sdk.server.Session;
import org.eclipse.milo.opcua.sdk.server.servicesets.impl.DefaultAttributeServiceSet;
import org.eclipse.milo.opcua.sdk.server.servicesets.impl.DefaultViewServiceSet;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseNextRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseNextResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.eclipse.milo.opcua.stack.transport.client.tcp.OpcTcpClientTransport;
import org.eclipse.milo.opcua.stack.transport.client.tcp.OpcTcpClientTransportConfig;
import org.eclipse.milo.opcua.stack.transport.server.ServiceRequestContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import util.EmbeddedOpcUaServerExtension;

/// The Read and Browse requests a data-type tree build sends, against servers that advertise 0 for
/// both limits, as the CAS-QS-3110-F gateway does (EDG-1125). Each case first builds a reference
/// tree while the server still advertises what it enforces.
class EdgeOpcUaClientIT {

    @Nested
    class ServerAcceptingTheDefault {

        @RegisterExtension
        final @NotNull EmbeddedOpcUaServerExtension server = new EmbeddedOpcUaServerExtension();

        @Test
        void buildsTheSameTree_inBatchesOfTheDefault() throws Exception {
            final Set<NodeId> reference = buildTree(new RecordingClient(server));
            server.advertiseOperationLimits(uint(0), uint(0));
            final RecordingClient client = new RecordingClient(server);

            assertThat(buildTree(client)).isEqualTo(reference);
            assertThat(client.browses).allMatch(size -> size <= DEFAULT_OPERATIONS_PER_REQUEST);
            assertThat(client.browses).contains(DEFAULT_OPERATIONS_PER_REQUEST);
            assertThat(client.reads).allMatch(size -> size <= DEFAULT_OPERATIONS_PER_REQUEST);
            assertThat(client.reads).contains(DEFAULT_OPERATIONS_PER_REQUEST);
        }
    }

    @Nested
    class ServerRefusingTheDefault {

        @RegisterExtension
        final @NotNull EmbeddedOpcUaServerExtension server = new EmbeddedOpcUaServerExtension(maxNodesPerRequest(10));

        /// The server refuses a batch of the default size with `Bad_TooManyOperations`; Milo
        /// replays it one operation at a time.
        @Test
        void buildsTheSameTree_oneOperationAtATime() throws Exception {
            final Set<NodeId> reference = buildTree(new RecordingClient(server));
            server.advertiseOperationLimits(uint(0), uint(0));
            final RecordingClient client = new RecordingClient(server);

            assertThat(buildTree(client)).isEqualTo(reference);
            assertThat(client.browses).contains(DEFAULT_OPERATIONS_PER_REQUEST, 1);
            assertThat(client.reads).contains(DEFAULT_OPERATIONS_PER_REQUEST, 1);
        }
    }

    @Nested
    class ServerHoldingOneContinuationPoint {

        @RegisterExtension
        final @NotNull EmbeddedOpcUaServerExtension server = new EmbeddedOpcUaServerExtension(oneContinuationPoint());

        /// The server pages every Browse result one reference at a time and holds one continuation
        /// point, so a batch of the default size gets `Bad_NoContinuationPoints` for the paged
        /// nodes past its pool. Milo reads such a node as having no references; the tree must still
        /// match Milo's own, built one node per request.
        @Test
        void buildsTheSameTree_whenTheBatchExhaustsTheContinuationPoints() throws Exception {
            server.advertiseOperationLimits(uint(0), uint(0));
            final Paging paging = pageEveryResult(server);
            final Set<NodeId> reference = buildMiloTree(server);
            paging.refused().set(0);
            final RecordingClient client = new RecordingClient(server);

            assertThat(buildTree(client)).isEqualTo(reference);
            assertThat(client.browses).contains(DEFAULT_OPERATIONS_PER_REQUEST);
            assertThat(paging.refused()).hasPositiveValue();
        }

        /// A build abandoned at its deadline while a Browse is in flight, as the connect-time
        /// ceiling and the rehydration deadline abandon it, must not leave the answer's
        /// continuation points on the session: the pool would stay full until the session closes,
        /// and no later build on that session could succeed.
        @Test
        void releasesTheContinuationPoints_ofABrowseAnsweredAfterTheBuildWasAbandoned() throws Exception {
            server.advertiseOperationLimits(uint(0), uint(0));
            final CountDownLatch abandoned = new CountDownLatch(1);
            final Paging paging = pageEveryResult(server, abandoned, false);
            final InterruptedClient client = new InterruptedClient(server);
            client.connect();
            try {
                assertThatThrownBy(client::getDataTypeTree).isInstanceOf(UaException.class);
                assertThat(Thread.interrupted())
                        .as("the interrupt the build was abandoned with")
                        .isTrue();
                abandoned.countDown();
                await().untilAsserted(
                                () -> assertThat(paging.handedOutToBatches()).hasPositiveValue());
                final Session session = onlySession(server);
                await().untilAsserted(() ->
                        assertThat(session.getBrowseContinuationPoints()).isEmpty());

                final Set<NodeId> types = new HashSet<>();
                client.getDataTypeTree().getRoot().traverse(type -> types.add(type.getNodeId()));
                assertThat(types).isEqualTo(buildMiloTree(server));
            } finally {
                client.disconnect();
            }
        }

        /// A batch answered with fewer results than nodes is rejected, and the continuation points
        /// of the results that did arrive must not stay on the session.
        @Test
        void releasesTheContinuationPoints_ofABatchAnsweredShort() throws Exception {
            server.advertiseOperationLimits(uint(0), uint(0));
            pageEveryResultAnsweringBatchesShort(server);
            final RecordingClient client = new RecordingClient(server);
            client.connect();
            try {
                assertThatThrownBy(client::getDataTypeTree).isInstanceOf(UaException.class);
                final Session session = onlySession(server);
                await().untilAsserted(() ->
                        assertThat(session.getBrowseContinuationPoints()).isEmpty());
            } finally {
                client.disconnect();
            }
        }

        /// A BrowseNext that fails (a service fault, a request timed out on a slow link) must not
        /// leave the continuation point it was sent with on the session.
        @Test
        void releasesTheContinuationPoint_ofABrowseNextThatFailed() throws Exception {
            server.advertiseOperationLimits(uint(0), uint(0));
            pageEveryResult(server, null, true);
            final RecordingClient client = new RecordingClient(server);
            client.connect();
            try {
                assertThatThrownBy(client::getDataTypeTree).isInstanceOf(UaException.class);
                final Session session = onlySession(server);
                await().untilAsserted(() ->
                        assertThat(session.getBrowseContinuationPoints()).isEmpty());
            } finally {
                client.disconnect();
            }
        }

        /// A node refused even on its own fails the build, instead of entering the tree without its
        /// subtypes.
        @Test
        void failsTheBuild_whenANodeIsRefusedEvenAlone() throws Exception {
            server.advertiseOperationLimits(uint(0), uint(0));
            refuseEveryPagedResult(server);

            assertThatThrownBy(() -> buildTree(new RecordingClient(server)))
                    .hasStackTraceContaining("refused even on its own");
        }
    }

    @Nested
    class ServerRefusingEachResultOfABigBatch {

        @RegisterExtension
        final @NotNull EmbeddedOpcUaServerExtension server = new EmbeddedOpcUaServerExtension();

        /// Some servers (the S7-1500 under overlapping requests) answer a batch they will not serve
        /// with `Bad_TooManyOperations` on every result instead of as a service fault. Milo reads
        /// such a result as a node without references; the tree must still match the one built
        /// while the server served every batch.
        @Test
        void buildsTheSameTree_whenTheServerRefusesEachResultOfABatch() throws Exception {
            final Set<NodeId> reference = buildTree(new RecordingClient(server));
            server.advertiseOperationLimits(uint(0), uint(0));
            refuseEachResultOfBatchesAbove(server, 10);
            final RecordingClient client = new RecordingClient(server);

            assertThat(buildTree(client)).isEqualTo(reference);
            assertThat(client.browses).contains(DEFAULT_OPERATIONS_PER_REQUEST);
        }
    }

    @Nested
    class ServerEndingWithAnEmptyContinuationPoint {

        @RegisterExtension
        final @NotNull EmbeddedOpcUaServerExtension server = new EmbeddedOpcUaServerExtension();

        /// OPC UA does not tell an empty continuation point from a null one: both say the result is
        /// complete. A server may end with an empty one on the Browse itself; no BrowseNext may
        /// follow it.
        @Test
        void buildsTheSameTree_whenTheBrowseEndsWithAnEmptyContinuationPoint() throws Exception {
            final Set<NodeId> reference = buildTree(new RecordingClient(server));
            server.advertiseOperationLimits(uint(0), uint(0));
            final AtomicInteger browseNextsOfEmpty = endWithAnEmptyContinuationPoint(server, false);

            assertThat(buildTree(new RecordingClient(server))).isEqualTo(reference);
            assertThat(browseNextsOfEmpty).hasValue(0);
        }

        /// As above, on the last BrowseNext page of a paged result.
        @Test
        void buildsTheSameTree_whenTheLastPageEndsWithAnEmptyContinuationPoint() throws Exception {
            final Set<NodeId> reference = buildTree(new RecordingClient(server));
            server.advertiseOperationLimits(uint(0), uint(0));
            final AtomicInteger browseNextsOfEmpty = endWithAnEmptyContinuationPoint(server, true);

            assertThat(buildTree(new RecordingClient(server))).isEqualTo(reference);
            assertThat(browseNextsOfEmpty).hasValue(0);
        }
    }

    /// The statuses a server may return per operation for an operation it did not serve in that request.
    private static final long[] REFUSALS = {
        StatusCodes.Bad_NoContinuationPoints,
        StatusCodes.Bad_ContinuationPointInvalid,
        StatusCodes.Bad_TooManyOperations,
        StatusCodes.Bad_ResponseTooLarge,
        StatusCodes.Bad_EncodingLimitsExceeded,
        StatusCodes.Bad_ResourceUnavailable,
        StatusCodes.Bad_OutOfMemory,
        StatusCodes.Bad_Timeout,
        StatusCodes.Bad_ServerTooBusy
    };

    static long @NotNull [] refusals() {
        return REFUSALS;
    }

    @Nested
    class ServerRefusingEachResultForItsSize {

        @RegisterExtension
        final @NotNull EmbeddedOpcUaServerExtension server = new EmbeddedOpcUaServerExtension();

        /// The common service results may also be returned per operation (OPC UA Part 4, 7.38.2): a
        /// server may answer each result of a batch it will not serve with one of them. Milo reads
        /// such a result as a node without subtypes; the tree must still match Milo's own, built one
        /// node per request.
        @ParameterizedTest
        @MethodSource("com.hivemq.edge.adapters.opcua.client.EdgeOpcUaClientIT#refusals")
        void buildsTheSameTree_whenTheServerRefusesEachSubtypeResultOfABatch(final long status) throws Exception {
            server.advertiseOperationLimits(uint(0), uint(0));
            refuseEachSubtypeResultOfBatchesAbove(server, 10, status);
            final Set<NodeId> reference = buildMiloTree(server);

            assertThat(reference).contains(NodeIds.Int32);
            assertThat(buildTree(new RecordingClient(server))).isEqualTo(reference);
        }

        /// As above, on the Read of the types' attributes: Milo reads a refused `IsAbstract` as
        /// concrete and a refused `DataTypeDefinition` as none.
        @ParameterizedTest
        @MethodSource("com.hivemq.edge.adapters.opcua.client.EdgeOpcUaClientIT#refusals")
        void readsTheSameAttributes_whenTheServerRefusesEachReadResultOfABatch(final long status) throws Exception {
            server.advertiseOperationLimits(uint(0), uint(0));
            refuseEachTypeAttributeReadOfBatchesAbove(server, 10, status);
            final Set<NodeId> reference = abstractTypes(new OpcUaClient(
                    RecordingClient.config(server),
                    new OpcTcpClientTransport(
                            OpcTcpClientTransportConfig.newBuilder().build())));
            final RecordingClient client = new RecordingClient(server);

            assertThat(reference).contains(NodeIds.Number, NodeIds.Structure, NodeIds.Enumeration);
            assertThat(abstractTypes(client)).isEqualTo(reference);
            assertThat(client.reads).anyMatch(size -> size > 10);
        }

        /// A Read refused for the request even on its own fails the build, instead of entering the
        /// tree with the wrong attributes.
        @Test
        void failsTheBuild_whenAReadIsRefusedEvenAlone() throws Exception {
            server.advertiseOperationLimits(uint(0), uint(0));
            refuseEachTypeAttributeReadOfBatchesAbove(server, 0, StatusCodes.Bad_TooManyOperations);

            assertThatThrownBy(() -> buildTree(new RecordingClient(server)))
                    .hasStackTraceContaining("refused even on its own");
        }

        /// A node whose Browse is refused for want of resources even on its own may be the node's
        /// own answer (a backend that is down): it goes back to Milo as Milo's own client gets it,
        /// and the rest of the tree is built, instead of failing every build.
        @Test
        void buildsTheSameTree_whenANodeIsRefusedForResourcesEvenAlone() throws Exception {
            server.advertiseOperationLimits(uint(0), uint(0));
            refuseTheSubtypesOf(server, NodeIds.Number, StatusCodes.Bad_ResourceUnavailable);
            final Set<NodeId> reference = buildMiloTree(server);

            assertThat(reference).contains(NodeIds.Number, NodeIds.String).doesNotContain(NodeIds.Int32);
            assertThat(buildTree(new RecordingClient(server))).isEqualTo(reference);
        }

        /// As above, on the Read of the types' attributes: an attribute too large to encode on its
        /// own.
        @Test
        void readsTheSameAttributes_whenAReadIsRefusedForItsSizeEvenAlone() throws Exception {
            server.advertiseOperationLimits(uint(0), uint(0));
            refuseTheTypeAttributesOf(server, NodeIds.Structure, StatusCodes.Bad_ResponseTooLarge);
            final Set<NodeId> reference = abstractTypes(new OpcUaClient(
                    RecordingClient.config(server),
                    new OpcTcpClientTransport(
                            OpcTcpClientTransportConfig.newBuilder().build())));

            assertThat(reference).contains(NodeIds.Number, NodeIds.Enumeration).doesNotContain(NodeIds.Structure);
            assertThat(abstractTypes(new RecordingClient(server))).isEqualTo(reference);
        }
    }

    @Nested
    class ServerRefusingEveryBrowseNext {

        @RegisterExtension
        final @NotNull EmbeddedOpcUaServerExtension server = new EmbeddedOpcUaServerExtension();

        /// The server pages every result and then refuses every BrowseNext for want of resources.
        /// The node's own answer was a page of references, so the refusal is about the
        /// continuation, not the node: the build fails instead of reading the node as having no
        /// subtypes, fewer than Milo's own client keeps.
        @Test
        void failsTheBuild_whenEveryBrowseNextIsRefusedEvenAlone() throws Exception {
            server.advertiseOperationLimits(uint(0), uint(0));
            refuseEveryBrowseNext(server, StatusCodes.Bad_ResourceUnavailable);

            assertThat(buildMiloTree(server)).hasSizeGreaterThan(1);
            assertThatThrownBy(() -> buildTree(new RecordingClient(server)))
                    .hasStackTraceContaining("refused even on its own");
        }
    }

    @Nested
    class ServerRefusingLargeRequests {

        @RegisterExtension
        final @NotNull EmbeddedOpcUaServerExtension server = new EmbeddedOpcUaServerExtension();

        /// A server may refuse a whole Read whose response would not fit its message size with
        /// `Bad_ResponseTooLarge` instead of `Bad_TooManyOperations`; it must still be replayed one
        /// operation at a time.
        @Test
        void buildsTheSameTree_whenTheServerRefusesLargeReads() throws Exception {
            server.advertiseOperationLimits(uint(0), uint(0));
            refuseServicesAbove(server, 20, true);
            final Set<NodeId> reference = buildMiloTree(server);
            final RecordingClient client = new RecordingClient(server);

            assertThat(buildTree(client)).isEqualTo(reference);
            assertThat(client.reads).contains(DEFAULT_OPERATIONS_PER_REQUEST, 1);
        }

        /// As above, on a Browse.
        @Test
        void buildsTheSameTree_whenTheServerRefusesLargeBrowses() throws Exception {
            server.advertiseOperationLimits(uint(0), uint(0));
            refuseServicesAbove(server, 20, false);
            final Set<NodeId> reference = buildMiloTree(server);
            final RecordingClient client = new RecordingClient(server);

            assertThat(buildTree(client)).isEqualTo(reference);
            assertThat(client.browses).contains(DEFAULT_OPERATIONS_PER_REQUEST, 1);
        }
    }

    @Nested
    class ServerFreeingContinuationPoints {

        @RegisterExtension
        final @NotNull EmbeddedOpcUaServerExtension server = new EmbeddedOpcUaServerExtension();

        /// A server frees a continuation point of the session when it needs it for a later request
        /// (OPC UA Part 4, 7.9), and answers the BrowseNext that uses it with
        /// `Bad_ContinuationPointInvalid`. Milo's own client holds one point at a time; a batch
        /// holds several, and the node whose point was freed is browsed again alone.
        @Test
        void buildsTheSameTree_whenTheServerFreesAnotherContinuationPointForABrowseNext() throws Exception {
            server.advertiseOperationLimits(uint(0), uint(0));
            final AtomicInteger freed = freeAnotherContinuationPointOnBrowseNext(server);
            final Set<NodeId> reference = buildMiloTree(server);

            assertThat(freed).as("Milo's own client holds one point at a time").hasValue(0);
            assertThat(buildTree(new RecordingClient(server))).isEqualTo(reference);
            assertThat(freed).hasPositiveValue();
        }
    }

    /// Records only the tree build: the connect-time reads and the limits read happen before
    /// recording starts.
    private static @NotNull Set<NodeId> buildTree(final @NotNull RecordingClient client) throws Exception {
        client.connect();
        try {
            client.getOperationLimits();
            client.browses.clear();
            client.reads.clear();
            final Set<NodeId> types = new HashSet<>();
            client.getDataTypeTree().getRoot().traverse(type -> types.add(type.getNodeId()));
            return types;
        } finally {
            client.disconnect();
        }
    }

    /// The tree Milo's own client builds, which sends one node per Browse when the server
    /// advertises 0.
    private static @NotNull Set<NodeId> buildMiloTree(final @NotNull EmbeddedOpcUaServerExtension server)
            throws Exception {
        final OpcUaClient client = new OpcUaClient(
                RecordingClient.config(server),
                new OpcTcpClientTransport(
                        OpcTcpClientTransportConfig.newBuilder().build()));
        client.connect();
        try {
            final Set<NodeId> types = new HashSet<>();
            client.getDataTypeTree().getRoot().traverse(type -> types.add(type.getNodeId()));
            return types;
        } finally {
            client.disconnect();
        }
    }

    /// What a paging server did: results refused with `Bad_NoContinuationPoints`, and results of
    /// multi-node Browses handed a continuation point.
    private record Paging(
            @NotNull AtomicInteger refused, @NotNull AtomicInteger handedOutToBatches) {}

    /// Makes the server answer every Browse with at most one reference per node, paging the rest.
    private static @NotNull Paging pageEveryResult(final @NotNull EmbeddedOpcUaServerExtension server) {
        return pageEveryResult(server, null, false);
    }

    /// As [#pageEveryResult(EmbeddedOpcUaServerExtension)]. With `holdSubtypeBatches`, the answer
    /// to a multi-node subtype Browse waits for the latch; with `failBrowseNext`, every BrowseNext
    /// that pages faults without consuming its continuation point.
    private static @NotNull Paging pageEveryResult(
            final @NotNull EmbeddedOpcUaServerExtension server,
            final @Nullable CountDownLatch holdSubtypeBatches,
            final boolean failBrowseNext) {
        final Paging paging = new Paging(new AtomicInteger(), new AtomicInteger());
        final OpcUaServer opcUaServer = Objects.requireNonNull(server.getOpcUaServer());
        opcUaServer.addServiceSet("/opcua/test", new DefaultViewServiceSet(opcUaServer) {
            @Override
            public @NotNull BrowseResponse onBrowse(
                    final @NotNull ServiceRequestContext context, final @NotNull BrowseRequest request)
                    throws UaException {
                final BrowseResponse response = super.onBrowse(
                        context,
                        new BrowseRequest(
                                request.getRequestHeader(), request.getView(), uint(1), request.getNodesToBrowse()));
                for (final BrowseResult result : Objects.requireNonNull(response.getResults())) {
                    if (result.getStatusCode().value() == StatusCodes.Bad_NoContinuationPoints) {
                        paging.refused().incrementAndGet();
                    }
                    if (request.getNodesToBrowse().length > 1
                            && result.getContinuationPoint() != null
                            && result.getContinuationPoint().isNotNull()) {
                        paging.handedOutToBatches().incrementAndGet();
                    }
                }
                if (holdSubtypeBatches != null
                        && request.getNodesToBrowse().length > 1
                        && NodeIds.HasSubtype.equals(request.getNodesToBrowse()[0].getReferenceTypeId())) {
                    try {
                        if (!holdSubtypeBatches.await(30, TimeUnit.SECONDS)) {
                            throw new UaException(StatusCodes.Bad_Timeout, "the test never released the answer");
                        }
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new UaException(StatusCodes.Bad_UnexpectedError, e);
                    }
                }
                return response;
            }

            @Override
            public @NotNull BrowseNextResponse onBrowseNext(
                    final @NotNull ServiceRequestContext context, final @NotNull BrowseNextRequest request)
                    throws UaException {
                if (failBrowseNext && !Boolean.TRUE.equals(request.getReleaseContinuationPoints())) {
                    throw new UaException(StatusCodes.Bad_InternalError, "BrowseNext failed on purpose");
                }
                return super.onBrowseNext(context, request);
            }
        });
        return paging;
    }

    /// Pages every Browse result one reference at a time, and answers a multi-node subtype Browse,
    /// the kind a paging server hands continuation points, with its last result missing, as a
    /// non-compliant server may.
    private static void pageEveryResultAnsweringBatchesShort(final @NotNull EmbeddedOpcUaServerExtension server) {
        final OpcUaServer opcUaServer = Objects.requireNonNull(server.getOpcUaServer());
        opcUaServer.addServiceSet("/opcua/test", new DefaultViewServiceSet(opcUaServer) {
            @Override
            public @NotNull BrowseResponse onBrowse(
                    final @NotNull ServiceRequestContext context, final @NotNull BrowseRequest request)
                    throws UaException {
                final BrowseResponse response = super.onBrowse(
                        context,
                        new BrowseRequest(
                                request.getRequestHeader(), request.getView(), uint(1), request.getNodesToBrowse()));
                final BrowseResult[] results = Objects.requireNonNull(response.getResults());
                if (results.length <= 1
                        || !NodeIds.HasSubtype.equals(request.getNodesToBrowse()[0].getReferenceTypeId())) {
                    return response;
                }
                return new BrowseResponse(
                        response.getResponseHeader(),
                        Arrays.copyOf(results, results.length - 1),
                        response.getDiagnosticInfos());
            }
        });
    }

    private static @NotNull Session onlySession(final @NotNull EmbeddedOpcUaServerExtension server) {
        return Objects.requireNonNull(server.getOpcUaServer())
                .getSessionManager()
                .getAllSessions()
                .get(0);
    }

    /// Answers every result of a Browse with more than `limit` nodes with `Bad_TooManyOperations`
    /// and no references, instead of refusing the request.
    private static void refuseEachResultOfBatchesAbove(
            final @NotNull EmbeddedOpcUaServerExtension server, final int limit) {
        final OpcUaServer opcUaServer = Objects.requireNonNull(server.getOpcUaServer());
        opcUaServer.addServiceSet("/opcua/test", new DefaultViewServiceSet(opcUaServer) {
            @Override
            public @NotNull BrowseResponse onBrowse(
                    final @NotNull ServiceRequestContext context, final @NotNull BrowseRequest request)
                    throws UaException {
                final BrowseResponse response = super.onBrowse(context, request);
                final BrowseResult[] results = Objects.requireNonNull(response.getResults());
                if (results.length <= limit) {
                    return response;
                }
                final BrowseResult[] refused = new BrowseResult[results.length];
                Arrays.fill(
                        refused,
                        new BrowseResult(
                                new StatusCode(StatusCodes.Bad_TooManyOperations),
                                ByteString.NULL_VALUE,
                                new ReferenceDescription[0]));
                return new BrowseResponse(response.getResponseHeader(), refused, response.getDiagnosticInfos());
            }
        });
    }

    /// Makes the server refuse every Browse result it would page, as a server whose pool another
    /// client holds.
    private static void refuseEveryPagedResult(final @NotNull EmbeddedOpcUaServerExtension server) {
        final OpcUaServer opcUaServer = Objects.requireNonNull(server.getOpcUaServer());
        opcUaServer.addServiceSet("/opcua/test", new DefaultViewServiceSet(opcUaServer) {
            @Override
            public @NotNull BrowseResponse onBrowse(
                    final @NotNull ServiceRequestContext context, final @NotNull BrowseRequest request)
                    throws UaException {
                final BrowseResponse response = super.onBrowse(
                        context,
                        new BrowseRequest(
                                request.getRequestHeader(), request.getView(), uint(1), request.getNodesToBrowse()));
                final BrowseResult[] results = Objects.requireNonNull(response.getResults());
                for (int i = 0; i < results.length; i++) {
                    if (results[i].getContinuationPoint() != null
                            && results[i].getContinuationPoint().isNotNull()) {
                        results[i] = new BrowseResult(
                                new StatusCode(StatusCodes.Bad_NoContinuationPoints), ByteString.NULL_VALUE, null);
                    }
                }
                return new BrowseResponse(response.getResponseHeader(), results, response.getDiagnosticInfos());
            }
        });
    }

    /// Makes the server end every complete Browse result with an empty continuation point instead
    /// of a null one: on the Browse itself, or with `paged`, on the last BrowseNext page of results
    /// paged one reference at a time. Counts the BrowseNext requests that carry an empty
    /// continuation point.
    private static @NotNull AtomicInteger endWithAnEmptyContinuationPoint(
            final @NotNull EmbeddedOpcUaServerExtension server, final boolean paged) {
        final AtomicInteger browseNextsOfEmpty = new AtomicInteger();
        final OpcUaServer opcUaServer = Objects.requireNonNull(server.getOpcUaServer());
        opcUaServer.addServiceSet("/opcua/test", new DefaultViewServiceSet(opcUaServer) {
            @Override
            public @NotNull BrowseResponse onBrowse(
                    final @NotNull ServiceRequestContext context, final @NotNull BrowseRequest request)
                    throws UaException {
                final BrowseResponse response = super.onBrowse(
                        context,
                        paged
                                ? new BrowseRequest(
                                        request.getRequestHeader(),
                                        request.getView(),
                                        uint(1),
                                        request.getNodesToBrowse())
                                : request);
                if (!paged) {
                    emptyTheEndMarkers(Objects.requireNonNull(response.getResults()));
                }
                return response;
            }

            @Override
            public @NotNull BrowseNextResponse onBrowseNext(
                    final @NotNull ServiceRequestContext context, final @NotNull BrowseNextRequest request)
                    throws UaException {
                for (final ByteString continuationPoint : Objects.requireNonNull(request.getContinuationPoints())) {
                    if (continuationPoint.isNullOrEmpty()) {
                        browseNextsOfEmpty.incrementAndGet();
                    }
                }
                final BrowseNextResponse response = super.onBrowseNext(context, request);
                if (paged && response.getResults() != null) {
                    emptyTheEndMarkers(response.getResults());
                }
                return response;
            }
        });
        return browseNextsOfEmpty;
    }

    private static void emptyTheEndMarkers(final @NotNull BrowseResult @NotNull [] results) {
        for (int i = 0; i < results.length; i++) {
            final BrowseResult result = results[i];
            if (result.getStatusCode().isGood()
                    && (result.getContinuationPoint() == null
                            || result.getContinuationPoint().isNull())) {
                results[i] =
                        new BrowseResult(result.getStatusCode(), ByteString.of(new byte[0]), result.getReferences());
            }
        }
    }

    /// The abstract types of the data-type tree `client` builds.
    private static @NotNull Set<NodeId> abstractTypes(final @NotNull OpcUaClient client) throws Exception {
        client.connect();
        try {
            final Set<NodeId> types = new HashSet<>();
            client.getDataTypeTree().getRoot().traverse(type -> {
                if (Boolean.TRUE.equals(type.isAbstract())) {
                    types.add(type.getNodeId());
                }
            });
            return types;
        } finally {
            client.disconnect();
        }
    }

    /// Answers every result of a subtype Browse with more than `limit` nodes with `status` and no
    /// references, instead of refusing the request.
    private static void refuseEachSubtypeResultOfBatchesAbove(
            final @NotNull EmbeddedOpcUaServerExtension server, final int limit, final long status) {
        final OpcUaServer opcUaServer = Objects.requireNonNull(server.getOpcUaServer());
        opcUaServer.addServiceSet("/opcua/test", new DefaultViewServiceSet(opcUaServer) {
            @Override
            public @NotNull BrowseResponse onBrowse(
                    final @NotNull ServiceRequestContext context, final @NotNull BrowseRequest request)
                    throws UaException {
                final BrowseResponse response = super.onBrowse(context, request);
                final BrowseResult[] results = Objects.requireNonNull(response.getResults());
                if (results.length <= limit
                        || !NodeIds.HasSubtype.equals(request.getNodesToBrowse()[0].getReferenceTypeId())) {
                    return response;
                }
                final BrowseResult[] refused = new BrowseResult[results.length];
                Arrays.fill(refused, new BrowseResult(new StatusCode(status), ByteString.NULL_VALUE, null));
                return new BrowseResponse(response.getResponseHeader(), refused, response.getDiagnosticInfos());
            }
        });
    }

    /// Answers every result of a Read of the types' attributes with more than `limit` operations
    /// with `status`, instead of refusing the request.
    private static void refuseEachTypeAttributeReadOfBatchesAbove(
            final @NotNull EmbeddedOpcUaServerExtension server, final int limit, final long status) {
        final OpcUaServer opcUaServer = Objects.requireNonNull(server.getOpcUaServer());
        opcUaServer.addServiceSet("/opcua/test", new DefaultAttributeServiceSet(opcUaServer) {
            @Override
            public @NotNull ReadResponse onRead(
                    final @NotNull ServiceRequestContext context, final @NotNull ReadRequest request)
                    throws UaException {
                final ReadResponse response = super.onRead(context, request);
                final ReadValueId[] nodes = Objects.requireNonNull(request.getNodesToRead());
                final boolean typeAttributes = Arrays.stream(nodes)
                        .anyMatch(node -> AttributeId.DataTypeDefinition.uid().equals(node.getAttributeId()));
                if (nodes.length <= limit || !typeAttributes) {
                    return response;
                }
                final DataValue[] refused = new DataValue[nodes.length];
                Arrays.fill(refused, new DataValue(new StatusCode(status)));
                return new ReadResponse(response.getResponseHeader(), refused, response.getDiagnosticInfos());
            }
        });
    }

    /// Refuses every Read (with `reads`) or Browse with more than `limit` operations with a
    /// `Bad_ResponseTooLarge` service fault. Above the operations Milo reads the limits with, so
    /// only the tree build's batches are refused.
    private static void refuseServicesAbove(
            final @NotNull EmbeddedOpcUaServerExtension server, final int limit, final boolean reads) {
        final OpcUaServer opcUaServer = Objects.requireNonNull(server.getOpcUaServer());
        if (reads) {
            opcUaServer.addServiceSet("/opcua/test", new DefaultAttributeServiceSet(opcUaServer) {
                @Override
                public @NotNull ReadResponse onRead(
                        final @NotNull ServiceRequestContext context, final @NotNull ReadRequest request)
                        throws UaException {
                    if (request.getNodesToRead() != null && request.getNodesToRead().length > limit) {
                        throw new UaException(StatusCodes.Bad_ResponseTooLarge, "response too large on purpose");
                    }
                    return super.onRead(context, request);
                }
            });
        } else {
            opcUaServer.addServiceSet("/opcua/test", new DefaultViewServiceSet(opcUaServer) {
                @Override
                public @NotNull BrowseResponse onBrowse(
                        final @NotNull ServiceRequestContext context, final @NotNull BrowseRequest request)
                        throws UaException {
                    if (request.getNodesToBrowse() != null && request.getNodesToBrowse().length > limit) {
                        throw new UaException(StatusCodes.Bad_ResponseTooLarge, "response too large on purpose");
                    }
                    return super.onBrowse(context, request);
                }
            });
        }
    }

    /// Pages every Browse result one reference at a time; each BrowseNext first frees another
    /// continuation point of the session, as a server short of points may.
    private static @NotNull AtomicInteger freeAnotherContinuationPointOnBrowseNext(
            final @NotNull EmbeddedOpcUaServerExtension server) {
        final AtomicInteger freed = new AtomicInteger();
        final OpcUaServer opcUaServer = Objects.requireNonNull(server.getOpcUaServer());
        opcUaServer.addServiceSet("/opcua/test", new DefaultViewServiceSet(opcUaServer) {
            @Override
            public @NotNull BrowseResponse onBrowse(
                    final @NotNull ServiceRequestContext context, final @NotNull BrowseRequest request)
                    throws UaException {
                return super.onBrowse(
                        context,
                        new BrowseRequest(
                                request.getRequestHeader(), request.getView(), uint(1), request.getNodesToBrowse()));
            }

            @Override
            public @NotNull BrowseNextResponse onBrowseNext(
                    final @NotNull ServiceRequestContext context, final @NotNull BrowseNextRequest request)
                    throws UaException {
                if (!Boolean.TRUE.equals(request.getReleaseContinuationPoints())) {
                    final List<ByteString> asked =
                            Arrays.asList(Objects.requireNonNull(request.getContinuationPoints()));
                    final Map<ByteString, ?> points = onlySession(server).getBrowseContinuationPoints();
                    points.keySet().stream()
                            .filter(point -> !asked.contains(point))
                            .findFirst()
                            .ifPresent(point -> {
                                points.remove(point);
                                freed.incrementAndGet();
                            });
                }
                return super.onBrowseNext(context, request);
            }
        });
        return freed;
    }

    /// Answers every subtype Browse result of `node` with `status` and no references, whatever
    /// the size of the request.
    private static void refuseTheSubtypesOf(
            final @NotNull EmbeddedOpcUaServerExtension server, final @NotNull NodeId node, final long status) {
        final OpcUaServer opcUaServer = Objects.requireNonNull(server.getOpcUaServer());
        opcUaServer.addServiceSet("/opcua/test", new DefaultViewServiceSet(opcUaServer) {
            @Override
            public @NotNull BrowseResponse onBrowse(
                    final @NotNull ServiceRequestContext context, final @NotNull BrowseRequest request)
                    throws UaException {
                final BrowseResponse response = super.onBrowse(context, request);
                final BrowseDescription[] nodes = Objects.requireNonNull(request.getNodesToBrowse());
                final BrowseResult[] results = Objects.requireNonNull(response.getResults());
                for (int i = 0; i < nodes.length; i++) {
                    if (node.equals(nodes[i].getNodeId()) && NodeIds.HasSubtype.equals(nodes[i].getReferenceTypeId())) {
                        results[i] = new BrowseResult(new StatusCode(status), ByteString.NULL_VALUE, null);
                    }
                }
                return response;
            }
        });
    }

    /// Answers every Read of `node`'s `IsAbstract` and `DataTypeDefinition` with `status`, whatever
    /// the size of the request.
    private static void refuseTheTypeAttributesOf(
            final @NotNull EmbeddedOpcUaServerExtension server, final @NotNull NodeId node, final long status) {
        final OpcUaServer opcUaServer = Objects.requireNonNull(server.getOpcUaServer());
        opcUaServer.addServiceSet("/opcua/test", new DefaultAttributeServiceSet(opcUaServer) {
            @Override
            public @NotNull ReadResponse onRead(
                    final @NotNull ServiceRequestContext context, final @NotNull ReadRequest request)
                    throws UaException {
                final ReadResponse response = super.onRead(context, request);
                final ReadValueId[] nodes = Objects.requireNonNull(request.getNodesToRead());
                final DataValue[] values = Objects.requireNonNull(response.getResults());
                for (int i = 0; i < nodes.length; i++) {
                    if (node.equals(nodes[i].getNodeId())
                            && (AttributeId.IsAbstract.uid().equals(nodes[i].getAttributeId())
                                    || AttributeId.DataTypeDefinition.uid().equals(nodes[i].getAttributeId()))) {
                        values[i] = new DataValue(new StatusCode(status));
                    }
                }
                return response;
            }
        });
    }

    /// Pages every Browse result one reference at a time and answers every BrowseNext that
    /// continues one with `status` and no references; releases are served.
    private static void refuseEveryBrowseNext(final @NotNull EmbeddedOpcUaServerExtension server, final long status) {
        final OpcUaServer opcUaServer = Objects.requireNonNull(server.getOpcUaServer());
        opcUaServer.addServiceSet("/opcua/test", new DefaultViewServiceSet(opcUaServer) {
            @Override
            public @NotNull BrowseResponse onBrowse(
                    final @NotNull ServiceRequestContext context, final @NotNull BrowseRequest request)
                    throws UaException {
                return super.onBrowse(
                        context,
                        new BrowseRequest(
                                request.getRequestHeader(), request.getView(), uint(1), request.getNodesToBrowse()));
            }

            @Override
            public @NotNull BrowseNextResponse onBrowseNext(
                    final @NotNull ServiceRequestContext context, final @NotNull BrowseNextRequest request)
                    throws UaException {
                final BrowseNextResponse response = super.onBrowseNext(context, request);
                if (Boolean.TRUE.equals(request.getReleaseContinuationPoints())) {
                    return response;
                }
                final BrowseResult[] refused =
                        new BrowseResult[Objects.requireNonNull(request.getContinuationPoints()).length];
                Arrays.fill(refused, new BrowseResult(new StatusCode(status), ByteString.NULL_VALUE, null));
                return new BrowseNextResponse(response.getResponseHeader(), refused, response.getDiagnosticInfos());
            }
        });
    }

    private static @NotNull OpcUaServerConfigLimits oneContinuationPoint() {
        return new OpcUaServerConfigLimits() {
            @Override
            public @NotNull UShort getMaxBrowseContinuationPoints() {
                return ushort(1);
            }
        };
    }

    private static @NotNull OpcUaServerConfigLimits maxNodesPerRequest(final int limit) {
        return new OpcUaServerConfigLimits() {
            @Override
            public @NotNull UInteger getMaxNodesPerRead() {
                return uint(limit);
            }

            @Override
            public @NotNull UInteger getMaxNodesPerBrowse() {
                return uint(limit);
            }
        };
    }

    private static final class RecordingClient extends EdgeOpcUaClient {

        private final @NotNull List<Integer> browses = new CopyOnWriteArrayList<>();
        private final @NotNull List<Integer> reads = new CopyOnWriteArrayList<>();

        private RecordingClient(final @NotNull EmbeddedOpcUaServerExtension server) throws Exception {
            super(
                    config(server),
                    new OpcTcpClientTransport(
                            OpcTcpClientTransportConfig.newBuilder().build()));
        }

        private static @NotNull OpcUaClientConfig config(final @NotNull EmbeddedOpcUaServerExtension server)
                throws Exception {
            final List<EndpointDescription> endpoints =
                    DiscoveryClient.getEndpoints(server.getServerUri()).get();
            final EndpointDescription endpoint = endpoints.stream()
                    .filter(e -> SecurityPolicy.None.getUri().equals(e.getSecurityPolicyUri()))
                    .findFirst()
                    .orElseThrow();
            return OpcUaClientConfig.builder().setEndpoint(endpoint).build();
        }

        @Override
        public @NotNull List<BrowseResult> browse(final @NotNull List<BrowseDescription> nodesToBrowse)
                throws UaException {
            browses.add(nodesToBrowse.size());
            return super.browse(nodesToBrowse);
        }

        @Override
        public @NotNull ReadResponse read(
                final double maxAge,
                final @NotNull TimestampsToReturn timestampsToReturn,
                final @NotNull List<ReadValueId> readValueIds)
                throws UaException {
            reads.add(readValueIds.size());
            return super.read(maxAge, timestampsToReturn, readValueIds);
        }
    }

    /// Interrupts its own thread right before the first multi-node subtype Browse, the one a paging
    /// server answers with continuation points, as the preparation deadline interrupts a build.
    private static final class InterruptedClient extends EdgeOpcUaClient {

        private final @NotNull AtomicBoolean interrupted = new AtomicBoolean();

        private InterruptedClient(final @NotNull EmbeddedOpcUaServerExtension server) throws Exception {
            super(
                    RecordingClient.config(server),
                    new OpcTcpClientTransport(
                            OpcTcpClientTransportConfig.newBuilder().build()));
        }

        @Override
        public @NotNull List<BrowseResult> browse(final @NotNull List<BrowseDescription> nodesToBrowse)
                throws UaException {
            if (nodesToBrowse.size() > 1
                    && NodeIds.HasSubtype.equals(nodesToBrowse.get(0).getReferenceTypeId())
                    && interrupted.compareAndSet(false, true)) {
                Thread.currentThread().interrupt();
            }
            return super.browse(nodesToBrowse);
        }
    }
}
