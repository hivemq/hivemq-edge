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
package com.hivemq.uns.impl;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.hivemq.client.mqtt.datatypes.MqttTopic;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.configuration.service.UnsConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.uns.NamespaceUtils;
import com.hivemq.uns.UnifiedNamespaceService;
import com.hivemq.uns.config.ISA95;
import com.hivemq.uns.config.NamespaceProfile;
import com.hivemq.uns.config.impl.NamespaceProfileImpl;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 *
 * Services relating to UNS operations, including ISA95 & ISA88
 * configuration etc.
 *
 * ISA95 -> enterprise/site/area/production-line/work-cell
 *
 * @author Simon L Johnson
 */
public class UnifiedNamespaceServiceImpl implements UnifiedNamespaceService {

    private final @NotNull UnsConfigurationService configurationService;

    @Inject
    public UnifiedNamespaceServiceImpl(final @NotNull ConfigurationService configurationService) {
        this.configurationService = configurationService.unsConfiguration();
    }

    @Override
    public ISA95 getISA95() {
        return configurationService.getISA95();
    }

    @Override
    public void setISA95(final ISA95 isa95) {
        Preconditions.checkNotNull(isa95, "isa-95 must be set");
        configurationService.setISA95(isa95);
    }

    public Map<String, String> getTopicReplacements(final @NotNull ISA95 isa95){
        return ImmutableMap.<String, String>builder()
                .put(ISA95.ENTERPRISE,  Strings.isNullOrEmpty(isa95.getEnterprise()) ? "" : isa95.getEnterprise())
                .put(ISA95.SITE,        Strings.isNullOrEmpty(isa95.getSite()) ? "" : isa95.getSite())
                .put(ISA95.AREA,        Strings.isNullOrEmpty(isa95.getArea()) ? "" : isa95.getArea())
                .put(ISA95.WORK_CELL,   Strings.isNullOrEmpty(isa95.getWorkCell()) ? "" : isa95.getWorkCell())
                .put(ISA95.PRODUCTION_LINE, Strings.isNullOrEmpty(isa95.getProductionLine()) ? "" : isa95.getProductionLine())
                .build();
    }

    @Override
    public MqttTopic prefixISA95(final @NotNull MqttTopic topic) {
        Preconditions.checkNotNull(topic);
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        ISA95 isa95 = getISA95();
        if(!Strings.isNullOrEmpty(isa95.getEnterprise())){
            builder.add(isa95.getEnterprise());
        }
        if(!Strings.isNullOrEmpty(isa95.getSite())){
            builder.add(isa95.getSite());
        }
        if(!Strings.isNullOrEmpty(isa95.getArea())){
            builder.add(isa95.getArea());
        }
        if(!Strings.isNullOrEmpty(isa95.getProductionLine())){
            builder.add(isa95.getProductionLine());
        }
        if(!Strings.isNullOrEmpty(isa95.getWorkCell())){
            builder.add(isa95.getWorkCell());
        }
        List<String> parts = builder.addAll(topic.getLevels()).build();
        return MqttTopic.of(String.join("/", parts));
    }

    @Override
    public List<NamespaceProfile> getAvailableProfiles() {
        //we need to ensure that configuration created before the new uns concepts are still supported
        //so we need to check the old config items and seed to new stuff from the old (UNLESS) it has bee
        //updated since
        //check to see if it has subsequently been stored in the configured profiles
        Optional<NamespaceProfile> configured = getConfiguredProfileByType(NamespaceUtils.getNamespaceProfileType(
                NamespaceProfile.PROFILE_ISA_95));
        NamespaceProfile isa95;
        if(configured.isPresent()){
            isa95 = configured.get();
        } else {
            ISA95 legacy = getISA95();
            isa95 = new NamespaceProfileImpl(NamespaceProfile.PROFILE_ISA_95);
            NamespaceUtils.setValueAtSegment(isa95, ISA95.ENTERPRISE, legacy.getEnterprise());
            NamespaceUtils.setValueAtSegment(isa95, ISA95.SITE, legacy.getSite());
            NamespaceUtils.setValueAtSegment(isa95, ISA95.AREA, legacy.getArea());
            NamespaceUtils.setValueAtSegment(isa95, ISA95.PRODUCTION_LINE, legacy.getProductionLine());
            NamespaceUtils.setValueAtSegment(isa95, ISA95.WORK_CELL, legacy.getWorkCell());
        }
        return List.of(isa95);
    }

    @Override
    public synchronized List<NamespaceProfile> getConfiguredProfiles() {
        return new ArrayList<>(configurationService.getProfiles());
    }

    @Override
    public Optional<NamespaceProfile> getActiveProfile() {
        return configurationService.getActiveProfile();
    }

    @Override
    public Optional<NamespaceProfile> getConfiguredProfileByType(String type) {
        return getConfiguredProfiles().stream().filter(p ->
                NamespaceUtils.getNamespaceProfileType(p).equals(type)).findFirst();
    }

    @Override
    public synchronized void setConfiguredProfiles(final List<NamespaceProfile> profiles) {
        configurationService.setProfiles(profiles);
    }
}
