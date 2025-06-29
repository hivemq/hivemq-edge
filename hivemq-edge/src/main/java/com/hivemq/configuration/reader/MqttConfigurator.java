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
import com.hivemq.configuration.entity.InternalConfigEntity;
import com.hivemq.configuration.entity.MqttConfigEntity;
import com.hivemq.configuration.service.MqttConfigurationService;
import org.jetbrains.annotations.NotNull;
import com.hivemq.mqtt.message.QoS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import static com.hivemq.configuration.entity.mqtt.MqttConfigurationDefaults.KEEP_ALIVE_MAX_DEFAULT;
import static com.hivemq.configuration.entity.mqtt.MqttConfigurationDefaults.MAXIMUM_QOS_DEFAULT;
import static com.hivemq.configuration.entity.mqtt.MqttConfigurationDefaults.MAX_EXPIRY_INTERVAL_DEFAULT;
import static com.hivemq.configuration.entity.mqtt.MqttConfigurationDefaults.SERVER_RECEIVE_MAXIMUM_DEFAULT;
import static com.hivemq.configuration.entity.mqtt.MqttConfigurationDefaults.TOPIC_ALIAS_MAX_PER_CLIENT_MAXIMUM;
import static com.hivemq.configuration.entity.mqtt.MqttConfigurationDefaults.TOPIC_ALIAS_MAX_PER_CLIENT_MINIMUM;
import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT;
import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.DEFAULT_RECEIVE_MAXIMUM;
import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRE_ON_DISCONNECT;
import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRY_MAX;

public class MqttConfigurator implements Configurator<MqttConfigEntity>{

    private final @NotNull MqttConfigurationService mqttConfigurationService;
    private static final Logger log = LoggerFactory.getLogger(MqttConfigurator.class);
    private volatile MqttConfigEntity configEntity;
    private volatile boolean initialized = false;


    @Inject
    public MqttConfigurator(final @NotNull MqttConfigurationService mqttConfigurationService) {
        this.mqttConfigurationService = mqttConfigurationService;
    }

    @Override
    public boolean needsRestartWithConfig(final HiveMQConfigEntity config) {
        if(initialized && hasChanged(this.configEntity, config.getMqttConfig())) {
            return true;
        }
        return false;
    }

    @Override
    public ConfigResult applyConfig(final @NotNull HiveMQConfigEntity config) {
        this.configEntity = config.getMqttConfig();
        this.initialized = true;

        mqttConfigurationService.setRetainedMessagesEnabled(configEntity.getRetainedMessagesConfigEntity().isEnabled());

        mqttConfigurationService.setWildcardSubscriptionsEnabled(configEntity.getWildcardSubscriptionsConfigEntity().isEnabled());
        mqttConfigurationService.setSubscriptionIdentifierEnabled(configEntity.getSubscriptionIdentifierConfigEntity().isEnabled());
        mqttConfigurationService.setSharedSubscriptionsEnabled(configEntity.getSharedSubscriptionsConfigEntity().isEnabled());

        mqttConfigurationService.setMaximumQos(validateQoS(configEntity.getQoSConfigEntity().getMaxQos()));

        mqttConfigurationService.setTopicAliasEnabled(configEntity.getTopicAliasConfigEntity().isEnabled());
        mqttConfigurationService.setTopicAliasMaxPerClient(validateMaxPerClient(configEntity.getTopicAliasConfigEntity().getMaxPerClient()));

        mqttConfigurationService.setMaxQueuedMessages(configEntity.getQueuedMessagesConfigEntity().getMaxQueueSize());
        mqttConfigurationService.setQueuedMessagesStrategy(MqttConfigurationService.QueuedMessagesStrategy.valueOf(configEntity.getQueuedMessagesConfigEntity().getQueuedMessagesStrategy().name()));

        final long clientSessionExpiryInterval = configEntity.getSessionExpiryConfigEntity().getMaxInterval();
        mqttConfigurationService.setMaxSessionExpiryInterval(validateSessionExpiryInterval(clientSessionExpiryInterval));

        final long maxMessageExpiryInterval = configEntity.getMessageExpiryConfigEntity().getMaxInterval();
        mqttConfigurationService.setMaxMessageExpiryInterval(validateMessageExpiryInterval(maxMessageExpiryInterval));

        final int serverReceiveMaximum = configEntity.getReceiveMaximumConfigEntity().getServerReceiveMaximum();
        mqttConfigurationService.setServerReceiveMaximum(validateServerReceiveMaximum(serverReceiveMaximum));

        final int maxKeepAlive = configEntity.getKeepAliveConfigEntity().getMaxKeepAlive();
        mqttConfigurationService.setKeepAliveMax(validateKeepAliveMaximum(maxKeepAlive));
        mqttConfigurationService.setKeepAliveAllowZero(configEntity.getKeepAliveConfigEntity().isAllowUnlimted());

        final int maxPacketSize = configEntity.getPacketsConfigEntity().getMaxPacketSize();
        mqttConfigurationService.setMaxPacketSize(validateMaxPacketSize(maxPacketSize));

        return ConfigResult.SUCCESS;
    }

