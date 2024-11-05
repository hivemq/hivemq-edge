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
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterTagService;
import com.hivemq.edge.adapters.plc4x.config.Plc4xDataType;
import com.hivemq.edge.adapters.plc4x.config.Plc4xToMqttMapping;
import com.hivemq.edge.adapters.plc4x.config.tag.Plc4xTag;
import com.hivemq.edge.adapters.plc4x.config.tag.Plc4xTagDefinition;
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
    private final @NotNull ProtocolAdapterTagService protocolAdapterTagService = mock();
    private @NotNull TestS7ProtocolAdapter adapter;

    @BeforeEach
    void setUp() {
        when(protocolAdapterInput.moduleServices()).thenReturn(moduleServices);
        when(moduleServices.protocolAdapterTagService()).thenReturn(protocolAdapterTagService);
        adapter = new TestS7ProtocolAdapter(protocolAdapterInput);
    }

    @Test
    public void whenTagSpecialDataType_thenModifyAddress() {
        final Plc4xTag tag = new Plc4xTag("tag", "not set", new Plc4xTagDefinition("%IW200"));
        assertEquals("%IX200:DATE",
                adapter.createTagAddressForSubscription(new S7TestSub(Plc4xDataType.DATA_TYPE.DATE), tag));
    }

    @Test
    public void whenTagNormalDataType_thenDoNotModifyAddress() {
        final Plc4xTag tag = new Plc4xTag("tag", "not set", new Plc4xTagDefinition("%IW200"));
        assertEquals("%IW200:WORD",
                adapter.createTagAddressForSubscription(new S7TestSub(Plc4xDataType.DATA_TYPE.WORD), tag));
    }

    @Test
    public void whenTagBitDataType_thenDoNotModifyAddress() {
        final Plc4xTag tag = new Plc4xTag("tag", "not set", new Plc4xTagDefinition("%IX200.2"));
        assertEquals("%IX200.2:BOOL",
                adapter.createTagAddressForSubscription(new S7TestSub(Plc4xDataType.DATA_TYPE.BOOL), tag));
    }

    @Test
    public void whenBlockSpecialDataType_thenModifyAddress() {
        final Plc4xTag tag = new Plc4xTag("tag", "not set", new Plc4xTagDefinition("%DB23.DBW200"));
        assertEquals("%DB23.DBX200:WCHAR",
                adapter.createTagAddressForSubscription(new S7TestSub(Plc4xDataType.DATA_TYPE.WCHAR), tag));
    }

    @Test
    public void whenBlockNormalDataType_thenDoNotModifyAddress() {
        final Plc4xTag tag = new Plc4xTag("tag", "not set", new Plc4xTagDefinition("%DB23.DBD200"));
        assertEquals("%DB23.DBD200:DINT",
                adapter.createTagAddressForSubscription(new S7TestSub(Plc4xDataType.DATA_TYPE.DINT), tag));
    }

    @Test
    public void whenBlockShortSpecialDataType_thenModifyAddress() {
        final Plc4xTag tag = new Plc4xTag("tag", "not set", new Plc4xTagDefinition("%DB23:200"));
        assertEquals("%DB23:200:DATE",
                adapter.createTagAddressForSubscription(new S7TestSub(Plc4xDataType.DATA_TYPE.DATE), tag));
    }

    @Test
    public void whenBlockBitDataType_thenDoNotModifyAddress() {
        final Plc4xTag tag = new Plc4xTag("tag", "not set", new Plc4xTagDefinition("%DB100:DBX200.2"));
        assertEquals("%DB100:DBX200.2:BOOL",
                adapter.createTagAddressForSubscription(new S7TestSub(Plc4xDataType.DATA_TYPE.BOOL), tag));
    }

    @Test
    public void whenBlockShortNormalDataType_thenDoNotModifyAddress() {
        final Plc4xTag tag = new Plc4xTag("tag", "not set", new Plc4xTagDefinition("%DB23:200"));
        assertEquals("%DB23:200:DINT",
                adapter.createTagAddressForSubscription(new S7TestSub(Plc4xDataType.DATA_TYPE.DINT), tag));
    }

    @Test
    public void whenBlockShortBitDataType_thenDoNotModifyAddress() {
        final Plc4xTag tag = new Plc4xTag("tag", "not set", new Plc4xTagDefinition("%DB100:200.2"));
        assertEquals("%DB100:200.2:BOOL",
                adapter.createTagAddressForSubscription(new S7TestSub(Plc4xDataType.DATA_TYPE.BOOL), tag));
    }

    private static class TestS7ProtocolAdapter extends S7ProtocolAdapter {

        public TestS7ProtocolAdapter(final @NotNull ProtocolAdapterInput protocolAdapterInput) {
            super(S7ProtocolAdapterInformation.INSTANCE, protocolAdapterInput);
        }

        @Override
        public @NotNull String createTagAddressForSubscription(final @NotNull Plc4xToMqttMapping subscription, final @NotNull Plc4xTag tag) {
            return super.createTagAddressForSubscription(subscription, tag);
        }
    }

    private static class S7TestSub extends Plc4xToMqttMapping {

        public S7TestSub(
                final Plc4xDataType.@NotNull DATA_TYPE dataType) {
            super("mqttTopic", 1, MessageHandlingOptions.MQTTMessagePerTag, true, true, "tag", dataType, List.of());
        }
    }
}
