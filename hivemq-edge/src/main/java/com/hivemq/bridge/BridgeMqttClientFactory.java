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
package com.hivemq.bridge;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.bridge.mqtt.BridgeInterceptorHandler;
import com.hivemq.bridge.mqtt.BridgeMqttClient;
import com.hivemq.configuration.HivemqId;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BridgeMqttClientFactory {

    private final @NotNull BridgeInterceptorHandler bridgeInterceptorHandler;
    private final @NotNull HivemqId hivemqId;
    private final @NotNull MetricRegistry metricRegistry;

    @Inject
    public BridgeMqttClientFactory(
            final @NotNull BridgeInterceptorHandler bridgeInterceptorHandler,
            final @NotNull HivemqId hivemqId,
            final @NotNull MetricRegistry metricRegistry) {
        this.bridgeInterceptorHandler = bridgeInterceptorHandler;
        this.hivemqId = hivemqId;
        this.metricRegistry = metricRegistry;
    }

    public @NotNull BridgeMqttClient createRemoteClient(final @NotNull MqttBridge bridge) {
        return new BridgeMqttClient(bridge, bridgeInterceptorHandler, hivemqId, metricRegistry);
    }
}
