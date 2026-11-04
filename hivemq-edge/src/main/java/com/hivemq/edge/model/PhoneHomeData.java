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
package com.hivemq.edge.model;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Facts added to phone-home events that the code building the event cannot see: the licence id, set by the commercial
 * modules as system properties, and the inventory of configured adapters, supplied by each adapter framework. Both
 * travel in {@link HiveMQEdgeRemoteEvent#userData}, which the receiving schema accepts with any string key.
 */
public final class PhoneHomeData {

    /** System property the commercial modules set to the id of the loaded licence, and clear when it goes. */
    public static final @NotNull String LICENSE_ID_PROPERTY = "hivemq.edge.license.id";

    /**
     * System property the commercial modules set to the last day the loaded licence is valid ({@code yyyy-MM-dd}); the
     * id is not sent after that day, even before the licence folder is checked again.
     */
    public static final @NotNull String LICENSE_VALID_UNTIL_PROPERTY = "hivemq.edge.license.valid-until";

    public static final @NotNull String LICENSE_ID_KEY = "licenseId";

    /** {@code protocolId:version:count} per protocol and version, comma separated, sorted. */
    public static final @NotNull String ADAPTERS_KEY = "adapters";

    /** The version reported for a configured adapter whose type is not installed or failed to load. */
    public static final @NotNull String UNKNOWN_VERSION = "unknown";

    /** The placeholder first-party adapters return from {@code getVersion()}, resolved to the Edge version. */
    static final @NotNull String EDGE_VERSION_PLACEHOLDER = "${edge-version}";

    /** One configured adapter instance: its protocol id and the version of its installed type, if any. */
    public record ConfiguredAdapter(
            @NotNull String protocolId, @Nullable String version) {}

    /**
     * One source per adapter framework, keyed by framework name; a new framework registers its own. Written when a
     * framework starts, read by the phone-home thread when it builds a ping.
     */
    private static final @NotNull Map<String, Supplier<? extends Collection<ConfiguredAdapter>>> adapterSources =
            new ConcurrentHashMap<>();

    /** Today's date for the licence check; replaced only by tests, read by every thread that builds an event. */
    private static volatile @NotNull Clock clock = Clock.system(ZoneId.systemDefault());

    private PhoneHomeData() {}

    /**
     * Registers (or replaces) the configured adapters of one adapter framework. Every configured adapter instance is
     * listed, whether or not it loaded, so the inventory reports what the customer configured.
     *
     * @param framework the framework's name, unique per framework
     * @param source    supplies the framework's configured adapters when a ping is built
     * @return removes this registration only, so a framework instance that stops after its replacement registered
     *     (embedded Edge restarts) leaves the replacement's source in place
     */
    public static @NotNull Runnable adapterSource(
            final @NotNull String framework, final @NotNull Supplier<? extends Collection<ConfiguredAdapter>> source) {
        adapterSources.put(framework, source);
        return () -> adapterSources.remove(framework, source);
    }

    /** Removes a framework's source, when the framework stops for good or in tests. */
    public static void removeAdapterSource(final @NotNull String framework) {
        adapterSources.remove(framework);
    }

    @VisibleForTesting
    static void clock(final @NotNull Clock newClock) {
        clock = newClock;
    }

    /**
     * @param raw        the version an adapter type reports, or {@code null} when the type is not installed
     * @param edgeVersion replaces {@link #EDGE_VERSION_PLACEHOLDER}
     * @return the version to report
     */
    public static @NotNull String version(final @Nullable String raw, final @NotNull String edgeVersion) {
        if (raw == null || raw.isBlank()) {
            return UNKNOWN_VERSION;
        }
        return raw.replace(EDGE_VERSION_PLACEHOLDER, edgeVersion);
    }

    /**
     * @return the inventory of the given adapters as {@code protocolId:version:count}, one entry per protocol and
     *     version, sorted, comma separated; empty when there are none
     */
    static @NotNull String inventory(final @NotNull Iterable<ConfiguredAdapter> adapters) {
        final Map<String, Integer> counts = new TreeMap<>();
        for (final ConfiguredAdapter adapter : adapters) {
            counts.merge(field(adapter.protocolId()) + ':' + field(adapter.version()), 1, Integer::sum);
        }
        final StringBuilder sb = new StringBuilder();
        counts.forEach((key, count) -> {
            if (!sb.isEmpty()) {
                sb.append(',');
            }
            sb.append(key).append(':').append(count);
        });
        return sb.toString();
    }

    /**
     * A blank value becomes {@code unknown}; {@code %}, {@code :} and {@code ,} are percent-encoded, so distinct
     * values stay distinct and the format's separators never appear inside a field.
     */
    private static @NotNull String field(final @Nullable String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN_VERSION;
        }
        return value.strip().replace("%", "%25").replace(":", "%3A").replace(",", "%2C");
    }

    static void addTo(final @NotNull Map<String, String> userData, final boolean withAdapters) {
        final String licenseId = currentLicenseId();
        if (licenseId != null) {
            userData.put(LICENSE_ID_KEY, licenseId);
        }
        if (withAdapters) {
            final String adapters = currentInventory();
            if (!adapters.isEmpty()) {
                userData.put(ADAPTERS_KEY, adapters);
            }
        }
    }

    /** @return the loaded licence's id, or {@code null} when there is none or it expired before today */
    static @Nullable String currentLicenseId() {
        final String licenseId = System.getProperty(LICENSE_ID_PROPERTY);
        if (licenseId == null || licenseId.isBlank()) {
            return null;
        }
        final String validUntil = System.getProperty(LICENSE_VALID_UNTIL_PROPERTY);
        if (validUntil != null) {
            try {
                if (LocalDate.now(clock).isAfter(LocalDate.parse(validUntil))) {
                    return null;
                }
            } catch (final DateTimeParseException e) {
                return null;
            }
        }
        return licenseId;
    }

    private static @NotNull String currentInventory() {
        final List<ConfiguredAdapter> all = new ArrayList<>();
        for (final Supplier<? extends Collection<ConfiguredAdapter>> source : adapterSources.values()) {
            try {
                all.addAll(source.get());
            } catch (final RuntimeException ignored) {
                // the inventory is best effort; a failing source leaves its adapters out of this ping only
            }
        }
        return inventory(all);
    }
}
