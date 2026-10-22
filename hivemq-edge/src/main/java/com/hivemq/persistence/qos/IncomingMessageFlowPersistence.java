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
package com.hivemq.persistence.qos;

import org.jetbrains.annotations.NotNull;

/**
 * The inbound QoS 2 PUBLISHes whose PUBREL has not arrived yet, as packet identifiers per client.
 * <p>
 * Only the identifier is kept. The message has been forwarded when it is recorded; the entry exists so that a
 * retransmission of the same PUBLISH is answered with PUBREC and not forwarded again, until the client releases the
 * identifier with PUBREL. The entries are bounded per client by {@code max-incoming-qos2-flows-per-client}, so that one
 * client cannot hold the node's capacity, and across all clients by {@code max-incoming-qos2-flows}.
 */
public interface IncomingMessageFlowPersistence {

    /**
     * The outcome of {@link #add(String, int)}.
     */
    enum AddResult {
        /**
         * The identifier was recorded: the PUBLISH is new and must be forwarded.
         */
        ADDED,
        /**
         * The identifier was already recorded: the PUBLISH is a retransmission and must not be forwarded again.
         */
        ALREADY_PRESENT,
        /**
         * The client or the node holds its maximum number of entries: the PUBLISH must be refused.
         */
        LIMIT_REACHED
    }

    /**
     * Records that the client sent a QoS 2 PUBLISH with this packet identifier.
     *
     * @param client    the client that sent the PUBLISH
     * @param messageId the packet identifier of the PUBLISH
     * @return whether the identifier was recorded, already present, or refused because the client or the node is at
     *         its limit
     */
    @NotNull
    AddResult add(@NotNull String client, int messageId);

    /**
     * Releases one packet identifier of the client, on PUBREL.
     *
     * @param client    the client
     * @param messageId the packet identifier
     */
    void remove(@NotNull String client, int messageId);

    /**
     * Deletes every packet identifier of the client, when its session ends. Proportional to the client's own entries.
     *
     * @param client the client
     */
    void delete(@NotNull String client);

    /**
     * @return the number of packet identifiers held across all clients
     */
    long size();

    /**
     * close the persistence with all buckets.
     */
    void closeDB();
}
