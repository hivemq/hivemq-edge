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
 * A northbound mapping in the {@code <v2>} section: it routes a tag's polled or
 * subscribed values to an MQTT topic. The v2 config section carries only the {@code tag-name} / {@code topic}
 * pair — exactly what {@link ProtocolAdapterEntity#getReadUsedTagNames() read-used derivation} needs; the full
 * northbound message-handling detail belongs to the routing layer and is out of this section's scope. Edge only reads
 * it.
 */
@SuppressWarnings({"FieldMayBeFinal", "unused"})
@XmlAccessorType(XmlAccessType.NONE)
// Distinct XML type name so the JAXB context can host this beside the identically-named legacy
// com.hivemq.configuration.entity.adapter.NorthboundMappingEntity without a type-name collision.
@XmlType(name = "v2NorthboundMappingEntity")
public class NorthboundMappingEntity implements EntityValidatable {

    /** MQTT's UTF-8 string limit; a longer topic can never be delivered (matches the broker's restriction default). */
    public static final int MAX_TOPIC_LENGTH_BYTES = 65_535;

    @XmlAttribute(name = "tag-name", required = true)
    private @NotNull String tagName = "";

    @XmlAttribute(name = "topic", required = true)
    private @NotNull String topic = "";

    // no-arg constructor for JAXB; field initializers carry the defaults
    public NorthboundMappingEntity() {}

    public NorthboundMappingEntity(final @NotNull String tagName, final @NotNull String topic) {
        this.tagName = tagName;
        this.topic = topic;
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public @NotNull String getTopic() {
        return topic;
    }

    @Override
    public void validate(final @NotNull List<ValidationEvent> validationEvents) {
        EntityValidatable.notEmpty(validationEvents, tagName, "northbound-mapping tag-name");
        if (EntityValidatable.notEmpty(validationEvents, topic, "northbound-mapping topic")) {
            // A topic the broker cannot deliver to must be rejected here: accepting it silently drops every
            // reading behind a healthy-looking adapter status.
            EntityValidatable.notMatch(
                    validationEvents,
                    () -> Topics.isValidTopicToPublish(topic),
                    () -> "northbound-mapping topic [" + topic
                            + "] is not a valid topic to publish to (wildcards and null characters are not allowed)");
            EntityValidatable.notMatch(
                    validationEvents,
                    () -> !Topics.isDollarTopic(topic),
                    () -> "northbound-mapping topic [" + topic
                            + "] is in the reserved '$' namespace, which is owned by the broker");
            EntityValidatable.notMatch(
                    validationEvents,
                    () -> topic.getBytes(StandardCharsets.UTF_8).length <= MAX_TOPIC_LENGTH_BYTES,
                    () -> "northbound-mapping topic exceeds the maximum length of " + MAX_TOPIC_LENGTH_BYTES
                            + " bytes");
        }
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (o instanceof final NorthboundMappingEntity that) {
            return Objects.equals(tagName, that.tagName) && Objects.equals(topic, that.topic);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tagName, topic);
    }
}
