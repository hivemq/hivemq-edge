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
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.eclipse.milo.opcua.sdk.client.DiscoveryClient;
import org.eclipse.milo.opcua.sdk.client.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.server.OpcUaServerConfigLimits;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.transport.client.tcp.OpcTcpClientTransport;
import org.eclipse.milo.opcua.stack.transport.client.tcp.OpcTcpClientTransportConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import util.EmbeddedOpcUaServerExtension;

/// The Read and Browse requests a data-type tree build sends, against servers that advertise 0 for both limits, as
/// the CAS-QS-3110-F gateway does (EDG-1125). Each case first builds a reference tree while the server still
/// advertises what it enforces.
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

        /// The server refuses a batch of the default size with `Bad_TooManyOperations`; Milo replays it one
        /// operation at a time.
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

    /// Records only the tree build: the connect-time reads and the limits read happen before recording starts.
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
}
