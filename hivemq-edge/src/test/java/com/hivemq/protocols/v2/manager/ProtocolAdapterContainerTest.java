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
import static org.assertj.core.api.Assertions.assertThatCode;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.protocols.v2.config.ProtocolAdapterEntity;
import com.hivemq.protocols.v2.manager.ProtocolAdapterHandleRegistry.ProtocolAdapterHandle;
import com.hivemq.protocols.v2.runtime.ProtocolAdapterMetrics;
import com.hivemq.protocols.v2.view.AdapterStatusSnapshot;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The managed adapter's teardown: {@link ProtocolAdapterContainer#close()} releases every resource the manager owns
 * for one adapter — the wrapper's dispatcher binding, the protocol adapter's own dispatcher binding (so a template
 * adapter's dispatch thread is not leaked), the periodic tick, and the metrics — and is a harmless no-op for an
 * unknown / un-instantiable adapter.
 */
class ProtocolAdapterContainerTest {

    @Test
    void close_releasesEveryResource_includingTheProtocolAdapterDispatcherBinding() {
        final AtomicBoolean wrapperBindingClosed = new AtomicBoolean();
        final AtomicBoolean adapterBindingClosed = new AtomicBoolean();
        final AtomicBoolean tickClosed = new AtomicBoolean();
        final ProtocolAdapterEntity entity = adapter("a").build();
        final ProtocolAdapterContainer container = new ProtocolAdapterContainer(
                handle(entity),
                () -> wrapperBindingClosed.set(true),
                () -> adapterBindingClosed.set(true),
                () -> tickClosed.set(true),
                new ProtocolAdapterMetrics(new MetricRegistry(), "a", () -> 0),
                entity);

        container.close();

        assertThat(wrapperBindingClosed).isTrue();
        assertThat(adapterBindingClosed).isTrue();
        assertThat(tickClosed).isTrue();
    }

    @Test
    void close_doesNotFail_whenTheAdapterOwnsNoDispatchThread() {
        // A direct adapter that attaches no mailbox of its own carries a null adapter dispatcher binding; close() must
        // simply skip it.
        final ProtocolAdapterEntity entity = adapter("a").build();
        final ProtocolAdapterContainer container = new ProtocolAdapterContainer(
                handle(entity),
                () -> {},
                null,
                () -> {},
                new ProtocolAdapterMetrics(new MetricRegistry(), "a", () -> 0),
                entity);

        assertThatCode(container::close).doesNotThrowAnyException();
    }

    @Test
    void unknownContainer_isNotReal_andCloseIsANoOp() {
        final ProtocolAdapterEntity entity = adapter("a").build();
        final ProtocolAdapterContainer container = ProtocolAdapterContainer.unknown(handle(entity), entity);

        assertThat(container.isReal()).isFalse();
        assertThatCode(container::close).doesNotThrowAnyException();
    }

    private static @NotNull ProtocolAdapterHandle handle(final @NotNull ProtocolAdapterEntity entity) {
        return new ProtocolAdapterHandle(
                entity.getAdapterId(),
                message -> {},
                new AtomicReference<>(new AdapterStatusSnapshot(
                        entity.getAdapterId(),
                        ProtocolAdapterWrapperState.STOPPED,
                        false,
                        false,
                        List.of(),
                        0L,
                        null)));
    }
}
