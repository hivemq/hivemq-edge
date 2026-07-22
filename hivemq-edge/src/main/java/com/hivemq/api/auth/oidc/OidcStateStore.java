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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-memory store for OIDC login-flow state, bridging the {@code /login} and {@code /callback}
 * requests of a single login (the backend is otherwise stateless).
 * <p>
 * Each entry keys a random {@code state} token to the {@code nonce} and PKCE {@code codeVerifier}
 * minted at login time. Entries are:
 * <ul>
 *     <li><b>one-time use</b> — {@link #consume} removes the entry, preventing replay;</li>
 *     <li><b>short-lived</b> — expired entries (default 10 min) are rejected and pruned lazily on access;</li>
 *     <li><b>bounded</b> — the map is capped ({@value #MAX_ENTRIES}); {@link #put} is rejected when full,
 *         to bound memory under a flood of login requests.</li>
 * </ul>
 * If the process restarts mid-login the user simply restarts login; losing in-flight state is acceptable.
 */
@Singleton
public class OidcStateStore {

    private static final @NotNull Logger log = LoggerFactory.getLogger(OidcStateStore.class);

    static final long DEFAULT_TTL_MILLIS = 10 * 60 * 1000L;
    static final int MAX_ENTRIES = 1000;

    /**
     * The per-login secrets recovered on callback.
     *
     * @param nonce        the nonce embedded in the auth request and verified against the ID token claim
     * @param codeVerifier the PKCE code verifier presented at token exchange
     * @param expiryMillis  absolute epoch-millis after which the entry is invalid
     */
    public record StateEntry(@NotNull String nonce, @NotNull String codeVerifier, long expiryMillis) {

        boolean isExpired(final long nowMillis) {
            return nowMillis >= expiryMillis;
        }
    }

    private final @NotNull ConcurrentHashMap<String, StateEntry> entries = new ConcurrentHashMap<>();
    private final long ttlMillis;

    @Inject
    public OidcStateStore() {
        this(DEFAULT_TTL_MILLIS);
    }

    OidcStateStore(final long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    /**
     * Stores the nonce and PKCE verifier for a login, keyed by {@code state}.
     *
     * @return {@code true} if stored; {@code false} if the store is at capacity (login should be rejected).
     */
    public boolean put(final @NotNull String state, final @NotNull String nonce, final @NotNull String codeVerifier) {
        if (entries.size() >= MAX_ENTRIES) {
            pruneExpired();
            if (entries.size() >= MAX_ENTRIES) {
                log.warn("OIDC state store is at capacity ({} entries); rejecting login request.", MAX_ENTRIES);
                return false;
            }
        }
        entries.put(state, new StateEntry(nonce, codeVerifier, System.currentTimeMillis() + ttlMillis));
        return true;
    }

    /**
     * Atomically removes and returns the entry for {@code state} (one-time use).
     *
     * @return the entry, or empty if the state is unknown or expired.
     */
    public @NotNull Optional<StateEntry> consume(final @NotNull String state) {
        final StateEntry entry = entries.remove(state);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.isExpired(System.currentTimeMillis())) {
            return Optional.empty();
        }
        return Optional.of(entry);
    }

    private void pruneExpired() {
        final long now = System.currentTimeMillis();
        entries.values().removeIf(entry -> entry.isExpired(now));
    }

    int size() {
        return entries.size();
    }
}
