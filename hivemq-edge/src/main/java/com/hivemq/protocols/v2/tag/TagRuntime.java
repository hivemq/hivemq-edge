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
import com.hivemq.protocols.v2.runtime.PriorityTimerQueue;
import com.hivemq.protocols.v2.runtime.ProtocolAdapterMetrics;
import com.hivemq.protocols.v2.runtime.RetryPolicy;
import com.hivemq.protocols.v2.view.TagStatusSnapshot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One tag's runtime — both aspects of a tag plus their shared bookkeeping. A tag has an independent
 * {@link TagAspectRead read aspect} and {@link TagAspectWrite write aspect} sharing the Node/Tag pair but otherwise
 * orthogonal. The runtime forwards the events the wrapper routes to the right aspect, applies each aspect's
 * three-condition goal, and folds the two aspects into the published per-tag snapshot.
 * <p>
 * Created and used only on the wrapper's single dispatch thread.
 */
public final class TagRuntime {

    private final @NotNull NodeTagPair pair;
    private final @NotNull TagAspectRead readAspect;
    private final @NotNull TagAspectWrite writeAspect;

    private boolean readActivated;
    private boolean writeActivated;
    private boolean readUsed;
    private boolean writeUsed;

    /**
     * @param adapterId              the owning adapter's id.
     * @param pair                   the node/tag pair this runtime drives.
     * @param clock                  the actor clock.
     * @param timers                 the actor's single timer queue.
     * @param batches                the actor's batch collector.
     * @param metrics                the per-adapter metrics.
     * @param sharedNodeVerification the shared verification authority.
     * @param readinessListener      notified when the write aspect crosses its writability boundary.
     * @param pollIntervalMillis     the poll cadence for a polled read aspect, in milliseconds.
     * @param retryPolicy            the backoff policy for verification and subscription retries.
     */
    public TagRuntime(
            final @NotNull String adapterId,
            final @NotNull NodeTagPair pair,
            final @NotNull Clock clock,
            final @NotNull PriorityTimerQueue timers,
            final @NotNull BatchCollector batches,
            final @NotNull ProtocolAdapterMetrics metrics,
            final @NotNull SharedNodeVerification sharedNodeVerification,
            final @NotNull TagWriteReadinessListener readinessListener,
            final long pollIntervalMillis,
            final @NotNull RetryPolicy retryPolicy) {
        this.pair = pair;
        this.readAspect = new TagAspectRead(
                adapterId,
                pair.node(),
                pair.tag(),
                clock,
                timers,
                batches,
                metrics,
                sharedNodeVerification,
                pollIntervalMillis,
                retryPolicy);
        this.writeAspect = new TagAspectWrite(
                adapterId,
                pair.node(),
                pair.tag(),
                clock,
                timers,
                batches,
                metrics,
                sharedNodeVerification,
                readinessListener,
                retryPolicy);
    }

    /**
     * @return the node/tag pair this runtime drives.
     */
    public @NotNull NodeTagPair pair() {
        return pair;
    }

    /**
     * @return the protocol node this runtime's aspects share.
     */
    public @NotNull Node node() {
        return pair.node();
    }

    /**
     * @return the tag's name.
     */
    public @NotNull String tagName() {
        return pair.tag().name();
    }

    /**
     * @return whether either aspect is awaiting the connect-time verification — used by the
     *         coordinator to select this node for the single connect verification batch.
     */
    public boolean needsConnectVerification() {
        return readAspect.awaitingVerification() || writeAspect.awaitingVerification();
    }

    // ── goal and lifecycle ──────────────────────────────────────────────────────────────────

    /**
     * Apply the tag's activation and usage to its aspects atomically — the activation-only transition. Recomputes each aspect's three-condition goal (read on northbound, write on southbound) and applies
     * it; never reconnects.
     *
     * @param northboundActivated the read direction (northbound) goal.
     * @param southboundActivated the write direction (southbound) goal.
     * @param readActivated       the persisted read-aspect activation preference.
     * @param writeActivated      the persisted write-aspect activation preference.
     * @param readUsed            whether a northbound mapping consumes the tag.
     * @param writeUsed           whether a southbound mapping produces to the tag.
     */
    public void applyActivation(
            final boolean northboundActivated,
            final boolean southboundActivated,
            final boolean readActivated,
            final boolean writeActivated,
            final boolean readUsed,
            final boolean writeUsed) {
        this.readActivated = readActivated;
        this.writeActivated = writeActivated;
        this.readUsed = readUsed;
        this.writeUsed = writeUsed;
        readAspect.applyGoal(new TagAspectGoal(northboundActivated, readActivated, readUsed));
        writeAspect.applyGoal(new TagAspectGoal(southboundActivated, writeActivated, writeUsed));
    }

