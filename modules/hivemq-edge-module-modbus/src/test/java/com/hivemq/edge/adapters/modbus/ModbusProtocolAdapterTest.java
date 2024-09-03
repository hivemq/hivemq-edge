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

import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.edge.adapters.modbus.config.AddressRange;
import com.hivemq.edge.adapters.modbus.config.ModbusToMqttMapping;
import com.hivemq.edge.adapters.modbus.model.ModBusData;
import com.hivemq.edge.adapters.modbus.util.AdapterDataUtils;
import com.hivemq.edge.modules.adapters.data.DataPointImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModbusProtocolAdapterTest {

    @Test
    void test_deltaSamples() {

        final ModBusData data1 = createSampleData(10);
        final ModBusData data2 = createSampleData(10);

        assertEquals(0,
                AdapterDataUtils.mergeChangedSamples(data1.getDataPoints(), data2.getDataPoints()).size(),
                "There should be no deltas");
        data2.getDataPoints().set(5, new DataPointImpl("register-5", 777));

        assertEquals(1,
                AdapterDataUtils.mergeChangedSamples(data1.getDataPoints(), data2.getDataPoints()).size(),
                "There should be 1 delta");
    }

    @Test
    void test_mergedSamples() {

        final ModBusData data1 = createSampleData(10);
        final ModBusData data2 = createSampleData(10);
        data2.getDataPoints().set(5, new DataPointImpl("register-5", 777));

        AdapterDataUtils.mergeChangedSamples(data1.getDataPoints(), data2.getDataPoints());

        assertEquals(777,
                ((DataPoint) data1.getDataPoints().get(5)).getTagValue(),
                "Merged data should contain new value");
    }

    protected static ModBusData createSampleData(final int registerCount) {
        final ModbusToMqttMapping pollingContext = new ModbusToMqttMapping("topic",
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
