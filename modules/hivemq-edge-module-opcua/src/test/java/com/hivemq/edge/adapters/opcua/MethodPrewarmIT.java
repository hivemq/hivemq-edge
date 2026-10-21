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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import util.EmbeddedOpcUaServerExtension;
import util.TestNamespace;

/// Reading a method tag's methods at connect, instead of in the caller's first request.
///
/// **Why this exists.** A method tag's methods used to be read on the first request against that tag. On a
/// device that serves metadata slowly that browse takes minutes, so the first request paid for it and could
/// time out while a later one succeeded -- and waiting after connect was no remedy, because nothing read
/// anything until a request arrived. Part of Miguel's M11 on EDG-1087, where the slow-connect symptom
/// reproduced on two unrelated devices.
///
/// **Why against a real server.** The claim is that the browse happens without a request, which only a server
/// can settle: a mocked client would make the browse whatever the mock says it is, and the thing under test is
/// precisely that nobody asked for it.
class MethodPrewarmIT {

    private static final @NotNull String ADAPTER_ID = "opcua-method-prewarm";

    @RegisterExtension
    public final @NotNull EmbeddedOpcUaServerExtension opcUaServerExtension = new EmbeddedOpcUaServerExtension();

    private final @NotNull ProtocolAdapterStateImpl protocolAdapterState = mock(ProtocolAdapterStateImpl.class);
    private final @NotNull ModuleServices moduleServices = mock(ModuleServices.class);
    private final @NotNull FakeEventService eventService = new FakeEventService();

    @BeforeEach
    void setUp() {
        when(moduleServices.eventService()).thenReturn(eventService);
        when(moduleServices.protocolAdapterTagStreamingService())
                .thenReturn(mock(ProtocolAdapterTagStreamingService.class));
    }

    private @NotNull TestNamespace namespace() {
        return Objects.requireNonNull(opcUaServerExtension.getTestNamespace());
    }

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

