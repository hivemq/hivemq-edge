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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hivemq.edge.adapters.opcua.config.TlsChecksProjection.InvalidTlsChecksConfigException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The projection is the single place where a configuration becomes a decision about what is checked,
 * so it is tested exhaustively: every preset, every axis combination, and every way of writing a
 * configuration that cannot be honoured.
 */
class TlsChecksProjectionTest {

    private static Tls preset(final TlsChecks tlsChecks) {
        return new Tls(true, tlsChecks, null, null, null, allowListFor(tlsChecks));
    }

    private static Tls axes(final TlsChecksFull tlsChecksFull) {
        return new Tls(true, null, tlsChecksFull, null, null, new AllowList("/tmp/allow-list.txt"));
    }

    /** SELF_SIGNED requires an allow-list; supplying one keeps these cases about the projection. */
    private static AllowList allowListFor(final TlsChecks tlsChecks) {
        return tlsChecks == TlsChecks.SELF_SIGNED ? new AllowList("/tmp/allow-list.txt") : null;
    }

    private static EffectiveChecks project(final Tls tls) {
        try {
            return TlsChecksProjection.project(tls);
        } catch (final InvalidTlsChecksConfigException e) {
            throw new AssertionError("configuration was expected to be valid: " + e.reason(), e);
        }
    }

    @Nested
    class Doors {

        @Test
        void neitherDoorConfigured_projectsStandard() {
            // The compatibility guarantee: a configuration written before either knob existed keeps
            // behaving exactly as it did.
            assertThat(project(new Tls(true, null, null, null, null, null)))
                    .isEqualTo(TlsChecksProjection.fromPreset(TlsChecks.STANDARD));
        }

        @Test
        void defaultTls_projectsStandard() {
            assertThat(project(Tls.defaultTls())).isEqualTo(TlsChecksProjection.fromPreset(TlsChecks.STANDARD));
        }

        @Test
        void bothDoorsConfigured_isRejected() {
            final Tls both = new Tls(
                    true,
                    TlsChecks.STANDARD,
                    new TlsChecksFull(TrustMode.CHAIN, null, null, null, null, null),
                    null,
                    null,
                    null);

            assertThatThrownBy(() -> TlsChecksProjection.project(both))
                    .isInstanceOf(InvalidTlsChecksConfigException.class)
                    .hasMessageContaining("tlsChecks")
                    .hasMessageContaining("tlsChecksFull")
                    .hasMessageContaining("mutually exclusive");
        }

        @Test
        void bothDoorsConfigured_isRejectedEvenWhenTheyAgree() {
            // NO_VERIFICATION and these axes describe the same thing; ambiguity is still an error,
            // because the operator must not have to wonder which one wins.
            final Tls both = new Tls(
                    true,
                    TlsChecks.NO_VERIFICATION,
                    new TlsChecksFull(
                            TrustMode.ANY_CERT,
                            SanUriCheck.NONE,
                            HostnameCheck.NONE,
                            ValidityCheck.NONE,
                            RevocationCheck.NONE,
                            KeyUsageCheck.NONE),
                    null,
                    null,
                    null);

            assertThatThrownBy(() -> TlsChecksProjection.project(both))
                    .isInstanceOf(InvalidTlsChecksConfigException.class);
        }
    }

    @Nested
    class Presets {

        @ParameterizedTest
        @EnumSource(TlsChecks.class)
        void everyPresetProjects(final TlsChecks tlsChecks) {
            // Totality: no preset may fall through the table, now or when one is added.
            assertThat(project(preset(tlsChecks))).isNotNull();
        }

        @Test
        void none_isChainOnly() {
            final EffectiveChecks checks = project(preset(TlsChecks.NONE));
            assertThat(checks.trustMode()).isEqualTo(TrustMode.CHAIN);
            assertThat(checks.isAnyCertificateCheckEnabled())
                    .as("NONE disables the optional checks but still builds the chain")
                    .isFalse();
        }

