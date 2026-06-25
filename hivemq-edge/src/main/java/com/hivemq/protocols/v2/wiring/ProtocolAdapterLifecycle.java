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
package com.hivemq.protocols.v2.wiring;

import com.hivemq.adapter.sdk.api.v2.messaging.Mailbox;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageDispatcher;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageDispatcherHandle;
import com.hivemq.protocols.v2.config.ProtocolAdapterExtractor;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManager;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerMessage;
import com.hivemq.protocols.v2.runtime.SystemClock;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The bootstrap lifecycle of the v2 protocol-adapter subsystem (touchpoint 5): the single {@code start}/
 * {@code stop} seam the existing Edge bootstrap calls beside the legacy framework, with <b>zero behavioral change</b>
 * to that framework. All the singletons are constructed by {@link ProtocolAdapterModule} at graph build time, but no
 * thread runs until {@link #start()} is called from {@code HiveMQEdgeGateway}, so the actor model's two-phase
 * initialization is honored:
 * <ol>
 * <li>bind the supervisor {@link ProtocolAdapterManager} to its own mailbox (it needs a send-only handle to wire each
 * wrapper's health notifications back to itself);</li>
 * <li>attach the manager to the shared {@link MessageDispatcher}, starting its single dispatch thread;</li>
 * <li>schedule the manager's periodic housekeeping tick on the shared {@link SystemClock};</li>
 * <li>register the configuration consumer so the read-only {@code <v2-protocol-adapters>} extractor {@code tell}s a
 * {@code ConfigurationChanged} at startup and on every reload — the manager applies the gentlest correct transition.</li>
 * </ol>
 * The order matters: the manager is bound and pumping before the first configuration is delivered. {@link #stop()}
 * cancels the tick, stops the manager's dispatch thread, and closes the clock — which cancels every wrapper tick too,
 * since the whole subsystem shares the one clock. Both methods are idempotent.
 */
@Singleton
public class ProtocolAdapterLifecycle {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ProtocolAdapterLifecycle.class);

    /** The manager's housekeeping cadence: a coarse health-summary tick is sufficient. */
    private static final long MANAGER_TICK_PERIOD_MILLIS = 1_000L;

    private final @NotNull ProtocolAdapterManager manager;
    private final @NotNull Mailbox<ProtocolAdapterManagerMessage> managerMailbox;
    private final @NotNull MessageDispatcher dispatcher;
    private final @NotNull SystemClock clock;
    private final @NotNull ProtocolAdapterExtractor configExtractor;

    private @Nullable MessageDispatcherHandle managerDispatcherHandle;
    private @Nullable AutoCloseable managerTickHandle;
    private boolean started;

    /**
     * @param manager          the supervisor actor.
     * @param managerMailbox   the manager's mailbox — attached to the dispatcher here and pumped on a single thread.
     * @param dispatcher       the shared production dispatcher (one thread per actor).
     * @param clock            the shared production clock; closing it on stop cancels every v2 protocol-adapter tick.
     * @param configExtractor  the read-only {@code <v2-protocol-adapters>} extractor whose consumer feeds the manager.
     */
    @Inject
    public ProtocolAdapterLifecycle(
            final @NotNull ProtocolAdapterManager manager,
            final @NotNull Mailbox<ProtocolAdapterManagerMessage> managerMailbox,
            final @NotNull MessageDispatcher dispatcher,
            final @NotNull SystemClock clock,
            final @NotNull ProtocolAdapterExtractor configExtractor) {
        this.manager = manager;
        this.managerMailbox = managerMailbox;
        this.dispatcher = dispatcher;
        this.clock = clock;
        this.configExtractor = configExtractor;
    }

    /**
     * Start the v2 protocol-adapter subsystem: bind the manager, attach it to the dispatcher, schedule its tick, and
     * register the configuration consumer (which immediately delivers the already-loaded section). Idempotent.
     */
    public synchronized void start() {
        if (started) {
            return;
        }
        manager.bindSelf(managerMailbox);
        managerDispatcherHandle = dispatcher.attach(managerMailbox, manager);
        managerTickHandle = clock.scheduleTick(
                MANAGER_TICK_PERIOD_MILLIS,
                managerMailbox,
                () -> new ProtocolAdapterManagerMessage.ProtocolAdapterManagerTick(clock.nowMillis()));
        // The extractor notifies the consumer synchronously on registration with the current section, so the manager
        // — already bound and pumping above — reconciles the startup configuration here.
        configExtractor.registerConsumer(
                configs -> managerMailbox.tell(new ProtocolAdapterManagerMessage.ConfigurationChanged(configs)));
        started = true;
        log.info("Started the v2 protocol-adapter subsystem");
    }

    /**
     * Stop the v2 protocol-adapter subsystem: cancel the manager tick, stop the manager's dispatch thread, and close
     * the clock (which cancels every wrapper's tick too, since the whole subsystem shares the one clock). Idempotent.
     */
    public synchronized void stop() {
        if (!started) {
            return;
        }
        closeQuietly(managerTickHandle);
        if (managerDispatcherHandle != null) {
            managerDispatcherHandle.close();
        }
        clock.close();
        managerTickHandle = null;
        managerDispatcherHandle = null;
        started = false;
        log.info("Stopped the v2 protocol-adapter subsystem");
    }

    private static void closeQuietly(final @Nullable AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (final Exception exception) {
            log.warn("Error closing a v2 protocol-adapter lifecycle resource", exception);
        }
    }
}
