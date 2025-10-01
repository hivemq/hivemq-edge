/*
 *  Copyright 2019-present HiveMQ GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
