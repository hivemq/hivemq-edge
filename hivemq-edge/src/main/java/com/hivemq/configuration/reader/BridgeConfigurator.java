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
import com.hivemq.bridge.config.*;
import com.hivemq.configuration.entity.bridge.*;
import com.hivemq.configuration.entity.listener.tls.KeystoreEntity;
import com.hivemq.configuration.entity.listener.tls.TruststoreEntity;
import com.hivemq.configuration.service.BridgeConfigurationService;
import com.hivemq.edge.HiveMQEdgeConstants;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.util.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class BridgeConfigurator {

    private static final Logger log = LoggerFactory.getLogger(BridgeConfigurator.class);
    public static final String KEYSTORE_TYPE_PKCS12 = "PKCS12";
    public static final String KEYSTORE_TYPE_JKS = "JKS";

    private final @NotNull BridgeConfigurationService bridgeConfigurationService;

    @Inject
    public BridgeConfigurator(
            final @NotNull BridgeConfigurationService bridgeConfigurationService) {
        this.bridgeConfigurationService = bridgeConfigurationService;
    }

    public void setBridgeConfig(final @NotNull List<MqttBridgeEntity> bridgeConfigs) {

        if (bridgeConfigs.isEmpty()) {
            return;
        }

        for (MqttBridgeEntity bridgeConfig : bridgeConfigs) {
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

            bridgeConfigurationService.addBridge(builder.build());
        }
    }

    private @NotNull List<LocalSubscription> convertLocalSubscriptions(
            final @NotNull String name, @NotNull List<ForwardedTopicEntity> forwardedTopics) {
        final ImmutableList.Builder<LocalSubscription> builder = ImmutableList.builder();
        for (ForwardedTopicEntity forwardedTopic : forwardedTopics) {
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
        for (String filter : filters) {
            if (!Topics.isValidToSubscribe(filter)) {
                log.error("Topic filter '{}' for bridge '{}' is not valid", filter, name);
                throw new UnrecoverableException(false);
            }
        }
    }

    private @NotNull List<RemoteSubscription> convertRemoteSubscriptions(
            final @NotNull String name, final @NotNull List<RemoteSubscriptionEntity> remoteSubscriptions) {
        final ImmutableList.Builder<RemoteSubscription> builder = ImmutableList.builder();
        for (RemoteSubscriptionEntity remoteSubscription : remoteSubscriptions) {
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

    public void syncBridgeConfig(final @NotNull List<MqttBridgeEntity> bridgeConfigs) {
        if (bridgeConfigs == null) {
            return;
        }
        List<MqttBridge> liveBridges = bridgeConfigurationService.getBridges();
        List<MqttBridgeEntity> newList = liveBridges.stream().map(this::uncovert).collect(Collectors.toList());
        bridgeConfigs.clear();
        bridgeConfigs.addAll(newList);
    }

    protected MqttBridgeEntity uncovert(MqttBridge from) {

        MqttBridgeEntity entity = new MqttBridgeEntity();
        entity.setId(from.getId());

        //-- RemoteBrokerEntity
        RemoteBrokerEntity remoteBrokerEntity = unconvertBrokerEntity(from);
        entity.setRemoteBroker(remoteBrokerEntity);

        //-- LoopPreventionEntity
        LoopPreventionEntity loopPreventionEntity = new LoopPreventionEntity();
        loopPreventionEntity.setEnabled(from.isLoopPreventionEnabled());
        loopPreventionEntity.setHopCountLimit(from.getLoopPreventionHopCount());

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

    protected List<RemoteSubscriptionEntity> unconvertRemoteSubscriptions(List<RemoteSubscription> remoteSubscriptionList) {

        ImmutableList.Builder<RemoteSubscriptionEntity> builder = ImmutableList.builder();
        for (RemoteSubscription subscription : remoteSubscriptionList) {
            RemoteSubscriptionEntity subscriptionEntity = new RemoteSubscriptionEntity();
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

    protected List<ForwardedTopicEntity> unconvertLocalSubscriptions(List<LocalSubscription> localSubscriptionList) {

        ImmutableList.Builder<ForwardedTopicEntity> builder = ImmutableList.builder();
        for (LocalSubscription subscription : localSubscriptionList) {
            ForwardedTopicEntity forwardedTopicEntity = new ForwardedTopicEntity();
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

    protected CustomUserPropertyEntity unconvertCustomUserProperty(CustomUserProperty property) {
        CustomUserPropertyEntity entity = new CustomUserPropertyEntity();
        entity.setKey(property.getKey());
        entity.setValue(property.getValue());
        return entity;
    }

    protected RemoteBrokerEntity unconvertBrokerEntity(MqttBridge from) {

        RemoteBrokerEntity remoteBrokerEntity = new RemoteBrokerEntity();
        remoteBrokerEntity.setPort(from.getPort());
        remoteBrokerEntity.setHost(from.getHost());

        //Bridge MqttEntity
        BridgeMqttEntity bridgeMqttEntity = new BridgeMqttEntity();
        bridgeMqttEntity.setCleanStart(from.isCleanStart());
        bridgeMqttEntity.setClientId(from.getClientId());
        bridgeMqttEntity.setKeepAlive(from.getKeepAlive());
        bridgeMqttEntity.setSessionExpiry(from.getSessionExpiry());
        remoteBrokerEntity.setMqtt(bridgeMqttEntity);

        //Authentication
        if (from.getUsername() != null && from.getPassword() != null) {
            BridgeAuthenticationEntity authentication = new BridgeAuthenticationEntity();
            MqttSimpleAuthenticationEntity simpleAuthenticationEntity = new MqttSimpleAuthenticationEntity();
            simpleAuthenticationEntity.setPassword(from.getPassword());
            simpleAuthenticationEntity.setUser(from.getUsername());
            authentication.setMqttSimpleAuthenticationEntity(simpleAuthenticationEntity);
            remoteBrokerEntity.setAuthentication(authentication);
        }

        //TLS
        BridgeTls bridgeTls = from.getBridgeTls();
        if (bridgeTls != null) {
            BridgeTlsEntity bridgeTlsEntity = new BridgeTlsEntity();
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
                KeystoreEntity keystoreEntity = new KeystoreEntity();
                keystoreEntity.setPath(bridgeTls.getKeystorePath());
                keystoreEntity.setPassword(bridgeTls.getKeystorePassword());
                keystoreEntity.setPrivateKeyPassword(bridgeTls.getPrivateKeyPassword());
                bridgeTlsEntity.setKeyStore(keystoreEntity);
            }

            if (bridgeTls.getTruststorePath() != null) {
                TruststoreEntity truststoreEntity = new TruststoreEntity();
                truststoreEntity.setPath(bridgeTls.getTruststorePath());
                truststoreEntity.setPassword(bridgeTls.getTruststorePassword());
                bridgeTlsEntity.setTrustStore(truststoreEntity);
            }

            remoteBrokerEntity.setTls(bridgeTlsEntity);
        }

        return remoteBrokerEntity;
    }
}
