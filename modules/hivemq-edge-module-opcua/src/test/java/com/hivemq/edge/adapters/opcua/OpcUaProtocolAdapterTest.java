/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for OpcUaProtocolAdapter, focusing on exponential backoff retry logic.
 */
class OpcUaProtocolAdapterTest {

    /**
     * Tests the exponential backoff delay calculation using reflection to access the private method.
     * Verifies the backoff sequence: 1s, 2s, 4s, 8s, 16s, 32s, 64s, 128s, 256s, 300s (capped).
     */
    @ParameterizedTest
    @CsvSource({
            "1, 1000",      // First retry: 1 second
            "2, 2000",      // Second retry: 2 seconds
            "3, 4000",      // Third retry: 4 seconds
            "4, 8000",      // Fourth retry: 8 seconds
            "5, 16000",     // Fifth retry: 16 seconds
            "6, 32000",     // Sixth retry: 32 seconds
            "7, 64000",     // Seventh retry: 64 seconds
            "8, 128000",    // Eighth retry: 128 seconds
            "9, 256000",    // Ninth retry: 256 seconds
            "10, 300000",   // Tenth retry: 300 seconds (max)
            "11, 300000",   // Eleventh retry: still 300 seconds (capped)
            "20, 300000",   // Large attempt count: still 300 seconds (capped)
            "100, 300000",  // Very large attempt count: still 300 seconds (capped)
    })
    void testCalculateBackoffDelayMs_exponentialGrowthAndCapping(final int attemptCount, final long expectedDelayMs) {
        final long actualDelay = OpcUaProtocolAdapter.calculateBackoffDelayMs(attemptCount);
        assertThat(actualDelay).as("Backoff delay for attempt #%d should be %d ms", attemptCount, expectedDelayMs)
                .isEqualTo(expectedDelayMs);
    }

    /**
     * Tests that the backoff strategy correctly handles the maximum delay cap.
     * Any attempt count >= 10 should return the maximum delay of 300 seconds.
     */
    @Test
    void testCalculateBackoffDelayMs_capsAtMaximumDelay() {
        for (int attemptCount = 10; attemptCount <= 1000; attemptCount += 10) {
            final long actualDelay = OpcUaProtocolAdapter.calculateBackoffDelayMs(attemptCount);
            assertThat(actualDelay).as("Backoff delay for attempt #%d should be capped at 300 seconds", attemptCount)
                    .isEqualTo(300_000L);
        }
    }

    /**
     * Tests that the exponential backoff follows base-2 growth pattern.
     * Each delay should be double the previous one (until capped).
     */
    @Test
    void testCalculateBackoffDelayMs_followsExponentialPattern() {
        long previousDelay = 0;
        for (int attemptCount = 1; attemptCount <= 9; attemptCount++) {
            final long currentDelay = OpcUaProtocolAdapter.calculateBackoffDelayMs(attemptCount);

            if (attemptCount > 1) {
                assertThat(currentDelay).as("Delay for attempt #%d should be double the previous delay", attemptCount)
                        .isEqualTo(previousDelay * 2);
            }

            previousDelay = currentDelay;
        }

        // Verify that the 10th attempt doesn't follow the exponential pattern (it's capped)
        final long tenthAttemptDelay = OpcUaProtocolAdapter.calculateBackoffDelayMs(10);
        assertThat(tenthAttemptDelay).as("10th attempt should be capped, not double the 9th")
                .isLessThan(previousDelay * 2)
                .isEqualTo(300_000L);
    }

}
