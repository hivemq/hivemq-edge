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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.datapoint.DataPointBuilder;
import com.hivemq.adapter.sdk.api.datapoint.DataPointListBuilder;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingInput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingOutput;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.adapters.snmp.config.SnmpSpecificAdapterConfig;
import com.hivemq.edge.adapters.snmp.config.SnmpVersion;
import com.hivemq.edge.adapters.snmp.config.tag.SnmpTag;
import com.hivemq.edge.adapters.snmp.config.tag.SnmpTagDefinition;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.mp.StatusInformation;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

/**
 * Smoke tests that spin up an embedded SNMP4J agent and exercise the real SnmpClient
 * against it, then run a full adapter poll cycle to verify end-to-end data flow.
 */
@SuppressWarnings("unchecked")
class SnmpAdapterSmokeTest {

    // OIDs served by the embedded agent
    private static final String OID_SYS_DESCR = "1.3.6.1.2.1.1.1.0";
    private static final String OID_SYS_UPTIME = "1.3.6.1.2.1.1.3.0";
    private static final String OID_SYS_NAME = "1.3.6.1.2.1.1.5.0";

    private static final Map<String, Variable> OID_VALUES = Map.of(
            OID_SYS_DESCR, new OctetString("HiveMQ SNMP Test Agent"),
            OID_SYS_UPTIME, new TimeTicks(123456),
            OID_SYS_NAME, new OctetString("test-device"));

    private static Snmp agentSnmp;
    private static int agentPort;

    @BeforeAll
    static void startEmbeddedAgent() throws Exception {
        final DefaultUdpTransportMapping transport = new DefaultUdpTransportMapping(new UdpAddress("127.0.0.1/0"));
        agentSnmp = new Snmp(transport);

        // CommandResponder is non-generic; processPdu() carries the type parameter on the method
        agentSnmp.addCommandResponder(new CommandResponder() {
            @Override
            public <A extends Address> void processPdu(final CommandResponderEvent<A> event) {
                final PDU pdu = event.getPDU();
                if (pdu == null || pdu.getType() != PDU.GET) {
                    return;
                }

                final PDU response = new PDU();
                response.setType(PDU.RESPONSE);
                response.setRequestID(pdu.getRequestID());
                response.setErrorStatus(PDU.noError);
                response.setErrorIndex(0);

                for (final VariableBinding vb : pdu.getVariableBindings()) {
                    final String oid = vb.getOid().toString();
                    final Variable value = OID_VALUES.getOrDefault(oid, new OctetString("unknown"));
                    response.add(new VariableBinding(new OID(oid), value));
                }

                try {
                    event.getMessageDispatcher()
                            .returnResponsePdu(
                                    event.getMessageProcessingModel(),
                                    event.getSecurityModel(),
                                    event.getSecurityName(),
                                    event.getSecurityLevel(),
                                    response,
                                    event.getMaxSizeResponsePDU(),
                                    event.getStateReference(),
                                    new StatusInformation());
                    event.setProcessed(true);
                } catch (final Exception e) {
                    throw new RuntimeException("Failed to send SNMP response", e);
                }
            }
        });

        agentSnmp.listen();

        agentPort = ((UdpAddress) transport.getListenAddress()).getPort();
    }

    @AfterAll
    static void stopEmbeddedAgent() throws Exception {
        if (agentSnmp != null) {
            agentSnmp.close();
        }
    }

    // -------------------------------------------------------------------------
    // SnmpClient — unit-level with real network I/O
    // -------------------------------------------------------------------------

    @Test
    void snmpClient_get_sysDescr_returnsExpectedString() throws Exception {
        try (final SnmpClient client = new SnmpClient(config())) {
            final SnmpReadResult result = client.get(OID_SYS_DESCR);

            assertThat(result.getValue()).isEqualTo("HiveMQ SNMP Test Agent");
            assertThat(result.getRawType()).isEqualTo("OctetString");
            assertThat(result.getErrorStatus()).isEqualTo(PDU.noError);
        }
    }

    @Test
    void snmpClient_get_sysUpTime_returnsDoubleSeconds() throws Exception {
        try (final SnmpClient client = new SnmpClient(config())) {
            final SnmpReadResult result = client.get(OID_SYS_UPTIME);

            // TimeTicks(123456) hundredths-of-a-second → 1234.56 seconds
            assertThat(result.getValue()).isEqualTo(123456L / 100.0);
        }
    }

