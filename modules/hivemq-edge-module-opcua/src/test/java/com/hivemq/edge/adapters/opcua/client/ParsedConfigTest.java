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
package com.hivemq.edge.adapters.opcua.client;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.hivemq.edge.adapters.opcua.config.AllowList;
import com.hivemq.edge.adapters.opcua.config.EffectiveChecks;
import com.hivemq.edge.adapters.opcua.config.HostnameCheck;
import com.hivemq.edge.adapters.opcua.config.KeyUsageCheck;
import com.hivemq.edge.adapters.opcua.config.Keystore;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.RevocationCheck;
import com.hivemq.edge.adapters.opcua.config.RevocationList;
import com.hivemq.edge.adapters.opcua.config.SanUriCheck;
import com.hivemq.edge.adapters.opcua.config.SecPolicy;
import com.hivemq.edge.adapters.opcua.config.Security;
import com.hivemq.edge.adapters.opcua.config.Tls;
import com.hivemq.edge.adapters.opcua.config.TlsChecks;
import com.hivemq.edge.adapters.opcua.config.TlsChecksFull;
import com.hivemq.edge.adapters.opcua.config.TlsChecksProjection;
import com.hivemq.edge.adapters.opcua.config.TrustMode;
import com.hivemq.edge.adapters.opcua.config.Truststore;
import com.hivemq.edge.adapters.opcua.config.ValidityCheck;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.security.AllowListCertificateValidator;
import com.hivemq.edge.adapters.opcua.security.CheckOnlyCertificateValidator;
import com.hivemq.edge.adapters.opcua.security.TestCertificates;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.List;
import org.eclipse.milo.opcua.sdk.client.identity.AnonymousProvider;
import org.eclipse.milo.opcua.stack.core.security.CertificateValidator;
import org.eclipse.milo.opcua.stack.core.security.DefaultClientCertificateValidator;
import org.eclipse.milo.opcua.stack.core.util.validation.ValidationCheck;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import util.KeyChain;

class ParsedConfigTest {

    private static final String KEYSTORE_PASSWORD = "password";
    private static final String PRIVATE_KEY_PASSWORD = "password";
    private static final String TEST_URI = "opc.tcp://localhost:4840";

    @TempDir
    Path tempDir;

    @Test
    void testFromConfig_withCertificateSanUri_extractsAndStoresUri() throws Exception {
        // Given
        final String domain = "testclient";
        final String expectedUri = "urn:hivemq:edge:" + domain;

        final KeyChain keyChain = KeyChain.createKeyChain(domain);
        final File keystoreFile = keyChain.wrapInKeyStoreWithPrivateKey(
                tempDir.resolve("test-keystore").toString(), domain, KEYSTORE_PASSWORD, PRIVATE_KEY_PASSWORD);

        final OpcUaSpecificAdapterConfig adapterConfig = createAdapterConfig(
                true, // TLS enabled
                keystoreFile.getAbsolutePath(),
                null // No truststore, will use default
                );

        // When
        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(adapterConfig);

        // Then
        assertThat(result).isInstanceOf(Success.class);
        final ParsedConfig parsedConfig = ((Success<ParsedConfig, String>) result).result();

        assertThat(parsedConfig.applicationUri())
                .as("Application URI should be extracted from certificate SAN")
                .isNotNull()
                .isEqualTo(expectedUri);
        assertThat(parsedConfig.tlsEnabled()).isTrue();
        assertThat(parsedConfig.keyPairWithChain()).isNotNull();
        assertThat(parsedConfig.identityProvider()).isInstanceOf(AnonymousProvider.class);
    }

    @Test
    void testFromConfig_noKeystore_applicationUriIsNull() {
        // Given
        final OpcUaSpecificAdapterConfig adapterConfig = createAdapterConfig(
                true, // TLS enabled but no keystore
                null, null);

        // When
        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(adapterConfig);

        // Then
        assertThat(result).isInstanceOf(Success.class);
        final ParsedConfig parsedConfig = ((Success<ParsedConfig, String>) result).result();

        assertThat(parsedConfig.applicationUri())
                .as("Application URI should be null when no keystore is provided")
                .isNull();
        assertThat(parsedConfig.keyPairWithChain()).isNull();
    }

    @Test
    void testFromConfig_tlsDisabled_applicationUriIsNull() {
        // Given
        final OpcUaSpecificAdapterConfig adapterConfig = createAdapterConfig(
                false, // TLS disabled
                null, null);

        // When
        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(adapterConfig);

        // Then
        assertThat(result).isInstanceOf(Success.class);
        final ParsedConfig parsedConfig = ((Success<ParsedConfig, String>) result).result();

        assertThat(parsedConfig.applicationUri())
                .as("Application URI should be null when TLS is disabled")
                .isNull();
        assertThat(parsedConfig.tlsEnabled()).isFalse();
        assertThat(parsedConfig.keyPairWithChain()).isNull();
    }

    @Test
    void testFromConfig_multipleCertificates_extractsCorrectUri() throws Exception {
        // Given
        final String domain1 = "client1";
        final String domain2 = "client2";
        final String expectedUri = "urn:hivemq:edge:" + domain1;

        final KeyChain keyChain = KeyChain.createKeyChain(domain1, domain2);
        final File keystoreFile = keyChain.wrapInKeyStoreWithPrivateKey(
                tempDir.resolve("test-keystore-multi").toString(),
                domain1, // First domain is used for key entry
                KEYSTORE_PASSWORD,
                PRIVATE_KEY_PASSWORD);

        final OpcUaSpecificAdapterConfig adapterConfig =
                createAdapterConfig(true, keystoreFile.getAbsolutePath(), null);

        // When
        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(adapterConfig);

        // Then
        assertThat(result).isInstanceOf(Success.class);
        final ParsedConfig parsedConfig = ((Success<ParsedConfig, String>) result).result();

        assertThat(parsedConfig.applicationUri())
                .as("Should extract URI from first certificate")
                .isNotNull()
                .isEqualTo(expectedUri);
    }

    @Test
    void testFromConfig_invalidKeystorePath_failsGracefully() {
        // Given
        final OpcUaSpecificAdapterConfig adapterConfig =
                createAdapterConfig(true, "/path/that/does/not/exist/keystore.jks", null);

        // When
        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(adapterConfig);

        // Then
        assertThat(result).isInstanceOf(Failure.class);
        final String errorMessage = ((Failure<ParsedConfig, String>) result).failure();
        assertThat(errorMessage).contains("Failed to load keypair with chain from keystore");
    }

