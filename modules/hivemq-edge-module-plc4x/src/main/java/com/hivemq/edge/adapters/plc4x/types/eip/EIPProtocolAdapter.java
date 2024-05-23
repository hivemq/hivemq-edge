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
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.data.ProtocolAdapterDataSample;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.polling.PollingInput;
import com.hivemq.adapter.sdk.api.polling.PollingOutput;
import com.hivemq.edge.adapters.plc4x.impl.AbstractPlc4xAdapter;
import com.hivemq.edge.adapters.plc4x.model.Plc4xAdapterConfig;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

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
    protected @NotNull String createTagAddressForSubscription(final Plc4xAdapterConfig.PollingContextImpl subscription) {
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
    public void poll(
            final @NotNull PollingInput pollingInput, final @NotNull PollingOutput pollingOutput) {
        final PollingContext pollingContext = pollingInput.getPollingContext();
        if (!(pollingContext instanceof EIPAdapterConfig.EIPPollingContextImpl)) {
            pollingOutput.fail( "Subscription configuration is not of correct type Ethernet/IP");
            return;
        }
        if (connection.isConnected()) {
            connection.read((Plc4xAdapterConfig.PollingContextImpl) pollingContext)
                    .thenApply(response -> (ProtocolAdapterDataSample) processReadResponse((EIPAdapterConfig.EIPPollingContextImpl) pollingContext,
                            response))
                    .thenApply(data -> captureDataSample(data, pollingContext))
                    .whenComplete((sample, t) -> handleDataAndExceptions(sample, t, pollingOutput));
        } else {
            pollingOutput.fail( "EIP Adapter is not connected.");
        }
    }
}
