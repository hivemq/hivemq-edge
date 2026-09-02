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
package com.hivemq.edge.adapters.file.v2;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import org.jetbrains.annotations.NotNull;

/**
 * The v2 File adapter's instance configuration. The File adapter has no adapter-level settings — the polling
 * cadence lives on each tag ({@code poll-interval-millis}) and the MQTT envelope (topic, QoS, timestamp,
 * user-properties) is owned by the framework's northbound mappings — so this configuration carries nothing. It is
 * parsed tolerantly so that an absent, empty, or additionally-populated configuration section is accepted without
 * error.
 */
public record FileAdapterConfiguration() {

    /**
     * Parse the adapter's instance configuration. The File adapter has no settings, so any configuration value is
     * accepted and ignored.
     *
     * @param adapterConfig the reused v1 configuration value handed to the adapter (ignored).
     * @return the (empty) File adapter configuration.
     */
    public static @NotNull FileAdapterConfiguration parse(final @NotNull DataPoint adapterConfig) {
        return new FileAdapterConfiguration();
    }
}