        @Test
        void applicationUri_isChainPlusApplicationUri() {
            assertThat(project(preset(TlsChecks.APPLICATION_URI)))
                    .isEqualTo(new EffectiveChecks(
                            TrustMode.CHAIN,
                            SanUriCheck.APPLICATION_URI,
                            HostnameCheck.NONE,
                            ValidityCheck.NONE,
                            RevocationCheck.NONE,
                            KeyUsageCheck.NONE));
        }

        @Test
        void standard_isChainPlusApplicationUriValidityRevocation() {
            assertThat(project(preset(TlsChecks.STANDARD)))
                    .isEqualTo(new EffectiveChecks(
                            TrustMode.CHAIN,
                            SanUriCheck.APPLICATION_URI,
                            HostnameCheck.NONE,
                            ValidityCheck.NOT_BEFORE_OR_AFTER,
                            RevocationCheck.REQUIRE_CRLS,
                            KeyUsageCheck.NONE));
        }

        @Test
        void all_isEverything() {
            assertThat(project(preset(TlsChecks.ALL)))
                    .isEqualTo(new EffectiveChecks(
                            TrustMode.CHAIN,
                            SanUriCheck.APPLICATION_URI,
                            HostnameCheck.HOSTNAME,
                            ValidityCheck.NOT_BEFORE_OR_AFTER,
                            RevocationCheck.REQUIRE_CRLS,
                            KeyUsageCheck.SERVER_AUTH));
        }

        @Test
        void all_enforcesKeyUsage() {
            // Guards the released meaning of ALL: it has always enforced key usage, and an upgrade
            // must not quietly stop doing so.
            final EffectiveChecks checks = project(preset(TlsChecks.ALL));
            assertThat(checks.isKeyUsage()).isTrue();
            assertThat(checks.isExtendedKeyUsage()).isTrue();
        }

        @Test
        void selfSigned_isAllowListWithIdentityAndValidity() {
            assertThat(project(preset(TlsChecks.SELF_SIGNED)))
                    .isEqualTo(new EffectiveChecks(
                            TrustMode.ALLOW_LIST,
                            SanUriCheck.APPLICATION_URI,
                            HostnameCheck.HOSTNAME,
                            ValidityCheck.NOT_BEFORE_OR_AFTER,
                            RevocationCheck.NONE,
                            KeyUsageCheck.NONE));
        }

        @Test
        void noVerification_checksNothing() {
            final EffectiveChecks checks = project(preset(TlsChecks.NO_VERIFICATION));
            assertThat(checks.trustMode()).isEqualTo(TrustMode.ANY_CERT);
            assertThat(checks.isAnyCertificateCheckEnabled()).isFalse();
        }

        @Test
        void onlyTheTwoNewPresetsLeaveTheChain() {
            // Every legacy preset must still build a chain: that is what "nothing changes on upgrade"
            // means at this layer.
            for (final TlsChecks tlsChecks :
                    List.of(TlsChecks.NONE, TlsChecks.APPLICATION_URI, TlsChecks.STANDARD, TlsChecks.ALL)) {
                assertThat(project(preset(tlsChecks)).isChainBuilt())
                        .as("preset %s must build a chain", tlsChecks)
                        .isTrue();
            }
            assertThat(project(preset(TlsChecks.SELF_SIGNED)).isChainBuilt()).isFalse();
            assertThat(project(preset(TlsChecks.NO_VERIFICATION)).isChainBuilt())
                    .isFalse();
        }
    }

    @Nested
    class Axes {

        @Test
        void emptyAxes_meansMaximumValidation() {
            // The fail-safe rule, stated as one assertion: writing nothing down asks for everything.
            assertThat(project(axes(new TlsChecksFull(null, null, null, null, null, null))))
                    .isEqualTo(new EffectiveChecks(
                            TrustMode.CHAIN,
                            SanUriCheck.APPLICATION_URI,
                            HostnameCheck.HOSTNAME,
                            ValidityCheck.NOT_BEFORE_OR_AFTER,
                            RevocationCheck.REQUIRE_CRLS,
                            KeyUsageCheck.SERVER_AUTH));
        }

