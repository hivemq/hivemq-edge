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
package com.hivemq.protocols.v2.runtime;

import org.jetbrains.annotations.NotNull;

/**
 * The exponential-backoff retry policy for an adapter type (design §5.6). A {@link Backoff} turns this into a
 * concrete delay sequence.
 *
 * @param initialMillis  the first retry delay, in milliseconds.
 * @param factor         the multiplier applied to each successive delay; must be at least 1.
 * @param ceilingMillis  the maximum retry delay, in milliseconds; delays never grow past it.
 * @param maximumRetries the number of retries the policy permits before it is exhausted.
 */
public record RetryPolicy(long initialMillis, double factor, long ceilingMillis, int maximumRetries) {

    public RetryPolicy {
        if (initialMillis <= 0) {
            throw new IllegalArgumentException("initialMillis must be positive, but was " + initialMillis);
        }
        if (factor < 1.0) {
            throw new IllegalArgumentException("factor must be at least 1.0, but was " + factor);
        }
        if (ceilingMillis < initialMillis) {
            throw new IllegalArgumentException(
                    "ceilingMillis (" + ceilingMillis + ") must be at least initialMillis (" + initialMillis + ")");
        }
        if (maximumRetries < 0) {
            throw new IllegalArgumentException("maximumRetries must not be negative, but was " + maximumRetries);
        }
    }

    /**
     * @return the default policy: 1 s, &times;1.41, capped at 32 s, unbounded retries — 1 s, 1.4 s, 2 s, 2.8 s,
     *         4 s, 5.7 s, 8 s, 11 s, 16 s, 22 s, 32 s, 32 s&hellip;
     */
    public static @NotNull RetryPolicy defaults() {
        return new RetryPolicy(1000, 1.41, 32000, Integer.MAX_VALUE);
    }
}
