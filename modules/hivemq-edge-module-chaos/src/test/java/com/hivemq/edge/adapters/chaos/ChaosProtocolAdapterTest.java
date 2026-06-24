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

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.discovery.NodeType;
import com.hivemq.adapter.sdk.api.v2.model.BrowseFilter;
import com.hivemq.adapter.sdk.api.v2.model.BrowseResultEntry;
import com.hivemq.adapter.sdk.api.v2.model.ErrorScope;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterOutput;
import com.hivemq.adapter.sdk.api.v2.model.VerifyOutcome;
import com.hivemq.adapter.sdk.api.v2.model.WriteEntry;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * The scriptable simulator in isolation (design §10; plan T10 {@code ChaosProtocolAdapterTest}). Each scripted
 * behavior produces the right callback at the right tick: immediate behaviors report within the command call, and
 * deferred ones ({@code acknowledgmentLatencyTicks}, {@link ChaosBehavior.Delay}, a subscription loss, a browse
 * duration, injected events) fire only when {@link ChaosProtocolAdapter#onTick()} reaches their tick — the
 * simulator holds no timers of its own.
 */
class ChaosProtocolAdapterTest {

    private static final @NotNull ChaosNode NODE_A = new ChaosNode("a");
    private static final @NotNull ChaosNode NODE_B = new ChaosNode("b");
    private static final @NotNull DataPoint VALUE = new ChaosDataPoint("a", "21");

    @Test
    void start_succeedByDefault_reportsStartedWithinTheCall() {
        final RecordingOutput output = new RecordingOutput();
        final ChaosProtocolAdapter adapter =
                new ChaosProtocolAdapter("a", output, ChaosScript.builder().build());

        adapter.start();

        assertThat(output.events).containsExactly("started");
    }

    @Test
    void connectFailConnection_reportsAConnectionError() {
        final RecordingOutput output = new RecordingOutput();
        final ChaosProtocolAdapter adapter = new ChaosProtocolAdapter(
                "a",
                output,
                ChaosScript.builder()
                        .onConnect(ChaosBehavior.failConnection("refused"))
                        .build());

        adapter.connect();

        assertThat(output.events).containsExactly("error:CONNECTION:refused");
    }

    @Test
    void connectDropped_reportsNothingEverAfter() {
        final RecordingOutput output = new RecordingOutput();
        final ChaosProtocolAdapter adapter = new ChaosProtocolAdapter(
                "a",
                output,
                ChaosScript.builder().onConnect(ChaosBehavior.drop()).build());

        adapter.connect();
        adapter.onTick();
        adapter.onTick();

        assertThat(output.events).isEmpty();
        assertThat(adapter.commands()).containsExactly("connect");
    }

    @Test
    void acknowledgmentLatency_defersTheAckUntilThatTick() {
        final RecordingOutput output = new RecordingOutput();
        final ChaosProtocolAdapter adapter = new ChaosProtocolAdapter(
                "a", output, ChaosScript.builder().acknowledgmentLatencyTicks(3).build());

        adapter.connect();
        assertThat(output.events).isEmpty();

        adapter.onTick(); // tick 1
        adapter.onTick(); // tick 2
        assertThat(output.events).isEmpty();

        adapter.onTick(); // tick 3 — the ack is now due
        assertThat(output.events).containsExactly("connected");
    }

    @Test
    void delayBehavior_appliesTheInnerBehaviorAfterTheGivenTicks() {
        final RecordingOutput output = new RecordingOutput();
        final ChaosProtocolAdapter adapter = new ChaosProtocolAdapter(
                "a",
                output,
                ChaosScript.builder()
                        .onConnect(ChaosBehavior.delay(2, ChaosBehavior.succeed()))
                        .build());

        adapter.connect();
        adapter.onTick(); // tick 1
        assertThat(output.events).isEmpty();

        adapter.onTick(); // tick 2 — the inner behavior applies
        assertThat(output.events).containsExactly("connected");
    }

    @Test
    void verify_reportsTheScriptedOutcomePerNode() {
        final RecordingOutput output = new RecordingOutput();
        final ChaosProtocolAdapter adapter = new ChaosProtocolAdapter(
                "a",
                output,
                ChaosScript.builder()
                        .verify(NodeMatcher.byId("a"), new VerifyOutcome.Success())
                        .verify(NodeMatcher.byId("b"), new VerifyOutcome.PermanentFailure("unknown"))
                        .build());

        adapter.verifyBatch(List.of(NODE_A, NODE_B));

        assertThat(output.events).containsExactly("verifyResult:a:Success", "verifyResult:b:PermanentFailure");
    }

    @Test
    void verifyNoResponse_staysSilent() {
        final RecordingOutput output = new RecordingOutput();
        final ChaosProtocolAdapter adapter = new ChaosProtocolAdapter(
                "a",
                output,
                ChaosScript.builder().verifyNoResponse(NodeMatcher.all()).build());

        adapter.verifyBatch(List.of(NODE_A));

        assertThat(output.events).isEmpty();
        assertThat(adapter.commands()).containsExactly("verifyBatch");
    }

    @Test
    void poll_deliversAValueOrAErrorPerScript() {
        final RecordingOutput value = new RecordingOutput();
        new ChaosProtocolAdapter(
                        "a",
                        value,
                        ChaosScript.builder()
                                .poll(NodeMatcher.all(), PollBehavior.value(VALUE))
                                .build())
                .pollBatch(List.of(NODE_A));
        assertThat(value.events).containsExactly("dataPoint:a");

        final RecordingOutput error = new RecordingOutput();
        new ChaosProtocolAdapter(
                        "a",
                        error,
                        ChaosScript.builder()
                                .poll(NodeMatcher.all(), PollBehavior.nodeError("read timeout"))
                                .build())
                .pollBatch(List.of(NODE_A));
        assertThat(error.events).containsExactly("nodeError:a:false");
    }

    @Test
    void subscribeAccept_pushesTheFirstValue() {
        final RecordingOutput output = new RecordingOutput();
        final ChaosProtocolAdapter adapter = new ChaosProtocolAdapter(
                "a",
                output,
                ChaosScript.builder()
                        .subscribe(NodeMatcher.all(), SubscriptionBehavior.accept(VALUE))
                        .build());

        adapter.addSubscriptionBatch(List.of(NODE_A));

        assertThat(output.events).containsExactly("dataPoint:a");
    }

    @Test
    void subscribeLoseAfter_pushesThenLosesTheSubscriptionAtTheGivenTick() {
        final RecordingOutput output = new RecordingOutput();
        final ChaosProtocolAdapter adapter = new ChaosProtocolAdapter(
                "a",
                output,
                ChaosScript.builder()
                        .subscribe(NodeMatcher.all(), SubscriptionBehavior.loseAfter(VALUE, 2, true, "device reset"))
                        .build());

        adapter.addSubscriptionBatch(List.of(NODE_A));
        assertThat(output.events).containsExactly("dataPoint:a");

        adapter.onTick(); // tick 1
        assertThat(output.events).containsExactly("dataPoint:a");

        adapter.onTick(); // tick 2 — the loss is now due, reported as a spontaneous node error
        assertThat(output.events).containsExactly("dataPoint:a", "nodeError:a:true");
    }

    @Test
    void write_reportsTheScriptedOutcome() {
        final RecordingOutput output = new RecordingOutput();
        final ChaosProtocolAdapter adapter = new ChaosProtocolAdapter(
                "a",
                output,
                ChaosScript.builder()
                        .write(NodeMatcher.all(), false, "device rejected the value")
                        .build());

        adapter.writeBatch(List.of(new WriteEntry(NODE_A, VALUE)));

        assertThat(output.events).containsExactly("writeResult:a:false");
    }

    @Test
    void browseImmediate_reportsTheResultWithinTheCall() {
        final RecordingOutput output = new RecordingOutput();
        final List<BrowseResultEntry> entries = List.of(new BrowseResultEntry(NODE_A, NodeType.VALUE, true));
        final ChaosProtocolAdapter adapter = new ChaosProtocolAdapter(
                "a",
                output,
                ChaosScript.builder().browse(NodeMatcher.all(), entries, 0).build());

        adapter.browse(new BrowseFilter(NODE_A));

        assertThat(output.events).containsExactly("browseResult:1");
    }

    @Test
    void browseWithDuration_reportsTheResultAfterTheGivenTicks() {
        final RecordingOutput output = new RecordingOutput();
        final List<BrowseResultEntry> entries = List.of(new BrowseResultEntry(NODE_A, NodeType.VALUE, true));
        final ChaosProtocolAdapter adapter = new ChaosProtocolAdapter(
                "a",
                output,
                ChaosScript.builder().browse(NodeMatcher.all(), entries, 3).build());

        adapter.browse(new BrowseFilter(NODE_A));
        adapter.onTick(); // tick 1
        adapter.onTick(); // tick 2
        assertThat(output.events).isEmpty();

        adapter.onTick(); // tick 3 — the browse completes
        assertThat(output.events).containsExactly("browseResult:1");
    }

    @Test
    void injectedEvent_firesAtItsChosenTick() {
        final RecordingOutput output = new RecordingOutput();
        final ChaosProtocolAdapter adapter = new ChaosProtocolAdapter(
                "a",
                output,
                ChaosScript.builder().injectAtTick(2, ChaosEvent.disconnect()).build());

        adapter.onTick(); // tick 1
        assertThat(output.events).isEmpty();

        adapter.onTick(); // tick 2 — the injected event fires
        assertThat(output.events).containsExactly("disconnected");
    }

    @Test
    void commandsAreRecordedInOrder() {
        final ChaosProtocolAdapter adapter = new ChaosProtocolAdapter(
                "a", new RecordingOutput(), ChaosScript.builder().build());

        adapter.start();
        adapter.connect();
        adapter.pollBatch(List.of(NODE_A));

        assertThat(adapter.commands()).containsExactly("start", "connect", "pollBatch");
    }

    /**
     * Records each callback as a descriptive string, so a test can assert which callback fired (and with what
     * salient detail) and at which tick.
     */
    private static final class RecordingOutput implements ProtocolAdapterOutput {

        private final @NotNull List<String> events = new ArrayList<>();

        @Override
        public void started() {
            events.add("started");
        }

        @Override
        public void stopped() {
            events.add("stopped");
        }

        @Override
        public void connected() {
            events.add("connected");
        }

        @Override
        public void disconnected() {
            events.add("disconnected");
        }

        @Override
        public void error(final @NotNull ErrorScope scope, final @NotNull String reason) {
            events.add("error:" + scope + ":" + reason);
        }

        @Override
        public void verifyResult(final @NotNull Node node, final @NotNull VerifyOutcome outcome) {
            events.add(
                    "verifyResult:" + node.nodeId() + ":" + outcome.getClass().getSimpleName());
        }

        @Override
        public void dataPoint(final @NotNull Node node, final @NotNull DataPoint value) {
            events.add("dataPoint:" + node.nodeId());
        }

        @Override
        public void nodeError(final @NotNull Node node, final @NotNull String reason, final boolean spontaneous) {
            events.add("nodeError:" + node.nodeId() + ":" + spontaneous);
        }

        @Override
        public void writeResult(final @NotNull Node node, final boolean success, final @Nullable String reason) {
            events.add("writeResult:" + node.nodeId() + ":" + success);
        }

        @Override
        public void browseResult(final @NotNull List<BrowseResultEntry> entries) {
            events.add("browseResult:" + entries.size());
        }
    }
}
