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
    public Map<String, String> getTopicReplacements(final NamespaceProfile profile) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        profile.getSegments().forEach(c ->
                builder.put(c.getName(), Strings.isNullOrEmpty(c.getValue()) ? "" : c.getValue()));
        return builder.build();
    }

    @Override
    public MqttTopic prefixWithActiveProfile(final @NotNull NamespaceProfile profile, final @NotNull  MqttTopic topic) {
        Preconditions.checkNotNull(topic);
        Preconditions.checkNotNull(profile);
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        profile.getSegments().stream().
                filter(s -> !Strings.isNullOrEmpty(s.getValue())).forEach(c ->
                    builder.add(c.getValue()));
        List<String> parts = builder.addAll(topic.getLevels()).build();
        return MqttTopic.of(String.join("/", parts));
    }

    @Override
    public synchronized List<NamespaceProfile> getConfiguredProfiles(boolean includeLegacy) {

        //we need to ensure that configuration created before the new uns concepts are still supported
        //so we need to check the old config items and seed to new stuff from the old (UNLESS) it has bee
        //updated since
        //check to see if it has subsequently been stored in the configured profiles
        List<NamespaceProfile> configured = configurationService.getProfiles();
        if(includeLegacy){
            if(!configured.contains(NamespaceProfile.PROFILE_ISA_95)){
                ISA95 legacy = configurationService.getISA95();
                NamespaceProfile isa95 = new NamespaceProfileImpl(NamespaceProfile.PROFILE_ISA_95);
                NamespaceUtils.setValueAtSegment(isa95, ISA95.ENTERPRISE, legacy.getEnterprise());
                NamespaceUtils.setValueAtSegment(isa95, ISA95.SITE, legacy.getSite());
                NamespaceUtils.setValueAtSegment(isa95, ISA95.AREA, legacy.getArea());
                NamespaceUtils.setValueAtSegment(isa95, ISA95.PRODUCTION_LINE, legacy.getProductionLine());
                NamespaceUtils.setValueAtSegment(isa95, ISA95.WORK_CELL, legacy.getWorkCell());
                //-- If there is only 1 its from the old config and therefore it should be enabled
                if(configured.isEmpty()){
                    isa95.setEnabled(legacy.isEnabled());
                }
                isa95.setPrefixAllTopics(legacy.isPrefixAllTopics());
                configured.add(isa95);
            }
        }
        return new ArrayList<>(configured);
    }

    @Override
    public Optional<NamespaceProfile> getActiveProfile() {
        return getConfiguredProfiles(true).stream().filter(
                NamespaceProfile::getEnabled).findFirst();
    }

    @Override
    public synchronized void setConfiguredProfiles(final List<NamespaceProfile> profiles) {
        configurationService.setProfiles(profiles);
    }
}
