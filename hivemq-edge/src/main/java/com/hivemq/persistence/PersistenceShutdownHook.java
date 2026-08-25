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
package com.hivemq.persistence;

import static com.hivemq.configuration.service.InternalConfigurations.PERSISTENCE_SHUTDOWN_TIMEOUT_SEC;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.clientsession.ClientSessionPersistence;
import com.hivemq.persistence.clientsession.ClientSessionSubscriptionPersistence;
import com.hivemq.persistence.ioc.annotation.Persistence;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.persistence.qos.IncomingMessageFlowPersistence;
import com.hivemq.persistence.retained.RetainedMessagePersistence;
import com.hivemq.persistence.util.FutureUtils;
import jakarta.inject.Inject;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Lukas Brandl
 */
public class PersistenceShutdownHook implements HiveMQShutdownHook {

    private static final Logger log = LoggerFactory.getLogger(PersistenceShutdownHook.class);

    private final @NotNull ClientSessionPersistence clientSessionPersistence;
    private final @NotNull ClientSessionSubscriptionPersistence clientSessionSubscriptionPersistence;
    private final @NotNull IncomingMessageFlowPersistence incomingMessageFlowPersistence;
    private final @NotNull RetainedMessagePersistence retainedMessagePersistence;
    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @NotNull ListeningExecutorService persistenceExecutorService;
    private final @NotNull ListeningScheduledExecutorService persistenceScheduledExecutorService;
    private final @NotNull SingleWriterService singleWriterService;
    private final @NotNull PublishPayloadPersistence payloadPersistence;

    @Inject
    PersistenceShutdownHook(
            final @NotNull ClientSessionPersistence clientSessionPersistence,
            final @NotNull ClientSessionSubscriptionPersistence clientSessionSubscriptionPersistence,
            final @NotNull IncomingMessageFlowPersistence incomingMessageFlowPersistence,
            final @NotNull RetainedMessagePersistence retainedMessagePersistence,
            final @NotNull PublishPayloadPersistence payloadPersistence,
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull @Persistence ListeningExecutorService persistenceExecutorService,
            final @NotNull @Persistence ListeningScheduledExecutorService persistenceScheduledExecutorService,
            final @NotNull SingleWriterService singleWriterService) {

        this.clientSessionPersistence = clientSessionPersistence;
        this.clientSessionSubscriptionPersistence = clientSessionSubscriptionPersistence;
        this.incomingMessageFlowPersistence = incomingMessageFlowPersistence;
        this.retainedMessagePersistence = retainedMessagePersistence;
        this.clientQueuePersistence = clientQueuePersistence;
        this.persistenceExecutorService = persistenceExecutorService;
        this.persistenceScheduledExecutorService = persistenceScheduledExecutorService;
        this.singleWriterService = singleWriterService;
        this.payloadPersistence = payloadPersistence;
    }

    @Override
    public @NotNull String name() {
        return "Persistence Shutdown";
    }

    @Override
    public void run() {
        final long start = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.debug("Shutting down persistent stores");
        }
        final ImmutableList.Builder<ListenableFuture<Void>> builder = ImmutableList.builder();

        incomingMessageFlowPersistence.closeDB();
        builder.add(clientSessionPersistence.closeDB());
        builder.add(clientSessionSubscriptionPersistence.closeDB());
        builder.add(retainedMessagePersistence.closeDB());
        builder.add(clientQueuePersistence.closeDB());

        // We have to use a direct executor service here because the usual persistence executor might already be shut
        // down
        final ListenableFuture<Void> combinedFuture = FutureUtils.voidFutureFromList(builder.build());

        final int shutdownTimeout = PERSISTENCE_SHUTDOWN_TIMEOUT_SEC.get();

        try {
            combinedFuture.get(shutdownTimeout, TimeUnit.SECONDS);
            if (log.isDebugEnabled()) {
                log.debug("Finished persistence shutdown in {} ms", (System.currentTimeMillis() - start));
            }
        } catch (final TimeoutException te) {
            log.warn("Persistences were not closed properly");
        } catch (final Exception e) {
            log.error("Persistences were not closed properly: {}", e.getMessage());
            log.debug("Original Exception: ", e);
        }
        payloadPersistence.closeDB();

        // All persistence producers are terminated at this point. Make sure all other producers for the single writer
        // service are stopped as well.
        singleWriterService.stop();

        persistenceScheduledExecutorService.shutdownNow();
        persistenceExecutorService.shutdown();

