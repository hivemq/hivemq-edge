package com.hivemq.pulse.asset;


import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public interface ExternalAssetProvider {

    /**
     * Returns the list of assets provided by this provider.
     *
     * @return an Optional containing the list of assets, or an empty Optional if the provider is not active.
     */
    @NotNull Optional<List<Asset>> getAssets();

    @NotNull Optional<Asset> getAsset(@NotNull String assetId);

    /**
     * Adds a listener that will be notified when the assets change.
     * Will be called once with current state when registered.
     *
     * @param listener the listener
     */
    void addAssetChangedListener(@NotNull AssetChangedListener listener);

    void removeAssetChangedListener(@NotNull AssetChangedListener listener);

    interface AssetChangedListener {
        void onAssetsChanged(@NotNull List<Asset> assets);
    }

}
