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

import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlMixed;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRY_MAX;

/**
 * Configuration entity for session expiry settings.
 * <p>
 * Supports two XML formats for backwards compatibility:
 * <ul>
 *   <li>Simple format: {@code <session-expiry>123</session-expiry>}</li>
 *   <li>Nested format: {@code <session-expiry><max-interval>123</max-interval></session-expiry>}</li>
 * </ul>
 * Always writes in nested format for consistency.
 *
 * @author Florian Limpöck
 * @since 4.0.0
 */
@XmlRootElement(name = "session-expiry")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class SessionExpiryConfigEntity {

    // For reading: captures both text content and child elements
    @XmlMixed
    @XmlAnyElement
    private List<Object> content = new ArrayList<>();

    // For writing: outputs as <max-interval>value</max-interval>
    @XmlElement(name = "max-interval", defaultValue = "4294967295")
    private Long maxIntervalForWrite;

    // Cached parsed value
    private Long parsedMaxInterval;

    public long getMaxInterval() {
        if (parsedMaxInterval == null) {
            parsedMaxInterval = parseValue();
        }
        return parsedMaxInterval != null ? parsedMaxInterval : SESSION_EXPIRY_MAX;
    }

    /**
     * Called by JAXB before marshalling to ensure the write field is populated.
     */
    @SuppressWarnings("unused")
    void beforeMarshal(final Marshaller marshaller) {
        maxIntervalForWrite = getMaxInterval();
        content = null; // Clear mixed content for clean output
    }

    /**
     * Parses the value from either the @XmlElement field (nested format) or the mixed content (simple format).
     * <p>
     * Priority:
     * 1. If maxIntervalForWrite was set by JAXB via @XmlElement, use it (nested format)
     * 2. Otherwise, check the mixed content for simple text format
     */
    private Long parseValue() {
        // First check if JAXB parsed the nested <max-interval> element
        if (maxIntervalForWrite != null) {
            return maxIntervalForWrite;
        }

        // Otherwise, check mixed content for simple text format
        if (content == null) {
            return null;
        }
        for (final Object item : content) {
            if (item instanceof String text) {
                // Simple text format: 123
                final String trimmed = text.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        return Long.parseLong(trimmed);
                    } catch (final NumberFormatException e) {
                        // Whitespace or invalid text, ignore
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final SessionExpiryConfigEntity that = (SessionExpiryConfigEntity) o;
        return getMaxInterval() == that.getMaxInterval();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getMaxInterval());
    }
}
