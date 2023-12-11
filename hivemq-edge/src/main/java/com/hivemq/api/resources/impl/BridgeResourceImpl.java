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
import com.hivemq.api.model.core.TlsConfiguration;
import com.hivemq.api.model.status.Status;
import com.hivemq.api.model.status.StatusList;
import com.hivemq.api.model.status.StatusTransitionCommand;
import com.hivemq.api.model.status.StatusTransitionResult;
import com.hivemq.api.resources.BridgeApi;
import com.hivemq.api.utils.ApiErrorUtils;
import com.hivemq.bridge.BridgeService;
import com.hivemq.bridge.config.*;
import com.hivemq.configuration.reader.BridgeConfigurator;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.HiveMQEdgeConstants;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

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
    public Response listBridges() {

        logger.trace("Bridge API listing events at {}", System.currentTimeMillis());
        List<MqttBridge> bridges = configurationService.bridgeConfiguration().getBridges();
        BridgeList list = new BridgeList(bridges.stream()
                .map(m -> Bridge.convert(m, getStatusInternal(m.getId())))
                .collect(Collectors.toList()));
        return Response.status(200).entity(list).build();
    }

    @Override
    public Response addBridge(final @NotNull Bridge bridge) {

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
                return Response.status(200).build();
            } finally {
                executorService.submit(() -> bridgeService.updateBridges());
            }
        }
    }

    @Override
    public Response getBridgeByName(final String bridgeId) {

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
    public Response deleteBridge(final @NotNull String bridgeId) {
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
                return Response.status(200).build();
            } finally {
                bridgeService.updateBridges();
            }
        }
    }

    @Override
    public Response changeStatus(final String bridgeId, final StatusTransitionCommand command) {

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
    public Response updateBridge(final @NotNull String bridgeId, final @NotNull Bridge bridge) {

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
            return Response.status(200).build();
        }
    }

    @Override
    public Response getStatus(final @NotNull String bridgeId) {

        ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        ApiErrorUtils.validateRequiredField(errorMessages, "id", bridgeId, false);
        ApiErrorUtils.validateRequiredFieldRegex(errorMessages, "id", bridgeId, HiveMQEdgeConstants.ID_REGEX);
        if (!checkBridgeExists(bridgeId)) {
            return ApiErrorUtils.notFound(String.format("Bridge not found by id '%s'", bridgeId));
        }
        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ApiErrorUtils.badRequest(errorMessages);
        } else {
            return Response.status(200).entity(getStatusInternal(bridgeId)).build();
        }
    }

    @Override
    public Response status() {
        //-- Bridges
        ImmutableList.Builder<Status> builder = new ImmutableList.Builder<>();
        List<MqttBridge> bridges = configurationService.bridgeConfiguration().getBridges();
        for (MqttBridge bridge : bridges) {
            builder.add(getStatusInternal(bridge.getId()));
        }
        return Response.status(200).entity(new StatusList(builder.build())).build();
    }

    protected Status getStatusInternal(final @NotNull String bridgeId) {

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

    protected String getLastErrorInternal(final @NotNull String bridgeId) {

        Preconditions.checkNotNull(bridgeId);
        Throwable throwable = bridgeService.getLastError(bridgeId);
        return throwable == null ? null : throwable.getMessage();
    }

    protected boolean checkBridgeExists(@NotNull final String bridgeName) {
        Optional<MqttBridge> bridge = configurationService.bridgeConfiguration()
                .getBridges()
                .stream()
                .filter(b -> b.getId().equals(bridgeName))
                .findFirst();
        return bridge.isPresent();
    }

    private @Nullable MqttBridge getBridge(@NotNull final String bridgeName) {
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
                .stream()
                .forEach(s -> validateValidSubscribeTopicField(errorMessages, "local-filters", s.getFilters()));

        bridge.getRemoteSubscriptions()
                .stream()
                .forEach(s -> validateValidSubscribeTopicField(errorMessages, "remote-filters", s.getFilters()));
    }

    public static void validateValidSubscribeTopicField(
            final ApiErrorMessages apiErrorMessages, final String fieldName, final List<String> topicFilters) {
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
                                .map(f -> unconvertLocal(f))
                                .collect(Collectors.toList()) :
                        List.of())
                .withRemoteSubscriptions(bridge.getRemoteSubscriptions() != null ?
                        bridge.getRemoteSubscriptions()
                                .stream()
                                .map(f -> unconvertRemote(f))
                                .collect(Collectors.toList()) :
                        List.of())
                .withBridgeTls(convertTls(bridge.getTlsConfiguration()))
                .persist(bridge.isPersist());
        return builder.build();
    }

    private static LocalSubscription unconvertLocal(final @NotNull Bridge.LocalBridgeSubscription subscription) {

        return new LocalSubscription(subscription.getFilters(),
                subscription.getDestination(),
                subscription.getExcludes() == null ? List.of() : subscription.getExcludes(),
                subscription.getCustomUserProperties() != null ?
                        subscription.getCustomUserProperties()
                                .stream()
                                .map(f -> convertProperty(f))
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
                                .map(f -> convertProperty(f))
                                .collect(Collectors.toList()) :
                        List.of(),
                subscription.isPreserveRetain(),
                subscription.getMaxQoS());
    }

    public static CustomUserProperty convertProperty(final @NotNull Bridge.BridgeCustomUserProperty customUserProperty) {
        if (customUserProperty == null) {
            return null;
        }
        CustomUserProperty property = CustomUserProperty.of(customUserProperty.getKey(), customUserProperty.getValue());
        return property;
    }

    public static BridgeTls convertTls(TlsConfiguration tls) {
        if (tls == null || !tls.isEnabled()) {
            return null;
        }

        BridgeTls tlsConfiguration = new BridgeTls(tls.getKeystorePath(),
                tls.getKeystorePassword(),
                tls.getPrivateKeyPassword(),
                tls.getTruststorePath(),
                tls.getTruststorePassword(),
                tls.getProtocols() != null ? tls.getProtocols() : List.of(),
                tls.getCipherSuites() != null ? tls.getCipherSuites() : List.of(),
                tls.getKeystoreType(),
                tls.getTruststoreType(),
                tls.isVerifyHostname(),
                Math.max(10, tls.getHandshakeTimeout()));
        return tlsConfiguration;
    }
}
