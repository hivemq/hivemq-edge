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
package com.hivemq.configuration.service.impl;

import com.google.common.base.Preconditions;
import com.hivemq.annotations.ReadOnly;
import com.hivemq.configuration.entity.mqttsn.BroadcastAddress;
import com.hivemq.configuration.service.MqttsnConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqttsn.MqttsnTopicAlias;
import org.slj.mqtt.sn.MqttsnConstants;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Simon L Johnson
 */
@Singleton
public class MqttsnConfigurationServiceImpl implements MqttsnConfigurationService {

    private final Map<Integer, MqttsnTopicAlias> predefinedTopicAliases = new ConcurrentHashMap();
    private final List<BroadcastAddress> discoveryBroadcastAddresses = new ArrayList();

    private boolean discoveryEnabled = false;
    private int discoveryBroadcastIntervalSeconds = 30;
    private int gatewayId = 1;
    private boolean allowEmptyClientIdentifierEnabled = false;
    private boolean allowAnonymousPublishMinus1Enabled = false;
    private boolean allowWakingPingToHijackSessionEnabled = false;
    private boolean topicRegistrationsHeldDuringSleepEnabled = false;
    private int maxClientIdentifierLength = MqttsnConstants.MAX_CLIENT_ID_LENGTH_v12;

    @Override
    @ReadOnly
    @NotNull  public Map<Integer, MqttsnTopicAlias> getPredefinedTopicAliases() {
        return predefinedTopicAliases;
    }

    @Override
    public void addPredefinedAlias(final @NotNull MqttsnTopicAlias alias) {
        Preconditions.checkNotNull(alias);
        predefinedTopicAliases.put(alias.getAlias(), alias);
    }

    public boolean isAllowEmptyClientIdentifierEnabled() {
        return allowEmptyClientIdentifierEnabled;
    }

    @Override
    public void setAllowEmptyClientIdentifierEnabled(final boolean allowEmptyClientIdentifierEnabled) {
        this.allowEmptyClientIdentifierEnabled = allowEmptyClientIdentifierEnabled;
    }

    public boolean isAllowAnonymousPublishMinus1Enabled() {
        return allowAnonymousPublishMinus1Enabled;
    }

    @Override
    public void setAllowAnonymousPublishMinus1Enabled(final boolean allowAnonymousPublishMinus1Enabled) {
        this.allowAnonymousPublishMinus1Enabled = allowAnonymousPublishMinus1Enabled;
    }

    public boolean isAllowWakingPingToHijackSessionEnabled() {
        return allowWakingPingToHijackSessionEnabled;
    }

    @Override
    public void setAllowWakingPingToHijackSessionEnabled(final boolean allowWakingPingToHijackSessionEnabled) {
        this.allowWakingPingToHijackSessionEnabled = allowWakingPingToHijackSessionEnabled;
    }

    public boolean isTopicRegistrationsHeldDuringSleepEnabled() {
        return topicRegistrationsHeldDuringSleepEnabled;
    }

    @Override
    public void setTopicRegistrationsHeldDuringSleepEnabled(final boolean topicRegistrationsHeldDuringSleepEnabled) {
        this.topicRegistrationsHeldDuringSleepEnabled = topicRegistrationsHeldDuringSleepEnabled;
    }

    public int getMaxClientIdentifierLength() {
        return maxClientIdentifierLength;
    }

    @Override
    public void setMaxClientIdentifierLength(final int maxClientIdentifierLength) {
        this.maxClientIdentifierLength = maxClientIdentifierLength;
    }

    @Override
    public List<BroadcastAddress> getDiscoveryBroadcastAddresses() {
        return discoveryBroadcastAddresses;
    }

    @Override
    public boolean isDiscoveryEnabled() {
        return discoveryEnabled;
    }

    @Override
    public void setDiscoveryEnabled(final boolean discoveryEnabled) {
        this.discoveryEnabled = discoveryEnabled;
    }

    @Override
    public int getDiscoveryBroadcastIntervalSeconds() {
        return discoveryBroadcastIntervalSeconds;
    }

    @Override
    public void setDiscoveryBroadcastIntervalSeconds(final int discoveryBroadcastIntervalSeconds) {
        this.discoveryBroadcastIntervalSeconds = discoveryBroadcastIntervalSeconds;
    }

    @Override
    public void setDiscoveryBroadcastAddresses(final List<BroadcastAddress> addresses) {
        this.discoveryBroadcastAddresses.clear();
        this.discoveryBroadcastAddresses.addAll(addresses);
    }

    @Override
    public int getGatewayId() {
        return gatewayId;
    }

    @Override
    public void setGatewayId(final int gatewayId) {
        this.gatewayId = gatewayId;
    }
}

