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

import com.hivemq.protocols.v2.config.AccessFlagsEntity;
import com.hivemq.protocols.v2.config.NorthboundMappingEntity;
import com.hivemq.protocols.v2.config.ProtocolAdapterEntity;
import com.hivemq.protocols.v2.config.RetryPolicyEntity;
import com.hivemq.protocols.v2.config.SouthboundMappingEntity;
import com.hivemq.protocols.v2.config.TagEntity;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Classifies the difference between an adapter's running configuration and a freshly-loaded one into the gentlest
 * correct {@link ProtocolAdapterConfigStateTransition}. A pure function of the two entities — it touches no runtime state,
 * so it is exhaustively unit-testable on its own.
 * <p>
 * The classification is layered, gentlest last to win only when nothing more disruptive changed:
 * <ol>
 * <li>any <b>connection-critical</b> field differs (protocol id, config version, skip-verification, adapter
 * configuration, retry policy, watchdog / command timeouts, southbound write-backlog capacity, southbound
 * mappings — the last two are baked into the adapter's southbound write plane and MQTT intake) &rarr;
 * {@link ProtocolAdapterConfigStateTransition#FULL_RECREATE};</li>
 * <li>otherwise, if the <b>tag set</b> (a tag's identity beyond its activation flags) or the <b>northbound
 * mappings</b> (which drive {@code read-used}) differ &rarr; {@link ProtocolAdapterConfigStateTransition#TAGS_ONLY};</li>
 * <li>otherwise, if any <b>activation flag</b> differs (adapter {@code northbound-activated} /
 * {@code southbound-activated} or a tag's {@code read-activated} / {@code write-activated}) &rarr;
 * {@link ProtocolAdapterConfigStateTransition#ACTIVATION_ONLY};</li>
 * <li>otherwise &rarr; {@link ProtocolAdapterConfigStateTransition#NO_CHANGE}.</li>
 * </ol>
 * A {@code TAGS_ONLY} difference may carry activation changes too; the manager applies the tag-set update and, only
 * when the adapter-level direction goal also changed, follows it with an activation command (the REST live goal is
 * otherwise preserved).
 */
public final class ProtocolAdapterConfigDiffUtils {

    private ProtocolAdapterConfigDiffUtils() {}

    /**
     * Classify the difference between two configurations of the same adapter (same {@code adapter-id}).
     *
     * @param running the configuration currently applied to the running adapter.
     * @param updated the freshly-loaded configuration.
     * @return the gentlest transition that brings the running adapter in line with {@code updated}.
     */
    public static @NotNull ProtocolAdapterConfigStateTransition classify(
            final @NotNull ProtocolAdapterEntity running, final @NotNull ProtocolAdapterEntity updated) {
        if (!connectionCritical(running).equals(connectionCritical(updated))) {
            return ProtocolAdapterConfigStateTransition.FULL_RECREATE;
        }
        if (!tagSetIdentity(running).equals(tagSetIdentity(updated)) || mappingsChanged(running, updated)) {
            return ProtocolAdapterConfigStateTransition.TAGS_ONLY;
        }
        if (activationChanged(running, updated)) {
            return ProtocolAdapterConfigStateTransition.ACTIVATION_ONLY;
        }
        return ProtocolAdapterConfigStateTransition.NO_CHANGE;
    }

    /**
     * @param running the running configuration.
     * @param updated the freshly-loaded configuration.
     * @return whether the adapter-level direction activation (the {@code northbound-activated} /
     *         {@code southbound-activated} goal) differs between the two — the case in which a
     *         {@link ProtocolAdapterConfigStateTransition#TAGS_ONLY} transition must also re-assert the config-declared direction goal.
     */
    public static boolean adapterDirectionChanged(
            final @NotNull ProtocolAdapterEntity running, final @NotNull ProtocolAdapterEntity updated) {
        return running.isNorthboundActivated() != updated.isNorthboundActivated()
                || running.isSouthboundActivated() != updated.isSouthboundActivated();
    }

    // ── comparison projections ──────────────────────────────────────────────────────────────────────────────────

    /**
     * The fields whose change forces a full recreate — everything baked into the adapter instance or its connection
     * policy, independent of tags, mappings, and activation.
     */
    private record ConnectionCritical(
            @NotNull String protocolId,
            int configVersion,
            boolean skipVerification,
            @NotNull Map<String, Object> adapterConfiguration,
            @NotNull RetryPolicyEntity retryPolicy,
            long watchdogTimeoutMillis,
            long commandTimeoutMillis,
            int southboundWriteBacklogCapacity,
            @NotNull List<SouthboundMappingEntity> southboundMappings) {}

    private static @NotNull ConnectionCritical connectionCritical(final @NotNull ProtocolAdapterEntity entity) {
        return new ConnectionCritical(
                entity.getProtocolId(),
                entity.getConfigVersion(),
                entity.isSkipVerification(),
                entity.getAdapterConfiguration(),
                entity.getRetryPolicy(),
                entity.getWatchdogTimeoutMillis(),
                entity.getCommandTimeoutMillis(),
                // The backlog bound is baked into the southbound write plane at creation; changing it rebuilds the
                // adapter (and deliberately drops the interim in-memory backlogs with it).
                entity.getSouthboundWriteBacklogCapacity(),
                // Southbound mappings define the MQTT intake subscriptions and durable queues, baked in at creation;
                // a change recreates the adapter — the durable queues themselves survive the recreate.
                entity.getSouthboundMappings());
    }

    /**
     * A tag's identity excluding its activation flags — what makes it a different tag rather than the same tag with
     * a flipped preference. Comparing the per-name identity map detects tags added, removed, or edited.
     */
    private record TagIdentity(
            @NotNull String nodeString,
            boolean pollable,
            boolean subscribable,
            long pollIntervalMillis,
            @NotNull AccessFlagsEntity access) {}

    private static @NotNull Map<String, TagIdentity> tagSetIdentity(final @NotNull ProtocolAdapterEntity entity) {
        final Map<String, TagIdentity> byName = new LinkedHashMap<>();
        for (final TagEntity tag : entity.getTags()) {
            byName.put(
                    tag.getName(),
                    new TagIdentity(
                            tag.getNodeString(),
                            tag.isPollable(),
                            tag.isSubscribable(),
                            tag.getPollIntervalMillis(),
                            tag.getAccess()));
        }
        return byName;
    }

    private static boolean mappingsChanged(
            final @NotNull ProtocolAdapterEntity running, final @NotNull ProtocolAdapterEntity updated) {
        // Northbound only: southbound mappings are connection-critical (they define the adapter's MQTT intake
        // subscriptions and durable queues, which never mutate in place — see SouthboundMqttIntake).
        final List<NorthboundMappingEntity> runningNorth = running.getNorthboundMappings();
        final List<NorthboundMappingEntity> updatedNorth = updated.getNorthboundMappings();
        return !runningNorth.equals(updatedNorth);
    }

    private static boolean activationChanged(
            final @NotNull ProtocolAdapterEntity running, final @NotNull ProtocolAdapterEntity updated) {
        if (adapterDirectionChanged(running, updated)) {
            return true;
        }
        return !tagActivation(running).equals(tagActivation(updated));
    }

    private static @NotNull Map<String, List<Boolean>> tagActivation(final @NotNull ProtocolAdapterEntity entity) {
        final Map<String, List<Boolean>> byName = new LinkedHashMap<>();
        for (final TagEntity tag : entity.getTags()) {
            byName.put(tag.getName(), List.of(tag.isReadActivated(), tag.isWriteActivated()));
        }
        return byName;
    }
}
