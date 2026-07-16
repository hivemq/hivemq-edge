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
import com.hivemq.protocols.v2.runtime.BatchCollector;
import com.hivemq.protocols.v2.runtime.Clock;
import com.hivemq.protocols.v2.runtime.DataPointStamping;
import com.hivemq.protocols.v2.runtime.PriorityTimerQueue;
import com.hivemq.protocols.v2.runtime.ProtocolAdapterMetrics;
import com.hivemq.protocols.v2.runtime.RetryPolicy;
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
 * The running {@link TagAspectCoordinator} — the wrapper's view of an adapter's tag aspect machines,
 * backed by a live {@link TagRuntime} per tag. It replaces the snapshot-only stand-in once aspects come online:
 * it routes the events the wrapper hands it to the owning tag, drives every aspect's three-condition goal, and
 * publishes a per-tag snapshot reflecting the real aspect states.
 * <p>
 * It is constructed from configuration alone and then <b>bound</b> to the actor's runtime through
 * {@link #bindRuntime} — the actor's clock, single timer queue, batch collector, metrics, and the verify seam —
 * mirroring the wrapper context's own {@code bindMachine}. Binding builds the {@link SharedNodeVerification} and
 * the per-tag runtimes and applies the initial goals. Everything runs on the wrapper's single dispatch thread.
 */
public final class TagAspectRuntimeCoordinator implements TagAspectCoordinator {

    private record BoundRuntime(
            @NotNull Clock clock,
            @NotNull PriorityTimerQueue timers,
            @NotNull BatchCollector batches,
            @NotNull ProtocolAdapterMetrics metrics,
            @NotNull SharedNodeVerification sharedNodeVerification) {}

    private final @NotNull String adapterId;
    private final long pollIntervalMillis;
    private final @NotNull RetryPolicy retryPolicy;
    private final @NotNull TagWriteReadinessListener readinessListener;

    private @NotNull List<NodeTagPair> nodes;
    private @NotNull Map<String, TagAspectActivationPreference> activation;
    private @NotNull Set<String> readUsedTagNames;
    private @NotNull Set<String> writeUsedTagNames;
    private @NotNull ProtocolAdapterGoalState goal;

    private @Nullable BoundRuntime runtime;
    private @NotNull List<TagRuntime> tagRuntimes = new ArrayList<>();
    private @NotNull Map<Node, TagRuntime> tagRuntimesByNode = new HashMap<>();

    /**
     * @param adapterId          the owning adapter's id.
     * @param nodes              the configured node/tag pairs.
     * @param activation         the per-tag activation preferences.
     * @param readUsedTagNames   the tags consumed by a northbound mapping.
     * @param writeUsedTagNames  the tags produced to by a southbound mapping.
     * @param initialGoal        the initial adapter direction goal (from configuration).
     * @param pollIntervalMillis the poll cadence for polled read aspects, in milliseconds.
     * @param retryPolicy        the backoff policy for verification and subscription retries.
     */
    public TagAspectRuntimeCoordinator(
            final @NotNull String adapterId,
            final @NotNull List<NodeTagPair> nodes,
            final @NotNull Map<String, TagAspectActivationPreference> activation,
            final @NotNull Set<String> readUsedTagNames,
            final @NotNull Set<String> writeUsedTagNames,
            final @NotNull ProtocolAdapterGoalState initialGoal,
            final long pollIntervalMillis,
            final @NotNull RetryPolicy retryPolicy) {
        this(
                adapterId,
                nodes,
                activation,
                readUsedTagNames,
                writeUsedTagNames,
                initialGoal,
                pollIntervalMillis,
                retryPolicy,
                TagWriteReadinessListener.NONE);
    }

    /**
     * @param adapterId          the owning adapter's id.
     * @param nodes              the configured node/tag pairs.
     * @param activation         the per-tag activation preferences.
     * @param readUsedTagNames   the tags consumed by a northbound mapping.
     * @param writeUsedTagNames  the tags produced to by a southbound mapping.
     * @param initialGoal        the initial adapter direction goal (from configuration).
     * @param pollIntervalMillis the poll cadence for polled read aspects, in milliseconds.
     * @param retryPolicy        the backoff policy for verification and subscription retries.
     * @param readinessListener  notified when a write aspect crosses its writability boundary — the seam the
     *                           southbound delivery side hangs its suspend/resume on.
     */
    public TagAspectRuntimeCoordinator(
            final @NotNull String adapterId,
            final @NotNull List<NodeTagPair> nodes,
            final @NotNull Map<String, TagAspectActivationPreference> activation,
            final @NotNull Set<String> readUsedTagNames,
            final @NotNull Set<String> writeUsedTagNames,
            final @NotNull ProtocolAdapterGoalState initialGoal,
            final long pollIntervalMillis,
            final @NotNull RetryPolicy retryPolicy,
            final @NotNull TagWriteReadinessListener readinessListener) {
        this.adapterId = adapterId;
        this.readinessListener = readinessListener;
        this.nodes = List.copyOf(nodes);
        this.activation = new HashMap<>(activation);
        this.readUsedTagNames = new HashSet<>(readUsedTagNames);
        this.writeUsedTagNames = new HashSet<>(writeUsedTagNames);
        this.goal = initialGoal;
        this.pollIntervalMillis = pollIntervalMillis;
        this.retryPolicy = retryPolicy;
    }

    /**
     * Bind the actor's runtime — its clock, single timer queue, batch collector, metrics, and the verify seam —
     * then build the verification coordinator and the per-tag runtimes and apply the initial goals. Called once,
     * after the wrapper context exists and before any message is handled (the two-phase init mirrors the context's
     * {@code bindMachine}).
     *
     * @param clock           the actor clock the aspect timers are scheduled against.
     * @param timers          the actor's single timer queue.
     * @param batches         the actor's batch collector.
     * @param metrics         the per-adapter metrics.
     * @param nodeVerifier the seam re-verifications are issued through — the adapter's {@code verifyBatch}.
     */
    public void bindRuntime(
            final @NotNull Clock clock,
            final @NotNull PriorityTimerQueue timers,
            final @NotNull BatchCollector batches,
            final @NotNull ProtocolAdapterMetrics metrics,
            final @NotNull NodeVerifier nodeVerifier) {
        final SharedNodeVerification sharedNodeVerification =
                new SharedNodeVerification(nodeVerifier, this::findTagRuntime);
        this.runtime = new BoundRuntime(clock, timers, batches, metrics, sharedNodeVerification);
        rebuildTagRuntimes();
        applyGoalToAll();
    }

    // ── adapter-readiness coupling ──────────────────────────────────────────────────────────

    @Override
    public void onAdapterVerifying() {
        // Move active aspects into verification, then issue the nodes they need as ONE connect-verification batch
        // through the shared authority — a read-and-write tag therefore yields a single verifyBatch entry.
        final List<Node> toVerify = new ArrayList<>();
        for (final TagRuntime tagRuntime : tagRuntimes) {
            tagRuntime.onAdapterVerifying();
            if (tagRuntime.needsConnectVerification()) {
                toVerify.add(tagRuntime.node());
            }
        }
        runtime().sharedNodeVerification().beginConnectVerification(toVerify);
    }

    @Override
    public boolean allReported() {
        return runtime().sharedNodeVerification().allReported();
    }

    @Override
    public void resetVerificationGate() {
        runtime().sharedNodeVerification().reset();
    }

    @Override
    public void onAdapterReady() {
        for (final TagRuntime tagRuntime : tagRuntimes) {
            tagRuntime.onAdapterReady();
        }
    }

    @Override
    public void onAdapterUnavailable() {
        runtime().sharedNodeVerification().reset();
        for (final TagRuntime tagRuntime : tagRuntimes) {
            tagRuntime.onAdapterUnavailable();
        }
    }

    // ── routed events ───────────────────────────────────────────────────────────────────────────────

    @Override
    public void routeVerifyResult(final @NotNull Node node, final @NotNull VerifyOutcome outcome) {
        runtime().sharedNodeVerification().onVerifyResult(node, outcome);
    }

    @Override
    public @Nullable DataPoint routeDataPoint(
            final @NotNull Node node, final @NotNull DataPoint value, final @NotNull String adapterId) {
        final TagRuntime tagRuntime = findTagRuntime(node);
        if (tagRuntime != null) {
            if (tagRuntime.onValue(value)) {
                return DataPointStamping.stamp(value, tagRuntime.pair().tag(), adapterId);
            }
        }
        return null;
    }

    @Override
    public void routeNodeError(final @NotNull Node node, final @NotNull String reason, final boolean spontaneous) {
        final TagRuntime tagRuntime = findTagRuntime(node);
        if (tagRuntime != null) {
            tagRuntime.onNodeError(reason, spontaneous);
        }
    }

    @Override
    public void submitWrite(
            final @NotNull Node node,
            final @NotNull DataPoint value,
            final @NotNull SouthboundWriteCompletion completion) {
        final TagRuntime tagRuntime = findTagRuntime(node);
        if (tagRuntime != null) {
            tagRuntime.submitWrite(value, completion);
        } else {
            // No such tag under this adapter: settle so a back-pressuring producer is never left waiting.
            completion.settle(SouthboundWriteOutcome.REJECTED_BUSY);
        }
    }

    @Override
    public void routeWriteResult(final @NotNull Node node, final boolean success, final @Nullable String reason) {
        final TagRuntime tagRuntime = findTagRuntime(node);
        if (tagRuntime != null) {
            tagRuntime.onWriteResult(success, reason);
        }
    }

    // ── configuration transitions ─────────────────────────────────────────────────────────────────

    @Override
    public void applyActivation(
            final @NotNull ProtocolAdapterGoalState adapterDirections,
            final @NotNull Map<String, TagAspectActivationPreference> tagActivation) {
        this.goal = adapterDirections;
        this.activation = new HashMap<>(tagActivation);
        applyGoalToAll();
    }

    @Override
    public void updateTagSet(
            final @NotNull List<NodeTagPair> newNodes,
            final @NotNull Map<String, TagAspectActivationPreference> newActivation,
            final @NotNull Set<String> newReadUsedTagNames,
            final @NotNull Set<String> newWriteUsedTagNames) {
        // Tear down the current aspects (cancel their timers, drop subscriptions) before rebuilding, so no timer
        // outlives its tag. Preserving surviving tags untouched is a gentlest-transition refinement for the PAM
        // task; here a tags-only change re-verifies the new set without ever reconnecting the adapter.
        deactivateAll();
        this.nodes = List.copyOf(newNodes);
        this.activation = new HashMap<>(newActivation);
        this.readUsedTagNames = new HashSet<>(newReadUsedTagNames);
        this.writeUsedTagNames = new HashSet<>(newWriteUsedTagNames);
        rebuildTagRuntimes();
        applyGoalToAll();
    }

    @Override
    public void retryTag(final @NotNull String tagName) {
        for (final TagRuntime tagRuntime : tagRuntimes) {
            if (tagRuntime.tagName().equals(tagName)) {
                tagRuntime.retry();
            }
        }
    }

    @Override
    public @NotNull List<TagStatusSnapshot> tagSnapshots() {
        final List<TagStatusSnapshot> snapshots = new ArrayList<>(tagRuntimes.size());
        for (final TagRuntime tagRuntime : tagRuntimes) {
            snapshots.add(tagRuntime.snapshot());
        }
        return snapshots;
    }

    // ── internals ───────────────────────────────────────────────────────────────────────────────────────────────

    private @NotNull BoundRuntime runtime() {
        return Objects.requireNonNull(runtime, "bindRuntime() must be called before the coordinator is used");
    }

    private @Nullable TagRuntime findTagRuntime(final @NotNull Node node) {
        return tagRuntimesByNode.get(node);
    }

    private void rebuildTagRuntimes() {
        final BoundRuntime bound = runtime();
        final List<TagRuntime> rebuilt = new ArrayList<>(nodes.size());
        final Map<Node, TagRuntime> byNode = new HashMap<>();
        for (final NodeTagPair pair : nodes) {
            final TagRuntime tagRuntime = new TagRuntime(
                    adapterId,
                    pair,
                    bound.clock(),
                    bound.timers(),
                    bound.batches(),
                    bound.metrics(),
                    bound.sharedNodeVerification(),
                    readinessListener,
                    pollIntervalMillis,
                    retryPolicy);
            rebuilt.add(tagRuntime);
            byNode.put(pair.node(), tagRuntime);
        }
        this.tagRuntimes = rebuilt;
        this.tagRuntimesByNode = byNode;
    }

    private void deactivateAll() {
        for (final TagRuntime tagRuntime : tagRuntimes) {
            tagRuntime.deactivate();
        }
    }

    private void applyGoalToAll() {
        for (final TagRuntime tagRuntime : tagRuntimes) {
            final String tagName = tagRuntime.tagName();
            final TagAspectActivationPreference preference =
                    activation.getOrDefault(tagName, TagAspectActivationPreference.defaults());
            tagRuntime.applyActivation(
                    goal.northboundActivated(),
                    goal.southboundActivated(),
                    preference.readActivated(),
                    preference.writeActivated(),
                    readUsedTagNames.contains(tagName),
                    writeUsedTagNames.contains(tagName));
        }
    }
}
