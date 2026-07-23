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
package com.hivemq.protocols.v2.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.hivemq.adapter.sdk.api.v2.messaging.MailboxSender;
import com.hivemq.protocols.v2.manager.ProtocolAdapterHandleRegistry.ProtocolAdapterHandle;
import com.hivemq.protocols.v2.view.AdapterStatusSnapshot;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperMessage;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The REST-readable registry: basic lifecycle, and — with real threads and Awaitility, never
 * {@code Thread.sleep} — that handles and their published snapshots are safely readable from a foreign thread while
 * the (single) writer mutates the registry, honoring the actor model's snapshot-only read path.
 */
class ProtocolAdapterHandleRegistryTest {

    private static final @NotNull Duration TIMEOUT = Duration.ofSeconds(5);
    private static final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> NO_OP_SENDER = message -> {};

    @Test
    void registerFindAndUnregister() {
        final ProtocolAdapterHandleRegistry handleRegistry = new ProtocolAdapterHandleRegistry();
        final ProtocolAdapterHandle handle = handle("a", ProtocolAdapterWrapperState.STOPPED);

        handleRegistry.register(handle);

        assertThat(handleRegistry.find("a")).isSameAs(handle);
        assertThat(handleRegistry.all()).containsExactly(handle);

        handleRegistry.unregister("a");

        assertThat(handleRegistry.find("a")).isNull();
        assertThat(handleRegistry.all()).isEmpty();
    }

    @Test
    void publishedSnapshotIsVisibleFromAForeignThread() throws InterruptedException {
        final ProtocolAdapterHandleRegistry handleRegistry = new ProtocolAdapterHandleRegistry();
        final AtomicReference<AdapterStatusSnapshot> snapshot =
                new AtomicReference<>(snapshot("a", ProtocolAdapterWrapperState.WAITING_FOR_CONNECTED));
        handleRegistry.register(new ProtocolAdapterHandle("a", NO_OP_SENDER, snapshot));

        final CountDownLatch reading = new CountDownLatch(1);
        final AtomicReference<ProtocolAdapterWrapperState> seen = new AtomicReference<>();
        final Thread reader = new Thread(() -> {
            reading.countDown();
            await().atMost(TIMEOUT).until(() -> {
                final ProtocolAdapterHandle found = handleRegistry.find("a");
                return found != null && found.snapshot().get().machineState() == ProtocolAdapterWrapperState.CONNECTED;
            });
            final ProtocolAdapterHandle found = handleRegistry.find("a");
            seen.set(found == null ? null : found.snapshot().get().machineState());
        });
        reader.start();

        reading.await();
        // The owning thread publishes a new snapshot; the foreign reader must observe it (AtomicReference).
        snapshot.set(snapshot("a", ProtocolAdapterWrapperState.CONNECTED));
        reader.join(TIMEOUT.toMillis());

        assertThat(seen.get()).isEqualTo(ProtocolAdapterWrapperState.CONNECTED);
    }

    @Test
    void concurrentRegistrationsAreAllVisible() {
        final ProtocolAdapterHandleRegistry handleRegistry = new ProtocolAdapterHandleRegistry();
        final int writers = 8;
        final int perWriter = 50;
        final List<Thread> threads = new java.util.ArrayList<>(writers);
        for (int writer = 0; writer < writers; writer++) {
            final int base = writer * perWriter;
            threads.add(new Thread(() -> {
                for (int i = 0; i < perWriter; i++) {
                    handleRegistry.register(handle("adapter-" + (base + i), ProtocolAdapterWrapperState.STOPPED));
                }
            }));
        }
        threads.forEach(Thread::start);

        await().atMost(TIMEOUT).until(() -> handleRegistry.all().size() == writers * perWriter);

        assertThat(handleRegistry.all()).hasSize(writers * perWriter);
    }

    private static @NotNull ProtocolAdapterHandle handle(
            final @NotNull String adapterId, final @NotNull ProtocolAdapterWrapperState state) {
        return new ProtocolAdapterHandle(adapterId, NO_OP_SENDER, new AtomicReference<>(snapshot(adapterId, state)));
    }

    private static @NotNull AdapterStatusSnapshot snapshot(
            final @NotNull String adapterId, final @NotNull ProtocolAdapterWrapperState state) {
        return new AdapterStatusSnapshot(adapterId, state, false, false, List.of(), 0L, null);
    }
}
