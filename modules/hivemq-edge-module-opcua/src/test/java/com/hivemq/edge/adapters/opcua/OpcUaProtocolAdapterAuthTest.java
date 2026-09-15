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
import com.hivemq.edge.adapters.opcua.config.AllowList;
import com.hivemq.edge.adapters.opcua.config.Auth;
import com.hivemq.edge.adapters.opcua.config.BasicAuth;
import com.hivemq.edge.adapters.opcua.config.Keystore;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.SecPolicy;
import com.hivemq.edge.adapters.opcua.config.Security;
import com.hivemq.edge.adapters.opcua.config.Tls;
import com.hivemq.edge.adapters.opcua.config.TlsChecks;
import com.hivemq.edge.adapters.opcua.config.X509Auth;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.listeners.OpcUaSessionActivityListener;
import com.hivemq.edge.adapters.opcua.security.AllowListCertificateValidator;
import com.hivemq.edge.adapters.opcua.security.CertificateFingerprints;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import util.EmbeddedOpcUaServerExtension;
import util.KeyChain;

class OpcUaProtocolAdapterAuthTest {

    @RegisterExtension
    public final @NotNull EmbeddedOpcUaServerExtension opcUaServerExtension = new EmbeddedOpcUaServerExtension();

    @TempDir
    Path tempDir;

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
        final Tls tls = new Tls(true, TlsChecks.NONE, null, null, null, null);
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
                null,
                new Keystore(keystore.getAbsolutePath(), "password", "password"),
                null,
                null);
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

    // ----- EDG-585: the no-CA environment, end to end -----

    /**
     * EDG-585 / Miele repro: the OPC UA server presents a self-signed certificate that cannot be
     * loaded as a trust anchor, so chain validation against the JVM cacerts cannot succeed under a
     * non-None security policy. The preset {@code NO_VERIFICATION} accepts it and the adapter connects.
     *
     * <p>The {@link EmbeddedOpcUaServerExtension} produces exactly such a self-signed certificate (its
     * builder adds no KeyUsage extension), making this a faithful repro of the customer environment.
     */
    @Test
    @Timeout(30)
    public void whenNoVerification_andServerCertNotChainable_thenConnectSuccessfully() throws Exception {
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
                    TlsChecks.NO_VERIFICATION,
                    null,
                    new Keystore(clientKeystore.getAbsolutePath(), "password", "password"),
                    null, // no user truststore - would otherwise fall back to JVM cacerts
                    null);
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

            // Visibility: a WARN at adapter start, naming the adapter id, the endpoint and the mode.
            assertThat(initWarn.list)
                    .as("init WARN must surface trust mode ANY_CERT with adapter id and URI")
                    .anySatisfy(event -> {
                        assertThat(event.getLevel()).isEqualTo(Level.WARN);
                        final String message = event.getFormattedMessage();
                        assertThat(message).contains("ANY_CERT");
                        assertThat(message).contains(opcUaServerExtension.getServerUri());
                    });

            // Visibility: the same WARN must also fire on every successful connect, so an operator
            // running without trust cannot miss it during incident triage.
            assertThat(connectWarn.list)
                    .as("per-connect WARN must surface trust mode ANY_CERT with adapter id and URI")
                    .anySatisfy(event -> {
                        assertThat(event.getLevel()).isEqualTo(Level.WARN);
                        final String message = event.getFormattedMessage();
                        assertThat(message).contains("ANY_CERT");
                        assertThat(message).contains(opcUaServerExtension.getServerUri());
                    });
        } finally {
            detachAppender(OpcUaClientConnection.class, initWarn);
            detachAppender(OpcUaSessionActivityListener.class, connectWarn);
        }
    }

    /**
     * The recommendation for the Miele class of environment: trust the specific self-signed server by
     * SHA-256 fingerprint. Unlike accepting any certificate, this still detects the server being
     * replaced — and it requires no CA, which is the whole constraint.
     */
    @Test
    @Timeout(30)
    public void whenSelfSignedPreset_andFingerprintListed_thenConnectSuccessfully() throws Exception {
        final KeyChain clientKeyChain = KeyChain.createKeyChain("client");
        final var clientKeystore =
                clientKeyChain.wrapInKeyStoreWithPrivateKey("client-keystore", "client", "password", "password");
        opcUaServerExtension.addTrustedClientCertificate(clientKeyChain.getRootCertificate());

        final Path allowListFile = tempDir.resolve("allow-list.txt");
        Files.writeString(
                allowListFile,
                "# the factory machine, fingerprint provided by the vendor\n"
                        + CertificateFingerprints.toDisplayForm(
                                CertificateFingerprints.fingerprintOf(opcUaServerExtension.getServerCertificate()))
                        + "\n");

        // SELF_SIGNED asserts hostname and ApplicationUri too, and the embedded server's certificate
        // carries both, so the connection exercises the identity checks rather than skipping them.
        final Tls tls = new Tls(
                true,
                TlsChecks.SELF_SIGNED,
                null,
                new Keystore(clientKeystore.getAbsolutePath(), "password", "password"),
                null,
                new AllowList(allowListFile.toString()));
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

        await().until(() ->
                CONNECTED == protocolAdapter.getProtocolAdapterState().getConnectionStatus());
    }

    /**
     * The allow-list must actually gate: a server whose fingerprint is not listed is refused, and the
     * fingerprint it presented is logged in the form the allow-list file accepts, so an operator who
     * recognises the server can enrol it out of band.
     */
    @Test
    @Timeout(20)
    public void whenSelfSignedPreset_andFingerprintNotListed_thenConnectionFailsAndLogsSeenFingerprint()
            throws Exception {
        final KeyChain clientKeyChain = KeyChain.createKeyChain("client");
        final var clientKeystore =
                clientKeyChain.wrapInKeyStoreWithPrivateKey("client-keystore", "client", "password", "password");
        opcUaServerExtension.addTrustedClientCertificate(clientKeyChain.getRootCertificate());

        // A syntactically valid fingerprint that belongs to some other machine.
        final Path allowListFile = tempDir.resolve("allow-list-wrong.txt");
        Files.writeString(allowListFile, "0".repeat(64) + "\n");

        final String expectedFingerprint = CertificateFingerprints.toDisplayForm(
                CertificateFingerprints.fingerprintOf(opcUaServerExtension.getServerCertificate()));

        final ListAppender<ILoggingEvent> rejectLog = attachAppender(AllowListCertificateValidator.class);
        try {
            final Tls tls = new Tls(
                    true,
                    TlsChecks.SELF_SIGNED,
                    null,
                    new Keystore(clientKeystore.getAbsolutePath(), "password", "password"),
                    null,
                    new AllowList(allowListFile.toString()));
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

            Thread.sleep(5000);
            assertThat(protocolAdapter.getProtocolAdapterState().getConnectionStatus())
                    .as("adapter must NOT connect to a server whose fingerprint is not in the allow-list")
                    .isNotEqualTo(CONNECTED);

            assertThat(rejectLog.list)
                    .as("the rejection must log the seen fingerprint in the allow-list file format")
                    .anySatisfy(event -> {
                        assertThat(event.getLevel()).isEqualTo(Level.WARN);
                        assertThat(event.getFormattedMessage()).contains(expectedFingerprint);
                    });
        } finally {
            detachAppender(AllowListCertificateValidator.class, rejectLog);
        }
    }

    /**
     * The pre-fix Miele failure mode, preserved by design under the default preset: an adapter that has
     * not opted out of chain validation must not connect to a server it cannot chain to.
     */
    @Test
    @Timeout(15)
    public void whenDefaultPreset_andServerCertNotChainable_thenConnectionFails() throws Exception {
        final KeyChain clientKeyChain = KeyChain.createKeyChain("client");
        final var clientKeystore =
                clientKeyChain.wrapInKeyStoreWithPrivateKey("client-keystore", "client", "password", "password");

        // Server-side trust is set up so the client cert is accepted; the failure we want to observe
        // is purely the client refusing the server's self-signed cert.
        opcUaServerExtension.addTrustedClientCertificate(clientKeyChain.getRootCertificate());

        final ListAppender<ILoggingEvent> hintLog = attachAppender(ParsedConfig.class);
        try {
            // Neither knob set: the adapter behaves exactly as it did before EDG-585.
            final Tls tls = new Tls(
                    true,
                    null,
                    null,
                    new Keystore(clientKeystore.getAbsolutePath(), "password", "password"),
                    null, // no user truststore - falls back to JVM cacerts, which lacks the self-signed cert
                    null);
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
                    .as("adapter must NOT reach CONNECTED under the default preset when the self-signed "
                            + "server certificate cannot chain to the JVM cacerts")
                    .isNotEqualTo(CONNECTED);

            // An operator hitting the Miele failure mode must find the way out in the log, without
            // reading source.
            assertThat(hintLog.list)
                    .as("operator-facing INFO must name the fingerprint route out of this failure")
                    .anySatisfy(event -> {
                        final String message = event.getFormattedMessage();
                        assertThat(message).contains("SELF_SIGNED");
                        assertThat(message).contains("cacerts");
                    });
        } finally {
            detachAppender(ParsedConfig.class, hintLog);
        }
    }

    private static @NotNull ListAppender<ILoggingEvent> attachAppender(final @NotNull Class<?> loggerClass) {
        final Logger logger = (Logger) LoggerFactory.getLogger(loggerClass);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        // A live OPC UA connection emits per-connect WARNs on background threads. Back the appender
        // with a thread-safe list so assertions that iterate it cannot hit ConcurrentModificationException.
        appender.list = new CopyOnWriteArrayList<>();
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
