/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.http.v2;

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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link ProtocolAdapterOutput} test double that records every tell the adapter makes. Thread-safe because the HTTP
 * adapter reports poll results from the HTTP client's own completion threads, not the dispatch thread — so a test
 * polls the recorder with Awaitility to observe the asynchronous result.
 */
public final class RecordingProtocolAdapterOutput implements ProtocolAdapterOutput {

    /**
     * The ordered names of the lifecycle/event tells, for sequence assertions.
     */
    public final @NotNull List<String> events = new CopyOnWriteArrayList<>();

    /**
     * The reported data points, in arrival order.
     */
    public final @NotNull List<DataPointRecord> dataPoints = new CopyOnWriteArrayList<>();

    /**
     * The reported per-node errors, in arrival order.
     */
    public final @NotNull List<NodeErrorRecord> nodeErrors = new CopyOnWriteArrayList<>();

    /**
     * The reported connection/adapter errors, in order.
     */
    public final @NotNull List<String> errors = new CopyOnWriteArrayList<>();

    /**
     * The reported write results, in order.
     */
    public final @NotNull List<WriteResultRecord> writeResults = new CopyOnWriteArrayList<>();

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
        events.add("error");
        errors.add(scope + ":" + reason);
    }

    @Override
    public void verifyResult(final @NotNull Node node, final @NotNull VerifyOutcome outcome) {
        events.add("verifyResult");
    }

    @Override
    public void dataPoint(final @NotNull Node node, final @NotNull DataPoint value) {
        events.add("dataPoint");
        dataPoints.add(new DataPointRecord(node, value));
    }

    @Override
    public void nodeError(final @NotNull Node node, final @NotNull String reason, final boolean spontaneous) {
        events.add("nodeError");
        nodeErrors.add(new NodeErrorRecord(node, reason, spontaneous));
    }

    @Override
    public void writeResult(final @NotNull Node node, final boolean success, final @Nullable String reason) {
        events.add("writeResult");
        writeResults.add(new WriteResultRecord(node, success, reason));
    }

    @Override
    public void browsePage(
            final int requestId,
            final @NotNull List<BrowseNode> entries,
            final @Nullable BrowseContinuation continuation) {
        events.add("browsePage");
    }

    @Override
    public void readAttributesResult(final int requestId, final @NotNull List<ResolvedAttributes> attributes) {
        events.add("readAttributesResult");
    }

    @Override
    public void browseError(final int requestId, final @NotNull String reason) {
        events.add("browseError");
    }

    /**
     * @param node  the node the value was reported for.
     * @param value the reported value.
     */
    public record DataPointRecord(
            @NotNull Node node, @NotNull DataPoint value) {}

    /**
     * @param node        the node the error was reported for.
     * @param reason      the reported reason.
     * @param spontaneous whether the error was reported spontaneously.
     */
    public record NodeErrorRecord(
            @NotNull Node node, @NotNull String reason, boolean spontaneous) {}

    /**
     * @param node    the node the write targeted.
     * @param success whether the write succeeded.
     * @param reason  the reported reason, or {@code null} on success.
     */
    public record WriteResultRecord(
            @NotNull Node node, boolean success, @Nullable String reason) {}
}
