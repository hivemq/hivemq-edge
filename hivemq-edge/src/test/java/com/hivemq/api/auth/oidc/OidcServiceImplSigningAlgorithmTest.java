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
package com.hivemq.api.auth.oidc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.SubjectType;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import java.net.URI;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OidcServiceImpl#selectSigningAlgorithm}: the ID token signing algorithm is
 * chosen from what the provider advertises, restricted to asymmetric algorithms. It is never taken
 * from the token header, which an attacker controls.
 */
class OidcServiceImplSigningAlgorithmTest {

    @Test
    void selects_rs256_whenTheProviderAdvertisesIt() {
        assertThat(OidcServiceImpl.selectSigningAlgorithm(metadata(JWSAlgorithm.RS256)))
                .isEqualTo(JWSAlgorithm.RS256);
    }

    @Test
    void selects_es256_whenThatIsTheOnlyAdvertisedAlgorithm() {
        // A provider that has migrated away from RS256 must still work.
        assertThat(OidcServiceImpl.selectSigningAlgorithm(metadata(JWSAlgorithm.ES256)))
                .isEqualTo(JWSAlgorithm.ES256);
    }

    @Test
    void selects_ps256_whenAdvertised() {
        assertThat(OidcServiceImpl.selectSigningAlgorithm(metadata(JWSAlgorithm.PS256)))
                .isEqualTo(JWSAlgorithm.PS256);
    }

    @Test
    void prefersRs256_whenSeveralAreAdvertised() {
        // The choice must be deterministic rather than depending on the provider's ordering.
        assertThat(OidcServiceImpl.selectSigningAlgorithm(
                        metadata(JWSAlgorithm.ES512, JWSAlgorithm.RS256, JWSAlgorithm.PS384)))
                .isEqualTo(JWSAlgorithm.RS256);
    }

    @Test
    void defaultsToRs256_whenTheProviderAdvertisesNothing() {
        // RS256 is required of every OpenID Provider, so it is the safe default.
        assertThat(OidcServiceImpl.selectSigningAlgorithm(metadata())).isEqualTo(JWSAlgorithm.RS256);
    }

    @Test
    void rejects_hmacOnly_becauseTheClientSecretCouldForgeTokens() {
        assertThatThrownBy(() -> OidcServiceImpl.selectSigningAlgorithm(metadata(JWSAlgorithm.HS256)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no supported ID token signing algorithm");
    }

    @Test
    void rejects_none_becauseItSkipsVerificationEntirely() {
        assertThatThrownBy(() -> OidcServiceImpl.selectSigningAlgorithm(metadata(new JWSAlgorithm("none"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no supported ID token signing algorithm");
    }

    private static @NotNull OIDCProviderMetadata metadata(final @NotNull JWSAlgorithm... idTokenAlgs) {
        final OIDCProviderMetadata metadata = new OIDCProviderMetadata(
                new Issuer("https://idp.example.com"), List.of(SubjectType.PUBLIC), URI.create("https://idp/jwks"));
        if (idTokenAlgs.length > 0) {
            metadata.setIDTokenJWSAlgs(List.of(idTokenAlgs));
        }
        return metadata;
    }
}
