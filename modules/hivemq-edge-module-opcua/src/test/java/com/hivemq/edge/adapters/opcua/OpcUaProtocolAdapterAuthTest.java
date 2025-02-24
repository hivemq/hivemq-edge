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

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterPublishService;
import com.hivemq.edge.adapters.opcua.config.Auth;
import com.hivemq.edge.adapters.opcua.config.BasicAuth;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.SecPolicy;
import com.hivemq.edge.adapters.opcua.config.Security;
import com.hivemq.edge.adapters.opcua.config.Tls;
import com.hivemq.edge.adapters.opcua.config.X509Auth;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.modules.adapters.data.DataPointImpl;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import com.hivemq.edge.modules.adapters.impl.factories.AdapterFactoriesImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import util.EmbeddedOpcUaServerExtension;

import java.util.List;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpcUaProtocolAdapterAuthTest {

    @RegisterExtension
    public final @NotNull EmbeddedOpcUaServerExtension opcUaServerExtension = new EmbeddedOpcUaServerExtension();

    private final @NotNull ProtocolAdapterInput<OpcUaSpecificAdapterConfig> protocolAdapterInput = mock();

    @BeforeEach
    void setUp() {
        when(protocolAdapterInput.getProtocolAdapterState()).thenReturn(new ProtocolAdapterStateImpl(mock(),
                "id",
                "protocolId"));
        final ModuleServices moduleServices = mock();
        when(moduleServices.eventService()).thenReturn(mock(EventService.class));
        when(moduleServices.adapterPublishService()).thenReturn(mock(ProtocolAdapterPublishService.class));
        when(protocolAdapterInput.moduleServices()).thenReturn(moduleServices);

        final AdapterFactories adapterFactories = mock(AdapterFactoriesImpl.class);
        when(adapterFactories.dataPointFactory()).thenReturn(new DataPointFactory() {
            @Override
            public @NotNull DataPoint create(final @NotNull String tagName, final @NotNull Object tagValue) {
                return new DataPointImpl(tagName, tagValue);
            }
        });
        when(protocolAdapterInput.adapterFactories()).thenReturn(adapterFactories);
    }

    @Test
    @Timeout(10)
    public void whenNoAuthAndNoSubscriptions_thenConnectSuccessfully() {
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                new OpcUaToMqttConfig(null, null),
                null);

        when(protocolAdapterInput.getConfig()).thenReturn(config);
        when(protocolAdapterInput.getPollingContexts()).thenReturn(List.of());

        final OpcUaProtocolAdapter protocolAdapter =
                new OpcUaProtocolAdapter(OpcUaProtocolAdapterInformation.INSTANCE, protocolAdapterInput);

        final ProtocolAdapterStartInput in = new TestProtocolAdapterStartInput();
        final ProtocolAdapterStartOutput out = mock(ProtocolAdapterStartOutput.class);
        protocolAdapter.start(in, out);

        await().until(() -> CONNECTED == protocolAdapter.getProtocolAdapterState().getConnectionStatus());
    }

    @Test
    @Timeout(10)
    public void whenBasicAuthAndNoSubscriptions_thenConnectSuccessfully() {
        final Auth auth = new Auth(new BasicAuth("testuser", "testpass"), null);
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(),
                false,
                auth,
                null,
                null,
                null);

        when(protocolAdapterInput.getConfig()).thenReturn(config);
        final OpcUaProtocolAdapter protocolAdapter =
                new OpcUaProtocolAdapter(OpcUaProtocolAdapterInformation.INSTANCE, protocolAdapterInput);

        final ProtocolAdapterStartInput in = new TestProtocolAdapterStartInput();
        final ProtocolAdapterStartOutput out = mock(ProtocolAdapterStartOutput.class);
        protocolAdapter.start(in, out);

        await().until(() -> CONNECTED == protocolAdapter.getProtocolAdapterState().getConnectionStatus());
    }

    @Test
    @Timeout(10)
    public void whenTlsAndNoSubscriptions_thenConnectSuccessfully() {
        final Security security = new Security(SecPolicy.NONE);
        final Tls tls = new Tls(true, null, null);
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(),
                false,
                null,
                tls,
                null,
                security);

        when(protocolAdapterInput.getConfig()).thenReturn(config);

        final OpcUaProtocolAdapter protocolAdapter =
                new OpcUaProtocolAdapter(OpcUaProtocolAdapterInformation.INSTANCE, protocolAdapterInput);

        final ProtocolAdapterStartInput in = new TestProtocolAdapterStartInput();
        final ProtocolAdapterStartOutput out = mock(ProtocolAdapterStartOutput.class);
        protocolAdapter.start(in, out);

        await().until(() -> CONNECTED == protocolAdapter.getProtocolAdapterState().getConnectionStatus());
    }

    @Test
    @Timeout(10)
    public void whenCertAuthAndNoSubscriptions_thenConnectSuccessfully() {
        final Auth auth = new Auth(null, new X509Auth(true));
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(),
                false,
                auth,
                null,
                null,
                null);

        when(protocolAdapterInput.getConfig()).thenReturn(config);

        final OpcUaProtocolAdapter protocolAdapter =
                new OpcUaProtocolAdapter(OpcUaProtocolAdapterInformation.INSTANCE, protocolAdapterInput);

        final ProtocolAdapterStartInput in = new TestProtocolAdapterStartInput();
        final ProtocolAdapterStartOutput out = mock(ProtocolAdapterStartOutput.class);
        protocolAdapter.start(in, out);

        await().until(() -> CONNECTED == protocolAdapter.getProtocolAdapterState().getConnectionStatus());

    }

    private static class TestProtocolAdapterStartInput implements ProtocolAdapterStartInput {

        private final @NotNull ModuleServices moduleServices;

        TestProtocolAdapterStartInput() {
            moduleServices = mock(ModuleServices.class);
            when(moduleServices.eventService()).thenReturn(mock(EventService.class));
        }

        @Override
        public @NotNull ModuleServices moduleServices() {
            return moduleServices;
        }
    }
}