    @Test
    void testFromConfig_withConfiguredApplicationUri_usesConfiguredUri() throws Exception {
        // Given
        final String configuredUri = "urn:custom:configured:uri";
        final String domain = "certclient";

        final KeyChain keyChain = KeyChain.createKeyChain(domain);
        final File keystoreFile = keyChain.wrapInKeyStoreWithPrivateKey(
                tempDir.resolve("test-keystore").toString(), domain, KEYSTORE_PASSWORD, PRIVATE_KEY_PASSWORD);

        final OpcUaSpecificAdapterConfig adapterConfig = createAdapterConfig(
                true, keystoreFile.getAbsolutePath(), null, configuredUri // Configured override URI
                );

        // When
        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(adapterConfig);

        // Then
        assertThat(result).isInstanceOf(Success.class);
        final ParsedConfig parsedConfig = ((Success<ParsedConfig, String>) result).result();

        assertThat(parsedConfig.applicationUri())
                .as("Should use configured override URI (priority 1)")
                .isNotNull()
                .isEqualTo(configuredUri);
    }

    @Test
    void testFromConfig_withBothConfiguredAndCertificateUri_configuredTakesPrecedence() throws Exception {
        // Given
        final String configuredUri = "urn:custom:configured:uri";
        final String domain = "certclient";
        final String certificateUri = "urn:hivemq:edge:" + domain;

        final KeyChain keyChain = KeyChain.createKeyChain(domain);
        final File keystoreFile = keyChain.wrapInKeyStoreWithPrivateKey(
                tempDir.resolve("test-keystore-both").toString(), domain, KEYSTORE_PASSWORD, PRIVATE_KEY_PASSWORD);

        final OpcUaSpecificAdapterConfig adapterConfig = createAdapterConfig(
                true, keystoreFile.getAbsolutePath(), null, configuredUri // This should take precedence
                );

        // When
        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(adapterConfig);

        // Then
        assertThat(result).isInstanceOf(Success.class);
        final ParsedConfig parsedConfig = ((Success<ParsedConfig, String>) result).result();

        assertThat(parsedConfig.applicationUri())
                .as("Configured URI should take precedence over certificate SAN URI")
                .isNotNull()
                .isEqualTo(configuredUri)
                .isNotEqualTo(certificateUri);
    }

    @Test
    void testFromConfig_withBlankConfiguredUri_usesCertificateUri() throws Exception {
        // Given
        final String blankUri = "   "; // Blank string
        final String domain = "certclient";
        final String expectedUri = "urn:hivemq:edge:" + domain;

        final KeyChain keyChain = KeyChain.createKeyChain(domain);
        final File keystoreFile = keyChain.wrapInKeyStoreWithPrivateKey(
                tempDir.resolve("test-keystore-blank").toString(), domain, KEYSTORE_PASSWORD, PRIVATE_KEY_PASSWORD);

        final OpcUaSpecificAdapterConfig adapterConfig = createAdapterConfig(
                true, keystoreFile.getAbsolutePath(), null, blankUri // Blank should be treated as not configured
                );

        // When
        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(adapterConfig);

        // Then
        assertThat(result).isInstanceOf(Success.class);
        final ParsedConfig parsedConfig = ((Success<ParsedConfig, String>) result).result();

        assertThat(parsedConfig.applicationUri())
                .as("Blank configured URI should fall through to certificate URI")
                .isNotNull()
                .isEqualTo(expectedUri);
    }

    @Test
    void testFromConfig_priorityOrder_configuredOverCertificateOverDefault() throws Exception {
        // Test Priority 1: Configured URI is used when available
        final String configuredUri = "urn:priority:test:configured";
        final String domain = "testclient";

        final KeyChain keyChain = KeyChain.createKeyChain(domain);
        final File keystoreFile = keyChain.wrapInKeyStoreWithPrivateKey(
                tempDir.resolve("test-priority").toString(), domain, KEYSTORE_PASSWORD, PRIVATE_KEY_PASSWORD);

        final OpcUaSpecificAdapterConfig configWithAll =
                createAdapterConfig(true, keystoreFile.getAbsolutePath(), null, configuredUri);

        final Result<ParsedConfig, String> resultWithAll = ParsedConfig.fromConfig(configWithAll);
        assertThat(resultWithAll).isInstanceOf(Success.class);
        assertThat(((Success<ParsedConfig, String>) resultWithAll).result().applicationUri())
                .as("Priority 1: Configured URI should be used")
                .isEqualTo(configuredUri);

        // Test Priority 2: Certificate URI is used when no configured URI
        final OpcUaSpecificAdapterConfig configWithCert = createAdapterConfig(
                true, keystoreFile.getAbsolutePath(), null, null // No configured URI
                );

        final Result<ParsedConfig, String> resultWithCert = ParsedConfig.fromConfig(configWithCert);
        assertThat(resultWithCert).isInstanceOf(Success.class);
        assertThat(((Success<ParsedConfig, String>) resultWithCert).result().applicationUri())
                .as("Priority 2: Certificate SAN URI should be used")
                .isEqualTo("urn:hivemq:edge:" + domain);

        // Test Priority 3: Default will be used when neither configured nor in certificate
        final OpcUaSpecificAdapterConfig configWithNone = createAdapterConfig(
                true, // TLS enabled but no keystore
                null, null, null);

        final Result<ParsedConfig, String> resultWithNone = ParsedConfig.fromConfig(configWithNone);
        assertThat(resultWithNone).isInstanceOf(Success.class);
        assertThat(((Success<ParsedConfig, String>) resultWithNone).result().applicationUri())
                .as("Priority 3: Should be null (will use default in configurator)")
                .isNull();
    }

    private OpcUaSpecificAdapterConfig createAdapterConfig(
            final boolean tlsEnabled, final String keystorePath, final String truststorePath) {
        return createAdapterConfig(tlsEnabled, keystorePath, truststorePath, null, null);
    }

    private OpcUaSpecificAdapterConfig createAdapterConfig(
            final boolean tlsEnabled,
            final String keystorePath,
            final String truststorePath,
            final String applicationUri) {
        return createAdapterConfig(tlsEnabled, keystorePath, truststorePath, applicationUri, null);
    }

    private OpcUaSpecificAdapterConfig createAdapterConfig(
            final boolean tlsEnabled,
            final String keystorePath,
            final String truststorePath,
            final String applicationUri,
            final TlsChecks tlsChecks) {
        return createAdapterConfig(tlsEnabled, keystorePath, truststorePath, applicationUri, tlsChecks, null, null);
    }

    private OpcUaSpecificAdapterConfig createAdapterConfig(
            final boolean tlsEnabled,
            final String keystorePath,
            final String truststorePath,
            final String applicationUri,
            final TlsChecks tlsChecks,
            final TlsChecksFull tlsChecksFull,
            final String allowListPath) {

        final Keystore keystore =
                keystorePath != null ? new Keystore(keystorePath, KEYSTORE_PASSWORD, PRIVATE_KEY_PASSWORD) : null;

        final Truststore truststore = truststorePath != null ? new Truststore(truststorePath, KEYSTORE_PASSWORD) : null;

        final Tls tls = new Tls(
                tlsEnabled,
                tlsChecks,
                tlsChecksFull,
                keystore,
                truststore,
                allowListPath == null ? null : new AllowList(allowListPath));
        final Security security = new Security(SecPolicy.NONE);
        final OpcUaToMqttConfig opcUaToMqttConfig = new OpcUaToMqttConfig(1, 1000);

        return new OpcUaSpecificAdapterConfig(
                TEST_URI,
                false,
                applicationUri,
                null, // no auth
                tls,
                opcUaToMqttConfig,
                security,
                null);
    }

