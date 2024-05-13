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

import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterInput;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterStartInput;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterStartOutput;
import com.hivemq.edge.modules.adapters.services.ModuleServices;
import com.hivemq.edge.modules.events.EventService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import util.EmbeddedOpcUaServerExtension;

import static com.hivemq.edge.modules.adapters.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("NullabilityAnnotations")
class OpcUaProtocolAdapterAuthTest {

    @RegisterExtension
    public final @NotNull EmbeddedOpcUaServerExtension opcUaServerExtension = new EmbeddedOpcUaServerExtension();

    private final @NotNull ProtocolAdapterInput<OpcUaAdapterConfig> protocolAdapterInput = mock();

    @BeforeEach
    void setUp() {
        when(protocolAdapterInput.getProtocolAdapterState()).thenReturn(new ProtocolAdapterStateImpl(mock()));
    }

    @Test
    @Timeout(10)
    public void whenNoAuthAndNoSubscriptions_thenConnectSuccessfully() throws Exception {
        final OpcUaAdapterConfig config = new OpcUaAdapterConfig("test", opcUaServerExtension.getServerUri());
        when(protocolAdapterInput.getConfig()).thenReturn(config);
        final OpcUaProtocolAdapter protocolAdapter =
                new OpcUaProtocolAdapter(OpcUaProtocolAdapterInformation.INSTANCE, protocolAdapterInput);

        final ProtocolAdapterStartInput in = new TestProtocolAdapterStartInput();
        final ProtocolAdapterStartOutput out = mock(ProtocolAdapterStartOutput.class);
        protocolAdapter.start(in, out).get();

        await().until(() -> CONNECTED == protocolAdapter.getProtocolAdapterState().getConnectionStatus());
    }

    @Test
    @Timeout(10)
    public void whenBasicAuthAndNoSubscriptions_thenConnectSuccessfully() throws Exception {
        final OpcUaAdapterConfig config = new OpcUaAdapterConfig("test", opcUaServerExtension.getServerUri());
        config.setAuth(new OpcUaAdapterConfig.Auth(new OpcUaAdapterConfig.BasicAuth("testuser", "testpass"), null));
        when(protocolAdapterInput.getConfig()).thenReturn(config);
        final OpcUaProtocolAdapter protocolAdapter =
                new OpcUaProtocolAdapter(OpcUaProtocolAdapterInformation.INSTANCE, protocolAdapterInput);

        final ProtocolAdapterStartInput in = new TestProtocolAdapterStartInput();
        final ProtocolAdapterStartOutput out = mock(ProtocolAdapterStartOutput.class);
        protocolAdapter.start(in, out).get();

        await().until(() -> CONNECTED == protocolAdapter.getProtocolAdapterState().getConnectionStatus());
    }

    @Test
    @Timeout(10)
    public void whenTlsAndNoSubscriptions_thenConnectSuccessfully() throws Exception {
        final OpcUaAdapterConfig config = new OpcUaAdapterConfig("test", opcUaServerExtension.getServerUri());
        config.setSecurity(new OpcUaAdapterConfig.Security(OpcUaAdapterConfig.SecPolicy.NONE));
        config.setTls(new OpcUaAdapterConfig.Tls(true, null, null));
        when(protocolAdapterInput.getConfig()).thenReturn(config);

        final OpcUaProtocolAdapter protocolAdapter =
                new OpcUaProtocolAdapter(OpcUaProtocolAdapterInformation.INSTANCE, protocolAdapterInput);

        final ProtocolAdapterStartInput in = new TestProtocolAdapterStartInput();
        final ProtocolAdapterStartOutput out = mock(ProtocolAdapterStartOutput.class);
        protocolAdapter.start(in, out).get();

        await().until(() -> CONNECTED == protocolAdapter.getProtocolAdapterState().getConnectionStatus());
    }

    @Test
    @Timeout(10)
    public void whenCertAuthAndNoSubscriptions_thenConnectSuccessfully() throws Exception {
        final OpcUaAdapterConfig config = new OpcUaAdapterConfig("test", opcUaServerExtension.getServerUri());
        config.setAuth(new OpcUaAdapterConfig.Auth(null, new OpcUaAdapterConfig.X509Auth()));
        when(protocolAdapterInput.getConfig()).thenReturn(config);

        final OpcUaProtocolAdapter protocolAdapter =
                new OpcUaProtocolAdapter(OpcUaProtocolAdapterInformation.INSTANCE, protocolAdapterInput);

        final ProtocolAdapterStartInput in = new TestProtocolAdapterStartInput();
        final ProtocolAdapterStartOutput out = mock(ProtocolAdapterStartOutput.class);
        protocolAdapter.start(in, out).get();

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