    @Test
    void snmpClient_testConnection_returnsTrueWhenAgentReachable() throws Exception {
        try (final SnmpClient client = new SnmpClient(config())) {
            assertThat(client.testConnection()).isTrue();
        }
    }

    // -------------------------------------------------------------------------
    // Full adapter poll cycle
    // -------------------------------------------------------------------------

    @Test
    void adapterPoll_publishesDataPointsFromRealAgent() throws Exception {
        // Set up mock SDK objects
        final ProtocolAdapterInput<SnmpSpecificAdapterConfig> adapterInput = mock(ProtocolAdapterInput.class);
        final ProtocolAdapterState state = mock(ProtocolAdapterState.class);
        final ProtocolAdapterStartInput startInput = mock(ProtocolAdapterStartInput.class);
        final ProtocolAdapterStartOutput startOutput = mock(ProtocolAdapterStartOutput.class);
        final BatchPollingInput pollingInput = mock(BatchPollingInput.class);
        final BatchPollingOutput pollingOutput = mock(BatchPollingOutput.class);
        final DataPointListBuilder publisher = mock(DataPointListBuilder.class);
        final DataPointBuilder<DataPointListBuilder> dpBuilder = mock(DataPointBuilder.class);
        final DataPointBuilder.ObjectBuilder<DataPointBuilder<DataPointListBuilder>> metaBuilder =
                mock(DataPointBuilder.ObjectBuilder.class);

        final SnmpTag sysDescrTag = new SnmpTag("sysDescr", "desc", new SnmpTagDefinition(OID_SYS_DESCR, null));
        final SnmpTag sysNameTag = new SnmpTag("sysName", "name", new SnmpTagDefinition(OID_SYS_NAME, null));

        when(adapterInput.getAdapterId()).thenReturn("smoke-snmp");
        when(adapterInput.getProtocolAdapterState()).thenReturn(state);
        when(adapterInput.getConfig()).thenReturn(config());
        when(adapterInput.getTags()).thenReturn((List) List.of(sysDescrTag, sysNameTag));

        when(pollingOutput.dataPointListPublisher()).thenReturn(publisher);
        when(publisher.addDataPoint(any())).thenReturn(dpBuilder);
        when(dpBuilder.startObjectMetadata()).thenReturn(metaBuilder);
        when(metaBuilder.put(anyString(), anyString())).thenReturn(metaBuilder);
        when(metaBuilder.put(anyString(), anyInt())).thenReturn(metaBuilder);
        when(metaBuilder.endObject()).thenReturn(dpBuilder);
        when(dpBuilder.value(anyString())).thenReturn(dpBuilder);
        when(dpBuilder.value(anyInt())).thenReturn(dpBuilder);
        when(dpBuilder.value(any(Long.class))).thenReturn(dpBuilder);
        when(dpBuilder.value(any(Double.class))).thenReturn(dpBuilder);
        when(dpBuilder.valueNull()).thenReturn(dpBuilder);

        final SnmpProtocolAdapter adapter =
                new SnmpProtocolAdapter(mock(ProtocolAdapterInformation.class), adapterInput);

        adapter.start(startInput, startOutput);
        verify(startOutput).startedSuccessfully();

        adapter.poll(pollingInput, pollingOutput);

        // Both tags should have been added and the batch published
        verify(publisher).addDataPoint(sysDescrTag);
        verify(publisher).addDataPoint(sysNameTag);
        verify(publisher).publish();
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static @NotNull SnmpSpecificAdapterConfig config() {
        final SnmpSpecificAdapterConfig cfg = mock(SnmpSpecificAdapterConfig.class);
        when(cfg.getHost()).thenReturn("127.0.0.1");
        when(cfg.getPort()).thenReturn(agentPort);
        when(cfg.getSnmpVersion()).thenReturn(SnmpVersion.V2C);
        when(cfg.getCommunity()).thenReturn("public");
        when(cfg.getTimeoutMillis()).thenReturn(2000);
        when(cfg.getRetries()).thenReturn(1);
        return cfg;
    }
}