    private String writeAllowList(final String name) throws Exception {
        final Path file = tempDir.resolve(name);
        Files.writeString(file, "a".repeat(64) + "\n");
        return file.toString();
    }

    // ----- EDG-585: which validator each configuration selects -----

    @Test
    void noVerification_selectsTheInsecureValidator() {
        // Nothing is checked at all, so the stack's own do-nothing validator is the honest choice.
        final Result<ParsedConfig, String> result =
                ParsedConfig.fromConfig(createAdapterConfig(true, null, null, null, TlsChecks.NO_VERIFICATION));

        assertThat(result).isInstanceOf(Success.class);
        final ParsedConfig parsedConfig = ((Success<ParsedConfig, String>) result).result();
        assertThat(parsedConfig.anyCertificateValidatorActive()).isTrue();
        assertThat(parsedConfig.clientCertificateValidator())
                .isInstanceOf(CertificateValidator.InsecureCertificateValidator.class);
    }

    @Test
    void noVerification_ignoresAConfiguredTruststore() throws Exception {
        final KeyChain keyChain = KeyChain.createKeyChain("ca");
        final File truststoreFile = keyChain.wrapInKeyStoreWithPrivateKey(
                tempDir.resolve("truststore-any").toString(), "ca", KEYSTORE_PASSWORD, PRIVATE_KEY_PASSWORD);

        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(
                createAdapterConfig(true, null, truststoreFile.getAbsolutePath(), null, TlsChecks.NO_VERIFICATION));

        assertThat(result).isInstanceOf(Success.class);
        assertThat(((Success<ParsedConfig, String>) result).result().clientCertificateValidator())
                .isInstanceOf(CertificateValidator.InsecureCertificateValidator.class);
    }

    @Test
    void anyCertWithChecks_selectsTheCheckOnlyValidator() {
        // ANY_CERT does not mean "check nothing": the remaining axes still have to be applied.
        final OpcUaSpecificAdapterConfig adapterConfig = createAdapterConfig(
                true,
                null,
                null,
                null,
                null,
                new TlsChecksFull(
                        TrustMode.ANY_CERT,
                        SanUriCheck.APPLICATION_URI,
                        HostnameCheck.NONE,
                        ValidityCheck.NONE,
                        RevocationCheck.NONE,
                        KeyUsageCheck.NONE),
                null);

        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(adapterConfig);

        assertThat(result).isInstanceOf(Success.class);
        final ParsedConfig parsedConfig = ((Success<ParsedConfig, String>) result).result();
        assertThat(parsedConfig.anyCertificateValidatorActive()).isTrue();
        assertThat(parsedConfig.clientCertificateValidator()).isInstanceOf(CheckOnlyCertificateValidator.class);
    }

    @Test
    void selfSigned_selectsTheAllowListValidator() throws Exception {
        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(createAdapterConfig(
                true, null, null, null, TlsChecks.SELF_SIGNED, null, writeAllowList("allow-list.txt")));

        assertThat(result).isInstanceOf(Success.class);
        final ParsedConfig parsedConfig = ((Success<ParsedConfig, String>) result).result();
        assertThat(parsedConfig.anyCertificateValidatorActive())
                .as("an allow-list is a trust decision, not the absence of one")
                .isFalse();
        assertThat(parsedConfig.clientCertificateValidator()).isInstanceOf(AllowListCertificateValidator.class);
    }

    @Test
    void selfSigned_withAMissingAllowListFile_failsWithAnActionableMessage() {
        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(createAdapterConfig(
                true, null, null, null, TlsChecks.SELF_SIGNED, null, "/path/that/does/not/exist/allow-list.txt"));

        assertThat(result).isInstanceOf(Failure.class);
        assertThat(((Failure<ParsedConfig, String>) result).failure())
                .contains("allow-list")
                .contains("/path/that/does/not/exist/allow-list.txt");
    }

    @Test
    void selfSigned_withAMalformedAllowList_failsNamingTheLine() throws Exception {
        final Path file = tempDir.resolve("bad-allow-list.txt");
        Files.writeString(file, "a".repeat(64) + "\nnot-a-fingerprint\n");

        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(
                createAdapterConfig(true, null, null, null, TlsChecks.SELF_SIGNED, null, file.toString()));

        assertThat(result).isInstanceOf(Failure.class);
        assertThat(((Failure<ParsedConfig, String>) result).failure()).contains("line 2");
    }

    @Test
    void selfSigned_withoutAnAllowList_failsAtStartup() {
        final Result<ParsedConfig, String> result =
                ParsedConfig.fromConfig(createAdapterConfig(true, null, null, null, TlsChecks.SELF_SIGNED));

        assertThat(result).isInstanceOf(Failure.class);
        assertThat(((Failure<ParsedConfig, String>) result).failure()).contains("ALLOW_LIST");
    }

    @Test
    void selfSigned_withAnAllowListThatHasNoPath_failsAtStartup() {
        // `<allowList/>` with nothing inside it. The adapter must refuse to start with the same
        // message as a missing allowList, rather than letting a NullPointerException escape
        // fromConfig - by then start() has already spun up the schedulers and will never reach
        // failStart, leaving the adapter neither started nor cleanly failed.
        final Tls tls = new Tls(true, TlsChecks.SELF_SIGNED, null, null, null, new AllowList(null));
        final OpcUaSpecificAdapterConfig adapterConfig = new OpcUaSpecificAdapterConfig(
                TEST_URI, false, null, null, tls, new OpcUaToMqttConfig(1, 1000), new Security(SecPolicy.NONE), null);

        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(adapterConfig);

        assertThat(result).isInstanceOf(Failure.class);
        assertThat(((Failure<ParsedConfig, String>) result).failure())
                .contains("ALLOW_LIST")
                .contains("allowList");
    }

    @Test
    void bothDoorsConfigured_failsAtStartup() {
        final OpcUaSpecificAdapterConfig adapterConfig = createAdapterConfig(
                true,
                null,
                null,
                null,
                TlsChecks.STANDARD,
                new TlsChecksFull(TrustMode.CHAIN, null, null, null, null, null),
                null);

        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(adapterConfig);

        assertThat(result).isInstanceOf(Failure.class);
        assertThat(((Failure<ParsedConfig, String>) result).failure()).contains("mutually exclusive");
    }

