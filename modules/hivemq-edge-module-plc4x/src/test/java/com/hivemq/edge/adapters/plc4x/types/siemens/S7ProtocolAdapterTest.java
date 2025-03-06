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
package com.hivemq.edge.adapters.plc4x.types.siemens;

import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.edge.adapters.plc4x.config.Plc4xDataType;
import com.hivemq.edge.adapters.plc4x.config.Plc4xToMqttMapping;
import com.hivemq.edge.adapters.plc4x.config.tag.Plc4xTag;
import com.hivemq.edge.adapters.plc4x.config.tag.Plc4xTagDefinition;
import com.hivemq.edge.modules.adapters.data.DataPointImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class S7ProtocolAdapterTest {

    private final @NotNull ProtocolAdapterInput protocolAdapterInput = mock();
    private final @NotNull ModuleServices moduleServices = mock();
    private @NotNull TestS7ProtocolAdapter adapter;

    @BeforeEach
    void setUp() {

        var adapterFactories = mock(AdapterFactories.class);
        when(adapterFactories.dataPointFactory()).thenReturn(new DataPointFactory() {
            @Override
            public @NotNull DataPoint create(final @NotNull String tagName, final @NotNull Object tagValue) {
                return new DataPointImpl(tagName, tagValue);
            }

            @Override
            public @NotNull DataPoint createJsonDataPoint(
                    final @NotNull String tagName,
                    final @NotNull Object tagValue) {
                return new DataPointImpl(tagName, tagValue);
            }
        });
        when(protocolAdapterInput.moduleServices()).thenReturn(moduleServices);
        when(protocolAdapterInput.adapterFactories()).thenReturn(adapterFactories);
        adapter = new TestS7ProtocolAdapter(protocolAdapterInput);
    }

    @Test
    public void whenTagSpecialDataType_thenModifyAddress() {
        final Plc4xTag tag = new Plc4xTag("tag", "not set",
                new Plc4xTagDefinition("%IW200", Plc4xDataType.DATA_TYPE.DATE));
        assertEquals("%IX200:DATE",
                adapter.createTagAddressForSubscription(tag));
    }

    @Test
    public void whenTagNormalDataType_thenDoNotModifyAddress() {
        final Plc4xTag tag = new Plc4xTag("tag", "not set",
                new Plc4xTagDefinition("%IW200", Plc4xDataType.DATA_TYPE.WORD));
        assertEquals("%IW200:WORD",
                adapter.createTagAddressForSubscription(tag));
    }

    @Test
    public void whenTagBitDataType_thenDoNotModifyAddress() {
        final Plc4xTag tag = new Plc4xTag("tag", "not set",
                new Plc4xTagDefinition("%IX200.2", Plc4xDataType.DATA_TYPE.BOOL));
        assertEquals("%IX200.2:BOOL",
                adapter.createTagAddressForSubscription(tag));
    }

    @Test
    public void whenBlockSpecialDataType_thenModifyAddress() {
        final Plc4xTag tag = new Plc4xTag("tag", "not set",
                new Plc4xTagDefinition("%DB23.DBW200", Plc4xDataType.DATA_TYPE.WCHAR));
        assertEquals("%DB23.DBX200:WCHAR",
                adapter.createTagAddressForSubscription(tag));
    }

    @Test
    public void whenBlockNormalDataType_thenDoNotModifyAddress() {
        final Plc4xTag tag = new Plc4xTag("tag", "not set",
                new Plc4xTagDefinition("%DB23.DBD200", Plc4xDataType.DATA_TYPE.DINT));
        assertEquals("%DB23.DBD200:DINT",
                adapter.createTagAddressForSubscription(tag));
    }

    @Test
    public void whenBlockShortSpecialDataType_thenModifyAddress() {
        final Plc4xTag tag = new Plc4xTag("tag", "not set",
                new Plc4xTagDefinition("%DB23:200", Plc4xDataType.DATA_TYPE.DATE));
        assertEquals("%DB23:200:DATE",
                adapter.createTagAddressForSubscription(tag));
    }

    @Test
    public void whenBlockBitDataType_thenDoNotModifyAddress() {
        final Plc4xTag tag = new Plc4xTag("tag", "not set",
                new Plc4xTagDefinition("%DB100:DBX200.2", Plc4xDataType.DATA_TYPE.BOOL));
        assertEquals("%DB100:DBX200.2:BOOL",
                adapter.createTagAddressForSubscription(tag));
    }

    @Test
    public void whenBlockShortNormalDataType_thenDoNotModifyAddress() {
        final Plc4xTag tag = new Plc4xTag("tag", "not set",
                new Plc4xTagDefinition("%DB23:200", Plc4xDataType.DATA_TYPE.DINT));
        assertEquals("%DB23:200:DINT",
                adapter.createTagAddressForSubscription(tag));
    }

    @Test
    public void whenBlockShortBitDataType_thenDoNotModifyAddress() {
        final Plc4xTag tag = new Plc4xTag("tag", "not set",
                new Plc4xTagDefinition("%DB100:200.2", Plc4xDataType.DATA_TYPE.BOOL));
        assertEquals("%DB100:200.2:BOOL",
                adapter.createTagAddressForSubscription(tag));
    }

    private static class TestS7ProtocolAdapter extends S7ProtocolAdapter {

        public TestS7ProtocolAdapter(final @NotNull ProtocolAdapterInput protocolAdapterInput) {
            super(S7ProtocolAdapterInformation.INSTANCE, protocolAdapterInput);
        }

        @Override
        public @NotNull String createTagAddressForSubscription(final @NotNull Plc4xTag tag) {
            return super.createTagAddressForSubscription(tag);
        }
    }

    private static class S7TestSub extends Plc4xToMqttMapping {

        public S7TestSub() {
            super("mqttTopic", 1, MessageHandlingOptions.MQTTMessagePerTag, true, true, "tag", List.of());
        }
    }
}
