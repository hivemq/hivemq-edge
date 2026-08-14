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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.util.encoders.Base64;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LocalSubscription {
    private final @NotNull List<String> filters;
    private final @Nullable String destination;
    private final @NotNull List<String> excludes;
    private final @NotNull List<CustomUserProperty> customUserProperties;
    private final boolean preserveRetain;
    private final int maxQoS;
    private @Nullable String uniqueId;
    private final @Nullable Long queueLimit;

    public LocalSubscription(final @NotNull List<String> filters, final @Nullable String destination) {
        this.filters = canonical(filters);
        this.destination = destination;
        this.excludes = List.of();
        this.customUserProperties = List.of();
        this.maxQoS = 2;
        this.preserveRetain = false;
        this.queueLimit = null;
    }

    public LocalSubscription(
            @NotNull final List<String> filters,
            @Nullable final String destination,
            @NotNull final List<String> excludes,
            @NotNull final List<CustomUserProperty> customUserProperties,
            final boolean preserveRetain,
            final int maxQoS,
            final @Nullable Long queueLimit) {

        this.filters = canonical(filters);
        this.destination = destination;
        this.excludes = canonical(excludes);
        this.customUserProperties = customUserProperties;
        this.maxQoS = maxQoS;
        this.preserveRetain = preserveRetain;
        this.queueLimit = queueLimit;
    }

    /**
     * Sorts a list of topic filters, so that two subscriptions differing only in the order they were
     * written in are one subscription (EDG-882 F-05's sibling, F-07).
     * <p>
     * Order carries no meaning for either list: the filters are what the forwarder subscribes to, and
     * the excludes are a match-any test. But {@link #equals(Object)} compared them positionally while
     * {@link #calculateUniqueId()} sorted them, so reordering a filter in the configuration file
     * produced a subscription that was "changed" for the reload path and identical for the queue
     * naming — and the reload path answers a change by restarting the bridge in the mode that clears
     * its queues. A formatting edit destroyed every message waiting to be forwarded.
     * <p>
     * <b>Sorted, not de-duplicated.</b> The digest that names every persisted queue is taken over the
     * sorted filters <i>including</i> repeats, so collapsing {@code ["a", "a"]} to {@code ["a"]} would
     * change that name and strand the messages already queued under the old one on upgrade — the same
     * trade this ticket refused for the encoding itself. Repeats stay, and two configurations that
     * differ by one are still different.
     * <p>
     * {@code customUserProperties} is deliberately left alone: MQTT user properties are ordered, and
     * two properties with the same key are legal, so their order is part of the configuration rather
     * than an accident of how it was written.
     */
    private static @NotNull List<String> canonical(final @NotNull List<String> topicFilters) {
        return topicFilters.stream().sorted().toList();
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

    /**
     * Compares the configured state only.
     * <p>
     * {@code uniqueId} is deliberately excluded: it is derived data — a digest over {@link #filters}
     * and {@link #destination}, both of which are compared here — and it is computed lazily by
     * {@link #calculateUniqueId()} rather than at construction. A running bridge has therefore had it
     * filled in, while a subscription freshly read from the configuration file has not, so including
     * it made two identical configurations compare as different (EDG-882's sibling defect, EDG-884).
     * The config-reload path takes that as "the bridge changed", restarts the bridge in the mode that
     * discards its queue, and destroys every message waiting to be forwarded — under an identity that
     * recomputes to exactly the same value.
     * <p>
     * Excluding it is behaviour-preserving for genuine configuration changes: any change that would
     * alter the digest necessarily alters {@code filters} or {@code destination} first.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof LocalSubscription that)) return false;

        if (preserveRetain != that.preserveRetain) return false;
        if (maxQoS != that.maxQoS) return false;
        if (!filters.equals(that.filters)) return false;
        if (!Objects.equals(destination, that.destination)) return false;
        if (!excludes.equals(that.excludes)) return false;
        if (!customUserProperties.equals(that.customUserProperties)) return false;
        return Objects.equals(queueLimit, that.queueLimit);
    }

    /** Mirrors {@link #equals(Object)}: {@code uniqueId} is excluded, so the hash is stable over the
     * object's lifetime rather than changing the first time the digest is memoised. */
    @Override
    public int hashCode() {
        int result = filters.hashCode();
        result = 31 * result + (destination != null ? destination.hashCode() : 0);
        result = 31 * result + excludes.hashCode();
        result = 31 * result + customUserProperties.hashCode();
        result = 31 * result + (preserveRetain ? 1 : 0);
        result = 31 * result + maxQoS;
        result = 31 * result + (queueLimit != null ? queueLimit.hashCode() : 0);
        return result;
    }

    /**
     * The identity a bridge forwarder — and therefore the name of every queue it owns — is derived
     * from: an MD5 digest over the sorted filters and the destination, rendered in standard Base64.
     * <p>
     * <b>This function is not injective, deliberately so.</b> The filters are joined with an
     * <em>empty</em> separator, so any two filter lists with the same sorted concatenation hash to the
     * same value: {@code ["ab", "c"]} and {@code ["a", "bc"]} both digest the bytes {@code abc}. Two
     * such subscriptions on one bridge would own queues under a single identity, and registering the
     * second would take the first's queues out of the ownership index — leaving live queues looking
     * orphaned to the periodic clean-up, which is exactly the EDG-882 message-loss path.
     * <p>
     * The ambiguity is <b>not</b> fixed here, and must not be: any change to this encoding — a
     * separator, length prefixes, a URL-safe alphabet — changes the digest of <em>every</em>
     * configuration, renaming every persisted bridge queue on upgrade and stranding the messages
     * already sitting in them. Trading silent loss for loss-on-upgrade is not a fix; EDG-882 rejected
     * re-encoding for that reason. Removing the ambiguity requires versioned identities with a
     * migration or dual lookup, which is a change of its own.
     * <p>
     * What guards the defect instead is {@link com.hivemq.bridge.mqtt.BridgeMqttClient#createForwarders()},
     * which refuses to start a bridge whose local subscriptions do not resolve to distinct forwarder
     * ids. An operator sees a startup error naming both subscriptions; nothing is silently discarded.
     */
    public @NotNull String calculateUniqueId() {
        if (uniqueId != null) {
            return uniqueId;
        }
        final MD5Digest md5Overall = new MD5Digest();
        final int digestSize = md5Overall.getDigestSize();
        final byte[] digestOverAll = new byte[digestSize];

        if (!filters.isEmpty()) {
            // Sorted again rather than trusting the constructor's canonical order: this digest names
            // every persisted queue of the subscription, so it must not become sensitive to how the
            // list arrived here if a future path ever bypasses canonicalisation. Sorting a sorted list
            // costs nothing and the digest is unchanged either way.
            final ArrayList<String> strings = new ArrayList<>(filters);
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

    @Override
    public String toString() {
        return "LocalSubscription{" + "filters="
                + filters
                + ", destination='"
                + destination
                + '\''
                + ", excludes="
                + excludes
                + ", customUserProperties="
                + customUserProperties
                + ", preserveRetain="
                + preserveRetain
                + ", maxQoS="
                + maxQoS
                + ", uniqueId='"
                + uniqueId
                + '\''
                + ", queueLimit="
                + queueLimit
                + '}';
    }
}
