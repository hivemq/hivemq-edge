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
import com.hivemq.configuration.entity.RestrictionsEntity;
import com.hivemq.configuration.entity.uns.ISA95Entity;
import com.hivemq.configuration.entity.uns.UnsConfigEntity;
import com.hivemq.configuration.service.UnsConfigurationService;
import org.jetbrains.annotations.NotNull;
import com.hivemq.uns.config.ISA95;

import javax.inject.Inject;

public class UnsConfigurator implements Syncable<UnsConfigEntity>{

    private final @NotNull UnsConfigurationService unsConfigurationService;

    private volatile UnsConfigEntity configEntity;
    private volatile boolean initialized = false;

    @Inject
    public UnsConfigurator(final @NotNull UnsConfigurationService unsConfigurationService) {
        this.unsConfigurationService = unsConfigurationService;
    }

    @Override
    public boolean needsRestartWithConfig(final HiveMQConfigEntity config) {
        if(initialized && hasChanged(this.configEntity, config.getUns())) {
            return true;
        }
        return false;
    }

    @Override
    public ConfigResult applyConfig(final @NotNull HiveMQConfigEntity config) {
        this.configEntity = config.getUns();
        this.initialized = true;

        if (configEntity == null) {
            return ConfigResult.SUCCESS;
        }

        final ISA95Entity isa95Entity = configEntity.getIsa95();
        final ISA95.Builder builderIsa95 = new ISA95.Builder();
        builderIsa95.withArea(isa95Entity.getArea()).
                withEnterprise(isa95Entity.getEnterprise()).
                withProductionLine(isa95Entity.getProductionLine()).
                withWorkCell(isa95Entity.getWorkCell()).
                withSite(isa95Entity.getSite()).
                withPrefixAllTopics(isa95Entity.isPrefixAllTopics()).
                withEnabled(isa95Entity.isEnabled());
        unsConfigurationService.setISA95(builderIsa95.build());

        return ConfigResult.SUCCESS;
    }

    @Override
    public void sync(final @NotNull HiveMQConfigEntity entity){
        final ISA95 isa95 = unsConfigurationService.getISA95();
        entity.getUns().getIsa95().setEnabled(isa95.isEnabled());
        entity.getUns().getIsa95().setPrefixAllTopics(isa95.isPrefixAllTopics());
        entity.getUns().getIsa95().setArea(isa95.getArea());
        entity.getUns().getIsa95().setEnterprise(isa95.getEnterprise());
        entity.getUns().getIsa95().setSite(isa95.getSite());
        entity.getUns().getIsa95().setWorkCell(isa95.getWorkCell());
        entity.getUns().getIsa95().setProductionLine(isa95.getProductionLine());
    }
}
