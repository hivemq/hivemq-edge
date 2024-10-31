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

import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterPublishService;
import com.hivemq.edge.adapters.opcua.config.Auth;
import com.hivemq.edge.adapters.opcua.config.BasicAuth;
import com.hivemq.edge.adapters.opcua.config.OpcUaAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.SecPolicy;
import com.hivemq.edge.adapters.opcua.config.Security;
import com.hivemq.edge.adapters.opcua.config.Tls;
import com.hivemq.edge.adapters.opcua.config.X509Auth;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
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

    private final @NotNull ProtocolAdapterInput<OpcUaAdapterConfig> protocolAdapterInput = mock();

    @BeforeEach
    void setUp() {
        when(protocolAdapterInput.getProtocolAdapterState()).thenReturn(new ProtocolAdapterStateImpl(mock(),
                "id",
                "protocolId"));
        final ModuleServices moduleServices = mock();
        when(moduleServices.eventService()).thenReturn(mock(EventService.class));
        when(moduleServices.adapterPublishService()).thenReturn(mock(ProtocolAdapterPublishService.class));
        when(protocolAdapterInput.moduleServices()).thenReturn(moduleServices);
    }

    @Test
    @Timeout(10)
    public void whenNoAuthAndNoSubscriptions_thenConnectSuccessfully() {
        final OpcUaAdapterConfig config = new OpcUaAdapterConfig("test",
                opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                new OpcUaToMqttConfig(List.of()),
                null,
                List.of());

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
    public void whenBasicAuthAndNoSubscriptions_thenConnectSuccessfully() {
        final Auth auth = new Auth(new BasicAuth("testuser", "testpass"), null);
        final OpcUaAdapterConfig config = new OpcUaAdapterConfig("test",
                opcUaServerExtension.getServerUri(),
                false,
                auth,
                null,
                null,
                null,
                List.of());

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
        final OpcUaAdapterConfig config = new OpcUaAdapterConfig("test",
                opcUaServerExtension.getServerUri(),
                false,
                null,
                tls,
                null,
                security,
                List.of());

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
        final OpcUaAdapterConfig config = new OpcUaAdapterConfig("test",
                opcUaServerExtension.getServerUri(),
                false,
                auth,
                null,
                null,
                null,
                List.of());

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
