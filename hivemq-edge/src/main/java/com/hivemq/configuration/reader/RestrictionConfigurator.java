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
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.hivemq.configuration.service.RestrictionsConfigurationService.*;

public class RestrictionConfigurator implements Configurator<RestrictionsEntity>{

    private static final Logger log = LoggerFactory.getLogger(RestrictionConfigurator.class);

    private final @NotNull RestrictionsConfigurationService restrictionsConfigurationService;

    private volatile RestrictionsEntity configEntity;
    private volatile boolean initialized = false;

    public RestrictionConfigurator(final @NotNull RestrictionsConfigurationService restrictionsConfigurationService) {
        this.restrictionsConfigurationService = restrictionsConfigurationService;
    }

    @Override
    public boolean needsRestartWithConfig(final HiveMQConfigEntity config) {
        if(initialized && hasChanged(this.configEntity, config.getRestrictionsConfig())) {
            return true;
        }
        return false;
    }

    @Override
    public ConfigResult applyConfig(final @NotNull HiveMQConfigEntity config) {
        this.configEntity = config.getRestrictionsConfig();
        this.initialized = true;

        restrictionsConfigurationService.setMaxConnections(validateMaxConnections(configEntity.getMaxConnections()));
        restrictionsConfigurationService.setMaxClientIdLength(validateMaxClientIdLength(configEntity.getMaxClientIdLength()));
        restrictionsConfigurationService.setNoConnectIdleTimeout(validateNoConnectIdleTimeout(configEntity.getNoConnectIdleTimeout()));
        restrictionsConfigurationService.setIncomingLimit(validateIncomingLimit(configEntity.getIncomingBandwidthThrottling()));
        restrictionsConfigurationService.setMaxTopicLength(validateMaxTopicLength(configEntity.getMaxTopicLength()));
        return ConfigResult.SUCCESS;
    }

    private long validateMaxConnections(final long maxConnections) {
        if (maxConnections == UNLIMITED_CONNECTIONS) {
            return maxConnections;
        }
        if (maxConnections < MAX_CONNECTIONS_MINIMUM) {
            log.warn(
                    "The configured max-connections ({}) must be at least {}. The default value (unlimited) is used instead.",
                    maxConnections,
                    MAX_CONNECTIONS_MINIMUM);
            return MAX_CONNECTIONS_DEFAULT;
        }
        return maxConnections;
    }

    private int validateMaxClientIdLength(final int maxClientIdLength) {
        if (maxClientIdLength < MAX_CLIENT_ID_LENGTH_MINIMUM || maxClientIdLength > MAX_CLIENT_ID_LENGTH_MAXIMUM) {
            log.warn(
                    "The configured max-client-id-length ({}) must be in the range {} - {}. The default value ({}) is used instead.",
                    maxClientIdLength,
                    MAX_CLIENT_ID_LENGTH_MINIMUM,
                    MAX_CLIENT_ID_LENGTH_MAXIMUM,
                    MAX_CLIENT_ID_LENGTH_DEFAULT);
            return MAX_CLIENT_ID_LENGTH_DEFAULT;
        }
        return maxClientIdLength;
    }

    private int validateMaxTopicLength(final int maxTopicLength) {
        if (maxTopicLength < MAX_TOPIC_LENGTH_MINIMUM || maxTopicLength > MAX_TOPIC_LENGTH_MAXIMUM) {
            log.warn(
                    "The configured max-topic-length ({}) must be in the range {} - {}. The default value ({}) is used instead.",
                    maxTopicLength, MAX_TOPIC_LENGTH_MINIMUM, MAX_TOPIC_LENGTH_MAXIMUM, MAX_TOPIC_LENGTH_DEFAULT);
            return MAX_TOPIC_LENGTH_DEFAULT;
        }
        return maxTopicLength;
    }

    private long validateNoConnectIdleTimeout(final long noConnectIdleTimeout) {
        if (noConnectIdleTimeout < NO_CONNECT_IDLE_TIMEOUT_MINIMUM) {
            log.warn(
                    "The configured no-connect-idle-timeout ({}ms) must be at least {}ms. The default value ({}ms) is used instead.",
                    noConnectIdleTimeout,
                    NO_CONNECT_IDLE_TIMEOUT_MINIMUM,
                    NO_CONNECT_IDLE_TIMEOUT_DEFAULT);
            return NO_CONNECT_IDLE_TIMEOUT_DEFAULT;
        }
        return noConnectIdleTimeout;
    }

    private long validateIncomingLimit(final long incomingLimit) {
        if (incomingLimit < INCOMING_BANDWIDTH_THROTTLING_MINIMUM) {
            log.warn(
                    "The configured incoming-bandwidth-throttling ({} bytes/second) must be at least {} bytes/second. The default value (unlimited) is used instead.",
                    incomingLimit,
                    INCOMING_BANDWIDTH_THROTTLING_MINIMUM);
            return INCOMING_BANDWIDTH_THROTTLING_DEFAULT;
        }
        return incomingLimit;
    }



}
