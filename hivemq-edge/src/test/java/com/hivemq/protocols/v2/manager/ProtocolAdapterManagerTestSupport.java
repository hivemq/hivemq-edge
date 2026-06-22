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
package com.hivemq.protocols.v2.manager;

import com.hivemq.adapter.sdk.api.ProtocolAdapterCategory;
import com.hivemq.adapter.sdk.api.ProtocolAdapterTag;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.schema.ScalarSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.v2.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.v2.model.BrowseFilter;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterOutput;
import com.hivemq.adapter.sdk.api.v2.model.VerifyOutcome;
import com.hivemq.adapter.sdk.api.v2.model.WriteEntry;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.node.NodeProperty;
import com.hivemq.protocols.v2.config.AccessFlagsEntity;
import com.hivemq.protocols.v2.config.NorthboundMappingEntity;
import com.hivemq.protocols.v2.config.ProtocolAdapterEntity;
import com.hivemq.protocols.v2.config.RetryPolicyEntity;
import com.hivemq.protocols.v2.config.SouthboundMappingEntity;
import com.hivemq.protocols.v2.config.TagEntity;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Shared fixtures for the manager tests: fluent builders for the read-only {@code <v2-protocol-adapters>} entities,
 * and the minimal SDK-v2 doubles (a Jackson-friendly {@link Node}, a synchronous protocol adapter, its information,
 * and a factory) the real wiring is exercised against. The doubles are deliberately tiny — the manager and the
 * wrapper factory are what these tests prove, not a real protocol.
 */
final class ProtocolAdapterManagerTestSupport {

    static final @NotNull String TEST_PROTOCOL_ID = "test";

    private ProtocolAdapterManagerTestSupport() {}

    static @NotNull EntityBuilder adapter(final @NotNull String adapterId) {
        return new EntityBuilder(adapterId);
    }

    static @NotNull TagBuilder tag(final @NotNull String name) {
        return new TagBuilder(name);
    }

    /**
     * A fluent builder for a {@link ProtocolAdapterEntity}, defaulting to a northbound-activated single-tag adapter
     * of the {@link #TEST_PROTOCOL_ID test} type.
     */
    static final class EntityBuilder {

        private final @NotNull String adapterId;
        private @NotNull String protocolId = TEST_PROTOCOL_ID;
        private int configVersion = ProtocolAdapterEntity.DEFAULT_CONFIG_VERSION;
        private boolean northboundActivated = true;
        private boolean southboundActivated = false;
        private boolean skipVerification = false;
        private @NotNull Map<String, Object> adapterConfiguration = new HashMap<>();
        private @NotNull RetryPolicyEntity retryPolicy = new RetryPolicyEntity();
        private long watchdogTimeoutMillis = ProtocolAdapterEntity.DEFAULT_WATCHDOG_TIMEOUT_MILLIS;
        private long commandTimeoutMillis = ProtocolAdapterEntity.DEFAULT_COMMAND_TIMEOUT_MILLIS;
        private @NotNull List<TagEntity> tags =
                new ArrayList<>(List.of(tag("temperature").build()));
        private @NotNull List<NorthboundMappingEntity> northboundMappings = new ArrayList<>();
        private @NotNull List<SouthboundMappingEntity> southboundMappings = new ArrayList<>();

        private EntityBuilder(final @NotNull String adapterId) {
            this.adapterId = adapterId;
        }

        @NotNull
        EntityBuilder protocolId(final @NotNull String protocolId) {
            this.protocolId = protocolId;
            return this;
        }

        @NotNull
        EntityBuilder configVersion(final int configVersion) {
            this.configVersion = configVersion;
            return this;
        }

        @NotNull
        EntityBuilder northboundActivated(final boolean northboundActivated) {
            this.northboundActivated = northboundActivated;
            return this;
        }

        @NotNull
        EntityBuilder southboundActivated(final boolean southboundActivated) {
            this.southboundActivated = southboundActivated;
            return this;
        }

        @NotNull
        EntityBuilder skipVerification(final boolean skipVerification) {
            this.skipVerification = skipVerification;
            return this;
        }

        @NotNull
        EntityBuilder adapterConfiguration(final @NotNull Map<String, Object> adapterConfiguration) {
            this.adapterConfiguration = adapterConfiguration;
            return this;
        }

