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
package com.hivemq.configuration.entity.api;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * XML entity for LDAP authentication configuration.
 * <p>
 * Configures connection to an LDAP server for user authentication in the Admin API.
 * Supports plain LDAP, LDAPS (LDAP over TLS), and START_TLS modes.
 * <p>
 * Example configuration:
 * <pre>{@code
 * <ldap-authentication>
 *     <host>ldap.example.com</host>
 *     <port>636</port>
 *     <tls-mode>LDAPS</tls-mode>
 *     <tls>
 *         <truststore-path>/path/to/truststore.jks</truststore-path>
 *         <truststore-password>changeit</truststore-password>
 *         <truststore-type>JKS</truststore-type>
 *     </tls>
 *     <user-dn-template>uid={username},ou=people,{baseDn}</user-dn-template>
 *     <base-dn>dc=example,dc=com</base-dn>
 * </ldap-authentication>
 * }</pre>
 */
@XmlRootElement(name = "ldap-authentication")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class LdapAuthenticationEntity {

    @XmlElement(name = "host", required = true)
    private @NotNull String host = "";

    @XmlElement(name = "port")
    private int port = 0;

    @XmlElement(name = "tls-mode")
    private @NotNull String tlsMode = "NONE";

    @XmlElementRef(required = false)
    private @Nullable LdapTlsEntity tls = null;

    @XmlElement(name = "connect-timeout-millis")
    private int connectTimeoutMillis = 0;

    @XmlElement(name = "response-timeout-millis")
    private int responseTimeoutMillis = 0;

    @XmlElement(name = "user-dn-template", required = true)
    private @NotNull String userDnTemplate = "";

    @XmlElement(name = "base-dn", required = true)
    private @NotNull String baseDn = "";

    public @NotNull String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public @NotNull String getTlsMode() {
        return tlsMode;
    }

    public @Nullable LdapTlsEntity getTls() {
        return tls;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public int getResponseTimeoutMillis() {
        return responseTimeoutMillis;
    }

    public @NotNull String getUserDnTemplate() {
        return userDnTemplate;
    }

    public @NotNull String getBaseDn() {
        return baseDn;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final LdapAuthenticationEntity that = (LdapAuthenticationEntity) o;
        return port == that.port &&
                connectTimeoutMillis == that.connectTimeoutMillis &&
                responseTimeoutMillis == that.responseTimeoutMillis &&
                Objects.equals(host, that.host) &&
                Objects.equals(tlsMode, that.tlsMode) &&
                Objects.equals(tls, that.tls) &&
                Objects.equals(userDnTemplate, that.userDnTemplate) &&
                Objects.equals(baseDn, that.baseDn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, tlsMode, tls, connectTimeoutMillis, responseTimeoutMillis, userDnTemplate, baseDn);
    }
}
