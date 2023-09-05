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
package com.hivemq.edge.adapters.http;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.edge.adapters.http.impl.HttpConnectorImpl;
import com.hivemq.edge.adapters.http.model.HttpData;
import com.hivemq.edge.modules.adapters.ProtocolAdapterException;
import com.hivemq.edge.modules.adapters.impl.AbstractProtocolAdapter;
import com.hivemq.edge.modules.adapters.params.NodeTree;
import com.hivemq.edge.modules.adapters.params.ProtocolAdapterDiscoveryInput;
import com.hivemq.edge.modules.adapters.params.ProtocolAdapterDiscoveryOutput;
import com.hivemq.edge.modules.adapters.params.ProtocolAdapterStartInput;
import com.hivemq.edge.modules.adapters.params.ProtocolAdapterStartOutput;
import com.hivemq.edge.modules.adapters.params.impl.ProtocolAdapterPollingInputImpl;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPublishBuilder;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.handler.publish.PublishReturnCode;
import com.hivemq.mqtt.message.QoS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author HiveMQ Adapter Generator
 */
public class HttpProtocolAdapter extends AbstractProtocolAdapter<HttpAdapterConfig> {

    private static final Logger log = LoggerFactory.getLogger(HttpProtocolAdapter.class);
    private final @NotNull Object lock = new Object();
    private volatile @Nullable IHttpClient client;
    private @Nullable Map<HttpData.TYPE, HttpData> lastSamples = new HashMap<>();

    public HttpProtocolAdapter(final @NotNull ProtocolAdapterInformation adapterInformation,
                             final @NotNull HttpAdapterConfig adapterConfig,
                             final @NotNull MetricRegistry metricRegistry) {
        super(adapterInformation, adapterConfig, metricRegistry);
    }

    @Override
    public CompletableFuture<Void> start(
            final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
        try {
            bindServices(input.moduleServices());
            initStartAttempt();
            if (client == null) {
                createClient();
            }

//            if (adapterConfig.getSubscriptions() != null) {
//                for (HttpAdapterConfig.Subscription subscription : adapterConfig.getSubscriptions()) {
//                    subscribeInternal(subscription);
//                }
//            }
            output.startedSuccessfully("Successfully connected");
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            output.failStart(e, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    private IHttpClient createClient() {
        if (client == null) {
            synchronized (lock) {
                if (client == null) {
                    log.info("Creating new Instance Of Http Connector with {}", adapterConfig);
                    client = new HttpConnectorImpl(adapterConfig);
                }
            }
        }
        return client;
    }

    @Override
    public CompletableFuture<Void> stop() {
        if (client != null) {
            try {
                //-- Stop polling jobs
                protocolAdapterPollingService.getPollingJobsForAdapter(getId()).stream().forEach(
                        protocolAdapterPollingService::stopPolling);
                //-- Disconnect client
                client.disconnect();
            } catch (ProtocolAdapterException e) {
                log.error("Error disconnecting from Http Client", e);
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    private void startPolling(final @NotNull Poller poller) {
        protocolAdapterPollingService.schedulePolling(this, poller);
    }

    @Override
    public CompletableFuture<Void> close() {
        return stop();
    }


    protected void subscribeInternal(final @NotNull HttpAdapterConfig.Subscription subscription) {
        if (subscription != null) {
            startPolling(new Poller(null, subscription));
        }
    }

    @Override
    public CompletableFuture<Void> discoverValues(final @NotNull ProtocolAdapterDiscoveryInput input, final @NotNull ProtocolAdapterDiscoveryOutput output) {
        //-- Do the discovery of registers and coils, only for root level
        final NodeTree nodeTree = output.getNodeTree();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public @NotNull Status status() {
        return client != null && client.isConnected() ? Status.CONNECTED : Status.DISCONNECTED;
    }

    protected void captured(final @NotNull HttpData data) throws ProtocolAdapterException {
        boolean publishData = true;
        if (adapterConfig.getPublishChangedDataOnly()) {
            HttpData previousSample = lastSamples.put(data.getType(), data);
            if (previousSample != null) {
                byte[] sample = previousSample.getData();
                publishData = !Objects.deepEquals(data.getData(), sample);
            }
        }
        if (publishData) {

            final ProtocolAdapterPublishBuilder publishBuilder = adapterPublishService.publish()
                    .withTopic(data.getTopic())
                    .withPayload(convertToJson(data.getData()))
                    .withQoS(data.getQos().getQosNumber());

            final CompletableFuture<PublishReturnCode> publishFuture = publishBuilder.send();

            publishFuture.thenAccept(publishReturnCode -> {
                protocolAdapterMetricsHelper.incrementReadPublishSuccess();
            }).exceptionally(throwable -> {
                protocolAdapterMetricsHelper.incrementReadPublishFailure();
                log.warn("Error Publishing Http Payload", throwable);
                return null;
            });
        }
    }


    class Poller extends ProtocolAdapterPollingInputImpl {

        private final HttpData.TYPE type;
        private final HttpAdapterConfig.Subscription subscription;

        public Poller(final @NotNull HttpData.TYPE type, final @NotNull HttpAdapterConfig.Subscription subscription) {
            super(adapterConfig.getPublishingInterval(),
                    adapterConfig.getPublishingInterval(),
                    TimeUnit.MILLISECONDS,
                    adapterConfig.getMaxPollingErrorsBeforeRemoval());
            this.type = type;
            this.subscription = subscription;
        }

        public HttpData.TYPE getType() {
            return type;
        }

        @Override
        public void execute() throws Exception {
            if (!client.isConnected()) {
                client.connect();
            }
            HttpData data = readTag();
            if (data != null) {
                captured(data);
            }
        }

        protected HttpData createData() {
            HttpData data = new HttpData(type,
                    subscription.getDestination(),
                    QoS.valueOf(subscription.getQos()));
            return data;
        }

        protected HttpData readTag() throws ProtocolAdapterException {
            //TODO complete me
            return createData();
        }
    }

}
