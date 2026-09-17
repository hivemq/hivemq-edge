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
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.UaServiceFaultException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseNextResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ResponseHeader;
import org.eclipse.milo.opcua.stack.core.types.structured.ServiceFault;
import org.eclipse.milo.opcua.stack.core.types.structured.ViewDescription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Test doubles for the OPC UA server as the browser sees it through {@link OpcUaClient}: a Browse/BrowseNext
 * service over a fixed address space ({@link FakeBrowseServer}) and a Read service ({@link FakeReadServer}),
 * both recording every request, plus fixtures for the address-space shapes the browse tests share.
 */
final class FakeOpcUaServer {

    private FakeOpcUaServer() {}

    static @NotNull ReferenceDescription variable(final @NotNull String nodeId, final @NotNull String name) {
        return new ReferenceDescription(
                NodeIds.HasComponent,
                true,
                ExpandedNodeId.parse(nodeId),
                new QualifiedName(2, name),
                LocalizedText.english(name),
                NodeClass.Variable,
                ExpandedNodeId.NULL_VALUE);
    }

    static @NotNull ReferenceDescription object(final @NotNull String nodeId, final @NotNull String name) {
        return new ReferenceDescription(
                NodeIds.Organizes,
                true,
                ExpandedNodeId.parse(nodeId),
                new QualifiedName(2, name),
                LocalizedText.english(name),
                NodeClass.Object,
                ExpandedNodeId.NULL_VALUE);
    }

    static @NotNull FakeBrowseServer folders(final int count) {
        final FakeBrowseServer server = new FakeBrowseServer();
        final ReferenceDescription[] folders = new ReferenceDescription[count];
        for (int i = 0; i < count; i++) {
            final String id = "ns=2;s=F" + String.format("%03d", i);
            folders[i] = object(id, "F" + String.format("%03d", i));
            server.children(id, variable(id + ".V", "V"));
        }
        return server.children(NodeIds.ObjectsFolder, folders);
    }

    /** {@code folders} folders under Objects, each with {@code width} variables. */
    static @NotNull FakeBrowseServer wideFolders(final int folders, final int width) {
        final FakeBrowseServer server = new FakeBrowseServer();
        final ReferenceDescription[] refs = new ReferenceDescription[folders];
        for (int i = 0; i < folders; i++) {
            final String id = "ns=2;s=F" + String.format("%03d", i);
            refs[i] = object(id, "F" + String.format("%03d", i));
            final ReferenceDescription[] vars = new ReferenceDescription[width];
            for (int v = 0; v < width; v++) {
                vars[v] = variable(id + ".V" + v, "V" + v);
            }
            server.children(id, vars);
        }
        return server.children(NodeIds.ObjectsFolder, refs);
    }

    static @NotNull OpcUaClient client(final @NotNull FakeBrowseServer browse, final @NotNull FakeReadServer read) {
        final OpcUaClient client = mock(OpcUaClient.class);
        final NamespaceTable nsTable = new NamespaceTable();
        nsTable.add("urn:test");
        nsTable.add("urn:test:plc");
        when(client.getNamespaceTable()).thenReturn(nsTable);
        browse.install(client);
        read.install(client);
        return client;
    }

    /** A client with the browse fake only; reads are never issued by the Phase 1 classes. */
    static @NotNull OpcUaClient client(final @NotNull FakeBrowseServer browse) {
        return client(browse, new FakeReadServer(0, Integer.MAX_VALUE));
    }

