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

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.hivemq.adapter.sdk.api.ProtocolAdapterConnectionDirection;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterPublishService;
import com.hivemq.edge.adapters.opcua.client.ParsedConfig;
import com.hivemq.edge.adapters.opcua.config.Auth;
import com.hivemq.edge.adapters.opcua.config.BasicAuth;
import com.hivemq.edge.adapters.opcua.config.Keystore;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.SecPolicy;
import com.hivemq.edge.adapters.opcua.config.Security;
import com.hivemq.edge.adapters.opcua.config.Tls;
import com.hivemq.edge.adapters.opcua.config.TlsChecks;
import com.hivemq.edge.adapters.opcua.config.TrustLevel;
import com.hivemq.edge.adapters.opcua.config.X509Auth;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.listeners.OpcUaSessionActivityListener;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;
import util.EmbeddedOpcUaServerExtension;
import util.KeyChain;

class OpcUaProtocolAdapterAuthTest {

    @RegisterExtension
    public final @NotNull EmbeddedOpcUaServerExtension opcUaServerExtension = new EmbeddedOpcUaServerExtension();

    private final @NotNull ProtocolAdapterInput<OpcUaSpecificAdapterConfig> protocolAdapterInput = mock();

    private ModuleServices moduleServices;

    @BeforeEach
    void setUp() {
        when(protocolAdapterInput.getProtocolAdapterState())
                .thenReturn(new ProtocolAdapterStateImpl(mock(), "id", "protocolId"));
        moduleServices = mock();
        when(moduleServices.adapterPublishService()).thenReturn(mock(ProtocolAdapterPublishService.class));
        when(moduleServices.eventService()).thenReturn(new FakeEventService());
        when(moduleServices.adapterPublishService()).thenReturn(mock(ProtocolAdapterPublishService.class));
        when(protocolAdapterInput.moduleServices()).thenReturn(moduleServices);
        final var metricsService = mock(ProtocolAdapterMetricsService.class);
        when(protocolAdapterInput.getProtocolAdapterMetricsHelper()).thenReturn(metricsService);
        when(protocolAdapterInput.getAdapterId()).thenReturn("id");
    }

