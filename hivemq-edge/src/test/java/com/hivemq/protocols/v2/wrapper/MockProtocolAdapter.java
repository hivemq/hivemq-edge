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

import com.hivemq.adapter.sdk.api.v2.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.v2.model.BrowseContinuation;
import com.hivemq.adapter.sdk.api.v2.model.BrowseFilter;
import com.hivemq.adapter.sdk.api.v2.model.ErrorScope;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterOutput;
import com.hivemq.adapter.sdk.api.v2.model.VerifyOutcome;
import com.hivemq.adapter.sdk.api.v2.model.WriteEntry;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * A hand-rolled, scriptable {@link ProtocolAdapter} for the adapter-machine tests. It records every command and
 * answers each through the {@link ProtocolAdapterOutput} tell-façade, so its replies travel back through the
 * wrapper mailbox exactly as a real adapter's would.
 * <p>
 * Replies are scriptable per command. The default is to acknowledge every command successfully; a {@link Reply}
 * of {@code DROP} makes the adapter silent (so the wrapper parks in the waiting state until its watchdog fires),
 * and {@code FAIL_*} reports an error. {@code connect()} additionally drains {@link #connectReplies} so a test can
 * fail the first attempt and succeed the next.
 */
final class MockProtocolAdapter implements ProtocolAdapter {

    enum Reply {
        ACK,
        FAIL_ADAPTER,
        FAIL_CONNECTION,
        DROP
    }

    private final @NotNull String adapterId;
    private final @NotNull ProtocolAdapterOutput output;

    final @NotNull List<String> commands = new ArrayList<>();
    final @NotNull Deque<Reply> connectReplies = new ArrayDeque<>();

    @NotNull
    Reply startReply = Reply.ACK;

    @NotNull
    Reply stopReply = Reply.ACK;

    @NotNull
    Reply connectReply = Reply.ACK;

    @NotNull
    Reply disconnectReply = Reply.ACK;

    @NotNull
    VerifyOutcome verifyOutcome = new VerifyOutcome.Success();

    boolean verifyDrop;

    MockProtocolAdapter(final @NotNull String adapterId, final @NotNull ProtocolAdapterOutput output) {
        this.adapterId = adapterId;
        this.output = output;
    }

    @Override
    public @NotNull String adapterId() {
        return adapterId;
    }

    @Override
    public void start() {
        commands.add("start");
        switch (startReply) {
            case ACK -> output.started();
            case FAIL_ADAPTER -> output.error(ErrorScope.ADAPTER, "start failed");
            case FAIL_CONNECTION, DROP -> {}
        }
    }

    @Override
    public void stop() {
        commands.add("stop");
        switch (stopReply) {
            case ACK -> output.stopped();
            case FAIL_ADAPTER -> output.error(ErrorScope.ADAPTER, "stop failed");
            case FAIL_CONNECTION, DROP -> {}
        }
    }

    @Override
    public void connect() {
        commands.add("connect");
        final Reply reply = connectReplies.isEmpty() ? connectReply : connectReplies.poll();
        switch (reply) {
            case ACK -> output.connected();
            case FAIL_CONNECTION -> output.error(ErrorScope.CONNECTION, "connect failed");
            case FAIL_ADAPTER -> output.error(ErrorScope.ADAPTER, "adapter failed");
            case DROP -> {}
        }
    }

    @Override
    public void disconnect() {
        commands.add("disconnect");
        switch (disconnectReply) {
            case ACK -> output.disconnected();
            case FAIL_ADAPTER -> output.error(ErrorScope.ADAPTER, "disconnect failed");
            case FAIL_CONNECTION, DROP -> {}
        }
    }

    @Override
    public void verifyBatch(final @NotNull List<Node> nodes) {
        commands.add("verifyBatch");
        if (verifyDrop) {
            return;
        }
        for (final Node node : nodes) {
            output.verifyResult(node, verifyOutcome);
        }
    }

    @Override
    public void pollBatch(final @NotNull List<Node> nodes) {
        commands.add("pollBatch");
    }

    @Override
    public void addSubscriptionBatch(final @NotNull List<Node> nodes) {
        commands.add("addSubscriptionBatch");
    }

    @Override
    public void removeSubscriptionBatch(final @NotNull List<Node> nodes) {
        commands.add("removeSubscriptionBatch");
    }

    @Override
    public void writeBatch(final @NotNull List<WriteEntry> entries) {
        commands.add("writeBatch");
    }

    @Override
    public void browse(final int requestId, final @NotNull BrowseFilter filter, final int maxReferences) {
        commands.add("browse");
    }

    @Override
    public void browseNext(final int requestId, final @NotNull BrowseContinuation continuation) {
        commands.add("browseNext");
    }

    @Override
    public void readNodeAttributes(final int requestId, final @NotNull List<Node> nodes) {
        commands.add("readNodeAttributes");
    }
}
