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
package com.hivemq.extensions.services.executor;

import org.jetbrains.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.CompletableScheduledFuture;
import com.hivemq.extension.sdk.api.services.ManagedExtensionExecutorService;
import com.hivemq.extensions.HiveMQExtensions;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class ManagedExecutorServicePerExtension implements ManagedExtensionExecutorService {

    @NotNull
    private final GlobalManagedExtensionExecutorService managedPluginExecutorService;

    @NotNull
    private final ClassLoader classLoader;

    @NotNull
    private final HiveMQExtensions hiveMQExtensions;

    public ManagedExecutorServicePerExtension(
            final @NotNull GlobalManagedExtensionExecutorService managedPluginExecutorService,
            final @NotNull ClassLoader classLoader, final @NotNull HiveMQExtensions hiveMQExtensions) {
        this.managedPluginExecutorService = managedPluginExecutorService;
        this.classLoader = classLoader;
        this.hiveMQExtensions = hiveMQExtensions;
    }

    @Override
    public void execute(final @NotNull Runnable command) {
        if (!isShutdown()) {
            managedPluginExecutorService.execute(new WrappedRunnable(command, classLoader, null));
        }
    }

    @NotNull
    @Override
    public CompletableScheduledFuture<?> schedule(
            final @NotNull Runnable command, final long delay, final @NotNull TimeUnit unit) {
        final CompletableScheduledFutureImpl<?> completableScheduledFuture = new CompletableScheduledFutureImpl<>();
        final ScheduledFuture<?> scheduledFuture = managedPluginExecutorService.schedule(
                new WrappedRunnable(command, classLoader, completableScheduledFuture), delay, unit);
        completableScheduledFuture.setScheduledFuture(scheduledFuture);
        return completableScheduledFuture;
    }

    @NotNull
    @Override
    public <V> CompletableScheduledFuture<V> schedule(
            final @NotNull Callable<V> callable, final long delay, final @NotNull TimeUnit unit) {
        final CompletableScheduledFutureImpl<V> completableScheduledFuture = new CompletableScheduledFutureImpl<>();
        final ScheduledFuture<V> scheduledFuture = managedPluginExecutorService.schedule(
                new WrappedCallable<>(callable, classLoader, completableScheduledFuture), delay, unit);
        completableScheduledFuture.setScheduledFuture(scheduledFuture);
        return completableScheduledFuture;
    }

    @NotNull
    @Override
    public CompletableScheduledFuture<?> scheduleAtFixedRate(
            final @NotNull Runnable command, final long initialDelay, final long period, final @NotNull TimeUnit unit) {
        final CompletableScheduledFutureImpl<?> completableScheduledFuture = new CompletableScheduledFutureImpl<>();
        final ScheduledFuture<?> scheduledFuture = managedPluginExecutorService.scheduleAtFixedRate(
                new WrappedScheduledRunnable(command, classLoader, completableScheduledFuture, hiveMQExtensions),
                initialDelay,
                period, unit);
        completableScheduledFuture.setScheduledFuture(scheduledFuture);
        return completableScheduledFuture;
    }

    @NotNull
    @Override
    public CompletableScheduledFuture<?> scheduleWithFixedDelay(
            final @NotNull Runnable command, final long initialDelay, final long delay, final @NotNull TimeUnit unit) {

        final CompletableScheduledFutureImpl<?> completableScheduledFuture = new CompletableScheduledFutureImpl<>();
        final ScheduledFuture<?> scheduledFuture = managedPluginExecutorService.scheduleWithFixedDelay(
                new WrappedScheduledRunnable(command, classLoader, completableScheduledFuture, hiveMQExtensions),
                initialDelay,
                delay, unit);
        completableScheduledFuture.setScheduledFuture(scheduledFuture);
        return completableScheduledFuture;
    }

    @Override
    public boolean isShutdown() {
        return managedPluginExecutorService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return managedPluginExecutorService.isTerminated();
    }

    @Override
    public boolean awaitTermination(final long timeout, final @NotNull TimeUnit unit) throws InterruptedException {
        return managedPluginExecutorService.awaitTermination(timeout, unit);
    }

    @NotNull
    @Override
    public <T> CompletableFuture<T> submit(final @NotNull Callable<T> task) {
        final CompletableFuture<T> completableFuture = new CompletableFuture<>();
        managedPluginExecutorService.submit(new WrappedCallable<>(task, classLoader, completableFuture));
        return completableFuture;
    }

    @NotNull
    @Override
    public <T> CompletableFuture<T> submit(final @NotNull Runnable task, final @NotNull T result) {
        final CompletableFuture<T> completableFuture = new CompletableFuture<>();
        managedPluginExecutorService.submit(
                new WrappedRunnableWithResult<>(task, classLoader, completableFuture, result), result);
        return completableFuture;
    }

    @NotNull
    @Override
    public CompletableFuture<?> submit(final @NotNull Runnable task) {
        final CompletableFuture<?> completableFuture = new CompletableFuture<>();
        managedPluginExecutorService.submit(new WrappedRunnable(task, classLoader, completableFuture));
        return completableFuture;
    }

    @NotNull
    @Override
    public <T> List<Future<T>> invokeAll(final @NotNull Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        return managedPluginExecutorService.invokeAll(tasks.stream()
                .map(callable -> new WrappedCallable<>(callable, classLoader, null))
                .collect(Collectors.toList()));
    }

    @NotNull
    @Override
    public <T> List<Future<T>> invokeAll(
            final @NotNull Collection<? extends Callable<T>> tasks, final long timeout, final @NotNull TimeUnit unit)
            throws InterruptedException {
        return managedPluginExecutorService.invokeAll(tasks.stream()
                .map(callable -> new WrappedCallable<>(callable, classLoader, null))
                .collect(Collectors.toList()), timeout, unit);
    }

    @NotNull
    @Override
    public <T> T invokeAny(final @NotNull Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        return managedPluginExecutorService.invokeAny(tasks.stream()
                .map(callable -> new WrappedCallable<>(callable, classLoader, null))
                .collect(Collectors.toList()));
    }

    @NotNull
    @Override
    public <T> T invokeAny(
            final @NotNull Collection<? extends Callable<T>> tasks, final long timeout, final @NotNull TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return managedPluginExecutorService.invokeAny(tasks.stream()
                .map(callable -> new WrappedCallable<>(callable, classLoader, null))
                .collect(Collectors.toList()), timeout, unit);
    }
}