        verify(protocolAdapterState, timeout(30_000))
                .setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
        return adapter;
    }

    /// Waits for the pre-warm run the readiness publication started.
    ///
    /// Nothing in production waits for this -- that is the point of it -- so the wait exists only here.
    private static void awaitPrewarm(final @NotNull OpcUaProtocolAdapter adapter) throws Exception {
        Future<?> run = adapter.methodPrewarmRun();
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (run == null && System.nanoTime() < deadline) {
            Thread.sleep(25);
            run = adapter.methodPrewarmRun();
        }
        assertThat(run)
                .as("the readiness publication should have started a pre-warm run")
                .isNotNull();
        run.get(30, TimeUnit.SECONDS);
    }

    /// Waits for a pre-warm run that is not the one given, and returns it.
    private static @NotNull Future<?> awaitDifferentPrewarm(
            final @NotNull OpcUaProtocolAdapter adapter, final @Nullable Future<?> previous) throws Exception {

        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        Future<?> run = adapter.methodPrewarmRun();
        while (run == previous && System.nanoTime() < deadline) {
            Thread.sleep(25);
            run = adapter.methodPrewarmRun();
        }
        assertThat(run).as("a new pre-warm run should have been submitted").isNotSameAs(previous);
        Objects.requireNonNull(run).get(30, TimeUnit.SECONDS);
        return run;
    }

    private static @NotNull OpcuaTag methodTag(final @NotNull String name, final @NotNull String node) {
        return new OpcuaTag(name, "", new OpcuaTagDefinition(node, OpcuaTagKind.METHOD));
    }

    @Test
    void aMethodTagsMethodsAreKnownAfterConnect_withoutAnyRequest() throws Exception {
        final String object = namespace().addMethodObject("Boiler").toParseableString();
        final OpcUaProtocolAdapter adapter = started(methodTag("boiler", object));

        awaitPrewarm(adapter);

        // The whole claim: no request was made, and the methods are known anyway.
        assertThat(adapter.methodsKnownFor("boiler")).isTrue();
    }

    @Test
    void everyMethodTagIsPrewarmed_notOnlyTheFirst() throws Exception {
        final String first = namespace().addMethodObject("BoilerA").toParseableString();
        final String second = namespace().addMethodObject("BoilerB").toParseableString();
        final OpcUaProtocolAdapter adapter = started(methodTag("boilerA", first), methodTag("boilerB", second));

        awaitPrewarm(adapter);

        assertThat(adapter.methodsKnownFor("boilerA")).isTrue();
        assertThat(adapter.methodsKnownFor("boilerB")).isTrue();
        // The line a user waits for before sending method requests.
        assertThat(lastLifecycleLine())
                .endsWith("connection 1; read the methods of 2 of 2 method tags; state now - value reads: working, "
                        + "value writes: working, method requests: working");
    }

    @Test
    void aValueTagIsNotPrewarmed() throws Exception {
        // Only method tags have methods to read. A value tag must not provoke a browse that can only fail.
        final String variable = namespace().addNode("aVariable", NodeIds.Int32, () -> 1, 7_101L);
        final String object = namespace().addMethodObject("Boiler").toParseableString();
        final OpcUaProtocolAdapter adapter = started(
                new OpcuaTag("plain", "", new OpcuaTagDefinition(variable, OpcuaTagKind.VALUE)),
                methodTag("boiler", object));

        awaitPrewarm(adapter);

        assertThat(adapter.methodsKnownFor("boiler")).isTrue();
        assertThat(adapter.methodsKnownFor("plain")).isFalse();
    }

    @Test
    void aSessionReplacedInsideTheClient_prewarmsAgain() throws Exception {
        // The seam that invalidation alone used to leave empty. Milo activates a fresh session inside the
        // existing client on a reconnect, so no connection is swapped and the readiness hook -- which runs once
        // per connection object -- never fires again. Wiring that hook to invalidation alone therefore cleared
        // the cache and left nothing to refill it, and a slow device's first request paid for the browse again:
        // the case the pre-warm exists for. Found by Miguel in round 2 of EDG-1087.
        final String object = namespace().addMethodObject("Boiler").toParseableString();
        final OpcUaProtocolAdapter adapter = started(methodTag("boiler", object));

        awaitPrewarm(adapter);
        assertThat(adapter.methodsKnownFor("boiler")).isTrue();
        final Future<?> firstRun = adapter.methodPrewarmRun();

        // Exactly what the connection's reconnect hook invokes.
        adapter.onSessionReplaced();

        // A *different* run, not the completed first one: the field still holds that until the new task is
        // submitted, so waiting on "whatever is there" would prove nothing.
        final Future<?> secondRun = awaitDifferentPrewarm(adapter, firstRun);

        assertThat(secondRun).isNotSameAs(firstRun);
        assertThat(adapter.methodsKnownFor("boiler")).isTrue();
    }

    @Test
    void aTagNamingANodeTheServerDoesNotHave_doesNotStopTheRest() throws Exception {
        // A pre-warm failure is logged and nothing more: the adapter is connected and streaming, and the
        // request path reports the fault to whoever asks. What must not happen is one bad tag costing the
        // others their pre-warm, so the good tag is listed second deliberately.
        final String object = namespace().addMethodObject("Boiler").toParseableString();
        final OpcUaProtocolAdapter adapter = started(
                methodTag("ghost", "ns=" + namespace().getNamespaceIndex() + ";s=NoSuchObject"),
                methodTag("boiler", object));

        awaitPrewarm(adapter);

        assertThat(adapter.methodsKnownFor("boiler")).isTrue();
        // A node the server does not have is a verdict the cache keeps, so a request against 'ghost' is refused at
        // once rather than delayed. Reported as refused, not as read (Miguel, round 2 on #1772), and method
        // requests are working, because no request has to read methods itself.
        assertThat(lastLifecycleLine())
                .contains("connection 1; PROBLEM: read the methods of 1 of 2 method tags. Unusable, method requests "
                        + "to them are refused: 'ghost'; ")
                .endsWith("state now - value reads: working, value writes: working, method requests: working");
    }

    @Test
    void aTagNamingANamespaceTheServerDoesNotPublish_isReportedAsRefusedNotAsSlow() throws Exception {
        // Found by Miguel on #1772, together with #1771: a node id that cannot be used is never read, and a request
        // to it is refused rather than reading the methods first. The line must not promise the second.
        final String object = namespace().addMethodObject("Boiler").toParseableString();
        final OpcUaProtocolAdapter adapter = started(
                methodTag("lost", "nsu=urn:not:published:by:this:server;s=Boiler"), methodTag("boiler", object));

        awaitPrewarm(adapter);

        assertThat(adapter.methodsKnownFor("boiler")).isTrue();
        assertThat(lastLifecycleLine())
                .contains("connection 1; PROBLEM: read the methods of 1 of 2 method tags. Unusable, method requests "
                        + "to them are refused: 'lost'; state now - ")
                .doesNotContain("read the methods first");
    }

    private @NotNull String lastLifecycleLine() {
        final List<String> lines = eventService.readEvents(null, null).stream()
                .map(event -> event.getMessage())
                .filter(message -> message.startsWith(OpcUaProtocolAdapter.LIFECYCLE_PREFIX))
                .toList();
        assertThat(lines).as("lifecycle lines").isNotEmpty();
        return lines.getLast();
    }
}
