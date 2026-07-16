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

import com.hivemq.adapter.sdk.api.v2.node.Node;
import org.jetbrains.annotations.NotNull;

/**
 * Builds the {@link SouthboundWriteBacklog} behind one write-mapped tag's delivery channel — the seam that lets
 * the {@link SouthboundWritePlane} run over the interim in-memory backlog today and the durable
 * {@link ClientQueueSouthboundWriteBacklog client-queue one} once the MQTT intake supplies queue ids. The plane
 * calls it once per channel creation (initial build, and again for channels replaced on a tags-only reload) and
 * closes the backlog when the channel is dropped.
 */
@FunctionalInterface
public interface SouthboundWriteBacklogFactory {

    /**
     * @param tagName the write-mapped tag the backlog will feed.
     * @param node    the tag's node — the correlation key its commands target.
     * @return the backlog behind the tag's delivery channel.
     */
    @NotNull
    SouthboundWriteBacklog create(@NotNull String tagName, @NotNull Node node);

    /**
     * The interim default: a bounded in-memory backlog per tag — not durable; commands die with the process.
     *
     * @param capacity the per-tag bound; offers beyond it shed the newest.
     * @return a factory of in-memory backlogs.
     */
    static @NotNull SouthboundWriteBacklogFactory inMemory(final int capacity) {
        return (tagName, node) -> new InMemorySouthboundWriteBacklog(capacity);
    }
}
