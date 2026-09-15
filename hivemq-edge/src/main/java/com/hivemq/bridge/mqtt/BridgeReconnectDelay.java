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
package com.hivemq.bridge.mqtt;

import java.util.concurrent.ThreadLocalRandom;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decides how long a bridge waits before its next reconnection attempt.
 * <p>
 * The production schedule is exponential: the delay doubles with every consecutive failed attempt,
 * so a remote broker that stays down is retried ever more rarely instead of being hammered. On top
 * of that sits additive jitter, which spreads the attempts of many bridges that lost the same remote
 * broker at the same moment.
 * <p>
 * That schedule is a poor fit for tests that reproduce reconnection defects. Such a test breaks the
 * connection, waits for the bridge to give up, restores the connection and then waits for it to come
 * back. Under the exponential schedule the second wait is not bounded by how quickly the bridge
 * <i>can</i> reconnect but by how much backoff the first wait accumulated: after five failures the
 * next attempt is already 16 seconds away, and after seven it has reached its ceiling of 128 seconds.
 * Tests therefore end up sleeping for the backoff rather than waiting for the bridge, which is why
 * the Toxiproxy bridge tests carried 30-second toxic windows.
 * <p>
 * {@link Mode#CONSTANT} exists for those tests. It retries at a fixed one-second interval, so the
 * time to the next attempt never exceeds one second no matter how long the outage lasted, and a test
 * can wait for the reconnection itself instead of for the schedule. It is deliberately not reachable
 * from configuration: it is selected by the system property {@value #MODE_PROPERTY}, which a test
 * sets on the JVM that runs it.
 *
 * @see <a href="https://linear.app/hivemq/issue/EDG-862">EDG-862</a>
 */
public final class BridgeReconnectDelay {

    private static final @NotNull Logger log = LoggerFactory.getLogger(BridgeReconnectDelay.class);

    /**
     * System property selecting the retry schedule. Unset means {@link Mode#EXPONENTIAL}.
     * <p>
     * This is a testing switch, not a configuration option. It is read from the system properties
     * rather than the Edge configuration so that it cannot be set by a deployment, and it is
     * deliberately noisy: selecting a non-production schedule is logged at WARN, and an unrecognised
     * value fails rather than silently reverting to the production schedule. A test that believes it
     * disabled backoff but did not would not fail, it would flake.
     */
    public static final @NotNull String MODE_PROPERTY = "hivemq.bridge.reconnect.mode";

    /**
     * Delay before the first retry, and the fixed delay of {@link Mode#CONSTANT}.
     */
    static final long MIN_DELAY_MS = 1_000; // 1 second

    /**
     * Upper bound on the exponent, and so on the delay: the doubling stops at
     * {@code MIN_DELAY_MS << MAX_BACKOFF_EXPONENT}, and every further attempt waits that long.
     * <p>
     * It bounds the exponent rather than the delay because it does two jobs at once. The delay it
     * yields is the policy — a bridge whose remote broker stays down still retries about every two
     * minutes, so it recovers on its own once the broker returns. Capping the exponent is also what
     * keeps {@code MIN_DELAY_MS << attempts} in range: Java shifts a long by only the low six bits
     * of its operand, so an uncapped exponent would turn negative at 54 consecutive failures and
     * wrap back to a one-second delay at 64, converting the backoff into the hot retry loop it
     * exists to prevent.
     * <p>
     * Expressing the ceiling this way costs the freedom to choose a round number: the reachable
     * ceilings are powers of two times a second, so 128 seconds rather than 120. At the point where
     * a bridge has failed seven times in a row that difference is immaterial — jitter already
     * spreads the delay across a wider range than that — and the alternative, a second constant
     * clamping the result, reads as redundant with this one and invites deleting whichever of the
     * two looks unused.
     */
    static final int MAX_BACKOFF_EXPONENT = 7; // 2^7 = 128 seconds, about two minutes

    /**
     * Fraction of the delay that may be added as jitter, so that bridges which failed together do
     * not retry together.
     */
    static final double JITTER_FACTOR = 0.25; // 25% max jitter

    public enum Mode {
        /**
         * Production: 1s, 2s, 4s, 8s, 16s, 32s, 64s, then 128s for every further attempt.
         */
        EXPONENTIAL,

        /**
         * Testing only: 1s before every attempt, however many have failed. Retry moments are then
         * one second apart, so a test can bound how long a reconnection takes.
         */
        CONSTANT;

        static @NotNull Mode parse(final @NotNull String value) {
            for (final Mode mode : values()) {
                if (mode.name().equalsIgnoreCase(value)) {
                    return mode;
                }
            }
            throw new IllegalArgumentException("Unknown bridge reconnect mode '"
                    + value
                    + "' in system property '"
                    + MODE_PROPERTY
                    + "'. Known modes: EXPONENTIAL, CONSTANT.");
        }
    }

    private BridgeReconnectDelay() {}

    /**
     * The currently selected mode.
     * <p>
     * Read on every call rather than cached, because the tests that set it share a JVM with tests
     * that must not have it: a value captured when this class was first loaded would leak the
     * schedule of whichever test happened to run first.
     *
     * @throws IllegalArgumentException if the property holds a value that is not a mode
     */
    public static @NotNull Mode currentMode() {
        final String configured = System.getProperty(MODE_PROPERTY);
        if (configured == null || configured.isBlank()) {
            return Mode.EXPONENTIAL;
        }
        final Mode mode = Mode.parse(configured.trim());
        if (mode != Mode.EXPONENTIAL) {
            log.warn(
                    "Bridge reconnection is using the non-production '{}' schedule, selected by system property '{}'. "
                            + "This disables exponential backoff and is intended for tests only.",
                    mode,
                    MODE_PROPERTY);
        }
        return mode;
    }

    /**
     * How long to wait before the next reconnection attempt, and why.
     *
     * @param mode     the schedule that produced it
     * @param baseDelayMs the delay the schedule gives for this attempt count
     * @param jitterMs    the spread added on top
     */
    public record Delay(@NotNull Mode mode, long baseDelayMs, long jitterMs) {

        /**
         * What the caller waits: the schedule's delay plus its jitter.
         */
        public long totalMs() {
            return baseDelayMs + jitterMs;
        }
    }

    /**
     * How long to wait before the next reconnection attempt.
     * <p>
     * The randomness for the jitter is drawn here rather than asked of the caller, so that deciding
     * a reconnection delay is one call and callers cannot get the spread wrong. The parts are
     * returned alongside the total because the caller logs them separately, and a bridge that
     * reconnects later than expected is much easier to explain when the log distinguishes a long
     * schedule from a large draw.
     *
     * @param attempts number of attempts that have already failed in this reconnection chain; zero
     *                 for the first retry after a connection that had been established
     * @return the delay, its jitter, and the schedule they came from
     * @throws IllegalArgumentException if {@value #MODE_PROPERTY} holds a value that is not a mode
     */
    public static @NotNull Delay nextDelay(final int attempts) {
        final Mode mode = currentMode();
        final long baseDelayMs = baseDelayMs(attempts, mode);
        return new Delay(
                mode,
                baseDelayMs,
                jitterMs(baseDelayMs, mode, ThreadLocalRandom.current().nextDouble()));
    }

    /**
     * Delay before the next attempt under an explicitly given mode, without jitter.
     * <p>
     * Pure: the same arguments always give the same answer, which is what lets the schedule be
     * asserted exactly rather than by its range.
     *
     * @param attempts number of attempts that have already failed in this reconnection chain
     * @param mode     the schedule to apply
     * @return the delay in milliseconds, at least {@link #MIN_DELAY_MS} and at most
     * {@code MIN_DELAY_MS << MAX_BACKOFF_EXPONENT}
     */
    static long baseDelayMs(final int attempts, final @NotNull Mode mode) {
        if (mode == Mode.CONSTANT) {
            return MIN_DELAY_MS;
        }
        final int exponent = Math.min(Math.max(attempts, 0), MAX_BACKOFF_EXPONENT);
        return MIN_DELAY_MS << exponent;
    }

    /**
     * Jitter to add to a base delay, spreading the retries of bridges that failed together.
     * <p>
     * {@link Mode#CONSTANT} adds none: a test selects it precisely to know when the retries happen,
     * and jitter would blur those moments for no benefit, since a test runs one bridge rather than a
     * fleet of them.
     *
     * @param baseDelayMs the delay from {@link #baseDelayMs(int, Mode)}
     * @param mode        the schedule in effect
     * @param random      a value in {@code [0, 1)}; taken as a parameter rather than drawn here so
     *                    that the spread can be asserted exactly
     * @return milliseconds to add, in {@code [0, baseDelayMs * JITTER_FACTOR)}
     */
    static long jitterMs(final long baseDelayMs, final @NotNull Mode mode, final double random) {
        if (mode == Mode.CONSTANT) {
            return 0;
        }
        return (long) (baseDelayMs * JITTER_FACTOR * random);
    }
}
