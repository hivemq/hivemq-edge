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
package com.hivemq.bootstrap.ioc;

import com.hivemq.bootstrap.services.AfterHiveMQStartBootstrapService;
import com.hivemq.bootstrap.services.AfterHiveMQStartBootstrapServiceImpl;
import com.hivemq.bootstrap.services.CompleteBootstrapService;
import com.hivemq.bootstrap.services.CompleteBootstrapServiceImpl;
import com.hivemq.bootstrap.services.PersistenceBootstrapService;
import com.hivemq.bootstrap.services.PersistenceBootstrapServiceImpl;
import org.jetbrains.annotations.NotNull;
import dagger.Binds;
import dagger.Module;

import jakarta.inject.Singleton;

@Module
public abstract class BootstrapServicesModule {

    @Singleton
    @Binds
    abstract @NotNull PersistenceBootstrapService persistenceBootstrapService(final @NotNull PersistenceBootstrapServiceImpl persistenceBootstrapService);

    @Singleton
    @Binds
    abstract @NotNull CompleteBootstrapService completeBootstrapService(final @NotNull CompleteBootstrapServiceImpl completeBootstrapService);

    @Singleton
    @Binds
    abstract @NotNull AfterHiveMQStartBootstrapService afterHiveMQStartBootstrapService(final @NotNull AfterHiveMQStartBootstrapServiceImpl afterHiveMQStartBootstrapService);
}
