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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public class AssetFactoryImpl implements AssetFactory {

    @Inject
    public AssetFactoryImpl() {}

    @Override
    public @NotNull Asset create(
            final @NotNull String id,
            final @NotNull String topic,
            final @NotNull String name,
            final @Nullable String description,
            final @NotNull String jsonSchema) {
        return new AssetImpl(id, topic, name, description, jsonSchema);
    }
}
