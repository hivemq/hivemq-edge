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

import com.hivemq.adapter.sdk.api.v2.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.v2.node.AccessTriState;
import com.hivemq.protocols.v2.config.ProtocolAdapterEntity;
import com.hivemq.protocols.v2.config.TagEntity;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterGoalState;
import com.hivemq.protocols.v2.wrapper.TagAspectActivationPreference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
     * The persisted per-aspect activation preferences keyed by tag name, with the tag's declared
     * {@code <access>} capabilities enforced (EDG-824 #14): an aspect is activatable only when the access model
     * permits it. {@code readable} is the master read gate; {@code pollable} / {@code subscribable} select the read
     * transports under it; {@code writable} gates the write aspect. Only {@link AccessTriState#YES} permits — both
     * {@code NO} and {@code WILL_NOT_USE} (declared unused) forbid the capability at runtime. A config that says
     * <i>do-not-read this point</i> therefore keeps the read aspect {@code DEACTIVATED}: the point is never polled,
     * never subscribed, and never delivered northbound.
     *
     * @param entity the adapter configuration.
     * @return a map of tag name to its effective {@code read-activated} / {@code write-activated} preference, in
     *         declaration order.
     */
    public static @NotNull Map<String, TagAspectActivationPreference> activationOf(
            final @NotNull ProtocolAdapterEntity entity) {
        final Map<String, TagAspectActivationPreference> activation = new LinkedHashMap<>();
        for (final TagEntity tag : entity.getTags()) {
            final boolean readUsable = effectivePollable(tag) || effectiveSubscribable(tag);
            final boolean writeUsable = tag.getAccess().getWritable() == AccessTriState.YES;
            activation.put(
                    tag.getName(),
                    new TagAspectActivationPreference(
                            tag.isReadActivated() && readUsable, tag.isWriteActivated() && writeUsable));
        }
        return activation;
    }

    /**
     * @param tag the tag configuration.
     * @return whether the tag may actually be polled: the transport is declared on the tag AND the access model
     *         permits it ({@code readable} and {@code pollable} both {@link AccessTriState#YES}).
     */
    public static boolean effectivePollable(final @NotNull TagEntity tag) {
        return tag.isPollable()
                && tag.getAccess().getReadable() == AccessTriState.YES
                && tag.getAccess().getPollable() == AccessTriState.YES;
    }

    /**
     * @param tag the tag configuration.
     * @return whether the tag may actually be subscribed: the transport is declared on the tag AND the access model
     *         permits it ({@code readable} and {@code subscribable} both {@link AccessTriState#YES}).
     */
    public static boolean effectiveSubscribable(final @NotNull TagEntity tag) {
        return tag.isSubscribable()
                && tag.getAccess().getReadable() == AccessTriState.YES
                && tag.getAccess().getSubscribable() == AccessTriState.YES;
    }

    /**
     * The capability demands of a configuration that the adapter type's declared capability set does
     * not cover (EDG-824 #17). The declaration is honest and must be honored: a configuration with southbound
     * mappings demands {@link ProtocolAdapterCapability#WRITE}; a configuration with an effectively-subscribable tag
     * demands {@link ProtocolAdapterCapability#SUBSCRIPTIONS}. A configuration the type cannot serve is refused —
     * the adapter surfaces {@code ERROR} instead of starting normally.
     *
     * @param entity       the adapter configuration.
     * @param capabilities the adapter type's declared capability set.
     * @return human-readable descriptions of the missing capabilities, empty when the type can serve the
     *         configuration.
     */
    public static @NotNull List<String> missingCapabilities(
            final @NotNull ProtocolAdapterEntity entity, final @NotNull Set<ProtocolAdapterCapability> capabilities) {
        final List<String> missing = new ArrayList<>();
        if (!entity.getSouthboundMappings().isEmpty() && !capabilities.contains(ProtocolAdapterCapability.WRITE)) {
            missing.add("WRITE (the configuration declares southbound mappings)");
        }
        // Deliberately DECLARED-based, like the WRITE demand above: a tag that declares the subscribe transport
        // demands the capability even when access flags currently forbid it — both demands read what the
        // configuration declares, not what is momentarily effective.
        if (entity.getTags().stream().anyMatch(TagEntity::isSubscribable)
                && !capabilities.contains(ProtocolAdapterCapability.SUBSCRIPTIONS)) {
            missing.add("SUBSCRIPTIONS (the configuration declares subscribable tags)");
        }
        return missing;
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
            // Only tags that can actually be polled contribute: an inert or subscribe-only tag's declared interval
            // must not set the whole adapter's poll clock.
            if (effectivePollable(tag)) {
                shortest = Math.min(shortest, tag.getPollIntervalMillis());
            }
        }
        return shortest == Long.MAX_VALUE ? new TagEntity().getPollIntervalMillis() : shortest;
    }
}
