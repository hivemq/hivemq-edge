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
package com.hivemq.edge.adapters.plc4x.types.ads.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.edge.adapters.plc4x.config.Plc4XSpecificAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;


public class ADSSpecificAdapterConfig extends Plc4XSpecificAdapterConfig<ADSToMqttConfig> {

    private static final int PORT_MIN = 1;
    private static final int PORT_MAX = 65535;

    @JsonProperty(value = "port", required = true)
    @ModuleConfigField(title = "Port",
                       description = "The port number on the device to connect to",
                       required = true,
                       numberMin = PORT_MIN,
                       numberMax = PORT_MAX,
                       defaultValue = "48898")
    private final int port;

    @JsonProperty(value = "targetAmsPort", required = true)
    @ModuleConfigField(title = "Target AMS Port",
                       description = "The AMS port number on the device to connect to",
                       required = true,
                       numberMin = PORT_MIN,
                       numberMax = PORT_MAX,
                       defaultValue = "851")
    private final int targetAmsPort;

    @JsonProperty(value = "sourceAmsPort", required = true)
    @ModuleConfigField(title = "Source AMS Port",
                       description = "The local AMS port number used by HiveMQ Edge",
                       required = true,
                       numberMin = PORT_MIN,
                       numberMax = PORT_MAX,
                       defaultValue = "48898")
    private final int sourceAmsPort;

    @JsonProperty(value = "sourceAmsNetId", required = true)
    @ModuleConfigField(title = "Source Ams Net Id",
                       required = true,
                       stringPattern = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}",
                       description = "The AMS Net ID used by HiveMQ Edge")
    private final @NotNull String sourceAmsNetId;

    @JsonProperty(value = "targetAmsNetId", required = true)
    @ModuleConfigField(title = "Target Ams Net Id",
                       required = true,
                       stringPattern = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}",
                       description = "The AMS Net ID of the device to connect to")
    private final @NotNull String targetAmsNetId;

    @JsonProperty(value = "adsToMqtt", required = true)
    @ModuleConfigField(title = "ADS To MQTT Config",
                       description = "The configuration for a data stream from ADS to MQTT",
                       required = true)
    private final @Nullable ADSToMqttConfig adsToMqttConfig;

    @JsonCreator
    public ADSSpecificAdapterConfig(
            @JsonProperty(value = "port", required = true) final int port,
            @JsonProperty(value = "host", required = true) final @NotNull String host,
            @JsonProperty(value = "targetAmsPort", required = true) final int targetAmsPort,
            @JsonProperty(value = "sourceAmsPort", required = true) final int sourceAmsPort,
            @JsonProperty(value = "targetAmsNetId", required = true) final @NotNull String targetAmsNetId,
            @JsonProperty(value = "sourceAmsNetId", required = true) final @NotNull String sourceAmsNetId,
            @JsonProperty(value = "adsToMqtt") final @Nullable ADSToMqttConfig adsToMqttConfig) {
        super( port, host);
        this.port = port;
        this.targetAmsPort = targetAmsPort;
        this.sourceAmsPort = sourceAmsPort;
        this.sourceAmsNetId = sourceAmsNetId;
        this.targetAmsNetId = targetAmsNetId;
        if (adsToMqttConfig == null) {
            this.adsToMqttConfig = new ADSToMqttConfig(null, null, null);
        } else {
            this.adsToMqttConfig = adsToMqttConfig;
        }

    }

    @Override
    public int getPort() {
        return port;
    }

    public int getSourceAmsPort() {
        return sourceAmsPort;
    }

    public int getTargetAmsPort() {
        return targetAmsPort;
    }

    public @NotNull String getSourceAmsNetId() {
        return sourceAmsNetId;
    }

    public @NotNull String getTargetAmsNetId() {
        return targetAmsNetId;
    }

    @Override
    public @Nullable ADSToMqttConfig getPlc4xToMqttConfig() {
        return adsToMqttConfig;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final ADSSpecificAdapterConfig that = (ADSSpecificAdapterConfig) o;
        return getPort() == that.getPort() &&
                getTargetAmsPort() == that.getTargetAmsPort() &&
                getSourceAmsPort() == that.getSourceAmsPort() &&
                Objects.equals(getSourceAmsNetId(), that.getSourceAmsNetId()) &&
                Objects.equals(getTargetAmsNetId(), that.getTargetAmsNetId()) &&
                Objects.equals(adsToMqttConfig, that.adsToMqttConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPort(),
                getTargetAmsPort(),
                getSourceAmsPort(),
                getSourceAmsNetId(),
                getTargetAmsNetId(),
                adsToMqttConfig);
    }
}
