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
package com.hivemq.pulse.asset;
import com.hivemq.edge.integration.api.asset.AssetProviderRegistry;
import com.hivemq.edge.integration.api.asset.ExternalAssetProvider;

import com.hivemq.configuration.reader.AssetMappingExtractor;
import com.hivemq.configuration.reader.PulseExtractor;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.jetbrains.annotations.NotNull;

@Singleton
public class AssetProviderRegistryImpl implements AssetProviderRegistry {

    private final @NotNull Set<ExternalAssetProvider> assetProviders = new CopyOnWriteArraySet<>();
    private final @NotNull PulseAgentAssetChangedListener edgeListener;

    @Inject
    public AssetProviderRegistryImpl(
            final @NotNull AssetMappingExtractor assetMappingExtractor, final @NotNull PulseExtractor pulseExtractor) {
        this.edgeListener = new PulseAgentAssetChangedListener(assetMappingExtractor, pulseExtractor);
    }

    @Override
    public void registerAssetProvider(final @NotNull ExternalAssetProvider assetProvider) {
        assetProvider.addAssetChangedListener(edgeListener);
        assetProviders.add(assetProvider);
    }

    @Override
    public @NotNull Set<ExternalAssetProvider> getAssetProviders() {
        return Set.copyOf(assetProviders);
    }
}
