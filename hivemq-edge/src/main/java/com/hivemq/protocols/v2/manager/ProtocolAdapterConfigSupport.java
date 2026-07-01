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
     * The per-tag poll cadence the running tag coordinator applies to each polled aspect, keyed by tag name. Each
     * tag is scheduled at its own declared {@code poll-interval-millis}, so a slow tag is never overpolled because a
     * fast sibling exists.
     *
     * @param entity the adapter configuration.
     * @return a map of tag name to its declared poll interval, in declaration order.
     */
    public static @NotNull Map<String, Long> pollIntervalMillisByTagName(final @NotNull ProtocolAdapterEntity entity) {
        final Map<String, Long> pollIntervals = new LinkedHashMap<>();
        for (final TagEntity tag : entity.getTags()) {
            pollIntervals.put(tag.getName(), tag.getPollIntervalMillis());
        }
        return pollIntervals;
    }
}
