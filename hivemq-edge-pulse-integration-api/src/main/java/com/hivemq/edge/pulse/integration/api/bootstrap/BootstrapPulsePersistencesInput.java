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
package com.hivemq.edge.pulse.integration.api.bootstrap;

import java.io.File;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Context handed to {@link PulseAgentBootstrap#bootstrapPulsePersistences(BootstrapPulsePersistencesInput, BootstrapPulsePersistencesOutput)}.
 * Exposes the services the Pulse Agent integration needs from HiveMQ Edge during the persistence bootstrap phase.
 */
public interface BootstrapPulsePersistencesInput {

    enum Mode {
        IN_MEMORY,
        FILE_NATIVE,
        FILE
    }

    @Nullable
    File dataFolder();

    @Nullable
    File pulseTokenFolder();

    @NotNull
    Mode persistenceMode();

    /**
     * Returns the values of the given internal-configuration keys that have been explicitly overridden, keyed by the
     * input key. Keys without an override are absent from the returned map.
     */
    @NotNull
    Map<String, String> internalConfigOverrides(@NotNull Set<String> keys);

    @NotNull
    Map<String, Object> commercialModuleConfigs();
}
