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

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.discovery.NodeType;
import com.hivemq.adapter.sdk.api.v2.model.BrowseContinuation;
import com.hivemq.adapter.sdk.api.v2.model.BrowseFilter;
import com.hivemq.adapter.sdk.api.v2.model.BrowseResultEntry;
import com.hivemq.adapter.sdk.api.v2.model.ErrorScope;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterOutput;
import com.hivemq.adapter.sdk.api.v2.model.ResolvedAttributes;
import com.hivemq.adapter.sdk.api.v2.model.VerifyOutcome;
import com.hivemq.adapter.sdk.api.v2.node.AccessFlags;
import com.hivemq.adapter.sdk.api.v2.node.AccessTriState;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.template.AbstractProtocolAdapter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ViewDescription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * EDG-737 — a minimal real OPC-UA protocol adapter written against the SDK v2 contract
 * ({@link AbstractProtocolAdapter}) and a real Milo client, used only to verify that the foundation model
 * carries real OPC-UA interactions against an embedded server. Test scope; never shipped.
 * <p>
 * Scenarios:
 * 1. connect/verify/poll,
 * 2. write,
 * 3. subscribe (incremental-add shadow set),
 * 4. browse (paginated — one page per command, demonstrating EDG-737 Finding B),
 * 5. resolve (read discovered nodes' DataType/AccessLevel/Description — the RESOLVE step of browse).
 */
final class OpcUaConformanceAdapter extends AbstractProtocolAdapter implements OpcUaSubscription.SubscriptionListener {

    private static final long REQUEST_TIMEOUT_SECONDS = 5L;
    private static final double SAMPLING_INTERVAL_MILLIS = 200.0;

    private final @NotNull String endpointUrl;
    // the PA's shadow set of subscribed nodes — the incremental-add contract (PR #97)
    private final @NotNull Map<NodeId, Node> subscribedNodes;
    private @Nullable OpcUaClient client;
    private @Nullable OpcUaSubscription subscription;
    private long pageDelayMillis;

    OpcUaConformanceAdapter(
            final @NotNull ProtocolAdapterInput input,
            final @NotNull ProtocolAdapterOutput output,
            final @NotNull String endpointUrl) {
        super(input, output);
        this.endpointUrl = endpointUrl;
        this.subscribedNodes = new ConcurrentHashMap<>();
    }

    /**
     * Map one node's three attribute reads into the SDK's protocol-agnostic {@link ResolvedAttributes}.
     */
    private static @NotNull ResolvedAttributes resolveOne(
            final @NotNull Node node,
            final @NotNull DataValue dataTypeValue,
            final @NotNull DataValue accessLevelValue,
            final @NotNull DataValue descriptionValue) {
        final Object dataType = dataTypeValue.getValue().getValue();
        final String dataTypeId = (dataTypeValue.getStatusCode().isGood() && dataType instanceof final NodeId nodeId)
                ? nodeId.toParseableString()
                : "";
        final Object accessLevel = accessLevelValue.getValue().getValue();
        final int accessBits = (accessLevelValue.getStatusCode().isGood() && accessLevel instanceof final UByte uByte)
                ? uByte.intValue()
                : 0;
        final Object description = descriptionValue.getValue().getValue();
        final String descriptionText =
                (descriptionValue.getStatusCode().isGood() && description instanceof final LocalizedText localizedText)
                        ? requireNonNullElse(localizedText.getText(), "")
                        : "";
        return new ResolvedAttributes(node, dataTypeId, accessFlags(accessBits), descriptionText);
    }

    /**
     * OPC-UA AccessLevel bitmask (bit0 CurrentRead, bit1 CurrentWrite) → the SDK's {@link AccessFlags}.
     */
    private static @NotNull AccessFlags accessFlags(final int accessBits) {
        final AccessTriState readable = (accessBits & 0x01) != 0 ? AccessTriState.YES : AccessTriState.NO;
        final AccessTriState writable = (accessBits & 0x02) != 0 ? AccessTriState.YES : AccessTriState.NO;
        // a readable OPC-UA variable is both pollable and subscribable; an unreadable one is neither
        final AccessTriState pollable = readable;
        final AccessTriState subscribable = readable;
        return AccessFlags.builder()
                .readable(readable)
                .writable(writable)
                .pollable(pollable)
                .subscribable(subscribable)
                .build();
    }

    @Override
    protected void doStart() {
        output.started();
    }

    @Override
    protected void doStop() {
        closeClientQuietly();
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
                    _ -> {},
                    _ -> {});
            client.connect();
            output.connected();
        } catch (final @NotNull Exception connectFailure) {
            output.error(ErrorScope.CONNECTION, String.valueOf(connectFailure.getMessage()));
        }
    }

    @Override
    protected void doDisconnect() {
        closeClientQuietly();
        output.disconnected();
    }

    @Override
    protected void doVerifyNode(final @NotNull Node node) {
        try {
            final NodeId nodeId = NodeId.parse(node.nodeId());
            final List<ReadValueId> reads = List.of(
                    new ReadValueId(nodeId, AttributeId.DataType.uid(), null, null),
                    new ReadValueId(nodeId, AttributeId.AccessLevel.uid(), null, null));
            final DataValue[] results = requireNonNull(client)
                    .readAsync(0.0, TimestampsToReturn.Neither, reads)
                    .get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .getResults();
            final boolean good = results != null
                    && Arrays.stream(results)
                            .allMatch(value -> value.getStatusCode().isGood());
            output.verifyResult(
                    node,
                    good
                            ? new VerifyOutcome.Success()
                            : new VerifyOutcome.PermanentFailure(
                                    "declared node not readable on device: " + node.nodeId()));
        } catch (final @NotNull Exception failure) {
            output.verifyResult(node, new VerifyOutcome.TransientFailure(String.valueOf(failure.getMessage())));
        }
    }

    @Override
    protected void doPoll(final @NotNull Node node) {
        try {
            final NodeId nodeId = NodeId.parse(node.nodeId());
            final DataValue value = requireNonNull(requireNonNull(client)
                    .readAsync(
                            0.0,
                            TimestampsToReturn.Both,
                            List.of(new ReadValueId(nodeId, AttributeId.Value.uid(), null, null)))
                    .get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .getResults())[0];
            if (value.getStatusCode().isGood() && value.getValue().getValue() != null) {
                output.dataPoint(
                        node,
                        dataPointFactory.create(node.nodeId(), value.getValue().getValue()));
            } else {
                output.nodeError(node, "bad read status for " + node.nodeId(), false);
            }
        } catch (final @NotNull Exception failure) {
            output.nodeError(node, String.valueOf(failure.getMessage()), false);
        }
    }

    @Override
    protected void doAddSubscription(final @NotNull Node node) {
        try {
            if (subscription == null) {
                subscription = new OpcUaSubscription(requireNonNull(client));
                subscription.setPublishingInterval(SAMPLING_INTERVAL_MILLIS);
                subscription.create();
                subscription.setSubscriptionListener(this);
            }
            final NodeId nodeId = NodeId.parse(node.nodeId());
            // incremental add: only this node is touched; existing monitored items are untouched
            final OpcUaMonitoredItem monitoredItem = OpcUaMonitoredItem.newDataItem(nodeId);
            monitoredItem.setSamplingInterval(SAMPLING_INTERVAL_MILLIS);
            subscription.addMonitoredItem(monitoredItem);
            subscription.synchronizeMonitoredItems();
            subscribedNodes.put(nodeId, node);
        } catch (final @NotNull Exception failure) {
            output.nodeError(node, String.valueOf(failure.getMessage()), false);
        }
    }

    @Override
    protected void doWrite(final @NotNull Node node, final @NotNull DataPoint value) {
        try {
            final NodeId nodeId = NodeId.parse(node.nodeId());
            final List<StatusCode> statusCodes = requireNonNull(client)
                    .writeValuesAsync(
                            List.of(nodeId),
                            List.of(new DataValue(Variant.of(value.getTagValue()), StatusCode.GOOD, null)))
                    .get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            final Optional<StatusCode> bad =
                    statusCodes.stream().filter(StatusCode::isBad).findFirst();
            output.writeResult(
                    node, bad.isEmpty(), bad.map(StatusCode::toString).orElse(null));
        } catch (final @NotNull Exception failure) {
            output.writeResult(node, false, String.valueOf(failure.getMessage()));
        }
    }

    /**
     * Simulate a slow device: each browse page waits this long before being reported.
     */
    void pageDelay(final long millis) {
        this.pageDelayMillis = millis;
    }

    /**
     * One page of a paginated browse below the filter node (EDG-737 Finding B fix). The adapter does a single
     * server round-trip and reports one page via {@code browsePage} — with a {@link BrowseContinuation} when
     * more remain — instead of walking the whole address space in one command. Each page is a separate mailbox
     * round-trip, so the framework interleaves {@code CONTROL} commands and polls between pages.
     */
    @Override
    protected void doBrowse(final int requestId, final @NotNull BrowseFilter filter, final int maxReferences) {
        try {
            final BrowseDescription browseDescription = new BrowseDescription(
                    NodeId.parse(filter.filterNode().nodeId()),
                    BrowseDirection.Forward,
                    NodeIds.HierarchicalReferences,
                    true,
                    uint(0),
                    uint(BrowseResultMask.All.getValue()));
            final ViewDescription view = new ViewDescription(NodeId.NULL_VALUE, DateTime.MIN_VALUE, uint(0));
            final BrowseResult result = requireNonNull(requireNonNull(client)
                    .browseAsync(view, uint(Math.max(0, maxReferences)), List.of(browseDescription))
                    .get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .getResults())[0];
            emitPage(requestId, result);
        } catch (final @NotNull Exception failure) {
            output.browseError(requestId, String.valueOf(failure.getMessage()));
        }
    }

    @Override
    protected void doBrowseNext(final int requestId, final @NotNull BrowseContinuation continuation) {
        try {
            final ByteString continuationPoint =
                    ByteString.of(Base64.getDecoder().decode(continuation.token()));
            final BrowseResult result = requireNonNull(requireNonNull(client)
                    .browseNextAsync(false, List.of(continuationPoint))
                    .get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .getResults())[0];
            emitPage(requestId, result);
        } catch (final @NotNull Exception failure) {
            output.browseError(requestId, String.valueOf(failure.getMessage()));
        }
    }

    /**
     * Build one page from a {@link BrowseResult} and report it — after an optional per-page delay simulating a
     * slow device, so the framework's interleaving between pages is observable.
     */
    private void emitPage(final int requestId, final @NotNull BrowseResult result) {
        if (pageDelayMillis > 0L) {
            try {
                Thread.sleep(pageDelayMillis);
            } catch (final @NotNull InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        final NamespaceTable namespaceTable = requireNonNull(client).getNamespaceTable();
        final List<BrowseResultEntry> entries = new ArrayList<>();
        if (result.getReferences() != null) {
            for (final ReferenceDescription reference : result.getReferences()) {
                final Optional<NodeId> childId = reference.getNodeId().toNodeId(namespaceTable);
                if (childId.isEmpty()) {
                    continue;
                }
                final boolean isVariable = reference.getNodeClass() == NodeClass.Variable;
                final String browseName = reference.getBrowseName() != null
                        ? requireNonNullElse(reference.getBrowseName().getName(), "")
                        : "";
                entries.add(new BrowseResultEntry(
                        new ConformanceNode(childId.get().toParseableString()),
                        isVariable ? NodeType.VALUE : NodeType.OBJECT,
                        isVariable,
                        browseName));
            }
        }
        final ByteString continuationPoint = result.getContinuationPoint();
        final BrowseContinuation continuation =
                (continuationPoint != null && continuationPoint.bytes() != null && continuationPoint.bytes().length > 0)
                        ? new BrowseContinuation(Base64.getEncoder().encodeToString(continuationPoint.bytes()))
                        : null;
        output.browsePage(requestId, entries, continuation);
    }

    /**
     * RESOLVE step (EDG-737): read each discovered node's DataType, AccessLevel and Description in a single
     * batched {@code readAsync} round-trip and report them as one {@code readAttributesResult}. This is the same
     * mechanism as {@link #doVerifyNode(Node)} (which reads DataType + AccessLevel to verify), generalised to
     * return the resolved values the framework's PAW needs to build typed tag definitions. One round-trip per
     * batch keeps RESOLVE on the same paginated, interleavable footing as browse.
     */
    @Override
    protected void doReadNodeAttributes(final int requestId, final @NotNull List<Node> nodes) {
        try {
            final List<ReadValueId> reads = new ArrayList<>(nodes.size() * 3);
            for (final Node node : nodes) {
                final NodeId nodeId = NodeId.parse(node.nodeId());
                reads.add(new ReadValueId(nodeId, AttributeId.DataType.uid(), null, null));
                reads.add(new ReadValueId(nodeId, AttributeId.AccessLevel.uid(), null, null));
                reads.add(new ReadValueId(nodeId, AttributeId.Description.uid(), null, null));
            }
            final DataValue[] results = requireNonNull(client)
                    .readAsync(0.0, TimestampsToReturn.Neither, reads)
                    .get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .getResults();
            final List<ResolvedAttributes> resolved = new ArrayList<>(nodes.size());
            for (int i = 0; i < nodes.size(); i++) {
                resolved.add(resolveOne(
                        nodes.get(i), requireNonNull(results)[i * 3], results[i * 3 + 1], results[i * 3 + 2]));
            }
            output.readAttributesResult(requestId, resolved);
        } catch (final @NotNull Exception failure) {
            output.browseError(requestId, String.valueOf(failure.getMessage()));
        }
    }

    @Override
    public void onDataReceived(
            final @NotNull OpcUaSubscription subscription,
            final @NotNull List<OpcUaMonitoredItem> items,
            final @NotNull List<DataValue> values) {
        for (int i = 0; i < items.size(); i++) {
            final NodeId nodeId = items.get(i).getReadValueId().getNodeId();
            final Node node = subscribedNodes.get(nodeId);
            final Object value = values.get(i).getValue().getValue();
            if (node != null && value != null) {
                output.dataPoint(node, dataPointFactory.create(node.nodeId(), value));
            }
        }
    }

    private void closeClientQuietly() {
        try {
            if (client != null) {
                client.disconnect();
            }
        } catch (final @NotNull Exception ignored) {
            // teardown failure does not change the outcome
        }
        client = null;
        subscription = null;
        subscribedNodes.clear();
    }
}
