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

import com.hivemq.api.auth.oidc.OidcStateStore.StateEntry;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OidcStateStore}.
 */
class OidcStateStoreTest {

    @Test
    void put_thenConsume_returnsTheStoredNonceAndVerifier() {
        final OidcStateStore store = new OidcStateStore();

        assertThat(store.put("state-1", "nonce-1", "verifier-1")).isTrue();

        final Optional<StateEntry> entry = store.consume("state-1");
        assertThat(entry).isPresent();
        assertThat(entry.get().nonce()).isEqualTo("nonce-1");
        assertThat(entry.get().codeVerifier()).isEqualTo("verifier-1");
    }

    @Test
    void consume_isOneTimeUse_secondConsumeReturnsEmpty() {
        final OidcStateStore store = new OidcStateStore();
        store.put("state-1", "nonce-1", "verifier-1");

        assertThat(store.consume("state-1")).isPresent();
        assertThat(store.consume("state-1")).isEmpty();
    }

    @Test
    void consume_unknownState_returnsEmpty() {
        final OidcStateStore store = new OidcStateStore();

        assertThat(store.consume("never-stored")).isEmpty();
    }

    @Test
    void consume_expiredEntry_returnsEmpty() {
        // TTL of 0 → every entry is immediately expired.
        final OidcStateStore store = new OidcStateStore(0L);
        store.put("state-1", "nonce-1", "verifier-1");

        assertThat(store.consume("state-1")).isEmpty();
    }

    @Test
    void consume_removesEntryEvenWhenExpired() {
        final OidcStateStore store = new OidcStateStore(0L);
        store.put("state-1", "nonce-1", "verifier-1");
        assertThat(store.size()).isEqualTo(1);

        store.consume("state-1");

        assertThat(store.size()).isEqualTo(0);
    }

    @Test
    void put_atCapacity_isRejected() {
        // A long TTL so nothing prunes; fill to the cap, then the next put must be rejected.
        final OidcStateStore store = new OidcStateStore(60_000L);
        for (int i = 0; i < OidcStateStore.MAX_ENTRIES; i++) {
            assertThat(store.put("state-" + i, "nonce", "verifier")).isTrue();
        }
        assertThat(store.size()).isEqualTo(OidcStateStore.MAX_ENTRIES);

        assertThat(store.put("one-too-many", "nonce", "verifier")).isFalse();
    }

    @Test
    void put_atCapacityWithExpiredEntries_prunesAndAccepts() {
        // Fill the store with already-expired entries (TTL 0), then a fresh put should prune and succeed.
        final OidcStateStore store = new OidcStateStore(0L);
        for (int i = 0; i < OidcStateStore.MAX_ENTRIES; i++) {
            store.put("state-" + i, "nonce", "verifier");
        }
        assertThat(store.size()).isEqualTo(OidcStateStore.MAX_ENTRIES);

        // the expired entries are pruned inside put(), making room
        assertThat(store.put("fresh", "nonce", "verifier")).isTrue();
    }
}
