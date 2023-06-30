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
package com.hivemq.configuration.service;

import com.hivemq.configuration.entity.mqttsn.BroadcastAddress;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqttsn.MqttsnTopicAlias;

import java.util.List;
import java.util.Map;

/**
 * A Configuration service which allows to get information about the current MQTTSN configuration
 * and allows to change the global MQTTSN configuration of HiveMQ at runtime.
 */
public interface MqttsnConfigurationService {

    void addPredefinedAlias(final @NotNull MqttsnTopicAlias alias);

    Map<Integer, MqttsnTopicAlias> getPredefinedTopicAliases();

    void setAllowEmptyClientIdentifierEnabled(final boolean enabled);

    void setAllowAnonymousPublishMinus1Enabled(final boolean enabled);

    void setAllowWakingPingToHijackSessionEnabled(final boolean enabled);

    void setTopicRegistrationsHeldDuringSleepEnabled(final boolean enabled);

    void setMaxClientIdentifierLength(final int len);

    void setGatewayId(final int gatewayId);

    int getGatewayId();

    boolean isAllowEmptyClientIdentifierEnabled();

    boolean isAllowAnonymousPublishMinus1Enabled();

    boolean isAllowWakingPingToHijackSessionEnabled();

    boolean isTopicRegistrationsHeldDuringSleepEnabled();

    int getMaxClientIdentifierLength();

    void setDiscoveryEnabled(boolean enabled);

    void setDiscoveryBroadcastIntervalSeconds(int discoveryIntervalSeconds);

    void setDiscoveryBroadcastAddresses(List<BroadcastAddress> addresses);

    int getDiscoveryBroadcastIntervalSeconds();

    boolean isDiscoveryEnabled();

    List<BroadcastAddress> getDiscoveryBroadcastAddresses();
}
