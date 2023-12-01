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

import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;

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
}
