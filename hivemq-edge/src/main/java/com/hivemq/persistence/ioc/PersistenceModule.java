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
package com.hivemq.persistence.ioc;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.hivemq.bootstrap.provider.ClientQueueLocalPersistenceProvider;
import com.hivemq.bootstrap.provider.ClientSessionLocalPersistenceProvider;
import com.hivemq.bootstrap.provider.ClientSessionSubscriptionLocalPersistenceProvider;
import com.hivemq.bootstrap.provider.PublishPayloadPersistenceProvider;
import com.hivemq.bootstrap.provider.RetainedMessageLocalPersistenceProvider;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.dropping.MessageDroppedServiceImpl;
import com.hivemq.mqtt.topic.tree.TopicTreeStartup;
import com.hivemq.persistence.InMemorySingleWriter;
import com.hivemq.persistence.PersistenceShutdownHookInstaller;
import com.hivemq.persistence.ScheduledCleanUpService;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientqueue.ClientQueueLocalPersistence;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.clientqueue.ClientQueuePersistenceImpl;
import com.hivemq.persistence.clientsession.*;
import com.hivemq.persistence.connection.ConnectionPersistence;
import com.hivemq.persistence.connection.ConnectionPersistenceImpl;
import com.hivemq.persistence.ioc.annotation.Persistence;
import com.hivemq.persistence.ioc.provider.SingleWriterProvider;
import com.hivemq.persistence.ioc.provider.local.PersistenceExecutorProvider;
import com.hivemq.persistence.ioc.provider.local.PersistenceScheduledExecutorProvider;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.persistence.local.ClientSessionSubscriptionLocalPersistence;
import com.hivemq.persistence.local.IncomingMessageFlowInMemoryLocalPersistence;
import com.hivemq.persistence.local.IncomingMessageFlowLocalPersistence;
import com.hivemq.persistence.local.memory.ClientSessionSubscriptionMemoryLocalPersistence;
import com.hivemq.persistence.local.memory.RetainedMessageMemoryLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.persistence.qos.IncomingMessageFlowPersistence;
import com.hivemq.persistence.qos.IncomingMessageFlowPersistenceImpl;
import com.hivemq.persistence.retained.RetainedMessageLocalPersistence;
import com.hivemq.persistence.retained.RetainedMessagePersistence;
import com.hivemq.persistence.retained.RetainedMessagePersistenceImpl;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;

import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Dominik Obermaier
 */
@Module
public abstract class PersistenceModule {

    @Binds
    abstract @NotNull RetainedMessagePersistence retainedMessagePersistence(
            @NotNull RetainedMessagePersistenceImpl retainedMessagePersistence);

    @Binds
    abstract @NotNull ConnectionPersistence connectionPersistence(@NotNull ConnectionPersistenceImpl connectionPersistence);

    @Binds
    abstract @NotNull ClientSessionPersistence clientSessionPersistence(@NotNull ClientSessionPersistenceImpl clientSessionPersistence);

    @Binds
    abstract @NotNull SharedSubscriptionService sharedSubscriptionService(@NotNull SharedSubscriptionServiceImpl sharedSubscriptionService);

    @Binds
    abstract @NotNull ClientSessionSubscriptionPersistence clientSessionSubscriptionPersistence(@NotNull ClientSessionSubscriptionPersistenceImpl clientSessionSubscriptionPersistence);

    @Binds
    abstract @NotNull IncomingMessageFlowPersistence incomingMessageFlowPersistence(@NotNull IncomingMessageFlowPersistenceImpl incomingMessageFlowPersistence);

    @Binds
    abstract @NotNull IncomingMessageFlowLocalPersistence incomingMessageFlowLocalPersistence(@NotNull IncomingMessageFlowInMemoryLocalPersistence incomingMessageFlowPersistence);

    @Binds
    abstract @NotNull ClientQueuePersistence clientQueuePersistence(@NotNull ClientQueuePersistenceImpl clientQueuePersistence);