        @Test
        void omittingOneAxisNeverRelaxesIt() {
            // Each axis omitted in turn, with all others at their loosest: the omitted one must still
            // come back strict. A forgotten entry must give more verification, never less.
            assertThat(project(axes(new TlsChecksFull(
                                    null,
                                    SanUriCheck.NONE,
                                    HostnameCheck.NONE,
                                    ValidityCheck.NONE,
                                    RevocationCheck.NONE,
                                    KeyUsageCheck.NONE)))
                            .trustMode())
                    .isEqualTo(TrustMode.CHAIN);

            assertThat(project(axes(new TlsChecksFull(
                                    TrustMode.ANY_CERT,
                                    null,
                                    HostnameCheck.NONE,
                                    ValidityCheck.NONE,
                                    RevocationCheck.NONE,
                                    KeyUsageCheck.NONE)))
                            .sanUri())
                    .isEqualTo(SanUriCheck.APPLICATION_URI);

            assertThat(project(axes(new TlsChecksFull(
                                    TrustMode.ANY_CERT,
                                    SanUriCheck.NONE,
                                    null,
                                    ValidityCheck.NONE,
                                    RevocationCheck.NONE,
                                    KeyUsageCheck.NONE)))
                            .hostname())
                    .isEqualTo(HostnameCheck.HOSTNAME);

            assertThat(project(axes(new TlsChecksFull(
                                    TrustMode.ANY_CERT,
                                    SanUriCheck.NONE,
                                    HostnameCheck.NONE,
                                    null,
                                    RevocationCheck.NONE,
                                    KeyUsageCheck.NONE)))
                            .validity())
                    .isEqualTo(ValidityCheck.NOT_BEFORE_OR_AFTER);

            assertThat(project(axes(new TlsChecksFull(
                                    TrustMode.CHAIN,
                                    SanUriCheck.NONE,
                                    HostnameCheck.NONE,
                                    ValidityCheck.NONE,
                                    null,
                                    KeyUsageCheck.NONE)))
                            .revocation())
                    .isEqualTo(RevocationCheck.REQUIRE_CRLS);

            assertThat(project(axes(new TlsChecksFull(
                                    TrustMode.ANY_CERT,
                                    SanUriCheck.NONE,
                                    HostnameCheck.NONE,
                                    ValidityCheck.NONE,
                                    RevocationCheck.NONE,
                                    null)))
                            .keyUsage())
                    .isEqualTo(KeyUsageCheck.SERVER_AUTH);
        }

        /**
         * Every reachable combination of all six axes — the whole configuration space an operator can
         * write — must project to exactly the values written, and must be either accepted or rejected
         * for a stated reason. Nothing may be silently altered.
         */
        @ParameterizedTest(name = "{0}/{1}/{2}/{3}/{4}/{5}")
        @MethodSource("com.hivemq.edge.adapters.opcua.config.TlsChecksProjectionTest#allAxisCombinations")
        void everyAxisCombinationProjectsToItself(
                final TrustMode trustMode,
                final SanUriCheck sanUri,
                final HostnameCheck hostname,
                final ValidityCheck validity,
                final RevocationCheck revocation,
                final KeyUsageCheck keyUsage) {

            final Tls tls = axes(new TlsChecksFull(trustMode, sanUri, hostname, validity, revocation, keyUsage));
            final boolean revocationNeedsChain = revocation != RevocationCheck.NONE && trustMode != TrustMode.CHAIN;

            if (revocationNeedsChain) {
                assertThatThrownBy(() -> TlsChecksProjection.project(tls))
                        .as("revocation without a chain cannot be honoured and must be rejected")
                        .isInstanceOf(InvalidTlsChecksConfigException.class);
                return;
            }

            assertThat(project(tls))
                    .isEqualTo(new EffectiveChecks(trustMode, sanUri, hostname, validity, revocation, keyUsage));
        }
    }

    @Nested
    class Rejections {

