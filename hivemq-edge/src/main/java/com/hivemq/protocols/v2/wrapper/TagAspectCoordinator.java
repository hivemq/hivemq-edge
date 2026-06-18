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
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The wrapper's view of all its tag aspect machines — the seam between the adapter machine (this task) and the
 * per-tag read/write aspect machines (later tasks). The wrapper invokes these hooks at the points where aspects
 * react: it never reaches into aspect internals.
 * <p>
 * This task ships {@link SnapshotOnlyTagAspectCoordinator}, a minimal implementation that holds the tag set and activation but runs
 * no aspect machines. A later task supplies a real {@code TagAspectCoordinator} backed by per-tag aspect runtimes; the
 * adapter machine and snapshot publication built here do not change. The hook set is therefore deliberately the
 * full wrapper-to-aspect contract, even though most hooks are no-ops for now.
 * <p>
 * Every method runs on the wrapper's single dispatch thread.
 */
public interface TagAspectCoordinator {

    /**
     * The adapter reached {@code CONNECTED}: aspects may begin verifying and operating (design §7.2).
     */
    void onAdapterReady();

    /**
     * The adapter is no longer connected (disconnect, connection loss, stop, or error): all non-deactivated
     * aspects return to waiting for the adapter (design §7.2).
     */
    void onAdapterUnavailable();

    /**
     * Route one node's verification outcome to that node's aspects (design §6.3). The adapter gate's counting is
     * separate and lives in the wrapper.
     *
     * @param node    the verified node.
     * @param outcome the verification outcome.
     */
    void routeVerifyResult(@NotNull Node node, @NotNull VerifyOutcome outcome);

    /**
     * Route one value to the node's read aspect (design §7.3, §7.4).
     *
     * @param node  the node the value belongs to.
     * @param value the reused v1 value.
     */
    void routeDataPoint(@NotNull Node node, @NotNull DataPoint value);

    /**
     * Route a per-node failure to the node's read aspect (design §7.4).
     *
     * @param node        the node the failure belongs to.
     * @param reason      a human-readable description.
     * @param spontaneous whether the failure arrived outside a command-response exchange.
     */
    void routeNodeError(@NotNull Node node, @NotNull String reason, boolean spontaneous);

    /**
     * Route a write acknowledgment to the node's write aspect (design §7.5).
     *
     * @param node    the node the write targeted.
     * @param success whether the write succeeded.
     * @param reason  the failure reason, or {@code null} on success.
     */
    void routeWriteResult(@NotNull Node node, boolean success, @Nullable String reason);

    /**
     * A tick elapsed: aspects post their due work (polls, subscription retries) to the batch collector before it
     * is dispatched (design §5.7, §7.3).
     *
     * @param nowMillis the tick's logical time, in milliseconds.
     */
    void onTick(long nowMillis);

    /**
     * Apply changed activation flags atomically — the activation-only transition (design §8.2). Recomputes aspect
     * goals without reconnecting or re-verifying unaffected aspects.
     *
     * @param adapterDirections the new adapter direction goal.
     * @param tagActivation     the new per-tag activation preferences.
     */
    void applyActivation(
            @NotNull ProtocolAdapterGoalState adapterDirections,
            @NotNull Map<String, TagAspectActivationPreference> tagActivation);

    /**
     * Replace the tag set in place — the tags-only transition (design §8.2). Never reconnects.
     *
     * @param nodes             the new node/tag pairs.
     * @param activation        the per-tag activation preferences.
     * @param readUsedTagNames  the tags consumed by a northbound mapping.
     * @param writeUsedTagNames the tags produced to by a southbound mapping.
     */
    void updateTagSet(
            @NotNull List<NodeTagPair> nodes,
            @NotNull Map<String, TagAspectActivationPreference> activation,
            @NotNull Set<String> readUsedTagNames,
            @NotNull Set<String> writeUsedTagNames);

    /**
     * Retry a permanently-failed tag (design §7.6, EDG-462): each of its aspects in permanent verification
     * failure resets and re-verifies; other aspects are skipped.
     *
     * @param tagName the tag to retry.
     */
    void retryTag(@NotNull String tagName);

    /**
     * @return the per-tag status snapshots for publication (design §6.6), one per tag in the current set.
     */
    @NotNull
    List<TagStatusSnapshot> tagSnapshots();
}
