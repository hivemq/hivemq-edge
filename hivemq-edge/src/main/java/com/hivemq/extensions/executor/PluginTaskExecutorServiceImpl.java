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
package com.hivemq.extensions.executor;

import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.common.shutdown.ShutdownHooks;
import org.jetbrains.annotations.NotNull;
import com.hivemq.extensions.executor.task.*;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.configuration.service.InternalConfigurations.EXTENSION_TASK_QUEUE_EXECUTOR_THREADS_COUNT;

/**
 * @author Christoph Schäbel
 */
@Singleton
public class PluginTaskExecutorServiceImpl implements PluginTaskExecutorService {

    private final @NotNull PluginTaskExecutor[] taskExecutors;

    private final int taskExecutorCount;

    @Inject
    public PluginTaskExecutorServiceImpl(
            final @NotNull Provider<PluginTaskExecutor> taskExecutorProvider,
            final @NotNull ShutdownHooks shutdownHooks) {

        taskExecutorCount = EXTENSION_TASK_QUEUE_EXECUTOR_THREADS_COUNT.get();

        taskExecutors = new PluginTaskExecutor[taskExecutorCount];

        for (int i = 0; i < taskExecutorCount; i++) {
            taskExecutors[i] = taskExecutorProvider.get();
        }

        shutdownHooks.add(new PluginTaskExecutorServiceShutdownHook(taskExecutors));
    }

    @Override
    public <I extends PluginTaskInput> void handlePluginInTaskExecution(
            final @NotNull PluginInTaskContext pluginInTaskContext,
            final @NotNull Supplier<I> pluginInputSupplier,
            final @NotNull PluginInTask<I> pluginTask) {
        final PluginTaskExecutor taskExecutor = getPluginTaskExecutor(pluginInTaskContext);

        checkNotNull(pluginInTaskContext, "Extension context cannot be null");
        checkNotNull(pluginInputSupplier, "Input supplier cannot be null");
        checkNotNull(pluginTask, "Extension task cannot be null");

        taskExecutor.handlePluginTaskExecution(new PluginTaskExecution<I, DefaultPluginTaskOutput>(
                pluginInTaskContext, pluginInputSupplier, null, pluginTask));

    }

    @Override
    public <O extends PluginTaskOutput> void handlePluginOutTaskExecution(
            final @NotNull PluginOutTaskContext<O> pluginOutTaskContext,
            final @NotNull Supplier<O> pluginOutputSupplier,
            final @NotNull PluginOutTask<O> pluginTask) {

        checkNotNull(pluginOutTaskContext, "Extension context cannot be null");
        checkNotNull(pluginOutputSupplier, "Output supplier cannot be null");
        checkNotNull(pluginTask, "Extension task cannot be null");

        final PluginTaskExecutor taskExecutor = getPluginTaskExecutor(pluginOutTaskContext);
        taskExecutor.handlePluginTaskExecution(new PluginTaskExecution<DefaultPluginTaskInput, O>(
                pluginOutTaskContext, null, pluginOutputSupplier, pluginTask));
    }

    @Override
    public <I extends PluginTaskInput, O extends PluginTaskOutput> void handlePluginInOutTaskExecution(
            final @NotNull PluginInOutTaskContext<O> pluginInOutContext, final @NotNull Supplier<I> pluginInputSupplier,
            final @NotNull Supplier<O> pluginOutputSupplier, final @NotNull PluginInOutTask<I, O> pluginTask) {

        checkNotNull(pluginInOutContext, "Extension context cannot be null");
        checkNotNull(pluginInputSupplier, "Input supplier cannot be null");
        checkNotNull(pluginOutputSupplier, "Output supplier cannot be null");
        checkNotNull(pluginTask, "Extension task cannot be null");

        final PluginTaskExecutor taskExecutor = getPluginTaskExecutor(pluginInOutContext);
        taskExecutor.handlePluginTaskExecution(new PluginTaskExecution<>(
                pluginInOutContext, pluginInputSupplier, pluginOutputSupplier, pluginTask));
    }

    @NotNull
    private PluginTaskExecutor getPluginTaskExecutor(final @NotNull PluginTaskContext pluginTaskContext) {
        final int bucket = BucketUtils.getBucket(pluginTaskContext.getIdentifier(), taskExecutorCount);
        return taskExecutors[bucket];
    }

    private static class PluginTaskExecutorServiceShutdownHook implements HiveMQShutdownHook {

        private final @NotNull PluginTaskExecutor[] taskExecutors;

        PluginTaskExecutorServiceShutdownHook(final @NotNull PluginTaskExecutor[] taskExecutors) {
            this.taskExecutors = taskExecutors;
        }

        @Override
        public @NotNull String name() {
            return "Plugin Task Executor Service Shutdown Hook";
        }

        @Override
        public void run() {
            for (final PluginTaskExecutor taskExecutor : taskExecutors) {
                taskExecutor.stop();
            }
        }
    }
}
