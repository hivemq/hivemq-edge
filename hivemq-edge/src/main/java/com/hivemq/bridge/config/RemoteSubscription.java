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

import java.util.List;
import java.util.Objects;

public class RemoteSubscription {

    private final @NotNull List<String> filters;
    private final @Nullable String destination;
    private final @NotNull List<CustomUserProperty> customUserProperties;
    private final boolean preserveRetain;
    private final int maxQoS;

    public RemoteSubscription(@NotNull final List<String> filters, @Nullable final String destination) {
        this.filters = filters;
        this.destination = destination;
        this.customUserProperties = List.of();
        this.maxQoS = 2;
        this.preserveRetain = false;
    }

    public RemoteSubscription(
            @NotNull List<String> filters,
            @Nullable String destination,
            @NotNull List<CustomUserProperty> customUserProperties,
            boolean preserveRetain,
            int maxQoS) {
        this.filters = filters;
        this.destination = destination;
        this.customUserProperties = customUserProperties;
        this.preserveRetain = preserveRetain;
        this.maxQoS = maxQoS;
    }

    public @NotNull List<String> getFilters() {
        return filters;
    }

    public @Nullable String getDestination() {
        return destination;
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

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof RemoteSubscription)) return false;

        RemoteSubscription that = (RemoteSubscription) o;

        if (preserveRetain != that.preserveRetain) return false;
        if (maxQoS != that.maxQoS) return false;
        if (!filters.equals(that.filters)) return false;
        if (!Objects.equals(destination, that.destination)) return false;
        return customUserProperties.equals(that.customUserProperties);
    }

    @Override
    public int hashCode() {
        int result = filters.hashCode();
        result = 31 * result + (destination != null ? destination.hashCode() : 0);
        result = 31 * result + customUserProperties.hashCode();
        result = 31 * result + (preserveRetain ? 1 : 0);
        result = 31 * result + maxQoS;
        return result;
    }
}
