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
package com.hivemq.api.model.bridge;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.api.model.core.TlsConfiguration;
import com.hivemq.api.model.status.Status;
import com.hivemq.bridge.config.BridgeTls;
import com.hivemq.bridge.config.CustomUserProperty;
import com.hivemq.bridge.config.LocalSubscription;
import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.bridge.config.RemoteSubscription;
import com.hivemq.edge.HiveMQEdgeConstants;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Bean to transport Bridge details across the API
 *
 * @author Simon L Johnson
 */
public class Bridge {

    @JsonProperty("id")
    @Schema(name = "id",
            description = "The bridge id, must be unique and only contain alpha numeric characters with spaces and hyphens.",
            format = "string",
            minLength = 1,
            required = true,
            maxLength = HiveMQEdgeConstants.MAX_ID_LEN,
            pattern = HiveMQEdgeConstants.ID_REGEX)
    private final @NotNull String id;

    @JsonProperty("host")
    @Schema(name = "host",
            description = "The host the bridge connects to - a well formed hostname, ipv4 or ipv6 value.",
            required = true,
            maxLength = 255)
    private final @NotNull String host;

    @JsonProperty("port")
    @Schema(name = "port",
            description = "The port number to connect to",
            required = true,
            minimum = "1",
            maximum = HiveMQEdgeConstants.MAX_UINT16_String)
    private final int port;

    @JsonProperty("clientId")
    @Schema(name = "clientId",
            description = "The client identifier associated the the MQTT connection.",
            format = "string",
            example = "my-example-client-id",
            nullable = true,
            maxLength = HiveMQEdgeConstants.MAX_UINT16)
    private final @NotNull String clientId;

    @JsonProperty("keepAlive")
    @Schema(name = "keepAlive",
            description = "The keepAlive associated the the MQTT connection.",
            required = true,
            defaultValue = "240",
            minimum = "0",
            maximum = HiveMQEdgeConstants.MAX_UINT16_String,
            format = "integer")
    private final int keepAlive;

    @JsonProperty("sessionExpiry")
    @Schema(name = "sessionExpiry",
            description = "The sessionExpiry associated the the MQTT connection.",
            required = true,
            defaultValue = "3600",
            minimum = "0",
            maximum = "4294967295",
            format = "integer")
    private final int sessionExpiry;

    @JsonProperty("cleanStart")
    @Schema(name = "cleanStart",
            description = "The cleanStart value associated the the MQTT connection.",
            required = true,
            defaultValue = "true",
            format = "boolean")
    private final boolean cleanStart;

    @JsonProperty("username")
    @Schema(name = "username",
            description = "The username value associated the the MQTT connection.",
            maxLength = HiveMQEdgeConstants.MAX_UINT16,
            format = "string",
            nullable = true)
    private final @Nullable String username;

    @JsonProperty("password")
    @Schema(name = "password",
            description = "The password value associated the the MQTT connection.",
            maxLength = HiveMQEdgeConstants.MAX_UINT16,
            format = "string",
            nullable = true)
    private final @Nullable String password;

    @JsonProperty("loopPreventionEnabled")
    @Schema(description = "Is loop prevention enabled on the connection", defaultValue = "true", format = "boolean")
    private final boolean loopPreventionEnabled;

    @JsonProperty("loopPreventionHopCount")
    @Schema(description = "Loop prevention hop count",
            defaultValue = "1",
            minimum = "0",
            maximum = "100",
            format = "integer")
    private final int loopPreventionHopCount;

    @JsonProperty("remoteSubscriptions")
    @Schema(description = "remoteSubscriptions associated with the bridge")
    private final @NotNull List<BridgeSubscription> remoteSubscriptions;

    @JsonProperty("localSubscriptions")
    @Schema(description = "localSubscriptions associated with the bridge")
    private final @NotNull List<LocalBridgeSubscription> localSubscriptions;

    @JsonProperty("tlsConfiguration")
    @Schema(description = "tlsConfiguration associated with the bridge", nullable = true)
    private final @Nullable TlsConfiguration tlsConfiguration;

    @JsonProperty("status")
    @Schema(description = "status associated with the bridge", nullable = true)
    private final @Nullable Status status;

