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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Simon L Johnson
 */
@XmlRootElement(name = "tls")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class ApiTlsEntity {

    @XmlElementRef
    private @Nullable KeystoreEntity keystoreEntity;

    @XmlElementWrapper(name = "protocols")
    @XmlElement(name = "protocol")
    private @NotNull List<String> protocols = new ArrayList<>();

    @XmlElementWrapper(name = "cipher-suites")
    @XmlElement(name = "cipher-suite")
    private @NotNull List<String> cipherSuites = new ArrayList<>();

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
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ApiTlsEntity that = (ApiTlsEntity) o;
        return Objects.equals(getKeystoreEntity(), that.getKeystoreEntity()) &&
                Objects.equals(getProtocols(), that.getProtocols()) &&
                Objects.equals(getCipherSuites(), that.getCipherSuites());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKeystoreEntity(), getProtocols(), getCipherSuites());
    }
}
