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
package com.hivemq.edge.modules.adapters.simulation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.edge.HiveMQEdgeConstants;
import com.hivemq.edge.modules.adapters.annotations.ModuleConfigField;
import com.hivemq.edge.modules.config.CustomConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SimulationAdapterConfig implements CustomConfig {

    @JsonProperty(value = "id", required = true)
    @ModuleConfigField(title = "Identifier",
                       description = "Unique identifier for this protocol adapter",
                       format = ModuleConfigField.FieldType.IDENTIFIER,
                       required = true,
                       stringPattern = HiveMQEdgeConstants.ID_REGEX,
                       stringMinLength = 1,
                       stringMaxLength = 1024)
    private @NotNull String id;

    @JsonProperty(value = "pollingIntervalMillis")
    @ModuleConfigField(title = "Polling interval [ms]",
                       description = "Interval in milliseconds to poll for changes",
                       defaultValue = "10000",
                       numberMin = 100,
                       numberMax = 86400000) //24h
    private int pollingIntervalMillis = 10000;

    @JsonProperty("subscriptions")
    @ModuleConfigField(title = "Subscriptions",
                       description = "List of subscriptions for the simulation",
                       required = true)
    private @NotNull List<Subscription> subscriptions = new ArrayList<>();

    public SimulationAdapterConfig() {
    }

    public SimulationAdapterConfig(
            final @NotNull String id,
            final @NotNull List<Subscription> subscriptions) {
        this.id = id;
        this.subscriptions = subscriptions;
    }

    public @NotNull String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getPollingIntervalMillis() {
        return pollingIntervalMillis;
    }

    public void setPollingIntervalMillis(int pollingIntervalMillis) {
        this.pollingIntervalMillis = pollingIntervalMillis;
    }

    public void setSubscriptions(List<Subscription> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public @NotNull List<Subscription> getSubscriptions() {
        return subscriptions;
    }

    public static class Subscription {

        @JsonProperty(value = "destination", required = true)
        @ModuleConfigField(title = "Destination Topic",
                           description = "The topic to publish data on",
                           required = true,
                           format = ModuleConfigField.FieldType.MQTT_TOPIC)
        private @Nullable String destination;

        @JsonProperty(value = "qos", required = true)
        @ModuleConfigField(title = "QoS",
                           description = "MQTT quality of service level",
                           required = true,
                           numberMin = 0,
                           numberMax = 2,
                           defaultValue = "0")
        private int qos = 0;

        public Subscription() {
        }

        @JsonCreator
        public Subscription(
                @JsonProperty("destination") @Nullable final String destination,
                @JsonProperty("qos") final int qos) {
            this.destination = destination;
            this.qos = qos;
        }

        public String getDestination() {
            return destination;
        }

        public int getQos() {
            return qos;
        }
    }
}