        @NotNull
        EntityBuilder retryPolicy(final @NotNull RetryPolicyEntity retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        @NotNull
        EntityBuilder watchdogTimeoutMillis(final long watchdogTimeoutMillis) {
            this.watchdogTimeoutMillis = watchdogTimeoutMillis;
            return this;
        }

        @NotNull
        EntityBuilder commandTimeoutMillis(final long commandTimeoutMillis) {
            this.commandTimeoutMillis = commandTimeoutMillis;
            return this;
        }

        @NotNull
        EntityBuilder tags(final @NotNull TagEntity... tags) {
            this.tags = new ArrayList<>(List.of(tags));
            return this;
        }

        @NotNull
        EntityBuilder northboundMapping(final @NotNull String tagName, final @NotNull String topic) {
            this.northboundMappings.add(new NorthboundMappingEntity(tagName, topic));
            return this;
        }

        @NotNull
        EntityBuilder southboundMapping(final @NotNull String topic, final @NotNull String tagName) {
            this.southboundMappings.add(new SouthboundMappingEntity(topic, tagName));
            return this;
        }

        @NotNull
        ProtocolAdapterEntity build() {
            return new ProtocolAdapterEntity(
                    adapterId,
                    protocolId,
                    configVersion,
                    northboundActivated,
                    southboundActivated,
                    skipVerification,
                    adapterConfiguration,
                    retryPolicy,
                    watchdogTimeoutMillis,
                    commandTimeoutMillis,
                    tags,
                    northboundMappings,
                    southboundMappings);
        }
    }

    /**
     * A fluent builder for a {@link TagEntity}, defaulting to a pollable, both-aspect-activated tag whose
     * {@code node-string} deserializes into {@link TestNode}.
     */
    static final class TagBuilder {

        private final @NotNull String name;
        private @NotNull String nodeString;
        private boolean readActivated = true;
        private boolean writeActivated = true;
        private boolean pollable = true;
        private boolean subscribable = false;
        private long pollIntervalMillis = 5000;
        private @NotNull AccessFlagsEntity access = new AccessFlagsEntity();

        private TagBuilder(final @NotNull String name) {
            this.name = name;
            this.nodeString = "{\"identifier\":\"" + name + "\"}";
        }

        @NotNull
        TagBuilder nodeString(final @NotNull String nodeString) {
            this.nodeString = nodeString;
            return this;
        }

        @NotNull
        TagBuilder readActivated(final boolean readActivated) {
            this.readActivated = readActivated;
            return this;
        }

        @NotNull
        TagBuilder writeActivated(final boolean writeActivated) {
            this.writeActivated = writeActivated;
            return this;
        }

        @NotNull
        TagBuilder pollable(final boolean pollable) {
            this.pollable = pollable;
            return this;
        }

        @NotNull
        TagBuilder pollIntervalMillis(final long pollIntervalMillis) {
            this.pollIntervalMillis = pollIntervalMillis;
            return this;
        }

        @NotNull
        TagEntity build() {
            return new TagEntity(
                    name,
                    nodeString,
                    readActivated,
                    writeActivated,
                    pollable,
                    subscribable,
                    pollIntervalMillis,
                    access);
        }
    }

    static @NotNull Schema scalarSchema() {
        return new ScalarSchema(ScalarType.STRING, null, null, null, null, false, true, false);
    }

    /**
     * A reused-{@link DataPoint} double — the wrapper never inspects it, so the simplest carrier suffices.
     */
    record TestDataPoint(@NotNull String tagName, @NotNull Object value, boolean json) implements DataPoint {

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

    /**
     * A {@link DataPointFactory} double building {@link TestDataPoint}s.
     */
    static final class TestDataPointFactory implements DataPointFactory {

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
     * A Jackson-friendly {@link Node}: a public {@code identifier} field a {@code node-string} deserializes into.
     */
    static final class TestNode extends Node {

        public @NotNull String identifier = "";

        @Override
        public @NotNull String nodeId() {
            return identifier;
        }

        @Override
        public @NotNull String nodeString() {
            return "{\"identifier\":\"" + identifier + "\"}";
        }

        @Override
        public @NotNull EnumSet<NodeProperty> properties() {
            return EnumSet.of(NodeProperty.UNIQUE);
        }
    }

    /**
     * A synchronous protocol-adapter double: every command acknowledges immediately through the tell-façade, so a
     * real wrapper reaches {@code CONNECTED} deterministically under a {@link com.hivemq.protocols.v2.runtime.ManualDispatcher}.
     */
    static final class TestProtocolAdapter implements ProtocolAdapter {

