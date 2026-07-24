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
 * Unit tests for {@link OidcStateStore}, a fixed-size ring buffer of login-flow state.
 */
class OidcStateStoreTest {

    @Test
    void put_thenConsume_returnsTheStoredNonceAndVerifier() {
        final OidcStateStore store = new OidcStateStore();

        store.put("state-1", "nonce-1", "verifier-1");

        final Optional<StateEntry> entry = store.consume("state-1");
        assertThat(entry).isPresent();
        assertThat(entry.get().state()).isEqualTo("state-1");
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
    void consume_stillFindsAnEntryBehindAConsumedHole() {
        // Regression: consuming a middle entry clears its slot; older entries behind the hole must remain findable.
        final OidcStateStore store = new OidcStateStore();
        store.put("A", "nonce-a", "verifier-a");
        store.put("B", "nonce-b", "verifier-b");
        store.put("C", "nonce-c", "verifier-c");

        // Consume B (the middle entry), punching a hole between A and C.
        assertThat(store.consume("B")).isPresent();

        // A (behind the hole) and C (ahead of it) must both still be consumable.
        assertThat(store.consume("A")).map(StateEntry::nonce).contains("nonce-a");
        assertThat(store.consume("C")).map(StateEntry::nonce).contains("nonce-c");
    }

    @Test
    void put_whenRingIsFull_overwritesTheOldestEntry() {
        final OidcStateStore store = new OidcStateStore(60_000L);
        // Fill the ring; the very first entry occupies the oldest slot.
        for (int i = 0; i < OidcStateStore.CAPACITY; i++) {
            store.put("state-" + i, "nonce-" + i, "verifier-" + i);
        }
        // One more put wraps around and overwrites the oldest slot (state-0).
        store.put("newest", "nonce-newest", "verifier-newest");

        assertThat(store.consume("state-0")).as("oldest entry was overwritten").isEmpty();
        assertThat(store.consume("newest")).as("newest entry is present").isPresent();
        assertThat(store.consume("state-1"))
                .as("second-oldest is still present")
                .isPresent();
    }
}
