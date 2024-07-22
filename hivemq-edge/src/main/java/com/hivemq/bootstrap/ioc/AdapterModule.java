package com.hivemq.bootstrap.ioc;

import com.hivemq.bootstrap.factories.WritingServiceProvider;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.protocols.writing.ProtocolAdapterWritingService;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module
public abstract class AdapterModule {

    @Provides
    @Singleton
    static @NotNull ProtocolAdapterWritingService adapterWritingService(final WritingServiceProvider writingServiceProvider) {
        return writingServiceProvider.get();
    }

}
