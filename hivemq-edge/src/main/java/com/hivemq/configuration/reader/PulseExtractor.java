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

package com.hivemq.configuration.reader;

import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.pulse.PulseEntity;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class PulseExtractor implements ReloadableExtractor<PulseEntity, PulseEntity> {
    private final @NotNull ConfigFileReaderWriter configFileReaderWriter;

    private @NotNull PulseEntity pulseEntity;

    public PulseExtractor(final @NotNull ConfigFileReaderWriter configFileReaderWriter) {
        this.configFileReaderWriter = configFileReaderWriter;
        this.pulseEntity = new PulseEntity();
    }

    public synchronized @NotNull PulseEntity getPulseEntity() {
        return pulseEntity;
    }

    public synchronized void setPulseEntity(final @NotNull PulseEntity pulseEntity) {
        this.pulseEntity = pulseEntity;
        // TODO notify consumers.
    }

    @Override
    public @NotNull Configurator.ConfigResult updateConfig(final @NotNull HiveMQConfigEntity config) {
        // TODO
        return Configurator.ConfigResult.SUCCESS;
    }

    @Override
    public void sync(final @NotNull HiveMQConfigEntity entity) {
        // TODO
    }

    @Override
    public boolean needsRestartWithConfig(final @NotNull HiveMQConfigEntity config) {
        return false;
    }

    @Override
    public void registerConsumer(final @NotNull Consumer<PulseEntity> consumer) {
        // TODO
    }
}
