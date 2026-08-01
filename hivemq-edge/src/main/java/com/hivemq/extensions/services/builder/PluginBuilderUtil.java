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
package com.hivemq.extensions.services.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.configuration.entity.mqtt.MqttConfigurationDefaults.MAX_EXPIRY_INTERVAL_DEFAULT;

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.util.Topics;
import com.hivemq.util.Utf8Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class is tested by the Builder Impl unit tests.
 *
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class PluginBuilderUtil {

    public static final int UTF_8_STRING_MAX_LENGTH = 65535;

    public static boolean isValidUtf8String(final @NotNull String stringToValidate, final boolean validateUTF8) {
        if (Utf8Utils.containsMustNotCharacters(stringToValidate)) {
            return false;
        }
        return !validateUTF8 || !Utf8Utils.hasControlOrNonCharacter(stringToValidate);
    }

    /**
     * EDG-811: the message expiry interval has two ranges, and only one of them is a duration.
     * <ul>
     *     <li>{@code 0 .. 2^32-1} — a real duration, checked against the configured maximum and written to
     *     the wire as the four-byte Message Expiry Interval property.</li>
     *     <li>{@code >= 2^32} — "no expiry". MQTT expresses this by omitting the property, so there is no
     *     wire value to bound; several such markers exist internally ({@code MAX_EXPIRY_INTERVAL_DEFAULT},
     *     {@code MESSAGE_EXPIRY_INTERVAL_NOT_SET}, {@code JS_MAX_SAFE_INTEGER}) and all mean the same thing.</li>
     * </ul>
     * Validating the second range as if it were a duration is what broke the bidirectional adapter's
     * dead-letter repost: copying a publish that had no expiry threw, because the "absent" marker is
     * {@code Long.MAX_VALUE} and therefore far above any configured maximum.
     * <p>
     * Note the resulting asymmetry: an operator who configures a one-hour maximum rejects a client asking
     * for two hours but accepts one asking for no expiry at all. That matches MQTT, where an absent property
     * is the protocol default and already outlives any configured bound.
     */
    public static void checkMessageExpiryInterval(
            final long messageExpiryInterval, final long maxMessageExpiryInterval) {
        if (messageExpiryInterval >= MAX_EXPIRY_INTERVAL_DEFAULT) {
            return;
        }
        checkArgument(
                messageExpiryInterval <= maxMessageExpiryInterval,
                "Message expiry interval %s not allowed. Maximum = %s",
                messageExpiryInterval,
                maxMessageExpiryInterval);
        checkArgument(
                messageExpiryInterval > 0,
                "Message expiry interval must be bigger than 0 was %s.",
                messageExpiryInterval);
    }

    public static void checkResponseTopic(final @Nullable String responseTopic, final boolean validateUTF8) {
        if (responseTopic == null) {
            return;
        }

        checkUtf8StringLength(responseTopic, "Response topic");

        if (!isValidUtf8String(responseTopic, validateUTF8)) {
            throw new IllegalArgumentException("The response topic (" + responseTopic + ") is UTF-8 malformed");
        }
    }

    public static void checkReasonString(final @Nullable String reasonString, final boolean validateUTF8) {
        if (reasonString == null) {
            return;
        }

        checkUtf8StringLength(reasonString, "Reason string");

        if (!isValidUtf8String(reasonString, validateUTF8)) {
            throw new IllegalArgumentException("The reason string (" + reasonString + ") is UTF-8 malformed");
        }
    }

    public static void checkResponseInformation(
            final @Nullable String responseInformation,
            final boolean requestResponseInformation,
            final boolean validateUTF8) {
        if (responseInformation == null) {
            return;
        }

        if (!requestResponseInformation) {
            throw new IllegalStateException(
                    "Response information must not be set if it was not requested in the CONNECT message");
        }

        checkUtf8StringLength(responseInformation, "Response information");

        if (!isValidUtf8String(responseInformation, validateUTF8)) {
            throw new IllegalArgumentException(
                    "The response information (" + responseInformation + ") is UTF-8 malformed");
        }
    }

    public static void checkServerReference(final @Nullable String serverReference, final boolean validateUTF8) {
        if (serverReference == null) {
            return;
        }

        checkUtf8StringLength(serverReference, "Server reference");

        if (!isValidUtf8String(serverReference, validateUTF8)) {
            throw new IllegalArgumentException("The server reference (" + serverReference + ") is UTF-8 malformed");
        }
    }

    public static void checkContentType(final @Nullable String contentType, final boolean validateUTF8) {
        if (contentType == null) {
            return;
        }

        checkUtf8StringLength(contentType, "Content type");

        if (!isValidUtf8String(contentType, validateUTF8)) {
            throw new IllegalArgumentException("The content type (" + contentType + ") is UTF-8 malformed");
        }
    }

    public static void checkUserProperty(
            final @NotNull String name, final @NotNull String value, final boolean validateUTF8) {
        checkUserPropertyName(name, validateUTF8);
        checkUserPropertyValue(value, validateUTF8);
    }

    public static void checkUserPropertyName(final @NotNull String name, final boolean validateUTF8) {
        checkNotNull(name, "Name must never be null");

        checkUtf8StringLength(name, "User property name");

        if (!isValidUtf8String(name, validateUTF8)) {
            throw new IllegalArgumentException("The user property name (" + name + ") is UTF-8 malformed");
        }
    }

    public static void checkUserPropertyValue(final @NotNull String value, final boolean validateUTF8) {
        checkNotNull(value, "Value must never be null");

        checkUtf8StringLength(value, "User property value");

        if (!isValidUtf8String(value, validateUTF8)) {
            throw new IllegalArgumentException("The user property value (" + value + ") is UTF-8 malformed");
        }
    }

    public static void checkQos(final @NotNull Qos qos, final int maxQos) {
        checkNotNull(qos, "QoS must not be null");
        if (qos.getQosNumber() > maxQos) {
            throw new IllegalArgumentException("QoS " + qos.getQosNumber() + " not allowed. Maximum = " + maxQos);
        }
    }

    public static void checkTopic(final @NotNull String topic, final int maxTopicLength, final boolean validateUtf8) {
        checkNotNull(topic, "Topic must not be null");
        checkArgument(
                topic.length() <= maxTopicLength,
                "Topic length must not exceed '%s' characters, but has '%s' characters",
                maxTopicLength,
                topic.length());

        if (!Topics.isValidTopicToPublish(topic)) {
            throw new IllegalArgumentException("The topic (" + topic + ") is invalid for retained PUBLISH messages");
        }

        if (!isValidUtf8String(topic, validateUtf8)) {
            throw new IllegalArgumentException("The topic (" + topic + ") is UTF-8 malformed");
        }
    }

    public static void checkClientIdentifier(final @Nullable String clientIdentifier, final boolean validateUtf8) {

        if (clientIdentifier == null) {
            return;
        }

        checkUtf8StringLength(clientIdentifier, "Client ID");

        if (!isValidUtf8String(clientIdentifier, validateUtf8)) {
            throw new IllegalArgumentException("The client ID (" + clientIdentifier + ") is UTF-8 malformed");
        }

        Preconditions.checkArgument(!clientIdentifier.isEmpty(), "Client ID must not be empty");
    }

    private static void checkUtf8StringLength(final @NotNull String utf8String, final @NotNull String type) {

        if (utf8String.length() > UTF_8_STRING_MAX_LENGTH) {
            throw new IllegalArgumentException(type + " length must not exceed '" + UTF_8_STRING_MAX_LENGTH
                    + "' characters, but has '" + utf8String.length() + "' characters");
        }
    }
}
