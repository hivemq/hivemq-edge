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
package com.hivemq.edge.adapters.snmp;

import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.datapoint.DataPointBuilder;
import com.hivemq.adapter.sdk.api.datapoint.DataPointListBuilder;
import com.hivemq.adapter.sdk.api.discovery.NodeTree;
import com.hivemq.adapter.sdk.api.discovery.NodeType;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryInput;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingInput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingOutput;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.adapters.snmp.config.SnmpSpecificAdapterConfig;
import com.hivemq.edge.adapters.snmp.config.SnmpVersion;
import com.hivemq.edge.adapters.snmp.config.tag.SnmpTag;
import com.hivemq.edge.adapters.snmp.config.tag.SnmpTagDefinition;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.DISCONNECTED;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class SnmpProtocolAdapterTest {

    private ProtocolAdapterInput<SnmpSpecificAdapterConfig> adapterInput;
    private ProtocolAdapterInformation information;
    private SnmpSpecificAdapterConfig config;
    private ProtocolAdapterState state;
    private BatchPollingInput pollingInput;
    private BatchPollingOutput pollingOutput;
    private ProtocolAdapterStartInput startInput;
    private ProtocolAdapterStartOutput startOutput;
    private ProtocolAdapterStopInput stopInput;
    private ProtocolAdapterStopOutput stopOutput;
    private SnmpClient snmpClient;
    private DataPointListBuilder publisher;
    private DataPointBuilder<DataPointListBuilder> dpBuilder;
    private DataPointBuilder.ObjectBuilder<DataPointBuilder<DataPointListBuilder>> metaBuilder;

    @BeforeEach
    void setUp() {
        adapterInput = mock(ProtocolAdapterInput.class);
        information = mock(ProtocolAdapterInformation.class);
        config = mock(SnmpSpecificAdapterConfig.class);
        state = mock(ProtocolAdapterState.class);
        pollingInput = mock(BatchPollingInput.class);
        pollingOutput = mock(BatchPollingOutput.class);
        startInput = mock(ProtocolAdapterStartInput.class);
        startOutput = mock(ProtocolAdapterStartOutput.class);
        stopInput = mock(ProtocolAdapterStopInput.class);
        stopOutput = mock(ProtocolAdapterStopOutput.class);
        snmpClient = mock(SnmpClient.class);
        publisher = mock(DataPointListBuilder.class);
        dpBuilder = mock(DataPointBuilder.class);
        metaBuilder = mock(DataPointBuilder.ObjectBuilder.class);

        when(adapterInput.getAdapterId()).thenReturn("test-snmp");
        when(adapterInput.getProtocolAdapterState()).thenReturn(state);
        when(adapterInput.getConfig()).thenReturn(config);
        when(adapterInput.getTags()).thenReturn(List.of());
        when(config.getHost()).thenReturn("127.0.0.1");
        when(config.getPort()).thenReturn(161);
        when(config.getSnmpVersion()).thenReturn(SnmpVersion.V2C);
        when(config.getCommunity()).thenReturn("public");
        when(config.getSnmpToMqttConfig()).thenReturn(null);

        // Wire up the DataPointBuilder chain so none of the calls NPE
        when(pollingOutput.dataPointListPublisher()).thenReturn(publisher);
        when(publisher.addDataPoint(any())).thenReturn(dpBuilder);
        when(dpBuilder.startObjectMetadata()).thenReturn(metaBuilder);
        when(metaBuilder.put(anyString(), anyString())).thenReturn(metaBuilder);
        when(metaBuilder.put(anyString(), anyInt())).thenReturn(metaBuilder);
        when(metaBuilder.endObject()).thenReturn(dpBuilder);
        when(dpBuilder.value(anyInt())).thenReturn(dpBuilder);
        when(dpBuilder.value(anyLong())).thenReturn(dpBuilder);
        when(dpBuilder.value(anyDouble())).thenReturn(dpBuilder);
        when(dpBuilder.value(anyString())).thenReturn(dpBuilder);
        when(dpBuilder.valueNull()).thenReturn(dpBuilder);
    }

    // -------------------------------------------------------------------------
    // start()
    // -------------------------------------------------------------------------

    @Test
    void start_whenConnectionSucceeds_setsConnectedAndSignalsSuccess() {
        when(snmpClient.testConnection()).thenReturn(true);

        final SnmpProtocolAdapter adapter = createAdapter();
        adapter.start(startInput, startOutput);

        verify(state).setConnectionStatus(CONNECTED);
        verify(startOutput).startedSuccessfully();
    }

    @Test
    void start_whenConnectionTestFails_setsErrorAndFailsStart() {
        when(snmpClient.testConnection()).thenReturn(false);

        final SnmpProtocolAdapter adapter = createAdapter();
        adapter.start(startInput, startOutput);

        verify(state).setConnectionStatus(ERROR);
        verify(startOutput).failStart(any(RuntimeException.class), anyString());
    }

    @Test
    void start_whenCalledTwice_secondCallFails() {
        when(snmpClient.testConnection()).thenReturn(true);

        final SnmpProtocolAdapter adapter = createAdapter();
        adapter.start(startInput, startOutput);
        adapter.start(startInput, startOutput);

        // Only one successful start; second call should fail
        verify(startOutput, times(1)).startedSuccessfully();
        verify(startOutput, times(1)).failStart(any(IllegalStateException.class), anyString());
    }

    // -------------------------------------------------------------------------
    // stop()
    // -------------------------------------------------------------------------

    @Test
    void stop_afterStart_closesClientAndSetsDisconnected() throws IOException {
        when(snmpClient.testConnection()).thenReturn(true);

        final SnmpProtocolAdapter adapter = createAdapter();
        adapter.start(startInput, startOutput);
        adapter.stop(stopInput, stopOutput);

        verify(snmpClient).close();
        verify(state).setConnectionStatus(DISCONNECTED);
        verify(stopOutput).stoppedSuccessfully();
    }

    // -------------------------------------------------------------------------
    // poll()
    // -------------------------------------------------------------------------

    @Test
    void poll_whenAdapterNotStarted_failsImmediately() {
        final SnmpProtocolAdapter adapter = createAdapter();

        adapter.poll(pollingInput, pollingOutput);

        verify(pollingOutput).fail(any(IllegalStateException.class), anyString());
        verify(publisher, never()).publish();
    }

    @Test
    void poll_whenNoTags_publishesWithoutAddingDataPoints() {
        when(snmpClient.testConnection()).thenReturn(true);

        final SnmpProtocolAdapter adapter = createAdapter(List.of());
        adapter.start(startInput, startOutput);
        adapter.poll(pollingInput, pollingOutput);

        verify(publisher).publish();
        verify(publisher, never()).addDataPoint(any());
    }

    @Test
    void poll_withOneTag_addsDataPointAndPublishes() throws IOException {
        when(snmpClient.testConnection()).thenReturn(true);
        when(snmpClient.get("1.3.6.1.2.1.1.1.0"))
                .thenReturn(new SnmpReadResult("HiveMQ Edge", "OctetString", 0, 0));

        final SnmpTag tag = sysDescrTag();
        final SnmpProtocolAdapter adapter = createAdapter(List.of(tag));
        adapter.start(startInput, startOutput);
        adapter.poll(pollingInput, pollingOutput);

        verify(publisher).addDataPoint(tag);
        verify(dpBuilder).value("HiveMQ Edge");
        verify(dpBuilder).endDataPoint();
        verify(publisher).publish();
        verify(state, times(2)).setConnectionStatus(CONNECTED);
    }

    @Test
    void poll_withIntegerValue_usesTypedIntOverload() throws IOException {
        when(snmpClient.testConnection()).thenReturn(true);
        when(snmpClient.get("1.3.6.1.2.1.1.7.0"))
                .thenReturn(new SnmpReadResult(72, "Integer32", 0, 0));

        final SnmpTag tag = new SnmpTag("sysServices", "", new SnmpTagDefinition("1.3.6.1.2.1.1.7.0", null));
        final SnmpProtocolAdapter adapter = createAdapter(List.of(tag));
        adapter.start(startInput, startOutput);
        adapter.poll(pollingInput, pollingOutput);

        verify(dpBuilder).value(72);
    }

    @Test
    void poll_withLongValue_usesTypedLongOverload() throws IOException {
        when(snmpClient.testConnection()).thenReturn(true);
        when(snmpClient.get("1.3.6.1.2.1.31.1.1.1.10.1"))
                .thenReturn(new SnmpReadResult(4_000_000_000L, "Counter64", 0, 0));

        final SnmpTag tag = new SnmpTag("ifHCInOctets", "", new SnmpTagDefinition("1.3.6.1.2.1.31.1.1.1.10.1", null));
        final SnmpProtocolAdapter adapter = createAdapter(List.of(tag));
        adapter.start(startInput, startOutput);
        adapter.poll(pollingInput, pollingOutput);

        verify(dpBuilder).value(4_000_000_000L);
    }

    @Test
    void poll_whenOneTagFails_otherTagsAreStillPublished() throws IOException {
        when(snmpClient.testConnection()).thenReturn(true);
        when(snmpClient.get("1.3.6.1.2.1.1.1.0")).thenThrow(new IOException("SNMP timeout"));
        when(snmpClient.get("1.3.6.1.2.1.1.5.0"))
                .thenReturn(new SnmpReadResult("my-device", "OctetString", 0, 0));

        final SnmpTag failingTag = sysDescrTag();
        final SnmpTag successTag = sysNameTag();
        final SnmpProtocolAdapter adapter = createAdapter(List.of(failingTag, successTag));
        adapter.start(startInput, startOutput);
        adapter.poll(pollingInput, pollingOutput);

        verify(publisher, never()).addDataPoint(failingTag);
        verify(publisher).addDataPoint(successTag);
        verify(publisher).publish();
    }

    // -------------------------------------------------------------------------
    // discoverValues()
    // -------------------------------------------------------------------------

    @Test
    void discoverValues_atRootLevel_addsSixMib2Folders() {
        final ProtocolAdapterDiscoveryInput discoveryInput = mock(ProtocolAdapterDiscoveryInput.class);
        final ProtocolAdapterDiscoveryOutput discoveryOutput = mock(ProtocolAdapterDiscoveryOutput.class);
        final NodeTree nodeTree = mock(NodeTree.class);

        when(discoveryInput.getRootNode()).thenReturn(null);
        when(discoveryOutput.getNodeTree()).thenReturn(nodeTree);

        final SnmpProtocolAdapter adapter = createAdapter();
        adapter.discoverValues(discoveryInput, discoveryOutput);

        verify(nodeTree, times(6)).addNode(anyString(), anyString(), anyString(), anyString(),
                isNull(), eq(NodeType.FOLDER), eq(false));
        verify(discoveryOutput).finish();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private @NotNull SnmpProtocolAdapter createAdapter() {
        return createAdapter(List.of());
    }

    private @NotNull SnmpProtocolAdapter createAdapter(final @NotNull List<SnmpTag> tags) {
        when(adapterInput.getTags()).thenReturn((List) tags);
        return new TestableAdapter(information, adapterInput, snmpClient);
    }

    private static @NotNull SnmpTag sysDescrTag() {
        return new SnmpTag("sysDescr", "System description", new SnmpTagDefinition("1.3.6.1.2.1.1.1.0", null));
    }

    private static @NotNull SnmpTag sysNameTag() {
        return new SnmpTag("sysName", "System name", new SnmpTagDefinition("1.3.6.1.2.1.1.5.0", null));
    }

    /** Subclass that injects the mock SnmpClient instead of opening a real UDP socket. */
    private static class TestableAdapter extends SnmpProtocolAdapter {

        private final @NotNull SnmpClient injectedClient;

        TestableAdapter(
                final @NotNull ProtocolAdapterInformation information,
                final @NotNull ProtocolAdapterInput<SnmpSpecificAdapterConfig> input,
                final @NotNull SnmpClient injectedClient) {
            super(information, input);
            this.injectedClient = injectedClient;
        }

        @Override
        protected @NotNull SnmpClient createClient() {
            return injectedClient;
        }
    }
}
