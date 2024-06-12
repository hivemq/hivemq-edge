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

package com.hivemq.embedded.internal;

import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.hivemq.HiveMQEdgeMain;
import com.hivemq.bootstrap.ioc.Injector;
import com.hivemq.configuration.ConfigurationBootstrap;
import com.hivemq.configuration.info.SystemInformationImpl;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.edge.modules.ModuleLoader;
import com.hivemq.embedded.EmbeddedExtension;
import com.hivemq.embedded.EmbeddedHiveMQ;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.util.ThreadFactoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * @author Georg Held
 */
class EmbeddedHiveMQImpl implements EmbeddedHiveMQ {

    private static final @NotNull Logger log = LoggerFactory.getLogger(EmbeddedHiveMQImpl.class);

    private final @NotNull SystemInformationImpl systemInformation;
    private final @NotNull MetricRegistry metricRegistry;
    @VisibleForTesting
    final @NotNull ExecutorService stateChangeExecutor;
    private final @Nullable EmbeddedExtension embeddedExtension;
    private @Nullable ConfigurationService configurationService;
    private @Nullable HiveMQEdgeMain hiveMQServer;
    private final @NotNull Function<SystemInformationImpl, ModuleLoader> moduleLoaderFactory;

    private @NotNull State currentState = State.STOPPED;
    private @NotNull State desiredState = State.STOPPED;
    private @Nullable Exception failedException;

    private @NotNull LinkedList<CompletableFuture<Void>> startFutures = new LinkedList<>();
    private @NotNull LinkedList<CompletableFuture<Void>> stopFutures = new LinkedList<>();
    private @Nullable Future<?> shutDownFuture;

    EmbeddedHiveMQImpl(
            final @Nullable File conf,
            final @Nullable File data,
            final @Nullable File extensions,
            final @Nullable File license

    ) {
        this(conf, data, extensions, license, null);
    }

    EmbeddedHiveMQImpl(
            final @Nullable File conf,
            final @Nullable File data,
            final @Nullable File extensions,
            final @Nullable File license,
            final @Nullable EmbeddedExtension embeddedExtension) {
        this(conf, data, extensions, license, embeddedExtension, ModuleLoader::new);
    }

    EmbeddedHiveMQImpl(
            final @Nullable File conf,
            final @Nullable File data,
            final @Nullable File extensions,
            final @Nullable File license,
            final @Nullable EmbeddedExtension embeddedExtension,
            final @NotNull Function<SystemInformationImpl, ModuleLoader> moduleLoaderFactory) {
        this.embeddedExtension = embeddedExtension;
        this.moduleLoaderFactory = moduleLoaderFactory;
        log.info("Setting default authentication behavior to ALLOW ALL");
        InternalConfigurations.AUTH_DENY_UNAUTHENTICATED_CONNECTIONS.set(false);

        systemInformation = new SystemInformationImpl(true, true, conf, data, extensions, null, license);
        // we create the metric registry here to make it accessible before start
        metricRegistry = new MetricRegistry();

        // Once the EmbeddedHiveMQ gets garbage collected this gets automatically shut down
        stateChangeExecutor =
                Executors.newSingleThreadExecutor(ThreadFactoryUtil.create("embedded-hivemq-state-change-executor"));
    }

    @Override
    public void close() throws ExecutionException, InterruptedException {
        synchronized (this) {
            if (shutDownFuture == null) {
                log.info("Closing EmbeddedHiveMQ.");
                this.desiredState = State.CLOSED;
                stateChangeExecutor.submit(this::stateChange);
                shutDownFuture = stateChangeExecutor.submit(stateChangeExecutor::shutdown);
            }
        }
        shutDownFuture.get();
    }

    private enum State {
        RUNNING,
        STOPPED,
        FAILED,
        CLOSED
    }

