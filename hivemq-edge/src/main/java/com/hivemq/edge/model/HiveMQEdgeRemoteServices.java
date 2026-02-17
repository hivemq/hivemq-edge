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
package com.hivemq.edge.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;

/**
 * Defines endpoints to access for various remoteable interactions
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HiveMQEdgeRemoteServices {

    private static final @NotNull String LOCAL_ENDPOINTS_FILE = "/ext/remote-endpoints.txt";
    private static final @NotNull String FALLBACK_CONFIG_ENDPOINT =
            "https://raw.githubusercontent.com/hivemq/hivemq-edge/master/hivemq-edge/src/main/resources/hivemq-edge-configuration.json";
    private static final @NotNull String FALLBACK_USAGE_ENDPOINT = "https://analytics.hivemq.com/edge/v1";
    private static final @NotNull String DEFAULT_CONFIG_ENDPOINT;
    private static final @NotNull String DEFAULT_USAGE_ENDPOINT;

    static {
        String configEndpoint = FALLBACK_CONFIG_ENDPOINT;
        String usageEndpoint = FALLBACK_USAGE_ENDPOINT;
        try (final InputStream is = HiveMQEdgeRemoteServices.class.getResourceAsStream(LOCAL_ENDPOINTS_FILE)) {
            if (is != null) {
                final HiveMQEdgeRemoteServices localServices =
                        new ObjectMapper().readValue(is, HiveMQEdgeRemoteServices.class);
                configEndpoint = localServices.getConfigEndpoint();
                usageEndpoint = localServices.getUsageEndpoint();
            }
        } catch (final Throwable ignored) {
            // no-op
        }
        DEFAULT_CONFIG_ENDPOINT = configEndpoint;
        DEFAULT_USAGE_ENDPOINT = usageEndpoint;
    }

    private @NotNull String usageEndpoint = DEFAULT_USAGE_ENDPOINT;
    private @NotNull String configEndpoint = DEFAULT_CONFIG_ENDPOINT;

    public HiveMQEdgeRemoteServices() {
    }

    public @NotNull String getUsageEndpoint() {
        return usageEndpoint;
    }

    public void setUsageEndpoint(final @Nullable String usageEndpoint) {
        this.usageEndpoint = usageEndpoint != null ? usageEndpoint.trim() : DEFAULT_USAGE_ENDPOINT;
    }

    public @NotNull String getConfigEndpoint() {
        return configEndpoint;
    }

    public void setConfigEndpoint(final @Nullable String configEndpoint) {
        this.configEndpoint = configEndpoint != null ? configEndpoint.trim() : DEFAULT_CONFIG_ENDPOINT;
    }

    @Override
    public @NotNull String toString() {
        return "HiveMQEdgeRemoteServices{" +
                "usageEndpoint='" +
                usageEndpoint +
                "', configEndpoint='" +
                configEndpoint +
                "'}";
    }
}
