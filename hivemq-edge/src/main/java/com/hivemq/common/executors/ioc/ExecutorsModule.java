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
package com.hivemq.common.executors.ioc;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Module
public abstract class ExecutorsModule {

    private static final Logger log = LoggerFactory.getLogger(ExecutorsModule.class);

    static final @NotNull String GROUP_NAME = "hivemq-edge-group";
    static final @NotNull String SCHEDULED_WORKER_GROUP_NAME = "hivemq-edge-scheduled-group";
    static final @NotNull String CACHED_WORKER_GROUP_NAME = "hivemq-edge-cached-group";
    private static final @NotNull ThreadGroup coreGroup = new ThreadGroup(GROUP_NAME);

    @Provides
    @Singleton
    static @NotNull ScheduledExecutorService scheduledExecutor() {
        final ScheduledExecutorService executor =
                Executors.newScheduledThreadPool(4, new HiveMQEdgeThreadFactory(SCHEDULED_WORKER_GROUP_NAME));
        registerShutdownHook(executor, SCHEDULED_WORKER_GROUP_NAME);
        return executor;
    }

    @Provides
    @Singleton
    static @NotNull ExecutorService executorService() {
        final ExecutorService executor =
                Executors.newCachedThreadPool(new HiveMQEdgeThreadFactory(CACHED_WORKER_GROUP_NAME));
        registerShutdownHook(executor, CACHED_WORKER_GROUP_NAME);
        return executor;
    }

    private static void registerShutdownHook(final @NotNull ExecutorService executor, final @NotNull String name) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.debug("Shutting down executor service: {}", name);
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                    log.warn("Executor service {} did not terminate in time, forcing shutdown", name);
                    executor.shutdownNow();
                }
            } catch (final InterruptedException e) {
                log.warn("Interrupted while waiting for executor service {} to terminate", name);
                Thread.currentThread().interrupt();
                executor.shutdownNow();
            }
        }, "shutdown-hook-" + name));
    }

    static class HiveMQEdgeThreadFactory implements ThreadFactory {
        private final @NotNull String factoryName;
        private final @NotNull ThreadGroup group;
        private final @NotNull AtomicInteger counter = new AtomicInteger(0);

        public HiveMQEdgeThreadFactory(final @NotNull String factoryName) {
            this.factoryName = factoryName;
            this.group = new ThreadGroup(coreGroup, factoryName);
        }

        @Override
        public @NotNull Thread newThread(final @NotNull Runnable r) {
            final Thread thread = new Thread(group, r, factoryName + "-" + counter.getAndIncrement());
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler((t, e) ->
                log.error("Uncaught exception in thread {}", t.getName(), e));
            return thread;
        }
    }
}
