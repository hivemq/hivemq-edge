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

import com.hivemq.configuration.entity.listener.tls.KeystoreEntity;
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

@XmlRootElement(name = "tls")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class ApiTlsEntity {

    @XmlElementWrapper(name = "protocols")
    @XmlElement(name = "protocol")
    private final @NotNull List<String> protocols;
    @XmlElementWrapper(name = "cipher-suites")
    @XmlElement(name = "cipher-suite")
    private final @NotNull List<String> cipherSuites;
    @XmlElementRef
    private @Nullable KeystoreEntity keystoreEntity;

    public ApiTlsEntity() {
        protocols = new ArrayList<>();
        cipherSuites = new ArrayList<>();
    }

    public @Nullable KeystoreEntity getKeystoreEntity() {
        return keystoreEntity;
    }

    public @NotNull List<String> getProtocols() {
        return protocols;
    }

    public @NotNull List<String> getCipherSuites() {
        return cipherSuites;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof final ApiTlsEntity that) {
            return Objects.equals(keystoreEntity, that.keystoreEntity) &&
                    Objects.equals(protocols, that.protocols) &&
                    Objects.equals(cipherSuites, that.cipherSuites);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(keystoreEntity, protocols, cipherSuites);
    }
}
