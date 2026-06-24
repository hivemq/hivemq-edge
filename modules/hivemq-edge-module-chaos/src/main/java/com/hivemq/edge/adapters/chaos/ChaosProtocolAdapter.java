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
package com.hivemq.edge.adapters.chaos;

import com.hivemq.adapter.sdk.api.v2.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.v2.model.BrowseFilter;
import com.hivemq.adapter.sdk.api.v2.model.ErrorScope;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterOutput;
import com.hivemq.adapter.sdk.api.v2.model.WriteEntry;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The scriptable test simulator (design §10). It implements {@link ProtocolAdapter} directly and answers every
 * command by consulting a {@link ChaosScript}, reporting back through the {@link ProtocolAdapterOutput} tell-façade
 * exactly as a real adapter would — so its replies travel back through the wrapper mailbox. It records every
 * command for sequence assertions.
 * <p>
 * <b>Time contract (design §10.2).</b> The simulator holds no timers of its own. Immediate behaviors
 * ({@link ChaosBehavior#succeed()}, a {@link PollBehavior.Value}, …) report within the command call; deferred
 * behaviors ({@link ChaosBehavior.Delay}, the global acknowledgment latency, a {@link SubscriptionBehavior.LoseAfter}
 * loss, a browse duration, and {@link ChaosScript#injectedEvents() injected events}) are queued against a tick
 * counter and fired by {@link #onTick()}, which the harness calls once per advanced tick. This keeps the simulator
 * passive and every test fully deterministic.
 * <p>
 * Not thread-safe: it is driven from the single test thread via the {@code ManualDispatcher}, mirroring the actor's
 * single dispatch thread.
 */
public final class ChaosProtocolAdapter implements ProtocolAdapter {

    private final @NotNull String adapterId;
    private final @NotNull ProtocolAdapterOutput output;
    private final @NotNull ChaosScript script;

    private final @NotNull List<String> commands = new ArrayList<>();
    private final @NotNull List<Deferred> deferred = new ArrayList<>();
    private final @NotNull Map<Node, Integer> verifyAttempts = new HashMap<>();
    private long currentTick;
    private int connectAttempt;

    /**
     * @param adapterId the adapter instance id.
     * @param output    the tell-façade replies travel through.
     * @param script    the behavior script consulted per command.
     */
    public ChaosProtocolAdapter(
            final @NotNull String adapterId,
            final @NotNull ProtocolAdapterOutput output,
            final @NotNull ChaosScript script) {
        this.adapterId = adapterId;
        this.output = output;
        this.script = script;
        for (final ChaosScript.InjectedEvent injected : script.injectedEvents()) {
            deferred.add(new Deferred(injected.tick(), () -> applyEvent(injected.event())));
        }
    }

    // ── ProtocolAdapter (design §3.6) ───────────────────────────────────────────────────────────────────────────

    @Override
    public @NotNull String adapterId() {
        return adapterId;
    }

    @Override
    public void start() {
        commands.add("start");
        applyLifecycle(script.startBehavior(), output::started);
    }

    @Override
    public void stop() {
        commands.add("stop");
        applyLifecycle(script.stopBehavior(), output::stopped);
    }

    @Override
    public void connect() {
        commands.add("connect");
        connectAttempt++;
        applyLifecycle(script.connectBehaviorFor(connectAttempt), output::connected);
    }

    @Override
    public void disconnect() {
        commands.add("disconnect");
        applyLifecycle(script.disconnectBehavior(), output::disconnected);
    }

    @Override
    public void verifyBatch(final @NotNull List<Node> nodes) {
        commands.add("verifyBatch");
        for (final Node node : nodes) {
            final int attempt = verifyAttempts.merge(node, 1, Integer::sum);
            script.verifyResponseFor(node, attempt).ifPresent(outcome -> output.verifyResult(node, outcome));
        }
    }

    @Override
    public void pollBatch(final @NotNull List<Node> nodes) {
        commands.add("pollBatch");
        for (final Node node : nodes) {
            applyPoll(node, script.pollBehaviorFor(node));
        }
    }

    @Override
    public void addSubscriptionBatch(final @NotNull List<Node> nodes) {
        commands.add("addSubscriptionBatch");
        for (final Node node : nodes) {
            applySubscription(node, script.subscriptionBehaviorFor(node));
        }
    }

    @Override
    public void removeSubscriptionBatch(final @NotNull List<Node> nodes) {
        commands.add("removeSubscriptionBatch");
    }

    @Override
    public void writeBatch(final @NotNull List<WriteEntry> entries) {
        commands.add("writeBatch");
        for (final WriteEntry entry : entries) {
            final ChaosScript.WriteOutcome outcome = script.writeOutcomeFor(entry.node());
            output.writeResult(entry.node(), outcome.success(), outcome.reason());
        }
    }

    @Override
    public void browse(final @NotNull BrowseFilter filter) {
        commands.add("browse");
        final ChaosScript.BrowseOutcome outcome = script.browseOutcomeFor(filter.filterNode());
        if (outcome.durationTicks() <= 0) {
            output.browseResult(outcome.entries());
        } else {
            scheduleAt(currentTick + outcome.durationTicks(), () -> output.browseResult(outcome.entries()));
        }
    }

    // ── harness-driven time (design §10.2) ──────────────────────────────────────────────────────────────────────

    /**
     * Advance the simulator one harness tick and fire every deferred behavior that has come due. Called by the
     * harness once per advanced tick, after the clock told the wrapper its tick and before the dispatcher drains —
     * so a due acknowledgment ({@code EVENT}) is enqueued alongside the wrapper tick ({@code TICK}) and, by the
     * priority ladder, processed first (design §5.1, S24).
     */
    public void onTick() {
        currentTick++;
        final List<Runnable> due = new ArrayList<>();
        final List<Deferred> remaining = new ArrayList<>();
        for (final Deferred entry : deferred) {
            if (entry.fireAtTick() <= currentTick) {
                due.add(entry.action());
            } else {
                remaining.add(entry);
            }
        }
        deferred.clear();
        deferred.addAll(remaining);
        for (final Runnable action : due) {
            action.run();
        }
    }

    /**
     * @return the commands the wrapper has issued, in order — the source of {@code assertSequence} (design §10.3).
     */
    public @NotNull List<String> commands() {
        return commands;
    }

    /**
     * @return the current harness tick the simulator has advanced to.
     */
    public long currentTick() {
        return currentTick;
    }

    // ── behavior application ────────────────────────────────────────────────────────────────────────────────────

    private void applyLifecycle(final @NotNull ChaosBehavior behavior, final @NotNull Runnable onSucceed) {
        final int latency = script.acknowledgmentLatencyTicks();
        if (behavior instanceof ChaosBehavior.Delay || latency <= 0) {
            // An explicit Delay carries its own timing; otherwise an immediate behavior reports straight away.
            applyImmediate(behavior, onSucceed);
        } else {
            scheduleAt(currentTick + latency, () -> applyImmediate(behavior, onSucceed));
        }
    }

    private void applyImmediate(final @NotNull ChaosBehavior behavior, final @NotNull Runnable onSucceed) {
        switch (behavior) {
            case ChaosBehavior.Succeed ignored -> onSucceed.run();
            case ChaosBehavior.FailAdapter fail -> output.error(ErrorScope.ADAPTER, fail.reason());
            case ChaosBehavior.FailConnection fail -> output.error(ErrorScope.CONNECTION, fail.reason());
            case ChaosBehavior.Drop ignored -> {
                // Silent: the command is dropped, so the wrapper parks until its watchdog fires.
            }
            case ChaosBehavior.NoResponse ignored -> {
                // Silent: the command is recorded but never acknowledged.
            }
            case ChaosBehavior.Delay delay -> {
                if (delay.ticks() <= 0) {
                    applyImmediate(delay.then(), onSucceed);
                } else {
                    scheduleAt(currentTick + delay.ticks(), () -> applyImmediate(delay.then(), onSucceed));
                }
            }
        }
    }

    private void applyPoll(final @NotNull Node node, final @NotNull PollBehavior behavior) {
        switch (behavior) {
            case PollBehavior.Value value -> output.dataPoint(node, value.value());
            case PollBehavior.NodeErrorResponse error -> output.nodeError(node, error.reason(), false);
            case PollBehavior.NoResponse ignored -> {
                // The poll never returns; the read aspect waits and the next scheduled poll is the retry (§7.3).
            }
        }
    }

    private void applySubscription(final @NotNull Node node, final @Nullable SubscriptionBehavior behavior) {
        if (behavior == null) {
            // Unscripted subscribe: silent, leaving the read aspect in WAITING_FOR_SUBSCRIPTION (design §7.4).
            return;
        }
        switch (behavior) {
            case SubscriptionBehavior.Accept accept -> output.dataPoint(node, accept.firstValue());
            case SubscriptionBehavior.Fail fail -> output.nodeError(node, fail.reason(), false);
            case SubscriptionBehavior.LoseAfter lose -> {
                output.dataPoint(node, lose.firstValue());
                final int delay = Math.max(1, lose.ticks());
                scheduleAt(currentTick + delay, () -> output.nodeError(node, lose.reason(), lose.spontaneous()));
            }
        }
    }

    private void applyEvent(final @NotNull ChaosEvent event) {
        switch (event) {
            case ChaosEvent.Started ignored -> output.started();
            case ChaosEvent.Stopped ignored -> output.stopped();
            case ChaosEvent.Connected ignored -> output.connected();
            case ChaosEvent.Disconnect ignored -> output.disconnected();
            case ChaosEvent.ErrorReport error -> output.error(error.scope(), error.reason());
            case ChaosEvent.DataPointPush push -> output.dataPoint(push.node(), push.value());
            case ChaosEvent.NodeErrorPush push -> output.nodeError(push.node(), push.reason(), push.spontaneous());
        }
    }

    private void scheduleAt(final long fireAtTick, final @NotNull Runnable action) {
        deferred.add(new Deferred(fireAtTick, action));
    }

    private record Deferred(long fireAtTick, @NotNull Runnable action) {}
}
