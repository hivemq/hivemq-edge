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
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * XML entity for LDAP TLS/SSL configuration.
 * <p>
 * Configures truststore settings for secure LDAP connections (LDAPS or START_TLS).
 * If no truststore is configured, the system's default CA certificates will be used.
 */
@XmlRootElement(name = "tls")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class TrustStoreEntity {

    @XmlElement(name = "truststore-path", required = true, defaultValue = "")
    private @NotNull String trustStorePath = "";

    @XmlElement(name = "truststore-password")
    private @Nullable String trustStorePassword = null;

    @XmlElement(name = "truststore-type")
    private @Nullable String trustStoreType = null;

    public @NotNull String getTrustStorePath() {
        return trustStorePath;
    }

    public @Nullable String getTrustStorePassword() {
        return trustStorePassword;
    }

    public @Nullable String getTrustStoreType() {
        return trustStoreType;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TrustStoreEntity)) {
            return false;
        }
        final TrustStoreEntity that = (TrustStoreEntity) o;
        return Objects.equals(trustStorePath, that.trustStorePath)
                && Objects.equals(trustStorePassword, that.trustStorePassword)
                && Objects.equals(trustStoreType, that.trustStoreType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trustStorePath, trustStorePassword, trustStoreType);
    }
}
