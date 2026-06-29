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
package com.hivemq.protocols.v2.wrapper;

import com.hivemq.adapter.sdk.api.v2.messaging.MailboxMessagePriority;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * A goal or lifecycle command. Commands are valid in <em>every</em> state: the wrapper handles them
 * through {@link com.hivemq.protocols.v2.fsm.FSM#onGoalChange(Runnable)} — mutate the goal,
 * then {@code stepTowardGoal} — so they never reach the transition table and can never trigger a defensive reset
 *.
 * <p>
 * Band: {@link MailboxMessagePriority#CONTROL} — delivered ahead of events, ticks, and data. Jumping the queue is
 * always safe precisely because a command bypasses the table.
 * <p>
 * Origins: {@link ActivateDirection} / {@link DeactivateDirection} and {@link RetryTag} come from REST (runtime
 * state); {@link UpdateTagSet} and {@link ApplyActivation} come from the manager's config-difference
 * classification (a later task); {@link StopAdapter} comes from the manager (removal / shutdown).
 */
public sealed interface ProtocolAdapterWrapperCommand extends ProtocolAdapterWrapperMessage {

    @Override
    default @NotNull MailboxMessagePriority priority() {
        return MailboxMessagePriority.CONTROL;
    }

    /**
     * Activate a direction's goal (REST origin) — a live goal change, never persisted.
     *
     * @param direction the direction to activate.
     */
    record ActivateDirection(@NotNull ProtocolAdapterDirection direction) implements ProtocolAdapterWrapperCommand {}

    /**
     * Deactivate a direction's goal (REST origin) — a live goal change, never persisted.
     *
     * @param direction the direction to deactivate.
     */
    record DeactivateDirection(@NotNull ProtocolAdapterDirection direction) implements ProtocolAdapterWrapperCommand {}

    /**
     * Stop the adapter (manager origin: removal or shutdown). Drives the goal to "stopped".
     */
    record StopAdapter() implements ProtocolAdapterWrapperCommand {}

    /**
     * Replace the tag set in place — the atomic tags-only transition. Never reconnects.
     *
     * @param nodes             the new node/tag pairs.
     * @param activation        the per-tag activation preferences from the configuration.
     * @param readUsedTagNames  the tags consumed by at least one northbound mapping (the {@code readUsed}
     *                          derivation).
     * @param writeUsedTagNames the tags produced to by at least one southbound mapping (the {@code writeUsed}
     *                          derivation).
     */
    record UpdateTagSet(
            @NotNull List<NodeTagPair> nodes,
            @NotNull Map<String, TagAspectActivationPreference> activation,
            @NotNull Set<String> readUsedTagNames,
            @NotNull Set<String> writeUsedTagNames)
            implements ProtocolAdapterWrapperCommand {}

    /**
     * Apply changed activation flags atomically — the activation-only transition. Never reconnects
     * and never re-verifies unaffected aspects.
     *
     * @param adapterDirections the new adapter direction goal.
     * @param tagActivation     the new per-tag activation preferences.
     */
    record ApplyActivation(
            @NotNull ProtocolAdapterGoalState adapterDirections,
            @NotNull Map<String, TagAspectActivationPreference> tagActivation)
            implements ProtocolAdapterWrapperCommand {}

    /**
     * Retry a permanently-failed tag (REST origin) — a runtime-only command out of permanent
     * verification failure; never touches configuration.
     *
     * @param tagName the tag to retry.
     */
    record RetryTag(@NotNull String tagName) implements ProtocolAdapterWrapperCommand {}
}
