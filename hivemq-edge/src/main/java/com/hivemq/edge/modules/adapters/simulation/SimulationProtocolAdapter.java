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
package com.hivemq.edge.modules.adapters.simulation;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.api.model.status.Status;
import com.hivemq.edge.modules.adapters.ProtocolAdapterException;
import com.hivemq.edge.modules.adapters.impl.AbstractProtocolAdapter;
import com.hivemq.edge.modules.adapters.params.NodeTree;
import com.hivemq.edge.modules.adapters.params.NodeType;
import com.hivemq.edge.modules.adapters.params.ProtocolAdapterDiscoveryInput;
import com.hivemq.edge.modules.adapters.params.ProtocolAdapterDiscoveryOutput;
import com.hivemq.edge.modules.adapters.params.ProtocolAdapterPollingOutput;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class SimulationProtocolAdapter extends AbstractProtocolAdapter<SimulationAdapterConfig> {
    private static final Logger log = LoggerFactory.getLogger(SimulationProtocolAdapter.class);

    public SimulationProtocolAdapter(
            @NotNull final ProtocolAdapterInformation adapterInformation,
            @NotNull final SimulationAdapterConfig adapterConfig,
            @NotNull final MetricRegistry metricRegistry) {
        super(adapterInformation, adapterConfig, metricRegistry);
    }

    @Override
    public ConnectionStatus getConnectionStatus() {
        return ConnectionStatus.STATELESS;
    }

    @Override
    public CompletableFuture<Void> start(
            final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
        try {
            bindServices(input.moduleServices());
            setRuntimeStatus(RuntimeStatus.STARTED);
            if (adapterConfig.getSubscriptions() != null) {
                for (SimulationAdapterConfig.Subscription subscription : adapterConfig.getSubscriptions()) {
                    subscribeInternal(subscription);
                }
            }
            output.startedSuccessfully("Successfully connected");
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            output.failStart(e, e.getMessage());
            setRuntimeStatus(RuntimeStatus.STOPPED);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<Void> stop() {
        try {
            //-- Stop polling jobs
            protocolAdapterPollingService.getPollingJobsForAdapter(getId())
                    .stream()
                    .forEach(protocolAdapterPollingService::stopPolling);
            return CompletableFuture.completedFuture(null);
        } finally {
            setRuntimeStatus(RuntimeStatus.STOPPED);
        }
    }

    private void startPolling(final @NotNull SimulationPoller poller) {
        protocolAdapterPollingService.schedulePolling(this, poller);
    }

    @Override
    public CompletableFuture<Void> close() {
        return stop();
    }

    protected void subscribeInternal(final @NotNull SimulationAdapterConfig.Subscription subscription) {
        if (subscription != null) {
            startPolling(new SimulationPoller(subscription));
        }
    }

    protected void captured(SimulationData data) throws ProtocolAdapterException {

        final ProtocolAdapterPublishBuilder publishBuilder = adapterPublishService.publish()
                .withTopic(data.getTopic())
                .withPayload(convertToJson(data.getData()))
                .withQoS(data.getQos().getQosNumber())
                .withContextInformation("polling-interval-ms", Long.toString(adapterConfig.getPollingIntervalMillis()));

        final CompletableFuture<PublishReturnCode> publishFuture = publishBuilder.send();

        publishFuture.thenAccept(publishReturnCode -> {
            protocolAdapterMetricsHelper.incrementReadPublishSuccess();
        }).exceptionally(throwable -> {
            protocolAdapterMetricsHelper.incrementReadPublishFailure();
            log.warn("Error Publishing Simulation Payload", throwable);
            return null;
        });
    }

    class SimulationPoller extends ProtocolAdapterPollingInputImpl {

        private final SimulationAdapterConfig.Subscription subscription;

        public SimulationPoller(SimulationAdapterConfig.Subscription subscription) {
            super(adapterConfig.getPollingIntervalMillis(),
                    adapterConfig.getPollingIntervalMillis(),
                    TimeUnit.MILLISECONDS,
                    1);
            this.subscription = subscription;
        }

        @Override
        public void execute() throws Exception {
            SimulationData data = createData();
            data.setData(ThreadLocalRandom.current().nextDouble());
            captured(data);
        }

        protected SimulationData createData() {
            SimulationData data = new SimulationData(SimulationData.TYPE.RANDOM,
                    subscription.getDestination(),
                    QoS.valueOf(subscription.getQos()));
            return data;
        }
    }
}
