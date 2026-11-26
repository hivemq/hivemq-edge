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
package com.hivemq.security.tls;

import static com.hivemq.security.tls.TLSFixtures.anyCertificatePolicy;
import static com.hivemq.security.tls.TLSFixtures.bridgeClient;
import static com.hivemq.security.tls.TLSFixtures.chainPolicy;
import static com.hivemq.security.tls.TLSFixtures.handshake;
import static com.hivemq.security.tls.TLSFixtures.keyPair;
import static com.hivemq.security.tls.TLSFixtures.serverContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.hivemq.bridge.mqtt.BridgeMqttClient;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext;
import java.io.IOException;
import java.net.ConnectException;
import java.security.KeyPair;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.net.ssl.SSLHandshakeException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import util.LogbackCapturingAppender;

/// What a bridge says about its certificate validation: which side a refused handshake names, the
/// warnings a policy draws at start-up and on each connection, and what the bridge's client logs.
///
/// Every certificate is generated per test; every handshake runs against a TLS server on a local
/// ephemeral port.
class TLSValidatorBridgeDiagnosticsTest {

    private static final @NotNull String BRIDGE_ID = "a-bridge";
    private static final @NotNull String ENDPOINT = "broker.example:8883";

    /// Material a configuration names; never read here, only its presence matters.
    private static final @NotNull MaterialRef NAMED = new MaterialRef.ByName("material", "entry");

    @AfterEach
    void releaseLogCapture() {
        LogbackCapturingAppender.Factory.cleanUp();
    }

    /// Proves that a handshake refused by the broker, because it needs a client certificate Edge does
    /// not present, is described as the broker's refusal.
    @Test
    void aBrokerThatRefusesEdgesCertificateIsNamedAsTheRefusingSide() throws Exception {
        final Instant now = Instant.now();
        final TestPki pki = new TestPki(now);
        final KeyPair keys = keyPair();
        final X509Certificate broker =
                pki.leaf("localhost", keys, now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(30)), null, null);
        final TLSPolicy policy = anyCertificatePolicy(StubTLSConfig.of(TLSConstants.TrustMode.ANY_CERT));

        final Throwable failure = handshake(policy, serverContext(keys, broker, pki.intermediate()), true)
                .clientFailure();

