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
package com.hivemq.configuration.service.impl;

import com.google.common.collect.ImmutableList;
import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.configuration.service.BridgeConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BridgeConfigurationServiceImpl implements BridgeConfigurationService {

    private final @NotNull List<MqttBridge> mqttBridges = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void addBridge(final @NotNull MqttBridge mqttBridge) {
        mqttBridges.add(mqttBridge);
    }

    @Override
    public @NotNull List<MqttBridge> getBridges() {
        synchronized (mqttBridges){
            return new ImmutableList.Builder().addAll(mqttBridges).build();
        }
    }

    @Override
    public boolean removeBridge(@NotNull final String id) {
        synchronized (mqttBridges){
            return mqttBridges.removeIf(mqttBridge -> mqttBridge.getId().equals(id));
        }
    }
}
