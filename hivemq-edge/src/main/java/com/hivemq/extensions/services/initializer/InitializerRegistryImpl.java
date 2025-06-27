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
package com.hivemq.extensions.services.initializer;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.intializer.ClientInitializer;
import com.hivemq.extension.sdk.api.services.intializer.InitializerRegistry;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@Singleton
public class InitializerRegistryImpl implements InitializerRegistry {

    @NotNull
    private final Initializers initializers;

    @Inject
    public InitializerRegistryImpl(final @NotNull Initializers initializers) {
        this.initializers = initializers;
    }

    @Override
    public void setClientInitializer(final @NotNull ClientInitializer initializer) {
        Preconditions.checkNotNull(initializer, "Client initializer must never be null");
        initializers.addClientInitializer(initializer);
    }
}
