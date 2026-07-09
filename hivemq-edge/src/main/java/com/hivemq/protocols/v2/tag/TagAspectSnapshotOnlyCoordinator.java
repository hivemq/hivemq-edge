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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The minimal {@link TagAspectCoordinator} stand-in: it holds the tag set, activation preferences, and
 * {@code used} derivation, and publishes a minimal per-tag snapshot, but runs <b>no</b> aspect machines. The
 * aspect state names it reports are always {@code DEACTIVATED} and every per-aspect flag is {@code false}, which
 * is honest: with no aspect machines running, every aspect is at rest.
 * <p>
 * It does still drive the adapter's connect-time verification gate so the adapter machine and
 * snapshot publication can be exercised on their own (the adapter-machine tests use it): on connect it verifies
 * <b>all</b> configured nodes through a {@link SharedNodeVerification} with a no-op fan-out (there are no aspects
 * to route results to), and {@link #allReported()} gates {@code CONNECTED}. It must be bound to the adapter's
 * verify seam through {@link #bindVerifier(NodeVerifier)} before the first connect. {@link TagAspectRuntimeCoordinator}
 * is the running implementation that drives real aspect machines, with no change to the wrapper.
 */
public final class TagAspectSnapshotOnlyCoordinator implements TagAspectCoordinator {

    private static final @NotNull String AT_REST_ASPECT_STATE = "DEACTIVATED";

    private @NotNull List<NodeTagPair> nodes;
    private @NotNull Map<String, TagAspectActivationPreference> activation;
    private @NotNull Set<String> readUsedTagNames;
    private @NotNull Set<String> writeUsedTagNames;
    private @Nullable SharedNodeVerification verification;

    /**
     * @param nodes             the initial node/tag pairs.
     * @param activation        the initial per-tag activation preferences.
     * @param readUsedTagNames  the tags consumed by a northbound mapping.
     * @param writeUsedTagNames the tags produced to by a southbound mapping.
     */
    public TagAspectSnapshotOnlyCoordinator(
            final @NotNull List<NodeTagPair> nodes,
            final @NotNull Map<String, TagAspectActivationPreference> activation,
            final @NotNull Set<String> readUsedTagNames,
            final @NotNull Set<String> writeUsedTagNames) {
        this.nodes = List.copyOf(nodes);
        this.activation = new HashMap<>(activation);
        this.readUsedTagNames = new HashSet<>(readUsedTagNames);
        this.writeUsedTagNames = new HashSet<>(writeUsedTagNames);
    }

    /**
     * Bind the adapter's verify seam so the connect gate can issue {@code verifyBatch}. Called once,
     * before the first connect — mirroring {@link TagAspectRuntimeCoordinator#bindRuntime}. The fan-out is a no-op
     * because there are no aspect machines; the verification authority is used purely for the gate count.
     *
     * @param nodeVerifier the adapter's {@code verifyBatch} seam.
     */
    public void bindVerifier(final @NotNull NodeVerifier nodeVerifier) {
        this.verification = new SharedNodeVerification(nodeVerifier, node -> null);
    }

    @Override
    public void onAdapterVerifying() {
        // No aspect machines: verify every configured node so the adapter machine's verification flow is exercised.
        final List<Node> toVerify = new ArrayList<>(nodes.size());
        for (final NodeTagPair pair : nodes) {
            toVerify.add(pair.node());
        }
        verification().beginConnectVerification(toVerify);
    }

    @Override
    public boolean allReported() {
        return verification().allReported();
    }

    @Override
    public void resetVerificationGate() {
        verification().reset();
    }

    @Override
    public void onAdapterReady() {
        // No aspect machines; nothing to start.
    }

    @Override
    public void onAdapterUnavailable() {
        verification().reset();
    }

    @Override
    public void routeVerifyResult(final @NotNull Node node, final @NotNull VerifyOutcome outcome) {
        // No aspects to fan out to; decrement the connect gate so allReported() advances the adapter to CONNECTED.
        verification().onVerifyResult(node, outcome);
    }

    @Override
    public @Nullable DataPoint routeDataPoint(
            final @NotNull Node node, final @NotNull DataPoint value, final @NotNull String adapterId) {
        // No read aspect yet; the value is absorbed.
        return null;
    }

    @Override
    public void routeNodeError(final @NotNull Node node, final @NotNull String reason, final boolean spontaneous) {
        // No read aspect yet; the failure is absorbed.
    }

    @Override
    public void submitWrite(final @NotNull Node node, final @NotNull DataPoint value) {
        // No write aspect; the write is absorbed.
    }

    @Override
    public void routeWriteResult(final @NotNull Node node, final boolean success, final @Nullable String reason) {
        // No write aspect; the acknowledgment is absorbed.
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
                    false, // readAspectGoalActive — no aspect machine, so never active
                    false, // writeAspectGoalActive
                    false, // readAspectOperating
                    false, // writeAspectOperating
                    false, // readAspectPermanentFailure
                    false, // writeAspectPermanentFailure
                    0,
                    null,
                    0L));
        }
        return snapshots;
    }

    private @NotNull SharedNodeVerification verification() {
        return Objects.requireNonNull(
                verification, "bindVerifier() must be called before the snapshot-only coordinator is used");
    }
}
