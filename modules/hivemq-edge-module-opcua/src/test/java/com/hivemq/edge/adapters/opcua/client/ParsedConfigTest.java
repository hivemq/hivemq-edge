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
import com.hivemq.edge.adapters.opcua.config.Keystore;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.SecPolicy;
import com.hivemq.edge.adapters.opcua.config.Security;
import com.hivemq.edge.adapters.opcua.config.Tls;
import com.hivemq.edge.adapters.opcua.config.TlsChecks;
import com.hivemq.edge.adapters.opcua.config.TrustLevel;
import com.hivemq.edge.adapters.opcua.config.Truststore;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.security.TrustAnyIdentityCertificateValidator;
import java.io.File;
import java.nio.file.Path;
import org.eclipse.milo.opcua.sdk.client.identity.AnonymousProvider;
import org.eclipse.milo.opcua.stack.core.security.CertificateValidator;
import org.eclipse.milo.opcua.stack.core.security.DefaultClientCertificateValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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
            final TrustLevel trustLevel) {
        return createAdapterConfig(
                tlsEnabled, keystorePath, truststorePath, applicationUri, trustLevel, TlsChecks.NONE);
    }

    private OpcUaSpecificAdapterConfig createAdapterConfig(
            final boolean tlsEnabled,
            final String keystorePath,
            final String truststorePath,
            final String applicationUri,
            final TrustLevel trustLevel,
            final TlsChecks tlsChecks) {

        final Keystore keystore =
                keystorePath != null ? new Keystore(keystorePath, KEYSTORE_PASSWORD, PRIVATE_KEY_PASSWORD) : null;

        final Truststore truststore = truststorePath != null ? new Truststore(truststorePath, KEYSTORE_PASSWORD) : null;

        final Tls tls = new Tls(tlsEnabled, tlsChecks, keystore, truststore, trustLevel);
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

    // ----- EDG-585: trustLevel x tlsChecks validator selection matrix -----

    @Test
    void trustLevelTrust_identityNone_noTruststore_selectsInsecureValidator() {
        // TRUST + identity NONE → chain bypassed, no identity checks → Milo InsecureCertificateValidator.
        final OpcUaSpecificAdapterConfig adapterConfig =
                createAdapterConfig(true, null, null, null, TrustLevel.TRUST, TlsChecks.NONE);

        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(adapterConfig);

        assertThat(result).isInstanceOf(Success.class);
        final ParsedConfig parsedConfig = ((Success<ParsedConfig, String>) result).result();
        assertThat(parsedConfig.trustAnyServerCertificate()).isTrue();
        assertThat(parsedConfig.clientCertificateValidator())
                .isInstanceOf(CertificateValidator.InsecureCertificateValidator.class);
    }

    @Test
    void trustLevelTrust_identityNone_truststorePresent_stillInsecureValidator() throws Exception {
        // TRUST + identity NONE ignores the truststore entirely.
        final KeyChain keyChain = KeyChain.createKeyChain("ca");
        final File truststoreFile = keyChain.wrapInKeyStoreWithPrivateKey(
                tempDir.resolve("truststore-trust").toString(), "ca", KEYSTORE_PASSWORD, PRIVATE_KEY_PASSWORD);

        final OpcUaSpecificAdapterConfig adapterConfig = createAdapterConfig(
                true, null, truststoreFile.getAbsolutePath(), null, TrustLevel.TRUST, TlsChecks.NONE);

        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(adapterConfig);

        assertThat(result).isInstanceOf(Success.class);
        final ParsedConfig parsedConfig = ((Success<ParsedConfig, String>) result).result();
        assertThat(parsedConfig.trustAnyServerCertificate()).isTrue();
        assertThat(parsedConfig.clientCertificateValidator())
                .isInstanceOf(CertificateValidator.InsecureCertificateValidator.class);
    }

    @Test
    void trustLevelTrust_identityApplicationUri_selectsTrustAnyIdentityValidator() {
        // EDG-594 cell: TRUST + identity → chain bypassed, identity still enforced via the custom validator.
        final OpcUaSpecificAdapterConfig adapterConfig =
                createAdapterConfig(true, null, null, null, TrustLevel.TRUST, TlsChecks.APPLICATION_URI);

        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(adapterConfig);

        assertThat(result).isInstanceOf(Success.class);
        final ParsedConfig parsedConfig = ((Success<ParsedConfig, String>) result).result();
        assertThat(parsedConfig.trustAnyServerCertificate()).isTrue();
        assertThat(parsedConfig.clientCertificateValidator()).isInstanceOf(TrustAnyIdentityCertificateValidator.class);
    }

    @Test
    void trustLevelTrust_identityHostname_selectsTrustAnyIdentityValidator() {
        final OpcUaSpecificAdapterConfig adapterConfig =
                createAdapterConfig(true, null, null, null, TrustLevel.TRUST, TlsChecks.HOSTNAME);

        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(adapterConfig);

        assertThat(result).isInstanceOf(Success.class);
        assertThat(((Success<ParsedConfig, String>) result).result().clientCertificateValidator())
                .isInstanceOf(TrustAnyIdentityCertificateValidator.class);
    }

    @Test
    void trustLevelTrust_identityBoth_selectsTrustAnyIdentityValidator() {
        final OpcUaSpecificAdapterConfig adapterConfig =
                createAdapterConfig(true, null, null, null, TrustLevel.TRUST, TlsChecks.APPLICATION_URI_AND_HOSTNAME);

        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(adapterConfig);

        assertThat(result).isInstanceOf(Success.class);
        assertThat(((Success<ParsedConfig, String>) result).result().clientCertificateValidator())
                .isInstanceOf(TrustAnyIdentityCertificateValidator.class);
    }

    @Test
    void trustLevelChain_truststorePresent_selectsDefaultValidator() throws Exception {
        // CHAIN + truststore present → normal Milo chain-building validator.
        final KeyChain keyChain = KeyChain.createKeyChain("ca");
        final File truststoreFile = keyChain.wrapInKeyStoreWithPrivateKey(
                tempDir.resolve("truststore-chain").toString(), "ca", KEYSTORE_PASSWORD, PRIVATE_KEY_PASSWORD);

        final OpcUaSpecificAdapterConfig adapterConfig = createAdapterConfig(
                true, null, truststoreFile.getAbsolutePath(), null, TrustLevel.CHAIN, TlsChecks.APPLICATION_URI);

        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(adapterConfig);

        assertThat(result).isInstanceOf(Success.class);
        final ParsedConfig parsedConfig = ((Success<ParsedConfig, String>) result).result();
        assertThat(parsedConfig.trustAnyServerCertificate()).isFalse();
        assertThat(parsedConfig.clientCertificateValidator()).isInstanceOf(DefaultClientCertificateValidator.class);
    }

    @Test
    void trustLevelChain_noTruststore_selectsDefaultValidatorWithCacerts() {
        // CHAIN + no user truststore → DefaultClientCertificateValidator backed by JVM cacerts.
        // Jochen's case: an unconfigured user truststore must remain a valid configuration.
        final OpcUaSpecificAdapterConfig adapterConfig =
                createAdapterConfig(true, null, null, null, TrustLevel.CHAIN, TlsChecks.NONE);

        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(adapterConfig);

        assertThat(result).isInstanceOf(Success.class);
        final ParsedConfig parsedConfig = ((Success<ParsedConfig, String>) result).result();
        assertThat(parsedConfig.trustAnyServerCertificate()).isFalse();
        assertThat(parsedConfig.clientCertificateValidator()).isInstanceOf(DefaultClientCertificateValidator.class);
    }

    @Test
    void trustLevelChainPki_truststorePresent_selectsDefaultValidator() throws Exception {
        // CHAIN_PKI + truststore present → DefaultClientCertificateValidator (with PKI-hygiene checks).
        final KeyChain keyChain = KeyChain.createKeyChain("ca");
        final File truststoreFile = keyChain.wrapInKeyStoreWithPrivateKey(
                tempDir.resolve("truststore-pki").toString(), "ca", KEYSTORE_PASSWORD, PRIVATE_KEY_PASSWORD);

        final OpcUaSpecificAdapterConfig adapterConfig = createAdapterConfig(
                true,
                null,
                truststoreFile.getAbsolutePath(),
                null,
                TrustLevel.CHAIN_PKI,
                TlsChecks.APPLICATION_URI_AND_HOSTNAME);

        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(adapterConfig);

        assertThat(result).isInstanceOf(Success.class);
        assertThat(((Success<ParsedConfig, String>) result).result().clientCertificateValidator())
                .isInstanceOf(DefaultClientCertificateValidator.class);
    }

    @Test
    void trustLevelChain_truststoreUnreadable_returnsFailureNamingTrustLevelTrust() {
        // CHAIN + configured-but-unreadable truststore → Failure.of(...) with an actionable message
        // that names trustLevel=TRUST as one of the resolutions.
        final OpcUaSpecificAdapterConfig adapterConfig = createAdapterConfig(
                true, null, "/path/that/does/not/exist/truststore.jks", null, TrustLevel.CHAIN, TlsChecks.NONE);

        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(adapterConfig);

        assertThat(result).isInstanceOf(Failure.class);
        final String error = ((Failure<ParsedConfig, String>) result).failure();
        assertThat(error)
                .contains("Truststore is configured but the file is missing or unreadable")
                .contains("trustLevel=TRUST");
    }

    @Test
    void trustLevelTrust_reconnectionsKeepInsecureValidator() {
        // A fresh validator of the same type is produced on each re-parse; documents stability across
        // reconfigurations under trustLevel=TRUST.
        final OpcUaSpecificAdapterConfig adapterConfig =
                createAdapterConfig(true, null, null, null, TrustLevel.TRUST, TlsChecks.NONE);

        final Result<ParsedConfig, String> first = ParsedConfig.fromConfig(adapterConfig);
        final Result<ParsedConfig, String> second = ParsedConfig.fromConfig(adapterConfig);

        assertThat(first).isInstanceOf(Success.class);
        assertThat(second).isInstanceOf(Success.class);
        assertThat(((Success<ParsedConfig, String>) first).result().clientCertificateValidator())
                .isInstanceOf(CertificateValidator.InsecureCertificateValidator.class);
        assertThat(((Success<ParsedConfig, String>) second).result().clientCertificateValidator())
                .isInstanceOf(CertificateValidator.InsecureCertificateValidator.class);
    }

    @Test
    void trustLevelDefault_isNotTrustAny() {
        // Secure-by-default: the default trust level is never TRUST.
        final OpcUaSpecificAdapterConfig adapterConfig = createAdapterConfig(false, null, null);
        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(adapterConfig);

        assertThat(result).isInstanceOf(Success.class);
        assertThat(((Success<ParsedConfig, String>) result).result().trustAnyServerCertificate())
                .isFalse();
    }

    // ----- hostname-verification WARN (logged once at start) -----

    @Test
    void chainWithoutHostname_logsHostnameVerificationWarn() {
        final ListAppender<ILoggingEvent> appender = attachParsedConfigAppender();
        try {
            // CHAIN with an identity that omits HOSTNAME → one-shot advisory WARN.
            ParsedConfig.fromConfig(
                    createAdapterConfig(true, null, null, null, TrustLevel.CHAIN, TlsChecks.APPLICATION_URI));
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
            ParsedConfig.fromConfig(createAdapterConfig(
                    true, null, null, null, TrustLevel.CHAIN, TlsChecks.APPLICATION_URI_AND_HOSTNAME));
            assertThat(appender.list).noneSatisfy(event -> assertThat(event.getFormattedMessage())
                    .contains("hostname verification is not enabled"));
        } finally {
            detachParsedConfigAppender(appender);
        }
    }

    @Test
    void trust_doesNotLogHostnameVerificationWarn() {
        // Under TRUST the MITM warning already covers it; the hostname advisory must not double up.
        final ListAppender<ILoggingEvent> appender = attachParsedConfigAppender();
        try {
            ParsedConfig.fromConfig(createAdapterConfig(true, null, null, null, TrustLevel.TRUST, TlsChecks.NONE));
            assertThat(appender.list).noneSatisfy(event -> assertThat(event.getFormattedMessage())
                    .contains("hostname verification is not enabled"));
        } finally {
            detachParsedConfigAppender(appender);
        }
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
