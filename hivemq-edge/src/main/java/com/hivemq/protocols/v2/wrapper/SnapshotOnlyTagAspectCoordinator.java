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

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.v2.model.VerifyOutcome;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.protocols.v2.view.TagStatusSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The {@link TagAspectCoordinator} this task ships: it holds the tag set, activation preferences, and {@code used}
 * derivation, and publishes a minimal per-tag snapshot, but runs no aspect machines. Every routing and lifecycle
 * hook is a no-op — the read and write aspect machines that consume them are later tasks. The aspect state names
 * it reports are always {@code DEACTIVATED}, which is honest: with no aspect machines running, every aspect is at
 * rest.
 * <p>
 * It exists so the adapter machine and snapshot publication can be built and tested in full now; a later task
 * replaces it with a real tag plane without changing the wrapper.
 */
public final class SnapshotOnlyTagAspectCoordinator implements TagAspectCoordinator {

    private static final @NotNull String AT_REST_ASPECT_STATE = "DEACTIVATED";

    private @NotNull List<NodeTagPair> nodes;
    private @NotNull Map<String, TagAspectActivationPreference> activation;
    private @NotNull Set<String> readUsedTagNames;
    private @NotNull Set<String> writeUsedTagNames;

    /**
     * @param nodes             the initial node/tag pairs.
     * @param activation        the initial per-tag activation preferences.
     * @param readUsedTagNames  the tags consumed by a northbound mapping.
     * @param writeUsedTagNames the tags produced to by a southbound mapping.
     */
    public SnapshotOnlyTagAspectCoordinator(
            final @NotNull List<NodeTagPair> nodes,
            final @NotNull Map<String, TagAspectActivationPreference> activation,
            final @NotNull Set<String> readUsedTagNames,
            final @NotNull Set<String> writeUsedTagNames) {
        this.nodes = List.copyOf(nodes);
        this.activation = new HashMap<>(activation);
        this.readUsedTagNames = new HashSet<>(readUsedTagNames);
        this.writeUsedTagNames = new HashSet<>(writeUsedTagNames);
    }

    @Override
    public void onAdapterReady() {
        // No aspect machines yet; nothing to start.
    }

    @Override
    public void onAdapterUnavailable() {
        // No aspect machines yet; nothing to suspend.
    }

    @Override
    public void routeVerifyResult(final @NotNull Node node, final @NotNull VerifyOutcome outcome) {
        // No aspect machines yet; the adapter gate's counting is handled by the wrapper.
    }

    @Override
    public void routeDataPoint(final @NotNull Node node, final @NotNull DataPoint value) {
        // No read aspect yet; the value is absorbed.
    }

    @Override
    public void routeNodeError(final @NotNull Node node, final @NotNull String reason, final boolean spontaneous) {
        // No read aspect yet; the failure is absorbed.
    }

    @Override
    public void routeWriteResult(final @NotNull Node node, final boolean success, final @Nullable String reason) {
        // No write aspect yet; the acknowledgment is absorbed.
    }

    @Override
    public void onTick(final long nowMillis) {
        // No aspect machines yet; nothing to schedule.
    }

    @Override
    public void applyActivation(
            final @NotNull ProtocolAdapterGoalState adapterDirections,
            final @NotNull Map<String, TagAspectActivationPreference> tagActivation) {
        this.activation = new HashMap<>(tagActivation);
    }

    @Override
    public void updateTagSet(
            final @NotNull List<NodeTagPair> nodes,
            final @NotNull Map<String, TagAspectActivationPreference> activation,
            final @NotNull Set<String> readUsedTagNames,
            final @NotNull Set<String> writeUsedTagNames) {
        this.nodes = List.copyOf(nodes);
        this.activation = new HashMap<>(activation);
        this.readUsedTagNames = new HashSet<>(readUsedTagNames);
        this.writeUsedTagNames = new HashSet<>(writeUsedTagNames);
    }

    @Override
    public void retryTag(final @NotNull String tagName) {
        // No aspect machines yet; nothing to retry.
    }

    @Override
    public @NotNull List<TagStatusSnapshot> tagSnapshots() {
        final List<TagStatusSnapshot> snapshots = new ArrayList<>(nodes.size());
        for (final NodeTagPair pair : nodes) {
            final String tagName = pair.tag().name();
            final TagAspectActivationPreference tagActivation =
                    activation.getOrDefault(tagName, TagAspectActivationPreference.defaults());
            snapshots.add(new TagStatusSnapshot(
                    tagName,
                    tagActivation.readActivated(),
                    tagActivation.writeActivated(),
                    readUsedTagNames.contains(tagName),
                    writeUsedTagNames.contains(tagName),
                    AT_REST_ASPECT_STATE,
                    AT_REST_ASPECT_STATE,
                    0,
                    null,
                    0L));
        }
        return snapshots;
    }
}
