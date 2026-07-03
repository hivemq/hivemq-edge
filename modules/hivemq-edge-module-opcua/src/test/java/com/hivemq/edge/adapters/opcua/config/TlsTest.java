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
package com.hivemq.edge.adapters.opcua.config;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Verifies the {@code (trustLevel, tlsChecks)} normalization performed in {@link Tls}: alias
 * expansion of the deprecated STANDARD/ALL values, the {@code CHAIN -> CHAIN_PKI} promotion, and the
 * backward-compatible defaults for unset knobs. See EDG-585.
 */
@SuppressWarnings("deprecation") // intentionally exercises the STANDARD/ALL aliases
class TlsTest {

    private static Tls tls(final TlsChecks tlsChecks, final TrustLevel trustLevel) {
        return new Tls(false, tlsChecks, null, null, trustLevel);
    }

    // ----- defaults -----

    @Test
    void fullyUnset_normalizesToLegacyStandard() {
        // Unset config == legacy STANDARD == CHAIN_PKI + APPLICATION_URI.
        final Tls tls = tls(null, null);
        assertThat(tls.trustLevel()).isEqualTo(TrustLevel.CHAIN_PKI);
        assertThat(tls.tlsChecks()).isEqualTo(TlsChecks.APPLICATION_URI);
    }

    @Test
    void defaultTls_normalizesToLegacyStandard() {
        final Tls tls = Tls.defaultTls();
        assertThat(tls.trustLevel()).isEqualTo(TrustLevel.CHAIN_PKI);
        assertThat(tls.tlsChecks()).isEqualTo(TlsChecks.APPLICATION_URI);
    }

    // ----- canonical identity values: unset trustLevel implies CHAIN (no PKI hygiene) -----

    @Test
    void identityNone_unsetTrust_impliesChain() {
        final Tls tls = tls(TlsChecks.NONE, null);
        assertThat(tls.trustLevel()).isEqualTo(TrustLevel.CHAIN);
        assertThat(tls.tlsChecks()).isEqualTo(TlsChecks.NONE);
    }

    @Test
    void identityApplicationUri_unsetTrust_impliesChain() {
        final Tls tls = tls(TlsChecks.APPLICATION_URI, null);
        assertThat(tls.trustLevel()).isEqualTo(TrustLevel.CHAIN);
        assertThat(tls.tlsChecks()).isEqualTo(TlsChecks.APPLICATION_URI);
    }

    @Test
    void identityHostname_unsetTrust_impliesChain() {
        final Tls tls = tls(TlsChecks.HOSTNAME, null);
        assertThat(tls.trustLevel()).isEqualTo(TrustLevel.CHAIN);
        assertThat(tls.tlsChecks()).isEqualTo(TlsChecks.HOSTNAME);
    }

    @Test
    void identityBoth_unsetTrust_impliesChain() {
        final Tls tls = tls(TlsChecks.APPLICATION_URI_AND_HOSTNAME, null);
        assertThat(tls.trustLevel()).isEqualTo(TrustLevel.CHAIN);
        assertThat(tls.tlsChecks()).isEqualTo(TlsChecks.APPLICATION_URI_AND_HOSTNAME);
    }

    // ----- deprecated STANDARD alias -----

    @Test
    void standardAlias_unsetTrust_expandsAndImpliesChainPki() {
        final Tls tls = tls(TlsChecks.STANDARD, null);
        assertThat(tls.trustLevel()).isEqualTo(TrustLevel.CHAIN_PKI);
        assertThat(tls.tlsChecks()).isEqualTo(TlsChecks.APPLICATION_URI);
    }

    @Test
    void standardAlias_explicitChain_promotedToChainPki() {
        final Tls tls = tls(TlsChecks.STANDARD, TrustLevel.CHAIN);
        assertThat(tls.trustLevel()).isEqualTo(TrustLevel.CHAIN_PKI);
        assertThat(tls.tlsChecks()).isEqualTo(TlsChecks.APPLICATION_URI);
    }

    @Test
    void standardAlias_explicitTrust_notPromoted() {
        // Under TRUST the chain is bypassed; there is no PKI hygiene to preserve, so no promotion.
        final Tls tls = tls(TlsChecks.STANDARD, TrustLevel.TRUST);
        assertThat(tls.trustLevel()).isEqualTo(TrustLevel.TRUST);
        assertThat(tls.tlsChecks()).isEqualTo(TlsChecks.APPLICATION_URI);
    }

