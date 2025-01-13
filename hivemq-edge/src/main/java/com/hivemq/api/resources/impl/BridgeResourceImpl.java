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
import com.hivemq.api.errors.InvalidQueryParameterErrors;
import com.hivemq.api.errors.bridge.BridgeFailedSchemaValidationError;
import com.hivemq.api.errors.bridge.BridgeNotFoundError;
import com.hivemq.api.model.ApiConstants;
import com.hivemq.api.model.ApiErrorMessages;
import com.hivemq.api.utils.ApiErrorUtils;
import com.hivemq.api.utils.BridgeUtils;
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
import com.hivemq.edge.api.BridgesApi;
import com.hivemq.edge.api.model.Bridge;
import com.hivemq.edge.api.model.BridgeCustomUserProperty;
import com.hivemq.edge.api.model.BridgeList;
import com.hivemq.edge.api.model.BridgeSubscription;
import com.hivemq.edge.api.model.LocalBridgeSubscription;
import com.hivemq.edge.api.model.Status;
import com.hivemq.edge.api.model.StatusList;
import com.hivemq.edge.api.model.StatusTransitionCommand;
import com.hivemq.edge.api.model.StatusTransitionResult;
import com.hivemq.edge.api.model.TlsConfiguration;
import com.hivemq.edge.api.model.WebsocketConfiguration;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.http.error.Error;
import com.hivemq.util.ErrorResponseUtil;
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
public class BridgeResourceImpl extends AbstractApi implements BridgesApi {

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
    public @NotNull Response getBridges() {
        logger.trace("Bridge API listing events at {}", System.currentTimeMillis());
        final List<MqttBridge> bridges = configurationService.bridgeConfiguration().getBridges();
        final BridgeList list = new BridgeList().items(
                bridges.stream()
                        .map(m -> BridgeUtils.convert(m, getStatusInternal(m.getId())))
                        .collect(Collectors.toList()));
        return Response.ok(list).build();
    }


