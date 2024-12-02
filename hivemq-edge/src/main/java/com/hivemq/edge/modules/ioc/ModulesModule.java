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
package com.hivemq.edge.modules.ioc;

import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterPublishService;
import com.hivemq.edge.impl.events.EventServiceDelegateImpl;
import com.hivemq.edge.impl.events.InMemoryEventImpl;
import com.hivemq.edge.modules.adapters.impl.ModuleServicesImpl;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterPublishServiceImpl;
import com.hivemq.edge.modules.adapters.impl.polling.ProtocolAdapterPollingServiceImpl;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingService;
import com.hivemq.edge.modules.api.events.EventListener;
import com.hivemq.edge.modules.api.events.EventStore;
import org.jetbrains.annotations.NotNull;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ElementsIntoSet;

import javax.inject.Singleton;
import java.util.Set;

@SuppressWarnings("unused")
@Module
public abstract class ModulesModule {

    @Binds
    @Singleton
    abstract @NotNull ModuleServices moduleServices(@NotNull ModuleServicesImpl moduleServices);

    @Binds
    @Singleton
    abstract @NotNull ProtocolAdapterPublishService adapterPublishService(@NotNull ProtocolAdapterPublishServiceImpl adapterPublishService);

    @Binds
    @Singleton
    abstract @NotNull ProtocolAdapterPollingService protocolAdapterPollingService(@NotNull ProtocolAdapterPollingServiceImpl protocolAdapterPollingService);

    @Binds
    @Singleton
    abstract @NotNull EventService eventService(@NotNull EventServiceDelegateImpl eventServiceDelegate);

    @Binds
    @Singleton
    abstract @NotNull EventStore eventStore(@NotNull InMemoryEventImpl inMemoryEvent);

    @Provides
    @ElementsIntoSet
    @Singleton
    static @NotNull Set<EventListener> provideEventListeners() {
        return Set.of();
    }
}
