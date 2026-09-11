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
package com.hivemq.persistence.clientqueue;

/**
 * How a queue behaves when it is full — chosen by the component that produces the messages, and
 * carried into the persistence rather than guessed there.
 * <p>
 * EDG-882, F-05. The persistence used to decide this from the queue ID: a queue whose ID started with
 * {@code $SAMPLER::} was treated as a sampler and had its oldest messages discarded, QoS 0 included.
 * But a queue ID is built from a share name, and a share name is chosen by the client — {@code
 * $share/$SAMPLER::customer/alerts} is a legal subscription that no part of Edge owns. Inferring a
 * destructive eviction policy from it meant an ordinary client could lose its own messages by naming a
 * subscription group unluckily. The same class of mistake as parsing an owner out of a queue ID, which
 * is what this ticket is about.
 * <p>
 * Deliberately named for the behaviour rather than the owner ({@code CLIENT}, {@code BRIDGE},
 * {@code SAMPLER}): the persistence has no use for who owns a queue, only for what to do when it
 * overflows, and owner values whose behaviour is identical would be a distinction the code cannot
 * honour. If a future owner needs its own behaviour, it gets its own policy here.
 */
public enum QueuePolicy {

    /**
     * The ordinary queue: the configured {@code QueuedMessagesStrategy} applies to QoS 1 and 2, and
     * QoS 0 is held only by the node-wide QoS 0 memory budget. Everything except sampling — client
     * queues, shared subscriptions, and bridge forwarders, whose outage buffer must not be silently
     * shortened.
     */
    DEFAULT,

    /**
     * A ring buffer of the most recent messages: the oldest are discarded to make room, and the count
     * bound applies to QoS 0 as well. Used by payload sampling, which exists to show the last few
     * messages on a topic and would otherwise be held only by the node-wide QoS 0 budget — a sampled
     * QoS 0 topic could then starve every other QoS 0 consumer on the node (EDG-885).
     */
    SAMPLE_RING
}
