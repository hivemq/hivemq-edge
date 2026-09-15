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

import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RemoteSubscription {

    private final @NotNull List<String> filters;
    private final @NotNull List<String> configuredFilters;
    private final @Nullable String destination;
    private final @NotNull List<CustomUserProperty> customUserProperties;
    private final boolean preserveRetain;
    private final int maxQoS;

    public RemoteSubscription(final @NotNull List<String> filters, final @Nullable String destination) {
        this.filters = canonical(filters);
        this.configuredFilters = List.copyOf(filters);
        this.destination = destination;
        this.customUserProperties = List.of();
        this.maxQoS = 2;
        this.preserveRetain = false;
    }

    public RemoteSubscription(
            @NotNull final List<String> filters,
            @Nullable final String destination,
            @NotNull final List<CustomUserProperty> customUserProperties,
            final boolean preserveRetain,
            final int maxQoS) {
        this.filters = canonical(filters);
        this.configuredFilters = List.copyOf(filters);
        this.destination = destination;
        this.customUserProperties = customUserProperties;
        this.preserveRetain = preserveRetain;
        this.maxQoS = maxQoS;
    }

    /**
     * Sorts the topic filters, so that two subscriptions differing only in the order they were written
     * in are one subscription — the same rule {@code LocalSubscription} applies to the local half,
     * applied here (EDG-882 QA, 2026-08-25).
     * <p>
     * Order carries no meaning: the filters become one SUBSCRIBE packet sent to the remote broker when
     * the bridge connects, and nothing downstream reads them in order. But {@link #equals(Object)}
     * compared them positionally, so reordering a {@code <mqtt-topic-filter>} inside a
     * {@code <remote-subscription>} made the bridge compare as changed, and the reload path answers a
     * change by restarting the bridge. F-07 canonicalised {@code LocalSubscription} and left this half
     * as it was — EDG-884's own yardstick, that an unchanged configuration must compare equal, failing
     * on the remote side.
     * <p>
     * <b>Cheaper here than it was for the local half, and for a reason worth knowing.</b>
     * {@code LocalSubscription}'s canonical order feeds {@code calculateUniqueId()}, which names every
     * persisted queue of the subscription, so changing what it sees is a decision about queue naming.
     * A remote subscription has no derived identity and owns no queue, so this is purely a change to
     * what counts as equal.
     * <p>
     * Sorted, not de-duplicated: two identical filters are a different configuration from one, and
     * {@code customUserProperties} is left alone because MQTT user properties are ordered and
     * duplicate keys are legal, so their order is part of the configuration.
     */
    private static @NotNull List<String> canonical(final @NotNull List<String> topicFilters) {
        return topicFilters.stream().sorted().toList();
    }

    public @NotNull List<String> getFilters() {
        return filters;
    }

    /**
     * The filters in the order they were configured, for writing the configuration back out and for
     * serving over the API.
     * <p>
     * Canonicalisation exists so that reordering a filter is not a configuration change; it was never
     * meant to reorder the operator's file. Every write of {@code config.xml} rebuilds the bridge
     * entities from these objects, and any REST write of any subsystem triggers one, so returning the
     * sorted list to the write-back would rewrite elements the operator put in a deliberate order —
     * exactly the damage {@link LocalSubscription#getConfiguredFilters()} exists to avoid.
     * <p>
     * Deliberately not part of {@link #equals(Object)} or {@link #hashCode()}.
     */
    public @NotNull List<String> getConfiguredFilters() {
        return configuredFilters;
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
    public String toString() {
        return "RemoteSubscription{" + "filters="
                + filters
                + ", destination='"
                + destination
                + '\''
                + ", customUserProperties="
                + customUserProperties
                + ", preserveRetain="
                + preserveRetain
                + ", maxQoS="
                + maxQoS
                + '}';
    }

    /**
     * Compares the configured state; {@code configuredFilters} is excluded, because two subscriptions
     * that differ only in the order they were written in are one subscription. See {@link #canonical}.
     */
    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof RemoteSubscription that)) return false;

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
