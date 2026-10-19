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

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import java.util.List;
import java.util.Optional;
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
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
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
 * Remove this class when Edge moves to Milo 1.2, which can override the limits in its client configuration.
 */
public class EdgeOpcUaClient extends OpcUaClient {

    static final int DEFAULT_OPERATIONS_PER_REQUEST = 100;

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
