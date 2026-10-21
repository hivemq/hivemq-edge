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
package com.hivemq.edge.adapters.opcua;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.ProtocolAdapterConnectionDirection;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationInput;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationOutput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.adapters.opcua.config.ConnectionOptions;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagKind;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import util.EmbeddedOpcUaServerExtension;
import util.TestNamespace;

/// Which outcome a method tag's schema request reports, against a live server.
///
/// **EDG-1100 P10.** Asking for the schema of a method tag that names a node the server does not expose, or one
/// that is a Variable rather than an Object, answered `500 InternalServerError`. QA was explicit that the detail
/// text was right and only the status misled: a 500 sends an operator to look at Edge, or at us, for a fault
/// that is in their own tag definition.
///
/// The runtime could not tell the two apart because the adapter reported both through `fail`, which maps to 500.
/// It now reports a tag that is **wrong about the device** through `tagNotFound`, which the REST layer already
/// maps to 404, and keeps `fail` for a device that **could not be asked** -- where the tag may be perfectly
/// correct and a configuration error would send the operator to fix something that is not broken.
///
/// Tested here rather than against a mocked adapter because the distinction is drawn from what the server
/// actually answers: the REST layer's own tests settle the status mapping, and this settles which outcome the
/// adapter reports for a real address space.
class MethodTagSchemaOutcomeIT {

    private static final @NotNull String ADAPTER_ID = "opcua-method-schema";

    @RegisterExtension
    public final @NotNull EmbeddedOpcUaServerExtension opcUaServerExtension = new EmbeddedOpcUaServerExtension();

    private final @NotNull ProtocolAdapterStateImpl protocolAdapterState = mock(ProtocolAdapterStateImpl.class);
    private final @NotNull ModuleServices moduleServices = mock(ModuleServices.class);

    @BeforeEach
    void setUp() {
        when(moduleServices.eventService()).thenReturn(new FakeEventService());
        when(moduleServices.protocolAdapterTagStreamingService())
                .thenReturn(mock(ProtocolAdapterTagStreamingService.class));
    }

    private @NotNull TestNamespace namespace() {
        return Objects.requireNonNull(opcUaServerExtension.getTestNamespace());
    }

    /// Starts an adapter against the embedded server and waits until it is connected.
    private @NotNull OpcUaProtocolAdapter started(final @NotNull OpcuaTag... tags) {
        final ProtocolAdapterInformation adapterInformation = mock(ProtocolAdapterInformation.class);
        when(adapterInformation.getProtocolId()).thenReturn("opcua");

        @SuppressWarnings("unchecked")
        final ProtocolAdapterInput<OpcUaSpecificAdapterConfig> input = mock(ProtocolAdapterInput.class);
        when(input.getAdapterId()).thenReturn(ADAPTER_ID);
        when(input.getProtocolAdapterState()).thenReturn(protocolAdapterState);
        when(input.getConfig())
                .thenReturn(new OpcUaSpecificAdapterConfig(
                        opcUaServerExtension.getServerUri(),
                        false,
                        null,
                        null,
                        null,
                        OpcUaToMqttConfig.defaultOpcUaToMqttConfig(),
                        null,
                        ConnectionOptions.defaultConnectionOptions()));
        when(input.getTags()).thenReturn(new ArrayList<Tag>(List.of(tags)));
        when(input.adapterFactories()).thenReturn(mock(AdapterFactories.class));
        when(input.getProtocolAdapterMetricsHelper()).thenReturn(mock(ProtocolAdapterMetricsService.class));
        when(input.moduleServices()).thenReturn(moduleServices);

        final OpcUaProtocolAdapter adapter = new OpcUaProtocolAdapter(adapterInformation, input);
        final ProtocolAdapterStartInput startInput = mock(ProtocolAdapterStartInput.class);
        when(startInput.moduleServices()).thenReturn(moduleServices);
        adapter.start(
                ProtocolAdapterConnectionDirection.Northbound, startInput, mock(ProtocolAdapterStartOutput.class));

        // The adapter reports CONNECTED once its client is usable, which is the condition every question here
        // depends on: a schema request before that answers "not connected" rather than anything about the tag.
        verify(protocolAdapterState, timeout(30_000))
                .setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
        return adapter;
    }

    private @NotNull TagSchemaCreationOutput schemaOf(
            final @NotNull OpcUaProtocolAdapter adapter, final @NotNull String tagName) {

        final TagSchemaCreationInput input = mock(TagSchemaCreationInput.class);
        when(input.getTagName()).thenReturn(tagName);
        final TagSchemaCreationOutput output = mock(TagSchemaCreationOutput.class);
        adapter.createTagSchema(input, output);
        return output;
    }

    private static @NotNull OpcuaTag methodTag(final @NotNull String name, final @NotNull String node) {
        return new OpcuaTag(name, "", new OpcuaTagDefinition(node, OpcuaTagKind.METHOD));
    }

    @Test
    void aMethodTagNamingAVariable_reportsTheTagRatherThanAFailure() {
        // The case QA reported. Browsing alone would have called this "an object with no methods" -- a usable
        // tag that can never serve a request. Naming the mistake is what makes a 404 the honest answer.
        final String variable = namespace().addNode("aVariable", NodeIds.Int32, () -> 1, 7_001L);
        final OpcUaProtocolAdapter adapter = started(methodTag("boiler", variable));

        final TagSchemaCreationOutput output = schemaOf(adapter, "boiler");

        verify(output, timeout(30_000)).tagNotFound(any());
        verify(output, never()).fail(any(String.class));
        verify(output, never()).fail(any(Throwable.class), any());
    }

    @Test
    void aMethodTagNamingANodeTheServerDoesNotHave_reportsTheTag() {
        final OpcUaProtocolAdapter adapter =
                started(methodTag("ghost", "ns=" + namespace().getNamespaceIndex() + ";s=NoSuchObject"));

        final TagSchemaCreationOutput output = schemaOf(adapter, "ghost");

        verify(output, timeout(30_000)).tagNotFound(any());
        verify(output, never()).fail(any(String.class));
    }

    // No test for a malformed node id, and the reason is worth recording: an adapter carrying one does not
    // start. The connection goes CONNECTING then ERROR and never reaches CONNECTED, because an invalid
    // configuration is decidable by reading the configuration alone -- no device could make the id parse -- and
    // Edge stops the adapter rather than running it with a tag it knows is broken. So the malformed branch in
    // createTagSchema is defensive, unreachable in a started adapter, and asserting on it here would pin a path
    // that does not occur. It was changed to report a tag fault for consistency, not because it is reached.

    @Test
    void aMethodTagNamingAProperObject_producesASchema() {
        // The boundary that keeps the change honest: making every outcome a tag fault would satisfy the tests
        // above and break the feature.
        namespace().addMethodObject("Boiler");
        final OpcUaProtocolAdapter adapter =
                started(methodTag("boiler", "ns=" + namespace().getNamespaceIndex() + ";s=Boiler"));

        final TagSchemaCreationOutput output = schemaOf(adapter, "boiler");

        verify(output, timeout(30_000)).finish(any(TagSchemaCreationOutput.DataPointSchema.class));
        verify(output, never()).tagNotFound(any());
        verify(output, never()).fail(any(String.class));
    }
}
