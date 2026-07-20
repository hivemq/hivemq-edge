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
package com.hivemq.edge.adapters.databases.v2;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.schema.ScalarSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageDispatcher;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.adapter.sdk.api.v2.services.ProtocolAdapterService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Test seams for driving a {@link DatabasesProtocolAdapter} directly: a {@link ProtocolAdapterInput} backed by a
 * supplied dispatcher, value factory, configuration map, and node set, and a {@link DataPointFactory} whose
 * {@link TestDataPoint} records whether a value was built as JSON — so a poll test can assert both the value type and
 * the JSON flag.
 */
public final class DatabasesAdapterTestFixtures {

    private DatabasesAdapterTestFixtures() {}

    /**
     * A configuration map carrying every connection parameter, for tests that construct the adapter.
     *
     * @param type the database engine name.
     * @param port the server port.
     * @return the configuration map.
     */
    public static @NotNull Map<String, Object> configuration(final @NotNull String type, final int port) {
        return Map.of(
                "type",
                type,
                "server",
                "localhost",
                "port",
                port,
                "database",
                "testdatabase",
                "username",
                "testuser",
                "password",
                "testpassword");
    }

    /**
     * As {@link #configuration(String, int)}, plus an explicit split-lines batch size.
     *
     * @param type      the database engine name.
     * @param port      the server port.
     * @param batchSize the number of result rows carried by each split-lines message.
     * @return the configuration map.
     */
    public static @NotNull Map<String, Object> configuration(
            final @NotNull String type, final int port, final int batchSize) {
        final Map<String, Object> configuration = new HashMap<>(configuration(type, port));
        configuration.put("batchSize", batchSize);
        return configuration;
    }

    /**
     * @param name  the tag name.
     * @param query the SQL query the tag's node carries.
     * @param split whether the node splits result rows into individual messages.
     * @return a pair whose {@link DatabaseNode} carries the query.
     */
    public static @NotNull NodeTagPair queryTag(
            final @NotNull String name, final @NotNull String query, final boolean split) {
        final Schema schema = new ScalarSchema(ScalarType.STRING, null, null, name, null, false, true, false);
        return NodeTagPair.create(new DatabaseNode(query, split), name, schema, true, false);
    }

    /**
     * @param adapterId     the adapter instance id.
     * @param dispatcher    the dispatcher the adapter's mailbox attaches to.
     * @param factory       the value factory the adapter builds its values with.
     * @param configuration the adapter's instance configuration map.
     * @param nodes         the pre-declared node/tag pairs.
     * @return a {@link ProtocolAdapterInput} carrying the given configuration and nodes.
     */
    public static @NotNull ProtocolAdapterInput input(
            final @NotNull String adapterId,
            final @NotNull MessageDispatcher dispatcher,
            final @NotNull DataPointFactory factory,
            final @NotNull Map<String, Object> configuration,
            final @NotNull List<NodeTagPair> nodes) {
        final DataPoint config = factory.create(adapterId, configuration);
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
                return nodes;
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
