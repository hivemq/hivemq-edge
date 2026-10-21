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
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import util.EmbeddedOpcUaServerExtension;

/// EDG-414: a tag's node may name its namespace by URI as well as by index.
public class NamespaceUriTagIT {

    @RegisterExtension
    public final @NotNull EmbeddedOpcUaServerExtension opcUaServerExtension = new EmbeddedOpcUaServerExtension();

    private final @NotNull FakeEventService eventService = new FakeEventService();
    private @NotNull OpcUaClient client;

    @AfterEach
    void tearDown() throws Exception {
        if (client != null) {
            client.disconnect();
        }
    }

    @Test
    @Timeout(120)
    void valueTagByUri_subscribesTheSameNodeAsByIndex() throws Exception {
        final String byIndex = opcUaServerExtension.getTestNamespace().addNode("ByUri", NodeIds.Int32, () -> 7, 93_101);
        final String byUri = "nsu=" + EmbeddedOpcUaServerExtension.NS_URI + ";i=93101";

        final OpcUaSubscriptionLifecycleHandler handler = handlerFor(List.of(
                new OpcuaTag("by-index", "", new OpcuaTagDefinition(byIndex, OpcuaTagKind.VALUE)),
                new OpcuaTag("by-uri", "", new OpcuaTagDefinition(byUri, OpcuaTagKind.VALUE))));
        final var subscription = handler.subscribe(client);

        assertThat(subscription).isPresent();
        assertThat(subscription.orElseThrow().getMonitoredItems())
                .as("both tags subscribe, on one node")
                .hasSize(2)
                .allSatisfy(
                        item -> assertThat(item.getReadValueId().getNodeId()).isEqualTo(NodeId.parse(byIndex)));
    }

    @Test
    @Timeout(120)
    void valueTagByUnpublishedUri_droppedWithAnEventNamingTheUri() throws Exception {
        final String healthy =
                opcUaServerExtension.getTestNamespace().addNode("Healthy", NodeIds.Int32, () -> 7, 93_102);

        final OpcUaSubscriptionLifecycleHandler handler = handlerFor(List.of(
                new OpcuaTag("healthy", "", new OpcuaTagDefinition(healthy, OpcuaTagKind.VALUE)),
                new OpcuaTag(
                        "unpublished",
                        "",
                        new OpcuaTagDefinition("nsu=http://not.published.example/;i=1", OpcuaTagKind.VALUE))));
        final var subscription = handler.subscribe(client);

        assertThat(subscription).isPresent();
        assertThat(subscription.orElseThrow().getMonitoredItems())
                .as("only the healthy tag subscribes")
                .singleElement()
                .satisfies(item -> assertThat(item.getReadValueId().getNodeId()).isEqualTo(NodeId.parse(healthy)));
        assertThat(eventService.readEvents(null, null)).anySatisfy(event -> assertThat(event.getMessage())
                .contains("unpublished")
                .contains("'node'")
                .contains("'http://not.published.example/', which the server does not publish"));
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
                eventService,
                "test-adapter",
                tags,
                client,
                config);
    }
}
