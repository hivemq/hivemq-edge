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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.ProtocolAdapterConnectionDirection;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.adapters.opcua.config.ConnectionOptions;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagKind;
import com.hivemq.edge.modules.adapters.data.TagManager;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingService;
import com.hivemq.protocols.InternalProtocolAdapterWritingService;
import com.hivemq.protocols.ProtocolAdapterConfig;
import com.hivemq.protocols.ProtocolAdapterWrapper;
import com.hivemq.protocols.northbound.NorthboundConsumerFactory;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;

/**
 * Review-05 finding 9: two REFRESH tags fail the adapter, on purpose.
 * <p>
 * A second refresh tag places a second monitored item on the Server object, and the refresh bracket is
 * copied to every notifier item in a subscription — so both would publish the same events and an operator
 * would see what looks like two refreshes for every one that happened.
 * <p>
 * <b>The behaviour was contested and then decided, and nothing pinned it either way.</b> The review argued
 * for dropping the extra tag and starting, on the strength of Edge's own per-tag doctrine — an unresolvable
 * notifier drops one tag and leaves the adapter running. Martin's 2026-08-12 comment settles it the other
 * way, from a distinction rather than from the particular case: a configuration that is not <em>valid</em>
 * is decidable by reading the configuration alone, with no device involved and no round trip that could
 * answer differently, and that stops the adapter; a valid tag that cannot be <em>verified against the
 * device</em> is a reasonable statement about the world that turned out to be false, and that is dropped on
 * its own. Two REFRESH tags fall on the first side. {@code NotifierResolver} falls on the second by
 * construction, so the comparison between them does not carry.
 * <p>
 * A decision reached that way is exactly the kind that needs a test: it is deliberate, it is not obvious,
 * and the argument against it is a good one. Without this, the next reader to notice the asymmetry could
 * "fix" it and no build would object.
 */
class DuplicateRefreshTagTest {

    private static final @NotNull String ADAPTER_ID = "boiler-house";

    private @NotNull ProtocolAdapterStateImpl protocolAdapterState;
    private @NotNull FakeEventService eventService;
    private @NotNull ModuleServices moduleServices;
    private @Nullable OpcUaProtocolAdapter adapter;

    @BeforeEach
    void setUp() {
        eventService = new FakeEventService();
        protocolAdapterState = new ProtocolAdapterStateImpl(eventService, ADAPTER_ID, "opcua");
        moduleServices = mock(ModuleServices.class);
        when(moduleServices.eventService()).thenReturn(eventService);
        when(moduleServices.protocolAdapterTagStreamingService())
                .thenReturn(mock(ProtocolAdapterTagStreamingService.class));
    }

    @AfterEach
    void tearDown() {
        if (adapter != null) {
            adapter.destroy();
        }
    }

    @Test
    @Timeout(60)
    void twoRefreshTagsFailTheStartAndNameBoth() {
        // The diagnostic is the whole value of failing here rather than at subscribe time: the operator has
        // to be able to find the tags, and "an adapter failed to start" would send them looking at the
        // device. Both names, because deleting either one fixes it and Edge has no basis to prefer one.
        // Names chosen so neither assertion can be satisfied by the other tag: "refresh" as a substring of
        // "refresh-copied-by-mistake" would have made the first check pass on a message naming only one.
        final ProtocolAdapterStartOutput output = start(
                refreshTag("plant-wide-refresh"), conditionTag("boiler-high-temp"), refreshTag("copied-by-mistake"));

        final ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(output).failStart(any(), message.capture());
        assertThat(message.getValue())
                .contains(ADAPTER_ID)
                .contains("plant-wide-refresh")
                .contains("copied-by-mistake")
                .contains("at most one REFRESH tag")
                .as("the condition tag is not at fault and must not be named")
                .doesNotContain("boiler-high-temp");
        verify(output, never()).startedSuccessfully();
    }

    @Test
    @Timeout(60)
    void wrapperPreservesTheConfigurationErrorWhileCleaningUpTheFailedStart() {
        final ProtocolAdapterWrapper wrapper =
                wrapperFor(refreshTag("plant-wide-refresh"), refreshTag("copied-by-mistake"));

        assertThat(wrapper.start()).isFalse();
        assertThat(wrapper.getConnectionStatus())
                .as("failed-start cleanup must not turn an actionable configuration error into an offline state")
                .isEqualTo(com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.ERROR);
        assertThat(wrapper.getErrorMessage()).contains("at most one REFRESH tag");
    }

    @Test
    @Timeout(60)
    void andTheRefusalCostsNothingThatHasToBeCollected() {
        // Why the check sits where it does. It runs before ParsedConfig.fromConfig and before any connection
        // object exists, so a rejected start leaves no executor behind -- the failure paths below it do not
        // shut schedulers down, and repeated invalid starts used to accumulate two threads apiece.
        start(refreshTag("refresh"), refreshTag("refresh-again"));

        assertThat(scheduler("retryScheduler"))
                .as("a start refused for an invalid configuration must not have created anything")
                .isNull();
        assertThat(scheduler("healthCheckScheduler")).isNull();
    }

