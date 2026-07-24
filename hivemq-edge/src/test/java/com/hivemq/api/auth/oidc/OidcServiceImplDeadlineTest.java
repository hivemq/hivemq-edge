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
package com.hivemq.api.auth.oidc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OidcServiceImpl#withDeadline}: a call finishing inside the deadline returns,
 * and a call exceeding it is aborted with an {@link IOException} and interrupted.
 */
class OidcServiceImplDeadlineTest {

    @Test
    void withDeadline_returnsWhenTheCallFinishesInTime() throws Exception {
        final String result = OidcServiceImpl.withDeadline(() -> "ok", 2000L);

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void withDeadline_abortsAndInterruptsACallThatExceedsTheDeadline() throws Exception {
        final CountDownLatch interrupted = new CountDownLatch(1);

        assertThatThrownBy(() -> OidcServiceImpl.withDeadline(
                        () -> {
                            try {
                                Thread.sleep(10_000L);
                            } catch (final InterruptedException e) {
                                interrupted.countDown();
                                throw e;
                            }
                            return "should not reach here";
                        },
                        100L))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("did not respond within");

        // The stuck call is cancelled with interruption, so the worker thread does not linger.
        assertThat(interrupted.await(2, TimeUnit.SECONDS))
                .as("the timed-out call was interrupted")
                .isTrue();
    }

    @Test
    void withDeadline_propagatesTheCallsOwnException() {
        assertThatThrownBy(() -> OidcServiceImpl.withDeadline(
                        () -> {
                            throw new IOException("boom");
                        },
                        2000L))
                .isInstanceOf(IOException.class)
                .hasMessage("boom");
    }
}
