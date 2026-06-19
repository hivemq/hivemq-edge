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
package com.hivemq.protocols.v2.wrapper;

import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.CONNECTED;
import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.adapter.sdk.api.v2.model.VerifyOutcome;
import com.hivemq.protocols.v2.view.TagStatus;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The shared verification model and tag retry for a read-and-write tag (design §7.6; scenarios S29 and the
 * verification optimization at unit level). One {@code verifyBatch} entry serves both aspects; one {@code Success}
 * advances both, one {@code PermanentFailure} suspends both; and a runtime tag retry re-verifies a permanently
 * failed tag and resets its counters without touching configuration.
 */
class TagVerificationAndRetryTest {

    private static @NotNull WrapperTestFixture readAndWriteFixture() {
        return WrapperTestFixture.builder()
                .runningCoordinator()
                .nodes(List.of(WrapperTestSupport.pair("temperature")))
                .readUsed(Set.of("temperature"))
                .writeUsed(Set.of("temperature"))
                .pollIntervalMillis(1000)
                .build();
    }

    private static long verifyBatchCount(final @NotNull WrapperTestFixture fixture) {
        return fixture.commands().stream().filter("verifyBatch"::equals).count();
    }

    @Test
    void readAndWriteTag_verifiesOnce_andOneSuccessAdvancesBothAspects() {
        final WrapperTestFixture fixture = readAndWriteFixture();

        fixture.activate(ProtocolAdapterDirection.BOTH);

        assertThat(fixture.state()).isEqualTo(CONNECTED);
        // Exactly one verifyBatch for the connect gate — the single result served both aspects (design §7.6).
        assertThat(verifyBatchCount(fixture)).isEqualTo(1);
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
        assertThat(fixture.writeState("temperature")).isEqualTo("WAITING_FOR_WRITE_REQUEST");
        assertThat(fixture.tagStatus("temperature")).isEqualTo(TagStatus.NORTHBOUND_AND_SOUTHBOUND);
    }

    @Test
    void onePermanentFailure_suspendsBothAspects() {
        final WrapperTestFixture fixture = readAndWriteFixture();
        fixture.adapter.verifyOutcome = new VerifyOutcome.PermanentFailure("unknown address");

        fixture.activate(ProtocolAdapterDirection.BOTH);

        assertThat(fixture.state()).isEqualTo(CONNECTED);
        assertThat(verifyBatchCount(fixture)).isEqualTo(1);
        assertThat(fixture.readState("temperature")).isEqualTo("ERROR_PERMANENT_VERIFICATION_FAILURE");
        assertThat(fixture.writeState("temperature")).isEqualTo("ERROR_PERMANENT_VERIFICATION_FAILURE");
        assertThat(fixture.tagStatus("temperature")).isEqualTo(TagStatus.ERROR);
    }

    @Test
    void retry_reVerifiesAPermanentlyFailedTag_resetsCounters_andRestoresBothAspects() {
        final WrapperTestFixture fixture = readAndWriteFixture();
        fixture.adapter.verifyOutcome = new VerifyOutcome.PermanentFailure("unknown address");
        fixture.activate(ProtocolAdapterDirection.BOTH);
        assertThat(fixture.readState("temperature")).isEqualTo("ERROR_PERMANENT_VERIFICATION_FAILURE");
        assertThat(fixture.writeState("temperature")).isEqualTo("ERROR_PERMANENT_VERIFICATION_FAILURE");
        assertThat(fixture.tag("temperature").failureCount()).isEqualTo(2); // read + write each counted once

        // The device is fixed; a runtime tag retry re-verifies the tag (design §7.6, EDG-462).
        fixture.adapter.verifyOutcome = new VerifyOutcome.Success();
        fixture.retryTag("temperature");

        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
        assertThat(fixture.writeState("temperature")).isEqualTo("WAITING_FOR_WRITE_REQUEST");
        assertThat(fixture.tag("temperature").failureCount()).isZero(); // counters reset
        assertThat(fixture.tagStatus("temperature")).isEqualTo(TagStatus.NORTHBOUND_AND_SOUTHBOUND);
    }

    @Test
    void retry_onAHealthyTag_isANoOp() {
        final WrapperTestFixture fixture = readAndWriteFixture();
        fixture.activate(ProtocolAdapterDirection.BOTH);
        final long verifyBatchesBeforeRetry = verifyBatchCount(fixture);

        fixture.retryTag("temperature"); // neither aspect is permanently failed — nothing to retry

        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
        assertThat(fixture.writeState("temperature")).isEqualTo("WAITING_FOR_WRITE_REQUEST");
        assertThat(verifyBatchCount(fixture)).isEqualTo(verifyBatchesBeforeRetry); // no re-verification issued
    }
}
