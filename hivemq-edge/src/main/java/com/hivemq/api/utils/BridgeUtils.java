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
package com.hivemq.api.utils;

import com.hivemq.bridge.config.BridgeTls;
import com.hivemq.bridge.config.BridgeWebsocketConfig;
import com.hivemq.bridge.config.CustomUserProperty;
import com.hivemq.bridge.config.LocalSubscription;
import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.bridge.config.RemoteSubscription;
import com.hivemq.edge.api.model.Bridge;
import com.hivemq.edge.api.model.BridgeCustomUserProperty;
import com.hivemq.edge.api.model.BridgeSubscription;
import com.hivemq.edge.api.model.LocalBridgeSubscription;
import com.hivemq.edge.api.model.Status;
import com.hivemq.edge.api.model.WebsocketConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Collectors;

public class BridgeUtils {

    public static @NotNull Bridge convert(final @NotNull MqttBridge mqttBridge, final @NotNull Status status) {
        return new Bridge().id(mqttBridge.getId())
                .host(mqttBridge.getHost())
                .port(mqttBridge.getPort())
                .clientId(mqttBridge.getClientId())
                .keepAlive(mqttBridge.getKeepAlive())
                .sessionExpiry(mqttBridge.getSessionExpiry())
                .cleanStart(mqttBridge.isCleanStart())
                .username(mqttBridge.getUsername())
                .password(mqttBridge.getPassword())
                .loopPreventionEnabled(mqttBridge.isLoopPreventionEnabled())
                .loopPreventionHopCount(mqttBridge.getLoopPreventionHopCount() < 1 ?
                        0 :
                        mqttBridge.getLoopPreventionHopCount())
                .remoteSubscriptions(mqttBridge.getRemoteSubscriptions()
                        .stream()
                        .map(BridgeUtils::convertRemoteSubscription)
                        .collect(Collectors.toList()))
                .localSubscriptions(mqttBridge.getLocalSubscriptions()
                        .stream()
                        .map(BridgeUtils::convertLocalSubscription)
                        .collect(Collectors.toList()))
                .tlsConfiguration(convertTls(mqttBridge.getBridgeTls()))
                .websocketConfiguration(convertWebsocketConfig(mqttBridge.getBridgeWebsocketConfig()))
                .status(status)
                .persist(mqttBridge.isPersist());
    }

    public static @Nullable LocalBridgeSubscription convertLocalSubscription(final @Nullable LocalSubscription localSubscription) {
        if (localSubscription == null) {
            return null;
        }
        return new LocalBridgeSubscription().filters(localSubscription.getFilters())
                .destination(localSubscription.getDestination())
                .excludes(localSubscription.getExcludes())
                .customUserProperties(localSubscription.getCustomUserProperties()
                        .stream()
                        .map(BridgeUtils::convertProperty)
                        .collect(Collectors.toList()))
                .preserveRetain(localSubscription.isPreserveRetain())
                .maxQoS(LocalBridgeSubscription.MaxQoSEnum.fromValue(localSubscription.getMaxQoS()))
                .queueLimit(localSubscription.getQueueLimit());
    }

    public static @Nullable BridgeSubscription convertRemoteSubscription(final @Nullable RemoteSubscription remoteSubscription) {
        if (remoteSubscription == null) {
            return null;
        }
        return new BridgeSubscription().filters(remoteSubscription.getFilters())
                .destination(remoteSubscription.getDestination())
                .customUserProperties(remoteSubscription.getCustomUserProperties()
                        .stream()
                        .map(BridgeUtils::convertProperty)
                        .collect(Collectors.toList()))
                .preserveRetain(remoteSubscription.isPreserveRetain())
                .maxQoS(BridgeSubscription.MaxQoSEnum.fromValue(remoteSubscription.getMaxQoS()));
    }

    public static @Nullable BridgeCustomUserProperty convertProperty(final @Nullable CustomUserProperty customUserProperty) {
        if (customUserProperty == null) {
            return null;
        }
        return new BridgeCustomUserProperty().key(customUserProperty.getKey()).value(customUserProperty.getValue());

    }

    public static com.hivemq.edge.api.model.@Nullable TlsConfiguration convertTls(final @Nullable BridgeTls tls) {
        if (tls == null) {
            return null;
        }

        return new com.hivemq.edge.api.model.TlsConfiguration().enabled(true)
                .keystorePath(tls.getKeystorePath())
                .keystorePassword(tls.getKeystorePassword())
                .privateKeyPassword(tls.getPrivateKeyPassword())
                .truststorePath(tls.getTruststorePath())
                .truststorePassword(tls.getTruststorePassword())
                .protocols(tls.getProtocols())
                .cipherSuites(tls.getCipherSuites())
                .keystoreType(tls.getKeystoreType())
                .truststoreType(tls.getTruststoreType())
                .verifyHostname(tls.isVerifyHostname())
                .handshakeTimeout(tls.getHandshakeTimeout());
    }

    public static @Nullable WebsocketConfiguration convertWebsocketConfig(
            final @Nullable BridgeWebsocketConfig bridgeWebsocketConfig) {
        if (bridgeWebsocketConfig == null) {
            return null;
        }
        return new WebsocketConfiguration().enabled(true)
                .serverPath(bridgeWebsocketConfig.getPath())
                .subProtocol(bridgeWebsocketConfig.getSubProtocol());
    }
}
