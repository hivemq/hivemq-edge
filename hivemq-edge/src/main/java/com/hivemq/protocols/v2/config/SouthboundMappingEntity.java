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
package com.hivemq.protocols.v2.config;

import com.hivemq.configuration.entity.EntityValidatable;
import com.hivemq.util.Topics;
import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A southbound mapping in the {@code <v2>} section: it routes an MQTT topic's payload
 * to a tag's write aspect. The v2 config section carries only the {@code topic} / {@code tag-name} pair — exactly
 * what {@link ProtocolAdapterEntity#getWriteUsedTagNames() write-used derivation} needs; the full southbound
 * field-mapping detail belongs to the routing layer and is out of this section's scope. Edge only reads it.
 */
@SuppressWarnings({"FieldMayBeFinal", "unused"})
@XmlAccessorType(XmlAccessType.NONE)
// Distinct XML type name so the JAXB context can host this beside the identically-named legacy
// com.hivemq.configuration.entity.adapter.SouthboundMappingEntity without a type-name collision.
@XmlType(name = "v2SouthboundMappingEntity")
public class SouthboundMappingEntity implements EntityValidatable {

    @XmlAttribute(name = "topic", required = true)
    private @NotNull String topic = "";

    @XmlAttribute(name = "tag-name", required = true)
    private @NotNull String tagName = "";

    // no-arg constructor for JAXB; field initializers carry the defaults
    public SouthboundMappingEntity() {}

    public SouthboundMappingEntity(final @NotNull String topic, final @NotNull String tagName) {
        this.topic = topic;
        this.tagName = tagName;
    }

    public @NotNull String getTopic() {
        return topic;
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    @Override
    public void validate(final @NotNull List<ValidationEvent> validationEvents) {
        if (EntityValidatable.notEmpty(validationEvents, topic, "southbound-mapping topic")) {
            // The southbound topic is an MQTT subscription filter: wildcards are legal, but the filter itself must
            // be subscribable and must stay out of the broker-owned '$' namespace.
            EntityValidatable.notMatch(
                    validationEvents,
                    () -> Topics.isValidToSubscribe(topic),
                    () -> "southbound-mapping topic [" + topic + "] is not a valid topic filter to subscribe to");
            // '$share/…' is a legitimate client-side shared-subscription filter; only the broker-owned '$'
            // namespace itself (e.g. $SYS) is off limits — including when smuggled INSIDE a shared filter
            // ($share/group/$SYS/# effectively subscribes to $SYS).
            EntityValidatable.notMatch(
                    validationEvents,
                    () -> !Topics.isDollarTopic(topic)
                            || (topic.startsWith("$share/") && !sharedFilterTargetsDollarNamespace(topic)),
                    () -> "southbound-mapping topic [" + topic
                            + "] is in the reserved '$' namespace, which is owned by the broker");
            EntityValidatable.notMatch(
                    validationEvents,
                    () -> topic.getBytes(StandardCharsets.UTF_8).length
                            <= NorthboundMappingEntity.MAX_TOPIC_LENGTH_BYTES,
                    () -> "southbound-mapping topic exceeds the maximum length of "
                            + NorthboundMappingEntity.MAX_TOPIC_LENGTH_BYTES + " bytes");
        }
        EntityValidatable.notEmpty(validationEvents, tagName, "southbound-mapping tag-name");
    }

    /**
     * @return whether a {@code $share/<group>/<filter>} topic's inner filter itself starts with {@code $} —
     *         a shared subscription into the broker-owned namespace.
     */
    private static boolean sharedFilterTargetsDollarNamespace(final @NotNull String topic) {
        final int groupEnd = topic.indexOf('/', "$share/".length());
        return groupEnd >= 0 && topic.startsWith("$", groupEnd + 1);
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (o instanceof final SouthboundMappingEntity that) {
            return Objects.equals(topic, that.topic) && Objects.equals(tagName, that.tagName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic, tagName);
    }
}
