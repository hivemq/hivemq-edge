/*
 * Copyright 2019-present HiveMQ GmbH
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
package com.hivemq.api.resources.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.bridge.BridgeService;
import com.hivemq.bridge.config.BridgeTls;
import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.reader.BridgeExtractor;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.api.model.Bridge;
import com.hivemq.edge.api.model.StatusTransitionCommand;
import com.hivemq.edge.api.model.TlsConfiguration;
import com.hivemq.http.error.ProblemDetails;
import com.hivemq.security.tls.MaterialRef;
import com.hivemq.security.tls.TLSPolicy;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/// What the REST resource does with a bridge's TLS settings. A body is converted to the settings its
/// fields state. Settings the bridge cannot carry out, and a save or restart that would take a
/// running bridge down for good, are refused with 400 naming `tlsConfiguration`, and the
/// configuration and the running bridge stay as they were.
class BridgeResourceImplTlsTest {

    private static final @NotNull String BRIDGE_ID = "rest-bridge";
    private static final @NotNull String PASSWORD = "trust-pw";

    @TempDir
    File temporaryFolder;

    private @NotNull BridgeExtractor bridgeExtractor;
    private @NotNull BridgeService bridgeService;
    private @NotNull BridgeResourceImpl resource;

    @BeforeEach
    void setUp() throws TLSPolicy.TLSMaterialException {
        bridgeExtractor = mock(BridgeExtractor.class);
        when(bridgeExtractor.getBridges()).thenReturn(List.of());
        final ConfigurationService configurationService = mock(ConfigurationService.class);
        when(configurationService.bridgeExtractor()).thenReturn(bridgeExtractor);
        final SystemInformation systemInformation = mock(SystemInformation.class);
        when(systemInformation.isConfigWriteable()).thenReturn(true);
        bridgeService = mock(BridgeService.class);
        doCallRealMethod().when(bridgeService).requireReadableTls(any());
        resource = new BridgeResourceImpl(configurationService, bridgeService, systemInformation);
    }

    // --- what a body converts to ----------------------------------------------------------------------

    /// A new bridge has no settings to keep; its body is derived.
    @Test
    void givenANewBridge_whenSaved_thenTheBodyIsDerived() {
        final TlsConfiguration body = new TlsConfiguration()
                .enabled(true)
                .truststorePath("/tls/authority.jks")
                .truststorePassword(PASSWORD);

        final BridgeTls saved = BridgeResourceImpl.convertTls(BRIDGE_ID, body, null);

        assertThat(saved).isNotNull();
        assertThat(saved.getEffectiveTls()).isNotNull();
        assertThat(saved.getEffectiveTls().truststore()).isPresent();
    }

    /// The container type a REST body states is the type of the store, whatever the file is named.
    @Test
    void givenAKeystoreTypeTheFileNameContradicts_whenSaved_thenTheStatedTypeIsUsed() {
        final TlsConfiguration body = new TlsConfiguration()
                .enabled(true)
                .keystorePath("/tls/client.p12")
                .keystorePassword("store-pw")
                .privateKeyPassword("key-pw")
                .keystoreType("JKS");

        final BridgeTls saved = BridgeResourceImpl.convertTls(BRIDGE_ID, body, null);

        assertThat(saved).isNotNull();
        assertThat(saved.getEffectiveTls()).isNotNull();
        assertThat(saved.getEffectiveTls().identity())
                .get()
                .isInstanceOfSatisfying(MaterialRef.Inline.class, inline -> assertThat(inline.format())
                        .isEqualToIgnoringCase("jks"));
    }

    // --- a store type the bridge cannot read ----------------------------------------------------------

    /// A new bridge whose keystore type no bridge reads is refused with 400 naming the TLS
    /// configuration, and nothing is added.
    @Test
    void givenAKeystoreTypeABridgeDoesNotRead_whenABridgeIsAdded_thenTheAnswerIs400NamingTheTlsConfiguration() {
        final Response response = resource.addBridge(bridgeWith(new TlsConfiguration()
                .enabled(true)
                .keystorePath("/tls/client.p12")
                .keystorePassword("store-pw")
                .privateKeyPassword("key-pw")
                .keystoreType("PEM")));

        assertRefusedNamingTheTlsConfiguration(response);
        verify(bridgeExtractor, never()).addBridge(any(MqttBridge.class));
    }

    /// The same body with a type a bridge reads is accepted and added.
    @Test
    void givenAKeystoreTypeABridgeReads_whenABridgeIsAdded_thenItIsAdded() {
        final Response response = resource.addBridge(bridgeWith(new TlsConfiguration()
                .enabled(true)
                .keystorePath("/tls/client.p12")
                .keystorePassword("store-pw")
                .privateKeyPassword("key-pw")
                .keystoreType("PKCS12")));

        assertThat(response.getStatus()).isEqualTo(200);
        verify(bridgeExtractor).addBridge(any(MqttBridge.class));
    }

    /// An update to a keystore type no bridge reads is refused with 400, and the configured bridge
    /// is not replaced.
    @Test
    void givenAKeystoreTypeABridgeDoesNotRead_whenABridgeIsUpdated_thenTheAnswerIs400AndTheConfigurationIsKept() {
        when(bridgeExtractor.getBridges()).thenReturn(List.of(bridgeWithoutTls()));

        final Response response = resource.updateBridge(
                BRIDGE_ID,
                bridgeWith(new TlsConfiguration()
                        .enabled(true)
                        .keystorePath("/tls/client.p12")
                        .keystorePassword("store-pw")
                        .privateKeyPassword("key-pw")
                        .keystoreType("PEM")));

        assertRefusedNamingTheTlsConfiguration(response);
        verify(bridgeExtractor, never()).replaceBridge(anyString(), any(MqttBridge.class));
    }

    // --- a save or restart that would not come back ---------------------------------------------------

    /// A save that would restart a running bridge on a truststore that cannot be read is refused,
    /// and the configuration is left as it was, so the running bridge stays connected.
    @Test
    void givenARunningBridge_whenSavedWithATruststoreThatCannotBeRead_thenTheSaveIsRefused() {
        when(bridgeExtractor.getBridges()).thenReturn(List.of(bridgeWithoutTls()));
        when(bridgeService.isRunning(BRIDGE_ID)).thenReturn(true);

        final Response response = resource.updateBridge(
                BRIDGE_ID,
                bridgeWith(new TlsConfiguration()
                        .enabled(true)
                        .truststorePath(new File(temporaryFolder, "missing.jks").getAbsolutePath())
                        .truststorePassword(PASSWORD)));

        assertThat(response.getStatus()).isEqualTo(400);
        verify(bridgeExtractor, never()).replaceBridge(anyString(), any(MqttBridge.class));
    }

    /// A restart of a running bridge whose truststore is missing is refused with 400, and the
    /// running bridge is neither restarted nor stopped.
    @Test
    void givenARunningBridgeWhoseTruststoreIsMissing_whenRestarted_thenTheAnswerIs400AndTheBridgeKeepsRunning() {
        final MqttBridge bridge = bridgeRecordingTruststore(new File(temporaryFolder, "missing.jks"));
        when(bridgeExtractor.getBridges()).thenReturn(List.of(bridge));
        when(bridgeService.isRunning(BRIDGE_ID)).thenReturn(true);

        final Response response = resource.transitionBridgeStatus(BRIDGE_ID, restart());

        assertRefusedNamingTheTlsConfiguration(response);
        verify(bridgeService, never()).restartBridge(anyString(), any());
        verify(bridgeService, never()).stopBridgeAndRemoveQueues(anyString());
    }

    /// A restart of a running bridge whose truststore can be read goes ahead.
    @Test
    void givenARunningBridgeWhoseTruststoreIsReadable_whenRestarted_thenTheBridgeIsRestarted() throws Exception {
        final File truststore = new File(temporaryFolder, "trust.jks");
        writeTruststore(truststore);
        final MqttBridge bridge = bridgeRecordingTruststore(truststore);
        when(bridgeExtractor.getBridges()).thenReturn(List.of(bridge));
        when(bridgeService.isRunning(BRIDGE_ID)).thenReturn(true);

        final Response response = resource.transitionBridgeStatus(BRIDGE_ID, restart());

        assertThat(response.getStatus()).isEqualTo(200);
        verify(bridgeService).restartBridge(BRIDGE_ID, bridge);
    }

    /// A bridge that is not running has nothing to lose: its restart starts it, and a start that
    /// fails on the material is what the operator sees in the bridge's status.
    @Test
    void givenABridgeThatIsNotRunning_whenRestartedWithAMissingTruststore_thenTheBridgeIsRestarted() {
        final MqttBridge bridge = bridgeRecordingTruststore(new File(temporaryFolder, "missing.jks"));
        when(bridgeExtractor.getBridges()).thenReturn(List.of(bridge));
        when(bridgeService.isRunning(BRIDGE_ID)).thenReturn(false);

        final Response response = resource.transitionBridgeStatus(BRIDGE_ID, restart());

        assertThat(response.getStatus()).isEqualTo(200);
        verify(bridgeService).restartBridge(BRIDGE_ID, bridge);
    }

    // --- fixtures -----------------------------------------------------------------------------------

    private static void assertRefusedNamingTheTlsConfiguration(final @NotNull Response response) {
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity())
                .isInstanceOfSatisfying(ProblemDetails.class, problem -> assertThat(problem.getErrors())
                        .extracting(com.hivemq.http.error.Error::getParameter)
                        .contains("tlsConfiguration"));
    }

    private static @NotNull StatusTransitionCommand restart() {
        return new StatusTransitionCommand().command(StatusTransitionCommand.CommandEnum.RESTART);
    }

    private static @NotNull Bridge bridgeWith(final @NotNull TlsConfiguration tls) {
        return new Bridge()
                .id(BRIDGE_ID)
                .host("broker.example.com")
                .port(8883)
                .clientId(BRIDGE_ID + "-client")
                .tlsConfiguration(tls);
    }

    /// The configured bridge a save replaces, without TLS.
    private static @NotNull MqttBridge bridgeWithoutTls() {
        return bridgeBuilder().build();
    }

    /// A bridge whose recorded TLS settings name the given truststore, derived as a REST save does.
    private static @NotNull MqttBridge bridgeRecordingTruststore(final @NotNull File truststore) {
        return bridgeBuilder()
                .withBridgeTls(BridgeResourceImpl.convertTls(
                        BRIDGE_ID,
                        new TlsConfiguration()
                                .enabled(true)
                                .truststorePath(truststore.getAbsolutePath())
                                .truststorePassword(PASSWORD)))
                .build();
    }

    private static @NotNull MqttBridge.Builder bridgeBuilder() {
        return new MqttBridge.Builder()
                .withId(BRIDGE_ID)
                .withHost("broker.example.com")
                .withPort(8883)
                .withClientId(BRIDGE_ID + "-client")
                .withLocalSubscriptions(List.of())
                .withRemoteSubscriptions(List.of());
    }

    @SuppressWarnings("JavaUtilDate") // JcaX509v3CertificateBuilder requires java.util.Date.
    private static void writeTruststore(final @NotNull File file) throws Exception {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        final KeyPair pair = generator.generateKeyPair();
        final Instant now = Instant.now();
        final JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                new X500Principal("CN=authority"),
                BigInteger.valueOf(System.nanoTime()),
                Date.from(now.minus(Duration.ofHours(1))),
                Date.from(now.plus(Duration.ofDays(1))),
                new X500Principal("CN=authority"),
                pair.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        final KeyStore store = KeyStore.getInstance("JKS");
        store.load(null, null);
        store.setCertificateEntry(
                "authority",
                new JcaX509CertificateConverter()
                        .getCertificate(
                                builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(pair.getPrivate()))));
        try (final OutputStream out = Files.newOutputStream(file.toPath())) {
            store.store(out, PASSWORD.toCharArray());
        }
    }
}