    /** The folder nodes {@code F000..F(count-1)} as a Phase 1 chunk, each with one level of depth left. */
    static @NotNull List<PendingNode> folderChunk(final int count) {
        final List<PendingNode> chunk = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            final String name = "F" + String.format("%03d", i);
            chunk.add(new PendingNode(NodeId.parse("ns=2;s=" + name), "/" + name, 1));
        }
        return chunk;
    }

    /** {@code count} Variable references {@code Var000..} under one parent, for Phase 2 fixtures. */
    static @NotNull ReferenceDescription[] variables(final int count) {
        final ReferenceDescription[] refs = new ReferenceDescription[count];
        for (int i = 0; i < count; i++) {
            refs[i] = variable(nodeId(i), "Var" + String.format("%03d", i));
        }
        return refs;
    }

    /** {@code count} discovered variables {@code /Var000..}, the shape Phase 1 hands to Phase 2. */
    static @NotNull List<DiscoveredVariable> discovered(final int count) {
        final List<DiscoveredVariable> vars = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            final String name = "Var" + String.format("%03d", i);
            vars.add(new DiscoveredVariable(NodeId.parse(nodeId(i)), "/" + name, "urn:test:plc", 2, name));
        }
        return vars;
    }

    /** {@code ns=2;s=Var000} .. — zero-padded so path order equals index order. */
    static @NotNull String nodeId(final int i) {
        return "ns=2;s=Var" + String.format("%03d", i);
    }

    static @NotNull List<String> nodeIds(final int count) {
        return IntStream.range(0, count).mapToObj(FakeOpcUaServer::nodeId).toList();
    }

    /**
     * Stands in for the server's Browse and BrowseNext services over a fixed address space: answers each node
     * with its configured children (paged at {@code pageSize} references through continuation points), rejects
     * requests of more than {@code enforced} nodes with Bad_TooManyOperations, and records every call.
     */
    static final class FakeBrowseServer {
        private final Map<NodeId, ReferenceDescription[]> children = new HashMap<>();
        private final Map<NodeId, BrowseResult> fixedResults = new HashMap<>();
        private final Map<ByteString, List<ReferenceDescription>> continuations = new HashMap<>();
        private int enforced = Integer.MAX_VALUE;
        private int pageSize = Integer.MAX_VALUE;
        private int continuationCapacity = Integer.MAX_VALUE;
        /** Status of the service fault for a request above {@code enforced}; Bad_TooManyOperations by default. */
        long rejectStatus = StatusCodes.Bad_TooManyOperations;
        /**
         * The n-th call (Browse or BrowseNext, 1-based) is refused with {@code rejectStatus} whatever its size:
         * a request the server cannot answer because of what the response would carry, not how many nodes
         * were asked for.
         */
        int rejectCall;
        /** While positive, every paged result answers Bad_NoContinuationPoints (another client holds the pool). */
        int refuseContinuationsForRequests;
        /** BrowseNext with more points than this is refused as a whole with Bad_TooManyOperations (Milo does this). */
        int maxPointsPerBrowseNext = Integer.MAX_VALUE;

        final List<Integer> exhaustedPerRequest = new ArrayList<>();
        private int nextContinuation;

        @Nullable
        StatusCode nextStatus;

        /** When set, {@code nextStatus} applies only to pages whose first reference's node id starts with this. */
        @Nullable
        String nextStatusOwnerPrefix;

        /** When set, every Browse response omits its last result (a server violating one-result-per-description). */
        boolean dropLastResult;

        /**
         * The n-th call (Browse or BrowseNext, 1-based) is answered — cursors allocated, results built — but its
         * future stays open until {@link #completeDeferred()}: the shape of a response that lands after the
         * client stopped waiting for it.
         */
        int deferCall;

        /** When non-zero, the deferred call completes with this service fault instead of its response. */
        long deferredFault;

        /** When set, every BrowseNext response omits its last result. */
        boolean dropLastNextResult;

        /** From the n-th call (1-based) on, responses carry a null results array (a server violating the contract). */
        int nullResultsFromCall;

        /** When non-zero, every BrowseNext (not release) request fails with this service fault. */
        long nextFault;

        /** When set, results are built without a status code (a server leaving it out means Good). */
        boolean nullStatus;

        /** When set, results without children carry a null reference array instead of an empty one. */
        boolean nullReferencesForLeaves;

        /** When set, every continuation page comes back Good with a null reference array and no cursor. */
        boolean nullReferencesOnPages;

        /** When non-zero, every release request fails with this service fault; TooManyOperations halves, others log. */
        long releaseFault;

        private @Nullable Runnable deferredCompletion;

        void completeDeferred() {
            Objects.requireNonNull(deferredCompletion, "no deferred response").run();
        }

        /** Continuation points the server still holds. */
        int openContinuationPoints() {
            return continuations.size();
        }

        final List<Integer> browseSizes = new ArrayList<>();
        final List<String> calls = new ArrayList<>();
        final List<String> browsedNodes = new ArrayList<>();
        final List<Integer> requestedMaxReferences = new ArrayList<>();

        @NotNull
        FakeBrowseServer children(final @NotNull NodeId parent, final @NotNull ReferenceDescription... refs) {
            children.put(parent, refs);
            return this;
        }

        @NotNull
        FakeBrowseServer children(final @NotNull String parent, final @NotNull ReferenceDescription... refs) {
            return children(NodeId.parse(parent), refs);
        }

        @NotNull
        FakeBrowseServer status(final @NotNull NodeId node, final @NotNull BrowseResult result) {
            fixedResults.put(node, result);
            return this;
        }

        @NotNull
        FakeBrowseServer enforce(final int maxNodesPerBrowse) {
            this.enforced = maxNodesPerBrowse;
            return this;
        }

        @NotNull
        FakeBrowseServer pageSize(final int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /** Continuation points the server can hold at once; results beyond that get Bad_NoContinuationPoints. */
        FakeBrowseServer continuationCapacity(final int capacity) {
            this.continuationCapacity = capacity;
            return this;
        }

        void install(final @NotNull OpcUaClient client) {
            when(client.browseAsync(any(ViewDescription.class), any(UInteger.class), anyList()))
                    .thenAnswer(invocation -> {
                        final UInteger maxReferences = invocation.getArgument(1);
                        final List<BrowseDescription> descriptions = invocation.getArgument(2);
                        requestedMaxReferences.add(maxReferences.intValue());
                        browseSizes.add(descriptions.size());
                        calls.add("browse[" + descriptions.size() + "]");
                        if (descriptions.size() > enforced || calls.size() == rejectCall) {
                            return fault(rejectStatus);
                        }
                        final boolean refuseAll = refuseContinuationsForRequests-- > 0;
                        final BrowseResult[] results = new BrowseResult[descriptions.size()];
                        int pointsInUse = continuations.size();
                        int exhausted = 0;
                        for (int i = 0; i < results.length; i++) {
                            final NodeId node = descriptions.get(i).getNodeId();
                            browsedNodes.add(node.toParseableString());
                            final BrowseResult fixed = fixedResults.get(node);
                            if (fixed != null) {
                                results[i] = fixed;
                                continue;
                            }
                            final ReferenceDescription[] refs =
                                    children.getOrDefault(node, new ReferenceDescription[0]);
                            if (refs.length > pageSize && (refuseAll || pointsInUse >= continuationCapacity)) {
                                results[i] = new BrowseResult(
                                        new StatusCode(StatusCodes.Bad_NoContinuationPoints),
                                        ByteString.NULL_VALUE,
                                        new ReferenceDescription[0]);
                                exhausted++;
                                continue;
                            }
                            results[i] = page(refs);
                            if (refs.length > pageSize) {
                                pointsInUse++;
                            }
                        }
                        exhaustedPerRequest.add(exhausted);
                        final BrowseResult[] returned =
                                dropLastResult ? Arrays.copyOf(results, results.length - 1) : results;
                        final BrowseResponse response =
                                new BrowseResponse(null, nullResultsNow() ? null : returned, null);
                        if (calls.size() == deferCall) {
                            final CompletableFuture<BrowseResponse> deferred = new CompletableFuture<>();
                            deferredCompletion = () -> complete(deferred, response);
                            return deferred;
                        }
                        return CompletableFuture.completedFuture(response);
                    });
            when(client.browseNextAsync(anyBoolean(), anyList())).thenAnswer(invocation -> {
                final boolean release = invocation.getArgument(0);
                final List<ByteString> points = invocation.getArgument(1);
                calls.add((release ? "release[" : "next[") + points.size() + "]");
                if (release && releaseFault != 0) {
                    return fault(releaseFault);
                }
                if (!release && nextFault != 0) {
                    return fault(nextFault);
                }
                if (points.size() > maxPointsPerBrowseNext) {
                    return fault(StatusCodes.Bad_TooManyOperations);
                }
                final BrowseResult[] results = new BrowseResult[points.size()];
                for (int i = 0; i < results.length; i++) {
                    final List<ReferenceDescription> rest = continuations.remove(points.get(i));
                    if (release) {
                        results[i] =
                                new BrowseResult(StatusCode.GOOD, ByteString.NULL_VALUE, new ReferenceDescription[0]);
                        continue;
                    }
                    final String owner = rest != null && !rest.isEmpty()
                            ? rest.get(0).getNodeId().toParseableString()
                            : "";
                    if (nextStatus != null
                            && (nextStatusOwnerPrefix == null || owner.startsWith(nextStatusOwnerPrefix))) {
                        results[i] = new BrowseResult(nextStatus, ByteString.NULL_VALUE, new ReferenceDescription[0]);
                        continue;
                    }
                    results[i] = nullReferencesOnPages
                            ? new BrowseResult(StatusCode.GOOD, ByteString.NULL_VALUE, null)
                            : page(rest.toArray(ReferenceDescription[]::new));
                }
                final BrowseResult[] returned =
                        dropLastNextResult ? Arrays.copyOf(results, results.length - 1) : results;
                final BrowseNextResponse response =
                        new BrowseNextResponse(null, nullResultsNow() ? null : returned, null);
                if (calls.size() == deferCall) {
                    final CompletableFuture<BrowseNextResponse> deferred = new CompletableFuture<>();
                    deferredCompletion = () -> complete(deferred, response);
                    return deferred;
                }
                return CompletableFuture.completedFuture(response);
            });
        }

        private boolean nullResultsNow() {
            return nullResultsFromCall > 0 && calls.size() >= nullResultsFromCall;
        }

        private <T> void complete(final @NotNull CompletableFuture<T> deferred, final @NotNull T response) {
            if (deferredFault != 0) {
                deferred.completeExceptionally(serviceFault(deferredFault));
            } else {
                deferred.complete(response);
            }
        }

        /**
         * A refused request, shaped like Milo's: the client's future is a dependent stage of the transport's,
         * so a callback attached to it sees the fault wrapped in a CompletionException.
         */
        static <T> @NotNull CompletableFuture<T> fault(final long status) {
            return CompletableFuture.<T>failedFuture(serviceFault(status)).thenApply(r -> r);
        }

        private @NotNull BrowseResult page(final @NotNull ReferenceDescription[] refs) {
            final StatusCode good = nullStatus ? null : StatusCode.GOOD;
            if (refs.length <= pageSize) {
                return new BrowseResult(
                        good, ByteString.NULL_VALUE, refs.length == 0 && nullReferencesForLeaves ? null : refs);
            }
            final ByteString point = ByteString.of(new byte[] {(byte) ++nextContinuation});
            continuations.put(point, List.of(refs).subList(pageSize, refs.length));
            return new BrowseResult(good, point, Arrays.copyOf(refs, pageSize));
        }
    }

    static @NotNull UaServiceFaultException serviceFault(final long status) {
        final ResponseHeader header =
                new ResponseHeader(DateTime.now(), uint(0), new StatusCode(status), null, null, null);
        return new UaServiceFaultException(new ServiceFault(header));
    }

    /**
     * Stands in for the server's Read service: answers the MaxNodesPerRead read with {@code advertised} (0 = the
     * node reads as null), rejects any attribute read with more than {@code enforced} ReadValueIds with
     * Bad_TooManyOperations, and answers everything else with null values. Records what it was asked.
     */
    static final class FakeReadServer {
        final int advertised;
        final int enforced;
        boolean limitReadFails;
        int limitReads;
        long rejectStatus = StatusCodes.Bad_TooManyOperations;
        final List<Integer> attributeReadSizes = new ArrayList<>();
        /** Index (within the browse's variable list) of the first variable of each attribute read. */
        final List<Integer> attributeReadOffsets = new ArrayList<>();

        final int advertisedBrowse;
        int advertisedContinuationPoints;
        /** When set, every attribute read answers one DataValue short (a server violating one-result-per-id). */
        boolean dropLastValue;

        FakeReadServer(final int advertised, final int enforced) {
            this(advertised, enforced, 0);
        }

        FakeReadServer(final int advertised, final int enforced, final int advertisedBrowse) {
            this.advertised = advertised;
            this.enforced = enforced;
            this.advertisedBrowse = advertisedBrowse;
        }

        void install(final @NotNull OpcUaClient client) {
            when(client.readAsync(anyDouble(), any(), anyList())).thenAnswer(invocation -> {
                final List<ReadValueId> ids = invocation.getArgument(2);
                if (ids.size() == 3
                        && NodeIds.Server_ServerCapabilities_OperationLimits_MaxNodesPerRead.equals(
                                ids.get(0).getNodeId())
                        && NodeIds.Server_ServerCapabilities_OperationLimits_MaxNodesPerBrowse.equals(
                                ids.get(1).getNodeId())
                        && NodeIds.Server_ServerCapabilities_MaxBrowseContinuationPoints.equals(
                                ids.get(2).getNodeId())) {
                    limitReads++;
                    if (limitReadFails) {
                        return CompletableFuture.failedFuture(new UaException(StatusCodes.Bad_NodeIdUnknown));
                    }
                    final Variant read = advertised > 0 ? new Variant(uint(advertised)) : Variant.NULL_VALUE;
                    final Variant browse =
                            advertisedBrowse > 0 ? new Variant(uint(advertisedBrowse)) : Variant.NULL_VALUE;
                    // UInt16 on the wire, unlike the two UInt32 operation limits
                    final Variant points = advertisedContinuationPoints > 0
                            ? new Variant(ushort(advertisedContinuationPoints))
                            : Variant.NULL_VALUE;
                    return CompletableFuture.completedFuture(new ReadResponse(
                            null,
                            new DataValue[] {new DataValue(read), new DataValue(browse), new DataValue(points)},
                            null));
                }
                attributeReadSizes.add(ids.size());
                // Only the browseVariables() fixtures use the zero-padded "VarNNN" ids this decodes.
                final String first = ids.get(0).getNodeId().toParseableString();
                if (first.matches(".*Var\\d{3}$")) {
                    attributeReadOffsets.add(Integer.parseInt(first.substring(first.length() - 3)));
                }
                if (ids.size() > enforced) {
                    return CompletableFuture.failedFuture(serviceFault(rejectStatus));
                }
                final DataValue[] values = new DataValue[dropLastValue ? ids.size() - 1 : ids.size()];
                Arrays.fill(values, new DataValue(Variant.NULL_VALUE));
                return CompletableFuture.completedFuture(new ReadResponse(null, values, null));
            });
        }
    }
}
