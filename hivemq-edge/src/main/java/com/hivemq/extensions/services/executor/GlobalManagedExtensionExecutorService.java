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

import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.InternalConfigurations;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.hivemq.util.ThreadFactoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.hivemq.configuration.service.InternalConfigurations.MANAGED_EXTENSION_EXECUTOR_SHUTDOWN_TIMEOUT_SEC;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@Singleton
public class GlobalManagedExtensionExecutorService implements ScheduledExecutorService {

    private static final Logger log = LoggerFactory.getLogger(GlobalManagedExtensionExecutorService.class);

    private final @NotNull ShutdownHooks shutdownHooks;

    private @Nullable ScheduledExecutorService scheduledExecutorService;
    private @Nullable ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    @Inject
    public GlobalManagedExtensionExecutorService(final @NotNull ShutdownHooks shutdownHooks) {
        this.shutdownHooks = shutdownHooks;
    }

    @Inject //method injection, this gets called once after instantiation
    public void postConstruct() {

        final ThreadFactory threadFactory = ThreadFactoryUtil.create("managed-extension-executor-%d");

        final int corePoolSize = InternalConfigurations.MANAGED_EXTENSION_THREAD_POOL_THREADS_COUNT.get();
        final int keepAlive = InternalConfigurations.MANAGED_EXTENSION_THREAD_POOL_KEEP_ALIVE_SEC.get();

        scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(corePoolSize, threadFactory);
        log.debug("Set extension executor thread pool size to {}", corePoolSize);

        scheduledThreadPoolExecutor.setKeepAliveTime(keepAlive, TimeUnit.SECONDS);
        log.debug("Set extension executor thread pool keep-alive to {} seconds", keepAlive);

        scheduledThreadPoolExecutor.allowCoreThreadTimeOut(true);

        //for instrumentation (metrics)
        scheduledExecutorService = scheduledThreadPoolExecutor;

        shutdownHooks.add(new ManagedPluginExecutorShutdownHook(this, MANAGED_EXTENSION_EXECUTOR_SHUTDOWN_TIMEOUT_SEC.get()));
    }

    public int getCorePoolSize() {
        return Objects.requireNonNull(scheduledThreadPoolExecutor).getCorePoolSize();
    }

    public int getMaxPoolSize() {
        return Objects.requireNonNull(scheduledThreadPoolExecutor).getMaximumPoolSize();
    }

    public int getCurrentPoolSize() {
        return Objects.requireNonNull(scheduledThreadPoolExecutor).getPoolSize();
    }

    public long getKeepAliveSeconds() {
        return Objects.requireNonNull(scheduledThreadPoolExecutor).getKeepAliveTime(TimeUnit.SECONDS);
    }

    @NotNull
    public ScheduledFuture<?> schedule(
            final @NotNull Runnable command, final long delay, final @NotNull TimeUnit unit) {
        return Objects.requireNonNull(scheduledExecutorService).schedule(command, delay, unit);
    }

    @NotNull
    public <V> ScheduledFuture<V> schedule(
            final @NotNull Callable<V> callable, final long delay, final @NotNull TimeUnit unit) {
        return Objects.requireNonNull(scheduledExecutorService).schedule(callable, delay, unit);
    }

    @NotNull
    public ScheduledFuture<?> scheduleAtFixedRate(
            final @NotNull Runnable command, final long initialDelay, final long period, final @NotNull TimeUnit unit) {
        return Objects.requireNonNull(scheduledExecutorService)
                .scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @NotNull
    public ScheduledFuture<?> scheduleWithFixedDelay(
            final @NotNull Runnable command, final long initialDelay, final long delay, final @NotNull TimeUnit unit) {
        return Objects.requireNonNull(scheduledExecutorService)
                .scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    @Override
    public void shutdown() {
        Objects.requireNonNull(scheduledExecutorService).shutdown();
    }

    @Override
    public @NotNull List<Runnable> shutdownNow() {
        return Objects.requireNonNull(scheduledExecutorService).shutdownNow();
    }

    public boolean isShutdown() {
        return Objects.requireNonNull(scheduledExecutorService).isShutdown();
    }

    public boolean isTerminated() {
        return Objects.requireNonNull(scheduledExecutorService).isTerminated();
    }

    public boolean awaitTermination(final long timeout, final @NotNull TimeUnit unit) throws InterruptedException {
        return Objects.requireNonNull(scheduledExecutorService).awaitTermination(timeout, unit);
    }

    @NotNull
    public <T> Future<T> submit(final @NotNull Callable<T> task) {
        return Objects.requireNonNull(scheduledExecutorService).submit(task);
    }

    @NotNull
    public <T> Future<T> submit(final @NotNull Runnable task, final @NotNull T result) {
        return Objects.requireNonNull(scheduledExecutorService).submit(task, result);
    }

    @NotNull
    public Future<?> submit(final @NotNull Runnable task) {
        return Objects.requireNonNull(scheduledExecutorService).submit(task);
    }

    @NotNull
    public <T> List<Future<T>> invokeAll(final @NotNull Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        return Objects.requireNonNull(scheduledExecutorService).invokeAll(tasks);
    }

    @NotNull
    public <T> List<Future<T>> invokeAll(
            final @NotNull Collection<? extends Callable<T>> tasks, final long timeout, final @NotNull TimeUnit unit)
            throws InterruptedException {
        return Objects.requireNonNull(scheduledExecutorService).invokeAll(tasks, timeout, unit);
    }

    @NotNull
    public <T> T invokeAny(final @NotNull Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        return Objects.requireNonNull(scheduledExecutorService).invokeAny(tasks);
    }

    @NotNull
    public <T> T invokeAny(
            final @NotNull Collection<? extends Callable<T>> tasks, final long timeout, final @NotNull TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return Objects.requireNonNull(scheduledExecutorService).invokeAny(tasks, timeout, unit);
    }

    public void execute(final @NotNull Runnable command) {
        Objects.requireNonNull(scheduledExecutorService).execute(command);
    }
}
