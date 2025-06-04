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
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@XmlRootElement(name = "authentication")
@XmlAccessorType(XmlAccessType.NONE)
public class BridgeAuthenticationEntity {

    @XmlElementRef
    private @Nullable MqttSimpleAuthenticationEntity mqttSimpleAuthenticationEntity;

    public @Nullable MqttSimpleAuthenticationEntity getMqttSimpleAuthenticationEntity() {
        return mqttSimpleAuthenticationEntity;
    }

    public void setMqttSimpleAuthenticationEntity(final MqttSimpleAuthenticationEntity mqttSimpleAuthenticationEntity) {
        this.mqttSimpleAuthenticationEntity = mqttSimpleAuthenticationEntity;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final BridgeAuthenticationEntity that = (BridgeAuthenticationEntity) o;
        return Objects.equals(getMqttSimpleAuthenticationEntity(), that.getMqttSimpleAuthenticationEntity());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getMqttSimpleAuthenticationEntity());
    }
}
