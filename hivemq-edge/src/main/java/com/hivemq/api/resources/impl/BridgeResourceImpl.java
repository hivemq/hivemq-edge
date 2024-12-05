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
package com.hivemq.api.resources.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.hivemq.api.AbstractApi;
import com.hivemq.api.model.ApiConstants;
import com.hivemq.api.model.ApiErrorMessages;
import com.hivemq.api.model.bridge.Bridge;
import com.hivemq.api.model.bridge.BridgeList;
import com.hivemq.api.model.bridge.WebsocketConfiguration;
import com.hivemq.api.model.core.TlsConfiguration;
import com.hivemq.api.model.status.Status;
import com.hivemq.api.model.status.StatusList;
import com.hivemq.api.model.status.StatusTransitionCommand;
import com.hivemq.api.model.status.StatusTransitionResult;
import com.hivemq.api.resources.BridgeApi;
import com.hivemq.api.utils.ApiErrorUtils;
import com.hivemq.bridge.BridgeService;
import com.hivemq.bridge.config.BridgeTls;
import com.hivemq.bridge.config.BridgeWebsocketConfig;
import com.hivemq.bridge.config.CustomUserProperty;
import com.hivemq.bridge.config.LocalSubscription;
import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.bridge.config.RemoteSubscription;
import com.hivemq.configuration.reader.BridgeConfigurator;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.HiveMQEdgeConstants;
import com.hivemq.exceptions.UnrecoverableException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * @author Simon L Johnson
 */
public class BridgeResourceImpl extends AbstractApi implements BridgeApi {

    private final @NotNull ConfigurationService configurationService;
    private final @NotNull BridgeService bridgeService;
    private final @NotNull ExecutorService executorService;

    @Inject
    public BridgeResourceImpl(
            final @NotNull ConfigurationService configurationService,
            final @NotNull BridgeService bridgeService,
            final @NotNull ExecutorService executorService) {
        this.configurationService = configurationService;
        this.bridgeService = bridgeService;
        this.executorService = executorService;
    }

    @Override
    public @NotNull Response listBridges() {

        logger.trace("Bridge API listing events at {}", System.currentTimeMillis());
        List<MqttBridge> bridges = configurationService.bridgeConfiguration().getBridges();
        BridgeList list = new BridgeList(bridges.stream()
                .map(m -> Bridge.convert(m, getStatusInternal(m.getId())))
                .collect(Collectors.toList()));
        return Response.ok(list).build();
    }

