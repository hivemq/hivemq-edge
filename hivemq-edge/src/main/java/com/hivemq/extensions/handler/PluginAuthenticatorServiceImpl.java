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
package com.hivemq.extensions.handler;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.bootstrap.ClientState;
import com.hivemq.bootstrap.netty.ChannelDependencies;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.configuration.service.InternalConfigurations;
import org.jetbrains.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.parameter.AuthenticatorProviderInput;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extensions.ExtensionPriorityComparator;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.auth.AuthConnectInput;
import com.hivemq.extensions.auth.AuthInput;
import com.hivemq.extensions.auth.ConnectAuthConnectTask;
import com.hivemq.extensions.auth.ConnectAuthContext;
import com.hivemq.extensions.auth.ConnectAuthOutput;
import com.hivemq.extensions.auth.ConnectAuthTask;
import com.hivemq.extensions.auth.ConnectSimpleAuthTask;
import com.hivemq.extensions.auth.ReAuthContext;
import com.hivemq.extensions.auth.ReAuthOutput;
import com.hivemq.extensions.auth.ReAuthTask;
import com.hivemq.extensions.auth.parameter.AuthenticatorProviderInputImpl;
import com.hivemq.extensions.auth.parameter.ModifiableClientSettingsImpl;
import com.hivemq.extensions.client.ClientAuthenticators;
import com.hivemq.extensions.client.ClientAuthenticatorsImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.packets.general.ModifiableDefaultPermissionsImpl;
import com.hivemq.extensions.services.auth.Authenticators;
import com.hivemq.extensions.services.auth.WrappedAuthenticatorProvider;
import com.hivemq.mqtt.handler.auth.MqttAuthSender;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.connect.ConnectHandler;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.auth.AUTH;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ReasonStrings;
import dagger.Lazy;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import static com.hivemq.bootstrap.netty.ChannelHandlerNames.AUTH_IN_PROGRESS_MESSAGE_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MQTT_MESSAGE_DECODER;

/**
 * @author Florian Limpöck
 * @author Daniel Krüger
 */
@Singleton
public class PluginAuthenticatorServiceImpl implements PluginAuthenticatorService {

    @VisibleForTesting
    static final String CONNACK_BAD_AUTHENTICATION_METHOD_LOG_STATEMENT = "Client with IP {} sent AUTH packet " +
            "with a different authentication method than in the CONNECT packet. Disconnecting client.";

    @VisibleForTesting
    static final String DISCONNECT_BAD_AUTHENTICATION_METHOD_LOG_STATEMENT = "Client with IP {} sent AUTH packet " +
            "with a different authentication method than in the CONNECT packet. Disconnecting client.";

    public static final String AUTH_FAILED_LOG = "Client with ip {} could not be authenticated";
    public static final String RE_AUTH_FAILED_LOG = "Client with ip {} could not be re-authenticated";

    private final @NotNull Lazy<ConnectHandler> connectHandler;
    private final @NotNull MqttConnacker connacker;
    private final @NotNull MqttServerDisconnector disconnector;
    private final @NotNull MqttAuthSender authSender;
    private final @NotNull Authenticators authenticators;
    private final @NotNull ChannelDependencies channelDependencies;
    private final @NotNull PluginOutPutAsyncer asyncer;
    private final @NotNull PluginTaskExecutorService pluginTaskExecutorService;
    private final @NotNull ServerInformation serverInformation;
    private final @NotNull ExtensionPriorityComparator priorityComparator;
    private final boolean validateUTF8;
    private final int timeout;

    @Inject
    public PluginAuthenticatorServiceImpl(
            final @NotNull Lazy<ConnectHandler> connectHandler,
            final @NotNull MqttConnacker connacker,
            final @NotNull MqttServerDisconnector disconnector,
            final @NotNull MqttAuthSender authSender,
            final @NotNull ConfigurationService configurationService,
            final @NotNull Authenticators authenticators,
            final @NotNull ChannelDependencies channelDependencies,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull PluginTaskExecutorService pluginTaskExecutorService,
            final @NotNull HiveMQExtensions extensions,
            final @NotNull ServerInformation serverInformation) {

        this.connectHandler = connectHandler;
        this.connacker = connacker;
        this.disconnector = disconnector;
        this.authenticators = authenticators;
        this.channelDependencies = channelDependencies;
        this.asyncer = asyncer;
        this.pluginTaskExecutorService = pluginTaskExecutorService;
        this.authSender = authSender;
        this.priorityComparator = new ExtensionPriorityComparator(extensions);
        this.serverInformation = serverInformation;
        this.timeout = InternalConfigurations.AUTH_PROCESS_TIMEOUT_SEC.get();
        this.validateUTF8 = configurationService.securityConfiguration().validateUTF8();
    }

