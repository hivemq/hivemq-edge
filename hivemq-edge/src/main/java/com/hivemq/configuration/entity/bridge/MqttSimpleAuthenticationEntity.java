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
package com.hivemq.configuration.entity.bridge;

import org.jetbrains.annotations.Nullable;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "mqtt-simple-authentication")
public class MqttSimpleAuthenticationEntity {

    @XmlElement(name = "username")
    private @Nullable String user;

    @XmlElement(name = "password")
    private @Nullable String password;

    public @Nullable String getUser() {
        return user;
    }

    public @Nullable String getPassword() {
        return password;
    }

    public void setUser(final String user) {
        this.user = user;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final MqttSimpleAuthenticationEntity that = (MqttSimpleAuthenticationEntity) o;
        return Objects.equals(getUser(), that.getUser()) && Objects.equals(getPassword(), that.getPassword());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUser(), getPassword());
    }
}