        @ParameterizedTest
        @EnumSource(
                value = TrustMode.class,
                names = {"ALLOW_LIST", "ANY_CERT"})
        void revocationWithoutAChain_isRejectedWithAnActionableMessage(final TrustMode trustMode) {
            final Tls tls = axes(new TlsChecksFull(
                    trustMode,
                    SanUriCheck.NONE,
                    HostnameCheck.NONE,
                    ValidityCheck.NONE,
                    RevocationCheck.REQUIRE_CRLS,
                    KeyUsageCheck.NONE));

            assertThatThrownBy(() -> TlsChecksProjection.project(tls))
                    .isInstanceOf(InvalidTlsChecksConfigException.class)
                    .hasMessageContaining("revocation")
                    .hasMessageContaining("revocation=NONE")
                    .hasMessageContaining(trustMode.name());
        }

        @Test
        void revocationCheckWithoutAChain_isAlsoRejected() {
            // The middle value must be caught too, not only the strictest one.
            final Tls tls = axes(new TlsChecksFull(
                    TrustMode.ANY_CERT,
                    SanUriCheck.NONE,
                    HostnameCheck.NONE,
                    ValidityCheck.NONE,
                    RevocationCheck.CHECK,
                    KeyUsageCheck.NONE));

            assertThatThrownBy(() -> TlsChecksProjection.project(tls))
                    .isInstanceOf(InvalidTlsChecksConfigException.class);
        }

        @Test
        void allowListTrustModeWithoutAnAllowList_isRejected() {
            final Tls tls = new Tls(
                    true,
                    null,
                    new TlsChecksFull(
                            TrustMode.ALLOW_LIST,
                            SanUriCheck.NONE,
                            HostnameCheck.NONE,
                            ValidityCheck.NONE,
                            RevocationCheck.NONE,
                            KeyUsageCheck.NONE),
                    null,
                    null,
                    null);

            assertThatThrownBy(() -> TlsChecksProjection.project(tls))
                    .isInstanceOf(InvalidTlsChecksConfigException.class)
                    .hasMessageContaining("ALLOW_LIST")
                    .hasMessageContaining("allowList");
        }

        @Test
        void selfSignedPresetWithoutAnAllowList_isRejected() {
            // The preset selects ALLOW_LIST, so it inherits the same requirement.
            final Tls tls = new Tls(true, TlsChecks.SELF_SIGNED, null, null, null, null);

            assertThatThrownBy(() -> TlsChecksProjection.project(tls))
                    .isInstanceOf(InvalidTlsChecksConfigException.class)
                    .hasMessageContaining("ALLOW_LIST");
        }

        @Test
        void allowListWithABlankPath_isRejected() {
            final Tls tls = new Tls(true, TlsChecks.SELF_SIGNED, null, null, null, new AllowList("   "));

            assertThatThrownBy(() -> TlsChecksProjection.project(tls))
                    .isInstanceOf(InvalidTlsChecksConfigException.class);
        }

        @Test
        void allowListWithNoPathAtAll_isRejectedRatherThanThrowingNull() {
            // An operator writing `<allowList/>` produces exactly this: Jackson binds the missing
            // element to a null path. It must reach the same actionable error as an absent allowList,
            // not escape the projection as a NullPointerException.
            final Tls tls = new Tls(true, TlsChecks.SELF_SIGNED, null, null, null, new AllowList(null));

            assertThatThrownBy(() -> TlsChecksProjection.project(tls))
                    .isInstanceOf(InvalidTlsChecksConfigException.class)
                    .hasMessageContaining("ALLOW_LIST")
                    .hasMessageContaining("allowList");
        }
    }

    static List<Arguments> allAxisCombinations() {
        final List<Arguments> combinations = new ArrayList<>();
        for (final TrustMode trustMode : TrustMode.values()) {
            for (final SanUriCheck sanUri : SanUriCheck.values()) {
                for (final HostnameCheck hostname : HostnameCheck.values()) {
                    for (final ValidityCheck validity : ValidityCheck.values()) {
                        for (final RevocationCheck revocation : RevocationCheck.values()) {
                            for (final KeyUsageCheck keyUsage : KeyUsageCheck.values()) {
                                combinations.add(
                                        Arguments.of(trustMode, sanUri, hostname, validity, revocation, keyUsage));
                            }
                        }
                    }
                }
            }
        }
        return combinations;
    }
}
