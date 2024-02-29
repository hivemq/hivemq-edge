package com.hivemq.edge.adapters.plc4x.types.siemens;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.edge.adapters.plc4x.model.Plc4xAdapterConfig;
import com.hivemq.edge.adapters.plc4x.model.Plc4xDataType;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class S7ProtocolAdapterTest {

    @Test
    public void whenTagSpecialDataType_thenModifyAddress() {
        final TestS7ProtocolAdapter adapter = new TestS7ProtocolAdapter();
        assertEquals("%IX200:DATE",
                adapter.createTagAddressForSubscription(new S7TestSub("%IW200", Plc4xDataType.DATA_TYPE.DATE)));
    }

    @Test
    public void whenTagNormalDataType_thenDoNotModifyAddress() {
        final TestS7ProtocolAdapter adapter = new TestS7ProtocolAdapter();
        assertEquals("%IW200:WORD",
                adapter.createTagAddressForSubscription(new S7TestSub("%IW200", Plc4xDataType.DATA_TYPE.WORD)));
    }

    @Test
    public void whenTagBitDataType_thenDoNotModifyAddress() {
        final TestS7ProtocolAdapter adapter = new TestS7ProtocolAdapter();
        assertEquals("%IX200.2:BOOL",
                adapter.createTagAddressForSubscription(new S7TestSub("%IX200.2", Plc4xDataType.DATA_TYPE.BOOL)));
    }

    @Test
    public void whenBlockSpecialDataType_thenModifyAddress() {
        final TestS7ProtocolAdapter adapter = new TestS7ProtocolAdapter();
        assertEquals("%DB23.DBX200:WCHAR",
                adapter.createTagAddressForSubscription(new S7TestSub("%DB23.DBW200", Plc4xDataType.DATA_TYPE.WCHAR)));
    }

    @Test
    public void whenBlockNormalDataType_thenDoNotModifyAddress() {
        final TestS7ProtocolAdapter adapter = new TestS7ProtocolAdapter();
        assertEquals("%DB23.DBD200:DINT",
                adapter.createTagAddressForSubscription(new S7TestSub("%DB23.DBD200", Plc4xDataType.DATA_TYPE.DINT)));
    }

    @Test
    public void whenBlockShortSpecialDataType_thenModifyAddress() {
        final TestS7ProtocolAdapter adapter = new TestS7ProtocolAdapter();
        assertEquals("%DB23:200:DATE",
                adapter.createTagAddressForSubscription(new S7TestSub("%DB23:200", Plc4xDataType.DATA_TYPE.DATE)));
    }

    @Test
    public void whenBlockBitDataType_thenDoNotModifyAddress() {
        final TestS7ProtocolAdapter adapter = new TestS7ProtocolAdapter();
        assertEquals("%DB100:DBX200.2:BOOL",
                adapter.createTagAddressForSubscription(new S7TestSub("%DB100:DBX200.2", Plc4xDataType.DATA_TYPE.BOOL)));
    }

    @Test
    public void whenBlockShortNormalDataType_thenDoNotModifyAddress() {
        final TestS7ProtocolAdapter adapter = new TestS7ProtocolAdapter();
        assertEquals("%DB23:200:DINT",
                adapter.createTagAddressForSubscription(new S7TestSub("%DB23:200", Plc4xDataType.DATA_TYPE.DINT)));
    }

    @Test
    public void whenBlockShortBitDataType_thenDoNotModifyAddress() {
        final TestS7ProtocolAdapter adapter = new TestS7ProtocolAdapter();
        assertEquals("%DB100:200.2:BOOL",
                adapter.createTagAddressForSubscription(new S7TestSub("%DB100:200.2", Plc4xDataType.DATA_TYPE.BOOL)));
    }

    private static class TestS7ProtocolAdapter extends S7ProtocolAdapter {

        public TestS7ProtocolAdapter() {
            super(S7ProtocolAdapterInformation.INSTANCE, getS7AdapterConfig(), new MetricRegistry());
        }

        private static S7AdapterConfig getS7AdapterConfig() {
            final S7AdapterConfig s7AdapterConfig = new S7AdapterConfig();
            s7AdapterConfig.setId("id");
            return s7AdapterConfig;
        }

        @Override
        public String createTagAddressForSubscription(final Plc4xAdapterConfig.@NotNull Subscription subscription) {
            return super.createTagAddressForSubscription(subscription);
        }
    }

    private static class S7TestSub extends Plc4xAdapterConfig.Subscription {

        private final String address;
        private final Plc4xDataType.DATA_TYPE type;

        public S7TestSub(final String address, final Plc4xDataType.DATA_TYPE type) {
            this.address = address;
            this.type = type;
        }

        @Override
        public String getTagAddress() {
            return address;
        }

        @Override
        public Plc4xDataType.DATA_TYPE getDataType() {
            return type;
        }
    }
}
