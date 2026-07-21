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
package com.hivemq.protocols.v2.tag;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.v2.model.VerifyOutcome;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.protocols.v2.view.TagStatusSnapshot;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterGoalState;
import com.hivemq.protocols.v2.wrapper.TagAspectActivationPreference;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The wrapper's view of all of an adapter's tag aspect machines — the seam between the adapter machine (the
 * {@code wrapper} package) and the per-tag read/write aspect machines (this {@code tag} package). The wrapper
 * invokes these hooks at the points where aspects react; it never reaches into aspect internals.
 * <p>
 * {@link TagAspectSnapshotOnlyCoordinator} is the minimal stand-in — tag set and activation only, no aspect
 * machines — that the adapter-machine tests use; {@link TagAspectRuntimeCoordinator} is the running implementation
 * backed by per-tag {@link TagRuntime}s. The hook set is the full wrapper-to-aspect contract.
 * <p>
 * Every method runs on the wrapper's single dispatch thread.
 */
public interface TagAspectCoordinator {

    /**
     * The adapter began verifying its nodes: active aspects waiting for the adapter move into
     * verification, and the coordinator issues the nodes those aspects need verified as <b>one</b>
     * {@code verifyBatch} through the shared verification authority. The single verify stream then feeds both the
     * adapter gate (via {@link #allReported()}) and the aspects (via {@link #routeVerifyResult}).
     */
    void onAdapterVerifying();

    /**
     * @return {@code true} when no connect-time verification is outstanding — the signal the wrapper uses to leave
     *         {@code WAITING_FOR_VERIFICATION} for {@code CONNECTED} (the adapter gate).
     */
    boolean allReported();

    /**
     * Drop any outstanding connect-time verification — called when verification is abandoned (the adapter
     * disconnected, errored, or stopped), so a stale request never lingers into the next connect gate.
     */
    void resetVerificationGate();

    /**
     * The adapter reached {@code CONNECTED}. When verification was skipped, aspects still waiting for
     * the adapter treat the connection as verified and begin operating; otherwise they have already advanced
     * through verification on the routed results and this is a no-op for them.
     */
    void onAdapterReady();

    /**
     * The adapter is no longer connected (disconnect, connection loss, stop, or error): all non-deactivated
     * aspects return to waiting for the adapter.
     */
    void onAdapterUnavailable();

    /**
     * Route one node's verification outcome to that node's aspects. The adapter gate's counting is
     * separate and lives in the wrapper.
     *
     * @param node    the verified node.
     * @param outcome the verification outcome.
     */
    void routeVerifyResult(@NotNull Node node, @NotNull VerifyOutcome outcome);

    /**
     * Route one value to the node's read aspect.
     *
     * @param node  the node the value belongs to.
     * @param value the reused v1 value.
     * @param adapterId the adapter instance id used to stamp values accepted by the read aspect.
     * @return a stamped data point for northbound consumers when the read aspect accepted the value; {@code null}
     *         for unknown nodes or values ignored by the aspect state machine.
     */
    @Nullable
    DataPoint routeDataPoint(@NotNull Node node, @NotNull DataPoint value, @NotNull String adapterId);

    /**
     * Route a per-node failure to the node's read aspect.
     *
     * @param node        the node the failure belongs to.
     * @param reason      a human-readable description.
     * @param spontaneous whether the failure arrived outside a command-response exchange.
     */
    void routeNodeError(@NotNull Node node, @NotNull String reason, boolean spontaneous);

    /**
     * Submit a southbound write to the node's write aspect — the "write arrives" trigger.
     *
     * @param node       the node to write to.
     * @param value      the reused v1 value to write.
     * @param completion the one-shot back-pressure signal, settled with the write's outcome.
     */
    void submitWrite(@NotNull Node node, @NotNull DataPoint value, @NotNull SouthboundWriteCompletion completion);

    /**
     * Route a write acknowledgment to the node's write aspect.
     *
     * @param node    the node the write targeted.
     * @param success whether the write succeeded.
     * @param reason  the failure reason, or {@code null} on success.
     */
    void routeWriteResult(@NotNull Node node, boolean success, @Nullable String reason);

    /**
     * Apply changed activation flags atomically — the activation-only transition. Recomputes aspect
     * goals without reconnecting or re-verifying unaffected aspects.
     *
     * @param adapterDirections the new adapter direction goal.
     * @param tagActivation     the new per-tag activation preferences.
     */
    void applyActivation(
            @NotNull ProtocolAdapterGoalState adapterDirections,
            @NotNull Map<String, TagAspectActivationPreference> tagActivation);

    /**
     * Replace the tag set in place — the tags-only transition. Never reconnects.
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
     * Retry a permanently-failed tag: each of its aspects in permanent verification
     * failure resets and re-verifies; other aspects are skipped.
     *
     * @param tagName the tag to retry.
     */
    void retryTag(@NotNull String tagName);

    /**
     * @return the per-tag status snapshots for publication, one per tag in the current set.
     */
    @NotNull
    List<TagStatusSnapshot> tagSnapshots();
}