    // ----- deprecated ALL alias -----

    @Test
    void allAlias_unsetTrust_expandsAndImpliesChainPki() {
        final Tls tls = tls(TlsChecks.ALL, null);
        assertThat(tls.trustLevel()).isEqualTo(TrustLevel.CHAIN_PKI);
        assertThat(tls.tlsChecks()).isEqualTo(TlsChecks.APPLICATION_URI_AND_HOSTNAME);
    }

    @Test
    void allAlias_explicitChain_promotedToChainPki() {
        final Tls tls = tls(TlsChecks.ALL, TrustLevel.CHAIN);
        assertThat(tls.trustLevel()).isEqualTo(TrustLevel.CHAIN_PKI);
        assertThat(tls.tlsChecks()).isEqualTo(TlsChecks.APPLICATION_URI_AND_HOSTNAME);
    }

    // ----- explicit trust levels with canonical identity -----

    @Test
    void explicitTrust_identityNone_preserved() {
        final Tls tls = tls(TlsChecks.NONE, TrustLevel.TRUST);
        assertThat(tls.trustLevel()).isEqualTo(TrustLevel.TRUST);
        assertThat(tls.tlsChecks()).isEqualTo(TlsChecks.NONE);
    }

    @Test
    void explicitTrust_unsetIdentity_defaultsToApplicationUri() {
        // Setting only trustLevel=TRUST does not implicitly disable identity; the identity knob is
        // independent and defaults to APPLICATION_URI. Set tlsChecks=NONE for pure accept-any.
        final Tls tls = tls(null, TrustLevel.TRUST);
        assertThat(tls.trustLevel()).isEqualTo(TrustLevel.TRUST);
        assertThat(tls.tlsChecks()).isEqualTo(TlsChecks.APPLICATION_URI);
    }

    @Test
    void explicitChainPki_identityNone_preserved() {
        final Tls tls = tls(TlsChecks.NONE, TrustLevel.CHAIN_PKI);
        assertThat(tls.trustLevel()).isEqualTo(TrustLevel.CHAIN_PKI);
        assertThat(tls.tlsChecks()).isEqualTo(TlsChecks.NONE);
    }

    // ----- deprecation WARN -----

    @Test
    void standardAlias_logsDeprecationWarn() {
        final ListAppender<ILoggingEvent> appender = attach();
        try {
            tls(TlsChecks.STANDARD, null);
            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage()).contains("deprecated").contains("trustLevel=CHAIN_PKI");
            });
        } finally {
            detach(appender);
        }
    }

    @Test
    void allAlias_deprecationWarn_flagsDroppedKeyUsageChecks() {
        final ListAppender<ILoggingEvent> appender = attach();
        try {
            tls(TlsChecks.ALL, null);
            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage())
                        .contains("deprecated")
                        .contains("SECURITY CHANGE")
                        .contains("key-usage");
            });
        } finally {
            detach(appender);
        }
    }

    @Test
    void standardAlias_deprecationWarn_hasNoKeyUsageNote() {
        // STANDARD maps to the identical check set, so it must not carry the ALL security-change note.
        final ListAppender<ILoggingEvent> appender = attach();
        try {
            tls(TlsChecks.STANDARD, null);
            assertThat(appender.list).noneSatisfy(event -> assertThat(event.getFormattedMessage())
                    .contains("SECURITY CHANGE"));
        } finally {
            detach(appender);
        }
    }

    @Test
    void canonicalValues_doNotLogDeprecationWarn() {
        final ListAppender<ILoggingEvent> appender = attach();
        try {
            tls(TlsChecks.APPLICATION_URI, TrustLevel.CHAIN);
            tls(null, null); // unset must not warn either
            assertThat(appender.list).noneSatisfy(event -> assertThat(event.getFormattedMessage())
                    .contains("deprecated"));
        } finally {
            detach(appender);
        }
    }

    private static ListAppender<ILoggingEvent> attach() {
        final Logger logger = (Logger) LoggerFactory.getLogger(Tls.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private static void detach(final ListAppender<ILoggingEvent> appender) {
        ((Logger) LoggerFactory.getLogger(Tls.class)).detachAppender(appender);
    }
}
