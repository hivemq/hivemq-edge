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
package com.hivemq.edge.modules.adapters.telemetry;

import com.hivemq.adapter.sdk.api.ProtocolAdapterCategory;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.ProtocolAdapterTag;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import com.hivemq.adapter.sdk.api.tag.Tag;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EdgeTelemetryAdapterInformation implements ProtocolAdapterInformation {

    public static final @NotNull ProtocolAdapterInformation INSTANCE = new EdgeTelemetryAdapterInformation();

    private static final @NotNull Logger log = LoggerFactory.getLogger(EdgeTelemetryAdapterInformation.class);

    private EdgeTelemetryAdapterInformation() {}

    @Override
    public @NotNull String getProtocolName() {
        return "Edge Telemetry";
    }

    @Override
    public @NotNull String getProtocolId() {
        return "edge-telemetry";
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Edge Telemetry Adapter";
    }

    @Override
    public @NotNull String getDescription() {
        return "Streams internal Edge metrics as MQTT messages. Each tag subscribes to a topic filter and publishes message counts periodically.";
    }

    @Override
    public @NotNull String getUrl() {
        return "https://docs.hivemq.com/hivemq-edge/protocol-adapters.html#edge-telemetry";
    }

    @Override
    public @NotNull List<ProtocolAdapterTag> getTags() {
        return List.of();
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
        return ProtocolAdapterCategory.SIMULATION;
    }

    @Override
    public @Nullable String getUiSchema() {
        try (final InputStream is =
                this.getClass().getClassLoader().getResourceAsStream("edge-telemetry-adapter-ui-schema.json")) {
            if (is == null) {
                log.warn("The UISchema for the Edge Telemetry Adapter could not be loaded from resources: Not found.");
                return null;
            }
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (final Exception e) {
            log.warn("The UISchema for the Edge Telemetry Adapter could not be loaded from resources:", e);
            return null;
        }
    }

    @Override
    public @NotNull Class<? extends Tag> tagConfigurationClass() {
        return EdgeTelemetryTag.class;
    }

    @Override
    public @NotNull Class<? extends ProtocolSpecificAdapterConfig> configurationClassNorthbound() {
        return EdgeTelemetryAdapterConfig.class;
    }

    @Override
    public @NotNull Class<? extends ProtocolSpecificAdapterConfig> configurationClassNorthAndSouthbound() {
        return EdgeTelemetryAdapterConfig.class;
    }

    @Override
    public int getCurrentConfigVersion() {
        return 1;
    }
}
