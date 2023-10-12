package com.hivemq.adapter;

import com.hivemq.edge.modules.adapters.impl.AbstractProtocolAdapterInformation;
import com.hivemq.edge.modules.config.CustomConfig;
import com.hivemq.edge.modules.config.impl.AbstractProtocolAdapterConfig;

/**
 * @author Simon L Johnson
 */
public class TestAdapterInformation extends AbstractProtocolAdapterInformation {

    public TestAdapterInformation() {
    }

    @Override
    public String getProtocolName() {
        return "TestProtocol";
    }

    @Override
    public String getProtocolId() {
        return "test-adapter-information";
    }

    @Override
    public String getDisplayName() {
        return "Test Adapter Information";
    }

    @Override
    public String getDescription() {
        return "This is the test protocol information";
    }

    class TestConfig extends AbstractProtocolAdapterConfig {

    }
}
