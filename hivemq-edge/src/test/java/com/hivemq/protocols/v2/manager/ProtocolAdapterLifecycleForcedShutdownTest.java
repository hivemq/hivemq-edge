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
import static org.awaitility.Awaitility.await;

import com.hivemq.adapter.sdk.api.v2.messaging.DefaultMailbox;
import com.hivemq.adapter.sdk.api.v2.messaging.Mailbox;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.protocols.v2.config.ProtocolAdapterEntity;
import com.hivemq.protocols.v2.config.ProtocolAdapterExtractor;
import com.hivemq.protocols.v2.runtime.SystemClock;
import com.hivemq.protocols.v2.runtime.SystemDispatcher;
import com.hivemq.protocols.v2.wiring.ProtocolAdapterLifecycle;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState;
import java.time.Duration;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The bootstrap lifecycle's forced teardown: when an adapter never acknowledges its stop, {@link
 * ProtocolAdapterLifecycle#stop()} must not wait forever or leave the wrapper container open. With the graceful-drain
 * bound set short, it gives up on the drain, tells the manager to force every still-pending container closed, and
 * returns — so no dispatch thread, tick, or metric is orphaned when the runtime is torn down. Driven over the real
 * manager actor on a real {@link SystemDispatcher}; the wrapper is the passive {@link RecordingWrapperFactory} double,
 * which never reports {@code stopped}, so the drain is guaranteed to time out.
 */
class ProtocolAdapterLifecycleForcedShutdownTest {

    private static final @NotNull Duration TIMEOUT = Duration.ofSeconds(5);

    @Test
    void stop_forcesTeardown_whenAnAdapterNeverAcknowledgesItsStop() {
        final SystemClock clock = new SystemClock();
        final SystemDispatcher dispatcher = new SystemDispatcher();
        final ProtocolAdapterHandleRegistry handleRegistry = new ProtocolAdapterHandleRegistry();
        final RecordingWrapperFactory wrapperFactory = new RecordingWrapperFactory();
        final ProtocolAdapterFactoryRegistry factories = new ProtocolAdapterFactoryRegistry(
                Set.of(new ProtocolAdapterManagerTestSupport.TestProtocolAdapterFactory(
                        ProtocolAdapterManagerTestSupport.TEST_PROTOCOL_ID)));
        final ProtocolAdapterManager manager =
                new ProtocolAdapterManager(factories, handleRegistry, wrapperFactory, clock);
        final Mailbox<ProtocolAdapterManagerMessage> mailbox = new DefaultMailbox<>();
        final ProtocolAdapterExtractor extractor = new ProtocolAdapterExtractor();

        // A short graceful-drain bound so the forced-teardown path is reached without the production wait.
        final ProtocolAdapterLifecycle lifecycle =
                new ProtocolAdapterLifecycle(manager, mailbox, dispatcher, clock, extractor, 200L);
        lifecycle.start();

        // Deliver one configured adapter through the real extractor, then mark it CONNECTED so shutdown must stop it.
        extractor.updateConfig(configWith(adapter("a").build()));
        await().atMost(TIMEOUT).until(() -> handleRegistry.find("a") != null);
        wrapperFactory.setMachineState("a", ProtocolAdapterWrapperState.CONNECTED);

        // The adapter never acknowledges its stop: stop() gives up on the graceful drain and forces the teardown.
        lifecycle.stop();

        assertThat(wrapperFactory.closedAdapterIds()).containsExactly("a");
        assertThat(handleRegistry.find("a")).isNull();
    }

    private static @NotNull HiveMQConfigEntity configWith(final @NotNull ProtocolAdapterEntity entity) {
        final HiveMQConfigEntity config = new HiveMQConfigEntity();
        config.getV2().getProtocolAdapters().add(entity);
        return config;
    }
}
