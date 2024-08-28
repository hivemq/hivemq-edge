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
package com.hivemq.edge.adapters.modbus;

import com.google.common.collect.ImmutableMap;
import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterPublishService;
import com.hivemq.api.mqtt.PublishReturnCode;
import com.hivemq.edge.adapters.modbus.config.AddressRange;
import com.hivemq.edge.adapters.modbus.config.ModbusAdapterConfig;
import com.hivemq.edge.adapters.modbus.config.ModbusToMqttConfig;
import com.hivemq.edge.adapters.modbus.config.ModbusToMqttMapping;
import com.hivemq.edge.adapters.modbus.model.ModBusData;
import com.hivemq.edge.adapters.modbus.util.AdapterDataUtils;
import com.hivemq.edge.modules.adapters.data.DataPointImpl;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterPublishBuilderImpl;
import com.hivemq.mqtt.message.publish.PUBLISH;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModbusProtocolAdapterTest {

    private final @NotNull ModbusAdapterConfig adapterConfig = new ModbusAdapterConfig("adapterId",
            532,
            "my.host.com",
            1000,
            new ModbusToMqttConfig(1000, 10, true, List.of()));
    private final @NotNull ModbusProtocolAdapter adapter =
            new ModbusProtocolAdapter(ModbusProtocolAdapterInformation.INSTANCE, adapterConfig, mock());
    private final @NotNull ProtocolAdapterPublishService publishService = mock(ProtocolAdapterPublishService.class);
    private final @NotNull ModuleServices moduleServices = mock(ModuleServices.class);
    private final @NotNull ProtocolAdapterPublishBuilderImpl.SendCallback sendCallback =
            mock(ProtocolAdapterPublishBuilderImpl.SendCallback.class);
    private final @NotNull ArgumentCaptor<PUBLISH> publishArgumentCaptor = ArgumentCaptor.forClass(PUBLISH.class);

    @BeforeEach
    void setUp() {
        when(moduleServices.adapterPublishService()).thenReturn(publishService);
        when(moduleServices.eventService()).thenReturn(mock(EventService.class));
        //noinspection unchecked
        when(sendCallback.onPublishSend(publishArgumentCaptor.capture(), any(), any(ImmutableMap.class))).thenReturn(
                CompletableFuture.completedFuture(PublishReturnCode.DELIVERED));
        final ProtocolAdapterPublishBuilderImpl protocolAdapterPublishBuilder =
                new ProtocolAdapterPublishBuilderImpl("hivemq", sendCallback);
        protocolAdapterPublishBuilder.withAdapter(adapter);
        when(publishService.createPublish()).thenReturn(protocolAdapterPublishBuilder);
    }

    @Test
    void test_deltaSamples() {

        final ModBusData data1 = createSampleData(10);
        final ModBusData data2 = createSampleData(10);

        Assertions.assertEquals(0,
                AdapterDataUtils.mergeChangedSamples(data1.getDataPoints(), data2.getDataPoints()).size(),
                "There should be no deltas");
        data2.getDataPoints().set(5, new DataPointImpl("register-5", 777));

        Assertions.assertEquals(1,
                AdapterDataUtils.mergeChangedSamples(data1.getDataPoints(), data2.getDataPoints()).size(),
                "There should be 1 delta");
    }

    @Test
    void test_mergedSamples() {

        final ModBusData data1 = createSampleData(10);
        final ModBusData data2 = createSampleData(10);
        data2.getDataPoints().set(5, new DataPointImpl("register-5", 777));

        AdapterDataUtils.mergeChangedSamples(data1.getDataPoints(), data2.getDataPoints());

        Assertions.assertEquals(777,
                ((DataPoint) data1.getDataPoints().get(5)).getTagValue(),
                "Merged data should contain new value");
    }

    protected static ModBusData createSampleData(final int registerCount) {
        final PollingContext pollingContext = new ModbusToMqttMapping("topic",
                2,
                MessageHandlingOptions.MQTTMessagePerSubscription,
                true,
                false,
                List.of(),
                new AddressRange(1, 2));
        final ModBusData data = new ModBusData(pollingContext, new DataPointFactory() {
            @Override
            public @NotNull DataPoint create(final @NotNull String tagName, final @NotNull Object tagValue) {
                return new DataPointImpl(tagName, tagValue);
            }
        });
        for (int i = 0; i < registerCount; i++) {
            data.addDataPoint("register-" + i, i);
        }
        return data;
    }
}
