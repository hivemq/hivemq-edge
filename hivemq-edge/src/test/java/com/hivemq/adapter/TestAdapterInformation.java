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
