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
package com.hivemq.edge.adapters.opcua.listeners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.edge.adapters.opcua.FakeEventService;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagKind;
import java.util.List;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import util.EmbeddedOpcUaServerExtension;

/**
 * EDG-835: two tags carrying the same definition must both be subscribed.
 * <p>
 * The configuration permits this — uniqueness is enforced on the tag <em>name</em> only, in
 * {@code DomainTag.equals} and in {@code ProtocolAdapterManager}'s add path — so a user may create
 * {@code boiler-alarm} and {@code boiler-alarm-copy} pointing at exactly the same node with the same kind
 * and type. Two northbound mappings sending one alarm to two topics is a legitimate reason to do it.
 * <p>
 * Reconciliation used to key by node id, and the second tag was discarded by a {@code (first, second) ->
 * first} merge before it could be subscribed: two tags in, <b>one</b> monitored item out, on both the value
 * and condition paths. No log, no event — a green adapter with one tag silently never producing data.
 * Measured against the previous code, not inferred from it.
 */
public class DuplicateTagDefinitionIT {

    @RegisterExtension
    public final @NotNull EmbeddedOpcUaServerExtension opcUaServerExtension = new EmbeddedOpcUaServerExtension();

    private @NotNull OpcUaClient client;

    @AfterEach
    void tearDown() throws Exception {
        if (client != null) {
            client.disconnect();
        }
    }

    @Test
    @Timeout(120)
    void twoConditionTagsWithTheSameDefinitionAreBothSubscribed() throws Exception {
        final String alarm =
                opcUaServerExtension.getTestNamespace().addAcknowledgeableConditionNode("DupAlarm", 93_001);

        // Same node, same kind, same type. Only the names differ, which is all the configuration requires.
        final OpcuaTagDefinition definition = new OpcuaTagDefinition(alarm, OpcuaTagKind.CONDITION);
        final List<OpcuaTag> tags =
                List.of(new OpcuaTag("alarm-a", "", definition), new OpcuaTag("alarm-b", "", definition));

        final OpcUaSubscriptionLifecycleHandler handler = handlerFor(tags);
        final var subscription = handler.subscribe(client);

        assertThat(subscription).isPresent();
        assertThat(subscription.orElseThrow().getMonitoredItems())
                .as("both condition tags must subscribe; keyed by node id the second was silently dropped")
                .hasSize(2);
    }

    @Test
    @Timeout(120)
    void twoValueTagsWithTheSameDefinitionAreBothSubscribed() throws Exception {
        // The same question for value tags, where the item is created on the tag's own node rather than on
        // a notifier -- so both items genuinely share a node id.
        final String node = opcUaServerExtension
                .getTestNamespace()
                .addNode("DupValue", org.eclipse.milo.opcua.stack.core.NodeIds.Int32, () -> 42, 93_002);

        final OpcuaTagDefinition definition = new OpcuaTagDefinition(node, OpcuaTagKind.VALUE);
        final List<OpcuaTag> tags =
                List.of(new OpcuaTag("value-a", "", definition), new OpcuaTag("value-b", "", definition));

        final OpcUaSubscriptionLifecycleHandler handler = handlerFor(tags);
        final var subscription = handler.subscribe(client);

        assertThat(subscription).isPresent();
        assertThat(subscription.orElseThrow().getMonitoredItems())
                .as("both value tags must subscribe; keyed by node id the second was silently dropped")
                .hasSize(2);
    }

    private @NotNull OpcUaSubscriptionLifecycleHandler handlerFor(final @NotNull List<OpcuaTag> tags) throws Exception {
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, 1000),
                null,
                null);

        client = OpcUaClient.create(opcUaServerExtension.getServerUri());
        client.connect();

        return new OpcUaSubscriptionLifecycleHandler(
                mock(ProtocolAdapterMetricsService.class),
                mock(ProtocolAdapterTagStreamingService.class),
                new FakeEventService(),
                "test-adapter",
                tags,
                client,
                config);
    }
}
