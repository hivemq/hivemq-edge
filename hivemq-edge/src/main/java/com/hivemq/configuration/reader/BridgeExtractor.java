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

import static com.google.common.base.Preconditions.checkArgument;

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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BridgeExtractor
        implements ReloadableExtractor<List<@NotNull MqttBridgeEntity>, List<@NotNull MqttBridge>> {

    private static final Logger log = LoggerFactory.getLogger(BridgeExtractor.class);
    public static final String KEYSTORE_TYPE_PKCS12 = "PKCS12";
    public static final String KEYSTORE_TYPE_JKS = "JKS";

    private volatile @NotNull List<@NotNull MqttBridge> bridgeEntities = List.of();
    private volatile @Nullable Consumer<List<@NotNull MqttBridge>> bridgeEntitiesConsumer =
            cfg -> log.debug("No consumer registered yet");

    private final @NotNull ConfigFileReaderWriter configFileReaderWriter;

    public BridgeExtractor(@NotNull final ConfigFileReaderWriter configFileReaderWriter) {
        this.configFileReaderWriter = configFileReaderWriter;
    }

    /**
     * These three mutators hold the extractor's monitor across {@code writeConfigWithSync()}, and that
     * is deliberate even though it is a lock-order inversion against the configuration watcher, which
     * takes {@code ConfigFileReaderWriter}'s lock first and then calls into {@link #updateConfig}.
     * <p>
     * Releasing the monitor before the write — tried in EDG-882 QA round 3 to close that inversion —
     * opens a worse window: {@code updateConfig} is itself synchronized, so between the model mutation
     * and the file write the watcher can read the not-yet-written file, overwrite {@code bridgeEntities}
     * with the pre-edit configuration and notify the bridge subsystem, which reverts the operator's
     * change and clears the queues of whatever the reverted configuration no longer contains. Trading a
     * pre-existing deadlock for a new path that silently discards a REST change and its messages is not
     * a fix. Closing both properly needs one configuration-transition lock shared by the reload and the
     * mutators, which is a change to the shared configuration subsystem and belongs to its own ticket
     * (EDG-882 QA round 4).
     */
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
        return new ImmutableList.Builder<MqttBridge>().addAll(bridgeEntities).build();
    }

    /**
     * Replaces the configuration of one bridge in place, as a single configuration transition.
     * <p>
     * An update used to be expressed as {@link #removeBridge(String)} followed by
     * {@link #addBridge(MqttBridge)}. Each of those notifies the consumer, so
     * {@code BridgeService.updateBridges} saw the bridge disappear from the configuration and treated
     * it as a removal: {@code internalStopBridge(active, clearQueue = true, retain = List.of())} —
     * an empty retain list, so every forwarder queue of the bridge was cleared, including the
     * subscriptions the operator had not touched. Editing one subscription through the API destroyed
     * the backlog of all the others, which is exactly the loss EDG-882 exists to stop, arrived at
     * through the path the UI uses (EDG-882 QA round 1).
     * <p>
     * Replacing in place keeps the bridge present across the transition, so it is classified as an
     * update and goes through {@code restartBridge}, which retains the queues of the forwarders that
     * survive into the new configuration and holds them across the hand-over.
     *
     * @return {@code false} if no bridge with that id is configured, in which case nothing changed.
     */
    public synchronized boolean replaceBridge(final @NotNull String id, final @NotNull MqttBridge mqttBridge) {
        // The contract enforced where it is relied upon (EDG-882 review v02, R2-09). The REST layer
        // rejects an id change, so today the two always agree; a caller that passed a body with a
        // different id would leave the list without the old id and with a new one, which updateBridges
        // classifies as remove + add -- and the removal clears every queue of the old bridge, silently.
        // That is the defect this method exists to prevent, arrived at from the other side.
        checkArgument(
                id.equals(mqttBridge.getId()),
                "Cannot replace bridge '%s' with a configuration carrying id '%s': replacing in place is"
                        + " what keeps the bridge present across the transition, and a different id makes"
                        + " it a removal followed by an addition, which clears the queues of the bridge"
                        + " being replaced.",
                id,
                mqttBridge.getId());
        if (bridgeEntities.stream().noneMatch(entry -> entry.getId().equals(id))) {
            return false;
        }
        if (!mqttBridge.isPersist()) {
            log.info(
                    "MQTT Bridge '{}' has persist flag set to false, QoS for publishes from local subscriptions will be downgraded to AT_MOST_ONCE.",
                    mqttBridge.getId());
        }

        bridgeEntities = bridgeEntities.stream()
                .map(entry -> entry.getId().equals(id) ? mqttBridge : entry)
                .collect(ImmutableList.toImmutableList());

        notifyConsumer();
        configFileReaderWriter.writeConfigWithSync();
        return true;
    }

    /**
     * Names a credential that carries leading or trailing whitespace, because nothing else will.
     * <p>
     * An element's text is its text: {@code <password>\n    ${ENV:PW}\n</password>} is rendered before
     * the file is parsed, so the value the bridge ends up sending carries the operator's indentation.
     * The remote rejects it, the log says the credentials were refused, and the configuration looks
     * correct to anyone reading it — the difference is invisible in a diff and in the REST response
     * alike. Writing a placeholder on its own indented line is the natural way to write one, so this is
     * a trap an operator falls into by formatting their file tidily (EDG-882 QA, 2026-08-25).
     * <p>
     * <b>Reported, not trimmed</b>, for two reasons. Whitespace is legal in an MQTT password and
     * trimming would silently break anyone who means it, with no way to opt out. And the placeholder
     * restore that keeps this credential out of {@code config.xml} on write-back
     * ({@link com.hivemq.util.render.EnvVarUtil#restorePlaceholders}) anchors on the element's text
     * exactly as the marshaller writes it: trimming here would make the search string miss, the restore
     * give up, and the secret land on disk — trading a failed connection for a disclosure. Whether to
     * trim is a product decision about every string in {@code config.xml}, not a repair to this method.
     */
    private static void warnIfPadded(
            final @NotNull String bridgeId, final @NotNull String element, final @Nullable String value) {
        if (value == null || value.isEmpty() || value.equals(value.strip())) {
            return;
        }
        log.warn(
                "The <{}> of bridge '{}' begins or ends with whitespace, and that whitespace is part of the"
                        + " value the bridge sends. If the element was written across several lines -- a"
                        + " '${ENV:...}' placeholder indented on its own line, for instance -- the indentation"
                        + " is in the credential and the remote broker will refuse the connection. Put the value"
                        + " on the same line as its element, or remove the padding.",
                element,
                bridgeId);
    }

    public synchronized void removeBridge(final @NotNull String id) {
        bridgeEntities = bridgeEntities.stream()
                .filter(entry -> !entry.getId().equals(id))
                .toList();

        notifyConsumer();
        configFileReaderWriter.writeConfigWithSync();
    }

    @Override
    public synchronized Configurator.ConfigResult updateConfig(final HiveMQConfigEntity config) {
        final var bridgeEntities = convertBridgeConfigs(config);

        final Set<String> bridgeIds = new HashSet<>();
        final var duplicates =
                bridgeEntities.stream().filter(n -> !bridgeIds.add(n.getId())).toList();

        if (!duplicates.isEmpty()) {
            log.error("Duplicated bridgeIds found: {}", duplicates);
            return Configurator.ConfigResult.ERROR;
        }

        this.bridgeEntities = bridgeEntities;
        notifyConsumer();
        return Configurator.ConfigResult.SUCCESS;
    }

    @Override
    public boolean needsRestartWithConfig(final HiveMQConfigEntity config) {
        return false;
    }

    @Override
    public void registerConsumer(final Consumer<List<@NotNull MqttBridge>> consumer) {
        this.bridgeEntitiesConsumer = consumer;
        notifyConsumer();
    }

    private @NotNull List<@NotNull MqttBridge> convertBridgeConfigs(final @NotNull HiveMQConfigEntity config) {
        return config.getBridgeConfig().stream()
                .map(bridgeConfig -> {
                    final RemoteBrokerEntity remoteBroker = bridgeConfig.getRemoteBroker();
                    final MqttBridge.Builder builder = new MqttBridge.Builder();

                    builder.withHost(remoteBroker.getHost())
                            .withPort(remoteBroker.getPort())
                            .withKeepAlive(remoteBroker.getMqtt().getKeepAlive())
                            .withSessionExpiry(remoteBroker.getMqtt().getSessionExpiry())
                            .withCleanStart(remoteBroker.getMqtt().isCleanStart())
                            .withLoopPreventionEnabled(
                                    bridgeConfig.getLoopPrevention().isEnabled())
                            .withLoopPreventionHopCount(
                                    bridgeConfig.getLoopPrevention().getHopCountLimit());

                    if (bridgeConfig.getId() == null || bridgeConfig.getId().isBlank()) {
                        log.error("Bridge id cannot be empty");
                        throw new UnrecoverableException(false);
                    }

                    if (!bridgeConfig.getId().matches(HiveMQEdgeConstants.ID_REGEX)) {
                        log.error(
                                "Bridge name is only allowed to contain: \"[a-z]|[A-Z]|[0-9]|-|_\". Found: '{}'",
                                bridgeConfig.getId());
                        throw new UnrecoverableException(false);
                    }

                    builder.withId(bridgeConfig.getId());

                    if (bridgeConfig.getRemoteSubscriptions().isEmpty()
                            && bridgeConfig.getForwardedTopics().isEmpty()) {
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
                    if (bridgeWebsocketConfig != null) {
                        builder.withWebsocketConfiguration(bridgeWebsocketConfig);
                    }

                    if (remoteBroker.getAuthentication() != null
                            && remoteBroker.getAuthentication().getMqttSimpleAuthenticationEntity() != null) {
                        final String user = remoteBroker
                                .getAuthentication()
                                .getMqttSimpleAuthenticationEntity()
                                .getUser();
                        final String password = remoteBroker
                                .getAuthentication()
                                .getMqttSimpleAuthenticationEntity()
                                .getPassword();
                        warnIfPadded(bridgeConfig.getId(), "username", user);
                        warnIfPadded(bridgeConfig.getId(), "password", password);
                        builder.withUsername(user).withPassword(password);
                    }

                    if (remoteBroker.getMqtt().getClientId() != null) {
                        builder.withClientId(remoteBroker.getMqtt().getClientId());
                    } else {
                        builder.withClientId(bridgeConfig.getId());
                    }

                    builder.persist(bridgeConfig.getPersist());
                    return builder.build();
                })
                .toList();
    }

    private @NotNull List<LocalSubscription> convertLocalSubscriptions(
            final @NotNull String name, final @NotNull List<ForwardedTopicEntity> forwardedTopics) {
        final ImmutableList.Builder<LocalSubscription> builder = ImmutableList.builder();
        for (final ForwardedTopicEntity forwardedTopic : forwardedTopics) {
            validateTopicFilters(name, forwardedTopic.getFilters());
            final String exampleTopicFilter = forwardedTopic.getFilters().get(0);
            validateDestinationTopic(name, forwardedTopic.getDestination(), exampleTopicFilter);
            builder.add(new LocalSubscription(
                    forwardedTopic.getFilters(),
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
            final String exampleTopicFilter = remoteSubscription.getFilters().isEmpty()
                    ? "#"
                    : remoteSubscription.getFilters().get(0);
            validateDestinationTopic(name, remoteSubscription.getDestination(), exampleTopicFilter);
            builder.add(new RemoteSubscription(
                    remoteSubscription.getFilters(),
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
                // try with a random generated example topic, based on topic filter and verify  if a destination can be
                // reached
                String exampleTopic = exampleTopicFilter.replaceAll(
                        "\\+", UUID.randomUUID().toString().substring(0, 4));
                exampleTopic =
                        exampleTopic.replace("#", UUID.randomUUID().toString().substring(0, 9));

                log.trace(
                        "Bridge Extension: Validation: validate a random topic {} against destination topic {} ",
                        exampleTopic,
                        destination);

                //                TopicFilterProcessor.applyDestinationModifier(MqttTopic.of(exampleTopic), destination,
                // bridgeName);

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

        for (final CustomUserPropertyEntity customUserProperty : customUserProperties) {
            if (customUserProperty.getKey() != null && customUserProperty.getValue() != null) {
                builder.add(CustomUserProperty.of(customUserProperty.getKey(), customUserProperty.getValue()));
            } else {
                log.debug("Ignoring custom user property for MQTT bridge '{}', key and value must be specified", name);
            }
        }
        return builder.build();
    }

    private @Nullable BridgeWebsocketConfig convertWebsocketConfig(
            final @Nullable BridgeWebsocketConfigurationEntity websocketConfiguration) {
        if (websocketConfiguration == null || !websocketConfiguration.isEnabled()) {
            return null;
        }
        return new BridgeWebsocketConfig(
                websocketConfiguration.getServerPath(), websocketConfiguration.getSubProtocol());
    }

    private @Nullable BridgeTls convertTls(final @Nullable BridgeTlsEntity tls) {
        if (tls == null || !tls.isEnabled()) {
            return null;
        }

        final BridgeTls.Builder builder = new BridgeTls.Builder();
        if (tls.getKeyStore() != null && !tls.getKeyStore().getPath().isBlank()) {
            builder.withKeystorePath(tls.getKeyStore().getPath())
                    .withKeystorePassword(
                            tls.getKeyStore().getPassword() != null
                                    ? tls.getKeyStore().getPassword()
                                    : "")
                    .withPrivateKeyPassword(
                            tls.getKeyStore().getPrivateKeyPassword() != null
                                    ? tls.getKeyStore().getPrivateKeyPassword()
                                    : "");

            if (tls.getKeyStore().getPath().endsWith(".p12")
                    || tls.getKeyStore().getPath().endsWith(".pfx")) {
                builder.withKeystoreType(KEYSTORE_TYPE_PKCS12);
            } else {
                builder.withKeystoreType(KEYSTORE_TYPE_JKS);
            }
        }

        if (tls.getTrustStore() != null && !tls.getTrustStore().getPath().isBlank()) {
            builder.withTruststorePath(tls.getTrustStore().getPath())
                    .withTruststorePassword(tls.getTrustStore().getPassword());

            if (tls.getTrustStore().getPath().endsWith(".p12")
                    || tls.getTrustStore().getPath().endsWith(".pfx")) {
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
        final List<MqttBridgeEntity> newList =
                tmpBridges.stream().map(this::uncovert).toList();
        entity.getBridgeConfig().clear();
        entity.getBridgeConfig().addAll(newList);
    }

    protected MqttBridgeEntity uncovert(final MqttBridge from) {

        final MqttBridgeEntity entity = new MqttBridgeEntity();
        entity.setId(from.getId());

        // -- RemoteBrokerEntity
        final RemoteBrokerEntity remoteBrokerEntity = unconvertBrokerEntity(from);
        entity.setRemoteBroker(remoteBrokerEntity);

        // -- LoopPreventionEntity
        final LoopPreventionEntity loopPreventionEntity = new LoopPreventionEntity();
        loopPreventionEntity.setEnabled(from.isLoopPreventionEnabled());
        loopPreventionEntity.setHopCountLimit(from.getLoopPreventionHopCount());
        entity.setLoopPrevention(loopPreventionEntity);
        entity.setPersist(from.isPersist());

        // -- ForwardedTopicEntity*
        if (from.getLocalSubscriptions() != null) {
            entity.setForwardedTopics(unconvertLocalSubscriptions(from.getLocalSubscriptions()));
        }

        // -- RemoteSubscriptionEntity*
        if (from.getRemoteSubscriptions() != null) {
            entity.setRemoteSubscriptions(unconvertRemoteSubscriptions(from.getRemoteSubscriptions()));
        }

        return entity;
    }

    protected List<RemoteSubscriptionEntity> unconvertRemoteSubscriptions(
            final List<RemoteSubscription> remoteSubscriptionList) {

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
                subscriptionEntity.setCustomUserProperties(subscription.getCustomUserProperties().stream()
                        .map(this::unconvertCustomUserProperty)
                        .collect(Collectors.toList()));
            }
            builder.add(subscriptionEntity);
        }
        return builder.build();
    }

    protected List<ForwardedTopicEntity> unconvertLocalSubscriptions(
            final List<LocalSubscription> localSubscriptionList) {

        final ImmutableList.Builder<ForwardedTopicEntity> builder = ImmutableList.builder();
        for (final LocalSubscription subscription : localSubscriptionList) {
            final ForwardedTopicEntity forwardedTopicEntity = new ForwardedTopicEntity();
            forwardedTopicEntity.setDestination(subscription.getDestination());
            // The configured order, not the canonical one: sorting exists so that a reorder is not a
            // configuration change, not so that writing the file reorders the operator's elements.
            forwardedTopicEntity.setExcludes(new ArrayList<>(subscription.getConfiguredExcludes()));
            forwardedTopicEntity.setFilters(new ArrayList<>(subscription.getConfiguredFilters()));
            forwardedTopicEntity.setMaxQoS(subscription.getMaxQoS());
            forwardedTopicEntity.setPreserveRetain(subscription.isPreserveRetain());
            // Dropped silently until now, so any write of config.xml deleted the operator's
            // <queue-limit> and the reload that followed restarted the bridge for a change nobody made.
            //
            // Only when it fits the element: config.xsd declares queue-limit as xs:int, and the REST API
            // takes an int64 that nothing bounds, so a larger value would fail schema-validated
            // marshalling -- and writeConfigWithSync logs that failure and carries on, which would lose
            // the whole write, including whatever unrelated subsystem triggered it (EDG-882 QA round 3).
            final Long queueLimit = subscription.getQueueLimit();
            if (queueLimit != null && (queueLimit > Integer.MAX_VALUE || queueLimit < Integer.MIN_VALUE)) {
                log.warn(
                        "The queue limit {} of the forwarded topic with destination '{}' does not fit the"
                                + " configuration file's queue-limit element and cannot be written to it; the"
                                + " limit stays in effect for this node but will not survive a restart.",
                        queueLimit,
                        subscription.getDestination());
            } else {
                forwardedTopicEntity.setQueueLimit(queueLimit);
            }
            if (subscription.getCustomUserProperties() != null) {
                forwardedTopicEntity.setCustomUserProperties(subscription.getCustomUserProperties().stream()
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

        // Bridge MqttEntity
        final BridgeMqttEntity bridgeMqttEntity = new BridgeMqttEntity();
        bridgeMqttEntity.setCleanStart(from.isCleanStart());
        // Only when it is not the default. convertBridgeConfigs fills the client id in from the bridge
        // id when the element is absent, so writing it back unconditionally added a <client-id> element
        // to the operator's file that they never wrote -- on every REST write of any subsystem
        // (EDG-882 QA round 2). Same configuration either way; the file just stops growing elements.
        if (!from.getId().equals(from.getClientId())) {
            bridgeMqttEntity.setClientId(from.getClientId());
        }
        bridgeMqttEntity.setKeepAlive(from.getKeepAlive());
        bridgeMqttEntity.setSessionExpiry(from.getSessionExpiry());
        remoteBrokerEntity.setMqtt(bridgeMqttEntity);

        // Authentication
        // Both halves, because config.xsd requires both elements: a bridge created over REST with a
        // username and no password -- legal MQTT, and rejected nowhere -- has its username silently
        // dropped here, and the reload that follows reads a changed bridge and restarts it for a change
        // nobody made. Writing the element with one half would fail schema-validated marshalling and
        // abort the whole configuration write instead, which is worse. Closing this properly means
        // either relaxing the schema or refusing the combination at the API; both are decisions of their
        // own and neither belongs to EDG-882 (QA round 3).
        if (from.getUsername() != null && from.getPassword() != null) {
            final BridgeAuthenticationEntity authentication = new BridgeAuthenticationEntity();
            final MqttSimpleAuthenticationEntity simpleAuthenticationEntity = new MqttSimpleAuthenticationEntity();
            simpleAuthenticationEntity.setPassword(from.getPassword());
            simpleAuthenticationEntity.setUser(from.getUsername());
            authentication.setMqttSimpleAuthenticationEntity(simpleAuthenticationEntity);
            remoteBrokerEntity.setAuthentication(authentication);
        }

        // Websocket
        if (from.getBridgeWebsocketConfig() != null) {
            final BridgeWebsocketConfigurationEntity websocketConfiguration = new BridgeWebsocketConfigurationEntity();
            websocketConfiguration.setEnabled(true);
            websocketConfiguration.setServerPath(from.getBridgeWebsocketConfig().getPath());
            websocketConfiguration.setSubProtocol(
                    from.getBridgeWebsocketConfig().getSubProtocol());
            remoteBrokerEntity.setBridgeWebsocketConfig(websocketConfiguration);
        }

        // TLS
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

    private void notifyConsumer() {
        final var consumer = bridgeEntitiesConsumer;
        if (consumer != null) {
            consumer.accept(bridgeEntities);
        }
    }
}
