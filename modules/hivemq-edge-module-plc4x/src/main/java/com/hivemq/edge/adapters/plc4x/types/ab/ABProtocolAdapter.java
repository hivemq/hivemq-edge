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
package com.hivemq.edge.adapters.plc4x.types.ab;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.edge.adapters.plc4x.impl.AbstractPlc4xAdapter;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;

import java.util.Collections;
import java.util.Map;

/**
 * @author HiveMQ Adapter Generator
 */
public class ABProtocolAdapter extends AbstractPlc4xAdapter<ABAdapterConfig> {

    public ABProtocolAdapter(
            final ProtocolAdapterInformation adapterInformation,
            final ABAdapterConfig adapterConfig,
            final MetricRegistry metricRegistry) {
        super(adapterInformation, adapterConfig, metricRegistry);
    }

    @Override
    protected String getProtocolHandler() {
        return "ab-eth";
    }

    @Override
    protected ReadType getReadType() {
        return ReadType.Read;
    }
}
