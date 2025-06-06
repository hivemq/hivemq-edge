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

import org.jetbrains.annotations.NotNull;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.Objects;

@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public abstract class ApiListenerEntity {

    @XmlElement(name = "port", required = true)
    private int port;

    @XmlElement(name = "bind-address", required = true)
    private @NotNull String bindAddress = "0.0.0.0";

    public int getPort() {
        return port;
    }

    public @NotNull String getBindAddress() {
        return bindAddress;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ApiListenerEntity that = (ApiListenerEntity) o;
        return getPort() == that.getPort() && Objects.equals(getBindAddress(), that.getBindAddress());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPort(), getBindAddress());
    }
}
