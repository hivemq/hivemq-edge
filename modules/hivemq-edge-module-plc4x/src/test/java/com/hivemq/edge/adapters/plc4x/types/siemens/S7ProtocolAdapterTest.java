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
import com.hivemq.edge.adapters.plc4x.config.Plc4xDataType;
import com.hivemq.edge.adapters.plc4x.config.Plc4xToMqttMapping;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class S7ProtocolAdapterTest {

    private final @NotNull ProtocolAdapterInput protocolAdapterInput = mock();

    @Test
    public void whenTagSpecialDataType_thenModifyAddress() {
        final TestS7ProtocolAdapter adapter = new TestS7ProtocolAdapter(protocolAdapterInput);
        assertEquals("%IX200:DATE",
                adapter.createTagAddressForSubscription(new S7TestSub("%IW200", Plc4xDataType.DATA_TYPE.DATE)));
    }

    @Test
    public void whenTagNormalDataType_thenDoNotModifyAddress() {
        final TestS7ProtocolAdapter adapter = new TestS7ProtocolAdapter(protocolAdapterInput);
        assertEquals("%IW200:WORD",
                adapter.createTagAddressForSubscription(new S7TestSub("%IW200", Plc4xDataType.DATA_TYPE.WORD)));
    }

    @Test
    public void whenTagBitDataType_thenDoNotModifyAddress() {
        final TestS7ProtocolAdapter adapter = new TestS7ProtocolAdapter(protocolAdapterInput);
        assertEquals("%IX200.2:BOOL",
                adapter.createTagAddressForSubscription(new S7TestSub("%IX200.2", Plc4xDataType.DATA_TYPE.BOOL)));
    }

    @Test
    public void whenBlockSpecialDataType_thenModifyAddress() {
        final TestS7ProtocolAdapter adapter = new TestS7ProtocolAdapter(protocolAdapterInput);
        assertEquals("%DB23.DBX200:WCHAR",
                adapter.createTagAddressForSubscription(new S7TestSub("%DB23.DBW200", Plc4xDataType.DATA_TYPE.WCHAR)));
    }

    @Test
    public void whenBlockNormalDataType_thenDoNotModifyAddress() {
        final TestS7ProtocolAdapter adapter = new TestS7ProtocolAdapter(protocolAdapterInput);
        assertEquals("%DB23.DBD200:DINT",
                adapter.createTagAddressForSubscription(new S7TestSub("%DB23.DBD200", Plc4xDataType.DATA_TYPE.DINT)));
    }

    @Test
    public void whenBlockShortSpecialDataType_thenModifyAddress() {
        final TestS7ProtocolAdapter adapter = new TestS7ProtocolAdapter(protocolAdapterInput);
        assertEquals("%DB23:200:DATE",
                adapter.createTagAddressForSubscription(new S7TestSub("%DB23:200", Plc4xDataType.DATA_TYPE.DATE)));
    }

    @Test
    public void whenBlockBitDataType_thenDoNotModifyAddress() {
        final TestS7ProtocolAdapter adapter = new TestS7ProtocolAdapter(protocolAdapterInput);
        assertEquals("%DB100:DBX200.2:BOOL",
                adapter.createTagAddressForSubscription(new S7TestSub("%DB100:DBX200.2",
                        Plc4xDataType.DATA_TYPE.BOOL)));
    }

    @Test
    public void whenBlockShortNormalDataType_thenDoNotModifyAddress() {
        final TestS7ProtocolAdapter adapter = new TestS7ProtocolAdapter(protocolAdapterInput);
        assertEquals("%DB23:200:DINT",
                adapter.createTagAddressForSubscription(new S7TestSub("%DB23:200", Plc4xDataType.DATA_TYPE.DINT)));
    }

    @Test
    public void whenBlockShortBitDataType_thenDoNotModifyAddress() {
        final TestS7ProtocolAdapter adapter = new TestS7ProtocolAdapter(protocolAdapterInput);
        assertEquals("%DB100:200.2:BOOL",
                adapter.createTagAddressForSubscription(new S7TestSub("%DB100:200.2", Plc4xDataType.DATA_TYPE.BOOL)));
    }

    private static class TestS7ProtocolAdapter extends S7ProtocolAdapter {

        public TestS7ProtocolAdapter(final @NotNull ProtocolAdapterInput protocolAdapterInput) {
            super(S7ProtocolAdapterInformation.INSTANCE,
                    protocolAdapterInput);
        }

        @Override
        public @NotNull String createTagAddressForSubscription(final @NotNull Plc4xToMqttMapping subscription) {
            return super.createTagAddressForSubscription(subscription);
        }
    }

    private static class S7TestSub extends Plc4xToMqttMapping {

        public S7TestSub(
                final @NotNull String tagAddress,
                final Plc4xDataType.@NotNull DATA_TYPE dataType) {
            super("mqttTopic",
                    1,
                    MessageHandlingOptions.MQTTMessagePerTag,
                    true,
                    true,
                    "tag",
                    tagAddress,
                    dataType,
                    List.of());
        }
    }
}