    @Override
    public void authenticateConnect(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull ClientConnection clientConnection,
            final @NotNull CONNECT connect,
            final @NotNull ModifiableClientSettingsImpl clientSettings) {

        final String authMethod = connect.getAuthMethod();
        if (authMethod != null) {
            clientConnection.setAuthMethod(authMethod);
        }

        final ModifiableDefaultPermissions defaultPermissions = new ModifiableDefaultPermissionsImpl();
        clientConnection.setAuthPermissions(defaultPermissions);

        final Map<String, WrappedAuthenticatorProvider> authenticatorProviderMap =
                authenticators.getAuthenticatorProviderMap();
        if (authenticatorProviderMap.isEmpty()) {
            connectHandler.get().connectSuccessfulUndecided(ctx, clientConnection, connect, clientSettings);
            return;
        }

        if (authMethod != null) {
            ctx.pipeline()
                    .addAfter(MQTT_MESSAGE_DECODER, AUTH_IN_PROGRESS_MESSAGE_HANDLER,
                            channelDependencies.getAuthInProgressMessageHandler());
        }

        final AuthenticatorProviderInput authenticatorProviderInput =
                new AuthenticatorProviderInputImpl(serverInformation, ctx.channel(), connect.getClientIdentifier());

        final AuthConnectInput input = new AuthConnectInput(connect, ctx.channel());

        final ClientAuthenticators clientAuthenticators = getClientAuthenticators(ctx);

        final ConnectAuthOutput output = new ConnectAuthOutput(
                asyncer, validateUTF8, defaultPermissions, clientSettings, timeout, authMethod != null);
        final ConnectAuthContext context = new ConnectAuthContext(
                ctx, authSender, authenticatorProviderMap.size(), output, connectHandler.get(), connacker, connect, true);

        // calls the authenticators in the order of the priority of their plugins
        for (final Map.Entry<String, WrappedAuthenticatorProvider> entry : authenticatorProviderMap.entrySet()) {
            final String extensionId = entry.getKey();
            final WrappedAuthenticatorProvider authenticatorProvider = entry.getValue();
            if (!authenticatorProvider.isEnhanced()) {
                final ConnectSimpleAuthTask task =
                        new ConnectSimpleAuthTask(authenticatorProvider, authenticatorProviderInput, extensionId);
                pluginTaskExecutorService.handlePluginInOutTaskExecution(context, input, context, task);
            } else {
                final ConnectAuthConnectTask task = new ConnectAuthConnectTask(
                        authenticatorProvider, authenticatorProviderInput, extensionId, clientAuthenticators);
                pluginTaskExecutorService.handlePluginInOutTaskExecution(context, input, context, task);
            }
        }
    }

    @Override
    public void authenticateAuth(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull ClientConnection clientConnection,
            final @NotNull AUTH auth) {

        final boolean reAuth = clientConnection.getClientState() == ClientState.RE_AUTHENTICATING;

        final String authMethod = auth.getAuthMethod();
        if (!authMethod.equals(clientConnection.getAuthMethod())) {
            badAuthMethodDisconnect(ctx, auth, reAuth);
            return;
        }

        final ScheduledFuture<?> authFuture = clientConnection.getAuthFuture();
        if (authFuture != null) {
            authFuture.cancel(true);
            clientConnection.setAuthFuture(null);
        }

        int enhancedAuthenticatorCount = 0;
        final Map<String, WrappedAuthenticatorProvider> authenticatorProviderMap =
                authenticators.getAuthenticatorProviderMap();
        for (final Map.Entry<String, WrappedAuthenticatorProvider> entry : authenticatorProviderMap.entrySet()) {
            if (entry.getValue().isEnhanced()) {
                enhancedAuthenticatorCount++;
            }
        }
        if (enhancedAuthenticatorCount == 0) {
            noAuthAvailableDisconnect(ctx, reAuth);
            return;
        }

        final String clientId = clientConnection.getClientId();

        final AuthenticatorProviderInput authenticatorProviderInput =
                new AuthenticatorProviderInputImpl(serverInformation, ctx.channel(), clientId);

        final AuthInput input = new AuthInput(clientId, ctx.channel(), auth, reAuth);

        final ModifiableDefaultPermissions defaultPermissions = clientConnection.getAuthPermissions();
        final ModifiableClientSettingsImpl clientSettings = getSettingsFromChannel(ctx.channel());

        final ClientAuthenticators clientAuthenticators = getClientAuthenticators(ctx);

        if (reAuth) {
            final ReAuthOutput output =
                    new ReAuthOutput(asyncer, validateUTF8, defaultPermissions, clientSettings, timeout);
            final ReAuthContext context = new ReAuthContext(
                    clientId, ctx, authSender, enhancedAuthenticatorCount, output, disconnector);

            for (final Map.Entry<String, WrappedAuthenticatorProvider> entry : authenticatorProviderMap.entrySet()) {
                final String extensionId = entry.getKey();
                final WrappedAuthenticatorProvider authenticatorProvider = entry.getValue();
                if (authenticatorProvider.isEnhanced()) {
                    final ReAuthTask task = new ReAuthTask(
                            authenticatorProvider, authenticatorProviderInput, extensionId, clientAuthenticators);
                    pluginTaskExecutorService.handlePluginInOutTaskExecution(context, input, context, task);
                }
            }
        } else {
            final CONNECT connect = clientConnection.getAuthConnect();

            final ConnectAuthOutput output = new ConnectAuthOutput(
                    asyncer, validateUTF8, defaultPermissions, clientSettings, timeout, true);
            final ConnectAuthContext context = new ConnectAuthContext(
                    ctx, authSender, enhancedAuthenticatorCount, output, connectHandler.get(), connacker, connect, false);

            for (final Map.Entry<String, WrappedAuthenticatorProvider> entry : authenticatorProviderMap.entrySet()) {
                final String extensionId = entry.getKey();
                final WrappedAuthenticatorProvider authenticatorProvider = entry.getValue();
                if (authenticatorProvider.isEnhanced()) {
                    final ConnectAuthTask task = new ConnectAuthTask(
                            authenticatorProvider, authenticatorProviderInput, extensionId, clientAuthenticators);
                    pluginTaskExecutorService.handlePluginInOutTaskExecution(context, input, context, task);
                }
            }
        }
    }

