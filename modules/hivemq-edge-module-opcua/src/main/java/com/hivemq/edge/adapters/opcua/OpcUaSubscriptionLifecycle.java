package com.hivemq.edge.adapters.opcua;

import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.exceptions.TagDefinitionParseException;
import com.hivemq.adapter.sdk.api.exceptions.TagNotFoundException;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterPublishService;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.adapters.opcua.client.OpcUaSubscriptionConsumer;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttMapping;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscriptionManager;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaServiceFaultException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class OpcUaSubscriptionLifecycle implements UaSubscriptionManager.SubscriptionListener {

    private static final Logger log = LoggerFactory.getLogger(OpcUaSubscriptionLifecycle.class);

    private final @NotNull Map<UInteger, OpcUaSubscriptionConsumer.SubscriptionResult> subscriptionMap =
            new ConcurrentHashMap<>();
    private final @NotNull OpcUaClient opcUaClient;
    private final @NotNull String adapterId;
    private final @NotNull String protocolAdapterId;
    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private final @NotNull EventService eventService;
    private final @NotNull ProtocolAdapterPublishService adapterPublishService;
    private final @NotNull List<OpcuaTag> opcuaTags;

    public OpcUaSubscriptionLifecycle(
            final @NotNull OpcUaClient opcUaClient,
            final @NotNull String adapterId,
            final @NotNull String protocolAdapterId,
            final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService,
            final @NotNull EventService eventService,
            final @NotNull ProtocolAdapterPublishService adapterPublishService,
            final @NotNull List<OpcuaTag> opcuaTags) {
        this.opcUaClient = opcUaClient;
        this.adapterId = adapterId;
        this.protocolAdapterId = protocolAdapterId;
        this.protocolAdapterMetricsService = protocolAdapterMetricsService;
        this.eventService = eventService;
        this.adapterPublishService = adapterPublishService;
        this.opcuaTags = opcuaTags;
    }

    @Override
    public void onKeepAlive(final @NotNull UaSubscription subscription, final @NotNull DateTime publishTime) {
        UaSubscriptionManager.SubscriptionListener.super.onKeepAlive(subscription, publishTime);
        protocolAdapterMetricsService.increment("subscription.keepalive.count");
    }

    @Override
    public void onSubscriptionTransferFailed(
            final @NotNull UaSubscription subscription, final @NotNull StatusCode statusCode) {

        protocolAdapterMetricsService.increment("subscription.transfer.failed.count");

        //clear subscription from the map since it is dead
        final OpcUaSubscriptionConsumer.SubscriptionResult subscriptionResult =
                subscriptionMap.remove(subscription.getSubscriptionId());

        if (subscriptionResult != null) {
            subscribe(subscriptionResult.subscription).exceptionally(ex -> {
                if (ex instanceof UaServiceFaultException) {
                    UaServiceFaultException cause = (UaServiceFaultException) ex.getCause();
                    if (cause.getStatusCode().getValue() == StatusCodes.Bad_SubscriptionIdInvalid) {
                        log.warn("Resubscribing to OPC UA after transfer failure: {}", statusCode, ex);
                        try {
                            subscribe(subscriptionResult.subscription).exceptionally(t -> {
                                log.error("Problem resucbscribing after subscription {} failed with {}",
                                        subscription.getSubscriptionId(),
                                        statusCode,
                                        t);
                                return null;
                            }).get();
                        } catch (InterruptedException | ExecutionException e) {
                            log.error("Problem resucbscribing after subscription {} failed with {}",
                                    subscription.getSubscriptionId(),
                                    statusCode,
                                    e);
                        }
                    } else {
                        log.error("Not able to recreate OPC UA subscription after transfer failure", ex);
                    }
                } else {
                    log.error("Not able to recreate OPC UA subscription after transfer failure", ex);
                }
                return null;
            });
        } else {
            log.error("Received Transfer Failure {} for a non existent subscription: {}", subscription, statusCode);
        }
    }

    public Optional<OpcuaTag> findTag(String tagName) {
        return opcuaTags.stream()
                .filter(tag -> tag.getTagName().equals(tagName))
                .findFirst();
    }

    public CompletableFuture<Void> subscribe(final @NotNull OpcUaToMqttMapping subscription) {
        final @NotNull String tagName = subscription.getTagName();

        return findTag(subscription.getTagName())
                .map(tag -> subscribeToOpcua(subscription, tag))
                .orElseGet(() ->
                        CompletableFuture.failedFuture(
                                new IllegalArgumentException("Opcua subscription for protocol adapter failed because the used tag '" +
                                    tagName +
                                    "' was not found. For the polling to work the tag must be created via REST API or the UI.")));
    }

    private CompletableFuture<Void> subscribeToOpcua(
            @NotNull OpcUaToMqttMapping subscription,
            Tag<OpcuaTagDefinition> opcuaTag) {
        final String nodeId = opcuaTag.getTagDefinition().getNode();
        log.info("Subscribing to OPC UA node {}", nodeId);
        final ReadValueId readValueId =
                new ReadValueId(NodeId.parse(nodeId), AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE);

        return opcUaClient.getSubscriptionManager()
                .createSubscription(subscription.getPublishingInterval())
                .thenCompose(uaSubscription -> new OpcUaSubscriptionConsumer(subscription,
                        uaSubscription,
                        readValueId,
                        adapterPublishService,
                        eventService,
                        Optional.ofNullable(opcUaClient.getStackClient().getConfig().getEndpoint()),
                        opcUaClient.getDynamicSerializationContext(),
                        protocolAdapterMetricsService,
                        adapterId,
                        protocolAdapterId).start())
                .thenApply(result -> {
                    subscriptionMap.put(result.uaSubscription.getSubscriptionId(), result);
                    return null;
                });
    }

    public @NotNull CompletableFuture<Void> subscribeAll(final @NotNull List<OpcUaToMqttMapping> mappings) {

        final CompletableFuture<Void> resultFuture = new CompletableFuture<>();

        final CompletableFuture[] subscriptionFutures =
                mappings.stream().map(this::subscribe).toArray(CompletableFuture[]::new);


        CompletableFuture.allOf(subscriptionFutures).thenApply(unused -> {
            resultFuture.complete(null);
            return null;
        }).exceptionally(throwable -> {
            resultFuture.completeExceptionally(throwable);
            return null;
        });
        return resultFuture;
    }


    public CompletableFuture<Void> stop() {
        return CompletableFuture.completedFuture(null);
    }

}
