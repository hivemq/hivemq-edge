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
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.protocols.v2.runtime.BatchCollector;
import com.hivemq.protocols.v2.runtime.Clock;
import com.hivemq.protocols.v2.runtime.NevskyMetrics;
import com.hivemq.protocols.v2.runtime.PriorityTimerQueue;
import com.hivemq.protocols.v2.runtime.RetryPolicy;
import com.hivemq.protocols.v2.view.TagStatusSnapshot;
import org.jetbrains.annotations.NotNull;

/**
 * One tag's runtime — both aspects of a tag plus their shared bookkeeping (design §7). A tag has an independent
 * read aspect and write aspect sharing the Node/Tag pair; this task ships the {@link TagAspectRead}, and the write
 * aspect joins in a later task with no change to the wrapper or the coordinator. The runtime forwards the events
 * the wrapper routes to the right aspect, applies the three-condition goal to each, and folds the aspects into the
 * published per-tag snapshot (design §6.6, §7.7).
 * <p>
 * Created and used only on the wrapper's single dispatch thread.
 */
public final class TagRuntime {

    private static final @NotNull String INERT_ASPECT_STATE = "DEACTIVATED";

    private final @NotNull NodeTagPair pair;
    private final @NotNull TagAspectRead readAspect;

    private boolean readActivated;
    private boolean writeActivated;
    private boolean readUsed;
    private boolean writeUsed;

    /**
     * @param adapterId               the owning adapter's id.
     * @param pair                    the node/tag pair this runtime drives.
     * @param clock                   the actor clock.
     * @param timers                  the actor's single timer queue.
     * @param batches                 the actor's batch collector.
     * @param metrics                 the per-adapter metrics.
     * @param sharedNodeVerification the shared verification authority.
     * @param pollIntervalMillis      the poll cadence for a polled read aspect, in milliseconds.
     * @param retryPolicy             the backoff policy for verification and subscription retries.
     */
    public TagRuntime(
            final @NotNull String adapterId,
            final @NotNull NodeTagPair pair,
            final @NotNull Clock clock,
            final @NotNull PriorityTimerQueue timers,
            final @NotNull BatchCollector batches,
            final @NotNull NevskyMetrics metrics,
            final @NotNull SharedNodeVerification sharedNodeVerification,
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
    }

    /**
     * @return the node/tag pair this runtime drives.
     */
    public @NotNull NodeTagPair pair() {
        return pair;
    }

    /**
     * @return the tag's name.
     */
    public @NotNull String tagName() {
        return pair.tag().name();
    }

    // ── goal and lifecycle (design §7.1, §7.2) ──────────────────────────────────────────────────────────────────

    /**
     * Apply the tag's activation and usage to its aspects atomically — the activation-only transition (design
     * §8.2). Recomputes each aspect's three-condition goal and applies it; never reconnects.
     *
     * @param northboundActivated the read direction (northbound) goal.
     * @param readActivated       the persisted read-aspect activation preference.
     * @param writeActivated      the persisted write-aspect activation preference.
     * @param readUsed            whether a northbound mapping consumes the tag.
     * @param writeUsed           whether a southbound mapping produces to the tag.
     */
    public void applyActivation(
            final boolean northboundActivated,
            final boolean readActivated,
            final boolean writeActivated,
            final boolean readUsed,
            final boolean writeUsed) {
        this.readActivated = readActivated;
        this.writeActivated = writeActivated;
        this.readUsed = readUsed;
        this.writeUsed = writeUsed;
        readAspect.applyGoal(new TagAspectGoal(northboundActivated, readActivated, readUsed));
    }

    /**
     * Forward the adapter's entry into verification to the aspects (design §6.3).
     */
    public void onAdapterVerifying() {
        readAspect.onAdapterVerifying();
    }

    /**
     * Forward the adapter reaching {@code CONNECTED} to the aspects (design §7.2).
     */
    public void onAdapterReady() {
        readAspect.onAdapterReady();
    }

    /**
     * Forward the loss of the adapter connection to the aspects (design §7.2).
     */
    public void onAdapterUnavailable() {
        readAspect.onAdapterUnavailable();
    }

    /**
     * Retry the tag after a permanent verification failure (design §7.6).
     */
    public void retry() {
        readAspect.retry();
    }

    /**
     * Force every aspect to {@code DEACTIVATED}, cancelling timers and dropping subscriptions — used when the tag
     * is being removed from the set (design §8.2). Never reconnects the adapter.
     */
    public void deactivate() {
        readActivated = false;
        writeActivated = false;
        readUsed = false;
        writeUsed = false;
        readAspect.applyGoal(TagAspectGoal.inactive());
    }

    // ── routed events (design §7) ───────────────────────────────────────────────────────────────────────────────

    /**
     * Fan a verification outcome to the tag's aspects (design §7.6).
     *
     * @param outcome the verification outcome.
     */
    public void onVerifyResult(final @NotNull VerifyOutcome outcome) {
        readAspect.onVerifyResult(outcome);
    }

    /**
     * Route a received value to the read aspect (design §7.3, §7.4).
     *
     * @param value the reused v1 value.
     */
    public void onValue(final @NotNull DataPoint value) {
        readAspect.onValue(value);
    }

    /**
     * Route a per-node failure to the read aspect (design §7.3, §7.4).
     *
     * @param reason      a human-readable description.
     * @param spontaneous whether the failure arrived outside a command-response exchange.
     */
    public void onNodeError(final @NotNull String reason, final boolean spontaneous) {
        readAspect.onNodeError(reason, spontaneous);
    }

    // ── snapshot (design §6.6, §7.7) ────────────────────────────────────────────────────────────────────────────

    /**
     * @return the immutable per-tag status for publication. The write aspect is inert in this task and reported as
     *         {@code DEACTIVATED}; the read aspect reports its live state, failure count, and last failure reason.
     */
    public @NotNull TagStatusSnapshot snapshot() {
        return new TagStatusSnapshot(
                tagName(),
                readActivated,
                writeActivated,
                readUsed,
                writeUsed,
                readAspect.stateName(),
                INERT_ASPECT_STATE,
                readAspect.failureCount(),
                readAspect.lastFailureReason(),
                readAspect.lastTransitionAtMillis());
    }

    /**
     * @return the read aspect — for direct assertions in tests.
     */
    public @NotNull TagAspectRead readAspect() {
        return readAspect;
    }
}
