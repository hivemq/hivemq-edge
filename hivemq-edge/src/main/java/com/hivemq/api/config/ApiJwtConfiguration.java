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
package com.hivemq.api.config;

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.Objects;

/**
 * @author Simon L Johnson
 */
public class ApiJwtConfiguration {

    private final int keySize;
    private final @NotNull String issuer;
    private final @NotNull String audience;
    private final int expiryTimeMinutes;
    private final int tokenEarlyEpochThresholdMinutes;

    public ApiJwtConfiguration(
            final int keySize,
            final @NotNull String issuer,
            final @NotNull String audience,
            final int expiryTimeMinutes,
            final int tokenEarlyEpochThresholdMinutes) {
        Preconditions.checkNotNull(issuer);
        Preconditions.checkNotNull(audience);
        this.keySize = keySize;
        this.issuer = issuer;
        this.audience = audience;
        this.expiryTimeMinutes = expiryTimeMinutes;
        this.tokenEarlyEpochThresholdMinutes = tokenEarlyEpochThresholdMinutes;
    }

    public int getKeySize() {
        return keySize;
    }

    public String getIssuer() {
        return issuer;
    }

    public int getExpiryTimeMinutes() {
        return expiryTimeMinutes;
    }

    public int getTokenEarlyEpochThresholdMinutes() {
        return tokenEarlyEpochThresholdMinutes;
    }

    public String getAudience() {
        return audience;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiJwtConfiguration that = (ApiJwtConfiguration) o;
        return keySize == that.keySize &&
                expiryTimeMinutes == that.expiryTimeMinutes &&
                tokenEarlyEpochThresholdMinutes == that.tokenEarlyEpochThresholdMinutes &&
                issuer.equals(that.issuer) &&
                audience.equals(that.audience);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keySize, issuer, audience, expiryTimeMinutes, tokenEarlyEpochThresholdMinutes);
    }

    public static class Builder {
        private @NotNull String issuer = "HiveMQ-Edge-Gateway";
        private @NotNull String audience = "API";
        private int keySize = 2048;
        private int tokenEarlyEpochThresholdMinutes = 1;
        private int expiryTimeMinutes = 30;

        public @NotNull ApiJwtConfiguration.Builder withIssuer(@NotNull String issuer) {
            this.issuer = issuer;
            return this;
        }

        public @NotNull ApiJwtConfiguration.Builder withAudience(@NotNull String audience) {
            this.audience = audience;
            return this;
        }

        public @NotNull ApiJwtConfiguration.Builder withKeySize(int keySize) {
            this.keySize = keySize;
            return this;
        }

        public @NotNull ApiJwtConfiguration.Builder withTokenEarlyEpochThresholdMinutes(int tokenEarlyEpochThresholdMinutes) {
            this.tokenEarlyEpochThresholdMinutes = tokenEarlyEpochThresholdMinutes;
            return this;
        }

        public @NotNull ApiJwtConfiguration.Builder withExpiryTimeMinutes(int expiryTimeMinutes) {
            this.expiryTimeMinutes = expiryTimeMinutes;
            return this;
        }

        public @NotNull ApiJwtConfiguration build() {
            return new ApiJwtConfiguration(keySize,
                    issuer,
                    audience,
                    expiryTimeMinutes,
                    tokenEarlyEpochThresholdMinutes);
        }
    }
}
