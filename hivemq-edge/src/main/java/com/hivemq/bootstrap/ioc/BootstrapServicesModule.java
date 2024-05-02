package com.hivemq.bootstrap.ioc;

import com.hivemq.bootstrap.services.AfterHiveMQStartBootstrapService;
import com.hivemq.bootstrap.services.AfterHiveMQStartBootstrapServiceImpl;
import com.hivemq.bootstrap.services.CompleteBootstrapService;
import com.hivemq.bootstrap.services.CompleteBootstrapServiceImpl;
import com.hivemq.bootstrap.services.PersistenceBootstrapService;
import com.hivemq.bootstrap.services.PersistenceBootstrapServiceImpl;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import dagger.Binds;
import dagger.Module;

import javax.inject.Singleton;

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