    @Provides
    @Singleton
    static @NotNull MessageDroppedService messageDroppedService(
            @NotNull MetricsHolder metricsHolder, @NotNull EventLog eventLog) {
        return new MessageDroppedServiceImpl(metricsHolder, eventLog);
    }

    @Provides
    @Singleton
    static @NotNull ClientQueueLocalPersistence clientQueueLocalPersistence(@NotNull ClientQueueLocalPersistenceProvider clientQueueLocalPersistenceProvider) {
        return clientQueueLocalPersistenceProvider.get();
    }

    @Provides
    @Singleton
    static @NotNull RetainedMessageLocalPersistence retainedMessageLocalPersistence(@NotNull RetainedMessageLocalPersistenceProvider retainedMessageLocalPersistenceProvider) {
        return retainedMessageLocalPersistenceProvider.get();
    }

    @Provides
    @Singleton
    static @NotNull ClientSessionLocalPersistence clientSessionLocalPersistence(@NotNull ClientSessionLocalPersistenceProvider clientSessionLocalPersistenceProvider) {
        return clientSessionLocalPersistenceProvider.get();
    }

    @Provides
    @Singleton
    static @NotNull ClientSessionSubscriptionLocalPersistence clientSessionSubscriptionLocalPersistence(@NotNull ClientSessionSubscriptionLocalPersistenceProvider clientSessionSubscriptionLocalPersistenceProvider) {
        return clientSessionSubscriptionLocalPersistenceProvider.get();
    }

    @Provides
    @Singleton
    static @NotNull PublishPayloadPersistence publishPayloadPersistence(@NotNull PublishPayloadPersistenceProvider publishPayloadPersistenceProvider) {
        return publishPayloadPersistenceProvider.get();
    }

    @Provides
    @Singleton
    static @NotNull SingleWriterService singleWriterService(@NotNull SingleWriterProvider singleWriterProvider) {
        return singleWriterProvider.get();
    }

    @Provides
    @Singleton
    @Persistence
    static @NotNull ExecutorService persistenceExecutorService(PersistenceExecutorProvider persistenceExecutorProvider) {
        return persistenceExecutorProvider.get();
    }

    @Provides
    @Singleton
    @Persistence
    static @NotNull ListeningExecutorService persistenceListeningExecutorService(PersistenceExecutorProvider persistenceExecutorProvider) {
        return persistenceExecutorProvider.get();
    }

    @Provides
    @Singleton
    @Persistence
    static @NotNull ScheduledExecutorService persistenceScheduledExecutorService(PersistenceScheduledExecutorProvider persistenceScheduledExecutorProvider) {
        return persistenceScheduledExecutorProvider.get();
    }

    @Provides
    @Singleton
    @Persistence
    static @NotNull ListeningScheduledExecutorService persistenceListeningScheduledExecutorService(
            PersistenceScheduledExecutorProvider persistenceScheduledExecutorProvider) {
        return persistenceScheduledExecutorProvider.get();
    }

    @Provides
    @IntoSet
    static @NotNull Boolean eagerSingletons(
            final @NotNull ShutdownHooks shutdownHooks,
            final @NotNull PersistenceShutdownHookInstaller shutdownHookInstaller,
            final @NotNull ScheduledCleanUpService scheduledCleanUpService,
            final @NotNull RetainedMessageLocalPersistence retainedMessageLocalPersistence,
            final @NotNull ClientSessionSubscriptionLocalPersistence clientSessionSubscriptionLocalPersistence,
            final @NotNull ClientSessionLocalPersistence clientSessionLocalPersistence,
            final @NotNull ClientQueueLocalPersistence clientQueueLocalPersistence,
            final @NotNull PublishPayloadPersistence publishPayloadPersistence,
            final @NotNull TopicTreeStartup topicTreeStartup) {
        // this is used to instantiate all the params, similar to guice's asEagerSingleton
        return Boolean.TRUE;
    }

}
