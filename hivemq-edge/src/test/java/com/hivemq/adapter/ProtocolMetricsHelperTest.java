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

import com.codahale.metrics.MetricRegistry;
import com.hivemq.edge.modules.adapters.metrics.ProtocolAdapterMetricsHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Simon L Johnson
 */
public class ProtocolMetricsHelperTest {

    @Test
    void testMetricsAdapterWrapperUpdatesRegistry() {

        MetricRegistry registry = new MetricRegistry();
        ProtocolAdapterMetricsHelper helper = new ProtocolAdapterMetricsHelper("test-adapter-name","test-adapter-id", registry);

        helper.incrementReadPublishSuccess();
        helper.incrementReadPublishFailure();

        assertEquals(1, registry.getCounters().get("com.hivemq.edge.protocol-adapters.test-adapter-name.test-adapter-id.read.publish.failed.count").getCount(), "Matching failed data point should be incremented");
        assertEquals(1, registry.getCounters().get("com.hivemq.edge.protocol-adapters.test-adapter-name.test-adapter-id.read.publish.success.count").getCount(), "Matching success data point should be incremented");

        helper.increment("arbitrary-metric");
        assertEquals(1, registry.getCounters().get("com.hivemq.edge.protocol-adapters.test-adapter-name.test-adapter-id.arbitrary-metric").getCount(), "Matching arbitrary data point should be incremented");

    }
}
