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

import static com.hivemq.bridge.mqtt.BridgeReconnectDelay.MODE_PROPERTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hivemq.bridge.mqtt.BridgeReconnectDelay.Delay;
import com.hivemq.bridge.mqtt.BridgeReconnectDelay.Mode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BridgeReconnectDelayTest {

    private @Nullable String propertyBeforeTest;

    @BeforeEach
    void saveProperty() {
        propertyBeforeTest = System.getProperty(MODE_PROPERTY);
        System.clearProperty(MODE_PROPERTY);
    }

    @AfterEach
    void restoreProperty() {
        if (propertyBeforeTest == null) {
            System.clearProperty(MODE_PROPERTY);
        } else {
            System.setProperty(MODE_PROPERTY, propertyBeforeTest);
        }
    }

    @Test
    void baseDelayMs_whenExponential_thenDoublesUntilTheCeiling() {
        // The head of the sequence is the doubling; the tail is the plateau at
        // MIN_DELAY_MS << MAX_BACKOFF_EXPONENT, which every attempt from the seventh onwards waits.
        // Both halves are asserted here because the head alone would still pass if the cap were
        // raised, and the cap is the only thing keeping the shift in range.
        assertEquals(
                List.of(1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 32_000L, 64_000L, 128_000L, 128_000L, 128_000L),
                delaysForFirstAttempts(10, Mode.EXPONENTIAL));
    }

    @Test
    void baseDelayMs_whenExponentialAndAttemptsAbsurdlyHigh_thenStillTheCeilingAndPositive() {
        // Java shifts a long by only the low six bits of the operand, so without the exponent cap
        // 1000L << 54 would be negative and 1000L << 64 would wrap back to 1000 — a bridge down for
        // long enough would wait a negative time, then retry every second. Reaching 54 consecutive
        // failures takes around 100 minutes of continuous outage, so this is a case production can
        // arrive at rather than a theoretical one.
        assertEquals(128_000L, BridgeReconnectDelay.baseDelayMs(54, Mode.EXPONENTIAL));
        assertEquals(128_000L, BridgeReconnectDelay.baseDelayMs(64, Mode.EXPONENTIAL));
        assertEquals(128_000L, BridgeReconnectDelay.baseDelayMs(Integer.MAX_VALUE, Mode.EXPONENTIAL));
    }

    @Test
    void baseDelayMs_whenConstant_thenAlwaysOneSecond() {
        // The point of the mode: the wait before the next attempt never grows, so a test that
        // restores a broken connection can bound how long the bridge takes to come back.
        assertEquals(
                List.of(1_000L, 1_000L, 1_000L, 1_000L, 1_000L, 1_000L, 1_000L, 1_000L, 1_000L, 1_000L),
                delaysForFirstAttempts(10, Mode.CONSTANT));
        assertEquals(1_000L, BridgeReconnectDelay.baseDelayMs(Integer.MAX_VALUE, Mode.CONSTANT));
    }

    @Test
    void baseDelayMs_whenAttemptsNegative_thenTreatedAsFirstAttempt() {
        assertEquals(1_000L, BridgeReconnectDelay.baseDelayMs(-1, Mode.EXPONENTIAL));
        assertEquals(1_000L, BridgeReconnectDelay.baseDelayMs(-1, Mode.CONSTANT));
    }

    @Test
    void jitterMs_whenExponential_thenAtMostAQuarterOfTheDelay() {
        assertEquals(0L, BridgeReconnectDelay.jitterMs(8_000L, Mode.EXPONENTIAL, 0.0));
        assertEquals(1_000L, BridgeReconnectDelay.jitterMs(8_000L, Mode.EXPONENTIAL, 0.5));
        // Callers pass a value below 1.0, so the quarter is approached but not reached.
        assertEquals(1_999L, BridgeReconnectDelay.jitterMs(8_000L, Mode.EXPONENTIAL, 0.9999));
    }

    @Test
    void jitterMs_whenConstant_thenNone() {
        // A test selects CONSTANT to know when the retries happen; jitter would blur exactly that,
        // and the fleet-spreading it buys is pointless for the single bridge a test runs.
        assertEquals(0L, BridgeReconnectDelay.jitterMs(1_000L, Mode.CONSTANT, 0.9999));
    }

    @Test
    void currentMode_whenPropertyUnset_thenExponential() {
        assertEquals(Mode.EXPONENTIAL, BridgeReconnectDelay.currentMode());
    }

    @Test
    void currentMode_whenPropertyBlank_thenExponential() {
        System.setProperty(MODE_PROPERTY, "   ");
        assertEquals(Mode.EXPONENTIAL, BridgeReconnectDelay.currentMode());
    }

    @Test
    void currentMode_whenPropertySet_thenParsedIgnoringCaseAndSurroundingSpace() {
        System.setProperty(MODE_PROPERTY, " constant ");
        assertEquals(Mode.CONSTANT, BridgeReconnectDelay.currentMode());
    }

    @Test
    void currentMode_whenPropertyUnrecognised_thenFailsRatherThanFallingBack() {
        // Silently falling back to EXPONENTIAL would turn a typo in a test's JVM arguments into a
        // flaky test rather than a failing one: the test would wait five seconds for a reconnection
        // that backoff had pushed sixteen seconds out.
        System.setProperty(MODE_PROPERTY, "linear");
        final IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, BridgeReconnectDelay::currentMode);
        assertEquals(
                "Unknown bridge reconnect mode 'linear' in system property '"
                        + MODE_PROPERTY
                        + "'. Known modes: EXPONENTIAL, CONSTANT.",
                exception.getMessage());
    }

    @Test
    void nextDelay_thenReadsTheProperty() {
        System.setProperty(MODE_PROPERTY, "CONSTANT");
        assertEquals(new Delay(Mode.CONSTANT, 1_000L, 0L), BridgeReconnectDelay.nextDelay(5));

        // Read per call rather than cached, so that a test setting the property does not impose its
        // schedule on whatever else shares the JVM.
        System.clearProperty(MODE_PROPERTY);
        assertEquals(Mode.EXPONENTIAL, BridgeReconnectDelay.nextDelay(5).mode());
        assertEquals(32_000L, BridgeReconnectDelay.nextDelay(5).baseDelayMs());
    }

    @Test
    void nextDelay_whenExponential_thenTotalWithinTheJitterRange() {
        // nextDelay draws its own randomness, so the schedule is asserted exactly elsewhere and this
        // pins what the draw may do: never shorter than the schedule, never more than a quarter over.
        for (int attempts = 0; attempts < 10; attempts++) {
            final Delay delay = BridgeReconnectDelay.nextDelay(attempts);
            final long base = BridgeReconnectDelay.baseDelayMs(attempts, Mode.EXPONENTIAL);
            assertEquals(base, delay.baseDelayMs());
            assertTrue(delay.jitterMs() >= 0, "jitter must not shorten the delay: " + delay);
            assertTrue(
                    delay.jitterMs() <= (long) (base * BridgeReconnectDelay.JITTER_FACTOR),
                    "jitter must stay within JITTER_FACTOR of the delay: " + delay);
            assertEquals(delay.baseDelayMs() + delay.jitterMs(), delay.totalMs());
        }
    }

    @Test
    void nextDelay_whenConstant_thenExactlyOneSecondEveryTime() {
        // The property the Toxiproxy bridge tests rest on: with no jitter and no growth, the moment
        // of the next attempt is known, so a test can bound how long a reconnection takes.
        System.setProperty(MODE_PROPERTY, "CONSTANT");
        for (int attempts = 0; attempts < 10; attempts++) {
            assertEquals(new Delay(Mode.CONSTANT, 1_000L, 0L), BridgeReconnectDelay.nextDelay(attempts));
            assertEquals(1_000L, BridgeReconnectDelay.nextDelay(attempts).totalMs());
        }
    }

    private static List<Long> delaysForFirstAttempts(final int count, final Mode mode) {
        final List<Long> delays = new ArrayList<>();
        IntStream.range(0, count).forEach(attempts -> delays.add(BridgeReconnectDelay.baseDelayMs(attempts, mode)));
        return delays;
    }
}
