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
package com.hivemq.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OidcSigningAlgorithms#validate}: the accepted ID-token signing algorithms are
 * restricted to asymmetric families, and {@code none}/HMAC are rejected as downgrade vectors.
 */
class OidcSigningAlgorithmsTest {

    @Test
    void validate_acceptsAsymmetricAlgorithms() {
        assertThat(OidcSigningAlgorithms.validate(List.of("RS256", "ES384", "PS512")))
                .containsExactlyInAnyOrder("RS256", "ES384", "PS512");
    }

    @Test
    void validate_isCaseInsensitiveAndTrims() {
        assertThat(OidcSigningAlgorithms.validate(List.of("  rs256 ", "Es256")))
                .containsExactlyInAnyOrder("RS256", "ES256");
    }

    @Test
    void validate_rejectsNone() {
        assertThatThrownBy(() -> OidcSigningAlgorithms.validate(List.of("none")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a supported asymmetric algorithm");
    }

    @Test
    void validate_rejectsHmac() {
        // With a public JWKS the HMAC key is public, so an HS256-signed token would be forgeable.
        assertThatThrownBy(() -> OidcSigningAlgorithms.validate(List.of("HS256")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a supported asymmetric algorithm");
    }

    @Test
    void validate_rejectsAnUnknownName() {
        assertThatThrownBy(() -> OidcSigningAlgorithms.validate(List.of("RS256", "made-up")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("made-up");
    }

    @Test
    void defaultSet_isTheFullAsymmetricAllowList() {
        assertThat(OidcSigningAlgorithms.DEFAULT).isEqualTo(OidcSigningAlgorithms.ALLOWED);
        assertThat(OidcSigningAlgorithms.ALLOWED).doesNotContain("HS256").doesNotContain("none");
    }
}
