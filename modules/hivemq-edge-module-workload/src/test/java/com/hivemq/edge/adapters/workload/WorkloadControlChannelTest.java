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
package com.hivemq.edge.adapters.workload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.hivemq.adapter.sdk.api.v2.node.Node;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The debugger's own semantics, engine-free: gates (hold / release / releaseone) capture and replay emissions in
 * order; injected pokes resolve the ORIGINAL node instance through the registry (the identity-keyed engine contract);
 * unknown commands are rejected loudly ({@code CTL_REJECTED}); a new session skips history instead of replaying it
 * ({@code CTL_SKIPPED}); and {@code block} arms a one-shot latency. Each behavior is what the user guide promises —
 * here it is proven against the real file protocol with a real watcher thread.
 */
class WorkloadControlChannelTest {

    @TempDir
    Path dir;

    private final RecordingOutput out = new RecordingOutput();
    private final Map<String, Node> knownNodes = new ConcurrentHashMap<>();
    private WorkloadControlChannel channel;
    private Path ctl;
    private Path journal;

    @BeforeEach
    void customBeforeEach() {
        ctl = dir.resolve("unit-device.ctl");
        journal = dir.resolve("unit-device.journal");
        channel = new WorkloadControlChannel("unit-device", dir.toString(), out, knownNodes::get, 0L);
        channel.start();
    }

    @AfterEach
    void customAfterEach() {
        channel.stop();
    }

    private void ctl(final String command) throws Exception {
        Files.writeString(
                ctl, command + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private String journal() throws Exception {
        return Files.exists(journal) ? Files.readString(journal) : "";
    }

    // ── gates: hold / release / releaseone ──────────────────────────────────────────────────────────────────────────

    @Test
    void holdCapturesEmissions_releaseReplaysThemInOrder() throws Exception {
        ctl("hold poll");
        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(journal()).contains("CTL hold poll"));

        final List<String> ran = new CopyOnWriteArrayList<>();
        channel.emit("poll", "t", () -> ran.add("first"));
        channel.emit("poll", "t", () -> ran.add("second"));
        assertThat(ran).isEmpty(); // both frozen, not delivered
        assertThat(journal()).contains("HELD key=poll:t depth=1").contains("HELD key=poll:t depth=2");

        ctl("release poll");
        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(ran).containsExactly("first", "second")); // FIFO replay
        // the gate is cleared: a new emission runs immediately
        channel.emit("poll", "t", () -> ran.add("third"));
        assertThat(ran).containsExactly("first", "second", "third");
    }

    @Test
    void aNodeScopedHold_freezesOnlyThatNode() throws Exception {
        ctl("hold poll frozen");
        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(journal()).contains("CTL hold poll frozen"));

        final List<String> ran = new CopyOnWriteArrayList<>();
        channel.emit("poll", "frozen", () -> ran.add("frozen"));
        channel.emit("poll", "free", () -> ran.add("free"));
        assertThat(ran).containsExactly("free"); // only the un-held node's emission ran
    }

    @Test
    void releaseoneDeliversExactlyTheOldestHeldEmission_evenNodeScoped() throws Exception {
        ctl("hold poll");
        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(journal()).contains("CTL hold poll"));
        final List<String> ran = new CopyOnWriteArrayList<>();
        channel.emit("poll", "t", () -> ran.add("one"));
        channel.emit("poll", "t", () -> ran.add("two"));

        ctl("releaseone poll"); // no node given: must find the op:node queue, not silently no-op
        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(ran).containsExactly("one"));
        assertThat(ran).hasSize(1); // exactly one released, the second stays frozen
    }

    // ── pokes resolve the engine's original node instance ───────────────────────────────────────────────────────────

    @Test
    void anInjectedDatapoint_carriesTheRegisteredOriginalNodeInstance() throws Exception {
        final Node original = new WorkloadNode("t");
        knownNodes.put("t", original);

        ctl("emit datapoint t 777");
        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(out.of("dataPoint")).containsExactly("dataPoint node=t"));
        // the SAME instance the engine tracks — identity, not just an equal id (the engine's map is identity-keyed)
        assertThat(out.dataPointNodes.get(0)).isSameAs(original);
        assertThat(journal()).contains("resolved=true");
    }

    @Test
    void anUnknownNodeId_fallsBackToAFreshNode_andSaysSo() throws Exception {
        ctl("emit datapoint ghost 1");
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> assertThat(out.of("dataPoint"))
                .hasSize(1)); // still emitted — the deliberate ghost-node probe
        assertThat(journal()).contains("resolved=false"); // but the miss is visible, never silent
    }

    // ── loud rejection of bad commands ──────────────────────────────────────────────────────────────────────────────

    @Test
    void anUnknownOp_isRejectedAndJournaled_notSilentlyArmed() throws Exception {
        ctl("hold pol"); // the classic typo
        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(journal()).contains("CTL_REJECTED"));
        final List<String> ran = new CopyOnWriteArrayList<>();
        channel.emit("poll", "t", () -> ran.add("ran"));
        assertThat(ran).containsExactly("ran"); // nothing was armed by the typo
    }

    // ── session rules: history is skipped, never replayed ───────────────────────────────────────────────────────────

    @Test
    void aNewSessionSkipsTheCommandHistory_insteadOfReplayingIt() throws Exception {
        ctl("emit connected"); // history from THIS session's perspective — executed once
        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(out.count("connected")).isEqualTo(1));
        channel.stop();

        // a NEW session over the same .ctl file must NOT re-execute the old command
        final RecordingOutput out2 = new RecordingOutput();
        final WorkloadControlChannel second =
                new WorkloadControlChannel("unit-device", dir.toString(), out2, knownNodes::get, 0L);
        second.start();
        try {
            ctl("emit disconnected"); // only commands appended AFTER the new session started may run
            await().atMost(Duration.ofSeconds(3))
                    .untilAsserted(() -> assertThat(out2.count("disconnected")).isEqualTo(1));
            assertThat(out2.count("connected")).isZero(); // history not replayed
            assertThat(journal()).contains("CTL_SKIPPED"); // and the skip is on the record
        } finally {
            second.stop();
        }
    }

    // ── block: one-shot latency injection ───────────────────────────────────────────────────────────────────────────

    @Test
    void blockArmsAOneShotLatency_consumedByTheNextCommand() throws Exception {
        ctl("block poll 250");
        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(journal()).contains("CTL block poll 250"));
        assertThat(channel.consumeBlock("poll")).isEqualTo(250); // armed for the next poll…
        assertThat(channel.consumeBlock("poll")).isZero(); // …and one-shot: consumed
    }
}
