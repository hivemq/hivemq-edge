/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.edge.modules.adapters.annotations.ModuleConfigField;
import com.hivemq.edge.modules.config.impl.AbstractProtocolAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;

import java.util.ArrayList;
import java.util.List;

public class OpcUaAdapterConfig extends AbstractProtocolAdapterConfig {

    @JsonProperty("uri")
    @ModuleConfigField(title = "OPC-UA Server URI",
                       description = "URI of the OPC-UA server to connect to",
                       format = ModuleConfigField.FieldType.URI,
                       required = true)
    private @NotNull String uri;

    @JsonProperty("overrideUri")
    @ModuleConfigField(title = "Override server returned endpoint URI",
                       description = "Overrides the endpoint URI returned from the OPC-UA server with the hostname and port from the specified URI.",
                       format = ModuleConfigField.FieldType.BOOLEAN,
                       defaultValue = "false",
                       required = false)
    private @NotNull Boolean overrideUri = false;

    @JsonProperty("subscriptions")
    private @NotNull List<Subscription> subscriptions = new ArrayList<>();

    @JsonProperty("auth")
    private @NotNull Auth auth = new Auth(null, null);

    @JsonProperty("tls")
    private @Nullable Tls tls = new Tls();

    @JsonProperty("security")
    private @NotNull Security security = new Security(SecPolicy.DEFAULT);

    public OpcUaAdapterConfig() {
    }

    public OpcUaAdapterConfig(
            final @NotNull String id, final @NotNull String uri) {
        this.id = id;
        this.uri = uri;
    }

    public @NotNull String getUri() {
        return uri;
    }

    public void setUri(final @NotNull String uri) {
        this.uri = uri;
    }

