package com.hivemq.pulse.asset;


import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Singleton
public class AssetProviderRegistry {

    private final @NotNull Set<ExternalAssetProvider> assetProviders = new CopyOnWriteArraySet<>();

    @Inject
    public AssetProviderRegistry() {
    }

    public void registerAssetProvider(final @NotNull ExternalAssetProvider assetProvider) {
        assetProviders.add(assetProvider);
    }

    public @NotNull Set<ExternalAssetProvider> getAssetProviders() {
        return Set.copyOf(assetProviders);
    }
}
