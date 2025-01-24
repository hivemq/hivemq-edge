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
package com.hivemq.configuration.entity.mqtt;

import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.*;
import java.util.Objects;

@XmlRootElement(name = "queued-messages")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class QueuedMessagesConfigEntity {

    @XmlEnum
    @XmlType(name = "strategy")
    public enum QueuedMessagesStrategy {
        @XmlEnumValue("discard-oldest")
        DISCARD_OLDEST,
        @XmlEnumValue("discard")
        DISCARD
    }

    @XmlElement(name = "max-queue-size", defaultValue = "1000")
    private long maxQueueSize = MqttConfigurationDefaults.MAX_QUEUED_MESSAGES_DEFAULT;

    @XmlElement(name = "strategy", defaultValue = "discard")
    private @NotNull QueuedMessagesStrategy queuedMessagesStrategy = QueuedMessagesStrategy.DISCARD;

    public long getMaxQueueSize() {
        return maxQueueSize;
    }

    public @NotNull QueuedMessagesStrategy getQueuedMessagesStrategy() {
        return queuedMessagesStrategy;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final QueuedMessagesConfigEntity that = (QueuedMessagesConfigEntity) o;
        return getMaxQueueSize() == that.getMaxQueueSize() &&
                getQueuedMessagesStrategy() == that.getQueuedMessagesStrategy();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMaxQueueSize(), getQueuedMessagesStrategy());
    }
}