    public @NotNull List<Subscription> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(final @NotNull List<Subscription> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public @NotNull Auth getAuth() {
        return auth;
    }

    public void setAuth(final @NotNull Auth auth) {
        this.auth = auth;
    }

    public @Nullable Tls getTls() {
        return tls;
    }

    public void setTls(final @NotNull Tls tls) {
        this.tls = tls;
    }

    public @NotNull Security getSecurity() {
        return security;
    }

    public void setSecurity(final @NotNull Security security) {
        this.security = security;
    }

    public @NotNull Boolean getOverrideUri() {
        return overrideUri;
    }

    @Override
    public @NotNull String toString() {
        return "OpcUaAdapterConfig{" +
                "id='" +
                id +
                '\'' +
                ", uri='" +
                uri +
                '\'' +
                ", subscriptions=" +
                subscriptions +
                ", auth=" +
                auth +
                '}';
    }

    public static class Subscription {

        @JsonProperty("node")
        @ModuleConfigField(title = "Source Node ID",
                           description = "identifier of the node on the OPC-UA server. Example: \"ns=3;s=85/0:Temperature\"",
                           required = true)
        private @NotNull String node = "";

        @JsonProperty("mqtt-topic")
        @ModuleConfigField(title = "Destination MQTT topic",
                           description = "The MQTT topic to publish to",
                           format = ModuleConfigField.FieldType.MQTT_TOPIC,
                           required = true)
        private @NotNull String mqttTopic;

        @JsonProperty("publishing-interval")
        @ModuleConfigField(title = "OPC UA publishing interval [ms]",
                           description = "OPC UA publishing interval in milliseconds for this subscription on the server",
                           numberMin = 1,
                           defaultValue = "1000")
        private int publishingInterval = DEFAULT_POLLING_INTERVAL; //1 second

        @JsonProperty("server-queue-size")
        @ModuleConfigField(title = "OPC UA server queue size",
                           description = "OPC UA queue size for this subscription on the server",
                           numberMin = 1,
                           defaultValue = "1")
        private int serverQueueSize = 1;

        @JsonProperty("qos")
        @ModuleConfigField(title = "MQTT QoS",
                           description = "MQTT quality of service level",
                           numberMin = 0,
                           numberMax = 2,
                           defaultValue = "0")
        private int qos = 0;

        @JsonProperty("message-expiry-interval")
        @ModuleConfigField(title = "MQTT message expiry interval [s]",
                           description = "Time in seconds until a MQTT message expires",
                           numberMin = 1,
                           numberMax = 4294967295L)
        private @Nullable Integer messageExpiryInterval;

        public Subscription() {
        }

        public Subscription(
                final @NotNull String node, final @NotNull String mqttTopic) {
            this.node = node;
            this.mqttTopic = mqttTopic;
        }

        public @NotNull String getNode() {
            return node;
        }

        public @NotNull String getMqttTopic() {
            return mqttTopic;
        }

        public int getPublishingInterval() {
            return publishingInterval;
        }

        public int getServerQueueSize() {
            return serverQueueSize;
        }

        public int getQos() {
            return qos;
        }

        public @Nullable Integer getMessageExpiryInterval() {
            return messageExpiryInterval;
        }

        @Override
        public @NotNull String toString() {
            return "Subscription{" + "node=" + node + ", mqttTopic='" + mqttTopic + '\'' + '}';
        }
    }

    public static class Auth {

        @JsonProperty("basic")
        @ModuleConfigField(title = "Basic Authentication", description = "Username / password based authentication")
        private @Nullable BasicAuth basicAuth = null;

        @JsonProperty("x509")
        @ModuleConfigField(title = "X509 Authentication",
                           description = "Authentication based on certificate / private key")
        private @Nullable X509Auth x509Auth = null;

        public Auth() {
        }

        public Auth(final @Nullable BasicAuth basicAuth, final @Nullable X509Auth x509Auth) {
            this.basicAuth = basicAuth;
            this.x509Auth = x509Auth;
        }

        public @Nullable BasicAuth getBasicAuth() {
            return basicAuth;
        }

        public @Nullable X509Auth getX509Auth() {
            return x509Auth;
        }
    }

    public static class BasicAuth {
        @JsonProperty("username")
        @ModuleConfigField(title = "Username", description = "Username for basic authentication")
        private @Nullable String username = null;

        @JsonProperty("password")
        @ModuleConfigField(title = "Password", description = "Password for basic authentication")
        private @Nullable String password = null;

        public BasicAuth() {
        }

        public BasicAuth(@Nullable final String username, @Nullable final String password) {
            this.username = username;
            this.password = password;
        }

        public @Nullable String getUsername() {
            return username;
        }

        public @Nullable String getPassword() {
            return password;
        }
    }

    public static class X509Auth {

        private final boolean enabled;

        public X509Auth() {
            this.enabled = true;
        }

        public boolean isEnabled() {
            return enabled;
        }
    }

    public static class Tls {

        @JsonProperty("enabled")
        @ModuleConfigField(title = "Enable TLS", description = "Enables TLS encrypted connection")
        private boolean enabled = false;

        @JsonProperty("keystore")
        @ModuleConfigField(title = "Keystore",
                           description = "Keystore that contains the client certificate including the chain. Required for X509 authentication.")
        private @Nullable Keystore keystore = null;

        @JsonProperty("truststore")
        @ModuleConfigField(title = "Truststore",
                           description = "Truststore wich contains the trusted server certificates or trusted intermediates.")
        private @Nullable Truststore truststore = null;

        public Tls() {
        }

        public Tls(final boolean enabled, @Nullable final Keystore keystore, final @Nullable Truststore truststore) {
            this.enabled = enabled;
            this.keystore = keystore;
            this.truststore = truststore;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public @Nullable Keystore getKeystore() {
            return keystore;
        }

        public @Nullable Truststore getTruststore() {
            return truststore;
        }
    }

    public static class Keystore {

        @JsonProperty("path")
        @ModuleConfigField(title = "Keystore path", description = "Path on the local file system to the keystore.")
        private @NotNull String path = "";

        @JsonProperty("password")
        @ModuleConfigField(title = "Keystore password", description = "Password to open the keystore.")

        private @NotNull String password = "";

        @JsonProperty("private-key-password")
        @ModuleConfigField(title = "Private key password", description = "Password to access the private key.")

        private @NotNull String privateKeyPassword = "";

        public Keystore() {
        }

        public Keystore(
                @NotNull final String path, @NotNull final String password, @NotNull final String privateKeyPassword) {
            this.path = path;
            this.password = password;
            this.privateKeyPassword = privateKeyPassword;
        }

        public @Nullable String getPath() {
            return path;
        }

        public @Nullable String getPassword() {
            return password;
        }

        public @Nullable String getPrivateKeyPassword() {
            return privateKeyPassword;
        }
    }

    public static class Truststore {

        @JsonProperty("path")
        @ModuleConfigField(title = "Truststore path", description = "Path on the local file system to the truststore.")
        private @NotNull String path = "";

        @JsonProperty("password")
        @ModuleConfigField(title = "Truststore password", description = "Password to open the truststore.")
        private @NotNull String password = "";

        public Truststore() {
        }

        public Truststore(@NotNull final String path, @NotNull final String password) {
            this.path = path;
            this.password = password;
        }

        public @Nullable String getPath() {
            return path;
        }

        public @Nullable String getPassword() {
            return password;
        }
    }

    public static class Security {

        @JsonProperty("policy")
        @ModuleConfigField(title = "OPC UA security policy",
                           description = "Security policy to use for communication with the server.")
        private @NotNull SecPolicy policy = SecPolicy.DEFAULT;

        public Security() {
        }

        public Security(@NotNull final SecPolicy policy) {
            this.policy = policy;
        }

        public @NotNull SecPolicy getPolicy() {
            return policy;
        }
    }

    public enum PayloadMode {
        STRING,
        JSON
    }

    public enum SecPolicy {
        NONE(1, SecurityPolicy.None),
        BASIC128RSA15(2, SecurityPolicy.Basic128Rsa15), //deprecated in spec, but may still be in use
        BASIC256(3, SecurityPolicy.Basic256), //deprecated in spec, but may still be in use
        BASIC256SHA256(4, SecurityPolicy.Basic256Sha256),
        AES128_SHA256_RSAOAEP(5, SecurityPolicy.Aes128_Sha256_RsaOaep),
        AES256_SHA256_RSAPSS(6, SecurityPolicy.Aes256_Sha256_RsaPss);

        SecPolicy(final int priority, final @NotNull SecurityPolicy securityPolicy) {
            this.priority = priority;
            this.securityPolicy = securityPolicy;
        }

        public static final SecPolicy DEFAULT = NONE;

        //higher is better
        private final int priority;
        private final @NotNull SecurityPolicy securityPolicy;

        public static @Nullable SecPolicy forUri(@NotNull String securityPolicyUri) {
            for (SecPolicy value : values()) {
                if (value.getSecurityPolicy().getUri().equals(securityPolicyUri)) {
                    return value;
                }
            }
            return null;
        }

        public int getPriority() {
            return priority;
        }

        public @NotNull SecurityPolicy getSecurityPolicy() {
            return securityPolicy;
        }
    }
}
