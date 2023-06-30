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
package com.hivemq.configuration.service;

import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;

/**
 * A Configuration service which allows to get information about the current MQTT-bridges configuration
 * and allows to change the global MQTT-bridge configuration at runtime.
 */
public interface BridgeConfigurationService {

    void addBridge(final @NotNull MqttBridge mqttBridge);

    @NotNull List<MqttBridge> getBridges();

    /**
     * @param id identifier of the bridge to remove
     * @return {@code true} if any bridges were removed
     */
    boolean removeBridge(@NotNull String id);
}
