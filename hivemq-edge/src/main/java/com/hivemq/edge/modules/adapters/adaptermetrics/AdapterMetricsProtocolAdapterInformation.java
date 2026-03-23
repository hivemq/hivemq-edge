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
package com.hivemq.edge.modules.adapters.adaptermetrics;

import com.hivemq.adapter.sdk.api.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.ProtocolAdapterCategory;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.ProtocolAdapterTag;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.modules.adapters.adaptermetrics.tag.AdapterMetricsTag;
import java.util.EnumSet;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AdapterMetricsProtocolAdapterInformation implements ProtocolAdapterInformation {

    public static final @NotNull String PROTOCOL_ID = "adapter-metrics";
    public static final @NotNull ProtocolAdapterInformation INSTANCE = new AdapterMetricsProtocolAdapterInformation();

    private AdapterMetricsProtocolAdapterInformation() {}

    @Override
    public @NotNull String getProtocolName() {
        return "Adapter Metrics";
    }

    @Override
    public @NotNull String getProtocolId() {
        return PROTOCOL_ID;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Adapter Metrics";
    }

    @Override
    public @NotNull String getDescription() {
        return "Samples metrics from a protocol adapter instance and publishes them as MQTT messages.";
    }

    @Override
    public @NotNull String getUrl() {
        return "https://docs.hivemq.com/hivemq-edge/protocol-adapters.html";
    }

    @Override
    public @NotNull String getVersion() {
        return "${edge-version}";
    }

    @Override
    public @NotNull String getLogoUrl() {
        return "/images/hivemq-icon.png";
    }

    @Override
    public @NotNull String getAuthor() {
        return "HiveMQ";
    }

    @Override
    public @NotNull ProtocolAdapterCategory getCategory() {
        return ProtocolAdapterCategory.TELEMETRY;
    }

    @Override
    public @Nullable List<ProtocolAdapterTag> getTags() {
        return List.of();
    }

    @Override
    public @NotNull EnumSet<ProtocolAdapterCapability> getCapabilities() {
        return EnumSet.of(ProtocolAdapterCapability.READ);
    }

    @Override
    public @Nullable String getUiSchema() {
        return null;
    }

    @Override
    public int getCurrentConfigVersion() {
        return 1;
    }

    @Override
    public @NotNull Class<? extends Tag> tagConfigurationClass() {
        return AdapterMetricsTag.class;
    }

    @Override
    public @NotNull Class<? extends ProtocolSpecificAdapterConfig> configurationClassNorthbound() {
        return AdapterMetricsSpecificAdapterConfig.class;
    }

    @Override
    public @NotNull Class<? extends ProtocolSpecificAdapterConfig> configurationClassNorthAndSouthbound() {
        return AdapterMetricsSpecificAdapterConfig.class;
    }
}