    private int validateMaxPerClient(final int maxPerClient) {
        if (maxPerClient < TOPIC_ALIAS_MAX_PER_CLIENT_MINIMUM) {
            log.warn("The configured topic alias maximum per client ({}) is too small. It was set to {} instead.", maxPerClient, TOPIC_ALIAS_MAX_PER_CLIENT_MINIMUM);
            return TOPIC_ALIAS_MAX_PER_CLIENT_MINIMUM;
        }
        if (maxPerClient > TOPIC_ALIAS_MAX_PER_CLIENT_MAXIMUM) {
            log.warn("The configured topic alias maximum per client ({}) is too large. It was set to {} instead.", maxPerClient, TOPIC_ALIAS_MAX_PER_CLIENT_MAXIMUM);
            return TOPIC_ALIAS_MAX_PER_CLIENT_MAXIMUM;
        }
        return maxPerClient;
    }

    @NotNull
    private QoS validateQoS(final int qos) {
        final QoS qoS = QoS.valueOf(qos);
        if (qoS != null) {
            return qoS;
        } else {
            log.warn("The configured maximum qos ({}) does not exist. It was set to ({}) instead.", qos, MAXIMUM_QOS_DEFAULT.getQosNumber());
            return MAXIMUM_QOS_DEFAULT;
        }
    }

    private long validateMessageExpiryInterval(final long maxMessageExpiryInterval) {
        if (maxMessageExpiryInterval <= 0) {
            log.warn("The configured max message expiry interval ({}) is too short. It was set to {} seconds instead.", maxMessageExpiryInterval, MAX_EXPIRY_INTERVAL_DEFAULT);
            return MAX_EXPIRY_INTERVAL_DEFAULT;
        }
        if (maxMessageExpiryInterval > MAX_EXPIRY_INTERVAL_DEFAULT) {
            log.warn("The configured max message expiry interval ({}) is too high. It was set to {} seconds instead.", maxMessageExpiryInterval, MAX_EXPIRY_INTERVAL_DEFAULT);
            return MAX_EXPIRY_INTERVAL_DEFAULT;
        }
        return maxMessageExpiryInterval;
    }

    private int validateMaxPacketSize(final int maxPacketSize) {
        if (maxPacketSize < 1) {
            log.warn("The configured max packet size ({}) is too short. It was set to {} bytes instead.", maxPacketSize, DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT);
            return DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT;
        }
        if (maxPacketSize > DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT) {
            log.warn("The configured max packet size ({}) is too high. It was set to {} bytes instead.", maxPacketSize, DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT);
            return DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT;
        }
        return maxPacketSize;
    }

    private long validateSessionExpiryInterval(final long sessionExpiryInterval) {
        if (sessionExpiryInterval < SESSION_EXPIRE_ON_DISCONNECT) {
            log.warn("The configured session expiry interval ({}) is too short. It was set to {} seconds instead.", sessionExpiryInterval, SESSION_EXPIRE_ON_DISCONNECT);
            return SESSION_EXPIRE_ON_DISCONNECT;
        }
        if (sessionExpiryInterval > SESSION_EXPIRY_MAX) {
            log.warn("The configured session expiry interval ({}) is too high. It was set to {} seconds instead.", sessionExpiryInterval, SESSION_EXPIRY_MAX);
            return SESSION_EXPIRY_MAX;
        }
        return sessionExpiryInterval;
    }

    private int validateServerReceiveMaximum(final int receiveMaximum) {
        if (receiveMaximum < 1) {
            log.warn("The configured server receive maximum ({}) is too short. It was set to {} seconds instead.", receiveMaximum, SERVER_RECEIVE_MAXIMUM_DEFAULT);
            return SERVER_RECEIVE_MAXIMUM_DEFAULT;
        }
        if (receiveMaximum > DEFAULT_RECEIVE_MAXIMUM) {
            log.warn("The configured server receive maximum ({}) is too high. It was set to {} seconds instead.", receiveMaximum, DEFAULT_RECEIVE_MAXIMUM);
            return DEFAULT_RECEIVE_MAXIMUM;
        }
        return receiveMaximum;
    }

    private int validateKeepAliveMaximum(final int keepAliveMaximum) {
        if (keepAliveMaximum < 1) {
            log.warn("The configured keep alive maximum ({}) is too short. It was set to {} seconds instead.", keepAliveMaximum, KEEP_ALIVE_MAX_DEFAULT);
            return KEEP_ALIVE_MAX_DEFAULT;
        }
        if (keepAliveMaximum > KEEP_ALIVE_MAX_DEFAULT) {
            log.warn("The configured keep alive maximum ({}) is too high. It was set to {} seconds instead.", keepAliveMaximum, KEEP_ALIVE_MAX_DEFAULT);
            return KEEP_ALIVE_MAX_DEFAULT;
        }
        return keepAliveMaximum;
    }
}
