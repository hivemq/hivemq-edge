/*
 * Copyright 2019-present HiveMQ GmbH
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
package com.hivemq.extensions.services.publish;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.codec.encoder.mqtt5.Mqtt5PayloadFormatIndicator;
import com.hivemq.configuration.HivemqId;
import com.hivemq.datagov.DataGovernanceContext;
import com.hivemq.datagov.DataGovernanceService;
import com.hivemq.datagov.impl.DataGovernanceContextImpl;
import com.hivemq.datagov.model.DataGovernanceData;
import com.hivemq.datagov.model.impl.DataGovernanceDataImpl;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;
import com.hivemq.extension.sdk.api.services.publish.Publish;
import com.hivemq.extension.sdk.api.services.publish.PublishService;
import com.hivemq.extension.sdk.api.services.publish.PublishToClientResult;
import com.hivemq.extensions.ListenableFutureConverter;
import com.hivemq.extensions.services.PluginServiceRateLimitService;
import com.hivemq.extensions.services.executor.GlobalManagedExtensionExecutorService;
import com.hivemq.mqtt.handler.publish.PublishStatus;
import com.hivemq.mqtt.handler.publish.PublishingResult;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.services.PublishDistributor;
import com.hivemq.mqtt.topic.SubscriberWithIdentifiers;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.util.Bytes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.mqtt.message.publish.PUBLISH.MESSAGE_EXPIRY_INTERVAL_NOT_SET;

/**
 * @author Lukas Brandl
 * @since 4.0.0
 */
@Singleton
public class PublishServiceImpl implements PublishService {

    @NotNull
    private final PluginServiceRateLimitService rateLimitService;

    @NotNull
    private final GlobalManagedExtensionExecutorService globalManagedExtensionExecutorService;

    @NotNull
    private final PublishDistributor publishDistributor;

    @NotNull
    private final HivemqId hiveMQId;

    @NotNull
    private final LocalTopicTree topicTree;

    @NotNull
    private final DataGovernanceService dataGovernanceService;

    @Inject
    public PublishServiceImpl(final @NotNull PluginServiceRateLimitService rateLimitService,
                              final @NotNull GlobalManagedExtensionExecutorService globalManagedExtensionExecutorService,
                              final @NotNull PublishDistributor publishDistributor,
                              final @NotNull HivemqId hiveMQId,
                              final @NotNull LocalTopicTree topicTree,
                              final @NotNull DataGovernanceService dataGovernanceService) {
        this.rateLimitService = rateLimitService;
        this.globalManagedExtensionExecutorService = globalManagedExtensionExecutorService;
        this.publishDistributor = publishDistributor;
        this.hiveMQId = hiveMQId;
        this.topicTree = topicTree;
        this.dataGovernanceService = dataGovernanceService;
    }

    @Override
    @NotNull
    public CompletableFuture<Void> publish(final @NotNull Publish publish) {
        checkNotNull(publish, "Publish must never be null");
        if (rateLimitService.rateLimitExceeded()) {
            return CompletableFuture.failedFuture(PluginServiceRateLimitService.RATE_LIMIT_EXCEEDED_EXCEPTION);
        }
        if (!(publish instanceof PublishImpl)) {
            return CompletableFuture.failedFuture(new DoNotImplementException(Publish.class.getSimpleName()));
        }

        final PUBLISH internalPublish = publishToPUBLISH((PublishImpl) publish);

        final DataGovernanceData data = new DataGovernanceDataImpl.Builder()
                .withPublish(internalPublish)
                .withClientId("publish service")
                .build();
        final DataGovernanceContext governanceContext = new DataGovernanceContextImpl(data);
        governanceContext.setExecutorService(globalManagedExtensionExecutorService);
        final ListenableFuture<PublishingResult> publishFuture = dataGovernanceService.applyAndPublish(governanceContext);
        return ListenableFutureConverter.toVoidCompletable(publishFuture, globalManagedExtensionExecutorService);
    }

    @Override
    @NotNull
    public CompletableFuture<PublishToClientResult> publishToClient(final @NotNull Publish publish, final @NotNull String clientId) {
        checkNotNull(publish, "Publish must never be null");
        checkNotNull(clientId, "Client ID must never be null");
        checkArgument(!clientId.isEmpty(), "Client ID must not be empty");
        if (rateLimitService.rateLimitExceeded()) {
            return CompletableFuture.failedFuture(PluginServiceRateLimitService.RATE_LIMIT_EXCEEDED_EXCEPTION);
        }
        if (!(publish instanceof PublishImpl)) {
            return CompletableFuture.failedFuture(new DoNotImplementException(Publish.class.getSimpleName()));
        }
        final PUBLISH internalPublish = publishToPUBLISH((PublishImpl) publish);

        final SettableFuture<PublishToClientResult> sendPublishFuture = SettableFuture.create();
        final SubscriberWithIdentifiers subscriber = topicTree.findSubscriber(clientId, publish.getTopic());

        if (subscriber == null) {
            sendPublishFuture.set(PublishToClientResult.NOT_SUBSCRIBED);
            return ListenableFutureConverter.toCompletable(sendPublishFuture, globalManagedExtensionExecutorService);
        }

        final ListenableFuture<PublishStatus> publishSendFuture = publishDistributor.sendMessageToSubscriber(internalPublish, clientId, subscriber.getQos(), false,
                subscriber.isRetainAsPublished(), subscriber.getSubscriptionIdentifier());
        Futures.addCallback(publishSendFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(final @Nullable PublishStatus result) {
                sendPublishFuture.set(PublishToClientResult.SUCCESSFUL);
            }

            @Override
            public void onFailure(final @NotNull Throwable t) {
                sendPublishFuture.setException(t);
            }
        }, MoreExecutors.directExecutor());


        return ListenableFutureConverter.toCompletable(sendPublishFuture, globalManagedExtensionExecutorService);
    }

    @NotNull
    private PUBLISH publishToPUBLISH(final @NotNull PublishImpl publish) {
        final byte[] payload = Bytes.getBytesFromReadOnlyBuffer(publish.getPayload());

        final byte[] correlationData = Bytes.getBytesFromReadOnlyBuffer(publish.getCorrelationData());

        final Mqtt5PayloadFormatIndicator payloadFormatIndicator = publish.getPayloadFormatIndicator().isPresent() ?
                Mqtt5PayloadFormatIndicator.from(publish.getPayloadFormatIndicator().get()) : null;

        return new PUBLISHFactory.Mqtt5Builder()
                .withHivemqId(hiveMQId.get())
                .withQoS(QoS.valueOf(publish.getQos().getQosNumber()))
                .withOnwardQos(QoS.valueOf(publish.getQos().getQosNumber()))
                .withRetain(publish.getRetain())
                .withTopic(publish.getTopic())
                .withPayload(payload)
                .withMessageExpiryInterval(publish.getMessageExpiryInterval().orElse(MESSAGE_EXPIRY_INTERVAL_NOT_SET))
                .withResponseTopic(publish.getResponseTopic().orElse(null))
                .withCorrelationData(correlationData)
                .withPayload(payload)
                .withContentType(publish.getContentType().orElse(null))
                .withPayloadFormatIndicator(payloadFormatIndicator)
                .withUserProperties(Mqtt5UserProperties.of(publish.getUserProperties().asInternalList()))
                .build();
    }
}
