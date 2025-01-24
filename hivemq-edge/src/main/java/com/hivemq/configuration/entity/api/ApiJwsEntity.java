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

import javax.xml.bind.annotation.*;
import java.util.Objects;

/**
 * @author Simon L Johnson
 */
@XmlRootElement(name = "generated-tokens")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class ApiJwsEntity {

    @XmlElement(name = "keySize", required = true)
    private int keySize = 2048;
    @XmlElement(name = "issuer", required = true, defaultValue = "HiveMQ-Edge")
    private @NotNull String issuer = "HiveMQ-Edge";
    @XmlElement(name = "audience", required = true, defaultValue = "HiveMQ-Edge-Api")
    private @NotNull String audience = "HiveMQ-Edge-Api";
    @XmlElement(name = "expiryTimeMinutes", required = true, defaultValue = "30")
    private int expiryTimeMinutes = 30;
    @XmlElement(name = "tokenEarlyEpochThresholdMinutes", required = true, defaultValue = "2")
    private int tokenEarlyEpochThresholdMinutes = 2;

    public int getKeySize() {
        return keySize;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getAudience() {
        return audience;
    }

    public int getExpiryTimeMinutes() {
        return expiryTimeMinutes;
    }

    public int getTokenEarlyEpochThresholdMinutes() {
        return tokenEarlyEpochThresholdMinutes;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ApiJwsEntity that = (ApiJwsEntity) o;
        return getKeySize() == that.getKeySize() &&
                getExpiryTimeMinutes() == that.getExpiryTimeMinutes() &&
                getTokenEarlyEpochThresholdMinutes() == that.getTokenEarlyEpochThresholdMinutes() &&
                Objects.equals(getIssuer(), that.getIssuer()) &&
                Objects.equals(getAudience(), that.getAudience());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKeySize(),
                getIssuer(),
                getAudience(),
                getExpiryTimeMinutes(),
                getTokenEarlyEpochThresholdMinutes());
    }
}
