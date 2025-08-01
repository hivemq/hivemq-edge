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
import com.hivemq.extensions.core.HandlerService;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.dropping.IncomingPublishDropper;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrePublishProcessorHandlingProviderTest {

    private final @NotNull HandlerService handlerService = mock();
    private final @NotNull MqttConnacker mqttConnacker = mock();
    private final @NotNull MqttServerDisconnector mqttServerDisconnector = mock();
    private final @NotNull IncomingPublishDropper incomingPublishDropper = mock();
    private final @NotNull MessageDroppedService messageDroppedService = mock();
    private final @NotNull ConfigurationService configurationService = mock();


    private final @NotNull PrePublishProcessorHandlingProvider processorHandlingProvider =
            new PrePublishProcessorHandlingProvider(handlerService,
                    mqttConnacker,
                    mqttServerDisconnector,
                    incomingPublishDropper,
                    messageDroppedService,
                    configurationService);

    @Test
    void get_calledTwoTimes_onlyOnceInitialized() {
        processorHandlingProvider.get();
        processorHandlingProvider.get();

        verify(handlerService, times(1)).getPrePublishProcessorHandlingFactories();
    }


    @Test
    void get_whenFactoriesPresent_thenCallBuildAndReturnHandlings() {
        final PrePublishProcessorHandling prePublishProcessorHandling = mock();
        final PrePublishProcessorHandlingFactory prePublishProcessorHandlingFactory =
                (mqttConnacker, mqttServerDisconnector, incomingPublishDropper, configurationService, messageDroppedService) -> prePublishProcessorHandling;
        final PrePublishProcessorHandling prePublishProcessorHandling2 = mock();
        final PrePublishProcessorHandlingFactory prePublishProcessorHandlingFactory2 =
                (mqttConnacker, mqttServerDisconnector, incomingPublishDropper, configurationService, messageDroppedService) -> prePublishProcessorHandling2;

        when(handlerService.getPrePublishProcessorHandlingFactories()).thenReturn(List.of(
                prePublishProcessorHandlingFactory,
                prePublishProcessorHandlingFactory2));

        final List<PrePublishProcessorHandling> handlings = processorHandlingProvider.get();

        assertEquals(2, handlings.size());

        assertSame(prePublishProcessorHandling, handlings.get(0));
        assertSame(prePublishProcessorHandling2, handlings.get(1));


    }
}
