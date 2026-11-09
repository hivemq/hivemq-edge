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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import com.hivemq.adapter.sdk.api.writing.WritingContext;
import com.hivemq.adapter.sdk.api.writing.WritingInput;
import com.hivemq.adapter.sdk.api.writing.WritingOutput;
import com.hivemq.edge.adapters.opcua.condition.CapturingTagStreamingService;
import com.hivemq.edge.adapters.opcua.config.ConnectionOptions;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagKind;
import com.hivemq.edge.adapters.opcua.method.MethodRequestFieldsOpcUa;
import com.hivemq.edge.adapters.opcua.southbound.OpcUaPayload;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import util.EmbeddedOpcUaServerExtension;
import util.TestNamespace;

/// A method request that Edge refused after its syntactic checks is answered, and the method does not run.
///
/// Edge's writer marks such a request with `refusal` in the request parameters when the schema check fails. The
/// adapter answers it with `Bad_InvalidArgument`, the caller's `correlationData` and the reason in
/// `statusInfo.reason`, and sends nothing to the server.
///
/// **Why against a real server.** "The method does not run" is a claim about the server, and only the server's
/// own count of the method's runs settles it. A mocked client would run the method whenever the mock says so.
/// Each refusal case ends with an unmarked request on the same adapter, which the server does run: a count of
/// one after it shows the server was reachable, and that nothing of the refused request arrived later.
class MethodRefusalIT {

    private static final @NotNull String ADAPTER_ID = "opcua-method-refusal";
    private static final @NotNull String TAG_NAME = "counter";

    // The ceiling MethodPrewarmIT uses for a connect to and a call on the embedded server. A refusal is answered
    // without a round trip, so it settles in milliseconds; only the control call waits on the server.
    private static final @NotNull Duration WAIT = Duration.ofSeconds(30);

    @RegisterExtension
    public final @NotNull EmbeddedOpcUaServerExtension opcUaServerExtension = new EmbeddedOpcUaServerExtension();

    private final @NotNull ObjectMapper mapper = new ObjectMapper();
    private final @NotNull ModuleServices moduleServices = mock(ModuleServices.class);
    private final @NotNull ProtocolAdapterStateImpl protocolAdapterState = mock(ProtocolAdapterStateImpl.class);
    private final @NotNull CapturingTagStreamingService streaming = new CapturingTagStreamingService();

    @BeforeEach
    void setUp() {
        when(moduleServices.eventService()).thenReturn(new FakeEventService());
        when(moduleServices.protocolAdapterTagStreamingService()).thenReturn(streaming);
    }

    /// A refused request gets `Bad_InvalidArgument` with the reason, its token and its reply address, and the
    /// server never runs the method: the unmarked request after it is the only run the server counts.
    @Test
    void aRefusedRequest_isAnsweredAndTheMethodNeverRuns() throws Exception {
        final OpcUaProtocolAdapter adapter = started();

        final WritingOutput refused = write(
                adapter, "Square", "{\"input1\":3}", "c-refused", "reply/refused", "$.value.input1: string found");

        verify(refused, timeout(WAIT.toMillis())).fail(anyString());
        verify(refused, never()).finish();
        final JsonNode answer = answer(0);
        assertThat(answer.path(MethodRequestFieldsOpcUa.ERROR_CODE).asText()).isEqualTo("Bad_InvalidArgument");
        assertThat(answer.path(MethodRequestFieldsOpcUa.CORRELATION_DATA).asText())
                .isEqualTo("c-refused");
        assertThat(answer.path(MethodRequestFieldsOpcUa.RESPONSE_TOPIC).asText())
                .isEqualTo("reply/refused");
        assertThat(answer.path(MethodRequestFieldsOpcUa.RESPONSE_METHOD_NAME).asText())
                .isEqualTo("Square");
        assertThat(answer.path(MethodRequestFieldsOpcUa.STATUS_INFO)
                        .path(MethodRequestFieldsOpcUa.REASON)
                        .asText())
                .contains("Square", TAG_NAME, "$.value.input1: string found");

        assertOnlyTheControlRuns(adapter);
    }

    /// The same request without the mark runs the method once and is answered with its output and no
    /// `statusInfo`.
    @Test
    void theSameRequestWithoutARefusal_runsTheMethod() throws Exception {
        final OpcUaProtocolAdapter adapter = started();

        final WritingOutput output = write(adapter, "Square", "{\"input1\":3}", "c-run", "reply/run", null);

        verify(output, timeout(WAIT.toMillis())).finish();
        final JsonNode answer = answer(0);
        assertThat(answer.has(MethodRequestFieldsOpcUa.ERROR_CODE)).isFalse();
        assertThat(answer.has(MethodRequestFieldsOpcUa.STATUS_INFO)).isFalse();
        assertThat(answer.path(MethodRequestFieldsOpcUa.CORRELATION_DATA).asText())
                .isEqualTo("c-run");
        assertThat(published(0).path("output1").asInt()).isEqualTo(9);
        assertThat(namespace().countedCalls()).isEqualTo(1);
    }

