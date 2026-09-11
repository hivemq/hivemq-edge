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

import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.common.shutdown.ShutdownHooks;
import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import org.jetbrains.annotations.NotNull;

/**
 * @author Simon L Johnson
 */
@Module
public abstract class ExecutorsModule {

    static final String GROUP_NAME = "hivemq-edge-group";
    static final String SCHEDULED_WORKER_GROUP_NAME = "hivemq-edge-scheduled-group";
    static final String CACHED_WORKER_GROUP_NAME = "hivemq-edge-cached-group";
    private static final ThreadGroup coreGroup = new ThreadGroup(GROUP_NAME);

    @Provides
    @Singleton
    static ScheduledExecutorService scheduledExecutor(final @NotNull ShutdownHooks shutdownHooks) {
        final ScheduledExecutorService service =
                Executors.newScheduledThreadPool(4, new HiveMQEdgeThreadFactory(SCHEDULED_WORKER_GROUP_NAME));
        shutdownHooks.add(shutdownHook("Edge Scheduled Executor Shutdown", service));
        return service;
    }

    @Provides
    @Singleton
    static ExecutorService executorService(final @NotNull ShutdownHooks shutdownHooks) {
        final ExecutorService service =
                Executors.newCachedThreadPool(new HiveMQEdgeThreadFactory(CACHED_WORKER_GROUP_NAME));
        shutdownHooks.add(shutdownHook("Edge Cached Executor Shutdown", service));
        return service;
    }

    /**
     * Without this the pool's idle workers stay parked for the lifetime of the JVM. That is invisible in
     * production (the process exits anyway) but decisive in tests, which start and stop an embedded Edge
     * hundreds of times in one JVM: threads are GC roots, so every parked worker keeps its whole object
     * graph -- including the retired Edge that created it -- permanently reachable.
     */
    private static @NotNull HiveMQShutdownHook shutdownHook(
            final @NotNull String name, final @NotNull ExecutorService service) {
        return new HiveMQShutdownHook() {
            @Override
            public @NotNull String name() {
                return name;
            }

            @Override
            public @NotNull Priority priority() {
                return Priority.MEDIUM;
            }

            @Override
            public void run() {
                service.shutdownNow();
            }
        };
    }

    static class HiveMQEdgeThreadFactory implements ThreadFactory {
        private final String factoryName;
        private final ThreadGroup group;
        private volatile int counter = 0;

        public HiveMQEdgeThreadFactory(final String factoryName) {
            this.factoryName = factoryName;
            this.group = new ThreadGroup(coreGroup, factoryName);
        }

        @Override
        public Thread newThread(final Runnable r) {
            synchronized (group) {
                Thread thread = new Thread(coreGroup, r, String.format(factoryName + "-%d", counter++));
                // Daemon so a missed pool shutdown cannot keep the JVM alive, and so an embedded Edge that
                // is stopped does not leave its workers parked for the lifetime of the process. Threads are
                // GC roots: a parked worker is never reclaimed and pins everything it references.
                thread.setDaemon(true);
                return thread;
            }
        }
    }
}