        private final @NotNull String adapterId;
        private final @NotNull ProtocolAdapterOutput output;
        private final @NotNull DataPointFactory dataPointFactory;

        TestProtocolAdapter(
                final @NotNull String adapterId,
                final @NotNull ProtocolAdapterOutput output,
                final @NotNull DataPointFactory dataPointFactory) {
            this.adapterId = adapterId;
            this.output = output;
            this.dataPointFactory = dataPointFactory;
        }

        @Override
        public @NotNull String adapterId() {
            return adapterId;
        }

        @Override
        public void start() {
            output.started();
        }

        @Override
        public void stop() {
            output.stopped();
        }

        @Override
        public void connect() {
            output.connected();
        }

        @Override
        public void disconnect() {
            output.disconnected();
        }

        @Override
        public void verifyBatch(final @NotNull List<Node> nodes) {
            nodes.forEach(node -> output.verifyResult(node, new VerifyOutcome.Success()));
        }

        @Override
        public void pollBatch(final @NotNull List<Node> nodes) {
            nodes.forEach(node -> output.dataPoint(node, dataPointFactory.create(node.nodeId(), "value")));
        }

        @Override
        public void addSubscriptionBatch(final @NotNull List<Node> nodes) {}

        @Override
        public void removeSubscriptionBatch(final @NotNull List<Node> nodes) {}

        @Override
        public void writeBatch(final @NotNull List<WriteEntry> entries) {
            entries.forEach(entry -> output.writeResult(entry.node(), true, null));
        }

        @Override
        public void browse(final @NotNull BrowseFilter filter) {
            output.browseResult(List.of());
        }
    }

    /**
     * The {@link ProtocolAdapterInformation} of {@link TestProtocolAdapter}: only {@code protocolId} and
     * {@code nodeClass} matter to the manager and the wrapper factory.
     */
    static final class TestProtocolAdapterInformation implements ProtocolAdapterInformation {

        private final @NotNull String protocolId;

        TestProtocolAdapterInformation(final @NotNull String protocolId) {
            this.protocolId = protocolId;
        }

        @Override
        public @NotNull String protocolId() {
            return protocolId;
        }

        @Override
        public @NotNull String displayName() {
            return "Test Adapter";
        }

        @Override
        public @NotNull String description() {
            return "A synchronous protocol-adapter double for the manager tests.";
        }

        @Override
        public @NotNull String version() {
            return "1";
        }

        @Override
        public @NotNull String logoUrl() {
            return "";
        }

        @Override
        public @NotNull String author() {
            return "HiveMQ";
        }

        @Override
        public @NotNull ProtocolAdapterCategory category() {
            return ProtocolAdapterCategory.SIMULATION;
        }

        @Override
        public @NotNull List<ProtocolAdapterTag> tags() {
            return List.of();
        }

        @Override
        public @NotNull EnumSet<ProtocolAdapterCapability> capabilities() {
            return EnumSet.of(ProtocolAdapterCapability.SUBSCRIPTIONS, ProtocolAdapterCapability.WRITE);
        }

        @Override
        public @NotNull Class<? extends Node> nodeClass() {
            return TestNode.class;
        }

        @Override
        public int currentConfigVersion() {
            return 2;
        }
    }

    /**
     * The factory of the {@link #TEST_PROTOCOL_ID test} adapter type, building {@link TestProtocolAdapter}s.
     */
    static final class TestProtocolAdapterFactory implements ProtocolAdapterFactory {

        private final @NotNull ProtocolAdapterInformation information;

        TestProtocolAdapterFactory(final @NotNull String protocolId) {
            this.information = new TestProtocolAdapterInformation(protocolId);
        }

        @Override
        public @NotNull ProtocolAdapterInformation information() {
            return information;
        }

        @Override
        public @NotNull ProtocolAdapter createAdapter(
                final @NotNull ProtocolAdapterInput input, final @NotNull ProtocolAdapterOutput output) {
            return new TestProtocolAdapter(
                    input.adapterId(), output, input.services().dataPointFactory());
        }

        @Override
        public @NotNull Schema adapterConfigSchema() {
            return scalarSchema();
        }

        @Override
        public @NotNull Schema nodeDefinitionSchema() {
            return scalarSchema();
        }
    }
}
