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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BackoffTest {

    @Test
    void defaults_startWithTheDocumentedSequence() {
        final Backoff backoff = new Backoff(RetryPolicy.defaults());

        assertThat(backoff.nextDelayMillis()).isEqualTo(1000);
        assertThat(backoff.nextDelayMillis()).isEqualTo(1410);
        assertThat(backoff.nextDelayMillis()).isEqualTo(1988);
    }

    @Test
    void delays_areMonotonicAndConvergeOnTheCeiling() {
        final Backoff backoff = new Backoff(RetryPolicy.defaults());

        long previous = 0L;
        long delay = 0L;
        for (int i = 0; i < 50; i++) {
            delay = backoff.nextDelayMillis();
            assertThat(delay).isGreaterThanOrEqualTo(previous);
            assertThat(delay).isLessThanOrEqualTo(32000);
            previous = delay;
        }
        assertThat(delay).isEqualTo(32000);
    }

    @Test
    void ceiling_capsAndHolds() {
        final Backoff backoff = new Backoff(new RetryPolicy(1000, 2.0, 5000, Integer.MAX_VALUE));

        assertThat(backoff.nextDelayMillis()).isEqualTo(1000);
        assertThat(backoff.nextDelayMillis()).isEqualTo(2000);
        assertThat(backoff.nextDelayMillis()).isEqualTo(4000);
        assertThat(backoff.nextDelayMillis()).isEqualTo(5000);
        assertThat(backoff.nextDelayMillis()).isEqualTo(5000);
    }

    @Test
    void reset_returnsToTheStartOfTheSequence() {
        final Backoff backoff = new Backoff(RetryPolicy.defaults());
        backoff.nextDelayMillis();
        backoff.nextDelayMillis();

        backoff.reset();

        assertThat(backoff.nextDelayMillis()).isEqualTo(1000);
    }

    @Test
    void exhausted_flipsAfterMaximumRetriesAndClearsOnReset() {
        final Backoff backoff = new Backoff(new RetryPolicy(1000, 2.0, 32000, 3));

        assertThat(backoff.exhausted()).isFalse();
        backoff.nextDelayMillis();
        assertThat(backoff.exhausted()).isFalse();
        backoff.nextDelayMillis();
        assertThat(backoff.exhausted()).isFalse();
        backoff.nextDelayMillis();
        assertThat(backoff.exhausted()).isTrue();

        backoff.reset();
        assertThat(backoff.exhausted()).isFalse();
    }

    @Test
    void retryPolicy_rejectsInvalidParameters() {
        assertThatThrownBy(() -> new RetryPolicy(0, 1.41, 32000, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RetryPolicy(1000, 0.9, 32000, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RetryPolicy(1000, 1.41, 500, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RetryPolicy(1000, 1.41, 32000, -1)).isInstanceOf(IllegalArgumentException.class);
    }
}
