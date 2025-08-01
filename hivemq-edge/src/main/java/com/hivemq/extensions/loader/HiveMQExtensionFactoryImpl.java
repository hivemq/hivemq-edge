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

package com.hivemq.extensions.loader;

import com.hivemq.extension.sdk.api.ExtensionMain;
import org.jetbrains.annotations.NotNull;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensionEntity;
import com.hivemq.extensions.HiveMQExtensionImpl;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.nio.file.Path;

@Singleton
public class HiveMQExtensionFactoryImpl implements HiveMQExtensionFactory {

    @Inject
    public HiveMQExtensionFactoryImpl() {
    }

    @Override
    public @NotNull HiveMQExtension createHiveMQExtension(final @NotNull ExtensionMain extensionMainInstance,
                                                          final @NotNull Path extensionFolder,
                                                          final @NotNull HiveMQExtensionEntity extensionConfig,
                                                          final boolean enabled) {
        return new HiveMQExtensionImpl(extensionConfig, extensionFolder, extensionMainInstance, enabled);
    }
}
