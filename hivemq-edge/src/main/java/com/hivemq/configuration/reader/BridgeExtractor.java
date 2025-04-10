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

import com.google.common.collect.ImmutableList;
import com.hivemq.bridge.config.BridgeTls;
import com.hivemq.bridge.config.BridgeWebsocketConfig;
import com.hivemq.bridge.config.CustomUserProperty;
import com.hivemq.bridge.config.LocalSubscription;
import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.bridge.config.RemoteSubscription;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.bridge.BridgeAuthenticationEntity;
import com.hivemq.configuration.entity.bridge.BridgeMqttEntity;
import com.hivemq.configuration.entity.bridge.BridgeTlsEntity;
import com.hivemq.configuration.entity.bridge.BridgeWebsocketConfigurationEntity;
import com.hivemq.configuration.entity.bridge.CustomUserPropertyEntity;
import com.hivemq.configuration.entity.bridge.ForwardedTopicEntity;
import com.hivemq.configuration.entity.bridge.LoopPreventionEntity;
import com.hivemq.configuration.entity.bridge.MqttBridgeEntity;
import com.hivemq.configuration.entity.bridge.MqttSimpleAuthenticationEntity;
import com.hivemq.configuration.entity.bridge.RemoteBrokerEntity;
import com.hivemq.configuration.entity.bridge.RemoteSubscriptionEntity;
import com.hivemq.configuration.entity.listener.tls.KeystoreEntity;
import com.hivemq.configuration.entity.listener.tls.TruststoreEntity;
import com.hivemq.edge.HiveMQEdgeConstants;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.util.Topics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BridgeExtractor implements ReloadableExtractor<List<@NotNull MqttBridgeEntity>, List<@NotNull MqttBridge>> {

    private static final Logger log = LoggerFactory.getLogger(BridgeExtractor.class);
    public static final String KEYSTORE_TYPE_PKCS12 = "PKCS12";
    public static final String KEYSTORE_TYPE_JKS = "JKS";

    private volatile @NotNull List<@NotNull MqttBridge> bridgeEntities = List.of();
    private volatile @Nullable Consumer<List<@NotNull MqttBridge>> bridgeEntitiesConsumer = cfg -> log.debug("No consumer registered yet");

    private final @NotNull ConfigFileReaderWriter configFileReaderWriter;

    public BridgeExtractor(@NotNull final ConfigFileReaderWriter configFileReaderWriter) {
        this.configFileReaderWriter = configFileReaderWriter;
    }

    public synchronized void addBridge(final @NotNull MqttBridge mqttBridge) {
        if (!mqttBridge.isPersist()) {
            log.info(
                    "MQTT Bridge '{}' has persist flag set to false, QoS for publishes from local subscriptions will be downgraded to AT_MOST_ONCE.",
                    mqttBridge.getId());
        }

        bridgeEntities = new ImmutableList.Builder<MqttBridge>()
                .addAll(bridgeEntities)
                .add(mqttBridge)
                .build();

        notifyConsumer();
        configFileReaderWriter.writeConfigWithSync();
    }

    public @NotNull List<MqttBridge> getBridges() {
        return new ImmutableList.Builder<MqttBridge>()
                .addAll(bridgeEntities)
                .build();
    }

    public synchronized void removeBridge(final @NotNull String id) {
        bridgeEntities = bridgeEntities.stream().filter(entry -> !entry.getId().equals(id)).toList();

        notifyConsumer();
        configFileReaderWriter.writeConfigWithSync();
    }

    private void notifyConsumer() {
        final var consumer = bridgeEntitiesConsumer;
        if(consumer != null) {
            consumer.accept(bridgeEntities);
        }
    }


    @Override
    public boolean needsRestartWithConfig(final HiveMQConfigEntity config) {
        return false;
    }

    @Override
    public synchronized Configurator.ConfigResult updateConfig(final HiveMQConfigEntity config) {
        var bridgeEntities = convertBridgeConfigs(config);
        bridgeEntities.forEach(entity -> bridgeIds.add(entity.getId()));

        Set<String> bridgeIds = new HashSet<>();
        bridgeEntities.stream()
            .filter(n -> !bridgeIds.add(n.getId()))
            .toList();



        this.bridgeEntities = bridgeEntities;
        notifyConsumer();
        return Configurator.ConfigResult.SUCCESS;
    }

    @Override
    public void registerConsumer(final Consumer<List<@NotNull MqttBridge>> consumer) {
        this.bridgeEntitiesConsumer = consumer;
        notifyConsumer();
    }

    private @NotNull List<@NotNull MqttBridge> convertBridgeConfigs(final @NotNull HiveMQConfigEntity config) {
        return config.getBridgeConfig().stream().map(bridgeConfig ->  {
            final RemoteBrokerEntity remoteBroker = bridgeConfig.getRemoteBroker();
            final MqttBridge.Builder builder = new MqttBridge.Builder();

            builder.withHost(remoteBroker.getHost())
                    .withPort(remoteBroker.getPort())
                    .withKeepAlive(remoteBroker.getMqtt().getKeepAlive())
                    .withSessionExpiry(remoteBroker.getMqtt().getSessionExpiry())
                    .withCleanStart(remoteBroker.getMqtt().isCleanStart())
                    .withLoopPreventionEnabled(bridgeConfig.getLoopPrevention().isEnabled())
                    .withLoopPreventionHopCount(bridgeConfig.getLoopPrevention().getHopCountLimit());

            if (bridgeConfig.getId() == null || bridgeConfig.getId().isBlank()) {
                log.error("Bridge id cannot be empty");
                throw new UnrecoverableException(false);
            }

            if (!bridgeConfig.getId().matches(HiveMQEdgeConstants.ID_REGEX)) {
                log.error("Bridge name is only allowed to contain: \"[a-z]|[A-Z]|[0-9]|-|_\". Found: '{}'",
                        bridgeConfig.getId());
                throw new UnrecoverableException(false);
            }

            builder.withId(bridgeConfig.getId());

            if (bridgeConfig.getRemoteSubscriptions().isEmpty() && bridgeConfig.getForwardedTopics().isEmpty()) {
                log.warn(
                        "No remote subscriptions or forwarded topics configured for bridge '{}', no messages will be processed by this bridge.",
                        bridgeConfig.getId());
            }

            final List<RemoteSubscription> remoteSubscriptions =
                    convertRemoteSubscriptions(bridgeConfig.getId(), bridgeConfig.getRemoteSubscriptions());
            builder.withRemoteSubscriptions(remoteSubscriptions);

            final List<LocalSubscription> localSubscriptions =
                    convertLocalSubscriptions(bridgeConfig.getId(), bridgeConfig.getForwardedTopics());
            builder.withLocalSubscriptions(localSubscriptions);


            final BridgeTls bridgeTls = convertTls(remoteBroker.getTls());
            if (bridgeTls != null) {
                builder.withBridgeTls(bridgeTls);
            }

            final BridgeWebsocketConfig bridgeWebsocketConfig =
                    convertWebsocketConfig(remoteBroker.getBridgeWebsocketConfig());
            if(bridgeWebsocketConfig != null) {
                builder.withWebsocketConfiguration(bridgeWebsocketConfig);
            }

            if (remoteBroker.getAuthentication() != null &&
                    remoteBroker.getAuthentication().getMqttSimpleAuthenticationEntity() != null) {
                builder.withUsername(remoteBroker.getAuthentication().getMqttSimpleAuthenticationEntity().getUser())
                        .withPassword(remoteBroker.getAuthentication()
                                .getMqttSimpleAuthenticationEntity()
                                .getPassword());
            }

            if (remoteBroker.getMqtt().getClientId() != null) {
                builder.withClientId(remoteBroker.getMqtt().getClientId());
            } else {
                builder.withClientId(bridgeConfig.getId());
            }

            builder.persist(bridgeConfig.getPersist());
            return builder.build();
        }).toList();
    }

    private @NotNull List<LocalSubscription> convertLocalSubscriptions(
            final @NotNull String name, @NotNull List<ForwardedTopicEntity> forwardedTopics) {
        final ImmutableList.Builder<LocalSubscription> builder = ImmutableList.builder();
        for (final ForwardedTopicEntity forwardedTopic : forwardedTopics) {
            validateTopicFilters(name, forwardedTopic.getFilters());
            final String exampleTopicFilter = forwardedTopic.getFilters().get(0);
            validateDestinationTopic(name, forwardedTopic.getDestination(), exampleTopicFilter);
            builder.add(new LocalSubscription(forwardedTopic.getFilters(),
                    forwardedTopic.getDestination(),
                    forwardedTopic.getExcludes(),
                    convertCustomUserProperties(name, forwardedTopic.getCustomUserProperties()),
                    forwardedTopic.isPreserveRetain(),
                    forwardedTopic.getMaxQoS(),
                    forwardedTopic.getQueueLimit()));
        }
        return builder.build();
    }

    public static void validateTopicFilters(final @NotNull String name, final @Nullable List<String> filters) {
        if (filters == null || filters.isEmpty()) {
            log.error("Topic filters are missing for bridge '{}'.", name);
            throw new UnrecoverableException(false);
        }
        for (final String filter : filters) {
            if (!Topics.isValidToSubscribe(filter)) {
                log.error("Topic filter '{}' for bridge '{}' is not valid", filter, name);
                throw new UnrecoverableException(false);
            }
        }
    }

    private @NotNull List<RemoteSubscription> convertRemoteSubscriptions(
            final @NotNull String name, final @NotNull List<RemoteSubscriptionEntity> remoteSubscriptions) {
        final ImmutableList.Builder<RemoteSubscription> builder = ImmutableList.builder();
        for (final RemoteSubscriptionEntity remoteSubscription : remoteSubscriptions) {
            final String exampleTopicFilter =
                    remoteSubscription.getFilters().isEmpty() ? "#" : remoteSubscription.getFilters().get(0);
            validateDestinationTopic(name, remoteSubscription.getDestination(), exampleTopicFilter);
            builder.add(new RemoteSubscription(remoteSubscription.getFilters(),
                    remoteSubscription.getDestination(),
                    convertCustomUserProperties(name, remoteSubscription.getCustomUserProperties()),
                    remoteSubscription.isPreserveRetain(),
                    remoteSubscription.getMaxQoS()));
        }
        return builder.build();
    }

    private static void validateDestinationTopic(
            final @NotNull String bridgeName,
            final @Nullable String destination,
            final @NotNull String exampleTopicFilter) {
        if (destination != null && !destination.isEmpty()) {
            try {
                // try with a random generated example topic, based on topic filter and verify  if a destination can be reached
                String exampleTopic =
                        exampleTopicFilter.replaceAll("\\+", UUID.randomUUID().toString().substring(0, 4));
                exampleTopic = exampleTopic.replace("#", UUID.randomUUID().toString().substring(0, 9));

                log.trace("Bridge Extension: Validation: validate a random topic {} against destination topic {} ",
                        exampleTopic,
                        destination);

//                TopicFilterProcessor.applyDestinationModifier(MqttTopic.of(exampleTopic), destination, bridgeName);


            } catch (final Exception all) {
                log.error("Destination topic for bridge '{}' is not valid ", bridgeName);
                throw new UnrecoverableException(false);
            }
        }
    }

    @NotNull
    private static List<CustomUserProperty> convertCustomUserProperties(
            final @NotNull String name, final @NotNull List<CustomUserPropertyEntity> customUserProperties) {
        final ImmutableList.Builder<CustomUserProperty> builder = ImmutableList.builder();

        for (CustomUserPropertyEntity customUserProperty : customUserProperties) {
            if (customUserProperty.getKey() != null && customUserProperty.getValue() != null) {
                builder.add(CustomUserProperty.of(customUserProperty.getKey(), customUserProperty.getValue()));
            } else {
                log.debug("Ignoring custom user property for MQTT bridge '{}', key and value must be specified", name);
            }
        }
        return builder.build();
    }

    private @Nullable BridgeWebsocketConfig convertWebsocketConfig(final @Nullable BridgeWebsocketConfigurationEntity websocketConfiguration) {
        if(websocketConfiguration == null || !websocketConfiguration.isEnabled()) {
            return null;
        }
        return new BridgeWebsocketConfig(websocketConfiguration.getServerPath(),
                websocketConfiguration.getSubProtocol());
    }

    private @Nullable BridgeTls convertTls(final @Nullable BridgeTlsEntity tls) {
        if (tls == null || !tls.isEnabled()) {
            return null;
        }

        final BridgeTls.Builder builder = new BridgeTls.Builder();
        if (tls.getKeyStore() != null && !tls.getKeyStore().getPath().isBlank()) {
            builder.withKeystorePath(tls.getKeyStore().getPath())
                    .withKeystorePassword(tls.getKeyStore().getPassword() != null ?
                            tls.getKeyStore().getPassword() :
                            "")
                    .withPrivateKeyPassword(tls.getKeyStore().getPrivateKeyPassword() != null ?
                            tls.getKeyStore().getPrivateKeyPassword() :
                            "");

            if (tls.getKeyStore().getPath().endsWith(".p12") || tls.getKeyStore().getPath().endsWith(".pfx")) {
                builder.withKeystoreType(KEYSTORE_TYPE_PKCS12);
            } else {
                builder.withKeystoreType(KEYSTORE_TYPE_JKS);
            }
        }

        if (tls.getTrustStore() != null && !tls.getTrustStore().getPath().isBlank()) {
            builder.withTruststorePath(tls.getTrustStore().getPath())
                    .withTruststorePassword(tls.getTrustStore().getPassword());

            if (tls.getTrustStore().getPath().endsWith(".p12") || tls.getTrustStore().getPath().endsWith(".pfx")) {
                builder.withTruststoreType(KEYSTORE_TYPE_PKCS12);
            } else {
                builder.withTruststoreType(KEYSTORE_TYPE_JKS);
            }
        }

        return builder.withProtocols(tls.getProtocols())
                .withCipherSuites(tls.getCipherSuites())
                .withHandshakeTimeout(tls.getHandshakeTimeout())
                .withVerifyHostname(tls.isVerifyHostname())
                .build();
    }

    @Override
    public synchronized void sync(final @NotNull HiveMQConfigEntity entity) {
        final var tmpBridges = bridgeEntities;
        final List<MqttBridgeEntity> newList = tmpBridges.stream().map(this::uncovert).toList();
        entity.getBridgeConfig().clear();
        entity.getBridgeConfig().addAll(newList);
    }

    protected MqttBridgeEntity uncovert(final MqttBridge from) {

        final MqttBridgeEntity entity = new MqttBridgeEntity();
        entity.setId(from.getId());

        //-- RemoteBrokerEntity
        final RemoteBrokerEntity remoteBrokerEntity = unconvertBrokerEntity(from);
        entity.setRemoteBroker(remoteBrokerEntity);

        //-- LoopPreventionEntity
        final LoopPreventionEntity loopPreventionEntity = new LoopPreventionEntity();
        loopPreventionEntity.setEnabled(from.isLoopPreventionEnabled());
        loopPreventionEntity.setHopCountLimit(from.getLoopPreventionHopCount());
        entity.setLoopPrevention(loopPreventionEntity);
        entity.setPersist(from.isPersist());

        //-- ForwardedTopicEntity*
        if (from.getLocalSubscriptions() != null) {
            entity.setForwardedTopics(unconvertLocalSubscriptions(from.getLocalSubscriptions()));
        }

        //-- RemoteSubscriptionEntity*
        if (from.getRemoteSubscriptions() != null) {
            entity.setRemoteSubscriptions(unconvertRemoteSubscriptions(from.getRemoteSubscriptions()));
        }

        return entity;
    }

    protected List<RemoteSubscriptionEntity> unconvertRemoteSubscriptions(final List<RemoteSubscription> remoteSubscriptionList) {

        final ImmutableList.Builder<RemoteSubscriptionEntity> builder = ImmutableList.builder();
        for (final RemoteSubscription subscription : remoteSubscriptionList) {
            final RemoteSubscriptionEntity subscriptionEntity = new RemoteSubscriptionEntity();
            subscriptionEntity.setDestination(subscription.getDestination());
            if (subscription.getFilters() != null) {
                subscriptionEntity.setFilters(new ArrayList<>(subscription.getFilters()));
            }
            subscriptionEntity.setPreserveRetain(subscription.isPreserveRetain());
            subscriptionEntity.setMaxQoS(subscription.getMaxQoS());
            if (subscription.getCustomUserProperties() != null) {
                subscriptionEntity.setCustomUserProperties(subscription.getCustomUserProperties()
                        .stream()
                        .map(this::unconvertCustomUserProperty)
                        .collect(Collectors.toList()));
            }
            builder.add(subscriptionEntity);
        }
        return builder.build();
    }

    protected List<ForwardedTopicEntity> unconvertLocalSubscriptions(final List<LocalSubscription> localSubscriptionList) {

        final ImmutableList.Builder<ForwardedTopicEntity> builder = ImmutableList.builder();
        for (final LocalSubscription subscription : localSubscriptionList) {
            final ForwardedTopicEntity forwardedTopicEntity = new ForwardedTopicEntity();
            forwardedTopicEntity.setDestination(subscription.getDestination());
            if (subscription.getExcludes() != null) {
                forwardedTopicEntity.setExcludes(new ArrayList<>(subscription.getExcludes()));
            }
            if (subscription.getFilters() != null) {
                forwardedTopicEntity.setFilters(new ArrayList<>(subscription.getFilters()));
            }
            forwardedTopicEntity.setMaxQoS(subscription.getMaxQoS());
            forwardedTopicEntity.setPreserveRetain(subscription.isPreserveRetain());
            if (subscription.getCustomUserProperties() != null) {
                forwardedTopicEntity.setCustomUserProperties(subscription.getCustomUserProperties()
                        .stream()
                        .map(this::unconvertCustomUserProperty)
                        .collect(Collectors.toList()));
            }
            builder.add(forwardedTopicEntity);
        }
        return builder.build();
    }

    protected CustomUserPropertyEntity unconvertCustomUserProperty(final CustomUserProperty property) {
        final CustomUserPropertyEntity entity = new CustomUserPropertyEntity();
        entity.setKey(property.getKey());
        entity.setValue(property.getValue());
        return entity;
    }

    protected RemoteBrokerEntity unconvertBrokerEntity(final MqttBridge from) {

        final RemoteBrokerEntity remoteBrokerEntity = new RemoteBrokerEntity();
        remoteBrokerEntity.setPort(from.getPort());
        remoteBrokerEntity.setHost(from.getHost());

        //Bridge MqttEntity
        final BridgeMqttEntity bridgeMqttEntity = new BridgeMqttEntity();
        bridgeMqttEntity.setCleanStart(from.isCleanStart());
        bridgeMqttEntity.setClientId(from.getClientId());
        bridgeMqttEntity.setKeepAlive(from.getKeepAlive());
        bridgeMqttEntity.setSessionExpiry(from.getSessionExpiry());
        remoteBrokerEntity.setMqtt(bridgeMqttEntity);

        //Authentication
        if (from.getUsername() != null && from.getPassword() != null) {
            final BridgeAuthenticationEntity authentication = new BridgeAuthenticationEntity();
            final MqttSimpleAuthenticationEntity simpleAuthenticationEntity = new MqttSimpleAuthenticationEntity();
            simpleAuthenticationEntity.setPassword(from.getPassword());
            simpleAuthenticationEntity.setUser(from.getUsername());
            authentication.setMqttSimpleAuthenticationEntity(simpleAuthenticationEntity);
            remoteBrokerEntity.setAuthentication(authentication);
        }

        //Websocket
        if (from.getBridgeWebsocketConfig() != null) {
            final BridgeWebsocketConfigurationEntity websocketConfiguration =
                    new BridgeWebsocketConfigurationEntity();
            websocketConfiguration.setEnabled(true);
            websocketConfiguration.setServerPath(from.getBridgeWebsocketConfig().getPath());
            websocketConfiguration.setSubProtocol(from.getBridgeWebsocketConfig().getSubProtocol());
            remoteBrokerEntity.setBridgeWebsocketConfig(websocketConfiguration);
        }

        //TLS
        final BridgeTls bridgeTls = from.getBridgeTls();
        if (bridgeTls != null) {
            final BridgeTlsEntity bridgeTlsEntity = new BridgeTlsEntity();
            bridgeTlsEntity.setEnabled(true);
            bridgeTlsEntity.setHandshakeTimeout(bridgeTls.getHandshakeTimeout());
            bridgeTlsEntity.setVerifyHostname(bridgeTls.isVerifyHostname());

            if (bridgeTls.getCipherSuites() != null) {
                bridgeTlsEntity.setCipherSuites(new ArrayList<>(bridgeTls.getCipherSuites()));
            }

            if (bridgeTls.getProtocols() != null) {
                bridgeTlsEntity.setProtocols(new ArrayList<>(bridgeTls.getProtocols()));
            }

            if (bridgeTls.getKeystorePath() != null) {
                final KeystoreEntity keystoreEntity = new KeystoreEntity();
                keystoreEntity.setPath(bridgeTls.getKeystorePath());
                keystoreEntity.setPassword(bridgeTls.getKeystorePassword());
                keystoreEntity.setPrivateKeyPassword(bridgeTls.getPrivateKeyPassword());
                bridgeTlsEntity.setKeyStore(keystoreEntity);
            }

            if (bridgeTls.getTruststorePath() != null) {
                final TruststoreEntity truststoreEntity = new TruststoreEntity();
                truststoreEntity.setPath(bridgeTls.getTruststorePath());
                truststoreEntity.setPassword(bridgeTls.getTruststorePassword());
                bridgeTlsEntity.setTrustStore(truststoreEntity);
            }

            remoteBrokerEntity.setTls(bridgeTlsEntity);
        }

        return remoteBrokerEntity;
    }
}
