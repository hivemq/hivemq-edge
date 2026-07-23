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
package com.hivemq.protocols.v2.manager;

import com.hivemq.adapter.sdk.api.v2.messaging.MailboxSender;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperEventListener;
import org.jetbrains.annotations.NotNull;

/**
 * The wrapper&rarr;manager health seam: turns each {@link ProtocolAdapterWrapperEventListener}
 * callback a managed wrapper makes into one immutable {@link ProtocolAdapterManagerMessage} told onto the manager's
 * own mailbox. A wrapper runs on its own dispatch thread and tells the manager from there; the manager then handles
 * the health event on its single thread (the actor model — no shared state crosses the boundary).
 * <p>
 * One instance is shared by every wrapper the manager owns: each callback already carries the adapter id, so the
 * listener only needs the manager's send-only handle.
 */
public final class ProtocolAdapterManagerHealthListener implements ProtocolAdapterWrapperEventListener {

    private final @NotNull MailboxSender<ProtocolAdapterManagerMessage> managerSender;

    /**
     * @param managerSender the send-only handle of the manager's own mailbox.
     */
    public ProtocolAdapterManagerHealthListener(
            final @NotNull MailboxSender<ProtocolAdapterManagerMessage> managerSender) {
        this.managerSender = managerSender;
    }

    @Override
    public void wrapperStarted(final @NotNull String adapterId) {
        managerSender.tell(new ProtocolAdapterManagerMessage.WrapperStarted(adapterId));
    }

    @Override
    public void wrapperStopped(final @NotNull String adapterId) {
        managerSender.tell(new ProtocolAdapterManagerMessage.WrapperStopped(adapterId));
    }

    @Override
    public void wrapperError(final @NotNull String adapterId, final @NotNull String reason) {
        managerSender.tell(new ProtocolAdapterManagerMessage.WrapperError(adapterId, reason));
    }
}
