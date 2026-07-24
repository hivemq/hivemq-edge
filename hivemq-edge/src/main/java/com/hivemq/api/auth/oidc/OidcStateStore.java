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
import org.jetbrains.annotations.NotNull;

/**
 * In-memory store for OIDC login-flow state, bridging the {@code /login} and {@code /callback}
 * requests of a single login (the backend is otherwise stateless).
 * <p>
 * Each entry keys a random {@code state} token to the {@code nonce} and PKCE {@code codeVerifier}
 * minted at login time. Entries are:
 * <ul>
 *     <li><b>one-time use</b> — {@link #consume} clears the matched slot, preventing replay;</li>
 *     <li><b>short-lived</b> — the TTL (default 10 min) is checked on consume; an expired entry is a miss;</li>
 *     <li><b>bounded</b> — the store is a fixed-size ring buffer ({@value #CAPACITY} slots). {@link #put}
 *         never fails; when the ring is full it overwrites the oldest slot.</li>
 * </ul>
 * A flooded {@code /login} endpoint therefore churns the ring rather than blocking new logins; a
 * legitimate user whose slot is overwritten before completing simply restarts login. If the process
 * restarts mid-login, in-flight state is lost, which is acceptable.
 * <p>
 * All access is synchronized on this instance.
 */
@Singleton
public class OidcStateStore {

    static final long DEFAULT_TTL_MILLIS = 10 * 60 * 1000L;
    static final int CAPACITY = 1000;

    /**
     * The per-login secrets recovered on callback.
     *
     * @param state        the random state token this entry is keyed by
     * @param nonce        the nonce embedded in the auth request and verified against the ID token claim
     * @param codeVerifier the PKCE code verifier presented at token exchange
     * @param expiryMillis absolute epoch-millis after which the entry is invalid
     */
    public record StateEntry(
            @NotNull String state,
            @NotNull String nonce,
            @NotNull String codeVerifier,
            long expiryMillis) {

        boolean isExpired(final long nowMillis) {
            return nowMillis >= expiryMillis;
        }
    }

    // Fixed-size ring; a slot holds null when empty or after its entry has been consumed.
    private final @NotNull StateEntry[] ring = new StateEntry[CAPACITY];
    private final long ttlMillis;

    // Index of the next slot to write. Entries are written in time order around the ring.
    private int writeIndex = 0;

    @Inject
    public OidcStateStore() {
        this(DEFAULT_TTL_MILLIS);
    }

    OidcStateStore(final long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    /**
     * Stores the nonce and PKCE verifier for a login, keyed by {@code state}. Never fails: when the ring is
     * full the oldest slot is overwritten.
     */
    public synchronized void put(
            final @NotNull String state, final @NotNull String nonce, final @NotNull String codeVerifier) {
        ring[writeIndex] = new StateEntry(state, nonce, codeVerifier, System.currentTimeMillis() + ttlMillis);
        writeIndex = (writeIndex + 1) % CAPACITY;
    }

    /**
     * Removes and returns the entry for {@code state} (one-time use), or empty if it is unknown or expired.
     * <p>
     * Scans backward from the write pointer, so newer entries are found first, and continues around the whole
     * ring. Null slots (never written or already consumed) and expired entries are skipped rather than used as
     * an early stop: consuming an entry clears its slot, so a live entry can sit behind a hole.
     */
    public synchronized @NotNull Optional<StateEntry> consume(final @NotNull String state) {
        final long now = System.currentTimeMillis();
        for (int i = 0; i < CAPACITY; i++) {
            final int index = Math.floorMod(writeIndex - 1 - i, CAPACITY);
            final StateEntry entry = ring[index];
            if (entry == null || entry.isExpired(now)) {
                continue;
            }
            if (entry.state().equals(state)) {
                ring[index] = null;
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }
}
