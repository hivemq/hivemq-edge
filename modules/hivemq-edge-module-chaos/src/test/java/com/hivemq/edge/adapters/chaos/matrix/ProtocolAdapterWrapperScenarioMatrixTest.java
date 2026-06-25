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
package com.hivemq.edge.adapters.chaos.matrix;

import static com.hivemq.protocols.v2.view.TagStatus.DEACTIVATED;
import static com.hivemq.protocols.v2.view.TagStatus.ERROR;
import static com.hivemq.protocols.v2.view.TagStatus.NORTHBOUND_AND_SOUTHBOUND;
import static com.hivemq.protocols.v2.view.TagStatus.NORTHBOUND_ONLY;
import static com.hivemq.protocols.v2.view.TagStatus.SOUTHBOUND_ONLY;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.CONNECTED;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.STOPPED;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_CONNECTED;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_CONNECTION_RETRY;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_DISCONNECTED_BEFORE_RECONNECT;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_STARTED;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_VERIFICATION;
import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.v2.model.ErrorScope;
import com.hivemq.adapter.sdk.api.v2.model.VerifyOutcome;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.edge.adapters.chaos.ChaosBehavior;
import com.hivemq.edge.adapters.chaos.ChaosDataPoint;
import com.hivemq.edge.adapters.chaos.ChaosEvent;
import com.hivemq.edge.adapters.chaos.ChaosScript;
import com.hivemq.edge.adapters.chaos.NodeMatcher;
import com.hivemq.edge.adapters.chaos.PollBehavior;
import com.hivemq.edge.adapters.chaos.SubscriptionBehavior;
import com.hivemq.edge.adapters.chaos.harness.ProtocolAdapterWrapperTestHarness;
import com.hivemq.protocols.v2.runtime.RetryPolicy;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The deterministic scenario matrix (design §15), driven through the {@link ProtocolAdapterWrapperTestHarness}: a
 * real wrapper actor and its tag-aspect machines, fed by a scripted {@link com.hivemq.edge.adapters.chaos
 * .ChaosProtocolAdapter ChaosProtocolAdapter} on a {@code FakeClock} + {@code ManualDispatcher}. This is the
 * single, consolidated re-expression of the per-class wrapper tests through the chaos simulator — proving the
 * simulator, the DSL, and the harness reproduce the whole matrix, which the wired end-to-end suite (T13) then
 * relies on.
 * <p>
 * <b>Time model.</b> One harness tick is one second ({@code tickPeriodMillis = 1000}); the watchdog, poll cadence,
 * and connection backoff are all one tick, so {@link ProtocolAdapterWrapperTestHarness#advance(int) advance(1)}
 * fires exactly one armed timer.
 * <p>
 * <b>Scope.</b> Covered here: S1–S14, S16–S19, S21, S24–S25, S27, S29–S30 — every scenario a single wrapper can
 * faithfully drive. The remainder live where their machinery does: S15 (full recreate) and S16's manager half in
 * {@code ProtocolAdapterConfigDiffUtilsTest} (T11); S20 / S28 / S31 (coexistence, restart, browse REST) in T13;
 * S22 (schema projection) in {@code SchemaJsonTest} (T2); S23 (mailbox concurrency) in {@code
 * MailboxConcurrencyTest}; S26 (batch reconciliation) in {@code BatchCollectorReconciliationTest} (T5); S32
 * (config validation) in T9.
 */
class ProtocolAdapterWrapperScenarioMatrixTest {

    private static final @NotNull DataPoint TEMPERATURE = new ChaosDataPoint("temperature", "21");

    private static @NotNull ProtocolAdapterWrapperTestHarness harness(final @NotNull ChaosScript script) {
        return ProtocolAdapterWrapperTestHarness.with(script)
                .tickPeriodMillis(1000)
                .watchdogTimeoutMillis(1000)
                .pollIntervalMillis(1000);
    }

    private static long count(final @NotNull ProtocolAdapterWrapperTestHarness harness, final @NotNull String command) {
        return harness.commandsSent().stream().filter(command::equals).count();
    }

    // ── S1: happy path ──────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void s1_happyPath_startConnectVerifyPoll() {
        final ProtocolAdapterWrapperTestHarness harness = harness(ChaosScript.builder()
                .poll(NodeMatcher.all(), PollBehavior.value(TEMPERATURE))
                .build());

        harness.activateNorthbound();

        assertThat(harness.wrapperState()).isEqualTo(CONNECTED);
        harness.assertSequence("start", "connect", "verifyBatch");
        assertThat(harness.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
        assertThat(harness.tagStatus("temperature")).isEqualTo(NORTHBOUND_ONLY);

        harness.advance(1); // the poll interval elapses: a poll is requested and answered with a value
        assertThat(harness.commandsSent()).contains("pollBatch");
        assertThat(harness.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
        assertThat(harness.tag("temperature").failureCount()).isZero();
    }

    // ── S2: connect fails, then succeeds ────────────────────────────────────────────────────────────────────────

    @Test
    void s2_connectFailsThenSucceeds_recoversAfterBackoff() {
        final ProtocolAdapterWrapperTestHarness harness = harness(ChaosScript.builder()
                .onConnectSequence(List.of(ChaosBehavior.failConnection("refused"), ChaosBehavior.succeed()))
                .build());

        harness.activateNorthbound();
        assertThat(harness.wrapperState()).isEqualTo(WAITING_FOR_CONNECTION_RETRY);

        harness.advance(1); // the backoff fires: reconnect, verify, CONNECTED
        assertThat(harness.wrapperState()).isEqualTo(CONNECTED);
        harness.assertSequence("start", "connect", "connect", "verifyBatch");
    }

    // ── S3: verification transient failure → retry → success ────────────────────────────────────────────────────

    @Test
    void s3_transientVerificationFailure_retriesThenSucceeds() {
        final ProtocolAdapterWrapperTestHarness harness = harness(ChaosScript.builder()
                .verifySequence(
                        NodeMatcher.all(),
                        List.of(new VerifyOutcome.TransientFailure("device busy"), new VerifyOutcome.Success()))
                .build());

        harness.activateNorthbound();
        // The connect gate reaches CONNECTED on any outcome (design §6.3); the aspect schedules a verify retry.
        assertThat(harness.wrapperState()).isEqualTo(CONNECTED);
        assertThat(harness.readState("temperature")).isEqualTo("WAITING_FOR_VERIFICATION_RETRY");

        harness.advance(1); // the verification retry timer fires: re-verify, succeed, begin polling
        assertThat(harness.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
    }

    // ── S4: verification permanent failure ──────────────────────────────────────────────────────────────────────

    @Test
    void s4_permanentVerificationFailure_suspendsTheTagButLeavesTheAdapterConnected() {
        final ProtocolAdapterWrapperTestHarness harness = harness(ChaosScript.builder()
                .verify(NodeMatcher.all(), new VerifyOutcome.PermanentFailure("unknown address"))
                .build());

        harness.activateNorthbound();

        assertThat(harness.wrapperState()).isEqualTo(CONNECTED); // the adapter does not reconnect (design §7.6)
        assertThat(harness.readState("temperature")).isEqualTo("ERROR_PERMANENT_VERIFICATION_FAILURE");
        assertThat(harness.tagStatus("temperature")).isEqualTo(ERROR);

        harness.advance(5); // no retry timer for a permanent failure
        assertThat(harness.readState("temperature")).isEqualTo("ERROR_PERMANENT_VERIFICATION_FAILURE");
    }

    // ── S5: spontaneous disconnect while CONNECTED ──────────────────────────────────────────────────────────────

    @Test
    void s5_spontaneousDisconnect_parksAspectsThenReconnectsAndReVerifies() {
        final ProtocolAdapterWrapperTestHarness harness = harness(
                ChaosScript.builder().injectAtTick(1, ChaosEvent.disconnect()).build());

        harness.activateNorthbound();
        assertThat(harness.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");

        harness.advance(1); // the injected spontaneous loss fires
        assertThat(harness.wrapperState()).isEqualTo(WAITING_FOR_CONNECTION_RETRY);
        assertThat(harness.readState("temperature")).isEqualTo("WAITING_FOR_ADAPTER_READY");
        // No disconnect() was issued — a spontaneous loss goes straight to backoff (contrast with S6).
        harness.assertSequence("start", "connect", "verifyBatch");

        harness.advance(1); // the connection backoff fires: reconnect, re-verify, resume
        assertThat(harness.wrapperState()).isEqualTo(CONNECTED);
        assertThat(harness.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
    }

    // ── S6: error(CONNECTION) while CONNECTED ───────────────────────────────────────────────────────────────────

    @Test
    void s6_connectionError_disconnectsCleanlyBeforeBackingOff() {
        final ProtocolAdapterWrapperTestHarness harness = harness(ChaosScript.builder()
                .onDisconnect(ChaosBehavior.drop()) // pause to observe the intermediate state
                .injectAtTick(1, ChaosEvent.error(ErrorScope.CONNECTION, "lost"))
                .build());

        harness.activateNorthbound();

        harness.advance(1); // the injected connection error fires
        assertThat(harness.wrapperState()).isEqualTo(WAITING_FOR_DISCONNECTED_BEFORE_RECONNECT);
        assertThat(harness.commandsSent()).contains("disconnect"); // a clean disconnect first (contrast with S5)
    }

    // ── S7: goal → STOPPED from waiting states ──────────────────────────────────────────────────────────────────

    @Test
    void s7_stopInWaitingForStarted_recordsIntentThenStopsWhenStartedLands() {
        final ProtocolAdapterWrapperTestHarness harness = ProtocolAdapterWrapperTestHarness.with(ChaosScript.builder()
                        .onStart(ChaosBehavior.delay(2, ChaosBehavior.succeed()))
                        .build())
                .tickPeriodMillis(1000)
                .watchdogTimeoutMillis(10_000) // longer than the deferred start, so no watchdog fires
                .pollIntervalMillis(1000);

        harness.activateNorthbound();
        assertThat(harness.wrapperState()).isEqualTo(WAITING_FOR_STARTED);

        harness.stop(); // intent recorded; cannot act while awaiting started()
        assertThat(harness.wrapperState()).isEqualTo(WAITING_FOR_STARTED);

        harness.advance(2); // started() lands and routes to stop(), not connect()
        assertThat(harness.wrapperState()).isEqualTo(STOPPED);
        assertThat(harness.commandsSent()).doesNotContain("connect");
    }

    @Test
    void s7_stopWhileConnected_disconnectsAndStops() {
        final ProtocolAdapterWrapperTestHarness harness =
                harness(ChaosScript.builder().build());
        harness.activateNorthbound();
        assertThat(harness.wrapperState()).isEqualTo(CONNECTED);

        harness.stop();
        assertThat(harness.wrapperState()).isEqualTo(STOPPED);
        assertThat(harness.commandsSent()).containsSubsequence("disconnect", "stop");
    }

    @Test
    void s7_stopInConnectionRetry_cancelsBackoffAndStops() {
        final ProtocolAdapterWrapperTestHarness harness = harness(ChaosScript.builder()
                .onConnect(ChaosBehavior.failConnection("refused"))
                .build());
        harness.activateNorthbound();
        assertThat(harness.wrapperState()).isEqualTo(WAITING_FOR_CONNECTION_RETRY);

        harness.stop();
        assertThat(harness.wrapperState()).isEqualTo(STOPPED);

        harness.advance(5); // the backoff was canceled — advancing never re-attempts a connect
        assertThat(harness.wrapperState()).isEqualTo(STOPPED);
    }

    // ── S8: watchdogs ───────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void s8_watchdogInWaitingForStarted_resetsToError() {
        final ProtocolAdapterWrapperTestHarness harness =
                harness(ChaosScript.builder().onStart(ChaosBehavior.drop()).build());
        harness.activateNorthbound();
        assertThat(harness.wrapperState()).isEqualTo(WAITING_FOR_STARTED);

        harness.advance(1); // the watchdog fires
        assertThat(harness.wrapperState()).isEqualTo(ProtocolAdapterWrapperState.ERROR);
        assertThat(harness.errorNotifications()).hasSize(1);
    }

    @Test
    void s8_watchdogInWaitingForConnected_resetsToError() {
        final ProtocolAdapterWrapperTestHarness harness =
                harness(ChaosScript.builder().onConnect(ChaosBehavior.drop()).build());
        harness.activateNorthbound();
        assertThat(harness.wrapperState()).isEqualTo(WAITING_FOR_CONNECTED);

        harness.advance(1);
        assertThat(harness.wrapperState()).isEqualTo(ProtocolAdapterWrapperState.ERROR);
    }

    @Test
    void s8_verificationWatchdog_disconnectsToReconnect_doesNotReset() {
        final ProtocolAdapterWrapperTestHarness harness = harness(ChaosScript.builder()
                .verifyNoResponse(NodeMatcher.all())
                .onDisconnect(ChaosBehavior.drop()) // observe the intermediate state
                .build());
        harness.activateNorthbound();
        assertThat(harness.wrapperState()).isEqualTo(WAITING_FOR_VERIFICATION);

        harness.advance(1); // the verification watchdog fires
        assertThat(harness.wrapperState()).isEqualTo(WAITING_FOR_DISCONNECTED_BEFORE_RECONNECT);
        assertThat(harness.errorNotifications()).isEmpty(); // a recoverable condition, not an ERROR
        assertThat(harness.commandsSent()).contains("disconnect");
    }

    // ── S9: unexpected event → defensive reset ──────────────────────────────────────────────────────────────────

    @Test
    void s9_unexpectedEvent_triggersOneDefensiveReset() {
        final ProtocolAdapterWrapperTestHarness harness = harness(
                ChaosScript.builder().injectAtTick(1, ChaosEvent.started()).build());
        harness.activateNorthbound(); // CONNECTED, goal stays "connected"

        harness.advance(1); // a spurious started() while CONNECTED is unexpected
        assertThat(harness.wrapperState()).isEqualTo(ProtocolAdapterWrapperState.ERROR);
        assertThat(harness.errorNotifications()).hasSize(1);
        assertThat(count(harness, "stop")).isEqualTo(1); // best-effort stop issued exactly once
    }

    // ── S10 / S11: subscribed loss paths ────────────────────────────────────────────────────────────────────────

    @Test
    void s10_subscribedCommandResponseLoss_backsOffAndReAddsWithoutReVerifying() {
        final NodeTagPair pair = ProtocolAdapterWrapperTestHarness.subscribableTag("temperature");
        final Node node = pair.node();
        final ProtocolAdapterWrapperTestHarness harness = harness(ChaosScript.builder()
                        .subscribe(NodeMatcher.all(), SubscriptionBehavior.accept(TEMPERATURE))
                        .injectAtTick(2, ChaosEvent.nodeError(node, "subscription dropped", false))
                        .build())
                .configure(List.of(pair));

        harness.activateNorthbound();
        harness.advance(1); // dispatch the add-subscription; the first value confirms SUBSCRIBED
        assertThat(harness.readState("temperature")).isEqualTo("SUBSCRIBED");

        harness.advance(1); // the command-response loss fires
        assertThat(harness.readState("temperature")).isEqualTo("WAITING_FOR_SUBSCRIPTION_RETRY");
        assertThat(count(harness, "verifyBatch")).isEqualTo(1); // no re-verify on a command-response loss

        harness.advance(1); // the subscription backoff fires: re-add (still no re-verify)
        assertThat(count(harness, "addSubscriptionBatch")).isEqualTo(2);
        assertThat(count(harness, "verifyBatch")).isEqualTo(1);
    }

    @Test
    void s11_subscribedSpontaneousLoss_powerCyclesThroughVerification() {
        final NodeTagPair pair = ProtocolAdapterWrapperTestHarness.subscribableTag("temperature");
        final Node node = pair.node();
        final ProtocolAdapterWrapperTestHarness harness = harness(ChaosScript.builder()
                        .subscribe(NodeMatcher.all(), SubscriptionBehavior.accept(TEMPERATURE))
                        .injectAtTick(2, ChaosEvent.nodeError(node, "device reset", true))
                        .build())
                .configure(List.of(pair));

        harness.activateNorthbound();
        harness.advance(1); // SUBSCRIBED
        assertThat(harness.readState("temperature")).isEqualTo("SUBSCRIBED");
        assertThat(count(harness, "verifyBatch")).isEqualTo(1);

        harness.advance(1); // the spontaneous loss power-cycles the aspect through verification
        assertThat(count(harness, "verifyBatch")).isEqualTo(2); // re-verified (contrast with S10)
    }

    // ── S12: poll failure ───────────────────────────────────────────────────────────────────────────────────────

    @Test
    void s12_pollFailure_returnsToPollIntervalAndCountsWithNoNewState() {
        final ProtocolAdapterWrapperTestHarness harness = harness(ChaosScript.builder()
                .poll(NodeMatcher.all(), PollBehavior.nodeError("read timeout"))
                .build());
        harness.activateNorthbound();

        harness.advance(1); // the poll fires and the device reports a failure

        assertThat(harness.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
        assertThat(harness.tag("temperature").failureCount()).isEqualTo(1);
        assertThat(harness.tag("temperature").lastFailureReason()).isEqualTo("read timeout");
    }

    // ── S13: mid-flight goal change ─────────────────────────────────────────────────────────────────────────────

    @Test
    void s13_goalChangeWhileAPollIsInFlight_isHandledCleanly() {
        final ProtocolAdapterWrapperTestHarness harness = harness(ChaosScript.builder()
                .poll(NodeMatcher.all(), PollBehavior.noResponse()) // the poll never returns
                .build());
        harness.activateNorthbound();
        harness.advance(1); // a poll is now in flight
        assertThat(harness.readState("temperature")).isEqualTo("WAITING_FOR_POLL_DATAPOINT");

        harness.deactivateNorthbound(); // goal flips mid-flight

        assertThat(harness.wrapperState()).isEqualTo(STOPPED);
        assertThat(harness.readState("temperature")).isEqualTo("DEACTIVATED");
    }

    // ── S14: tags-only reload ───────────────────────────────────────────────────────────────────────────────────

    @Test
    void s14_tagsOnlyReload_appliesInPlaceWithoutReconnecting() {
        final ProtocolAdapterWrapperTestHarness harness = harness(ChaosScript.builder()
                .poll(NodeMatcher.all(), PollBehavior.value(TEMPERATURE))
                .build());
        harness.activateNorthbound();
        assertThat(harness.wrapperState()).isEqualTo(CONNECTED);
        assertThat(count(harness, "connect")).isEqualTo(1);

        harness.updateTags(Set.of("temperature"), Set.of()); // a tags-only reload of the same set

        // The gentlest transition for a tags-only change: the tag set is re-applied in place and the adapter is
        // NEVER reconnected (design §8.2). (The rebuilt aspects re-couple to the adapter on its next readiness
        // signal; the no-reconnect invariant is the guarantee asserted here.)
        assertThat(harness.wrapperState()).isEqualTo(CONNECTED);
        assertThat(count(harness, "connect")).isEqualTo(1);
        assertThat(harness.tag("temperature")).isNotNull();
    }

    // ── S16: `used` flips ───────────────────────────────────────────────────────────────────────────────────────

    @Test
    void s16_usedFlipsToFalse_deactivatesTheAspectWithoutReconnecting() {
        final ProtocolAdapterWrapperTestHarness harness = harness(ChaosScript.builder()
                .poll(NodeMatcher.all(), PollBehavior.value(TEMPERATURE))
                .build());
        harness.activateNorthbound();
        assertThat(harness.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");

        harness.updateTags(Set.of(), Set.of()); // the last consuming mapping is gone → readUsed flips to false

        assertThat(count(harness, "connect")).isEqualTo(1); // no reconnect
        assertThat(harness.readState("temperature")).isEqualTo("DEACTIVATED");
        assertThat(harness.tagStatus("temperature")).isEqualTo(DEACTIVATED);
    }

    // ── S17: shared verification ────────────────────────────────────────────────────────────────────────────────

    @Test
    void s17_readAndWriteTag_verifiesOnceServingBothAspects() {
        final ProtocolAdapterWrapperTestHarness harness = harness(
                        ChaosScript.builder().build())
                .readUsed(Set.of("temperature"))
                .writeUsed(Set.of("temperature"));

        harness.activateBoth();

        assertThat(harness.wrapperState()).isEqualTo(CONNECTED);
        assertThat(count(harness, "verifyBatch")).isEqualTo(1); // one batch served both aspects (design §7.6)
        assertThat(harness.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
        assertThat(harness.writeState("temperature")).isEqualTo("WAITING_FOR_WRITE_REQUEST");
        assertThat(harness.tagStatus("temperature")).isEqualTo(NORTHBOUND_AND_SOUTHBOUND);
    }

    // ── S18: skip verification ──────────────────────────────────────────────────────────────────────────────────

    @Test
    void s18_skipVerification_reachesConnectedWithoutAVerifyBatch() {
        final ProtocolAdapterWrapperTestHarness harness =
                harness(ChaosScript.builder().build()).skipVerification(true);

        harness.activateNorthbound();

        assertThat(harness.wrapperState()).isEqualTo(CONNECTED);
        harness.assertSequence("start", "connect");
    }

    // ── S19: maximum retries exceeded ───────────────────────────────────────────────────────────────────────────

    @Test
    void s19_maximumRetriesExceeded_escalatesToErrorAndNotifiesTheSupervisor() {
        final ProtocolAdapterWrapperTestHarness harness = harness(ChaosScript.builder()
                        .onConnect(ChaosBehavior.failConnection("refused"))
                        .build())
                .retryPolicy(new RetryPolicy(1000, 1.41, 32000, 1)); // one retry, then give up
        harness.activateNorthbound();
        assertThat(harness.wrapperState()).isEqualTo(WAITING_FOR_CONNECTION_RETRY);

        harness.advance(2); // the backoff fires, the second connect fails, retries are exhausted
        assertThat(harness.wrapperState()).isEqualTo(ProtocolAdapterWrapperState.ERROR);
        assertThat(harness.errorNotifications()).hasSize(1);
        assertThat(harness.snapshot().lastErrorReason()).contains("retries");
    }

    // ── S21: reused DataPoint round-trip ────────────────────────────────────────────────────────────────────────

    @Test
    void s21_reusedDataPointRoundTrip_flowsThroughAPollKeyedByNode() {
        final ProtocolAdapterWrapperTestHarness harness = harness(ChaosScript.builder()
                .poll(NodeMatcher.all(), PollBehavior.value(new ChaosDataPoint("temperature", "21.5")))
                .build());
        harness.activateNorthbound();

        harness.advance(1); // the poll delivers the reused v1 DataPoint, correlated by Node

        assertThat(harness.eventsSeen()).contains("dataPoint");
        assertThat(harness.tag("temperature").failureCount()).isZero();
        assertThat(harness.tagStatus("temperature")).isEqualTo(NORTHBOUND_ONLY);
    }

    // ── S24: time-as-a-message ordering (EVENT > TICK) ──────────────────────────────────────────────────────────

    @Test
    void s24_acknowledgmentEnqueuedAlongsideADueTick_isProcessedFirst_noSpuriousWatchdog() {
        // The acknowledgment latency lands each ack on the same tick its watchdog would fire. Because EVENT
        // outranks TICK (§5.1), the ack is processed first and cancels the watchdog, so no spurious watchdog fires
        // and the adapter reaches CONNECTED without a single supervisor error notification.
        final ProtocolAdapterWrapperTestHarness harness =
                harness(ChaosScript.builder().acknowledgmentLatencyTicks(1).build());

        harness.activateNorthbound();
        assertThat(harness.wrapperState()).isEqualTo(WAITING_FOR_STARTED);

        harness.advance(1); // started() lands alongside the start watchdog tick
        assertThat(harness.wrapperState()).isEqualTo(WAITING_FOR_CONNECTED);
        assertThat(harness.errorNotifications()).isEmpty();

        harness.advance(1); // connected() lands alongside the connect watchdog tick
        assertThat(harness.wrapperState()).isEqualTo(CONNECTED);
        assertThat(harness.errorNotifications()).isEmpty();
    }

    // ── S25: ERROR absorption ───────────────────────────────────────────────────────────────────────────────────

    @Test
    void s25_stoppedAndDisconnectedArrivingInError_areAbsorbed_noSecondStop_noLoop() {
        final ProtocolAdapterWrapperTestHarness harness = harness(ChaosScript.builder()
                .injectAtTick(1, ChaosEvent.started()) // → defensive reset → ERROR; the reset's stop() → stopped()
                .injectAtTick(2, ChaosEvent.stopped()) // arrives in ERROR → absorbed
                .injectAtTick(2, ChaosEvent.disconnect()) // arrives in ERROR → absorbed
                .build());
        harness.activateNorthbound();

        harness.advance(1); // the spurious started() resets to ERROR
        assertThat(harness.wrapperState()).isEqualTo(ProtocolAdapterWrapperState.ERROR);
        assertThat(count(harness, "stop")).isEqualTo(1);

        harness.advance(1); // the stale stopped()/disconnected() are absorbed
        assertThat(harness.wrapperState()).isEqualTo(ProtocolAdapterWrapperState.ERROR);
        assertThat(count(harness, "stop")).isEqualTo(1); // no second stop, no reset loop
        assertThat(harness.errorNotifications()).hasSize(1);
    }

    // ── S27: three-condition rule (southbound off) ──────────────────────────────────────────────────────────────

    @Test
    void s27_southboundOff_keepsWriteAspectsDeactivated_thenActivatesWithoutReconnect() {
        final ProtocolAdapterWrapperTestHarness harness = harness(ChaosScript.builder()
                        .poll(NodeMatcher.all(), PollBehavior.value(TEMPERATURE))
                        .build())
                .readUsed(Set.of("temperature"))
                .writeUsed(Set.of("temperature"));

        harness.activateNorthbound(); // southbound stays off
        assertThat(harness.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
        assertThat(harness.writeState("temperature")).isEqualTo("DEACTIVATED");
        assertThat(harness.tagStatus("temperature")).isEqualTo(NORTHBOUND_ONLY);

        harness.activateSouthbound(); // flip southbound on
        assertThat(harness.writeState("temperature")).isEqualTo("WAITING_FOR_WRITE_REQUEST");
        assertThat(count(harness, "connect")).isEqualTo(1); // activating a direction never reconnects
        assertThat(harness.tagStatus("temperature")).isEqualTo(NORTHBOUND_AND_SOUTHBOUND);
    }

    // ── S29: tag retry after permanent verification failure ─────────────────────────────────────────────────────

    @Test
    void s29_tagRetry_reVerifiesAPermanentlyFailedTag_resetsCounters_withoutTouchingConfig() {
        final ProtocolAdapterWrapperTestHarness harness = harness(ChaosScript.builder()
                        .verifySequence(
                                NodeMatcher.all(),
                                List.of(
                                        new VerifyOutcome.PermanentFailure("unknown address"),
                                        new VerifyOutcome.Success()))
                        .build())
                .readUsed(Set.of("temperature"))
                .writeUsed(Set.of("temperature"));

        harness.activateBoth();
        assertThat(harness.readState("temperature")).isEqualTo("ERROR_PERMANENT_VERIFICATION_FAILURE");
        assertThat(harness.writeState("temperature")).isEqualTo("ERROR_PERMANENT_VERIFICATION_FAILURE");
        assertThat(harness.tag("temperature").failureCount()).isEqualTo(2); // read + write each counted once

        harness.retryTag("temperature"); // the device is fixed; a runtime retry re-verifies it

        assertThat(harness.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
        assertThat(harness.writeState("temperature")).isEqualTo("WAITING_FOR_WRITE_REQUEST");
        assertThat(harness.tag("temperature").failureCount()).isZero(); // counters reset
        assertThat(harness.tagStatus("temperature")).isEqualTo(NORTHBOUND_AND_SOUTHBOUND);
    }

    // ── S30: combination-aware tag status fold ──────────────────────────────────────────────────────────────────

    @Test
    void s30_writeOnlyHealthyTag_foldsToSouthboundOnly() {
        final ProtocolAdapterWrapperTestHarness harness = harness(
                        ChaosScript.builder().build())
                .configure(List.of(ProtocolAdapterWrapperTestHarness.polledTag("setpoint")))
                .readUsed(Set.of())
                .writeUsed(Set.of("setpoint"));

        harness.activateSouthbound();

        assertThat(harness.wrapperState()).isEqualTo(CONNECTED);
        assertThat(harness.writeState("setpoint")).isEqualTo("WAITING_FOR_WRITE_REQUEST");
        assertThat(harness.readState("setpoint")).isEqualTo("DEACTIVATED");
        assertThat(harness.tagStatus("setpoint")).isEqualTo(SOUTHBOUND_ONLY);
    }

    @Test
    void s30_readOnlyHealthyTag_foldsToNorthboundOnly() {
        final ProtocolAdapterWrapperTestHarness harness = harness(ChaosScript.builder()
                        .poll(NodeMatcher.all(), PollBehavior.value(TEMPERATURE))
                        .build())
                .readUsed(Set.of("temperature"))
                .writeUsed(Set.of());

        harness.activateNorthbound();

        assertThat(harness.tagStatus("temperature")).isEqualTo(NORTHBOUND_ONLY);
    }

    @Test
    void s30_readAndWriteHealthyTag_foldsToNorthboundAndSouthbound() {
        final ProtocolAdapterWrapperTestHarness harness = harness(ChaosScript.builder()
                        .poll(NodeMatcher.all(), PollBehavior.value(TEMPERATURE))
                        .build())
                .readUsed(Set.of("temperature"))
                .writeUsed(Set.of("temperature"));

        harness.activateBoth();

        assertThat(harness.tagStatus("temperature")).isEqualTo(NORTHBOUND_AND_SOUTHBOUND);
    }
}
