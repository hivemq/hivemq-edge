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
package com.hivemq.edge.adapters.file.v2;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageDispatcher;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.adapter.sdk.api.v2.services.ProtocolAdapterService;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Test seams for driving a {@link FileProtocolAdapter} directly: a {@link ProtocolAdapterInput} backed by a supplied
 * dispatcher and value factory, and a {@link DataPointFactory} whose {@link TestDataPoint} records whether a value was
 * built as JSON — so a poll test can assert both the value type and the JSON flag.
 */
public final class FileAdapterTestFixtures {

    private FileAdapterTestFixtures() {}

    /**
     * @param adapterId  the adapter instance id.
     * @param dispatcher the dispatcher the adapter's mailbox attaches to.
     * @param factory    the value factory the adapter builds its values with.
     * @return a {@link ProtocolAdapterInput} carrying an empty configuration and no pre-declared nodes (poll tests
     *         drive nodes through {@code pollBatch}).
     */
    public static @NotNull ProtocolAdapterInput input(
            final @NotNull String adapterId,
            final @NotNull MessageDispatcher dispatcher,
            final @NotNull DataPointFactory factory) {
        final DataPoint config = factory.create(adapterId, "");
        final ProtocolAdapterService services = new ProtocolAdapterService() {
            @Override
            public @NotNull DataPointFactory dataPointFactory() {
                return factory;
            }

            @Override
            public @NotNull MessageDispatcher dispatcher() {
                return dispatcher;
            }
        };
        return new ProtocolAdapterInput() {
            @Override
            public @NotNull String adapterId() {
                return adapterId;
            }

            @Override
            public @NotNull DataPoint adapterConfig() {
                return config;
            }

            @Override
            public @NotNull List<NodeTagPair> nodes() {
                return List.of();
            }

            @Override
            public @NotNull ProtocolAdapterService services() {
                return services;
            }
        };
    }

    /**
     * A {@link DataPointFactory} that builds {@link TestDataPoint}s, preserving the JSON flag so a test can assert
     * whether the adapter treated a value as JSON.
     */
    public static final class TestDataPointFactory implements DataPointFactory {

        @Override
        public @NotNull DataPoint create(final @NotNull String tagName, final @NotNull Object tagValue) {
            return new TestDataPoint(tagName, tagValue, false);
        }

        @Override
        public @NotNull DataPoint createJsonDataPoint(final @NotNull String tagName, final @NotNull Object tagValue) {
            return new TestDataPoint(tagName, tagValue, true);
        }
    }

    /**
     * @param tagName the tag name carried by the value.
     * @param value   the tag value.
     * @param json    whether the value is treated as JSON.
     */
    public record TestDataPoint(
            @NotNull String tagName, @NotNull Object value, boolean json) implements DataPoint {

        @Override
        public @NotNull Object getTagValue() {
            return value;
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
