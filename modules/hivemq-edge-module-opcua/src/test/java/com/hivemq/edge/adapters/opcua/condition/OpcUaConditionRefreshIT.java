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
package com.hivemq.edge.adapters.opcua.condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.ProtocolAdapterConnectionDirection;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.adapters.opcua.FakeEventService;
import com.hivemq.edge.adapters.opcua.OpcUaProtocolAdapter;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagKind;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import util.EmbeddedOpcUaServerExtension;

/**
 * Checks that Edge asks the server to re-report its retained conditions once a subscription is established.
 * <p>
 * Transitions are events, so a condition that went active while Edge was disconnected has already fired and
 * cannot be re-sent. Without this call the current alarm picture stays unknown until each alarm next changes
 * on its own — which for a stable plant could be a very long time.
 */
public class OpcUaConditionRefreshIT {

    private static final long CONDITION_NODE_ID = 9400L;

    @RegisterExtension
    public final @NotNull EmbeddedOpcUaServerExtension opcUaServerExtension = new EmbeddedOpcUaServerExtension();

    private @Nullable OpcUaProtocolAdapter adapter;
    private @NotNull ProtocolAdapterState protocolAdapterState;
    private @NotNull FakeEventService eventService;

    @BeforeEach
    void setUp() {
        protocolAdapterState = new ProtocolAdapterStateImpl(mock(), "test-adapter-id", "opcua");
        eventService = new FakeEventService();
    }

    @AfterEach
    void tearDown() {
        if (adapter != null) {
            adapter.destroy();
        }
    }

    @Test
    @Timeout(120)
    void whenAConditionTagIsSubscribed_thenTheServerIsAskedToRefresh() throws Exception {
        opcUaServerExtension.getTestNamespace().observeRefreshEvents();
        final String conditionNodeId = opcUaServerExtension
                .getTestNamespace()
                .addAcknowledgeableConditionNode("RefreshableAlarm", CONDITION_NODE_ID);

        startAdapterWith(
                new OpcuaTag("refresh-alarm", "", new OpcuaTagDefinition(conditionNodeId, OpcuaTagKind.CONDITION)));

        // Observed through the server's response, not through our namespace: OPC 10000-9 §5.5.7 fixes the
        // call's ObjectId as the well-known ConditionType (ns=0;i=2782), so it never reaches the test
        // namespace's own method handlers. A successful call makes the server emit RefreshStart/RefreshEnd,
        // which is the visible consequence.
        await().untilAsserted(
                        () -> assertThat(opcUaServerExtension.getTestNamespace().refreshBracketCount())
                                .as("the server must be asked to re-report its retained conditions")
                                .isPositive());
    }

    @Test
    @Timeout(120)
    void whenNoTagIsACondition_thenNoRefreshIsRequested() throws Exception {
        // A refresh only makes sense when something is subscribed to receive the burst. Asking anyway would
        // be a pointless round trip against every server Edge talks to.
        final String valueNodeId = opcUaServerExtension
                .getTestNamespace()
                .addNode("Counter", org.eclipse.milo.opcua.stack.core.NodeIds.Int32, () -> 42, CONDITION_NODE_ID + 1);

        startAdapterWith(new OpcuaTag("plain-value", "", new OpcuaTagDefinition(valueNodeId, OpcuaTagKind.VALUE)));

        await().untilAsserted(() -> assertThat(protocolAdapterState.getConnectionStatus())
                .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED));

        Thread.sleep(2000);

        assertThat(opcUaServerExtension.getTestNamespace().methodCalls())
                .as("a value-only adapter must not call ConditionRefresh")
                .noneSatisfy(call -> assertThat(call.methodName()).isEqualTo("ConditionRefresh"));
    }

    private void startAdapterWith(final @NotNull OpcuaTag tag) {
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, 1000),
                null,
                null);

        final ProtocolAdapterInformation adapterInformation = mock(ProtocolAdapterInformation.class);
        when(adapterInformation.getProtocolId()).thenReturn("opcua");

        @SuppressWarnings("unchecked")
        final ProtocolAdapterInput<OpcUaSpecificAdapterConfig> input = mock(ProtocolAdapterInput.class);
        when(input.getAdapterId()).thenReturn("test-adapter-id");
        when(input.getProtocolAdapterState()).thenReturn(protocolAdapterState);
        when(input.getConfig()).thenReturn(config);
        final List<Tag> genericTags = new ArrayList<>(List.of(tag));
        when(input.getTags()).thenReturn(genericTags);
        when(input.adapterFactories()).thenReturn(mock(AdapterFactories.class));
        when(input.getProtocolAdapterMetricsHelper()).thenReturn(mock(ProtocolAdapterMetricsService.class));

        final ModuleServices moduleServices = mock(ModuleServices.class);
        when(moduleServices.eventService()).thenReturn(eventService);
        when(moduleServices.protocolAdapterTagStreamingService())
                .thenReturn(mock(com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService.class));
        when(input.moduleServices()).thenReturn(moduleServices);

        adapter = new OpcUaProtocolAdapter(adapterInformation, input);

        final ProtocolAdapterStartInput startInput = mock(ProtocolAdapterStartInput.class);
        when(startInput.moduleServices()).thenReturn(moduleServices);

        adapter.start(
                ProtocolAdapterConnectionDirection.Northbound, startInput, mock(ProtocolAdapterStartOutput.class));
    }
}
