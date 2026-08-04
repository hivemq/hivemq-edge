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
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.hivemq.adapter.sdk.api.v2.messaging.MessageDispatcherHandle;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageHandler;
import com.hivemq.protocols.v2.runtime.SystemDispatcher;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * The green-while-dead guard (EDG-824 #7): a contract-violating adapter that throws from a
 * synchronous adapter-facing call no longer kills the dispatch loop with a frozen GREEN snapshot — the wrapper
 * counts a defensive reset, enters {@code ERROR} with the throw as the reason, and keeps processing messages.
 * <p>
 * The guard has one exception, and it is the boundary's other half: a <b>fatal JVM condition</b> is not an adapter
 * fault and must propagate rather than be demoted to "this adapter is in ERROR" (Sam, round 2 — the wide catch has
 * to obey {@code AdapterFaults}' own stated policy).
 * <p>
 * Propagating it ends the dispatch loop, though, and the last snapshot that loop published would otherwise stay on
 * the REST surface reading GREEN for the life of the process — the same green-while-dead condition by another route
 * (Sam, round 3). So the dying thread corrects its own status on the way out: the machine is never transitioned, but
 * what an operator reads is an adapter that is gone. Edge itself carries on; one adapter's implementation failing is
 * not a reason to take a broker down.
 */
class GreenWhileDeadGuardTest {

    private static WrapperTestFixture connectedFixture() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder()
                .runningCoordinator()
                .nodes(List.of(WrapperTestSupport.pair("temperature")))
                .pollIntervalMillis(1000)
                .build();
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        return fixture;
    }

    @Test
    void adapterThrowingFromAPollDispatch_entersErrorInsteadOfFrozenGreen() {
        final WrapperTestFixture fixture = connectedFixture();
        assertThat(fixture.state()).isEqualTo(CONNECTED);
        fixture.adapter.pollThrow = true;

        fixture.advance(1000); // the cadence polls; the batch dispatch throws on the dispatch thread

        assertThat(fixture.state()).isEqualTo(ERROR);
        assertThat(fixture.snapshot().lastErrorReason()).contains("adapter threw IllegalStateException");
        assertThat(fixture.defensiveResets()).isEqualTo(1);
    }

    @Test
    void afterTheThrow_theWrapperStillProcessesMessages() {
        final WrapperTestFixture fixture = connectedFixture();
        fixture.adapter.pollThrow = true;
        fixture.advance(1000);
        assertThat(fixture.state()).isEqualTo(ERROR);

        // The actor is alive: a follow-up command is processed, not queued into a dead mailbox. ERROR is manual
        // recovery territory — the state stays ERROR, but the snapshot keeps being republished.
        fixture.send(new ProtocolAdapterWrapperCommand.DeactivateDirection(ProtocolAdapterDirection.NORTHBOUND));
        assertThat(fixture.pending()).isZero();
        assertThat(fixture.snapshot()).isNotNull();
    }

    @Test
    void adapterRaisingAFatalJvmCondition_propagates_andLeavesATruthfulStatusBehind() {
        final WrapperTestFixture fixture = connectedFixture();
        assertThat(fixture.state()).isEqualTo(CONNECTED);
        fixture.adapter.pollFatalThrow = true;

        // The wide catch must NOT contain this: an OutOfMemoryError reported as "adapter X is in ERROR" hides a
        // process-level failure and keeps the loop dispatching on a JVM that cannot honour the work.
        assertThatThrownBy(() -> fixture.advance(1000))
                .isInstanceOf(InternalError.class)
                .hasMessageContaining("simulated fatal JVM condition");

        // No demotion happened on the way out — the machine never transitioned and no defensive reset was counted —
        // but the dying thread's last act was to stop advertising a healthy adapter nobody is running any more. The
        // published status was CONNECTED until this point and would have stayed that way for the life of the process
        // (Sam round 3, finding 2).
        assertThat(fixture.defensiveResets()).isZero();
        assertThat(fixture.state()).isEqualTo(ERROR);
        assertThat(fixture.snapshot().lastErrorReason()).contains("dispatch loop was ended by InternalError");
        assertThat(fixture.health.died).hasSize(1);
    }

    @Test
    void onARealDispatchThread_theDeadActorIsVisiblyDead() {
        // The system boundary, not just the thread: a real dispatch loop dies of a fatal error and the assertion is
        // what an operator reading the REST surface afterwards actually sees.
        final WrapperTestFixture fixture = connectedFixture();
        fixture.adapter.pollFatalThrow = true;

        // The dispatch thread is expected to die here, so it captures its own death rather than littering the build
        // log's stderr — the same arrangement SystemDispatcherTest uses.
        final AtomicReference<Throwable> uncaught = new AtomicReference<>();
        final MessageHandler<ProtocolAdapterWrapperMessage> capturingWrapper = message -> {
            Thread.currentThread().setUncaughtExceptionHandler((thread, error) -> uncaught.set(error));
            fixture.wrapper.receive(message);
        };
        final SystemDispatcher dispatcher = new SystemDispatcher();
        try (MessageDispatcherHandle handle = dispatcher.attach(fixture.mailbox, capturingWrapper)) {
            // One tick, far enough ahead to fire the poll cadence: the batch dispatch raises the fatal on the
            // dispatch thread, exactly as a real adapter would.
            fixture.tell(new ProtocolAdapterWrapperTick(fixture.clock.nowMillis() + 10_000));

            await().atMost(Duration.ofSeconds(5)).until(() -> uncaught.get() != null);
            assertThat(uncaught.get()).isInstanceOf(InternalError.class);

            // The loop is gone, and the status it left behind is the honest one.
            assertThat(fixture.snapshot().machineState()).isEqualTo(ERROR);
            assertThat(fixture.snapshot().lastErrorReason()).contains("dispatch loop was ended by InternalError");
        }
    }
}