    private void stateChange() {
        final List<CompletableFuture<Void>> localStartFutures;
        final List<CompletableFuture<Void>> localStopFutures;
        final State localDesiredState;

        synchronized (this) {
            localStartFutures = startFutures;
            localStopFutures = stopFutures;
            localDesiredState = desiredState;
            startFutures = new LinkedList<>();
            stopFutures = new LinkedList<>();
        }

        // Try to perform a stop regardless
        if (localDesiredState == State.CLOSED) {
            if ((currentState != State.STOPPED) && (currentState != State.CLOSED)) {
                performStop(localDesiredState, localStartFutures, localStopFutures);
            }
        } else if (currentState == State.FAILED) {
            if (failedException != null) {
                failFutureLists(failedException, localStartFutures, localStopFutures);
            } else {
                log.error("Encountered a FAILED EmbeddedHiveMQ state without a reason present.");
                failFutureLists(new IllegalStateException("FAILED EmbeddedHiveMQ state without a reason present"),
                        localStartFutures,
                        localStopFutures);
            }
        } else if (currentState == State.STOPPED) {
            if (localDesiredState == State.STOPPED) {
                failFutureList(new AbortedStateChangeException("EmbeddedHiveMQ was stopped"), localStartFutures);
                succeedFutureList(localStopFutures);
            } else if (localDesiredState == State.RUNNING) {
                final long startTime = System.currentTimeMillis();
                log.info("Starting Embedded HiveMQ Edge.");
                try {
                    systemInformation.init();
                    if (configurationService == null) {
                        bootstrapConfig();
                    }

                    hiveMQServer = new HiveMQEdgeMain(systemInformation,
                            metricRegistry,
                            configurationService,
                            moduleLoaderFactory.apply(systemInformation));
                    hiveMQServer.bootstrap();
                    hiveMQServer.start(embeddedExtension);

                    failFutureList(new AbortedStateChangeException("EmbeddedHiveMQ was started"), localStopFutures);
                    succeedFutureList(localStartFutures);
                    currentState = State.RUNNING;
                    log.info("Started Embedded HiveMQ Edge in {}ms", System.currentTimeMillis() - startTime);
                } catch (final Exception ex) {
                    currentState = State.FAILED;
                    failedException = ex;
                    failFutureLists(ex, localStartFutures, localStopFutures);
                }
            }
        } else if (currentState == State.RUNNING) {
            if (localDesiredState == State.RUNNING) {
                failFutureList(new AbortedStateChangeException("EmbeddedHiveMQ was started"), localStopFutures);
                succeedFutureList(localStartFutures);
            } else if (localDesiredState == State.STOPPED) {
                log.info("Stopping EmbeddedHiveMQ.");
                performStop(localDesiredState, localStartFutures, localStopFutures);
            }
        }
    }

    @NotNull ConfigurationService bootstrapConfig() {
        if (configurationService == null) {
            configurationService = ConfigurationBootstrap.bootstrapConfig(systemInformation);
        }
        return configurationService;
    }

    private void performStop(
            final @NotNull State desiredState,
            final @NotNull List<CompletableFuture<Void>> startFutures,
            final @NotNull List<CompletableFuture<Void>> stopFutures) {

        try {
            final long startTime = System.currentTimeMillis();

            try {
                hiveMQServer.stop();
            } catch (final Exception ex) {
                if (desiredState == State.CLOSED) {
                    log.error("Exception during running shutdown hook.", ex);
                } else {
                    throw ex;
                }
            }

            hiveMQServer = null;
            configurationService = null;
            failFutureList(new AbortedStateChangeException("EmbeddedHiveMQ was stopped"), startFutures);
            succeedFutureList(stopFutures);
            currentState = State.STOPPED;
            log.info("Stopped EmbeddedHiveMQ in {}ms", System.currentTimeMillis() - startTime);
        } catch (final Exception ex) {
            currentState = State.FAILED;
            failedException = ex;
            failFutureLists(ex, startFutures, stopFutures);
        }
    }

    private void failFutureLists(
            final @NotNull Exception exception,
            final @NotNull List<CompletableFuture<Void>> startFutures,
            final @NotNull List<CompletableFuture<Void>> stopFutures) {
        failFutureList(exception, startFutures);
        failFutureList(exception, stopFutures);
    }

    private void failFutureList(
            final @NotNull Exception exception, final @NotNull List<CompletableFuture<Void>> futures) {
        for (final CompletableFuture<Void> future : futures) {
            future.completeExceptionally(exception);
        }
    }

    private void succeedFutureList(final @NotNull List<CompletableFuture<Void>> futures) {
        for (final CompletableFuture<Void> future : futures) {
            future.complete(null);
        }
    }

    @Override
    public @NotNull CompletableFuture<Void> start() {
        synchronized (this) {
            if (this.desiredState == State.CLOSED) {
                return CompletableFuture.failedFuture(new IllegalStateException("EmbeddedHiveMQ was already closed"));
            }
            desiredState = State.RUNNING;
            final CompletableFuture<Void> future = new CompletableFuture<>();
            startFutures.add(future);
            stateChangeExecutor.execute(this::stateChange);
            return future;
        }
    }

    @Override
    public @NotNull CompletableFuture<Void> stop() {
        synchronized (this) {
            if (this.desiredState == State.CLOSED) {
                return CompletableFuture.failedFuture(new IllegalStateException("EmbeddedHiveMQ was already closed"));
            }
            desiredState = State.STOPPED;
            final CompletableFuture<Void> future = new CompletableFuture<>();
            stopFutures.add(future);
            stateChangeExecutor.execute(this::stateChange);
            return future;
        }
    }

    @Override
    public @NotNull MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }

    @VisibleForTesting
    @Nullable Injector getInjector() {
        return hiveMQServer != null ? hiveMQServer.getInjector() : null;
    }

    private static class AbortedStateChangeException extends Exception {

        public AbortedStateChangeException(final @NotNull String message) {
            super(message);
        }
    }
}
