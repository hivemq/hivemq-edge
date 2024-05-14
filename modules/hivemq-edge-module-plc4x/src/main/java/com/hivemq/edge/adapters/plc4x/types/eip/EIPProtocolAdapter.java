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
package com.hivemq.edge.adapters.plc4x.types.eip;

import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.config.AdapterSubscription;
import com.hivemq.adapter.sdk.api.data.ProtocolAdapterDataSample;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.edge.adapters.plc4x.impl.AbstractPlc4xAdapter;
import com.hivemq.edge.adapters.plc4x.impl.ProtocolAdapterDataSampleImpl;
import com.hivemq.edge.adapters.plc4x.model.Plc4xAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.apache.plc4x.java.api.messages.PlcReadResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author HiveMQ Adapter Generator
 */
public class EIPProtocolAdapter extends AbstractPlc4xAdapter<EIPAdapterConfig> {

    static final String SLOT = "slot", BACKPLANE = "backplane";

    public EIPProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<EIPAdapterConfig> input) {
        super(adapterInformation, input);
    }

    @Override
    protected @NotNull String getProtocolHandler() {
        return "eip:tcp";
    }

    @Override
    protected @NotNull ReadType getReadType() {
        return ReadType.Read;
    }

    @Override
    protected @NotNull String createTagAddressForSubscription(final Plc4xAdapterConfig.AdapterSubscriptionImpl subscription) {
        return "%" + subscription.getTagAddress();
    }

    @Override
    protected @NotNull Map<String, String> createQueryStringParams(final @NotNull EIPAdapterConfig config) {
        Map<String, String> map = new HashMap<>();
        map.put(BACKPLANE, nullSafe(config.getBackplane()));
        map.put(SLOT, nullSafe(config.getSlot()));
        map.put("bigEndian", "false");
        return map;
    }

    @Override
    public @NotNull CompletableFuture<? extends ProtocolAdapterDataSample> poll(final @NotNull AdapterSubscription adapterSubscription) {
        if (!(adapterSubscription instanceof EIPAdapterConfig.AdapterSubscription)) {
            throw new IllegalStateException("Subscription configuration is not of correct type Ethernet/IP");
        }
        if (connection.isConnected()) {
            try {
                CompletableFuture<? extends PlcReadResponse> request =
                        connection.read((Plc4xAdapterConfig.AdapterSubscriptionImpl) adapterSubscription);
                return request.thenApply(response -> (ProtocolAdapterDataSample) processReadResponse((EIPAdapterConfig.AdapterSubscription) adapterSubscription,
                        response)).exceptionally(throwable -> {
                    if (throwable instanceof InterruptedException ||
                            throwable.getCause() instanceof InterruptedException) {
                        return new ProtocolAdapterDataSampleImpl<>(adapterSubscription, adapterFactories.dataPointFactory());
                    }
                    throw new RuntimeException(throwable);
                }).thenApply(this::captureDataSample);

            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
        }
        return CompletableFuture.completedFuture(new ProtocolAdapterDataSampleImpl<>(adapterSubscription, adapterFactories.dataPointFactory()))
                .thenApply(this::captureDataSample);
    }
}
