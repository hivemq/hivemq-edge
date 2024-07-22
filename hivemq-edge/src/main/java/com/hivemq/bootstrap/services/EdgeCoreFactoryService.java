package com.hivemq.bootstrap.services;

import com.hivemq.bootstrap.factories.WritingServiceFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EdgeCoreFactoryService {

    private @Nullable WritingServiceFactory writingServiceFactory;

    public void provideWritingServiceFactory(final @NotNull WritingServiceFactory writingServiceFactory) {
        this.writingServiceFactory = writingServiceFactory;
    }

    public @Nullable WritingServiceFactory getWritingServiceFactory() {
        return writingServiceFactory;
    }
}
