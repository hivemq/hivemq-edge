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

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.v2.model.BrowseContinuation;
import com.hivemq.adapter.sdk.api.v2.model.BrowseNode;
import com.hivemq.adapter.sdk.api.v2.model.ErrorScope;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterOutput;
import com.hivemq.adapter.sdk.api.v2.model.ResolvedAttributes;
import com.hivemq.adapter.sdk.api.v2.model.VerifyOutcome;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A recording fake of the engine's callback interface: every callback the adapter emits is captured as one readable
 * event string (plus the raw arguments where a test needs them), so a test can assert EXACTLY what the adapter said —
 * order, count, and payload — with no engine involved. This is the oracle for the verb→callback validation: the
 * scenario promises a behavior, the adapter is the interpreter, and this fake records what was actually commanded.
 */
final class RecordingOutput implements ProtocolAdapterOutput {

    /** One entry per callback, in emission order, e.g. {@code "connected"}, {@code "dataPoint t=42.0"}. */
    final @NotNull List<String> events = new CopyOnWriteArrayList<>();

    final @NotNull List<VerifyOutcome> verifyOutcomes = new CopyOnWriteArrayList<>();
    final @NotNull List<Object> dataPointValues = new CopyOnWriteArrayList<>();
    final @NotNull List<Node> dataPointNodes = new CopyOnWriteArrayList<>();

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
        events.add("error scope=" + scope);
    }

    @Override
    public void verifyResult(final @NotNull Node node, final @NotNull VerifyOutcome outcome) {
        events.add("verifyResult node=" + node.nodeId() + " outcome="
                + outcome.getClass().getSimpleName());
        verifyOutcomes.add(outcome);
    }

    @Override
    public void dataPoint(final @NotNull Node node, final @NotNull DataPoint value) {
        events.add("dataPoint node=" + node.nodeId());
        dataPointNodes.add(node);
        dataPointValues.add(value.getTagValue());
    }

    @Override
    public void dataPoints(final @NotNull Node node, final @NotNull List<DataPoint> values) {
        // the multi-value poll boundary (EDG-812): a batch of values for one node in one poll
        events.add("dataPoints node=" + node.nodeId() + " count=" + values.size());
        for (final DataPoint value : values) {
            dataPointNodes.add(node);
            dataPointValues.add(value.getTagValue());
        }
    }

    @Override
    public void pollComplete(final @NotNull Node node) {
        // the poll terminator for empty/multi-value polls (EDG-812); single-value polls complete via dataPoint()
        events.add("pollComplete node=" + node.nodeId());
    }

    @Override
    public void nodeError(final @NotNull Node node, final @NotNull String reason, final boolean spontaneous) {
        events.add("nodeError node=" + node.nodeId() + " spontaneous=" + spontaneous);
    }

    @Override
    public void writeResult(final @NotNull Node node, final boolean success, final @Nullable String reason) {
        events.add("writeResult node=" + node.nodeId() + " success=" + success);
    }

    @Override
    public void browsePage(
            final int requestId,
            final @NotNull List<BrowseNode> entries,
            final @Nullable BrowseContinuation continuation) {
        events.add("browsePage requestId=" + requestId + " entries=" + entries.size());
    }

    @Override
    public void readAttributesResult(final int requestId, final @NotNull List<ResolvedAttributes> attributes) {
        events.add("readAttributesResult requestId=" + requestId + " attributes=" + attributes.size());
    }

    @Override
    public void browseError(final int requestId, final @NotNull String reason) {
        events.add("browseError requestId=" + requestId);
    }

    /** All events matching a prefix, e.g. {@code of("dataPoint")}. */
    @NotNull
    List<String> of(final @NotNull String prefix) {
        return events.stream().filter(e -> e.startsWith(prefix)).collect(Collectors.toList());
    }

    long count(final @NotNull String prefix) {
        return events.stream().filter(e -> e.startsWith(prefix)).count();
    }

    void clear() {
        events.clear();
        verifyOutcomes.clear();
        dataPointValues.clear();
        dataPointNodes.clear();
    }
}
