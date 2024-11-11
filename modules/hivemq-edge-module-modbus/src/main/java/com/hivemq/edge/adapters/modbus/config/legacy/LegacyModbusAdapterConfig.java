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
package com.hivemq.edge.adapters.modbus.config.legacy;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import com.hivemq.adapter.sdk.api.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;


@SuppressWarnings("FieldCanBeLocal")
public class LegacyModbusAdapterConfig implements ProtocolAdapterConfig {

    @JsonProperty(value = "id", required = true)
    private final @NotNull String id;

    @JsonProperty("pollingIntervalMillis")
    @JsonAlias(value = "publishingInterval") //-- Ensure we cater for properties created with legacy configuration
    private final int pollingIntervalMillis;

    @JsonProperty("maxPollingErrorsBeforeRemoval")
    private final int maxPollingErrorsBeforeRemoval;

    @JsonProperty("port")
    private final int port;

    @JsonProperty("host")
    private final @NotNull String host;

    @JsonProperty("timeout")
    private final int timeout;

    @JsonProperty("publishChangedDataOnly")
    private final boolean publishChangedDataOnly;

    @JsonProperty("subscriptions")
    private final @NotNull List<LegacyModbusPollingContext> subscriptions;

    @JsonCreator
    public LegacyModbusAdapterConfig(
            @JsonProperty(value = "id", required = true) final @NotNull String id,
            @JsonProperty(value = "pollingIntervalMillis") final @Nullable Integer pollingIntervalMillis,
            @JsonProperty(value = "maxPollingErrorsBeforeRemoval") final @Nullable Integer maxPollingErrorsBeforeRemoval,
            @JsonProperty(value = "port", required = true) final int port,
            @JsonProperty(value = "host", required = true) final @NotNull String host,
            @JsonProperty(value = "timeout") final @Nullable Integer timeout,
            @JsonProperty(value = "publishChangedDataOnly") final @Nullable Boolean publishChangedDataOnly,
            @JsonProperty(value = "subscriptions") final @Nullable List<LegacyModbusPollingContext> subscriptions
    ) {
        this.id = id;
        this.pollingIntervalMillis = Objects.requireNonNullElse(pollingIntervalMillis, 1000);
        this.maxPollingErrorsBeforeRemoval = Objects.requireNonNullElse(maxPollingErrorsBeforeRemoval, 10);
        this.port = port;
        this.host = host;
        this.timeout = Objects.requireNonNullElse(timeout, 5000);
        this.publishChangedDataOnly = Objects.requireNonNullElse(publishChangedDataOnly, true);
        this.subscriptions = Objects.requireNonNullElse(subscriptions, List.of());
    }

    public @NotNull String getId() {
        return id;
    }

    @Override
    public @NotNull Set<String> calculateAllUsedTags() {
        return Set.of();
    }

    public int getPollingIntervalMillis() {
        return pollingIntervalMillis;
    }

    public int getMaxPollingErrorsBeforeRemoval() {
        return maxPollingErrorsBeforeRemoval;
    }

    public boolean getPublishChangedDataOnly() {
        return publishChangedDataOnly;
    }

    public int getPort() {
        return port;
    }

    public @NotNull String getHost() {
        return host;
    }

    public @NotNull List<LegacyModbusPollingContext> getSubscriptions() {
        return subscriptions;
    }

    public int getTimeout() {
        return timeout;
    }
}
