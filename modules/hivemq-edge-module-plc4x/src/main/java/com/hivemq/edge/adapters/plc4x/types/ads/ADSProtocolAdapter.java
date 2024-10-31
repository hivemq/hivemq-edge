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

import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.adapters.plc4x.config.Plc4xToMqttMapping;
import com.hivemq.edge.adapters.plc4x.config.tag.Plc4xTagDefinition;
import com.hivemq.edge.adapters.plc4x.impl.AbstractPlc4xAdapter;
import com.hivemq.edge.adapters.plc4x.types.ads.config.ADSAdapterConfig;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author HiveMQ Adapter Generator
 */
public class ADSProtocolAdapter extends AbstractPlc4xAdapter<ADSAdapterConfig, Plc4xToMqttMapping> {

    private static final @NotNull String SOURCE_AMS_NET_ID = "sourceAmsNetId";
    private static final @NotNull String SOURCE_AMS_PORT = "sourceAmsPort";
    private static final @NotNull String TARGET_AMS_PORT = "targetAmsPort";
    private static final @NotNull String TARGET_AMS_NET_ID = "targetAmsNetId";

    public ADSProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<ADSAdapterConfig> input) {
        super(adapterInformation, input);
    }

    @Override
    protected @NotNull String getProtocolHandler() {
        return "ads";
    }

    @Override
    protected @NotNull ReadType getReadType() {
        return ReadType.Read;
    }

    @Override
    protected @NotNull String createTagAddressForSubscription(final @NotNull Plc4xToMqttMapping subscription, final @NotNull Tag<Plc4xTagDefinition> tag) {
        return tag.getTagDefinition().getTagAddress();
    }

    @Override
    protected @NotNull Map<String, String> createQueryStringParams(final @NotNull ADSAdapterConfig config) {
        Map<String, String> map = new HashMap<>();
        map.put(SOURCE_AMS_PORT, nullSafe(config.getSourceAmsPort()));
        map.put(SOURCE_AMS_NET_ID, nullSafe(config.getSourceAmsNetId()));

        map.put(TARGET_AMS_PORT, nullSafe(config.getTargetAmsPort()));
        map.put(TARGET_AMS_NET_ID, nullSafe(config.getTargetAmsNetId()));
        return map;
    }
}
