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
package com.hivemq.protocols.northbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.polling.PollingOutput;
import com.hivemq.adapter.sdk.api.polling.PollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingProtocolAdapter;
import com.hivemq.edge.modules.adapters.data.TagManager;
import com.hivemq.edge.modules.api.events.model.EventBuilderImpl;
import com.hivemq.protocols.PollingContextWrapper;
import com.hivemq.protocols.ProtocolAdapterWrapper;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/// Both real samplers hand the polling task a future that fails as soon as the driver reports its failure, while the
/// sampler's own WARN for that failure is still blocked in the log appender. Before the fix the future waited for the
/// appender, so a slow appender turned a reported polling error into a watchdog hit.
class SamplerBlockedLoggingTest {

    /// Per-tag sampler: the driver fails the poll from its own thread; the appender blocks.
    @Test
    void perContextSampler_whenTheAppenderBlocks_thenTheReportedFailureReachesThePollingTask() throws Exception {
        final AtomicReference<PollingOutput> output = new AtomicReference<>();
        final PollingProtocolAdapter adapter = mock();
        doAnswer(invocation -> {
                    output.set(invocation.getArgument(1));
                    return null;
                })
                .when(adapter)
                .poll(any(), any());
        final ProtocolAdapterWrapper wrapper = wrapperOf(adapter);
        final PerContextSampler sampler = new PerContextSampler(
                wrapper,
                new PollingContextWrapper(
                        "unused", "tag-1", MessageHandlingOptions.MQTTMessagePerTag, false, false, List.of(), 1, -1L),
                eventService(),
                mock(TagManager.class));

        assertFailureArrivesWhileTheAppenderBlocks(PerContextSampler.class, sampler::execute, output);
    }

    /// Batch sampler: the same, through the batch polling path.
    @Test
    void perAdapterSampler_whenTheAppenderBlocks_thenTheReportedFailureReachesThePollingTask() throws Exception {
        final AtomicReference<PollingOutput> output = new AtomicReference<>();
        final BatchPollingProtocolAdapter adapter = mock();
        doAnswer(invocation -> {
                    output.set(invocation.getArgument(1));
                    return null;
                })
                .when(adapter)
                .poll(any(), any());
        final PerAdapterSampler sampler =
                new PerAdapterSampler(wrapperOf(adapter), eventService(), mock(TagManager.class));

        assertFailureArrivesWhileTheAppenderBlocks(PerAdapterSampler.class, sampler::execute, output);
    }

    private static void assertFailureArrivesWhileTheAppenderBlocks(
            final @NotNull Class<?> samplerClass,
            final @NotNull Execution execution,
            final @NotNull AtomicReference<PollingOutput> output)
            throws Exception {
        final CountDownLatch appenderEntered = new CountDownLatch(1);
        final CountDownLatch releaseAppender = new CountDownLatch(1);
        final Logger logger = (Logger) LoggerFactory.getLogger(samplerClass);
        final AppenderBase<ILoggingEvent> blocking = new AppenderBase<>() {
            @Override
            protected void append(final @NotNull ILoggingEvent event) {
                appenderEntered.countDown();
                try {
                    if (!releaseAppender.await(10, TimeUnit.SECONDS)) {
                        throw new AssertionError("appender was never released");
                    }
                } catch (final InterruptedException _) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        blocking.start();
        logger.addAppender(blocking);
        Thread driver = null;
        try {
            final CompletableFuture<?> watched = execution.execute();
            assertThat(output.get()).isNotNull();
            driver = Thread.ofVirtual().name("test-driver-poll-failure").start(() -> output.get()
                    .fail(new IllegalStateException("device unreachable"), "unreachable"));
            assertThat(appenderEntered.await(5, TimeUnit.SECONDS)).isTrue();

            assertThatThrownBy(() -> watched.copy().get(2, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class);
        } finally {
            releaseAppender.countDown();
            logger.detachAppender(blocking);
            if (driver != null) {
                driver.join(5_000);
            }
        }
    }

    private static @NotNull ProtocolAdapterWrapper wrapperOf(final @NotNull Object adapter) {
        final ProtocolAdapterWrapper wrapper = mock(RETURNS_DEEP_STUBS);
        when(wrapper.getId()).thenReturn("adapter-1");
        when(wrapper.getAdapter()).thenReturn((ProtocolAdapter) adapter);
        return wrapper;
    }

    private static @NotNull EventService eventService() {
        final EventService eventService = mock();
        when(eventService.createAdapterEvent(any(), any())).thenReturn(new EventBuilderImpl(_ -> {}));
        return eventService;
    }

    @FunctionalInterface
    private interface Execution {
        @NotNull
        CompletableFuture<?> execute();
    }
}
