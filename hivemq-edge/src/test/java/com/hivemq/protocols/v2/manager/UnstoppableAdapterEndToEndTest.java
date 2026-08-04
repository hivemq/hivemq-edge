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

import static com.hivemq.protocols.v2.manager.ProtocolAdapterManagerTestSupport.adapter;
import static org.assertj.core.api.Assertions.assertThat;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.v2.messaging.DefaultMailbox;
import com.hivemq.adapter.sdk.api.v2.messaging.Mailbox;
import com.hivemq.protocols.v2.config.ProtocolAdapterEntity;
import com.hivemq.protocols.v2.manager.ProtocolAdapterHandleRegistry.ProtocolAdapterHandle;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerMessage.ConfigurationChanged;
import com.hivemq.protocols.v2.runtime.FakeClock;
import com.hivemq.protocols.v2.runtime.ManualDispatcher;
import com.hivemq.protocols.v2.view.AdapterStatusSnapshot;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The whole chain against an adapter that never acknowledges its {@code stop()} (EDG-824 #19): real manager, real
 * wrapper factory, real adapter machine, virtual clock. The wrapper spends its one goal-driven stop, settles in
 * {@code ERROR}, and reports the failure; the manager completes the teardown it was holding.
 * <p>
 * Without that chain a configuration change against such an adapter never lands: the pending removal waits on a
 * {@code stopped()} that is never coming, so the replacement is never built and the id stays occupied for the
 * lifetime of the JVM.
 */
class UnstoppableAdapterEndToEndTest {

    private static final @NotNull String STUCK_PROTOCOL_ID = "stuck-stop";
    private static final long WATCHDOG_MILLIS = 1000;

    private FakeClock clock;
    private ManualDispatcher dispatcher;
    private Mailbox<ProtocolAdapterManagerMessage> mailbox;
    private ProtocolAdapterHandleRegistry handleRegistry;
    private ProtocolAdapterManager manager;

    @BeforeEach
    void setUp() {
        clock = new FakeClock();
        dispatcher = new ManualDispatcher();
        mailbox = new DefaultMailbox<>();
        handleRegistry = new ProtocolAdapterHandleRegistry();
        final ProtocolAdapterFactoryRegistry factories = new ProtocolAdapterFactoryRegistry(
                Set.of(new ProtocolAdapterManagerTestSupport.TestProtocolAdapterFactory(STUCK_PROTOCOL_ID, true)));
        manager = new ProtocolAdapterManager(
                factories,
                handleRegistry,
                new DefaultProtocolAdapterWrapperFactory(
                        clock,
                        dispatcher,
                        new MetricRegistry(),
                        new ProtocolAdapterManagerTestSupport.TestDataPointFactory(),
                        new ObjectMapper(),
                        100),
                clock);
        dispatcher.attach(mailbox, manager);
        manager.bindSelf(mailbox);
    }

    @Test
    void aFullRecreate_completesEvenThoughTheOldAdapterNeverStops() {
        send(new ConfigurationChanged(List.of(stuckAdapter(Map.of("host", "a")))));
        final ProtocolAdapterHandle original = handleRegistry.find("a");
        assertThat(original).isNotNull();
        assertThat(machineState(original)).isEqualTo(ProtocolAdapterWrapperState.CONNECTED);

        // A connection-critical change: stop now, recreate when the stop lands. It never does.
        send(new ConfigurationChanged(List.of(stuckAdapter(Map.of("host", "b")))));
        assertThat(handleRegistry.find("a")).isNull();

        settleTheStop();

        final ProtocolAdapterHandle recreated = handleRegistry.find("a");
        assertThat(recreated).isNotNull().isNotSameAs(original);
        assertThat(machineState(recreated)).isEqualTo(ProtocolAdapterWrapperState.CONNECTED);
    }

    @Test
    void aRemoval_releasesTheIdEvenThoughTheOldAdapterNeverStops() {
        send(new ConfigurationChanged(List.of(stuckAdapter(Map.of("host", "a")))));
        final ProtocolAdapterHandle original = handleRegistry.find("a");
        assertThat(original).isNotNull();

        send(new ConfigurationChanged(List.of()));
        settleTheStop();
        assertThat(handleRegistry.find("a")).isNull();

        // The id is genuinely free: a later re-add builds a fresh adapter rather than folding into a removal that
        // is still pending.
        send(new ConfigurationChanged(List.of(stuckAdapter(Map.of("host", "a")))));

        final ProtocolAdapterHandle readded = handleRegistry.find("a");
        assertThat(readded).isNotNull().isNotSameAs(original);
        assertThat(machineState(readded)).isEqualTo(ProtocolAdapterWrapperState.CONNECTED);
    }

    /**
     * Advance past the stop watchdog twice: the first expiry resets the machine to {@code ERROR} and spends its one
     * goal-driven stop, the second settles it there and reports the failure to the manager.
     */
    private void settleTheStop() {
        clock.advance(WATCHDOG_MILLIS);
        dispatcher.drainAll();
        clock.advance(WATCHDOG_MILLIS);
        dispatcher.drainAll();
    }

    private static @NotNull ProtocolAdapterEntity stuckAdapter(final @NotNull Map<String, Object> configuration) {
        return adapter("a")
                .protocolId(STUCK_PROTOCOL_ID)
                .adapterConfiguration(configuration)
                .watchdogTimeoutMillis(WATCHDOG_MILLIS)
                .build();
    }

    private static @NotNull ProtocolAdapterWrapperState machineState(final @NotNull ProtocolAdapterHandle handle) {
        final AdapterStatusSnapshot snapshot = handle.snapshot().get();
        assertThat(snapshot).isNotNull();
        return snapshot.machineState();
    }

    private void send(final @NotNull ProtocolAdapterManagerMessage message) {
        mailbox.tell(message);
        dispatcher.drainAll();
    }
}