    @Test
    @Timeout(30)
    public void whenNoAuthAndNoSubscriptions_thenConnectSuccessfully() {
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, 1000),
                null,
                null);

        when(protocolAdapterInput.getConfig()).thenReturn(config);
        when(protocolAdapterInput.getPollingContexts()).thenReturn(List.of());

        final OpcUaProtocolAdapter protocolAdapter =
                new OpcUaProtocolAdapter(OpcUaProtocolAdapterInformation.INSTANCE, protocolAdapterInput);

        final ProtocolAdapterStartInput in = new TestProtocolAdapterStartInput(moduleServices);
        final ProtocolAdapterStartOutput out = mock(ProtocolAdapterStartOutput.class);
        protocolAdapter.start(ProtocolAdapterConnectionDirection.Northbound, in, out);

        final var metricsService = mock(ProtocolAdapterMetricsService.class);
        when(protocolAdapterInput.getProtocolAdapterMetricsHelper()).thenReturn(metricsService);
        await().until(() ->
                CONNECTED == protocolAdapter.getProtocolAdapterState().getConnectionStatus());
    }

    @Test
    @Timeout(30)
    public void whenBasicAuthAndNoSubscriptions_thenConnectSuccessfully() {
        final Auth auth = new Auth(new BasicAuth("testuser", "testpass"), null);
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(), false, null, auth, null, null, null, null);

        when(protocolAdapterInput.getConfig()).thenReturn(config);

        final OpcUaProtocolAdapter protocolAdapter =
                new OpcUaProtocolAdapter(OpcUaProtocolAdapterInformation.INSTANCE, protocolAdapterInput);

        final ProtocolAdapterStartInput in = new TestProtocolAdapterStartInput(moduleServices);
        final ProtocolAdapterStartOutput out = mock(ProtocolAdapterStartOutput.class);
        protocolAdapter.start(ProtocolAdapterConnectionDirection.Northbound, in, out);

        await().until(() ->
                CONNECTED == protocolAdapter.getProtocolAdapterState().getConnectionStatus());
    }

    @Test
    @Timeout(30)
    public void whenTlsAndNoSubscriptions_thenConnectSuccessfully() {
        final Security security = new Security(SecPolicy.NONE);
        final Tls tls = new Tls(true, TlsChecks.NONE, null, null, TrustLevel.CHAIN);
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(), false, null, null, tls, null, security, null);
        when(protocolAdapterInput.getConfig()).thenReturn(config);

        final OpcUaProtocolAdapter protocolAdapter =
                new OpcUaProtocolAdapter(OpcUaProtocolAdapterInformation.INSTANCE, protocolAdapterInput);

        final ProtocolAdapterStartInput in = new TestProtocolAdapterStartInput(moduleServices);
        final ProtocolAdapterStartOutput out = mock(ProtocolAdapterStartOutput.class);
        protocolAdapter.start(ProtocolAdapterConnectionDirection.Northbound, in, out);

        await().until(() ->
                CONNECTED == protocolAdapter.getProtocolAdapterState().getConnectionStatus());
    }

    @Test
    @Timeout(30)
    public void whenCertAuthAndNoSubscriptions_thenConnectSuccessfully() throws Exception {
        final Auth auth = new Auth(null, new X509Auth(true));

        final KeyChain root = KeyChain.createKeyChain("root");

        final var keystore = root.wrapInKeyStoreWithPrivateKey("keystore", "root", "password", "password");
        final Tls tls = new Tls(
                true,
                TlsChecks.NONE,
                new Keystore(keystore.getAbsolutePath(), "password", "password"),
                null,
                TrustLevel.CHAIN);
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(), false, null, auth, tls, null, null, null);

        when(protocolAdapterInput.getConfig()).thenReturn(config);

        final OpcUaProtocolAdapter protocolAdapter =
                new OpcUaProtocolAdapter(OpcUaProtocolAdapterInformation.INSTANCE, protocolAdapterInput);

        final ProtocolAdapterStartInput in = new TestProtocolAdapterStartInput(moduleServices);
        final ProtocolAdapterStartOutput out = mock(ProtocolAdapterStartOutput.class);
        protocolAdapter.start(ProtocolAdapterConnectionDirection.Northbound, in, out);

        await().until(() ->
                CONNECTED == protocolAdapter.getProtocolAdapterState().getConnectionStatus());
    }

    // ----- EDG-585: trustLevel=TRUST -----

    /**
     * EDG-585 / Miele repro: an OPC UA server presents a self-signed cert that lacks {@code keyCertSign}
     * (cannot be loaded as a trust anchor). With a non-None security policy and no usable truststore,
     * cert validation cannot succeed against JVM cacerts. Setting {@code trustLevel=TRUST}
     * bypasses chain validation and lets the adapter connect.
     *
     * <p>The {@link EmbeddedOpcUaServerExtension} produces exactly such a self-signed cert (no KeyUsage
     * extension is added by its certificate builder), making it a faithful repro of the customer environment.
     */
    @Test
    @Timeout(30)
    public void whenTrustLevelTrust_andServerCertNotChainable_thenConnectSuccessfully() throws Exception {
        final KeyChain clientKeyChain = KeyChain.createKeyChain("client");
        final var clientKeystore =
                clientKeyChain.wrapInKeyStoreWithPrivateKey("client-keystore", "client", "password", "password");

        // The embedded server's certificate validator must trust the client's app cert; otherwise the
        // handshake fails on the server side, masking the client-side scenario we want to verify.
        opcUaServerExtension.addTrustedClientCertificate(clientKeyChain.getRootCertificate());

        final ListAppender<ILoggingEvent> initWarn = attachAppender(OpcUaClientConnection.class);
        final ListAppender<ILoggingEvent> connectWarn = attachAppender(OpcUaSessionActivityListener.class);
        try {
            final Tls tls = new Tls(
                    true,
                    TlsChecks.NONE,
                    new Keystore(clientKeystore.getAbsolutePath(), "password", "password"),
                    null, // no user truststore — would otherwise fall back to JVM cacerts
                    TrustLevel.TRUST);
            final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                    opcUaServerExtension.getServerUri(),
                    false,
                    null,
                    null,
                    tls,
                    new OpcUaToMqttConfig(1, 1000),
                    new Security(SecPolicy.BASIC256SHA256), // requires server cert validation
                    null);

            when(protocolAdapterInput.getConfig()).thenReturn(config);

            final OpcUaProtocolAdapter protocolAdapter =
                    new OpcUaProtocolAdapter(OpcUaProtocolAdapterInformation.INSTANCE, protocolAdapterInput);

            final ProtocolAdapterStartInput in = new TestProtocolAdapterStartInput(moduleServices);
            final ProtocolAdapterStartOutput out = mock(ProtocolAdapterStartOutput.class);
            protocolAdapter.start(ProtocolAdapterConnectionDirection.Northbound, in, out);

            await().until(() ->
                    CONNECTED == protocolAdapter.getProtocolAdapterState().getConnectionStatus());

            // EDG-585 visibility: an init-time WARN must be emitted at adapter start, naming the
            // adapter id, the endpoint URI, and the flag.
            assertThat(initWarn.list)
                    .as("init WARN must surface trustLevel=TRUST with adapter id and URI")
                    .anySatisfy(event -> {
                        assertThat(event.getLevel()).isEqualTo(Level.WARN);
                        final String message = event.getFormattedMessage();
                        assertThat(message).contains("trustLevel=TRUST");
                        assertThat(message).contains("id");
                        assertThat(message).contains(opcUaServerExtension.getServerUri());
                    });

            // EDG-585 visibility: the same WARN must also fire on every successful connect, so an
            // operator running insecurely cannot miss it during incident triage.
            assertThat(connectWarn.list)
                    .as("per-connect WARN must surface trustLevel=TRUST with adapter id and URI")
                    .anySatisfy(event -> {
                        assertThat(event.getLevel()).isEqualTo(Level.WARN);
                        final String message = event.getFormattedMessage();
                        assertThat(message).contains("trustLevel=TRUST");
                        assertThat(message).contains("id");
                        assertThat(message).contains(opcUaServerExtension.getServerUri());
                    });
        } finally {
            detachAppender(OpcUaClientConnection.class, initWarn);
            detachAppender(OpcUaSessionActivityListener.class, connectWarn);
        }
    }

    /**
     * EDG-585 test #5: documents the pre-fix Miele failure mode is preserved when
     * {@code trustLevel=CHAIN} (the default). Same setup as the success test above minus the explicit
     * opt-in: the adapter must NOT reach CONNECTED.
     */
    @Test
    @Timeout(15)
    public void whenTrustLevelChain_andServerCertNotChainable_thenConnectionFails() throws Exception {
        final KeyChain clientKeyChain = KeyChain.createKeyChain("client");
        final var clientKeystore =
                clientKeyChain.wrapInKeyStoreWithPrivateKey("client-keystore", "client", "password", "password");

        // Server-side trust is set up so the client cert is accepted; the failure we want to observe
        // is purely the client refusing the server's self-signed cert.
        opcUaServerExtension.addTrustedClientCertificate(clientKeyChain.getRootCertificate());

        final ListAppender<ILoggingEvent> hintLog = attachAppender(ParsedConfig.class);
        try {
            final Tls tls = new Tls(
                    true,
                    TlsChecks.NONE,
                    new Keystore(clientKeystore.getAbsolutePath(), "password", "password"),
                    null, // no user truststore — falls back to JVM cacerts (won't contain the self-signed server cert)
                    TrustLevel.CHAIN); // default trust: today's failing behavior preserved
            final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                    opcUaServerExtension.getServerUri(),
                    false,
                    null,
                    null,
                    tls,
                    new OpcUaToMqttConfig(1, 1000),
                    new Security(SecPolicy.BASIC256SHA256),
                    null);

            when(protocolAdapterInput.getConfig()).thenReturn(config);

            final OpcUaProtocolAdapter protocolAdapter =
                    new OpcUaProtocolAdapter(OpcUaProtocolAdapterInformation.INSTANCE, protocolAdapterInput);

            final ProtocolAdapterStartInput in = new TestProtocolAdapterStartInput(moduleServices);
            final ProtocolAdapterStartOutput out = mock(ProtocolAdapterStartOutput.class);
            protocolAdapter.start(ProtocolAdapterConnectionDirection.Northbound, in, out);

            // Allow the adapter to attempt a few reconnects, then verify it never reached CONNECTED.
            Thread.sleep(5000);
            assertThat(protocolAdapter.getProtocolAdapterState().getConnectionStatus())
                    .as("Adapter must NOT reach CONNECTED when trustLevel=CHAIN (default) and "
                            + "the self-signed server cert cannot chain to JVM cacerts")
                    .isNotEqualTo(CONNECTED);

            // EDG-585 test #5: an operator-facing log message must point at the bypass, so an
            // operator hitting the Miele failure mode can find it without reading source.
            assertThat(hintLog.list)
                    .as("operator-facing INFO must mention trustLevel=TRUST as the bypass")
                    .anySatisfy(event -> {
                        final String message = event.getFormattedMessage();
                        assertThat(message).contains("trustLevel=TRUST");
                        assertThat(message).contains("cacerts");
                    });
        } finally {
            detachAppender(ParsedConfig.class, hintLog);
        }
    }

    private static @NotNull ListAppender<ILoggingEvent> attachAppender(final @NotNull Class<?> loggerClass) {
        final Logger logger = (Logger) LoggerFactory.getLogger(loggerClass);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private static void detachAppender(
            final @NotNull Class<?> loggerClass, final @NotNull ListAppender<ILoggingEvent> appender) {
        ((Logger) LoggerFactory.getLogger(loggerClass)).detachAppender(appender);
        appender.stop();
    }

    private static class TestProtocolAdapterStartInput implements ProtocolAdapterStartInput {

        private final @NotNull ModuleServices moduleServices;

        TestProtocolAdapterStartInput(final @NotNull ModuleServices moduleServices) {
            this.moduleServices = moduleServices;
        }

        @Override
        public @NotNull ModuleServices moduleServices() {
            return moduleServices;
        }
    }
}
