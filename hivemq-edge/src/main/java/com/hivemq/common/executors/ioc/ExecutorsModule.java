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

    public static final @NotNull String SCHEDULED_WORKER_GROUP_NAME = "hivemq-edge-scheduled-group";
    public static final @NotNull String CACHED_WORKER_GROUP_NAME = "hivemq-edge-cached-group";

    private static final @NotNull Logger log = LoggerFactory.getLogger(ExecutorsModule.class);

    private static final @NotNull String GROUP_NAME = "hivemq-edge-group";
    private static final int SCHEDULED_WORKER_GROUP_THREAD_COUNT = 4;
    private static final @NotNull ThreadGroup coreGroup = new ThreadGroup(GROUP_NAME);

    @Provides
    @Singleton
    static @NotNull ScheduledExecutorService scheduledExecutor() {
        return Executors.newScheduledThreadPool(SCHEDULED_WORKER_GROUP_THREAD_COUNT,
                new HiveMQEdgeThreadFactory(SCHEDULED_WORKER_GROUP_NAME));
    }

    @Provides
    @Singleton
    static @NotNull ExecutorService executorService() {
        return Executors.newCachedThreadPool(new HiveMQEdgeThreadFactory(CACHED_WORKER_GROUP_NAME));
    }

    public static void shutdownExecutor(
            final @NotNull ExecutorService executor,
            final @NotNull String name,
            final int timeoutSeconds) {
        log.debug("Shutting down executor service: {}", name);

        if (!executor.isShutdown()) {
            executor.shutdown();
        }

        try {
            if (!executor.awaitTermination(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)) {
                log.warn("Executor service {} did not terminate in {}s, forcing shutdown", name, timeoutSeconds);
                executor.shutdownNow();
                if (!executor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    log.error("Executor service {} still has running tasks after forced shutdown", name);
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Executor service {} shut down successfully", name);
                }
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for executor service {} to terminate", name);
            executor.shutdownNow();
        }
    }

    private static class HiveMQEdgeThreadFactory implements ThreadFactory {
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
            thread.setUncaughtExceptionHandler((t, e) -> log.error("Uncaught exception in thread {}", t.getName(), e));
            return thread;
        }
    }
}
