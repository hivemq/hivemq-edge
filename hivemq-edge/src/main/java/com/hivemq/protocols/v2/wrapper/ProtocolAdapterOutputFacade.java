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

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxSender;
import com.hivemq.adapter.sdk.api.v2.model.BrowseResultEntry;
import com.hivemq.adapter.sdk.api.v2.model.ErrorScope;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterOutput;
import com.hivemq.adapter.sdk.api.v2.model.VerifyOutcome;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The framework's implementation of {@link ProtocolAdapterOutput} (design §3.7, §6.1): a tell-façade that turns
 * each protocol-adapter callback into one immutable {@link ProtocolAdapterWrapperEvent} told onto the wrapper's
 * mailbox. Because {@link MailboxSender#tell(com.hivemq.adapter.sdk.api.v2.messaging.MailboxMessage)} is
 * thread-safe and non-blocking, an adapter may call any callback from any thread (library callbacks, receive
 * threads, …) with no locking.
 * <p>
 * The façade holds only a send-only handle to the mailbox — it cannot read wrapper state or reach the handler.
 */
public final class ProtocolAdapterOutputFacade implements ProtocolAdapterOutput {

    private final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> wrapperMailbox;

    /**
     * @param wrapperMailbox the send-only handle of the wrapper's mailbox.
     */
    public ProtocolAdapterOutputFacade(final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> wrapperMailbox) {
        this.wrapperMailbox = wrapperMailbox;
    }

    @Override
    public void started() {
        wrapperMailbox.tell(new ProtocolAdapterWrapperEvent.Started());
    }

    @Override
    public void stopped() {
        wrapperMailbox.tell(new ProtocolAdapterWrapperEvent.Stopped());
    }

    @Override
    public void connected() {
        wrapperMailbox.tell(new ProtocolAdapterWrapperEvent.Connected());
    }

    @Override
    public void disconnected() {
        wrapperMailbox.tell(new ProtocolAdapterWrapperEvent.Disconnected());
    }

    @Override
    public void error(final @NotNull ErrorScope scope, final @NotNull String reason) {
        wrapperMailbox.tell(new ProtocolAdapterWrapperEvent.ErrorEvent(scope, reason));
    }

    @Override
    public void verifyResult(final @NotNull Node node, final @NotNull VerifyOutcome outcome) {
        wrapperMailbox.tell(new ProtocolAdapterWrapperEvent.VerifyResultReceived(node, outcome));
    }

    @Override
    public void dataPoint(final @NotNull Node node, final @NotNull DataPoint value) {
        wrapperMailbox.tell(new ProtocolAdapterWrapperEvent.DataPointReceived(node, value));
    }

    @Override
    public void nodeError(final @NotNull Node node, final @NotNull String reason, final boolean spontaneous) {
        wrapperMailbox.tell(new ProtocolAdapterWrapperEvent.NodeErrorReceived(node, reason, spontaneous));
    }

    @Override
    public void writeResult(final @NotNull Node node, final boolean success, final @Nullable String reason) {
        wrapperMailbox.tell(new ProtocolAdapterWrapperEvent.WriteResultReceived(node, success, reason));
    }

    @Override
    public void browseResult(final @NotNull List<BrowseResultEntry> entries) {
        wrapperMailbox.tell(new ProtocolAdapterWrapperEvent.BrowseResultReceived(List.copyOf(entries)));
    }
}
