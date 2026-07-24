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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hivemq.adapter.sdk.api.v2.model.WriteEntry;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The verb→callback validation of the interpreter: for every instruction the scenario language offers (connect / verify
 * / poll / write behaviors, plus the contract-violating {@code misbehave} modes and the one-per-cycle down-announce
 * budget), construct the adapter with that instruction, drive the engine-side command, and assert the EXACT callbacks
 * recorded — order, count, and payload. Engine-free and wall-clock-light: what the language promises is what the
 * adapter commands.
 */
class WorkloadProtocolAdapterTest {

    private WorkloadProtocolAdapter adapter;
    private final RecordingOutput out = new RecordingOutput();

    private WorkloadProtocolAdapter adapter(final String scenarioJson) {
        adapter = new WorkloadProtocolAdapter("unit-device", out, WorkloadScenario.parseOrEmpty(scenarioJson));
        adapter.start();
        out.clear(); // drop the started() ack so each test asserts only its own verb's callbacks
        return adapter;
    }

    private static Node node(final String id) {
        return new WorkloadNode(id);
    }

    @AfterEach
    void tearDown() {
        if (adapter != null) {
            adapter.stop();
            adapter.close();
        }
    }

    // ── connect verbs ───────────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void connectSucceed_emitsExactlyConnected() {
        adapter("{}").connect();
        assertThat(out.events).containsExactly("connected");
    }

    @Test
    void connectFail_emitsAConnectionError_andNeverConnected() {
        adapter("{\"connect\":\"fail\"}").connect();
        assertThat(out.events).containsExactly("error scope=CONNECTION");
    }

    @Test
    void connectNoResponse_emitsNothing_theWatchdogsProblem() {
        adapter("{\"connect\":\"no-response\"}").connect();
        assertThat(out.events).isEmpty();
    }

    @Test
    void connectDrop_emitsConnectedThenDisconnected() {
        adapter("{\"connect\":\"drop\"}").connect();
        assertThat(out.events).containsExactly("connected", "disconnected");
    }

    // ── verify verbs ────────────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void verifyOutcomesMatchTheScenario_successPermanentTransient() {
        adapter("{\"tags\":{\"ok\":{},\"perm\":{\"verify\":\"permanent\"},\"trans\":{\"verify\":\"transient\"}}}");
        adapter.verifyBatch(List.of(node("ok"), node("perm"), node("trans")));
        assertThat(out.events)
                .containsExactly(
                        "verifyResult node=ok outcome=Success",
                        "verifyResult node=perm outcome=PermanentFailure",
                        "verifyResult node=trans outcome=TransientFailure");
    }

    @Test
    void verifyTransientThenSuccess_failsExactlyTransientCountTimes_thenPasses() {
        adapter("{\"tags\":{\"flappy\":{\"verify\":\"transient-then-success\",\"transientCount\":2}}}");
        for (int i = 0; i < 3; i++) {
            adapter.verifyBatch(List.of(node("flappy")));
        }
        assertThat(out.events)
                .containsExactly(
                        "verifyResult node=flappy outcome=TransientFailure",
                        "verifyResult node=flappy outcome=TransientFailure",
                        "verifyResult node=flappy outcome=Success");
    }

    @Test
    void verifySuccessThenPermanent_passesOnce_thenFailsForever() {
        adapter("{\"tags\":{\"souring\":{\"verify\":\"success-then-permanent\"}}}");
        for (int i = 0; i < 3; i++) {
            adapter.verifyBatch(List.of(node("souring")));
        }
        assertThat(out.events)
                .containsExactly(
                        "verifyResult node=souring outcome=Success",
                        "verifyResult node=souring outcome=PermanentFailure",
                        "verifyResult node=souring outcome=PermanentFailure");
    }

    @Test
    void verifyNoResponse_emitsNothing_leavingTheNodeToTheWatchdog() {
        adapter("{\"tags\":{\"mute\":{\"verify\":\"no-response\"}}}");
        adapter.verifyBatch(List.of(node("mute")));
        assertThat(out.events).isEmpty();
    }

    // ── poll verbs ──────────────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void pollValue_emitsTheWaveValue() {
        adapter("{\"tags\":{\"t\":{\"wave\":\"constant\",\"value\":42}}}");
        adapter.pollBatch(List.of(node("t")));
        assertThat(out.of("dataPoint")).containsExactly("dataPoint node=t");
        assertThat((Double) out.dataPointValues.get(0)).isEqualTo(42.0);
    }

    @Test
    void pollError_emitsANonSpontaneousNodeError() {
        adapter("{\"tags\":{\"t\":{\"poll\":\"error\"}}}");
        adapter.pollBatch(List.of(node("t")));
        assertThat(out.events).containsExactly("nodeError node=t spontaneous=false");
    }

    @Test
    void pollNoResponse_emitsNothingForThatNode() {
        adapter("{\"tags\":{\"t\":{\"poll\":\"no-response\"},\"u\":{\"wave\":\"constant\",\"value\":1}}}");
        adapter.pollBatch(List.of(node("t"), node("u")));
        assertThat(out.of("dataPoint")).containsExactly("dataPoint node=u"); // only the healthy sibling answers
    }

    @Test
    void pollGarbage_emitsAStringWhereANumberIsDue() {
        adapter("{\"tags\":{\"t\":{\"poll\":\"garbage\"}}}");
        adapter.pollBatch(List.of(node("t")));
        assertThat(out.dataPointValues).containsExactly("GARBAGE_NOT_A_NUMBER");
    }

    @Test
    void pollLiterals_emitTheExactEdgeCaseConstants() {
        adapter("{\"tags\":{\"n\":{\"poll\":\"literal\",\"reason\":\"nan\"},"
                + "\"p\":{\"poll\":\"literal\",\"reason\":\"precise_long\"},"
                + "\"m\":{\"poll\":\"literal\",\"reason\":\"max_long\"}}}");
        adapter.pollBatch(List.of(node("n"), node("p"), node("m")));
        assertThat(out.dataPointValues).containsExactly(Double.NaN, 9007199254740993L, Long.MAX_VALUE);
    }

    @Test
    void pollDouble_emitsTwoTerminalResultsForOnePoll_theContractViolation() {
        adapter("{\"tags\":{\"t\":{\"poll\":\"double\",\"wave\":\"constant\",\"value\":7}}}");
        adapter.pollBatch(List.of(node("t")));
        assertThat(out.of("dataPoint")).containsExactly("dataPoint node=t", "dataPoint node=t");
        assertThat(out.dataPointValues).containsExactly(7.0, 7.0); // same captured value, emitted twice
    }

    @Test
    void counterWave_advancesByOnePerEmission() {
        adapter("{\"tags\":{\"t\":{\"wave\":\"counter\"}}}");
        adapter.pollBatch(List.of(node("t")));
        adapter.pollBatch(List.of(node("t")));
        adapter.pollBatch(List.of(node("t")));
        assertThat(out.dataPointValues).containsExactly(1L, 2L, 3L);
    }

    // ── write verbs ─────────────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void writeOutcomesMatchTheScenario_successFailNoResponse() {
        adapter("{\"tags\":{\"ok\":{},\"bad\":{\"write\":\"fail\"},\"mute\":{\"write\":\"no-response\"}}}");
        adapter.writeBatch(List.of(
                new WriteEntry(node("ok"), new WorkloadDataPoint("ok", 1)),
                new WriteEntry(node("bad"), new WorkloadDataPoint("bad", 2)),
                new WriteEntry(node("mute"), new WorkloadDataPoint("mute", 3))));
        assertThat(out.of("writeResult"))
                .containsExactly(
                        "writeResult node=ok success=true",
                        "writeResult node=bad success=false"); // mute answers nothing — the write watchdog's problem
    }

    // ── misbehave modes (deliberate contract violations) ────────────────────────────────────────────────────────────

    @Test
    void misbehaveDoubleStart_answersOneStartWithStartedAndError() {
        adapter = new WorkloadProtocolAdapter(
                "unit-device", out, WorkloadScenario.parseOrEmpty("{\"misbehave\":\"double-start\"}"));
        adapter.start();
        assertThat(out.events).containsExactly("started", "error scope=ADAPTER");
    }

    @Test
    void misbehaveStartNoAck_neverAcksTheStart() {
        adapter = new WorkloadProtocolAdapter(
                "unit-device", out, WorkloadScenario.parseOrEmpty("{\"misbehave\":\"start-no-ack\"}"));
        adapter.start();
        assertThat(out.count("started")).isZero();
    }

    @Test
    void misbehaveStopNoAck_neverAcksTheStop() {
        adapter("{\"misbehave\":\"stop-no-ack\"}");
        adapter.stop();
        assertThat(out.count("stopped")).isZero();
    }

    @Test
    void misbehaveDisconnectNoAck_neverAcksTheCommandedDisconnect() {
        adapter("{\"misbehave\":\"disconnect-no-ack\"}");
        adapter.connect();
        out.clear();
        adapter.disconnect();
        assertThat(out.count("disconnected")).isZero();
    }

    @Test
    void misbehaveVerifyPartial_reportsOnlyTheFirstNodeAndDropsTheRest() {
        adapter("{\"misbehave\":\"verify-partial\"}");
        adapter.verifyBatch(List.of(node("a"), node("b"), node("c")));
        assertThat(out.of("verifyResult")).containsExactly("verifyResult node=a outcome=Success");
    }

    @Test
    void misbehaveThrowInPoll_throwsInsideTheCommand() {
        adapter("{\"misbehave\":\"throw-in-poll\"}");
        assertThatThrownBy(() -> adapter.pollBatch(List.of(node("t")))).isInstanceOf(RuntimeException.class);
    }

    // ── the down-announce budget (one disconnected() per connect cycle; spontaneous only while connected) ───────────

    @Test
    void aDownTimeline_announcesDisconnectedExactlyOnce_acrossRepeatedPolls() throws Exception {
        // device connects first (timeline still up), goes down at 120ms, then the engine keeps polling
        adapter("{\"tags\":{\"t\":{\"wave\":\"constant\",\"value\":1}},"
                + "\"timeline\":[{\"atMs\":120,\"action\":\"disconnect\"}]}");
        adapter.connect();
        assertThat(out.events).containsExactly("connected");
        Thread.sleep(200); // past the down instant
        out.clear();
        adapter.pollBatch(List.of(node("t")));
        adapter.pollBatch(List.of(node("t")));
        adapter.pollBatch(List.of(node("t")));
        assertThat(out.count("disconnected")).isEqualTo(1); // one announce budget per connect cycle — never more
        assertThat(out.count("dataPoint")).isZero(); // a down device emits no data
    }

    @Test
    void afterAFailedConnect_spontaneousDisconnectedIsForbidden() throws Exception {
        // the device is down BEFORE the first connect: connect() reports a connection error and the wrapper handles
        // retries — a spontaneous disconnected() into the retry window would be a contract violation the engine
        // defensive-resets on (the instrument must never manufacture it)
        adapter("{\"tags\":{\"t\":{\"wave\":\"constant\",\"value\":1}},"
                + "\"timeline\":[{\"atMs\":0,\"action\":\"disconnect\"}]}");
        Thread.sleep(30); // the down instant has passed
        adapter.connect();
        assertThat(out.events).containsExactly("error scope=CONNECTION"); // failed connect, never connected
        out.clear();
        adapter.pollBatch(List.of(node("t")));
        adapter.pollBatch(List.of(node("t")));
        assertThat(out.count("disconnected")).isZero(); // no announce without a connected() this cycle
    }

    @Test
    void theBudgetReopensOnTheNextSuccessfulConnect() throws Exception {
        adapter("{\"tags\":{\"t\":{\"wave\":\"constant\",\"value\":1}},"
                + "\"timeline\":[{\"atMs\":120,\"action\":\"disconnect\"},{\"atMs\":240,\"action\":\"reconnect\"}]}");
        adapter.connect(); // cycle 1 — up
        Thread.sleep(160); // down instant passes
        adapter.pollBatch(List.of(node("t"))); // spends cycle 1's budget
        Thread.sleep(140); // recover instant passes
        adapter.connect(); // cycle 2 — up again
        Thread.sleep(30);
        out.clear();
        adapter.pollBatch(List.of(node("t"))); // healthy again: data, no announce
        assertThat(out.count("dataPoint")).isEqualTo(1);
        assertThat(out.count("disconnected")).isZero();
    }

    // ── verification: a spontaneous nodeError only for subscribed faulted tags via timeline fault ──────────────────

    @Test
    void aTimelineFault_turnsThePollIntoANonSpontaneousNodeError() throws Exception {
        adapter("{\"tags\":{\"t\":{\"wave\":\"constant\",\"value\":1}},"
                + "\"timeline\":[{\"atMs\":0,\"action\":\"fault\",\"tag\":\"t\"}]}");
        adapter.connect();
        Thread.sleep(30);
        out.clear();
        adapter.pollBatch(List.of(node("t")));
        assertThat(out.events).containsExactly("nodeError node=t spontaneous=false");
    }

    @Test
    void aMutedTag_goesSilently_noDataNoError() throws Exception {
        adapter("{\"tags\":{\"t\":{\"wave\":\"constant\",\"value\":1}},"
                + "\"timeline\":[{\"atMs\":0,\"action\":\"mute\",\"tag\":\"t\"}]}");
        adapter.connect();
        Thread.sleep(30);
        out.clear();
        adapter.pollBatch(List.of(node("t")));
        assertThat(out.events).isEmpty(); // the silent-freeze primitive: no data, no error, connection stays up
    }
}
