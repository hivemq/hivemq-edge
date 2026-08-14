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
package com.hivemq.bridge;

import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public interface MessageForwarder {

    /**
     * Add forwarder to the service.
     * Multiple topic filters must be added separately.
     *
     * @param mqttForwarder the forwarder
     */
    void addForwarder(@NotNull MqttForwarder mqttForwarder);

    /**
     * Remove a topic filter from a forwarder.
     *
     * @param mqttForwarder       the forwarder
     * @param clearQueue          whether the current queue for this forwarder should be closed
     */
    void removeForwarder(@NotNull MqttForwarder mqttForwarder, final boolean clearQueue);

    /**
     * Call this method whenever new messages are available for a forwarder.
     *
     * @param queueId for which new messages are available
     */
    void messageAvailable(@NotNull String queueId);

    /**
     * Check if new messages need to be polled for the buffer.
     */
    void checkBuffers();

    /**
     * Claims the queues of a bridge that could not register its forwarders, so that the periodic
     * clean-up leaves them alone.
     * <p>
     * The clean-up clears every forwarder queue no registered forwarder owns, which is right for a
     * queue whose bridge is gone and fatal for one whose bridge merely failed to start: the messages
     * waiting in it are deleted within seconds, and the operator's chance to correct the configuration
     * with them still there is lost. A reservation is ownership without a forwarder — it makes
     * {@link #isForwarderQueue(String)} answer true, and nothing else. Messages are neither polled nor
     * forwarded while it stands.
     *
     * @param reservationId         identifies the reservation, so it can be released again; the bridge id
     * @param topicsByForwarderId   the topics each forwarder of the bridge would have registered. Passed
     *                              rather than the queue ids themselves so that the one place that
     *                              knows how a queue is named stays the one place that builds them.
     */
    void reserveQueues(@NotNull String reservationId, @NotNull Map<String, List<String>> topicsByForwarderId);

    /**
     * Drops a reservation made by {@link #reserveQueues}, either because the bridge has started and its
     * forwarders now own the queues, or because it is gone and they may be reclaimed. A no-op when
     * there is no reservation under this id.
     */
    void releaseReservedQueues(@NotNull String reservationId);

    /**
     * Records that the bridge configuration has been applied at least once, so that forwarder
     * ownership can be trusted from now on.
     */
    void markBridgeConfigurationApplied();

    /**
     * Whether any bridge configuration has been applied yet.
     * <p>
     * Until it has, {@link #isForwarderQueue(String)} answering false means "nobody has had the chance
     * to claim this queue", not "nobody owns it" — and the periodic clean-up deletes what nobody owns.
     * The clean-up service is scheduled during persistence bootstrap, before the bridge subsystem
     * exists, so a sweep landing in between would delete the queues of every bridge on the node while
     * they were waiting to be started (EDG-882, found while testing F-02).
     */
    boolean hasAppliedBridgeConfiguration();

    /**
     * Check whether a queue belongs to a currently registered forwarder.
     * Forwarder queue IDs cannot be parsed positionally: the embedded subscription hash is standard
     * Base64 and may itself contain '/', so ownership must be resolved against the registry instead.
     *
     * @param queueId the shared queue ID to check
     * @return true if a registered forwarder owns this queue
     */
    boolean isForwarderQueue(@NotNull String queueId);
}
