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

import com.hivemq.adapter.sdk.api.v2.model.BrowseResultEntry;
import com.hivemq.adapter.sdk.api.v2.model.VerifyOutcome;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The immutable, declarative script a {@link ChaosProtocolAdapter} consults per command. It maps
 * each lifecycle command to a {@link ChaosBehavior} and each node to its verification, poll, subscription, write,
 * and browse behaviors, plus events to inject at chosen harness ticks and a global acknowledgment latency that
 * exercises the wrapper's watchdogs.
 * <p>
 * Per-node behaviors are resolved by the <b>first matching rule</b>, so a specific {@link NodeMatcher#byId(String)}
 * rule declared before {@link NodeMatcher#all()} overrides the catch-all. Unscripted commands fall back to safe
 * defaults: lifecycle commands {@link ChaosBehavior#succeed() succeed}, verification {@link VerifyOutcome.Success
 * succeeds}, a poll returns {@link PollBehavior#noResponse() nothing}, a write succeeds, and a browse returns no
 * entries. There is deliberately no default subscription behavior — an unscripted subscribe is silent, leaving the
 * read aspect waiting.
 * <p>
 * <b>Time contract.</b> The script holds no timers; every deferral ({@link ChaosBehavior.Delay},
 * {@link #acknowledgmentLatencyTicks()}, {@link #injectedEvents()}, {@link SubscriptionBehavior.LoseAfter}) is
 * driven by the harness, which advances the shared {@code FakeClock} and pumps the simulator one tick at a time
 *, keeping every test fully deterministic.
 */
public final class ChaosScript {

    private final @NotNull ChaosBehavior startBehavior;
    private final @NotNull List<ChaosBehavior> connectBehaviors;
    private final @NotNull ChaosBehavior disconnectBehavior;
    private final @NotNull ChaosBehavior stopBehavior;
    private final @NotNull List<VerifyRule> verifyRules;
    private final @NotNull List<PollRule> pollRules;
    private final @NotNull List<SubscriptionRule> subscriptionRules;
    private final @NotNull List<WriteRule> writeRules;
    private final @NotNull List<BrowseRule> browseRules;
    private final @NotNull List<InjectedEvent> injectedEvents;
    private final int acknowledgmentLatencyTicks;

    private ChaosScript(final @NotNull Builder builder) {
        this.startBehavior = builder.startBehavior;
        this.connectBehaviors = List.copyOf(builder.connectBehaviors);
        this.disconnectBehavior = builder.disconnectBehavior;
        this.stopBehavior = builder.stopBehavior;
        this.verifyRules = List.copyOf(builder.verifyRules);
        this.pollRules = List.copyOf(builder.pollRules);
        this.subscriptionRules = List.copyOf(builder.subscriptionRules);
        this.writeRules = List.copyOf(builder.writeRules);
        this.browseRules = List.copyOf(builder.browseRules);
        this.injectedEvents = List.copyOf(builder.injectedEvents);
        this.acknowledgmentLatencyTicks = builder.acknowledgmentLatencyTicks;
    }

    /**
     * @return a new builder for a {@code ChaosScript}.
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    // ── resolution (consulted by the simulator) ─────────────────────────────────────────────────────────────────

    /**
     * @return the behavior for {@code start()}.
     */
    public @NotNull ChaosBehavior startBehavior() {
        return startBehavior;
    }

    /**
     * @param attempt the 1-based connect attempt number.
     * @return the behavior for that {@code connect()} attempt; the last scripted behavior repeats for every
     *         further attempt, so a single {@link Builder#onConnect(ChaosBehavior)} applies to every attempt.
     */
    public @NotNull ChaosBehavior connectBehaviorFor(final int attempt) {
        final int index = Math.clamp(attempt, 1, connectBehaviors.size()) - 1;
        return connectBehaviors.get(index);
    }

    /**
     * @return the behavior for {@code disconnect()}.
     */
    public @NotNull ChaosBehavior disconnectBehavior() {
        return disconnectBehavior;
    }

    /**
     * @return the behavior for {@code stop()}.
     */
    public @NotNull ChaosBehavior stopBehavior() {
        return stopBehavior;
    }

    /**
     * @param node    the node being verified.
     * @param attempt the 1-based verification attempt for the node (the first verify is attempt 1, a retry is
     *                attempt 2, …); the last scripted response repeats for further attempts.
     * @return the first matching verification response — present with the scripted outcome, or empty for a
     *         {@code verifyNoResponse} response (the adapter stays silent, exercising the verification watchdog). When no rule matches, a {@link VerifyOutcome.Success} is returned.
     */
    public @NotNull Optional<VerifyOutcome> verifyResponseFor(final @NotNull Node node, final int attempt) {
        for (final VerifyRule rule : verifyRules) {
            if (rule.matcher().matches(node)) {
                final int index = Math.clamp(attempt, 1, rule.responses().size()) - 1;
                return rule.responses().get(index);
            }
        }
        return Optional.of(new VerifyOutcome.Success());
    }

    /**
     * @param node the node being polled.
     * @return the first matching poll behavior, or {@link PollBehavior#noResponse()} when none matches.
     */
    public @NotNull PollBehavior pollBehaviorFor(final @NotNull Node node) {
        for (final PollRule rule : pollRules) {
            if (rule.matcher().matches(node)) {
                return rule.behavior();
            }
        }
        return PollBehavior.noResponse();
    }

    /**
     * @param node the node being subscribed.
     * @return the first matching subscription behavior, or {@code null} when none matches (the subscribe is
     *         silent, leaving the read aspect waiting).
     */
    public @Nullable SubscriptionBehavior subscriptionBehaviorFor(final @NotNull Node node) {
        for (final SubscriptionRule rule : subscriptionRules) {
            if (rule.matcher().matches(node)) {
                return rule.behavior();
            }
        }
        return null;
    }

    /**
     * @param node the node being written.
     * @return the first matching write outcome — present with the scripted result, or empty for a
     *         {@code writeNoResponse} rule (the adapter records the write but never acknowledges it, so the write
     *         aspect parks in {@code WAITING_FOR_WRITE_RESULT}). When no rule matches, a success is returned.
     */
    public @NotNull Optional<WriteOutcome> writeOutcomeFor(final @NotNull Node node) {
        for (final WriteRule rule : writeRules) {
            if (rule.matcher().matches(node)) {
                return Optional.ofNullable(rule.outcome());
            }
        }
        return Optional.of(new WriteOutcome(true, null));
    }

    /**
     * @param filterNode the browse filter's node.
     * @return the first matching browse outcome, or an empty, immediate result when none matches.
     */
    public @NotNull BrowseOutcome browseOutcomeFor(final @NotNull Node filterNode) {
        for (final BrowseRule rule : browseRules) {
            if (rule.matcher().matches(filterNode)) {
                return new BrowseOutcome(rule.results(), rule.durationTicks());
            }
        }
        return new BrowseOutcome(List.of(), 0);
    }

    /**
     * @return the events to inject, each at its chosen harness tick.
     */
    public @NotNull List<InjectedEvent> injectedEvents() {
        return injectedEvents;
    }

    /**
     * @return the global acknowledgment latency, in harness ticks; {@code 0} means immediate.
     */
    public int acknowledgmentLatencyTicks() {
        return acknowledgmentLatencyTicks;
    }

    // ── value types ─────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * The resolved outcome of a write.
     *
     * @param success whether the write succeeded.
     * @param reason  the failure reason, or {@code null} on success.
     */
    public record WriteOutcome(boolean success, @Nullable String reason) {}

    /**
     * The resolved outcome of a browse.
     *
     * @param entries       the discovered nodes.
     * @param durationTicks how many harness ticks the browse takes before it reports; {@code 0} is immediate.
     */
    public record BrowseOutcome(@NotNull List<BrowseResultEntry> entries, int durationTicks) {}

    /**
     * An event scheduled for a chosen harness tick.
     *
     * @param tick  the harness tick at which to report the event (the first {@code advance} tick is tick 1).
     * @param event the event to report.
     */
    public record InjectedEvent(long tick, @NotNull ChaosEvent event) {}

    private record VerifyRule(
            @NotNull NodeMatcher matcher, @NotNull List<Optional<VerifyOutcome>> responses) {}

    private record PollRule(
            @NotNull NodeMatcher matcher, @NotNull PollBehavior behavior) {}

    private record SubscriptionRule(
            @NotNull NodeMatcher matcher, @NotNull SubscriptionBehavior behavior) {}

    private record WriteRule(
            @NotNull NodeMatcher matcher, @Nullable WriteOutcome outcome) {}

    private record BrowseRule(
            @NotNull NodeMatcher matcher, @NotNull List<BrowseResultEntry> results, int durationTicks) {}

    // ── builder ─────────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Builds a {@link ChaosScript}. Every command without an explicit rule keeps the safe default described on
     * {@link ChaosScript}.
     */
    public static final class Builder {

        private @NotNull ChaosBehavior startBehavior = ChaosBehavior.succeed();
        private @NotNull List<ChaosBehavior> connectBehaviors = List.of(ChaosBehavior.succeed());
        private @NotNull ChaosBehavior disconnectBehavior = ChaosBehavior.succeed();
        private @NotNull ChaosBehavior stopBehavior = ChaosBehavior.succeed();
        private final @NotNull List<VerifyRule> verifyRules = new ArrayList<>();
        private final @NotNull List<PollRule> pollRules = new ArrayList<>();
        private final @NotNull List<SubscriptionRule> subscriptionRules = new ArrayList<>();
        private final @NotNull List<WriteRule> writeRules = new ArrayList<>();
        private final @NotNull List<BrowseRule> browseRules = new ArrayList<>();
        private final @NotNull List<InjectedEvent> injectedEvents = new ArrayList<>();
        private int acknowledgmentLatencyTicks;

        private Builder() {}

        /**
         * @param behavior how {@code start()} is answered.
         * @return this builder.
         */
        public @NotNull Builder onStart(final @NotNull ChaosBehavior behavior) {
            this.startBehavior = behavior;
            return this;
        }

        /**
         * @param behavior how every {@code connect()} attempt is answered.
         * @return this builder.
         */
        public @NotNull Builder onConnect(final @NotNull ChaosBehavior behavior) {
            this.connectBehaviors = List.of(behavior);
            return this;
        }

        /**
         * Script {@code connect()} per attempt — the first behavior answers the first attempt, and so on; the last
         * repeats for every further attempt. Models a connect that fails then recovers after a backoff, mirroring the {@code MockProtocolAdapter} connect-reply queue.
         *
         * @param behaviors the behaviors, one per attempt; must not be empty.
         * @return this builder.
         */
        public @NotNull Builder onConnectSequence(final @NotNull List<ChaosBehavior> behaviors) {
            if (behaviors.isEmpty()) {
                throw new IllegalArgumentException("the connect behavior sequence must not be empty");
            }
            this.connectBehaviors = List.copyOf(behaviors);
            return this;
        }

        /**
         * @param behavior how {@code disconnect()} is answered.
         * @return this builder.
         */
        public @NotNull Builder onDisconnect(final @NotNull ChaosBehavior behavior) {
            this.disconnectBehavior = behavior;
            return this;
        }

        /**
         * @param behavior how {@code stop()} is answered.
         * @return this builder.
         */
        public @NotNull Builder onStop(final @NotNull ChaosBehavior behavior) {
            this.stopBehavior = behavior;
            return this;
        }

        /**
         * @param matcher the nodes the outcome applies to.
         * @param outcome the verification outcome to report for matching nodes.
         * @return this builder.
         */
        public @NotNull Builder verify(final @NotNull NodeMatcher matcher, final @NotNull VerifyOutcome outcome) {
            verifyRules.add(new VerifyRule(matcher, List.of(Optional.of(outcome))));
            return this;
        }

        /**
         * Script verification per attempt for matching nodes — the first outcome answers the first verify, and so
         * on; the last repeats. Models a transient failure that clears on retry, or a permanent failure that
         * clears on a tag retry.
         *
         * @param matcher  the nodes the sequence applies to.
         * @param outcomes the outcomes, one per attempt; must not be empty.
         * @return this builder.
         */
        public @NotNull Builder verifySequence(
                final @NotNull NodeMatcher matcher, final @NotNull List<VerifyOutcome> outcomes) {
            if (outcomes.isEmpty()) {
                throw new IllegalArgumentException("the verify outcome sequence must not be empty");
            }
            final List<Optional<VerifyOutcome>> responses = new ArrayList<>(outcomes.size());
            for (final VerifyOutcome outcome : outcomes) {
                responses.add(Optional.of(outcome));
            }
            verifyRules.add(new VerifyRule(matcher, List.copyOf(responses)));
            return this;
        }

        /**
         * Make verification stay silent for matching nodes: the adapter records the {@code verifyBatch} but never
         * reports an outcome, so the wrapper's verification watchdog eventually fires.
         *
         * @param matcher the nodes that never report a verification outcome.
         * @return this builder.
         */
        public @NotNull Builder verifyNoResponse(final @NotNull NodeMatcher matcher) {
            verifyRules.add(new VerifyRule(matcher, List.of(Optional.empty())));
            return this;
        }

        /**
         * @param matcher  the nodes the behavior applies to.
         * @param behavior how a poll for matching nodes is answered.
         * @return this builder.
         */
        public @NotNull Builder poll(final @NotNull NodeMatcher matcher, final @NotNull PollBehavior behavior) {
            pollRules.add(new PollRule(matcher, behavior));
            return this;
        }

        /**
         * @param matcher  the nodes the behavior applies to.
         * @param behavior how an add-subscription for matching nodes is answered.
         * @return this builder.
         */
        public @NotNull Builder subscribe(
                final @NotNull NodeMatcher matcher, final @NotNull SubscriptionBehavior behavior) {
            subscriptionRules.add(new SubscriptionRule(matcher, behavior));
            return this;
        }

        /**
         * @param matcher the nodes the outcome applies to.
         * @param success whether a write to matching nodes succeeds.
         * @param reason  the failure reason, or {@code null} on success.
         * @return this builder.
         */
        public @NotNull Builder write(
                final @NotNull NodeMatcher matcher, final boolean success, final @Nullable String reason) {
            writeRules.add(new WriteRule(matcher, new WriteOutcome(success, reason)));
            return this;
        }

        /**
         * Make a write stay silent for matching nodes: the adapter records the {@code writeBatch} but never reports a
         * write result, so the tag's write aspect parks in {@code WAITING_FOR_WRITE_RESULT} until the next event —
         * the write-side mirror of {@link #verifyNoResponse(NodeMatcher)} and {@link PollBehavior#noResponse()}.
         *
         * @param matcher the nodes whose write is never acknowledged.
         * @return this builder.
         */
        public @NotNull Builder writeNoResponse(final @NotNull NodeMatcher matcher) {
            writeRules.add(new WriteRule(matcher, null));
            return this;
        }

        /**
         * @param matcher       the filter nodes the result applies to.
         * @param results       the entries a matching browse reports.
         * @param durationTicks how many harness ticks the browse takes before it reports; {@code 0} is immediate.
         * @return this builder.
         */
        public @NotNull Builder browse(
                final @NotNull NodeMatcher matcher,
                final @NotNull List<BrowseResultEntry> results,
                final int durationTicks) {
            browseRules.add(new BrowseRule(matcher, List.copyOf(results), durationTicks));
            return this;
        }

        /**
         * @param tick  the harness tick at which to report the event (the first {@code advance} tick is tick 1).
         * @param event the event to report.
         * @return this builder.
         */
        public @NotNull Builder injectAtTick(final long tick, final @NotNull ChaosEvent event) {
            injectedEvents.add(new InjectedEvent(tick, event));
            return this;
        }

        /**
         * @param count the global acknowledgment latency, in harness ticks; {@code 0} means immediate.
         * @return this builder.
         */
        public @NotNull Builder acknowledgmentLatencyTicks(final int count) {
            this.acknowledgmentLatencyTicks = count;
            return this;
        }

        /**
         * @return the immutable script.
         */
        public @NotNull ChaosScript build() {
            return new ChaosScript(this);
        }
    }
}