    /// A refusal the adapter makes itself, before the server, also carries its reason in `statusInfo.reason`:
    /// here a method name the object does not have.
    @Test
    void anAdapterRefusalBeforeTheServer_carriesItsReason() throws Exception {
        final OpcUaProtocolAdapter adapter = started();

        final WritingOutput refused = write(adapter, "NoSuchMethod", "{}", "c-unknown", "reply/unknown", null);

        verify(refused, timeout(WAIT.toMillis())).fail(anyString());
        final JsonNode answer = answer(0);
        assertThat(answer.path(MethodRequestFieldsOpcUa.ERROR_CODE).asText()).isNotBlank();
        assertThat(answer.path(MethodRequestFieldsOpcUa.CORRELATION_DATA).asText())
                .isEqualTo("c-unknown");
        assertThat(answer.path(MethodRequestFieldsOpcUa.STATUS_INFO)
                        .path(MethodRequestFieldsOpcUa.REASON)
                        .asText())
                .contains("NoSuchMethod");

        assertOnlyTheControlRuns(adapter);
    }

    /// Sends an unmarked request on the same adapter, waits for the server's answer, and asserts that the
    /// server ran the method exactly once in the whole test.
    private void assertOnlyTheControlRuns(final @NotNull OpcUaProtocolAdapter adapter) throws Exception {
        final WritingOutput control = write(adapter, "Square", "{\"input1\":4}", "c-control", "reply/control", null);

        verify(control, timeout(WAIT.toMillis())).finish();
        assertThat(answer(1).path(MethodRequestFieldsOpcUa.CORRELATION_DATA).asText())
                .isEqualTo("c-control");
        assertThat(published(1).path("output1").asInt()).isEqualTo(16);
        assertThat(streaming.published()).hasSize(2);
        assertThat(namespace().countedCalls()).isEqualTo(1);
    }

    private @NotNull TestNamespace namespace() {
        return Objects.requireNonNull(opcUaServerExtension.getTestNamespace());
    }

    private @NotNull OpcUaProtocolAdapter started() {
        final String object = namespace().addCountingMethodObject("Counter").toParseableString();
        final OpcuaTag tag = new OpcuaTag(TAG_NAME, "", new OpcuaTagDefinition(object, OpcuaTagKind.METHOD));

        final ProtocolAdapterInformation adapterInformation = mock(ProtocolAdapterInformation.class);
        when(adapterInformation.getProtocolId()).thenReturn("opcua");

        @SuppressWarnings("unchecked") // Mockito cannot mock a generic type without the unchecked conversion.
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
        when(input.getTags()).thenReturn(new ArrayList<Tag>(List.of(tag)));
        when(input.adapterFactories()).thenReturn(mock(AdapterFactories.class));
        when(input.getProtocolAdapterMetricsHelper()).thenReturn(mock(ProtocolAdapterMetricsService.class));
        when(input.moduleServices()).thenReturn(moduleServices);

        final OpcUaProtocolAdapter adapter = new OpcUaProtocolAdapter(adapterInformation, input);
        final ProtocolAdapterStartInput startInput = mock(ProtocolAdapterStartInput.class);
        when(startInput.moduleServices()).thenReturn(moduleServices);
        adapter.start(
                ProtocolAdapterConnectionDirection.Northbound, startInput, mock(ProtocolAdapterStartOutput.class));

        verify(protocolAdapterState, timeout(WAIT.toMillis()))
                .setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
        return adapter;
    }

    /// Hands the adapter a method request as Edge's writer builds it: the arguments in `value`, the request's
    /// parameters beside them in the internal field, and `refusal` among those parameters when given.
    private @NotNull WritingOutput write(
            final @NotNull OpcUaProtocolAdapter adapter,
            final @NotNull String methodName,
            final @NotNull String arguments,
            final @NotNull String correlationData,
            final @NotNull String responseTopic,
            final @Nullable String refusal)
            throws Exception {

        final ObjectNode value = (ObjectNode) mapper.readTree(arguments);
        final ObjectNode parameters = value.putObject(MethodRequestFieldsOpcUa.INTERNAL_REQUEST_PARAMETERS);
        parameters.put(MethodRequestFieldsOpcUa.METHOD_NAME, methodName);
        parameters.put(MethodRequestFieldsOpcUa.CORRELATION_DATA, correlationData);
        parameters.put(MethodRequestFieldsOpcUa.RESPONSE_TOPIC, responseTopic);
        if (refusal != null) {
            parameters.put(MethodRequestFieldsOpcUa.REFUSAL, refusal);
        }

        final WritingContext writingContext = mock(WritingContext.class);
        when(writingContext.getTagName()).thenReturn(TAG_NAME);
        final WritingInput writingInput = mock(WritingInput.class);
        when(writingInput.getWritingContext()).thenReturn(writingContext);
        when(writingInput.getWritingPayload()).thenReturn(OpcUaPayload.ofValue(value));

        final WritingOutput output = mock(WritingOutput.class);
        adapter.write(writingInput, output);
        return output;
    }

    /// The parameters Edge lifts out of the response published at `index`.
    private @NotNull JsonNode answer(final int index) {
        return published(index).path(MethodRequestFieldsOpcUa.INTERNAL_RESPONSE_PARAMETERS);
    }

    /// The response published at `index`, as the adapter published it, once it has been published.
    private @NotNull JsonNode published(final int index) {
        await().atMost(WAIT).until(() -> streaming.published().size() > index);
        return streaming.published().get(index);
    }
}
