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

import com.hivemq.adapter.sdk.api.v2.messaging.MailboxMessagePriority;
import com.hivemq.adapter.sdk.api.v2.model.BrowseFilter;
import com.hivemq.protocols.v2.browse.BrowsedNode;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

/**
 * A browse request the manager forwards to the wrapper on behalf of a REST thread — the wrapper
 * half of the browse bridge. It carries the {@link BrowseFilter} and the completion future the REST thread blocks
 * on. On the dispatch thread the wrapper issues a single {@code browse(filter)} to the protocol adapter when it is
 * {@code CONNECTED} with no browse already in flight, and completes the future from the matching
 * {@link ProtocolAdapterWrapperEvent.BrowseResultReceived} (or fails it on the deadline / a connection loss with a
 * {@link BrowseRejectedException}).
 * <p>
 * Band: {@link MailboxMessagePriority#CONTROL} — like the goal and lifecycle commands it is operator-initiated and
 * never starved by device traffic. It is its own kind of {@link ProtocolAdapterWrapperMessage}: it is
 * not a goal command (it mutates no goal and bypasses {@code onGoalChange}), not a transition-table event, and not
 * a tick. The future is the one mutable thing that crosses the boundary, the standard request/response bridge.
 *
 * @param filter     the browse filter selecting where to browse.
 * @param completion the future the REST thread awaits; completed with the results or completed exceptionally.
 */
public record ProtocolAdapterWrapperBrowseRequest(
        @NotNull BrowseFilter filter, @NotNull CompletableFuture<List<BrowsedNode>> completion)
        implements ProtocolAdapterWrapperMessage {

    @Override
    public @NotNull MailboxMessagePriority priority() {
        return MailboxMessagePriority.CONTROL;
    }
}
