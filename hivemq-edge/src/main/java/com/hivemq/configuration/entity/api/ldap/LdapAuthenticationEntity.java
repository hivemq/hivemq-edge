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
package com.hivemq.configuration.entity.api.ldap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * XML entity for LDAP authentication configuration.
 * <p>
 * Configures connection to an LDAP server for user authentication in the Admin API.
 * Supports plain LDAP, LDAPS (LDAP over TLS), and START_TLS modes.
 * <p>
 * Example configuration:
 * <pre>{@code
 * <ldap>
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
 * </ldap>
 * }</pre>
 */
@XmlRootElement(name = "ldap")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class LdapAuthenticationEntity {

    @XmlElementWrapper(name = "servers", required = true)
    @XmlElement(name = "ldap-server")
    private @NotNull List<LdapServerEntity> servers = new ArrayList<>();

    @XmlElement(name = "tls-mode")
    private @NotNull String tlsMode = "NONE";

    @XmlElementRef(required = false)
    private @Nullable TrustStoreEntity trustStore = null;

    @XmlElement(name = "connect-timeout-millis")
    private int connectTimeoutMillis = 0;

    @XmlElement(name = "response-timeout-millis")
    private int responseTimeoutMillis = 10_000;

    @XmlElement(name = "max-connections", required = true, defaultValue = "1")
    private int maxConnections = 1;

    @XmlElement(name = "uid-attribute")
    private @Nullable String uidAttribute = "uid";

    @XmlElement(name = "rdns")
    private @Nullable String rdns = null;

    @XmlElement(name = "base-dn")
    private @Nullable String baseDn = null;

    @XmlElement(name = "required-object-class")
    private @Nullable String requiredObjectClass = null;

    @XmlElement(name = "directory-descent")
    private boolean directoryDescent = false;

    @XmlElement(name = "search-timeout-seconds")
    private int searchTimeoutSeconds = 5;

    @XmlElement(name = "simple-bind", required = true)
    private @NotNull LdapSimpleBindEntity simpleBindEntity = new LdapSimpleBindEntity();

    @XmlElementWrapper(name = "user-roles", required = false)
    @XmlElement(name = "user-role")
    private @Nullable List<UserRoleEntity> userRoles = null;

    public @NotNull String getTlsMode() {
        return tlsMode;
    }

    public @Nullable TrustStoreEntity getTrustStore() {
        return trustStore;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public int getResponseTimeoutMillis() {
        return responseTimeoutMillis;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public @Nullable String getUidAttribute() {
        return uidAttribute;
    }

    public @Nullable String getRdns() {
        return rdns;
    }

    public @Nullable String getBaseDn() {
        return baseDn;
    }

    public @Nullable String getRequiredObjectClass() {
        return requiredObjectClass;
    }

    public boolean getDirecoryDescent() {
        return directoryDescent;
    }

    public int getSearchTimeoutSeconds() {
        return searchTimeoutSeconds;
    }

    public @NotNull LdapSimpleBindEntity getSimpleBindEntity() {
        return simpleBindEntity;
    }

    public @NotNull List<LdapServerEntity> getServers() {
        return servers;
    }

    public @Nullable List<UserRoleEntity> getUserRoles() {
        return userRoles;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final LdapAuthenticationEntity that = (LdapAuthenticationEntity) o;
        return getConnectTimeoutMillis() == that.getConnectTimeoutMillis() &&
                getResponseTimeoutMillis() == that.getResponseTimeoutMillis() &&
                getMaxConnections() == that.getMaxConnections() &&
                directoryDescent == that.directoryDescent &&
                getSearchTimeoutSeconds() == that.getSearchTimeoutSeconds() &&
                Objects.equals(getServers(), that.getServers()) &&
                Objects.equals(getTlsMode(), that.getTlsMode()) &&
                Objects.equals(getTrustStore(), that.getTrustStore()) &&
                Objects.equals(getUidAttribute(), that.getUidAttribute()) &&
                Objects.equals(getRdns(), that.getRdns()) &&
                Objects.equals(getBaseDn(), that.getBaseDn()) &&
                Objects.equals(getRequiredObjectClass(), that.getRequiredObjectClass()) &&
                Objects.equals(getSimpleBindEntity(), that.getSimpleBindEntity()) &&
                Objects.equals(getUserRoles(), that.getUserRoles());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getServers(),
                getTlsMode(),
                getTrustStore(),
                getConnectTimeoutMillis(),
                getResponseTimeoutMillis(),
                getMaxConnections(),
                getUidAttribute(),
                getRdns(),
                getBaseDn(),
                getRequiredObjectClass(),
                directoryDescent,
                getSearchTimeoutSeconds(),
                getSimpleBindEntity(),
                getUserRoles());
    }
}