    @Test
    void revocationWithoutAChain_failsAtStartup() {
        // The one combination the axes allow but the stack cannot honour. Failing loudly beats an axis
        // that silently does nothing.
        final OpcUaSpecificAdapterConfig adapterConfig = createAdapterConfig(
                true,
                null,
                null,
                null,
                null,
                new TlsChecksFull(
                        TrustMode.ANY_CERT,
                        SanUriCheck.NONE,
                        HostnameCheck.NONE,
                        ValidityCheck.NONE,
                        RevocationCheck.REQUIRE_CRLS,
                        KeyUsageCheck.NONE),
                null);

        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(adapterConfig);

        assertThat(result).isInstanceOf(Failure.class);
        assertThat(((Failure<ParsedConfig, String>) result).failure())
                .contains("revocation")
                .contains("revocation=NONE");
    }

    @Test
    void chain_withTruststore_selectsTheDefaultValidator() throws Exception {
        final KeyChain keyChain = KeyChain.createKeyChain("ca");
        final File truststoreFile = keyChain.wrapInKeyStoreWithPrivateKey(
                tempDir.resolve("truststore-chain").toString(), "ca", KEYSTORE_PASSWORD, PRIVATE_KEY_PASSWORD);

        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(
                createAdapterConfig(true, null, truststoreFile.getAbsolutePath(), null, TlsChecks.APPLICATION_URI));

        assertThat(result).isInstanceOf(Success.class);
        final ParsedConfig parsedConfig = ((Success<ParsedConfig, String>) result).result();
        assertThat(parsedConfig.anyCertificateValidatorActive()).isFalse();
        assertThat(parsedConfig.clientCertificateValidator()).isInstanceOf(DefaultClientCertificateValidator.class);
    }

    @Test
    void chain_withoutTruststore_fallsBackToCacerts() {
        // An unconfigured truststore has always been a valid configuration, and stays one.
        final Result<ParsedConfig, String> result =
                ParsedConfig.fromConfig(createAdapterConfig(true, null, null, null, TlsChecks.NONE));

        assertThat(result).isInstanceOf(Success.class);
        final ParsedConfig parsedConfig = ((Success<ParsedConfig, String>) result).result();
        assertThat(parsedConfig.anyCertificateValidatorActive()).isFalse();
        assertThat(parsedConfig.clientCertificateValidator()).isInstanceOf(DefaultClientCertificateValidator.class);
    }

    @Test
    void chain_withAnUnreadableTruststore_failsNamingTheWayOut() {
        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(
                createAdapterConfig(true, null, "/path/that/does/not/exist/truststore.jks", null, TlsChecks.NONE));

        assertThat(result).isInstanceOf(Failure.class);
        assertThat(((Failure<ParsedConfig, String>) result).failure())
                .contains("Truststore is configured but the file is missing or unreadable")
                .contains("ALLOW_LIST");
    }

    @Test
    void reparsingTheSameConfigurationIsStable() throws Exception {
        // Reconnects re-parse the configuration; the same file must keep selecting the same validator.
        final OpcUaSpecificAdapterConfig adapterConfig = createAdapterConfig(
                true, null, null, null, TlsChecks.SELF_SIGNED, null, writeAllowList("stable-allow-list.txt"));

        final Result<ParsedConfig, String> first = ParsedConfig.fromConfig(adapterConfig);
        final Result<ParsedConfig, String> second = ParsedConfig.fromConfig(adapterConfig);

        assertThat(first).isInstanceOf(Success.class);
        assertThat(second).isInstanceOf(Success.class);
        assertThat(((Success<ParsedConfig, String>) first).result().effectiveChecks())
                .isEqualTo(((Success<ParsedConfig, String>) second).result().effectiveChecks());
        assertThat(((Success<ParsedConfig, String>) second).result().clientCertificateValidator())
                .isInstanceOf(AllowListCertificateValidator.class);
    }

    @Test
    void theDefaultConfigurationTrustsNothingBlindly() {
        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(createAdapterConfig(false, null, null));

        assertThat(result).isInstanceOf(Success.class);
        assertThat(((Success<ParsedConfig, String>) result).result().anyCertificateValidatorActive())
                .isFalse();
    }

    @Test
    void aDisabledTlsConfigurationBuildsNoValidator() {
        final Result<ParsedConfig, String> result =
                ParsedConfig.fromConfig(createAdapterConfig(false, null, null, null, TlsChecks.NO_VERIFICATION));

        assertThat(result).isInstanceOf(Success.class);
        assertThat(((Success<ParsedConfig, String>) result).result().clientCertificateValidator())
                .isNull();
    }

    @Test
    void aKeystoreWithoutAPathFailsNamingTheElement() {
        // The record component is declared non-null, but a keystore whose <path> child is missing or
        // misspelled binds a null anyway; that must be a configuration error naming the element, not a
        // NullPointerException at adapter start.
        final Tls tls = new Tls(true, TlsChecks.NONE, null, new Keystore(null, "pass", "keyPass"), null, null);

        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(configWith(tls));

        assertThat(result).isInstanceOf(Failure.class);
        assertThat(((Failure<ParsedConfig, String>) result).failure())
                .contains("Keystore is configured but has no 'path'");
    }

    @Test
    void aKeystoreWithoutPasswordsFailsNamingTheMissingElement() {
        final Tls noPassword = new Tls(true, TlsChecks.NONE, null, new Keystore("/k.jks", null, "keyPass"), null, null);
        final Tls noKeyPassword = new Tls(true, TlsChecks.NONE, null, new Keystore("/k.jks", "pass", null), null, null);

        assertThat(((Failure<ParsedConfig, String>) ParsedConfig.fromConfig(configWith(noPassword))).failure())
                .contains("has no 'password'");
        assertThat(((Failure<ParsedConfig, String>) ParsedConfig.fromConfig(configWith(noKeyPassword))).failure())
                .contains("has no 'privateKeyPassword'");
    }

    @Test
    void aTruststoreWithoutAPathFailsNamingTheElement() {
        final Tls tls = new Tls(true, TlsChecks.NONE, null, null, new Truststore(null, "pass"), null);

        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(configWith(tls));

        assertThat(result).isInstanceOf(Failure.class);
        assertThat(((Failure<ParsedConfig, String>) result).failure())
                .contains("Truststore is configured but has no 'path'");
    }

    @Test
    void aTruststoreWithoutAPasswordFailsNamingTheElement() {
        final Tls tls = new Tls(true, TlsChecks.NONE, null, null, new Truststore("/t.jks", null), null);

        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(configWith(tls));

        assertThat(result).isInstanceOf(Failure.class);
        assertThat(((Failure<ParsedConfig, String>) result).failure())
                .contains("Truststore is configured but has no 'password'");
    }

