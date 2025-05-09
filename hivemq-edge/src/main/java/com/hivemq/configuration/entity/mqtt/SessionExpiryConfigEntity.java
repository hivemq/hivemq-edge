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
package com.hivemq.configuration.entity.mqtt;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.Objects;

import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRY_MAX;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@XmlRootElement(name = "session-expiry")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class SessionExpiryConfigEntity {

    @XmlElement(name = "max-interval", defaultValue = "4294967295")
    // => 136 Years = Unsigned Integer Max Value in seconds
    private long maxInterval = SESSION_EXPIRY_MAX;

    public long getMaxInterval() {
        return maxInterval;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final SessionExpiryConfigEntity that = (SessionExpiryConfigEntity) o;
        return getMaxInterval() == that.getMaxInterval();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getMaxInterval());
    }
}
