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
package com.hivemq.bridge.config;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.util.encoders.Base64;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LocalSubscription {
    private final @NotNull List<String> filters;
    private final @Nullable String destination;
    private final @NotNull List<String> excludes;
    private final @NotNull List<CustomUserProperty> customUserProperties;
    private final boolean preserveRetain;
    private final int maxQoS;
    private @Nullable String uniqueId;
    private final @Nullable Long queueLimit;

    public LocalSubscription(@NotNull final List<String> filters, @Nullable final String destination) {
        this.filters = filters;
        this.destination = destination;
        this.excludes = List.of();
        this.customUserProperties = List.of();
        this.maxQoS = 2;
        this.preserveRetain = false;
        this.queueLimit = null;
    }

    public LocalSubscription(
            @NotNull List<String> filters,
            @Nullable String destination,
            @NotNull List<String> excludes,
            @NotNull List<CustomUserProperty> customUserProperties,
            boolean preserveRetain,
            int maxQoS,
            final @Nullable Long queueLimit) {

        this.filters = filters;
        this.destination = destination;
        this.excludes = excludes;
        this.customUserProperties = customUserProperties;
        this.maxQoS = maxQoS;
        this.preserveRetain = preserveRetain;
        this.queueLimit = queueLimit;
    }

    public @NotNull List<String> getFilters() {
        return filters;
    }

    public @Nullable String getDestination() {
        return destination;
    }

    public @NotNull List<String> getExcludes() {
        return excludes;
    }

    public @NotNull List<CustomUserProperty> getCustomUserProperties() {
        return customUserProperties;
    }

    public boolean isPreserveRetain() {
        return preserveRetain;
    }

    public int getMaxQoS() {
        return maxQoS;
    }

    public @Nullable Long getQueueLimit() {
        return queueLimit;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocalSubscription that = (LocalSubscription) o;

        if (preserveRetain != that.preserveRetain) return false;
        if (maxQoS != that.maxQoS) return false;
        if (!filters.equals(that.filters)) return false;
        if (!Objects.equals(destination, that.destination)) return false;
        if (!excludes.equals(that.excludes)) return false;
        if (!customUserProperties.equals(that.customUserProperties)) return false;
        if (!Objects.equals(uniqueId, that.uniqueId)) return false;
        return Objects.equals(queueLimit, that.queueLimit);
    }

    @Override
    public int hashCode() {
        int result = filters.hashCode();
        result = 31 * result + (destination != null ? destination.hashCode() : 0);
        result = 31 * result + excludes.hashCode();
        result = 31 * result + customUserProperties.hashCode();
        result = 31 * result + (preserveRetain ? 1 : 0);
        result = 31 * result + maxQoS;
        result = 31 * result + (uniqueId != null ? uniqueId.hashCode() : 0);
        result = 31 * result + (queueLimit != null ? queueLimit.hashCode() : 0);
        return result;
    }

    public @NotNull String calculateUniqueId() {
        if (uniqueId != null) {
            return uniqueId;
        }
        final MD5Digest md5Overall = new MD5Digest();
        final int digestSize = md5Overall.getDigestSize();
        byte[] digestOverAll = new byte[digestSize];

        if (!filters.isEmpty()) {
            // input list is immutable, need mutable list
            ArrayList<String> strings= new ArrayList<>(filters);
            strings.sort(String::compareTo);
            final byte[] filtersAsBytes = String.join("", strings).getBytes(StandardCharsets.UTF_8);
            md5Overall.update(filtersAsBytes, 0, filtersAsBytes.length);
        }

        if (destination != null) {
            final byte[] destBytes = destination.getBytes(StandardCharsets.UTF_8);
            md5Overall.update(destBytes, 0, destBytes.length);
        }

        md5Overall.doFinal(digestOverAll, 0);
        uniqueId = Base64.toBase64String(digestOverAll);
        return uniqueId;
    }

}