    @JsonProperty("persist")
    @Schema(description = "If this flag is set to true, any outgoing mqtt messages with QoS-1 or QoS-2 will be persisted on disc in case disc persistence is active." +
                          "If this flag is set to false, the QoS of any outgoing mqtt messages will be set to QoS-0 and no traffic will be persisted on disc.", nullable = true)
    private boolean persist = true;


    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Bridge(
            @NotNull @JsonProperty("id") final String id,
            @NotNull @JsonProperty("host") final String host,
            @NotNull @JsonProperty("port") final int port,
            @NotNull @JsonProperty("clientId") final String clientId,
            @NotNull @JsonProperty("keepAlive") final int keepAlive,
            @NotNull @JsonProperty("sessionExpiry") final int sessionExpiry,
            @NotNull @JsonProperty("cleanStart") final boolean cleanStart,
            @Nullable @JsonProperty("username") final String username,
            @Nullable @JsonProperty("password") final String password,
            @NotNull @JsonProperty("loopPreventionEnabled") final boolean loopPreventionEnabled,
            @NotNull @JsonProperty("loopPreventionHopCount") final int loopPreventionHopCount,
            @NotNull @JsonProperty("remoteSubscriptions") final List<BridgeSubscription> remoteSubscriptions,
            @NotNull @JsonProperty("localSubscriptions") final List<LocalBridgeSubscription> localSubscriptions,
            @Nullable @JsonProperty("tlsConfiguration") final TlsConfiguration tlsConfiguration,
            @Nullable @JsonProperty("status") final Status status,
            @Nullable @JsonProperty("persist") final Boolean persist) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.clientId = clientId;
        this.keepAlive = keepAlive;
        this.sessionExpiry = sessionExpiry;
        this.cleanStart = cleanStart;
        this.username = username;
        this.password = password;
        this.loopPreventionEnabled = loopPreventionEnabled;
        this.loopPreventionHopCount = loopPreventionHopCount;
        this.remoteSubscriptions = remoteSubscriptions;
        this.localSubscriptions = localSubscriptions;
        this.tlsConfiguration = tlsConfiguration;
        this.status = status;
        this.persist = persist != null ? persist : true; // true is default
    }

    public Status getStatus() {
        return status;
    }

    public String getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getClientId() {
        return clientId;
    }

    public int getKeepAlive() {
        return keepAlive;
    }

    public int getSessionExpiry() {
        return sessionExpiry;
    }

    public boolean isCleanStart() {
        return cleanStart;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isLoopPreventionEnabled() {
        return loopPreventionEnabled;
    }

    public int getLoopPreventionHopCount() {
        return loopPreventionHopCount;
    }

    public List<BridgeSubscription> getRemoteSubscriptions() {
        return remoteSubscriptions;
    }

    public List<LocalBridgeSubscription> getLocalSubscriptions() {
        return localSubscriptions;
    }

    public TlsConfiguration getTlsConfiguration() {
        return tlsConfiguration;
    }

    public boolean isPersist() {
        return persist;
    }

    public static class BridgeSubscription {

        @JsonProperty("filters")
        @Schema(name = "filters",
                description = "The filters for this subscription.",
                required = true,
                example = "some/topic/value")
        private final @NotNull List<String> filters;

        @JsonProperty("customUserProperties")
        @Schema(description = "The customUserProperties for this subscription")
        private final @NotNull List<BridgeCustomUserProperty> customUserProperties;

        @JsonProperty("preserveRetain")
        @Schema(description = "The preserveRetain for this subscription")
        private final boolean preserveRetain;

        @JsonProperty("maxQoS")
        @Schema(name = "maxQoS",
                description = "The maxQoS for this subscription.",
                format = "number",
                required = true,
                defaultValue = "0",
                allowableValues = {"0", "1", "2"},
                minimum = "0",
                maximum = "2")
        private final int maxQoS;

        @JsonProperty("destination")
        @Schema(name = "destination",
                description = "The destination topic for this filter set.",
                required = true,
                example = "some/topic/value")
        private final @NotNull String destination;


        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public BridgeSubscription(
                @NotNull @JsonProperty("filters") final List<String> filters,
                @NotNull @JsonProperty("destination") final String destination,
                @NotNull @JsonProperty("customUserProperties") final List<BridgeCustomUserProperty> customUserProperties,
                @JsonProperty("preserveRetain") final boolean preserveRetain,
                @JsonProperty("maxQoS") final int maxQoS) {
            this.filters = filters;
            this.destination = destination;
            this.customUserProperties = customUserProperties;
            this.preserveRetain = preserveRetain;
            this.maxQoS = maxQoS;
        }

        public String getDestination() {
            return destination;
        }

        public @NotNull List<String> getFilters() {
            return filters;
        }


        public @NotNull List<BridgeCustomUserProperty> getCustomUserProperties() {
            return customUserProperties;
        }

        public boolean isPreserveRetain() {
            return preserveRetain;
        }

        public int getMaxQoS() {
            return maxQoS;
        }
    }

    public static class LocalBridgeSubscription extends BridgeSubscription {

        @JsonProperty("excludes")
        @Schema(description = "The exclusion patterns", nullable = true)
        private final @Nullable List<String> excludes;

        @JsonProperty("queueLimit")
        @Schema(description = "The limit of this bridge for QoS-1 and QoS-2 messages.", nullable = true)
        private @Nullable Long queueLimit;

        public LocalBridgeSubscription(
                @NotNull @JsonProperty("filters") final List<String> filters,
                @NotNull @JsonProperty("destination") final String destination,
                @Nullable @JsonProperty("excludes") final List<String> excludes,
                @NotNull @JsonProperty("customUserProperties") final List<BridgeCustomUserProperty> customUserProperties,
                @JsonProperty("preserveRetain") final boolean preserveRetain,
                @JsonProperty("maxQoS") final int maxQoS,
                @JsonProperty("queueLimit") final @Nullable Long queueLimit) {
            super(filters, destination, customUserProperties, preserveRetain, maxQoS);
            this.excludes = excludes;
            this.queueLimit = queueLimit;
        }

        public @Nullable List<String> getExcludes() {
            return excludes;
        }

        public @Nullable Long getQueueLimit() {
            return queueLimit;
        }
    }

    public static class BridgeCustomUserProperty {

        @JsonProperty("key")
        @Schema(description = "The key the from the property", required = true, format = "string")
        private final @NotNull String key;

        @JsonProperty("value")
        @Schema(description = "The value the from the property", required = true, format = "string")
        private final @NotNull String value;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public BridgeCustomUserProperty(
                @NotNull @JsonProperty("key") final String key, @NotNull @JsonProperty("value") final String value) {
            this.key = key;
            this.value = value;
        }

        public @NotNull String getKey() {
            return key;
        }

        public @NotNull String getValue() {
            return value;
        }
    }

    public static Bridge convert(MqttBridge mqttBridge, Status status) {

        Bridge bridge = new Bridge(mqttBridge.getId(),
                mqttBridge.getHost(),
                mqttBridge.getPort(),
                mqttBridge.getClientId(),
                mqttBridge.getKeepAlive(),
                mqttBridge.getSessionExpiry(),
                mqttBridge.isCleanStart(),
                mqttBridge.getUsername(),
                mqttBridge.getPassword(),
                mqttBridge.isLoopPreventionEnabled(),
                mqttBridge.getLoopPreventionHopCount() < 1 ? 0 : mqttBridge.getLoopPreventionHopCount(),
                mqttBridge.getRemoteSubscriptions()
                        .stream()
                        .map(Bridge::convertRemoteSubscription)
                        .collect(Collectors.toList()),
                mqttBridge.getLocalSubscriptions()
                        .stream()
                        .map(Bridge::convertLocalSubscription)
                        .collect(Collectors.toList()),
                convertTls(mqttBridge.getBridgeTls()),
                status,
                mqttBridge.isPersist());
        return bridge;
    }

    public static @NotNull LocalBridgeSubscription convertLocalSubscription(final @Nullable LocalSubscription localSubscription) {
        if (localSubscription == null) {
            return null;
        }
        return new LocalBridgeSubscription(localSubscription.getFilters(),
                localSubscription.getDestination(),
                localSubscription.getExcludes(),
                localSubscription.getCustomUserProperties()
                        .stream()
                        .map(Bridge::convertProperty)
                        .collect(Collectors.toList()),
                localSubscription.isPreserveRetain(),
                localSubscription.getMaxQoS(),
                localSubscription.getQueueLimit());
    }

    public static BridgeSubscription convertRemoteSubscription(RemoteSubscription remoteSubscription) {
        if (remoteSubscription == null) {
            return null;
        }
        BridgeSubscription subscription = new BridgeSubscription(remoteSubscription.getFilters(),
                remoteSubscription.getDestination(),
                remoteSubscription.getCustomUserProperties()
                        .stream()
                        .map(Bridge::convertProperty)
                        .collect(Collectors.toList()),
                remoteSubscription.isPreserveRetain(),
                remoteSubscription.getMaxQoS());
        return subscription;
    }

    public static BridgeCustomUserProperty convertProperty(CustomUserProperty customUserProperty) {
        if (customUserProperty == null) {
            return null;
        }
        BridgeCustomUserProperty property =
                new BridgeCustomUserProperty(customUserProperty.getKey(), customUserProperty.getValue());
        return property;
    }

    public static TlsConfiguration convertTls(BridgeTls tls) {
        if (tls == null) {
            return null;
        }
        TlsConfiguration tlsConfiguration = new TlsConfiguration(true,
                tls.getKeystorePath(),
                tls.getKeystorePassword(),
                tls.getPrivateKeyPassword(),
                tls.getTruststorePath(),
                tls.getTruststorePassword(),
                tls.getProtocols(),
                tls.getCipherSuites(),
                tls.getKeystoreType(),
                tls.getTruststoreType(),
                tls.isVerifyHostname(),
                tls.getHandshakeTimeout());
        return tlsConfiguration;
    }
}
