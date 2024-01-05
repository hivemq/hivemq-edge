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
package com.hivemq.edge.adapters.plc4x.types.ads;

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
public class ADSProtocolAdapter extends AbstractPlc4xAdapter<ADSAdapterConfig> {

    static final String
            SOURCE_AMS_NET_ID = "sourceAmsNetId",
            SOURCE_AMS_PORT = "sourceAmsPort",
            TARGET_AMS_PORT = "targetAmsPort",
            TARGET_AMS_NET_ID = "targetAmsNetId";

    public ADSProtocolAdapter(
            final ProtocolAdapterInformation adapterInformation,
            final ADSAdapterConfig adapterConfig,
            final MetricRegistry metricRegistry) {
        super(adapterInformation, adapterConfig, metricRegistry);
    }

    @Override
    protected String getProtocolHandler() {
        return "ads";
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
    protected Map<String, String> createQueryStringParams(final @NotNull ADSAdapterConfig config) {
        Map<String, String> map = new HashMap<>();
        map.put(SOURCE_AMS_PORT, nullSafe(config.getSourceAmsPort()));
        map.put(SOURCE_AMS_NET_ID, nullSafe(config.getSourceAmsNetId()));

        map.put(TARGET_AMS_PORT, nullSafe(config.getTargetAmsPort()));
        map.put(TARGET_AMS_NET_ID, nullSafe(config.getTargetAmsNetId()));
        return map;
    }
}
