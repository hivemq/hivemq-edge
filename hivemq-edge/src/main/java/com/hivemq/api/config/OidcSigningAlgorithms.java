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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * The JWS algorithms accepted when verifying an ID token's signature.
 * <p>
 * ID-token validation accepts a token whose {@code alg} is in the configured set and rejects anything
 * else. This is the defence against algorithm downgrade: the token header (attacker-controlled) can
 * never select an algorithm outside the set, and the set contains only asymmetric algorithms. Symmetric
 * ({@code HS*}) algorithms and {@code none} are deliberately excluded — with a public JWKS the HMAC key
 * is public, so an HMAC-signed token would be forgeable, and {@code none} skips verification entirely.
 * <p>
 * The default set covers the common asymmetric families (RS/PS/ES). An operator may narrow it — down to
 * a single algorithm — to enforce exactly what their Identity Provider is registered to sign with.
 */
public final class OidcSigningAlgorithms {

    /** Accepted algorithms, in the order shown in diagnostics. Asymmetric only. */
    public static final @NotNull Set<String> ALLOWED =
            Set.of("RS256", "RS384", "RS512", "PS256", "PS384", "PS512", "ES256", "ES384", "ES512");

    /** The default accepted set when no {@code <id-token-signing-algorithms>} is configured. */
    public static final @NotNull Set<String> DEFAULT = ALLOWED;

    private OidcSigningAlgorithms() {}

    /**
     * Normalizes and validates a configured list of algorithm names against {@link #ALLOWED}.
     *
     * @throws IllegalArgumentException if a name is blank or not an accepted asymmetric algorithm
     */
    public static @NotNull Set<String> validate(final @NotNull List<String> configured) {
        final Set<String> normalized = new LinkedHashSet<>();
        for (final String raw : configured) {
            final String name = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
            if (!ALLOWED.contains(name)) {
                throw new IllegalArgumentException("OIDC id-token-signing-algorithm '" + raw
                        + "' is not a supported asymmetric algorithm; allowed: " + ALLOWED);
            }
            normalized.add(name);
        }
        return normalized;
    }
}
