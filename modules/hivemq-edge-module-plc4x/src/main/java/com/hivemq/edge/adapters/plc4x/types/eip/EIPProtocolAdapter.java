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

import com.codahale.metrics.MetricRegistry;
import com.hivemq.edge.adapters.plc4x.impl.AbstractPlc4xAdapter;
import com.hivemq.edge.adapters.plc4x.model.Plc4xAdapterConfig;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author HiveMQ Adapter Generator
 */
public class EIPProtocolAdapter extends AbstractPlc4xAdapter<EIPAdapterConfig> {

    static final String
            SLOT = "slot",
            BACKPLANE = "backplane";

    public EIPProtocolAdapter(
            final ProtocolAdapterInformation adapterInformation,
            final EIPAdapterConfig adapterConfig,
            final MetricRegistry metricRegistry) {
        super(adapterInformation, adapterConfig, metricRegistry);
    }

    @Override
    protected String getProtocolHandler() {
        return "eip";
    }

    @Override
    protected ReadType getReadType() {
        return ReadType.Read;
    }

    @Override
    protected String createTagAddressForSubscription(final Plc4xAdapterConfig.Subscription subscription) {
        return subscription.getTagAddress();
    }

    @Override
    protected Map<String, String> createQueryStringParams(final @NotNull EIPAdapterConfig config) {
        Map<String, String> map = new HashMap<>();
        map.put(BACKPLANE, nullSafe(config.getBackplane()));
        map.put(SLOT, nullSafe(config.getSlot()));
        return map;
    }
}