    @Override
    public @NotNull Response addBridge(final @NotNull com.hivemq.edge.api.model.Bridge bridge) {

        final ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        if (checkBridgeExists(bridge.getId())) {
            return ErrorResponseUtil.errorResponse(new BridgeFailedSchemaValidationError(List.of(new Error(
                    "Bridge already existed",
                    "id"))));
        }
        validateBridge(errorMessages, bridge);
        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ErrorResponseUtil.errorResponse(new BridgeFailedSchemaValidationError(errorMessages.toErrorList()));
        } else {
            try {
                final MqttBridge mqttBridge = unconvert(bridge);
                configurationService.bridgeConfiguration().addBridge(mqttBridge);
                return Response.ok().build();
            } finally {
                executorService.submit(bridgeService::updateBridges);
            }
        }
    }

    @Override
    public @NotNull Response getBridgeByName(final @NotNull String bridgeId) {

        final ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        ApiErrorUtils.validateRequiredField(errorMessages, "id", bridgeId, false);
        ApiErrorUtils.validateRequiredFieldRegex(errorMessages, "id", bridgeId, HiveMQEdgeConstants.ID_REGEX);
        if (!checkBridgeExists(bridgeId)) {
            return ErrorResponseUtil.errorResponse(new BridgeNotFoundError(String.format("Bridge not found by id '%s'",
                    bridgeId)));
        }
        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ErrorResponseUtil.errorResponse(new InvalidQueryParameterErrors(errorMessages.toErrorList()));
        } else {
            final Optional<MqttBridge> bridge = configurationService.bridgeConfiguration()
                    .getBridges()
                    .stream()
                    .filter(b -> b.getId().equals(bridgeId))
                    .findFirst();
            if (bridge.isPresent()) {
                final MqttBridge mqttBridge = bridge.get();
                return Response.ok(BridgeUtils.convert(mqttBridge, getStatusInternal(bridgeId))).build();
            } else {
                return ErrorResponseUtil.errorResponse(new BridgeNotFoundError(String.format(
                        "Bridge not found by id '%s'",
                        bridgeId)));
            }
        }
    }

    @Override
    public @NotNull Response removeBridge(final @NotNull String bridgeId) {
        final ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        ApiErrorUtils.validateRequiredField(errorMessages, "id", bridgeId, false);
        ApiErrorUtils.validateRequiredFieldRegex(errorMessages, "id", bridgeId, HiveMQEdgeConstants.ID_REGEX);
        if (!checkBridgeExists(bridgeId)) {
            return ErrorResponseUtil.errorResponse(new BridgeNotFoundError(String.format("Bridge not found by id '%s'",
                    bridgeId)));
        }
        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ErrorResponseUtil.errorResponse(new InvalidQueryParameterErrors(errorMessages.toErrorList()));
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
    public @NotNull Response transitionBridgeStatus(
            final @NotNull String bridgeId, final @NotNull StatusTransitionCommand command) {

        final ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        ApiErrorUtils.validateRequiredField(errorMessages, "id", bridgeId, false);
        ApiErrorUtils.validateRequiredFieldRegex(errorMessages, "id", bridgeId, HiveMQEdgeConstants.ID_REGEX);
        ApiErrorUtils.validateRequiredEntity(errorMessages, "command", command);
        if (!checkBridgeExists(bridgeId)) {
            return ErrorResponseUtil.errorResponse(new BridgeNotFoundError(String.format("Bridge not found by id '%s'",
                    bridgeId)));
        }
        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ErrorResponseUtil.errorResponse(new InvalidQueryParameterErrors(errorMessages.toErrorList()));
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

            return Response.ok(new StatusTransitionResult().status(StatusTransitionResult.StatusEnum.PENDING)
                    .type(ApiConstants.BRIDGE_TYPE)
                    .identifier(bridgeId)
                    .callbackTimeoutMillis(ApiConstants.DEFAULT_TRANSITION_WAIT_TIMEOUT)).build();

        }
    }

    @Override
    public @NotNull Response updateBridge(
            final @NotNull String bridgeId,
            final @NotNull com.hivemq.edge.api.model.Bridge bridge) {

        final ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        validateBridge(errorMessages, bridge);
        if (!bridgeId.equals(bridge.getId())) {
            ApiErrorUtils.addValidationError(errorMessages,
                    "id",
                    "Unable to change the id of a bridge, this field is immutable");
        }

        final MqttBridge previousBridgeConfig = getBridge(bridgeId);
        if (previousBridgeConfig == null) {
            return ErrorResponseUtil.errorResponse(new BridgeNotFoundError(String.format("Bridge not found by id '%s'",
                    bridgeId)));
        }
        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ErrorResponseUtil.errorResponse(new InvalidQueryParameterErrors(errorMessages.toErrorList()));
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
    public @NotNull Response getBridgeStatus(final @NotNull String bridgeId) {

        final ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        ApiErrorUtils.validateRequiredField(errorMessages, "id", bridgeId, false);
        ApiErrorUtils.validateRequiredFieldRegex(errorMessages, "id", bridgeId, HiveMQEdgeConstants.ID_REGEX);
        if (!checkBridgeExists(bridgeId)) {
            return ErrorResponseUtil.errorResponse(new BridgeNotFoundError(String.format("Bridge not found by id '%s'",
                    bridgeId)));
        }
        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ErrorResponseUtil.errorResponse(new InvalidQueryParameterErrors(errorMessages.toErrorList()));
        } else {
            return Response.ok(getStatusInternal(bridgeId)).build();
        }
    }

    @Override
    public @NotNull Response getBridgesStatus() {
        //-- Bridges
        final ImmutableList.Builder<Status> builder = new ImmutableList.Builder<>();
        final List<MqttBridge> bridges = configurationService.bridgeConfiguration().getBridges();
        for (final MqttBridge bridge : bridges) {
            builder.add(getStatusInternal(bridge.getId()));
        }
        return Response.ok(new StatusList().items(builder.build())).build();
    }

    protected @NotNull Status getStatusInternal(final @NotNull String bridgeId) {

        Preconditions.checkNotNull(bridgeId);
        final boolean connected = bridgeService.isConnected(bridgeId);
        final Status.RuntimeEnum runtimeStatus =
                bridgeService.isRunning(bridgeId) ? Status.RuntimeEnum.STARTED : Status.RuntimeEnum.STOPPED;
        final Status status = connected ?
                new Status().connection(Status.ConnectionEnum.CONNECTED)
                        .runtime(runtimeStatus)
                        .type(ApiConstants.BRIDGE_TYPE)
                        .id(bridgeId) :
                new Status().connection(Status.ConnectionEnum.DISCONNECTED)
                        .runtime(runtimeStatus)
                        .type(ApiConstants.BRIDGE_TYPE)
                        .id(bridgeId);


        if (!connected) {
            status.setMessage(getLastErrorInternal(bridgeId));
        }
        return status;
    }

    protected @Nullable String getLastErrorInternal(final @NotNull String bridgeId) {

        Preconditions.checkNotNull(bridgeId);
        final Throwable throwable = bridgeService.getLastError(bridgeId);
        return throwable == null ? null : throwable.getMessage();
    }

    protected boolean checkBridgeExists(final @NotNull String bridgeName) {
        final Optional<MqttBridge> bridge = configurationService.bridgeConfiguration()
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
        } catch (final UnrecoverableException e) {
            ApiErrorUtils.addValidationError(apiErrorMessages,
                    fieldName,
                    "Invalid bridge topic filters for subscribing");
        }
    }


    private static MqttBridge unconvert(final @NotNull Bridge bridge) {

        final MqttBridge.Builder builder = new MqttBridge.Builder();
        builder.withCleanStart(bridge.getCleanStart())
                .withHost(bridge.getHost())
                .withId(bridge.getId())
                .withUsername(bridge.getUsername())
                .withPassword(bridge.getPassword())
                .withClientId(bridge.getClientId() != null && !bridge.getClientId().isBlank() ?
                        bridge.getClientId() :
                        "")
                .withKeepAlive(bridge.getKeepAlive())
                .withCleanStart(bridge.getCleanStart())
                .withPort(bridge.getPort())
                .withLoopPreventionEnabled(bridge.getLoopPreventionEnabled())
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
                .persist(bridge.getPersist() == null || bridge.getPersist());
        return builder.build();
    }


    private static LocalSubscription unconvertLocal(
            final @NotNull LocalBridgeSubscription subscription) {
        return new LocalSubscription(subscription.getFilters(),
                subscription.getDestination(),
                subscription.getExcludes() == null ? List.of() : subscription.getExcludes(),
                subscription.getCustomUserProperties() != null ?
                        subscription.getCustomUserProperties()
                                .stream()
                                .map(BridgeResourceImpl::convertProperty)
                                .collect(Collectors.toList()) :
                        List.of(),
                subscription.getPreserveRetain(),
                subscription.getMaxQoS().value(),
                subscription.getQueueLimit());
    }

    private static RemoteSubscription unconvertRemote(final @NotNull BridgeSubscription subscription) {

        return new RemoteSubscription(subscription.getFilters(),
                subscription.getDestination(),
                subscription.getCustomUserProperties() != null ?
                        subscription.getCustomUserProperties()
                                .stream()
                                .map(BridgeResourceImpl::convertProperty)
                                .collect(Collectors.toList()) :
                        List.of(),
                subscription.getPreserveRetain(),
                subscription.getMaxQoS().value());
    }

    public static CustomUserProperty convertProperty(final @NotNull BridgeCustomUserProperty customUserProperty) {
        return CustomUserProperty.of(customUserProperty.getKey(), customUserProperty.getValue());
    }

    public static @Nullable BridgeTls convertTls(final @Nullable TlsConfiguration tls) {
        if (tls == null || !tls.getEnabled()) {
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
                tls.getVerifyHostname(),
                Math.max(10, tls.getHandshakeTimeout()));
    }

    public static @Nullable BridgeWebsocketConfig convertWebsocketConfig(final @Nullable WebsocketConfiguration websocketConfiguration) {
        if (websocketConfiguration == null || !websocketConfiguration.getEnabled()) {
            return null;
        }

        return new BridgeWebsocketConfig(websocketConfiguration.getServerPath(),
                websocketConfiguration.getSubProtocol());
    }
}
