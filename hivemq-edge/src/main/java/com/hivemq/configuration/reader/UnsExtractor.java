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
package com.hivemq.configuration.reader;

import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.uns.UnsConfigEntity;
import com.hivemq.uns.config.ISA95;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class UnsExtractor implements ReloadableExtractor<UnsConfigEntity, ISA95> {

    private volatile @Nullable ISA95 config = null;;
    private volatile @Nullable Consumer<ISA95> consumer = cfg -> log.debug("No consumer registered yet");
    private final @NotNull ConfigFileReaderWriter configFileReaderWriter;

    public UnsExtractor(@NotNull final ConfigFileReaderWriter configFileReaderWriter) {
        this.configFileReaderWriter = configFileReaderWriter;
    }

    @Override
    public boolean needsRestartWithConfig(final HiveMQConfigEntity config) {
        return false;
    }

    @Override
    public Configurator.ConfigResult updateConfig(final HiveMQConfigEntity hmqConfig) {
        var config = hmqConfig.getUns().getIsa95();

        final ISA95.Builder builderIsa95 = new ISA95.Builder();
        builderIsa95.withArea(config.getArea()).
                withEnterprise(config.getEnterprise()).
                withProductionLine(config.getProductionLine()).
                withWorkCell(config.getWorkCell()).
                withSite(config.getSite()).
                withPrefixAllTopics(config.isPrefixAllTopics()).
                withEnabled(config.isEnabled());

        this.config = builderIsa95.build();
        return Configurator.ConfigResult.SUCCESS;
    }

    @Override
    public void sync(final @NotNull HiveMQConfigEntity entity){
        final ISA95 isa95 = config;
        entity.getUns().getIsa95().setEnabled(isa95.isEnabled());
        entity.getUns().getIsa95().setPrefixAllTopics(isa95.isPrefixAllTopics());
        entity.getUns().getIsa95().setArea(isa95.getArea());
        entity.getUns().getIsa95().setEnterprise(isa95.getEnterprise());
        entity.getUns().getIsa95().setSite(isa95.getSite());
        entity.getUns().getIsa95().setWorkCell(isa95.getWorkCell());
        entity.getUns().getIsa95().setProductionLine(isa95.getProductionLine());
    }


    public void setISA95(final ISA95 isa95) {
        replaceConfigsAndTriggerWrite(isa95);
    }

    private void replaceConfigsAndTriggerWrite(ISA95 newConfig) {
        config = newConfig;
        notifyConsumer();
        configFileReaderWriter.writeConfigWithSync();
    }

    private void notifyConsumer() {
        final var consumer = this.consumer;
        if(consumer != null) {
            consumer.accept(config);
        }
    }

    @Override
    public void registerConsumer(final Consumer<ISA95> consumer) {
        this.consumer = consumer;
        notifyConsumer();
    }
}
