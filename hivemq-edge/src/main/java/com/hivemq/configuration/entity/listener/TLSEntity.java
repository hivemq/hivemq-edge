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

package com.hivemq.configuration.entity.listener;

import com.hivemq.configuration.entity.listener.tls.ClientAuthenticationModeEntity;
import com.hivemq.configuration.entity.listener.tls.KeystoreEntity;
import com.hivemq.configuration.entity.listener.tls.TruststoreEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Dominik Obermaier
 * @author Florian Limpöck
 */
@XmlRootElement(name = "tls")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class TLSEntity {

    @XmlElementRef
    private @Nullable KeystoreEntity keystoreEntity = null;

    @XmlElementRef(required = false)
    private @Nullable TruststoreEntity truststoreEntity = null;

    @XmlElement(name = "handshake-timeout", defaultValue = "10000")
    private @NotNull Integer handshakeTimeout = 10000;

    @XmlElement(name = "client-authentication-mode", defaultValue = "NONE")
    private @NotNull ClientAuthenticationModeEntity clientAuthMode = ClientAuthenticationModeEntity.NONE;

    @XmlElementWrapper(name = "protocols")
    @XmlElement(name = "protocol")
    private @NotNull List<String> protocols = new ArrayList<>();

    @XmlElementWrapper(name = "cipher-suites")
    @XmlElement(name = "cipher-suite")
    private @NotNull List<String> cipherSuites = new ArrayList<>();

    @XmlElement(name = "prefer-server-cipher-suites")
    private @Nullable Boolean preferServerCipherSuites = null;

    public @Nullable KeystoreEntity getKeystoreEntity() {
        return keystoreEntity;
    }

    public @Nullable TruststoreEntity getTruststoreEntity() {
        return truststoreEntity;
    }

    public int getHandshakeTimeout() {
        return handshakeTimeout;
    }

    public @NotNull ClientAuthenticationModeEntity getClientAuthMode() {
        return clientAuthMode;
    }

    public @NotNull List<String> getProtocols() {
        return protocols;
    }

    public @NotNull List<String> getCipherSuites() {
        return cipherSuites;
    }

    public @Nullable Boolean isPreferServerCipherSuites() {
        return preferServerCipherSuites;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final TLSEntity tlsEntity = (TLSEntity) o;
        return Objects.equals(getKeystoreEntity(), tlsEntity.getKeystoreEntity()) &&
                Objects.equals(getTruststoreEntity(), tlsEntity.getTruststoreEntity()) &&
                Objects.equals(getHandshakeTimeout(), tlsEntity.getHandshakeTimeout()) &&
                getClientAuthMode() == tlsEntity.getClientAuthMode() &&
                Objects.equals(getProtocols(), tlsEntity.getProtocols()) &&
                Objects.equals(getCipherSuites(), tlsEntity.getCipherSuites()) &&
                Objects.equals(preferServerCipherSuites, tlsEntity.preferServerCipherSuites);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKeystoreEntity(),
                getTruststoreEntity(),
                getHandshakeTimeout(),
                getClientAuthMode(),
                getProtocols(),
                getCipherSuites(),
                preferServerCipherSuites);
    }
}
