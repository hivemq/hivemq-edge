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
import com.hivemq.codec.encoder.mqtt5.UnsignedDataTypes;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.mqtt.message.publish.PUBLISH;
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
     * Validates a message expiry interval offered through a public extension SDK setter.
     * <p>
     * This is a bounded contract and stays one: the accepted range is {@code 0 ..
     * min(maxMessageExpiryInterval, MAX_EXPIRY_INTERVAL_DEFAULT)}. {@code <message-expiry>} exists to
     * guarantee that no message outlives it, so no input — however it is spelled — may pass this check
     * unbounded. Zero is legal and means "expires immediately": MQTT 5 allows it, the decoders accept it, and
     * the extension SDK documents only a negative interval as throwing (EDG-811 CR-5).
     * <p>
     * The four-byte cap is not redundant with the configured maximum. {@code MqttConfigurator} bounds the
     * maximum at {@code MAX_EXPIRY_INTERVAL_DEFAULT}, but only when parsing {@code config.xml};
     * {@code MqttConfigurationServiceImpl.setMaxMessageExpiryInterval} is unchecked, so without the cap a
     * programmatically raised maximum would let a value with no MQTT representation into the domain model.
     * The cap is {@code MAX_EXPIRY_INTERVAL_DEFAULT} (2^32) rather than {@link PUBLISH#MESSAGE_EXPIRY_INTERVAL_MAX}
     * (2^32-1) because 2^32 is the default configured maximum itself — the value the decoders clamp an absent
     * property to, and the value the encoder omits — so it must remain settable while it is the operator's bound.
     * <p>
     * <b>Internal "no expiry" markers do not belong here (EDG-811 CR2-1).</b>
     * {@link PUBLISH#MESSAGE_EXPIRY_INTERVAL_NOT_SET} is {@code Long.MAX_VALUE} and therefore sits above every
     * finite maximum; exempting it — or 2^32 — from the bound would let an extension mint a message that
     * {@code MessageExpiryHandler} never counts down and the MQTT 5 encoder never emits, i.e. a genuine
     * no-expiry publish under a ten-second maximum. A caller copying an existing publish must instead route
     * the value through {@link #isCopyableMessageExpiryDuration} and assign the canonical marker directly,
     * where {@code build()} resolves it to the configured maximum.
     */
    public static void checkMessageExpiryInterval(
            final long messageExpiryInterval, final long maxMessageExpiryInterval) {
        final long effectiveMaximum = Math.min(maxMessageExpiryInterval, MAX_EXPIRY_INTERVAL_DEFAULT);
        checkArgument(
                messageExpiryInterval <= effectiveMaximum,
                "Message expiry interval %s not allowed. Maximum = %s",
                messageExpiryInterval,
                effectiveMaximum);
        checkArgument(
                messageExpiryInterval >= 0,
                "Message expiry interval must not be negative was %s.",
                messageExpiryInterval);
    }

    /**
     * EDG-811: tells a copy boundary whether the interval it was handed is a real duration that must stay
     * subject to the operator's configured maximum, or one of the internal "no expiry" markers that has to
     * bypass the public setter entirely.
     * <p>
     * Internally "not a real duration" has been spelled at least four ways —
     * {@link PUBLISH#MESSAGE_EXPIRY_INTERVAL_NOT_SET}, {@code MAX_EXPIRY_INTERVAL_DEFAULT} (what the decoders
     * clamp an absent property to), {@code TTL_DISABLED}, and {@code JS_MAX_SAFE_INTEGER} from the northbound
     * mapping defaults. A copy boundary cannot know which one it is being handed, and once the configured
     * maximum is finite every one of them fails {@link #checkMessageExpiryInterval} — which is exactly the
     * exception the bidirectional adapter's dead-letter repost hit. So the test is "is this an MQTT four-byte
     * duration?" rather than an enumeration of markers, and anything that is not one collapses to the
     * canonical marker at the caller. This mirrors the guard {@code RemoteMqttForwarder} already applies when
     * converting a PUBLISH for a bridge client.
     * <p>
     * Real durations deliberately keep going through the bounded setter: copying a publish must not become a
     * way around {@code <message-expiry>}.
     */
    public static boolean isCopyableMessageExpiryDuration(final long messageExpiryInterval) {
        return UnsignedDataTypes.isUnsignedInt(messageExpiryInterval);
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