    @Test
    void aStructurallyBrokenTruststoreFailsUnderEveryTrustMode() {
        // The guards run unconditionally, not only when the trust mode reads the truststore: a
        // configured store must be structurally valid even when it is inert, so the operator learns
        // about the broken element now rather than the first time a mode change makes it load.
        for (final TlsChecks preset : TlsChecks.values()) {
            // SELF_SIGNED requires an allow-list; it is inert but legal under the other presets.
            final AllowList allowList = new AllowList("/allow.txt");
            final Tls noPath = new Tls(true, preset, null, null, new Truststore(null, "pass"), allowList);
            final Tls noPassword = new Tls(true, preset, null, null, new Truststore("/t.jks", null), allowList);

            assertThat(((Failure<ParsedConfig, String>) ParsedConfig.fromConfig(configWith(noPath))).failure())
                    .as("truststore without a path must fail under preset %s", preset)
                    .contains("Truststore is configured but has no 'path'");
            assertThat(((Failure<ParsedConfig, String>) ParsedConfig.fromConfig(configWith(noPassword))).failure())
                    .as("truststore without a password must fail under preset %s", preset)
                    .contains("Truststore is configured but has no 'password'");
        }
    }

    // ----- a blank path is no path -----

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t"})
    void aTruststoreWithABlankPathIsRefusedRatherThanRunOnTheJvmCacerts(final @NotNull String blank) {
        // The fail-open this closes. A present <truststore> whose <path> is empty used to skip the
        // guard, skip the password guard with it, and fall through to getTrustedCerts, which read the
        // blank path as "no truststore configured" and trusted every CA in the JVM cacerts bundle -
        // while the configuration on disk said the deployment had its own truststore. A missing path
        // was already refused; only the blank spelling slipped through.
        final Tls tls = new Tls(true, TlsChecks.STANDARD, null, null, new Truststore(blank, "pass"), null);

        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(configWith(tls));

        assertThat(result).isInstanceOf(Failure.class);
        assertThat(((Failure<ParsedConfig, String>) result).failure())
                .contains("Truststore is configured but has no 'path'")
                .contains("remove the truststore element entirely");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t"})
    void aKeystoreWithABlankPathIsRefusedRatherThanRunWithoutAClientCertificate(final @NotNull String blank) {
        // The same hole one element over, and it fails later and further away: no keypair is loaded,
        // so the adapter starts and then cannot complete a handshake under a security policy that
        // needs a client certificate, or under X509 authentication.
        final Tls tls = new Tls(true, TlsChecks.STANDARD, null, new Keystore(blank, "pass", "keyPass"), null, null);

        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(configWith(tls));

        assertThat(result).isInstanceOf(Failure.class);
        assertThat(((Failure<ParsedConfig, String>) result).failure())
                .contains("Keystore is configured but has no 'path'");
    }

    @Test
    void aBlankPathDoesNotSwallowTheMissingPasswordItUsedToHide() {
        // The password guards were gated on the path being non-blank, so a blank path reported
        // nothing at all. Whichever element is named, exactly one problem is reported and it is the
        // one closest to the operator's mistake.
        final Tls blankPathNoPassword = new Tls(true, TlsChecks.STANDARD, null, null, new Truststore("  ", null), null);

        assertThat(((Failure<ParsedConfig, String>) ParsedConfig.fromConfig(configWith(blankPathNoPassword))).failure())
                .contains("Truststore is configured but has no 'path'");
    }

    @Test
    void anAbsentTruststoreStillMeansTheJvmCacerts() {
        // The compatibility half of the same change: absence is spelled by leaving the element out,
        // and that must keep working exactly as the documentation's minimal TLS example shows.
        final Tls tls = new Tls(true, TlsChecks.STANDARD, null, null, null, null);

        assertThat(ParsedConfig.fromConfig(configWith(tls))).isInstanceOf(Success.class);
    }

    private static @NotNull OpcUaSpecificAdapterConfig configWith(final @NotNull Tls tls) {
        return new OpcUaSpecificAdapterConfig(
                TEST_URI, false, null, null, tls, new OpcUaToMqttConfig(1, 1000), new Security(SecPolicy.NONE), null);
    }

    @Test
    void aDisabledTlsConfigurationDoesNotClaimAnyCertificateIsAccepted() {
        // <enabled>false</enabled> with tlsChecks=NO_VERIFICATION is inert: no validator is
        // installed and no certificate is ever examined, so the any-certificate warnings must stay
        // silent - a false security alarm trains operators to ignore the true ones.
        final Result<ParsedConfig, String> result =
                ParsedConfig.fromConfig(createAdapterConfig(false, null, null, null, TlsChecks.NO_VERIFICATION));

        assertThat(result).isInstanceOf(Success.class);
        assertThat(((Success<ParsedConfig, String>) result).result().anyCertificateValidatorActive())
                .isFalse();
    }

    // ----- the Milo check set each preset produces -----

    @Test
    void legacyPresetsProduceTheirReleasedCheckSets() {
        // Guards the upgrade promise at the level it actually matters: the exact set of optional checks
        // handed to the stack. These four sets are what the released implementation has always used.
        assertThat(ParsedConfig.toValidationChecks(TlsChecksProjection.fromPreset(TlsChecks.NONE)))
                .isEmpty();

        assertThat(ParsedConfig.toValidationChecks(TlsChecksProjection.fromPreset(TlsChecks.APPLICATION_URI)))
                .isEqualTo(ValidationCheck.NO_OPTIONAL_CHECKS);

        assertThat(ParsedConfig.toValidationChecks(TlsChecksProjection.fromPreset(TlsChecks.STANDARD)))
                .containsExactlyInAnyOrder(
                        ValidationCheck.APPLICATION_URI,
                        ValidationCheck.VALIDITY,
                        ValidationCheck.REVOCATION,
                        ValidationCheck.REVOCATION_LISTS);

        assertThat(ParsedConfig.toValidationChecks(TlsChecksProjection.fromPreset(TlsChecks.ALL)))
                .as("ALL has always meant every optional check, key usage included")
                .isEqualTo(ValidationCheck.ALL_OPTIONAL_CHECKS);
    }

    @Test
    void eachAxisContributesExactlyItsOwnChecks() {
        assertThat(ParsedConfig.toValidationChecks(new EffectiveChecks(
                        TrustMode.CHAIN,
                        SanUriCheck.NONE,
                        HostnameCheck.HOSTNAME,
                        ValidityCheck.NONE,
                        RevocationCheck.NONE,
                        KeyUsageCheck.NONE)))
                .containsExactly(ValidationCheck.HOSTNAME);

        assertThat(ParsedConfig.toValidationChecks(new EffectiveChecks(
                        TrustMode.CHAIN,
                        SanUriCheck.NONE,
                        HostnameCheck.NONE,
                        ValidityCheck.NONE,
                        RevocationCheck.CHECK,
                        KeyUsageCheck.NONE)))
                .as("CHECK asks for revocation without demanding a CRL for every CA")
                .containsExactly(ValidationCheck.REVOCATION);

        assertThat(ParsedConfig.toValidationChecks(new EffectiveChecks(
                        TrustMode.CHAIN,
                        SanUriCheck.NONE,
                        HostnameCheck.NONE,
                        ValidityCheck.NONE,
                        RevocationCheck.REQUIRE_CRLS,
                        KeyUsageCheck.NONE)))
                .containsExactlyInAnyOrder(ValidationCheck.REVOCATION, ValidationCheck.REVOCATION_LISTS);

        assertThat(ParsedConfig.toValidationChecks(new EffectiveChecks(
                        TrustMode.CHAIN,
                        SanUriCheck.NONE,
                        HostnameCheck.NONE,
                        ValidityCheck.NONE,
                        RevocationCheck.NONE,
                        KeyUsageCheck.KEY_USAGE)))
                .as("KEY_USAGE stops short of the extended key usage")
                .containsExactly(ValidationCheck.KEY_USAGE_END_ENTITY);

        assertThat(ParsedConfig.toValidationChecks(new EffectiveChecks(
                        TrustMode.CHAIN,
                        SanUriCheck.NONE,
                        HostnameCheck.NONE,
                        ValidityCheck.NONE,
                        RevocationCheck.NONE,
                        KeyUsageCheck.SERVER_AUTH)))
                .containsExactlyInAnyOrder(
                        ValidationCheck.KEY_USAGE_END_ENTITY, ValidationCheck.EXTENDED_KEY_USAGE_END_ENTITY);
    }

    @Test
    void maximumValidationAsksForEveryCheck() {
        assertThat(ParsedConfig.toValidationChecks(
                        TlsChecksProjection.fromAxes(new TlsChecksFull(null, null, null, null, null, null))))
                .isEqualTo(ValidationCheck.ALL_OPTIONAL_CHECKS);
    }

    // ----- hostname-verification WARN (logged once at start) -----

    @Test
    void chainWithoutHostname_logsHostnameVerificationWarn() {
        final ListAppender<ILoggingEvent> appender = attachParsedConfigAppender();
        try {
            ParsedConfig.fromConfig(createAdapterConfig(true, null, null, null, TlsChecks.STANDARD));
            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage()).contains("hostname verification is not enabled");
            });
        } finally {
            detachParsedConfigAppender(appender);
        }
    }

    @Test
    void chainWithHostname_doesNotLogHostnameVerificationWarn() {
        final ListAppender<ILoggingEvent> appender = attachParsedConfigAppender();
        try {
            ParsedConfig.fromConfig(createAdapterConfig(true, null, null, null, TlsChecks.ALL));
            assertThat(appender.list).noneSatisfy(event -> assertThat(event.getFormattedMessage())
                    .contains("hostname verification is not enabled"));
        } finally {
            detachParsedConfigAppender(appender);
        }
    }

    @Test
    void anyCert_doesNotLogHostnameVerificationWarn() {
        // The no-trust warning already covers this case; the hostname advisory must not double up.
        final ListAppender<ILoggingEvent> appender = attachParsedConfigAppender();
        try {
            ParsedConfig.fromConfig(createAdapterConfig(true, null, null, null, TlsChecks.NO_VERIFICATION));
            assertThat(appender.list).noneSatisfy(event -> assertThat(event.getFormattedMessage())
                    .contains("hostname verification is not enabled"));
        } finally {
            detachParsedConfigAppender(appender);
        }
    }

    @Test
    void allowList_logsHowManyFingerprintsWereLoaded() throws Exception {
        final ListAppender<ILoggingEvent> appender = attachParsedConfigAppender();
        try {
            ParsedConfig.fromConfig(createAdapterConfig(
                    true, null, null, null, TlsChecks.SELF_SIGNED, null, writeAllowList("counted-allow-list.txt")));

            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getLevel())
                        .as("trusting by fingerprint is a legitimate configuration, not a warning")
                        .isEqualTo(Level.INFO);
                assertThat(event.getFormattedMessage()).contains("1 fingerprint(s) loaded");
            });
        } finally {
            detachParsedConfigAppender(appender);
        }
    }

    // ----- EDG-585 F2: an allow-list that will never be read (logged once at start) -----

    @Test
    void chainWithAConfiguredAllowList_warnsThatItIsNeverRead() throws Exception {
        // The file is opened only under trustMode=ALLOW_LIST. Under CHAIN any certificate chaining to
        // the truststore or the JVM cacerts is accepted, which is a far wider set than the operator
        // who wrote the allow-list believes they are trusting. The configuration is honoured as
        // written - this only makes the gap between intent and effect visible in the log.
        final String path = writeAllowList("ignored-under-chain.txt");
        final ListAppender<ILoggingEvent> appender = attachParsedConfigAppender();
        try {
            final Result<ParsedConfig, String> result =
                    ParsedConfig.fromConfig(configWithAllowList(TlsChecks.STANDARD, new AllowList(path)));

            assertThat(result).as("a warning, not a refusal").isInstanceOf(Success.class);
            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage())
                        .contains("never reads it")
                        .contains(path)
                        .contains("CHAIN");
            });
        } finally {
            detachParsedConfigAppender(appender);
        }
    }

    @Test
    void anyCertWithAConfiguredAllowList_warnsThatItIsNeverRead() throws Exception {
        final String path = writeAllowList("ignored-under-any-cert.txt");
        final ListAppender<ILoggingEvent> appender = attachParsedConfigAppender();
        try {
            ParsedConfig.fromConfig(configWithAllowList(TlsChecks.NO_VERIFICATION, new AllowList(path)));

            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage())
                        .contains("never reads it")
                        .contains("ANY_CERT");
            });
        } finally {
            detachParsedConfigAppender(appender);
        }
    }

    @Test
    void allowListTrustMode_doesNotWarnAboutItsOwnAllowList() throws Exception {
        // The negative control that matters: the warning must not fire for the one configuration that
        // actually uses the file, or it trains operators to ignore it.
        final ListAppender<ILoggingEvent> appender = attachParsedConfigAppender();
        try {
            ParsedConfig.fromConfig(
                    configWithAllowList(TlsChecks.SELF_SIGNED, new AllowList(writeAllowList("used-allow-list.txt"))));

            assertThat(appender.list).noneSatisfy(event -> assertThat(event.getFormattedMessage())
                    .contains("never reads it"));
        } finally {
            detachParsedConfigAppender(appender);
        }
    }

    @Test
    void anAllowListWithNoUsablePath_doesNotWarn() {
        // `<allowList/>` and `<allowList><path></path></allowList>` both arrive as an allow-list with
        // no path. Nothing was configured, so there is nothing to report as ignored - and warning here
        // would fire for any empty object the UI materializes into an adapter that never asked for one.
        final ListAppender<ILoggingEvent> appender = attachParsedConfigAppender();
        try {
            ParsedConfig.fromConfig(configWithAllowList(TlsChecks.STANDARD, new AllowList(null)));
            ParsedConfig.fromConfig(configWithAllowList(TlsChecks.STANDARD, new AllowList("   ")));

            assertThat(appender.list).noneSatisfy(event -> assertThat(event.getFormattedMessage())
                    .contains("never reads it"));
        } finally {
            detachParsedConfigAppender(appender);
        }
    }

    @Test
    void tlsDisabled_doesNotWarnAboutAnAllowList() throws Exception {
        // No TLS means no certificate validation of any kind; singling out the allow-list would be
        // noise.
        final ListAppender<ILoggingEvent> appender = attachParsedConfigAppender();
        try {
            final AllowList allowList = new AllowList(writeAllowList("tls-disabled-allow-list.txt"));
            ParsedConfig.fromConfig(new OpcUaSpecificAdapterConfig(
                    TEST_URI,
                    false,
                    null,
                    null,
                    new Tls(false, TlsChecks.STANDARD, null, null, null, allowList),
                    new OpcUaToMqttConfig(1, 1000),
                    new Security(SecPolicy.NONE),
                    null));

            assertThat(appender.list).noneSatisfy(event -> assertThat(event.getFormattedMessage())
                    .contains("never reads it"));
        } finally {
            detachParsedConfigAppender(appender);
        }
    }

    // ----- SecurityPolicy NONE next to TLS settings (logged once at start) -----

    @Test
    void tlsEnabledUnderSecurityPolicyNone_warnsThatNoTlsSettingRuns() {
        // SecurityPolicy None exchanges no certificate, so everything in the TLS block is built but
        // never exercised - the operator who configured it believes they are protected.
        final ListAppender<ILoggingEvent> appender = attachParsedConfigAppender();
        try {
            ParsedConfig.fromConfig(createAdapterConfig(true, null, null, null, TlsChecks.ALL));

            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage()).contains("the security policy is NONE");
            });
        } finally {
            detachParsedConfigAppender(appender);
        }
    }

    @Test
    void tlsEnabledUnderARealSecurityPolicy_doesNotWarnAboutPolicyNone() {
        final ListAppender<ILoggingEvent> appender = attachParsedConfigAppender();
        try {
            ParsedConfig.fromConfig(new OpcUaSpecificAdapterConfig(
                    TEST_URI,
                    false,
                    null,
                    null,
                    new Tls(true, TlsChecks.ALL, null, null, null, null),
                    new OpcUaToMqttConfig(1, 1000),
                    new Security(SecPolicy.BASIC256SHA256),
                    null));

            assertThat(appender.list).noneSatisfy(event -> assertThat(event.getFormattedMessage())
                    .contains("the security policy is NONE"));
        } finally {
            detachParsedConfigAppender(appender);
        }
    }

    @Test
    void tlsDisabled_doesNotWarnAboutPolicyNone() {
        // No TLS settings are configured to be defeated, so there is nothing to warn about.
        final ListAppender<ILoggingEvent> appender = attachParsedConfigAppender();
        try {
            ParsedConfig.fromConfig(createAdapterConfig(false, null, null, null, TlsChecks.ALL));

            assertThat(appender.list).noneSatisfy(event -> assertThat(event.getFormattedMessage())
                    .contains("the security policy is NONE"));
        } finally {
            detachParsedConfigAppender(appender);
        }
    }

    // ----- EDG-891: revocation is enforced but no CRL can answer it (logged once at start) -----
    //
    // Estefania's finding: with a CA in the truststore and revocation on - which is the default,
    // preset STANDARD - a correctly configured CA-signed server is refused with
    // Bad_CertificateRevocationUnknown, because nothing supplies the CRLs revocation is decided
    // from. The refusal is right (unknown must fail closed) and stays; what these tests pin is that
    // it is no longer silent, and that the warning does not fire where revocation is satisfied
    // vacuously.

    @Test
    void chainWithACaAndRevocationButNoCrl_warnsThatItCannotSucceed() throws Exception {
        final KeyChain keyChain = KeyChain.createKeyChain("server");
        final File truststoreFile = keyChain.wrapInKeyStoreWithPrivateKey(
                tempDir.resolve("truststore-ca").toString(), "server", KEYSTORE_PASSWORD, PRIVATE_KEY_PASSWORD);
        final ListAppender<ILoggingEvent> appender = attachParsedConfigAppender();
        try {
            final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(
                    createAdapterConfig(true, null, truststoreFile.getAbsolutePath(), null, TlsChecks.STANDARD));

            assertThat(result)
                    .as("a warning, not a refusal: the configuration is honoured exactly as written")
                    .isInstanceOf(Success.class);
            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage())
                        .as("names the failure the operator will actually see, and both ways out")
                        .contains("no certificate revocation list is configured")
                        .contains("Bad_CertificateRevocationUnknown")
                        .contains("revocationList")
                        .contains("revocation=NONE")
                        .contains("REQUIRE_CRLS");
            });
        } finally {
            detachParsedConfigAppender(appender);
        }
    }

    @Test
    void chainWithACaAndRevocationAndACrl_doesNotWarnAndSaysWhatItLoaded() throws Exception {
        // The negative control that matters: once the operator supplies the missing input, the
        // warning must go away, or it trains them to ignore it.
        final KeyChain keyChain = KeyChain.createKeyChain("server");
        final File truststoreFile = keyChain.wrapInKeyStoreWithPrivateKey(
                tempDir.resolve("truststore-ca-crl").toString(), "server", KEYSTORE_PASSWORD, PRIVATE_KEY_PASSWORD);
        final String crl =
                TestCertificates.writeCrl(tempDir.resolve("ca.crl"), List.of()).toString();
        final ListAppender<ILoggingEvent> appender = attachParsedConfigAppender();
        try {
            final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(configWithRevocationList(
                    TlsChecks.STANDARD, truststoreFile.getAbsolutePath(), new RevocationList(crl)));

            assertThat(result).isInstanceOf(Success.class);
            assertThat(appender.list).noneSatisfy(event -> assertThat(event.getFormattedMessage())
                    .contains("no certificate revocation list is configured"));
            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getLevel())
                        .as("supplying CRLs is the correct configuration, not a warning")
                        .isEqualTo(Level.INFO);
                assertThat(event.getFormattedMessage())
                        .contains("1 certificate revocation list(s) loaded")
                        .contains(crl);
            });
        } finally {
            detachParsedConfigAppender(appender);
        }
    }

    @Test
    void chainWithACaButRevocationOff_doesNotWarn() throws Exception {
        // revocation=NONE asks no question, so there is nothing unanswerable about it.
        final KeyChain keyChain = KeyChain.createKeyChain("server");
        final File truststoreFile = keyChain.wrapInKeyStoreWithPrivateKey(
                tempDir.resolve("truststore-ca-norev").toString(), "server", KEYSTORE_PASSWORD, PRIVATE_KEY_PASSWORD);
        final ListAppender<ILoggingEvent> appender = attachParsedConfigAppender();
        try {
            ParsedConfig.fromConfig(
                    createAdapterConfig(true, null, truststoreFile.getAbsolutePath(), null, TlsChecks.APPLICATION_URI));

            assertThat(appender.list).noneSatisfy(event -> assertThat(event.getFormattedMessage())
                    .contains("no certificate revocation list is configured"));
        } finally {
            detachParsedConfigAppender(appender);
        }
    }

    @Test
    void chainWithoutACaInTheTruststore_doesNotWarnBecauseRevocationIsVacuous() throws Exception {
        // Direct trust: the server's own end-entity certificate is the anchor, so no issuer ever
        // enters the path and there is no revocation status to look up. Warning here would fire on
        // deployments that work, which is how a security warning becomes noise.
        final KeyChain keyChain = KeyChain.createKeyChain("server");
        final String truststore = writeTruststoreWithOnly(keyChain.getLeafCertificate("server"), "truststore-leaf");
        final ListAppender<ILoggingEvent> appender = attachParsedConfigAppender();
        try {
            final Result<ParsedConfig, String> result =
                    ParsedConfig.fromConfig(createAdapterConfig(true, null, truststore, null, TlsChecks.STANDARD));

            assertThat(result).isInstanceOf(Success.class);
            assertThat(appender.list).noneSatisfy(event -> assertThat(event.getFormattedMessage())
                    .contains("no certificate revocation list is configured"));
        } finally {
            detachParsedConfigAppender(appender);
        }
    }

    // ----- EDG-891: a revocation list that will never be read -----

    @ParameterizedTest
    @EnumSource(
            value = TlsChecks.class,
            names = {"SELF_SIGNED", "NO_VERIFICATION"})
    void aRevocationListUnderAChainlessTrustMode_warnsThatItIsNeverRead(final TlsChecks preset) throws Exception {
        // CRLs are consulted only while a certification path is built, which only CHAIN does. Same
        // inert-input shape as an allow-list configured under a mode that never opens it.
        final String crl = TestCertificates.writeCrl(tempDir.resolve("inert.crl"), List.of())
                .toString();
        final ListAppender<ILoggingEvent> appender = attachParsedConfigAppender();
        try {
            final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(new OpcUaSpecificAdapterConfig(
                    TEST_URI,
                    false,
                    null,
                    null,
                    new Tls(
                            true,
                            preset,
                            null,
                            null,
                            null,
                            preset == TlsChecks.SELF_SIGNED
                                    ? new AllowList(writeAllowList("with-inert-crl.txt"))
                                    : null,
                            new RevocationList(crl)),
                    new OpcUaToMqttConfig(1, 1000),
                    new Security(SecPolicy.NONE),
                    null));

            assertThat(result).as("a warning, not a refusal").isInstanceOf(Success.class);
            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage())
                        .contains("never reads them")
                        .contains("No revocation status is checked")
                        .contains(crl);
            });
        } finally {
            detachParsedConfigAppender(appender);
        }
    }

    @Test
    void chain_doesNotWarnAboutItsOwnRevocationList() throws Exception {
        final KeyChain keyChain = KeyChain.createKeyChain("server");
        final File truststoreFile = keyChain.wrapInKeyStoreWithPrivateKey(
                tempDir.resolve("truststore-used-crl").toString(), "server", KEYSTORE_PASSWORD, PRIVATE_KEY_PASSWORD);
        final String crl = TestCertificates.writeCrl(tempDir.resolve("used.crl"), List.of())
                .toString();
        final ListAppender<ILoggingEvent> appender = attachParsedConfigAppender();
        try {
            ParsedConfig.fromConfig(configWithRevocationList(
                    TlsChecks.STANDARD, truststoreFile.getAbsolutePath(), new RevocationList(crl)));

            assertThat(appender.list).noneSatisfy(event -> assertThat(event.getFormattedMessage())
                    .contains("never reads them"));
        } finally {
            detachParsedConfigAppender(appender);
        }
    }

    @Test
    void anUnreadableRevocationList_failsNamingTheFileAndTheWayOut() throws Exception {
        final KeyChain keyChain = KeyChain.createKeyChain("server");
        final File truststoreFile = keyChain.wrapInKeyStoreWithPrivateKey(
                tempDir.resolve("truststore-bad-crl").toString(), "server", KEYSTORE_PASSWORD, PRIVATE_KEY_PASSWORD);
        final String missing = tempDir.resolve("absent.crl").toString();

        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(configWithRevocationList(
                TlsChecks.STANDARD, truststoreFile.getAbsolutePath(), new RevocationList(missing)));

        assertThat(result)
                .as("a configured input that cannot be read is a configuration error, not a silent fallback")
                .isInstanceOf(Failure.class);
        assertThat(((Failure<ParsedConfig, String>) result).failure())
                .contains(missing)
                .contains("does not exist")
                .doesNotContain("java.nio");
    }

    private OpcUaSpecificAdapterConfig configWithRevocationList(
            final TlsChecks preset, final String truststorePath, final RevocationList revocationList) {
        return new OpcUaSpecificAdapterConfig(
                TEST_URI,
                false,
                null,
                null,
                new Tls(
                        true,
                        preset,
                        null,
                        null,
                        truststorePath == null ? null : new Truststore(truststorePath, KEYSTORE_PASSWORD),
                        null,
                        revocationList),
                new OpcUaToMqttConfig(1, 1000),
                new Security(SecPolicy.NONE),
                null);
    }

    /** A truststore holding exactly one certificate, so a test can control whether a CA is in it. */
    private String writeTruststoreWithOnly(final X509Certificate certificate, final String name) throws Exception {
        final KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
        keyStore.setCertificateEntry("only", certificate);
        final Path file = tempDir.resolve(name + ".jks");
        try (var out = Files.newOutputStream(file)) {
            keyStore.store(out, KEYSTORE_PASSWORD.toCharArray());
        }
        return file.toString();
    }

    private OpcUaSpecificAdapterConfig configWithAllowList(final TlsChecks preset, final AllowList allowList) {
        return new OpcUaSpecificAdapterConfig(
                TEST_URI,
                false,
                null,
                null,
                new Tls(true, preset, null, null, null, allowList),
                new OpcUaToMqttConfig(1, 1000),
                new Security(SecPolicy.NONE),
                null);
    }

    private static ListAppender<ILoggingEvent> attachParsedConfigAppender() {
        final Logger logger = (Logger) LoggerFactory.getLogger(ParsedConfig.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private static void detachParsedConfigAppender(final ListAppender<ILoggingEvent> appender) {
        ((Logger) LoggerFactory.getLogger(ParsedConfig.class)).detachAppender(appender);
    }
}
