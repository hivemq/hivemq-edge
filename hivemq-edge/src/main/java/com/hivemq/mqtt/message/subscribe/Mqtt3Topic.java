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
package com.hivemq.mqtt.message.subscribe;

import com.hivemq.mqtt.message.QoS;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a topic subscription in MQTT 3.
 *
 * @author Florian Limpöck
 * @since 4.0.0
 */
public interface Mqtt3Topic {

    /**
     * Returns the topic filter string.
     *
     * @return the topic as String representation
     */
    @NotNull
    String getTopic();

    /**
     * Returns the quality of service level for this topic.
     *
     * @return the QoS of a Topic
     */
    @NotNull
    QoS getQoS();
}
