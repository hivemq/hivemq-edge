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
package com.hivemq.protocols.v2.southbound;

import com.hivemq.adapter.sdk.api.v2.messaging.MailboxSender;
import com.hivemq.protocols.v2.tag.SouthboundWriteOutcome;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperMessage;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperWriteRequest;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** A send-only mailbox stand-in that records each write request and lets the test settle it as the adapter would. */
final class CapturingSender implements MailboxSender<ProtocolAdapterWrapperMessage> {

    final @NotNull List<ProtocolAdapterWrapperWriteRequest> requests = new ArrayList<>();

    @Override
    public void tell(final @NotNull ProtocolAdapterWrapperMessage message) {
        requests.add((ProtocolAdapterWrapperWriteRequest) message);
    }

    void settleLast(final @NotNull SouthboundWriteOutcome outcome) {
        settleLast(outcome, null);
    }

    /**
     * Settle the last delivered write as the adapter would, carrying the device's own words.
     *
     * @param outcome the outcome to settle with.
     * @param reason  the device's failure reason, or {@code null} when it supplied none.
     */
    void settleLast(final @NotNull SouthboundWriteOutcome outcome, final @Nullable String reason) {
        requests.getLast().completion().settle(outcome, reason);
    }
}
