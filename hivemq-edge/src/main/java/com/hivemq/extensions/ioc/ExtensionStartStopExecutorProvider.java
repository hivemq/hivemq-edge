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
package com.hivemq.extensions.ioc;

import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.common.shutdown.ShutdownHooks;
import org.jetbrains.annotations.NotNull;
import com.hivemq.util.ThreadFactoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Singleton
public class ExtensionStartStopExecutorProvider implements Provider<ExecutorService> {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ExtensionStartStopExecutorProvider.class);

    private final @NotNull ShutdownHooks shutdownHooks;

    @Inject
    public ExtensionStartStopExecutorProvider(final @NotNull ShutdownHooks shutdownHooks) {
        this.shutdownHooks = shutdownHooks;
    }

    @Override
    public ExecutorService get() {
        final ThreadFactory threadFactory = ThreadFactoryUtil.create("extension-start-stop-executor");
        final ExecutorService executorService = Executors.newSingleThreadExecutor(threadFactory);
        shutdownHooks.add(new ExtensionStartStopExecutorShutdownHook(executorService));
        return executorService;
    }

    private static class ExtensionStartStopExecutorShutdownHook implements HiveMQShutdownHook {

        private final @NotNull ExecutorService executorService;

        private ExtensionStartStopExecutorShutdownHook(final @NotNull ExecutorService executorService) {
            this.executorService = executorService;
        }

        @Override
        public @NotNull String name() {
            return "Extension Start Stop Executor";
        }

        @Override
        public @NotNull Priority priority() {
            return Priority.DOES_NOT_MATTER;
        }

        @Override
        public void run() {
            log.debug("Shutting down extension-start-stop-executor.");
            executorService.shutdown();
        }
    }
}
