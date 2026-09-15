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

import org.junit.jupiter.api.Test;

/**
 * The contract of {@link EffectiveChecks#hasStandaloneEndEntityChecks()}: it aggregates exactly the
 * checks {@code CertificateChecks.apply} can enforce on the end-entity certificate without building a
 * certification path. It decides whether a chainless trust mode installs a check-only validator or
 * none at all, so claiming a check that cannot be performed there — or missing one that can — would
 * silently change which validator runs.
 */
class EffectiveChecksTest {

    @Test
    void theHelperAggregatesExactlyTheFourStandaloneAxes() {
        // Exhaustive over the four axes CertificateChecks.apply enforces: sanUri, hostname, validity,
        // keyUsage. The helper must be their plain disjunction, for every combination.
        for (final SanUriCheck sanUri : SanUriCheck.values()) {
            for (final HostnameCheck hostname : HostnameCheck.values()) {
                for (final ValidityCheck validity : ValidityCheck.values()) {
                    for (final KeyUsageCheck keyUsage : KeyUsageCheck.values()) {
                        final EffectiveChecks checks = new EffectiveChecks(
                                TrustMode.ANY_CERT, sanUri, hostname, validity, RevocationCheck.NONE, keyUsage);

                        assertThat(checks.hasStandaloneEndEntityChecks())
                                .as("%s", checks.describe())
                                .isEqualTo(checks.isSanUri()
                                        || checks.isHostname()
                                        || checks.isValidity()
                                        || checks.isKeyUsage());
                    }
                }
            }
        }
    }

    @Test
    void revocationDoesNotCountAsAStandaloneCheck() {
        // Revocation is decidable only while a certification path is built; CertificateChecks.apply
        // deliberately cannot perform it, and the projection rejects revocation without CHAIN before
        // any chainless validator is selected. This EffectiveChecks is therefore not constructible
        // through the projection - it is built directly to pin that the helper does not claim a check
        // the chainless validators cannot perform.
        final EffectiveChecks revocationOnly = new EffectiveChecks(
                TrustMode.ANY_CERT,
                SanUriCheck.NONE,
                HostnameCheck.NONE,
                ValidityCheck.NONE,
                RevocationCheck.CHECK,
                KeyUsageCheck.NONE);

        assertThat(revocationOnly.hasStandaloneEndEntityChecks()).isFalse();
    }
}
