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

import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Facts added to every phone-home event that the code building the event cannot see: the licence id, set by the
 * commercial modules as a system property, and the adapter inventory, supplied by the protocol adapter manager.
 * Both travel in {@link HiveMQEdgeRemoteEvent#userData}, which the receiving schema accepts with any string key.
 */
public final class PhoneHomeData {

    /** System property the commercial modules set to the id of the loaded licence, and clear when it goes. */
    public static final @NotNull String LICENSE_ID_PROPERTY = "hivemq.edge.license.id";

    public static final @NotNull String LICENSE_ID_KEY = "licenseId";

    /** {@code protocolId:version:count} per protocol and version, comma separated, sorted. */
    public static final @NotNull String ADAPTERS_KEY = "adapters";

    private static volatile @Nullable Supplier<String> adapterInventory;

    private PhoneHomeData() {}

    public static void adapterInventory(final @Nullable Supplier<String> supplier) {
        adapterInventory = supplier;
    }

    /** The placeholder first-party adapters return from {@code getVersion()}, resolved to the Edge version. */
    static final @NotNull String EDGE_VERSION_PLACEHOLDER = "${edge-version}";

    /**
     * @param edgeVersion replaces {@link #EDGE_VERSION_PLACEHOLDER} in an adapter's version
     * @return the inventory of the given adapters as {@code protocolId:version:count}, one entry per protocol and
     *     version, sorted, comma separated; empty when there are none
     */
    public static @NotNull String inventory(
            final @NotNull Iterable<? extends ProtocolAdapterInformation> adapters, final @NotNull String edgeVersion) {
        final Map<String, Integer> counts = new TreeMap<>();
        for (final ProtocolAdapterInformation info : adapters) {
            final String version = String.valueOf(info.getVersion()).replace(EDGE_VERSION_PLACEHOLDER, edgeVersion);
            counts.merge(info.getProtocolId() + ':' + version, 1, Integer::sum);
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

    static void addTo(final @NotNull Map<String, String> userData, final boolean withAdapters) {
        final String licenseId = System.getProperty(LICENSE_ID_PROPERTY);
        if (licenseId != null && !licenseId.isBlank()) {
            userData.put(LICENSE_ID_KEY, licenseId);
        }
        final Supplier<String> supplier = adapterInventory;
        if (withAdapters && supplier != null) {
            try {
                final String adapters = supplier.get();
                if (adapters != null && !adapters.isEmpty()) {
                    userData.put(ADAPTERS_KEY, adapters);
                }
            } catch (final RuntimeException ignored) {
                // the inventory is best effort; the ping goes without it
            }
        }
    }
}
