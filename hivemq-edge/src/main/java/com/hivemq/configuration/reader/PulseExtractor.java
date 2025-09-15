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
import jakarta.xml.bind.ValidationEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PulseExtractor implements ReloadableExtractor<PulseEntity, PulseEntity> {
    private final static Consumer<PulseEntity> DEFAULT_CONSUMER =
            pulseEntity -> log.debug("No consumer registered for Pulse configuration changes.");
    private final @NotNull ConfigFileReaderWriter configFileReaderWriter;
    private @NotNull Consumer<PulseEntity> consumer;

    private @NotNull PulseEntity pulseEntity;

    public PulseExtractor(final @NotNull ConfigFileReaderWriter configFileReaderWriter) {
        consumer = DEFAULT_CONSUMER;
        this.configFileReaderWriter = configFileReaderWriter;
        this.pulseEntity = new PulseEntity();
    }

    public synchronized @NotNull PulseEntity getPulseEntity() {
        return pulseEntity;
    }

    public synchronized void setPulseEntity(final @NotNull PulseEntity pulseEntity) {
        final boolean needsUpdate = !this.pulseEntity.equals(pulseEntity);
        if (needsUpdate) {
            this.pulseEntity = pulseEntity;
            notifyConsumer();
            configFileReaderWriter.writeConfigWithSync();
        }
    }

    @Override
    public @NotNull Configurator.ConfigResult updateConfig(final @NotNull HiveMQConfigEntity config) {
        final PulseEntity newPulseEntity = config.getPulseEntity();
        final List<ValidationEvent> validationEvents = new ArrayList<>();
        newPulseEntity.validate(validationEvents);
        final List<ValidationEvent> errorEvents = validationEvents.stream()
                .filter(event -> event.getSeverity() == ValidationEvent.FATAL_ERROR ||
                        event.getSeverity() == ValidationEvent.ERROR)
                .toList();
        if (!errorEvents.isEmpty()) {
            errorEvents.forEach(event -> log.error("Pulse config error: {}", event.getMessage()));
            return Configurator.ConfigResult.ERROR;
        }
        setPulseEntity(newPulseEntity);
        return Configurator.ConfigResult.SUCCESS;
    }

    @Override
    public void sync(final @NotNull HiveMQConfigEntity entity) {
        entity.getPulseEntity().setPulseAssetsEntity(pulseEntity.getPulseAssetsEntity());
    }

    private void notifyConsumer() {
        consumer.accept(pulseEntity);
    }

    @Override
    public boolean needsRestartWithConfig(final @NotNull HiveMQConfigEntity config) {
        return false;
    }

    @Override
    public void registerConsumer(final @Nullable Consumer<PulseEntity> consumer) {
        this.consumer = consumer == null ? DEFAULT_CONSUMER : consumer;
        notifyConsumer();
    }
}