    @Override
    public @NotNull Response addBridge(final @NotNull Bridge bridge) {

        ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        validateBridge(errorMessages, bridge);
        if (checkBridgeExists(bridge.getId())) {
            ApiErrorUtils.addValidationError(errorMessages, "bridge", "Bridge already existed");
        }
        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ApiErrorUtils.badRequest(errorMessages);
        } else {
            try {
                MqttBridge mqttBridge = unconvert(bridge);
                configurationService.bridgeConfiguration().addBridge(mqttBridge);
                return Response.ok().build();
            } finally {
                executorService.submit(bridgeService::updateBridges);
            }
        }
    }

    @Override
    public @NotNull Response getBridgeByName(final @NotNull String bridgeId) {

        ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        ApiErrorUtils.validateRequiredField(errorMessages, "id", bridgeId, false);
        ApiErrorUtils.validateRequiredFieldRegex(errorMessages, "id", bridgeId, HiveMQEdgeConstants.ID_REGEX);
        if (!checkBridgeExists(bridgeId)) {
            return ApiErrorUtils.notFound(String.format("Bridge not found by id '%s'", bridgeId));
        }
        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ApiErrorUtils.badRequest(errorMessages);
        } else {
            Optional<MqttBridge> bridge = configurationService.bridgeConfiguration()
                    .getBridges()
                    .stream()
                    .filter(b -> b.getId().equals(bridgeId))
                    .findFirst();
            if (bridge.isPresent()) {
                MqttBridge mqttBridge = bridge.get();
                return Response.ok(Bridge.convert(mqttBridge, getStatusInternal(bridgeId))).build();
            } else {
                return ApiErrorUtils.notFound(String.format("Bridge not found by id '%s'", bridgeId));
            }
        }
    }

    @Override
    public @NotNull Response deleteBridge(final @NotNull String bridgeId) {
        ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        ApiErrorUtils.validateRequiredField(errorMessages, "id", bridgeId, false);
        ApiErrorUtils.validateRequiredFieldRegex(errorMessages, "id", bridgeId, HiveMQEdgeConstants.ID_REGEX);
        if (!checkBridgeExists(bridgeId)) {
            return ApiErrorUtils.notFound(String.format("Bridge not found by id '%s'", bridgeId));
        }
        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ApiErrorUtils.badRequest(errorMessages);
        } else {
            try {
                configurationService.bridgeConfiguration().removeBridge(bridgeId);
                return Response.ok().build();
            } finally {
                bridgeService.updateBridges();
            }
        }
    }

    @Override
    public @NotNull Response changeStatus(
            final @NotNull String bridgeId, final @NotNull StatusTransitionCommand command) {

        ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        ApiErrorUtils.validateRequiredField(errorMessages, "id", bridgeId, false);
        ApiErrorUtils.validateRequiredFieldRegex(errorMessages, "id", bridgeId, HiveMQEdgeConstants.ID_REGEX);
        ApiErrorUtils.validateRequiredEntity(errorMessages, "command", command);
        if (!checkBridgeExists(bridgeId)) {
            return ApiErrorUtils.notFound(String.format("Bridge not found by id '%s'", bridgeId));
        }
        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ApiErrorUtils.badRequest(errorMessages);
        } else {
            switch (command.getCommand()) {
                case START:
                    bridgeService.startBridge(bridgeId);
                    break;
                case STOP:
                    bridgeService.stopBridgeAndRemoveQueues(bridgeId);
                    break;
                case RESTART:
                    bridgeService.restartBridge(bridgeId, getBridge(bridgeId));
                    break;
            }

            return Response.ok(StatusTransitionResult.pending(ApiConstants.BRIDGE_TYPE,
                    bridgeId,
                    ApiConstants.DEFAULT_TRANSITION_WAIT_TIMEOUT)).build();
        }
    }

    @Override
    public @NotNull Response updateBridge(final @NotNull String bridgeId, final @NotNull Bridge bridge) {

        ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        validateBridge(errorMessages, bridge);
        if (!bridgeId.equals(bridge.getId())) {
            ApiErrorUtils.addValidationError(errorMessages,
                    "id",
                    "Unable to change the id of a bridge, this field is immutable");
        }

        final MqttBridge previousBridgeConfig = getBridge(bridgeId);
        if (previousBridgeConfig == null) {
            ApiErrorUtils.addValidationError(errorMessages, "bridge", "Bridge did not exist to update");
        }
        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ApiErrorUtils.badRequest(errorMessages);
        } else {
            //-- Modify the configuration
            configurationService.bridgeConfiguration().removeBridge(bridgeId);
            final MqttBridge newBridgeConfig = unconvert(bridge);
            configurationService.bridgeConfiguration().addBridge(newBridgeConfig);
            //-- Restart the new configuration on a new connection
            bridgeService.restartBridge(bridgeId, newBridgeConfig);
            return Response.ok().build();
        }
    }

    @Override
    public @NotNull Response getStatus(final @NotNull String bridgeId) {

        ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        ApiErrorUtils.validateRequiredField(errorMessages, "id", bridgeId, false);
        ApiErrorUtils.validateRequiredFieldRegex(errorMessages, "id", bridgeId, HiveMQEdgeConstants.ID_REGEX);
        if (!checkBridgeExists(bridgeId)) {
            return ApiErrorUtils.notFound(String.format("Bridge not found by id '%s'", bridgeId));
        }
        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ApiErrorUtils.badRequest(errorMessages);
        } else {
            return Response.ok(getStatusInternal(bridgeId)).build();
        }
    }

    @Override
    public @NotNull Response status() {
        //-- Bridges
        ImmutableList.Builder<Status> builder = new ImmutableList.Builder<>();
        List<MqttBridge> bridges = configurationService.bridgeConfiguration().getBridges();
        for (MqttBridge bridge : bridges) {
            builder.add(getStatusInternal(bridge.getId()));
        }
        return Response.ok(new StatusList(builder.build())).build();
    }

    protected @NotNull Status getStatusInternal(final @NotNull String bridgeId) {

        Preconditions.checkNotNull(bridgeId);
        boolean connected = bridgeService.isConnected(bridgeId);
        Status.RUNTIME_STATUS runtimeStatus =
                bridgeService.isRunning(bridgeId) ? Status.RUNTIME_STATUS.STARTED : Status.RUNTIME_STATUS.STOPPED;
        Status status = connected ?
                Status.connected(runtimeStatus, ApiConstants.BRIDGE_TYPE, bridgeId) :
                Status.disconnected(runtimeStatus, ApiConstants.BRIDGE_TYPE, bridgeId);
        if (!connected) {
            status.setMessage(getLastErrorInternal(bridgeId));
        }
        return status;
    }

    protected @Nullable String getLastErrorInternal(final @NotNull String bridgeId) {

        Preconditions.checkNotNull(bridgeId);
        Throwable throwable = bridgeService.getLastError(bridgeId);
        return throwable == null ? null : throwable.getMessage();
    }

    protected boolean checkBridgeExists(final @NotNull String bridgeName) {
        Optional<MqttBridge> bridge = configurationService.bridgeConfiguration()
                .getBridges()
                .stream()
                .filter(b -> b.getId().equals(bridgeName))
                .findFirst();
        return bridge.isPresent();
    }

    private @Nullable MqttBridge getBridge(final @NotNull String bridgeName) {
        return configurationService.bridgeConfiguration()
                .getBridges()
                .stream()
                .filter(b -> b.getId().equals(bridgeName))
                .findFirst()
                .orElse(null);
    }


    protected void validateBridge(final @NotNull ApiErrorMessages errorMessages, final @NotNull Bridge bridge) {
        ApiErrorUtils.validateRequiredEntity(errorMessages, "bridge", bridge);
        ApiErrorUtils.validateRequiredFieldRegex(errorMessages, "id", bridge.getId(), HiveMQEdgeConstants.ID_REGEX);
        ApiErrorUtils.validateFieldLengthBetweenIncl(errorMessages,
                "id",
                bridge.getId(),
                1,
                HiveMQEdgeConstants.MAX_ID_LEN);

        if (!bridge.getHost().matches(HiveMQEdgeConstants.IPV4_REGEX) &&
                !bridge.getHost().matches(HiveMQEdgeConstants.IPV6_REGEX) &&
                !bridge.getHost().matches(HiveMQEdgeConstants.HOSTNAME_REGEX)) {
            ApiErrorUtils.addValidationError(errorMessages,
                    "host",
                    "Supplied value does not match ipv4, ipv6 or host format");
        }
        ApiErrorUtils.validateFieldValueBetweenIncl(errorMessages,
                "port",
                bridge.getPort(),
                1,
                HiveMQEdgeConstants.MAX_UINT16);

        if (bridge.getLoopPreventionHopCount() != 0) {
            ApiErrorUtils.validateFieldValueBetweenIncl(errorMessages,
                    "loopPreventionHopCount",
                    bridge.getLoopPreventionHopCount(),
                    1,
                    100);
        }

        bridge.getLocalSubscriptions()
                .forEach(s -> validateValidSubscribeTopicField(errorMessages, "local-filters", s.getFilters()));

        bridge.getRemoteSubscriptions()
                .forEach(s -> validateValidSubscribeTopicField(errorMessages, "remote-filters", s.getFilters()));
    }

    public static void validateValidSubscribeTopicField(
            final @NotNull ApiErrorMessages apiErrorMessages,
            final @NotNull String fieldName,
            final @NotNull List<String> topicFilters) {
        try {
            BridgeConfigurator.validateTopicFilters(fieldName, topicFilters);
        } catch (UnrecoverableException e) {
            ApiErrorUtils.addValidationError(apiErrorMessages,
                    fieldName,
                    "Invalid bridge topic filters for subscribing");
        }
    }


    private static MqttBridge unconvert(final @NotNull Bridge bridge) {

        MqttBridge.Builder builder = new MqttBridge.Builder();
        builder.withCleanStart(bridge.isCleanStart())
                .withHost(bridge.getHost())
                .withId(bridge.getId())
                .withUsername(bridge.getUsername())
                .withPassword(bridge.getPassword())
                .withClientId(bridge.getClientId() != null && !bridge.getClientId().isBlank() ?
                        bridge.getClientId() :
                        "")
                .withKeepAlive(bridge.getKeepAlive())
                .withCleanStart(bridge.isCleanStart())
                .withPort(bridge.getPort())
                .withLoopPreventionEnabled(bridge.isLoopPreventionEnabled())
                .withLoopPreventionHopCount(bridge.getLoopPreventionHopCount() > 0 ?
                        bridge.getLoopPreventionHopCount() :
                        1)
                .withSessionExpiry(bridge.getSessionExpiry())
                .withLocalSubscriptions(bridge.getLocalSubscriptions() != null ?
                        bridge.getLocalSubscriptions()
                                .stream()
                                .map(BridgeResourceImpl::unconvertLocal)
                                .collect(Collectors.toList()) :
                        List.of())
                .withRemoteSubscriptions(bridge.getRemoteSubscriptions() != null ?
                        bridge.getRemoteSubscriptions()
                                .stream()
                                .map(BridgeResourceImpl::unconvertRemote)
                                .collect(Collectors.toList()) :
                        List.of())
                .withBridgeTls(convertTls(bridge.getTlsConfiguration()))
                .withWebsocketConfiguration(convertWebsocketConfig(bridge.getWebsocketConfiguration()))
                .persist(bridge.isPersist());
        return builder.build();
    }


    private static LocalSubscription unconvertLocal(
            final @NotNull Bridge.LocalBridgeSubscription subscription) {
        return new LocalSubscription(subscription.getFilters(),
                subscription.getDestination(),
                subscription.getExcludes() == null ? List.of() : subscription.getExcludes(),
                subscription.getCustomUserProperties() != null ?
                        subscription.getCustomUserProperties()
                                .stream()
                                .map(BridgeResourceImpl::convertProperty)
                                .collect(Collectors.toList()) :
                        List.of(),
                subscription.isPreserveRetain(),
                subscription.getMaxQoS(),
                subscription.getQueueLimit());
    }

    private static RemoteSubscription unconvertRemote(final @NotNull Bridge.BridgeSubscription subscription) {

        return new RemoteSubscription(subscription.getFilters(),
                subscription.getDestination(),
                subscription.getCustomUserProperties() != null ?
                        subscription.getCustomUserProperties()
                                .stream()
                                .map(BridgeResourceImpl::convertProperty)
                                .collect(Collectors.toList()) :
                        List.of(),
                subscription.isPreserveRetain(),
                subscription.getMaxQoS());
    }

    public static CustomUserProperty convertProperty(final @NotNull Bridge.BridgeCustomUserProperty customUserProperty) {
        return CustomUserProperty.of(customUserProperty.getKey(), customUserProperty.getValue());
    }

    public static @Nullable BridgeTls convertTls(final @Nullable TlsConfiguration tls) {
        if (tls == null || !tls.isEnabled()) {
            return null;
        }

        return new BridgeTls(tls.getKeystorePath(),
                tls.getKeystorePassword(),
                tls.getPrivateKeyPassword(),
                tls.getTruststorePath(),
                tls.getTruststorePassword(),
                tls.getProtocols(),
                tls.getCipherSuites(),
                tls.getKeystoreType(),
                tls.getTruststoreType(),
                tls.isVerifyHostname(),
                Math.max(10, tls.getHandshakeTimeout()));
    }

    public static @Nullable BridgeWebsocketConfig convertWebsocketConfig(final @Nullable WebsocketConfiguration websocketConfiguration) {
        if (websocketConfiguration == null || !websocketConfiguration.isEnabled()) {
            return null;
        }

        return new BridgeWebsocketConfig(websocketConfiguration.getServerPath(),
                websocketConfiguration.getSubProtocol());
    }
}
