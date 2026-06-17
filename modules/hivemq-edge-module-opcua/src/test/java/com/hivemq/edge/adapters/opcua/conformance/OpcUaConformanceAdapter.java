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
package com.hivemq.edge.adapters.opcua.conformance;

import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterOutput;
import com.hivemq.adapter.sdk.api.v2.model.VerifyOutcome;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.template.AbstractProtocolAdapter;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * EDG-737 — a minimal real OPC-UA protocol adapter written against the SDK v2 contract
 * ({@link AbstractProtocolAdapter}) and a real Milo client, used only to verify that the foundation model
 * carries real OPC-UA interactions (connect / verify / poll) against an embedded server. Test scope; never
 * shipped. Subscription, browse, and write are covered by later conformance scenarios.
 */
final class OpcUaConformanceAdapter extends AbstractProtocolAdapter {

    private static final long REQUEST_TIMEOUT_SECONDS = 5L;

    private final @NotNull String endpointUrl;
    private @NotNull OpcUaClient client;

    OpcUaConformanceAdapter(
            final @NotNull ProtocolAdapterInput input,
            final @NotNull ProtocolAdapterOutput output,
            final @NotNull String endpointUrl) {
        super(input, output);
        this.endpointUrl = endpointUrl;
    }

    @Override
    protected void doStart() {
        output.started();
    }

    @Override
    protected void doStop() {
        if (client != null) {
            try {
                client.disconnect();
            } catch (final Exception ignored) {
                // teardown failure does not change the outcome — stop() always acks stopped()
            }
        }
        output.stopped();
    }

    @Override
    protected void doConnect() {
        try {
            client = OpcUaClient.create(
                    endpointUrl,
                    endpoints -> endpoints.stream()
                            .filter(endpoint -> SecurityPolicy.None.getUri().equals(endpoint.getSecurityPolicyUri()))
                            .findFirst(),
                    transportConfig -> {},
                    clientConfig -> {});
            client.connect();
            output.connected();
        } catch (final Exception connectFailure) {
            output.error(com.hivemq.adapter.sdk.api.v2.model.ErrorScope.CONNECTION, connectFailure.getMessage());
        }
    }

    @Override
    protected void doDisconnect() {
        try {
            if (client != null) {
                client.disconnect();
            }
        } catch (final Exception ignored) {
            // best effort
        }
        output.disconnected();
    }

    @Override
    protected void doVerifyNode(final @NotNull Node node) {
        try {
            final NodeId nodeId = NodeId.parse(node.nodeId());
            final List<ReadValueId> reads = List.of(
                    new ReadValueId(nodeId, AttributeId.DataType.uid(), null, null),
                    new ReadValueId(nodeId, AttributeId.AccessLevel.uid(), null, null));
            final DataValue[] results = client.readAsync(0.0, TimestampsToReturn.Neither, reads)
                    .get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .getResults();
            final boolean good = results != null &&
                    Arrays.stream(results).allMatch(value -> value.getStatusCode().isGood());
            output.verifyResult(node, good
                    ? new VerifyOutcome.Success()
                    : new VerifyOutcome.PermanentFailure("declared node not readable on device: " + node.nodeId()));
        } catch (final Exception failure) {
            output.verifyResult(node, new VerifyOutcome.TransientFailure(failure.getMessage()));
        }
    }

    @Override
    protected void doPoll(final @NotNull Node node) {
        try {
            final NodeId nodeId = NodeId.parse(node.nodeId());
            final DataValue value = client.readAsync(
                            0.0,
                            TimestampsToReturn.Both,
                            List.of(new ReadValueId(nodeId, AttributeId.Value.uid(), null, null)))
                    .get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .getResults()[0];
            if (value.getStatusCode().isGood() && value.getValue().getValue() != null) {
                output.dataPoint(node, dataPointFactory.create(node.nodeId(), value.getValue().getValue()));
            } else {
                output.nodeError(node, "bad read status for " + node.nodeId(), false);
            }
        } catch (final Exception failure) {
            output.nodeError(node, failure.getMessage(), false);
        }
    }

    @Override
    protected void doAddSubscription(final @NotNull Node node) {
        // Real monitored-item subscription is conformance scenario 3 (EDG-737). Not exercised by scenario 1.
        throw new UnsupportedOperationException("subscription scenario not yet implemented");
    }

    @Override
    protected void doWrite(final @NotNull Node node, final @NotNull com.hivemq.adapter.sdk.api.data.DataPoint value) {
        // Southbound write is conformance scenario 2 (EDG-737). Not exercised by scenario 1.
        throw new UnsupportedOperationException("write scenario not yet implemented");
    }
}