    /**
     * Forward the adapter's entry into verification to both aspects.
     */
    public void onAdapterVerifying() {
        readAspect.onAdapterVerifying();
        writeAspect.onAdapterVerifying();
    }

    /**
     * Forward the adapter reaching {@code CONNECTED} to both aspects.
     */
    public void onAdapterReady() {
        readAspect.onAdapterReady();
        writeAspect.onAdapterReady();
    }

    /**
     * Forward the loss of the adapter connection to both aspects.
     */
    public void onAdapterUnavailable() {
        readAspect.onAdapterUnavailable();
        writeAspect.onAdapterUnavailable();
    }

    /**
     * Retry the tag after a permanent verification failure: each aspect in permanent verification
     * failure resets and re-verifies; an aspect in any other state is left untouched.
     */
    public void retry() {
        readAspect.retry();
        writeAspect.retry();
    }

    /**
     * Force both aspects to {@code DEACTIVATED}, cancelling timers and dropping subscriptions — used when the tag
     * is being removed from the set. Never reconnects the adapter.
     */
    public void deactivate() {
        readActivated = false;
        writeActivated = false;
        readUsed = false;
        writeUsed = false;
        readAspect.applyGoal(TagAspectGoal.inactive());
        writeAspect.applyGoal(TagAspectGoal.inactive());
    }

    // ── routed events ───────────────────────────────────────────────────────────────────────────────

    /**
     * Fan a verification outcome to both of the tag's aspects — the single result serves the read
     * and write aspect alike.
     *
     * @param outcome the verification outcome.
     */
    public void onVerifyResult(final @NotNull VerifyOutcome outcome) {
        readAspect.onVerifyResult(outcome);
        writeAspect.onVerifyResult(outcome);
    }

    /**
     * Route a received value to the read aspect.
     *
     * @param value the reused v1 value.
     * @return whether the read aspect accepted the value as part of its operating flow.
     */
    public boolean onValue(final @NotNull DataPoint value) {
        return readAspect.onValue(value);
    }

    /**
     * Route a per-node failure to the read aspect.
     *
     * @param reason      a human-readable description.
     * @param spontaneous whether the failure arrived outside a command-response exchange.
     */
    public void onNodeError(final @NotNull String reason, final boolean spontaneous) {
        readAspect.onNodeError(reason, spontaneous);
    }

    /**
     * Route a southbound write request to the write aspect.
     *
     * @param value      the reused v1 value to write.
     * @param completion the one-shot back-pressure signal for this write.
     */
    public void submitWrite(final @NotNull DataPoint value, final @NotNull SouthboundWriteCompletion completion) {
        writeAspect.onWriteRequested(value, completion);
    }

    /**
     * Route a write acknowledgment to the write aspect.
     *
     * @param success whether the write succeeded.
     * @param reason  the failure reason, or {@code null} on success.
     */
    public void onWriteResult(final boolean success, final @Nullable String reason) {
        writeAspect.onWriteResult(success, reason);
    }

    // ── snapshot ────────────────────────────────────────────────────────────────────────────

    /**
     * @return the immutable per-tag status for publication, folding both aspects' live state, goal activation,
     *         operating status, permanent-failure flag, and failure counters.
     */
    public @NotNull TagStatusSnapshot snapshot() {
        final int totalFailureCount = readAspect.failureCount() + writeAspect.failureCount();
        final long lastTransition = Math.max(readAspect.lastTransitionAtMillis(), writeAspect.lastTransitionAtMillis());
        // The single reported reason is the more recent of the two aspects' last failures.
        @Nullable String lastFailureReason = readAspect.lastFailureReason();
        if (writeAspect.lastFailureReason() != null
                && (lastFailureReason == null
                        || writeAspect.lastTransitionAtMillis() >= readAspect.lastTransitionAtMillis())) {
            lastFailureReason = writeAspect.lastFailureReason();
        }
        return new TagStatusSnapshot(
                tagName(),
                readActivated,
                writeActivated,
                readUsed,
                writeUsed,
                readAspect.stateName(),
                writeAspect.stateName(),
                readAspect.goalActive(),
                writeAspect.goalActive(),
                readAspect.operating(),
                writeAspect.operating(),
                readAspect.permanentFailure(),
                writeAspect.permanentFailure(),
                totalFailureCount,
                lastFailureReason,
                lastTransition);
    }

    /**
     * @return the read aspect — for direct assertions in tests.
     */
    public @NotNull TagAspectRead readAspect() {
        return readAspect;
    }

    /**
     * @return the write aspect — for direct assertions in tests.
     */
    public @NotNull TagAspectWrite writeAspect() {
        return writeAspect;
    }
}
