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
package com.hivemq.bootstrap.factories;

import com.hivemq.configuration.service.ConfigurationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.hivemq.extensions.core.HandlerService;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.dropping.IncomingPublishDropper;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;

import javax.inject.Inject;

public class AdapterHandlingProvider {

    private final @NotNull HandlerService handlerService;
    private final @NotNull MqttConnacker mqttConnacker;
    private final @NotNull MqttServerDisconnector mqttServerDisconnector;
    private final @NotNull IncomingPublishDropper incomingPublishDropper;
    private final @NotNull MessageDroppedService messageDroppedService;
    private final @NotNull ConfigurationService configurationService;

    @Inject
    public AdapterHandlingProvider(
            final @NotNull HandlerService handlerService,
            final @NotNull MqttConnacker mqttConnacker,
            final @NotNull MqttServerDisconnector mqttServerDisconnector,
            final @NotNull IncomingPublishDropper incomingPublishDropper,
            final @NotNull MessageDroppedService messageDroppedService,
            final @NotNull ConfigurationService configurationService) {
        this.handlerService = handlerService;
        this.mqttConnacker = mqttConnacker;
        this.mqttServerDisconnector = mqttServerDisconnector;
        this.incomingPublishDropper = incomingPublishDropper;
        this.messageDroppedService = messageDroppedService;
        this.configurationService = configurationService;
    }

    public @Nullable AdapterHandling get() {
        final AdapterHandlingFactory adapterHandlingFactory = handlerService.getAdapterHandlerFactory();
        if (adapterHandlingFactory == null) {
            return null;
        }
        return adapterHandlingFactory.build(mqttConnacker,
                mqttServerDisconnector,
                incomingPublishDropper,
                configurationService,
                messageDroppedService);
    }
}
