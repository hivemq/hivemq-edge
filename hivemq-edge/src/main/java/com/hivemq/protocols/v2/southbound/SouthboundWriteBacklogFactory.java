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
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperMessage;
import org.jetbrains.annotations.NotNull;

/**
 * Builds the {@link SouthboundWriteBacklog} behind one write-mapped tag's delivery channel — the seam that lets the
 * {@link SouthboundWritePlane} run over the durable {@link ClientQueueSouthboundWriteBacklog client-queue store} in
 * production and the in-memory stand-in where there is no broker runtime. The plane calls it once per channel
 * creation and closes the store when the channel is dropped. A tags-only reload creates nothing: a surviving tag
 * keeps its channel and its store, and only follows the tag to a new node.
 */
@FunctionalInterface
public interface SouthboundWriteBacklogFactory {

    /**
     * @param tagName       the write-mapped tag the store will feed — the key its answers are addressed to, and the
     *                      key a durable store's queue is derived from.
     * @param node          the tag's node at creation time. Neither implementation uses it: a store belongs to the
     *                      tag, not to whatever node the tag currently addresses, which is why a tag re-pointed at a
     *                      different node keeps the very same queue. Kept so an implementation that genuinely is
     *                      node-scoped remains expressible.
     * @param wrapperSender the wrapper mailbox the store tells its answers to; every store is asynchronous, and
     *                      this is how its answers reach the dispatch thread that owns the delivery state.
     * @return the store behind the tag's delivery channel.
     */
    @NotNull
    SouthboundWriteBacklog create(
            @NotNull String tagName,
            @NotNull Node node,
            @NotNull MailboxSender<ProtocolAdapterWrapperMessage> wrapperSender);

    /**
     * The non-durable default: a bounded in-memory store per tag — commands die with the process.
     *
     * @param capacity the per-tag bound; offers beyond it shed the newest.
     * @return a factory of in-memory stores.
     */
    static @NotNull SouthboundWriteBacklogFactory inMemory(final int capacity) {
        return (tagName, node, wrapperSender) -> new InMemorySouthboundWriteBacklog(capacity, tagName, wrapperSender);
    }
}