    @Test
    @Timeout(60)
    void andTheAdapterCanStillBeStartedOnceTheConfigurationIsCorrected() {
        // The lifecycle must not be claimed by a start that was refused. The same instance is reused across
        // a configuration change, so an operator deleting the extra tag has to be able to start it -- an
        // adapter permanently unstartable after one bad edit would be a worse failure than the one being
        // prevented.
        final OpcUaProtocolAdapter refused = adapterFor(refreshTag("refresh"), refreshTag("refresh-again"));
        refused.start(
                ProtocolAdapterConnectionDirection.Northbound, startInput(), mock(ProtocolAdapterStartOutput.class));

        final OpcUaProtocolAdapter corrected = adapterFor(refreshTag("refresh"));
        final ProtocolAdapterStartOutput output = mock(ProtocolAdapterStartOutput.class);
        corrected.start(ProtocolAdapterConnectionDirection.Northbound, startInput(), output);

        verify(output).startedSuccessfully();
        verify(output, never()).failStart(any(), any());
    }

    @Test
    @Timeout(60)
    void oneRefreshTagIsFine() {
        // The other side of "at most one", so the check cannot be satisfied by refusing every refresh tag.
        final ProtocolAdapterStartOutput output = start(refreshTag("refresh"), conditionTag("boiler-high-temp"));

        verify(output).startedSuccessfully();
        verify(output, never()).failStart(any(), any());
    }

    @Test
    @Timeout(60)
    void andSoIsNone() {
        final ProtocolAdapterStartOutput output = start(conditionTag("boiler-high-temp"));

        verify(output).startedSuccessfully();
    }

    @Test
    @Timeout(60)
    void aTagThatCannotBeVerifiedAgainstTheDeviceIsNotThisRule() {
        // The distinction the policy rests on, stated as a test rather than only as a comment. A condition
        // tag naming a node this server does not have is a perfectly valid configuration whose claim about
        // the world is false -- so it is the resolver's business, per tag, and the adapter still starts.
        // There is no server here at all, which is the point: the answer cannot depend on one.
        final ProtocolAdapterStartOutput output =
                start(conditionTag("boiler-high-temp"), conditionTag("nothing-like-this-exists"));

        verify(output).startedSuccessfully();
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────

    private @NotNull ProtocolAdapterStartOutput start(final @NotNull OpcuaTag... tags) {
        final ProtocolAdapterStartOutput output = mock(ProtocolAdapterStartOutput.class);
        adapterFor(tags).start(ProtocolAdapterConnectionDirection.Northbound, startInput(), output);
        return output;
    }

    private @NotNull ProtocolAdapterWrapper wrapperFor(final @NotNull OpcuaTag... tags) {
        final OpcUaProtocolAdapter created = adapterFor(tags);
        final ProtocolAdapterFactory<?> adapterFactory = mock();
        return new ProtocolAdapterWrapper(
                created,
                mock(ProtocolAdapterConfig.class),
                adapterFactory,
                created.getProtocolAdapterInformation(),
                mock(ProtocolAdapterMetricsService.class),
                protocolAdapterState,
                mock(ProtocolAdapterPollingService.class),
                eventService,
                moduleServices,
                mock(TagManager.class),
                mock(NorthboundConsumerFactory.class),
                mock(InternalProtocolAdapterWritingService.class),
                Runnable::run);
    }

    private @NotNull OpcUaProtocolAdapter adapterFor(final @NotNull OpcuaTag... tags) {
        final ProtocolAdapterInformation adapterInformation = mock(ProtocolAdapterInformation.class);
        when(adapterInformation.getProtocolId()).thenReturn("opcua");

        @SuppressWarnings("unchecked")
        final ProtocolAdapterInput<OpcUaSpecificAdapterConfig> input = mock(ProtocolAdapterInput.class);
        when(input.getAdapterId()).thenReturn(ADAPTER_ID);
        when(input.getProtocolAdapterState()).thenReturn(protocolAdapterState);
        when(input.getConfig()).thenReturn(adapterConfig());
        when(input.getTags()).thenReturn(new ArrayList<Tag>(List.of(tags)));
        when(input.adapterFactories()).thenReturn(mock(AdapterFactories.class));
        when(input.getProtocolAdapterMetricsHelper()).thenReturn(mock(ProtocolAdapterMetricsService.class));
        when(input.moduleServices()).thenReturn(moduleServices);

        final OpcUaProtocolAdapter created = new OpcUaProtocolAdapter(adapterInformation, input);
        adapter = created;
        return created;
    }

    private @NotNull ProtocolAdapterStartInput startInput() {
        final ProtocolAdapterStartInput startInput = mock(ProtocolAdapterStartInput.class);
        when(startInput.moduleServices()).thenReturn(moduleServices);
        return startInput;
    }

    /**
     * An address with no server behind it. The connection attempt fails, which is the ordinary "hardware is
     * not online yet" path — and every question here is answered before a connection is attempted at all.
     */
    private static @NotNull OpcUaSpecificAdapterConfig adapterConfig() {
        return new OpcUaSpecificAdapterConfig(
                "opc.tcp://127.0.0.1:4840",
                false,
                null,
                null,
                null,
                OpcUaToMqttConfig.defaultOpcUaToMqttConfig(),
                null,
                ConnectionOptions.defaultConnectionOptions());
    }

    private static @NotNull OpcuaTag refreshTag(final @NotNull String name) {
        return new OpcuaTag(name, "", new OpcuaTagDefinition("ns=0;i=2253", OpcuaTagKind.REFRESH));
    }

    private static @NotNull OpcuaTag conditionTag(final @NotNull String name) {
        return new OpcuaTag(name, "", new OpcuaTagDefinition("ns=2;s=" + name, OpcuaTagKind.CONDITION));
    }

    private @Nullable Object scheduler(final @NotNull String name) {
        try {
            final Field field = OpcUaProtocolAdapter.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(java.util.Objects.requireNonNull(adapter));
        } catch (final ReflectiveOperationException e) {
            throw new LinkageError("the adapter no longer schedules on a '" + name + "'", e);
        }
    }
}
