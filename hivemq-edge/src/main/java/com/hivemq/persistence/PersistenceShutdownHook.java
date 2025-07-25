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

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.hivemq.common.shutdown.HiveMQShutdownHook;
import org.jetbrains.annotations.NotNull;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.clientsession.ClientSessionPersistence;
import com.hivemq.persistence.clientsession.ClientSessionSubscriptionPersistence;
import com.hivemq.persistence.ioc.annotation.Persistence;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.persistence.qos.IncomingMessageFlowPersistence;
import com.hivemq.persistence.retained.RetainedMessagePersistence;
import com.hivemq.persistence.util.FutureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.hivemq.configuration.service.InternalConfigurations.PERSISTENCE_SHUTDOWN_TIMEOUT_SEC;

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

        //We have to use a direct executor service here because the usual persistence executor might already be shut down
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

        // All persistence producers are terminated at this point. Make sure all other producers for the single writer service are stopped as well.
        singleWriterService.stop();

        persistenceScheduledExecutorService.shutdownNow();
        persistenceExecutorService.shutdown();
    }
}