    private void badAuthMethodDisconnect(final @NotNull ChannelHandlerContext ctx, final @NotNull AUTH auth, final boolean reAuth) {
        final String reasonString = String.format(ReasonStrings.DISCONNECT_PROTOCOL_ERROR_AUTH_METHOD, auth.getType().name());
        if (reAuth) {
            disconnector.disconnect(
                    ctx.channel(),
                    DISCONNECT_BAD_AUTHENTICATION_METHOD_LOG_STATEMENT,
                    "Different auth method",
                    Mqtt5DisconnectReasonCode.BAD_AUTHENTICATION_METHOD,
                    reasonString,
                    Mqtt5UserProperties.NO_USER_PROPERTIES,
                    true,
                    false);
        } else {
            connacker.connackError(
                    ctx.channel(),
                    CONNACK_BAD_AUTHENTICATION_METHOD_LOG_STATEMENT,
                    "Different auth method",
                    Mqtt5ConnAckReasonCode.BAD_AUTHENTICATION_METHOD,
                    reasonString,
                    Mqtt5UserProperties.NO_USER_PROPERTIES,
                    true);
        }
    }

    private void noAuthAvailableDisconnect(final @NotNull ChannelHandlerContext ctx, final boolean reAuth) {
        if (reAuth) {
            disconnector.disconnect(
                    ctx.channel(),
                    RE_AUTH_FAILED_LOG,
                    ReasonStrings.RE_AUTH_FAILED_NO_AUTHENTICATOR,
                    Mqtt5DisconnectReasonCode.NOT_AUTHORIZED,
                    ReasonStrings.RE_AUTH_FAILED_NO_AUTHENTICATOR,
                    Mqtt5UserProperties.NO_USER_PROPERTIES,
                    true,
                    false);
        } else {
            connacker.connackError(
                    ctx.channel(),
                    PluginAuthenticatorServiceImpl.AUTH_FAILED_LOG,
                    ReasonStrings.AUTH_FAILED_NO_AUTHENTICATOR,
                    Mqtt5ConnAckReasonCode.NOT_AUTHORIZED,
                    ReasonStrings.AUTH_FAILED_NO_AUTHENTICATOR,
                    Mqtt5UserProperties.NO_USER_PROPERTIES,
                    true);
        }
    }

    private @NotNull ModifiableClientSettingsImpl getSettingsFromChannel(final @NotNull Channel channel) {
        final ClientConnection clientConnection = channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
        final Integer receiveMax = clientConnection.getClientReceiveMaximum();
        Preconditions.checkNotNull(receiveMax, "Receive maximum must not be null here");
        final Long queueSizeMaximum = clientConnection.getQueueSizeMaximum();
        return new ModifiableClientSettingsImpl(receiveMax, queueSizeMaximum);
    }

    private @NotNull ClientAuthenticators getClientAuthenticators(final @NotNull ChannelHandlerContext ctx) {
        final ClientConnection clientConnection = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
        if (clientConnection.getExtensionClientAuthenticators() == null) {
            clientConnection.setExtensionClientAuthenticators(new ClientAuthenticatorsImpl(priorityComparator));
        }
        return clientConnection.getExtensionClientAuthenticators();
    }
}
