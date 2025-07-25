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
package com.hivemq.extensions.iteration;

import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.common.shutdown.ShutdownHooks;
import org.jetbrains.annotations.NotNull;
import com.hivemq.util.ThreadFactoryUtil;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Christoph Schäbel
 */
@Singleton
public class AsyncIteratorFactory {

    private final @NotNull
    ExecutorService executorService;

    @Inject
    public AsyncIteratorFactory(final @NotNull ShutdownHooks shutdownHooks) {
        executorService = Executors.newFixedThreadPool(4, ThreadFactoryUtil.create("async-iterator-executor-%d"));
        shutdownHooks.add(new HiveMQShutdownHook() {
            @Override
            public @NotNull
            String name() {
                return "Async Iterator Executor Shutdown";
            }

            @Override
            public @NotNull Priority priority() {
                return Priority.MEDIUM;
            }

            @Override
            public void run() {
                executorService.shutdown();
            }
        });
    }

    @NotNull
    public <V> AsyncIterator<V> createIterator(
            final @NotNull FetchCallback<V> fetchCallback,
            final @NotNull AsyncIterator.ItemCallback<V> iterationCallback) {

        return new AsyncLocalChunkIterator<V>(fetchCallback, iterationCallback, executorService);
    }

}