        assertThat(failure).isNotNull();
        assertThat(TLSValidatorBridge.describeRefusal(failure))
                .hasValueSatisfying(description -> assertThat(description).startsWith("The remote broker refused"));
    }

    /// Proves that a handshake Edge refused, because the broker's certificate chains to no configured
    /// anchor, is described as Edge's refusal.
    @Test
    void edgeRefusingTheBrokersCertificateIsNamedAsTheRefusingSide() throws Exception {
        final Instant now = Instant.now();
        final TestPki trusted = new TestPki(now);
        final TestPki untrusted = new TestPki(now);
        final KeyPair keys = keyPair();
        final X509Certificate broker = untrusted.leaf(
                "localhost", keys, now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(30)), null, null);
        final TLSPolicy policy = new TLSPolicy(
                StubTLSConfig.chain(),
                Optional.empty(),
                Set.of(new TrustAnchor(trusted.root(), null)),
                Set.of(),
                List.of());

        final Throwable failure = handshake(policy, serverContext(keys, broker, untrusted.intermediate()), false)
                .clientFailure();

        assertThat(failure).isInstanceOf(SSLHandshakeException.class);
        assertThat(TLSValidatorBridge.describeRefusal(failure))
                .hasValueSatisfying(description -> assertThat(description).startsWith("Edge refused"));
    }

    /// Proves that the description recognises a broker's certificate alert however deeply the
    /// client library wraps it, and stays silent on a failure that is not a certificate refusal.
    @Test
    void onlyCertificateRefusalsAreDescribed() {
        final Throwable wrapped = new IllegalStateException(
                "connection failed", new SSLHandshakeException("Received fatal alert: bad_certificate"));

        assertThat(TLSValidatorBridge.describeRefusal(wrapped))
                .hasValueSatisfying(description -> assertThat(description).contains("'bad_certificate'"));
        assertThat(TLSValidatorBridge.describeRefusal(
                        new SSLHandshakeException("Received fatal alert: handshake_failure")))
                .isEmpty();
        assertThat(TLSValidatorBridge.describeRefusal(new ConnectException("Connection refused")))
                .isEmpty();
        assertThat(TLSValidatorBridge.describeRefusal(new IOException((String) null)))
                .isEmpty();
    }

    /// Proves that accepting any certificate is warned about, naming the bridge, the broker and both
    /// ways out, and that a configuration that pins or chains is not.
    @Test
    void anyCertificateTrustIsWarnedAboutAtStartUp() {
        final List<String> warnings = TLSValidatorBridge.startupWarnings(
                anyCertificatePolicy(StubTLSConfig.of(TLSConstants.TrustMode.ANY_CERT)), BRIDGE_ID, ENDPOINT);

        assertThat(warnings).singleElement().satisfies(warning -> assertThat(warning)
                .contains(BRIDGE_ID, ENDPOINT, "ANY_CERT", "<tls-truststore>", "SELF_SIGNED"));
        assertThat(TLSValidatorBridge.startupWarnings(
                        fingerprintPolicy(StubTLSConfig.of(TLSConstants.TrustMode.ALLOW_LIST)
                                .withAllowList(NAMED)),
                        BRIDGE_ID,
                        ENDPOINT))
                .isEmpty();
    }

    /// Proves that a chain trusted without the hostname check is warned about, and a chain with it
    /// is not.
    @Test
    void aChainWithoutTheHostnameCheckIsWarnedAboutAtStartUp() throws Exception {
        final TestPki pki = new TestPki(Instant.now());

        assertThat(TLSValidatorBridge.startupWarnings(
                        chainPolicy(StubTLSConfig.chain(), List.of(), pki.root()), BRIDGE_ID, ENDPOINT))
                .singleElement()
                .satisfies(warning -> assertThat(warning).contains(BRIDGE_ID, ENDPOINT, "hostname", "<tls-checks>ALL"));
        assertThat(TLSValidatorBridge.startupWarnings(
                        chainPolicy(
                                StubTLSConfig.chain().withHostname(TLSConstants.HostnameCheck.HOSTNAME),
                                List.of(),
                                pki.root()),
                        BRIDGE_ID,
                        ENDPOINT))
                .isEmpty();
    }

    /// Proves that an allow-list the trust mode never reads is warned about, and one it reads is not.
    @Test
    void anUnusedAllowListIsWarnedAboutAtStartUp() throws Exception {
        final TestPki pki = new TestPki(Instant.now());
        final StubTLSConfig chainWithList = StubTLSConfig.chain()
                .withHostname(TLSConstants.HostnameCheck.HOSTNAME)
                .withAllowList(NAMED);

        assertThat(TLSValidatorBridge.startupWarnings(
                        chainPolicy(chainWithList, List.of(), pki.root()), BRIDGE_ID, ENDPOINT))
                .singleElement()
                .satisfies(warning -> assertThat(warning).contains(BRIDGE_ID, ENDPOINT, "allow-list", "CHAIN"));
        assertThat(TLSValidatorBridge.startupWarnings(
                        fingerprintPolicy(StubTLSConfig.of(TLSConstants.TrustMode.ALLOW_LIST)
                                .withAllowList(NAMED)),
                        BRIDGE_ID,
                        ENDPOINT))
                .isEmpty();
    }

    /// Proves that revocation lists the trust mode never reads are warned about, and lists a chain
    /// reads are not.
    @Test
    void unusedRevocationListsAreWarnedAboutAtStartUp() throws Exception {
        final Instant now = Instant.now();
        final TestPki pki = new TestPki(now);
        final StubTLSConfig pinnedWithLists = StubTLSConfig.of(TLSConstants.TrustMode.ALLOW_LIST)
                .withAllowList(NAMED)
                .withRevocationList(NAMED);
        final StubTLSConfig chainWithLists = StubTLSConfig.chain()
                .withHostname(TLSConstants.HostnameCheck.HOSTNAME)
                .withRevocation(TLSConstants.RevocationCheck.REQUIRE_CRLS)
                .withRevocationList(NAMED);
        final X509CRL list = pki.revocationList(
                pki.leaf("other", now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(1))),
                now.minus(Duration.ofHours(2)),
                now.minus(Duration.ofHours(1)));

        assertThat(TLSValidatorBridge.startupWarnings(fingerprintPolicy(pinnedWithLists), BRIDGE_ID, ENDPOINT))
                .singleElement()
                .satisfies(warning -> assertThat(warning)
                        .contains(BRIDGE_ID, ENDPOINT, "revocation lists", "ALLOW_LIST", "<tls-revocationlist>"));
        assertThat(TLSValidatorBridge.startupWarnings(
                        chainPolicy(chainWithLists, List.of(list), pki.root()), BRIDGE_ID, ENDPOINT))
                .isEmpty();
    }

    /// Proves that required revocation lists with an authority anchor and no list are warned about,
    /// and that neither a list nor a truststore of broker certificates alone draws the warning.
    @Test
    void requiredRevocationWithAnAuthorityAndNoListIsWarnedAboutAtStartUp() throws Exception {
        final Instant now = Instant.now();
        final TestPki pki = new TestPki(now);
        final StubTLSConfig required = StubTLSConfig.chain()
                .withHostname(TLSConstants.HostnameCheck.HOSTNAME)
                .withRevocation(TLSConstants.RevocationCheck.REQUIRE_CRLS);
        final X509Certificate broker =
                pki.leaf("localhost", now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(30)));
        final X509CRL list = pki.revocationList(broker, now.minus(Duration.ofHours(2)), now.minus(Duration.ofHours(1)));

        assertThat(TLSValidatorBridge.startupWarnings(
                        chainPolicy(required, List.of(), pki.root()), BRIDGE_ID, ENDPOINT))
                .singleElement()
                .satisfies(warning -> assertThat(warning)
                        .contains(BRIDGE_ID, ENDPOINT, "REQUIRE_CRLS", "<tls-revocationlist>", "CHECK"));
        assertThat(TLSValidatorBridge.startupWarnings(
                        chainPolicy(required, List.of(list), pki.root()), BRIDGE_ID, ENDPOINT))
                .isEmpty();
        assertThat(TLSValidatorBridge.startupWarnings(chainPolicy(required, List.of(), broker), BRIDGE_ID, ENDPOINT))
                .isEmpty();
    }

    /// Proves that only accepting any certificate is warned about on every connection.
    @Test
    void onlyAnyCertificateTrustIsWarnedAboutOnEachConnection() {
        assertThat(TLSValidatorBridge.connectionWarning(
                        StubTLSConfig.of(TLSConstants.TrustMode.ANY_CERT), BRIDGE_ID, ENDPOINT))
                .hasValueSatisfying(warning -> assertThat(warning).contains(BRIDGE_ID, ENDPOINT, "ANY_CERT"));
        assertThat(TLSValidatorBridge.connectionWarning(StubTLSConfig.chain(), BRIDGE_ID, ENDPOINT))
                .isEmpty();
        assertThat(TLSValidatorBridge.connectionWarning(
                        StubTLSConfig.of(TLSConstants.TrustMode.ALLOW_LIST), BRIDGE_ID, ENDPOINT))
                .isEmpty();
    }

    /// Proves that a bridge accepting any certificate logs the start-up warnings once when its client
    /// is built, and the trust mode again on each connection.
    @Test
    void aBridgeAcceptingAnyCertificateLogsAtStartUpAndOnEveryConnection() {
        final LogbackCapturingAppender appender =
                LogbackCapturingAppender.Factory.weaveInto(LoggerFactory.getLogger(BridgeMqttClient.class));
        final StubTLSConfig anyWithUnusedMaterial = StubTLSConfig.of(TLSConstants.TrustMode.ANY_CERT)
                .withAllowList(NAMED)
                .withRevocationList(NAMED);

        final BridgeMqttClient client = bridgeClient(BRIDGE_ID, anyWithUnusedMaterial, null);

        assertThat(warnings(appender))
                .hasSize(3)
                .anySatisfy(warning -> assertThat(warning).contains("establishes no trust"))
                .anySatisfy(warning -> assertThat(warning).contains("allow-list"))
                .anySatisfy(warning -> assertThat(warning).contains("revocation lists"));

        final MqttClientConnectedContext connected = mock(MqttClientConnectedContext.class, RETURNS_DEEP_STUBS);
        client.getMqtt5Client()
                .getConfig()
                .getConnectedListeners()
                .forEach(listener -> listener.onConnected(connected));
        client.getMqtt5Client()
                .getConfig()
                .getConnectedListeners()
                .forEach(listener -> listener.onConnected(connected));

        assertThat(warnings(appender))
                .filteredOn(warning -> warning.contains("connected to " + ENDPOINT + " with trust mode ANY_CERT"))
                .hasSize(2);
    }

    /// Proves that a bridge whose handshake the broker refused names the broker in its disconnection
    /// warning.
    @Test
    void aBridgeNamesTheBrokerWhenTheBrokerRefusedTheHandshake() {
        final LogbackCapturingAppender appender =
                LogbackCapturingAppender.Factory.weaveInto(LoggerFactory.getLogger(BridgeMqttClient.class));
        final BridgeMqttClient client =
                bridgeClient(BRIDGE_ID, StubTLSConfig.of(TLSConstants.TrustMode.ANY_CERT), null);
        final MqttClientDisconnectedContext disconnected =
                mock(MqttClientDisconnectedContext.class, RETURNS_DEEP_STUBS);
        when(disconnected.getCause())
                .thenReturn(new SSLHandshakeException("Received fatal alert: certificate_required"));

        client.getMqtt5Client()
                .getConfig()
                .getDisconnectedListeners()
                .forEach(listener -> listener.onDisconnected(disconnected));

        assertThat(warnings(appender)).anySatisfy(warning -> assertThat(warning)
                .contains("disconnected from " + ENDPOINT)
                .contains("The remote broker refused the TLS handshake"));
    }

    /// A policy pinning a single fingerprint that no test certificate has.
    private static @NotNull TLSPolicy fingerprintPolicy(final @NotNull StubTLSConfig config) {
        return new TLSPolicy(config, Optional.empty(), Set.of(), Set.of("00"), List.of());
    }

    private static @NotNull List<String> warnings(final @NotNull LogbackCapturingAppender appender) {
        return appender.getCapturedLogs().stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }
}
