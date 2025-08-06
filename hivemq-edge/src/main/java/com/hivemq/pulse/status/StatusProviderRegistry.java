package com.hivemq.pulse.status;


import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Singleton
public class StatusProviderRegistry {

    private final @NotNull Set<StatusProvider> statusProviders = new CopyOnWriteArraySet<>();

    @Inject
    public StatusProviderRegistry() {
    }

    public void registerStatusProvider(final @NotNull StatusProvider statusProvider) {
        statusProviders.add(statusProvider);
    }

    public @NotNull Set<StatusProvider> getStatusProviders() {
        return Set.copyOf(statusProviders);
    }
}
