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
 * The write aspect (design §7.5; scenario S4 and the S30 write-only fold at unit level). The aspect verifies on
 * connect, then rests ready for writes; a write arriving requests it and waits for the acknowledgment; a write
 * failure is logged and counted but never flaps the tag to {@code ERROR}; one write is in flight at a time; an
 * unused or permanently-failed write aspect is off. All driven on {@code FakeClock} + {@code ManualDispatcher}
 * through the running coordinator, observed only through the published snapshot.
 */
class TagAspectWriteTest {

    private static @NotNull WrapperTestFixture writeOnlyFixture() {
        return WrapperTestFixture.builder()
                .runningCoordinator()
                .nodes(List.of(WrapperTestSupport.pair("setpoint")))
                .readUsed(Set.of()) // no northbound mapping — the read aspect stays deactivated
                .writeUsed(Set.of("setpoint"))
                .build();
    }

    @Test
    void verifyOnConnect_thenRestsReadyForWrites() {
        final WrapperTestFixture fixture = writeOnlyFixture();

        fixture.activate(ProtocolAdapterDirection.SOUTHBOUND);

        assertThat(fixture.state()).isEqualTo(CONNECTED);
        assertThat(fixture.writeState("setpoint")).isEqualTo("WAITING_FOR_WRITE_REQUEST");
        assertThat(fixture.readState("setpoint")).isEqualTo("DEACTIVATED");
        assertThat(fixture.tagStatus("setpoint")).isEqualTo(TagStatus.SOUTHBOUND_ONLY);
    }

    @Test
    void writeHappyPath_requestsWriteThenReturnsToRequestOnSuccess() {
        final WrapperTestFixture fixture = writeOnlyFixture();
        fixture.activate(ProtocolAdapterDirection.SOUTHBOUND);

        fixture.submitWrite("setpoint", WrapperTestSupport.dataPoint("setpoint", "42"));
        assertThat(fixture.writeState("setpoint")).isEqualTo("WAITING_FOR_WRITE_RESULT");

        fixture.advance(100); // a tick dispatches the pending write batch to the adapter
        assertThat(fixture.commands()).contains("writeBatch");

        fixture.output.writeResult(fixture.nodeFor("setpoint"), true, null);
        fixture.drain();
        assertThat(fixture.writeState("setpoint")).isEqualTo("WAITING_FOR_WRITE_REQUEST");
        assertThat(fixture.tag("setpoint").failureCount()).isZero();
        assertThat(fixture.tagStatus("setpoint")).isEqualTo(TagStatus.SOUTHBOUND_ONLY);
    }

    @Test
    void writeFailure_countsAndReturnsToRequestWithoutFlappingToError() {
        final WrapperTestFixture fixture = writeOnlyFixture();
        fixture.activate(ProtocolAdapterDirection.SOUTHBOUND);
        fixture.submitWrite("setpoint", WrapperTestSupport.dataPoint("setpoint", "42"));

        fixture.output.writeResult(fixture.nodeFor("setpoint"), false, "device rejected the value");
        fixture.drain();

        assertThat(fixture.writeState("setpoint")).isEqualTo("WAITING_FOR_WRITE_REQUEST");
        assertThat(fixture.tag("setpoint").failureCount()).isEqualTo(1);
        assertThat(fixture.tag("setpoint").lastFailureReason()).isEqualTo("device rejected the value");
        // A normal write round-trip — even a failed one — never flaps the tag to ERROR (design §7.7).
        assertThat(fixture.tagStatus("setpoint")).isEqualTo(TagStatus.SOUTHBOUND_ONLY);
    }

    @Test
    void secondWriteWhileOneIsInFlight_isDropped() {
        final WrapperTestFixture fixture = writeOnlyFixture();
        fixture.activate(ProtocolAdapterDirection.SOUTHBOUND);

        fixture.submitWrite("setpoint", WrapperTestSupport.dataPoint("setpoint", "1"));
        assertThat(fixture.writeState("setpoint")).isEqualTo("WAITING_FOR_WRITE_RESULT");

        // One write in flight at a time (design §7.5): a second write while waiting is dropped, not queued.
        fixture.submitWrite("setpoint", WrapperTestSupport.dataPoint("setpoint", "2"));
        assertThat(fixture.writeState("setpoint")).isEqualTo("WAITING_FOR_WRITE_RESULT");
    }

    @Test
    void unusedWriteTag_staysDeactivatedEvenWhenActivated() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder()
                .runningCoordinator()
                .nodes(List.of(WrapperTestSupport.pair("setpoint")))
                .readUsed(Set.of())
                .writeUsed(Set.of()) // no southbound mapping consumes the tag — the third condition fails
                .build();

        fixture.activate(ProtocolAdapterDirection.SOUTHBOUND);

        assertThat(fixture.state()).isEqualTo(CONNECTED);
        assertThat(fixture.writeState("setpoint")).isEqualTo("DEACTIVATED");
        assertThat(fixture.tagStatus("setpoint")).isEqualTo(TagStatus.DEACTIVATED);
    }

    @Test
    void permanentVerificationFailure_suspendsTheWriteAspectButLeavesTheAdapterConnected() {
        final WrapperTestFixture fixture = writeOnlyFixture();
        fixture.adapter.verifyOutcome = new VerifyOutcome.PermanentFailure("unknown address");

        fixture.activate(ProtocolAdapterDirection.SOUTHBOUND);

        assertThat(fixture.state()).isEqualTo(CONNECTED); // failures do not block CONNECTED (design §6.3)
        assertThat(fixture.writeState("setpoint")).isEqualTo("ERROR_PERMANENT_VERIFICATION_FAILURE");
        assertThat(fixture.tagStatus("setpoint")).isEqualTo(TagStatus.ERROR);
    }
}
