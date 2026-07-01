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
package com.hivemq.edge.adapters.opcua.conformance;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageDispatcher;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.adapter.sdk.api.v2.services.ProtocolAdapterService;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * EDG-737 — shared test plumbing for the OPC-UA conformance tests: a {@link ProtocolAdapterInput} backed by a
 * no-frills {@link DataPointFactory} and wired to the test's {@link MessageDispatcher}. Factored out of the
 * individual conformance tests so the fakes live in one place.
 */
final class ConformanceHarness {

    private ConformanceHarness() {}

    /** A minimal {@link ProtocolAdapterInput} for an adapter under test, wired to {@code dispatcher}. */
    static @NotNull ProtocolAdapterInput input(final @NotNull MessageDispatcher dispatcher) {
        final DataPointFactory factory = dataPointFactory();
        return new ConformanceInput(
                "opcua-conformance",
                factory.createJsonDataPoint("adapterConfig", JsonNodeFactory.instance.objectNode()),
                List.of(),
                new ConformanceService(factory, dispatcher));
    }

    /** A no-frills {@link DataPointFactory} that wraps a name/value into a {@link DataPoint}. */
    static @NotNull DataPointFactory dataPointFactory() {
        return new ConformanceDataPointFactory();
    }

    private record ConformanceInput(
            @NotNull String adapterId,
            @NotNull DataPoint adapterConfig,
            @NotNull List<NodeTagPair> nodes,
            @NotNull ProtocolAdapterService services)
            implements ProtocolAdapterInput {}

    private record ConformanceService(
            @NotNull DataPointFactory dataPointFactory,
            @NotNull MessageDispatcher dispatcher) implements ProtocolAdapterService {}

    private static final class ConformanceDataPointFactory implements DataPointFactory {
        @Override
        public @NotNull DataPoint create(final @NotNull String tagName, final @NotNull Object tagValue) {
            return new SimpleDataPoint(tagName, tagValue, false);
        }

        @Override
        public @NotNull DataPoint createJsonDataPoint(final @NotNull String tagName, final @NotNull Object tagValue) {
            return new SimpleDataPoint(tagName, tagValue, true);
        }

        private record SimpleDataPoint(
                @NotNull String tagName, @NotNull Object tagValue, boolean json) implements DataPoint {
            @Override
            public @NotNull Object getTagValue() {
                return tagValue;
            }

            @Override
            public boolean treatTagValueAsJson() {
                return json;
            }

            @Override
            public @NotNull String getTagName() {
                return tagName;
            }
        }
    }
}
