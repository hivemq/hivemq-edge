/*
 * Copyright 2023-present HiveMQ GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.hivemq.edge.adapters.file.config.legacy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


@SuppressWarnings({"unused", "FieldCanBeLocal", "FieldMayBeFinal"})
public class LegacyFileAdapterConfig {

    @JsonProperty(value = "id", required = true)
    protected @NotNull String id;

    @JsonProperty("pollingIntervalMillis")
    private int pollingIntervalMillis = 1000;

    @JsonProperty("maxPollingErrorsBeforeRemoval")
    private int maxPollingErrorsBeforeRemoval = 10;

    @JsonProperty("subscriptions")
    @ModuleConfigField(title = "subscription", description = "Map your file content to an MQTT Topic")
    private @NotNull List<LegacyFilePollingContext> pollingContexts = new ArrayList<>();

    public @NotNull String getId() {
        return id;
    }

    public int getPollingIntervalMillis() {
        return pollingIntervalMillis;
    }

    public int getMaxPollingErrorsBeforeRemoval() {
        return maxPollingErrorsBeforeRemoval;
    }

    public @NotNull List<LegacyFilePollingContext> getPollingContexts() {
        return pollingContexts;
    }
}