        finishXodusJobProcessors();
    }

    /**
     * Terminate the job processors Xodus keeps in its process-wide {@link ThreadJobProcessorPool}.
     * <p>
     * The pool is static, so it outlives the Edge instance that caused a processor to be created. Each
     * {@link ThreadJobProcessor} is a live thread -- a GC root -- and additionally keeps an explicit
     * {@code classLoader} field referencing the module classloader of the Edge that created it. Nothing in
     * Xodus releases them, so every retired Edge stays reachable through its classloader and every class that
     * classloader ever loaded stays resident.
     * <p>
     * Invisible in production (one Edge, process exits anyway), but decisive when a JVM starts and stops an
     * embedded Edge repeatedly: a heap dump of one test JVM showed 44 pooled processors pinning 72 module
     * classloaders. Interrupting the threads is not sufficient -- that is what the test harness already tries
     * -- because it neither clears the {@code classLoader} field nor removes the processor from the pool.
     */
    private void finishXodusJobProcessors() {
        // Reflection on purpose: Xodus is not on this module's compile classpath -- it arrives with the
        // commercial persistence module, which Edge loads in its own classloader -- and this is internal Xodus
        // API. Hence also the classloader dance below: a plain Class.forName() here resolves against *this*
        // class's loader, which has never seen Xodus, so it throws ClassNotFoundException and the whole
        // cleanup silently does nothing. Ask the persistence implementations instead: at runtime those are the
        // Xodus-backed classes from the commercial module, so their loader is the one that holds the pool.
        final ClassLoader loader = xodusClassLoader();
        if (loader == null) {
            log.debug("No Xodus-capable classloader found; skipping job processor cleanup");
            return;
        }
        int finished = 0;
        try {
            final Class<?> poolClass =
                    Class.forName("jetbrains.exodus.core.execution.ThreadJobProcessorPool", false, loader);
            final Object processors = poolClass.getMethod("getProcessors").invoke(null);
            if (!(processors instanceof Iterable<?>)) {
                return;
            }
            for (final Object processor : (Iterable<?>) processors) {
                try {
                    // finish() only *requests* termination; waitUntilFinished() joins the worker. Without the
                    // second call the thread is still alive when the next embedded Edge starts, so the pool
                    // keeps growing and every retired instance leaves its pollers behind.
                    processor.getClass().getMethod("finish").invoke(processor);
                    processor.getClass().getMethod("waitUntilFinished").invoke(processor);
                    finished++;
                } catch (final Exception e) {
                    log.debug("Could not finish Xodus job processor {}", processor, e);
                }
            }
            finished += finishSpawner(poolClass);
            log.debug("Finished {} Xodus job processors", finished);
        } catch (final Throwable t) {
            log.debug("Could not finish Xodus job processors", t);
        }
    }

    /**
     * Stop the pool's {@code SPAWNER}, returning 1 if it was stopped and 0 otherwise.
     * <p>
     * The spawner is created in the pool class's static initializer and held in a private static field that
     * {@code getProcessors()} does not report, so finishing the reported processors always leaves it running.
     * Because each module classloader loads its own copy of the pool class, that is one surviving thread per
     * embedded Edge -- measured as a count that climbed 1, 2, 3 ... 70 over a single test run.
     * <p>
     * Reflection on a private field of a third-party class is deliberate and deliberately fail-soft: if a Xodus
     * upgrade renames or removes the field this degrades to a debug line and the pre-existing (smaller) leak,
     * never to a shutdown failure.
     */
    private int finishSpawner(final @NotNull Class<?> poolClass) {
        try {
            final java.lang.reflect.Field field = poolClass.getDeclaredField("SPAWNER");
            field.setAccessible(true);
            final Object spawner = field.get(null);
            if (spawner == null) {
                return 0;
            }
            spawner.getClass().getMethod("finish").invoke(spawner);
            spawner.getClass().getMethod("waitUntilFinished").invoke(spawner);
            return 1;
        } catch (final Throwable t) {
            log.debug("Could not finish the Xodus job processor pool spawner", t);
            return 0;
        }
    }

    /**
     * A classloader that can resolve Xodus, or {@code null}. Tries the loaders of the injected persistences --
     * whichever of them is Xodus-backed brings the library with it -- then this class's own loader as a
     * fallback for deployments where everything shares one classpath.
     */
    private @Nullable ClassLoader xodusClassLoader() {
        final Object[] candidates = {
            clientQueuePersistence,
            clientSessionPersistence,
            retainedMessagePersistence,
            payloadPersistence,
            clientSessionSubscriptionPersistence,
            this
        };
        for (final Object candidate : candidates) {
            final ClassLoader loader = candidate.getClass().getClassLoader();
            if (loader == null) {
                continue;
            }
            try {
                Class.forName("jetbrains.exodus.core.execution.ThreadJobProcessorPool", false, loader);
                return loader;
            } catch (final Throwable ignored) {
                // try the next candidate
            }
        }
        return null;
    }
}
