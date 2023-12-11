package com.hivemq.persistence.ioc.provider;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.core.PersistencesService;
import com.hivemq.persistence.InMemorySingleWriter;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.InFileSingleWriter;

import javax.inject.Inject;
import javax.inject.Provider;

public class SingleWriterProvider {

    private final @NotNull PersistencesService persistencesService;
    private final @NotNull Provider<InMemorySingleWriter> inMemorySingleWriterProvider;
    private final @NotNull Provider<InFileSingleWriter> singleWriterServiceProvider;

    @Inject
    public SingleWriterProvider(
            final @NotNull PersistencesService persistencesService,
            final @NotNull Provider<InMemorySingleWriter> inMemorySingleWriterProvider,
            final @NotNull Provider<InFileSingleWriter> singleWriterServiceProvider) {
        this.persistencesService = persistencesService;
        this.inMemorySingleWriterProvider = inMemorySingleWriterProvider;
        this.singleWriterServiceProvider = singleWriterServiceProvider;
    }

    public @NotNull SingleWriterService get() {
        if (persistencesService.isFilePersistencesPresent()) {
            return singleWriterServiceProvider.get();
        } else {
           return inMemorySingleWriterProvider.get();
        }
    }
}
