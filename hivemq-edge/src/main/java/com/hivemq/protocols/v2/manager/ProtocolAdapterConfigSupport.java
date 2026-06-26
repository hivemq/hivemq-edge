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
package com.hivemq.protocols.v2.manager;

import com.hivemq.protocols.v2.config.ProtocolAdapterEntity;
import com.hivemq.protocols.v2.config.TagEntity;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterGoalState;
import com.hivemq.protocols.v2.wrapper.TagAspectActivationPreference;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Pure translations from the read-only {@code <v2>} configuration entities into the
 * runtime value types the wrapper consumes — the parts that need no protocol-specific deserialization (node-string
 * &rarr; {@code Node} translation needs the adapter type's {@code nodeClass} and lives in the wrapper factory).
 * Shared by the manager and the wrapper factory so the config-declared goal and per-tag activation are derived in
 * exactly one place.
 */
public final class ProtocolAdapterConfigSupport {

    private ProtocolAdapterConfigSupport() {}

    /**
     * The config-declared adapter direction goal — the initial goal at startup and the authoritative goal again
     * after a reload that touches the activation flags.
     *
     * @param entity the adapter configuration.
     * @return the goal carrying the entity's {@code northbound-activated} / {@code southbound-activated} flags.
     */
    public static @NotNull ProtocolAdapterGoalState goalOf(final @NotNull ProtocolAdapterEntity entity) {
        return new ProtocolAdapterGoalState(entity.isNorthboundActivated(), entity.isSouthboundActivated());
    }

    /**
     * The persisted per-aspect activation preferences keyed by tag name.
     *
     * @param entity the adapter configuration.
     * @return a map of tag name to its {@code read-activated} / {@code write-activated} preference, in declaration
     *         order.
     */
    public static @NotNull Map<String, TagAspectActivationPreference> activationOf(
            final @NotNull ProtocolAdapterEntity entity) {
        final Map<String, TagAspectActivationPreference> activation = new LinkedHashMap<>();
        for (final TagEntity tag : entity.getTags()) {
            activation.put(
                    tag.getName(), new TagAspectActivationPreference(tag.isReadActivated(), tag.isWriteActivated()));
        }
        return activation;
    }

    /**
     * The single poll cadence the running tag coordinator applies to every polled aspect. The
     * coordinator currently takes one interval per adapter; the configuration carries one per tag, so this reduces
     * them conservatively to the <b>shortest</b> declared interval — no tag then polls slower than its configuration
     * asks, though tags with a longer configured interval poll faster than declared. A per-tag poll cadence is a
     * follow-up tied to the coordinator's single-interval support.
     *
     * @param entity the adapter configuration.
     * @return the shortest declared poll interval, or {@link TagEntity}'s default when the adapter declares no tags.
     */
    public static long pollIntervalMillisOf(final @NotNull ProtocolAdapterEntity entity) {
        long shortest = Long.MAX_VALUE;
        for (final TagEntity tag : entity.getTags()) {
            shortest = Math.min(shortest, tag.getPollIntervalMillis());
        }
        return shortest == Long.MAX_VALUE ? new TagEntity().getPollIntervalMillis() : shortest;
    }
}
