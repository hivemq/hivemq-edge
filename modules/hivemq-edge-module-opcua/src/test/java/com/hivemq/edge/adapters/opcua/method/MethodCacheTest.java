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
package com.hivemq.edge.adapters.opcua.method;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/// What the cache remembers, and what it refuses to remember.
///
/// The rule it rests on: a method's NodeId is the server's to assign and stable for the session, so the only
/// thing that can make an entry stale is the session ending. Everything here pins that, and the two outcomes it
/// must not keep.
class MethodCacheTest {

    private static final MethodVerifier.Result VERIFIED = new MethodVerifier.Result.Verified(List.of());

    @Test
    void secondCallForOneTag_doesNotReadAgain() {
        final MethodCache cache = new MethodCache();
        final AtomicInteger reads = new AtomicInteger();

        cache.get("boiler1", () -> {
            reads.incrementAndGet();
            return CompletableFuture.completedFuture(VERIFIED);
        });
        cache.get("boiler1", () -> {
            reads.incrementAndGet();
            return CompletableFuture.completedFuture(VERIFIED);
        });

        assertThat(reads).hasValue(1);
    }

    @Test
    void differentTags_areReadIndependently() {
        // Two tags may name one object and still be configured differently, so the answer is per tag.
        final MethodCache cache = new MethodCache();
        final AtomicInteger reads = new AtomicInteger();

        cache.get("boiler1", () -> read(reads));
        cache.get("boiler2", () -> read(reads));

        assertThat(reads).hasValue(2);
    }

    @Test
    void invalidate_forgetsEverything() {
        // The session ended, so every NodeId it held may have been renumbered.
        final MethodCache cache = new MethodCache();
        final AtomicInteger reads = new AtomicInteger();

        cache.get("boiler1", () -> read(reads));
        cache.invalidate();
        cache.get("boiler1", () -> read(reads));

        assertThat(reads).hasValue(2);
    }

    @Test
    void inFlightRequests_shareOneRead() {
        // A request is admitted as soon as the previous write completes, so a method tag can have several in
        // flight. Without sharing, each would issue its own browse of the same object.
        final MethodCache cache = new MethodCache();
        final AtomicInteger reads = new AtomicInteger();
        final CompletableFuture<MethodVerifier.Result> pending = new CompletableFuture<>();

        final var first = cache.get("boiler1", () -> {
            reads.incrementAndGet();
            return pending;
        });
        final var second = cache.get("boiler1", () -> {
            reads.incrementAndGet();
            return pending;
        });

        assertThat(reads).hasValue(1);
        pending.complete(VERIFIED);
        assertThat(first.join()).isSameAs(VERIFIED);
        assertThat(second.join()).isSameAs(VERIFIED);
    }

    @Test
    void failedRead_isNotRemembered() {
        // One unanswered browse must not become a permanent fact about the device.
        final MethodCache cache = new MethodCache();
        final AtomicInteger reads = new AtomicInteger();

        final var failed = cache.get("boiler1", () -> {
            reads.incrementAndGet();
            return CompletableFuture.failedFuture(new RuntimeException("no route to host"));
        });
        assertThat(failed).isCompletedExceptionally();

        cache.get("boiler1", () -> read(reads));

        assertThat(reads).hasValue(2);
    }

    @Test
    void unverifiable_isNotRemembered() {
        // The device could not be asked. That is not a verdict about the device, so the next request asks again
        // rather than inheriting an answer about a connection that may since have recovered.
        final MethodCache cache = new MethodCache();
        final AtomicInteger reads = new AtomicInteger();

        cache.get("boiler1", () -> {
            reads.incrementAndGet();
            return CompletableFuture.completedFuture(new MethodVerifier.Result.Unverifiable("timed out"));
        });
        cache.get("boiler1", () -> read(reads));

        assertThat(reads).hasValue(2);
    }

    @Test
    void rejected_isRemembered() {
        // "Not an object" and "not there" are durable for the session. Re-browsing would learn the same thing
        // and cost a round trip per request to do it.
        final MethodCache cache = new MethodCache();
        final AtomicInteger reads = new AtomicInteger();

        cache.get("boiler1", () -> {
            reads.incrementAndGet();
            return CompletableFuture.completedFuture(new MethodVerifier.Result.Rejected("not an Object"));
        });
        cache.get("boiler1", () -> read(reads));

        assertThat(reads).hasValue(1);
    }

    private static CompletableFuture<MethodVerifier.Result> read(final AtomicInteger reads) {
        reads.incrementAndGet();
        return CompletableFuture.completedFuture(VERIFIED);
    }
}
