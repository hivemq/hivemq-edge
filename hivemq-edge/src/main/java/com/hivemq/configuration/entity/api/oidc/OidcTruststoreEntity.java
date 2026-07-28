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
package com.hivemq.configuration.entity.api.oidc;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/**
 * XML entity for the OIDC Identity Provider truststore.
 * <p>
 * Configures the truststore used to validate the Identity Provider's TLS certificate when Edge fetches
 * discovery, tokens, and signing keys. If no truststore is configured, the system default CA
 * certificates are used. This mirrors the LDAP truststore configuration.
 */
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class OidcTruststoreEntity {

    @XmlElement(name = "truststore-path")
    private @Nullable String truststorePath = null;

    @XmlElement(name = "truststore-password")
    private @Nullable String truststorePassword = null;

    @XmlElement(name = "truststore-type")
    private @Nullable String truststoreType = null;

    public @Nullable String getTruststorePath() {
        return truststorePath;
    }

    public @Nullable String getTruststorePassword() {
        return truststorePassword;
    }

    public @Nullable String getTruststoreType() {
        return truststoreType;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OidcTruststoreEntity that)) {
            return false;
        }
        return Objects.equals(truststorePath, that.truststorePath)
                && Objects.equals(truststorePassword, that.truststorePassword)
                && Objects.equals(truststoreType, that.truststoreType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(truststorePath, truststorePassword, truststoreType);
    }
}
