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
package com.hivemq.edge.adapters.opcua.condition;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A request to move a condition's state machine — what a southbound write to a condition tag means.
 * <p>
 * Edge exposes <em>one</em> action here, {@code update_state}, with {@code method} selecting the transition,
 * rather than mirroring the OPC UA server's separate {@code Acknowledge} / {@code Confirm} / {@code AddComment}
 * methods. Those all share the same shape — {@code (EventId, Comment)} invoked on a condition node — so
 * folding them into one parameterised action loses nothing and keeps Edge a conduit rather than a
 * re-implementation of an OPC UA server.
 * <p>
 * Edge may request transitions on the <em>observer</em> dimension only (acknowledge, confirm). The active
 * dimension belongs to the process and is never driven from here.
 *
 * @param eventId identifies the <em>transition</em> being responded to, as taken from the northbound message.
 *                Not the condition and not a state: the server mints a fresh one per event, so a late
 *                acknowledgement cannot be applied to a newer transition.
 * @param method  which transition to request.
 * @param comment free text recorded by the server alongside the transition; may be empty.
 */
public record ConditionUpdate(
        @NotNull ByteString eventId,
        @NotNull Method method,
        @NotNull String comment) {

    public static final @NotNull String FIELD_EVENT_ID = "eventId";
    public static final @NotNull String FIELD_METHOD = "method";
    public static final @NotNull String FIELD_COMMENT = "comment";

    /**
     * The transitions Edge can request. The wire form is the integer {@code method} field; the name is
     * accepted too, since a hand-written command is easier to read as {@code "ACKNOWLEDGE"} than as {@code 0}.
     */
    public enum Method {
        ACKNOWLEDGE(0, "Acknowledge"),
        CONFIRM(1, "Confirm");

        private final int wireValue;
        private final @NotNull String browseName;

        Method(final int wireValue, final @NotNull String browseName) {
            this.wireValue = wireValue;
            this.browseName = browseName;
        }

        public int wireValue() {
            return wireValue;
        }

        /**
         * The browse name of the OPC UA method this transition dispatches to, fixed by the specification for
         * {@code AcknowledgeableConditionType}. The method is looked up by this name on the condition
         * instance: a method must be a component of the object it is called on, so the type-level node id is
         * not itself callable.
         */
        public @NotNull String browseName() {
            return browseName;
        }

        public static @NotNull Method fromWireValue(final int wireValue) {
            for (final Method method : values()) {
                if (method.wireValue == wireValue) {
                    return method;
                }
            }
            throw new IllegalArgumentException(
                    "Unknown condition method '" + wireValue + "'. Known methods: " + describeKnownMethods());
        }

        public static @NotNull Method fromName(final @NotNull String name) {
            for (final Method method : values()) {
                if (method.name().equalsIgnoreCase(name)) {
                    return method;
                }
            }
            throw new IllegalArgumentException(
                    "Unknown condition method '" + name + "'. Known methods: " + describeKnownMethods());
        }

        private static @NotNull String describeKnownMethods() {
            final StringBuilder known = new StringBuilder();
            for (final Method method : values()) {
                if (!known.isEmpty()) {
                    known.append(", ");
                }
                known.append(method.wireValue)
                        .append(" (")
                        .append(method.name())
                        .append(')');
            }
            return known.toString();
        }
    }

    /**
     * Reads a condition update from the payload of a southbound message.
     *
     * @param payload the write payload's value.
     * @return the parsed update.
     * @throws IllegalArgumentException if a field is missing or malformed. A command that cannot be understood
     *                                  is rejected rather than guessed at: silently acknowledging the wrong
     *                                  transition is worse than failing the write.
     */
    public static @NotNull ConditionUpdate fromJson(final @Nullable JsonNode payload) {
        if (payload == null || !payload.isObject()) {
            throw new IllegalArgumentException("A condition update must be an object with '" + FIELD_EVENT_ID
                    + "' and '"
                    + FIELD_METHOD
                    + "' fields");
        }

        final JsonNode eventIdNode = payload.get(FIELD_EVENT_ID);
        if (eventIdNode == null
                || !eventIdNode.isTextual()
                || eventIdNode.asText().isEmpty()) {
            throw new IllegalArgumentException("A condition update needs a non-empty '" + FIELD_EVENT_ID
                    + "', taken from the northbound message it responds to");
        }

        final JsonNode methodNode = payload.get(FIELD_METHOD);
        if (methodNode == null) {
            throw new IllegalArgumentException("A condition update needs a '" + FIELD_METHOD + "' field");
        }
        final Method method =
                methodNode.isNumber() ? Method.fromWireValue(methodNode.asInt()) : Method.fromName(methodNode.asText());

        final JsonNode commentNode = payload.get(FIELD_COMMENT);
        final String comment = commentNode == null || commentNode.isNull() ? "" : commentNode.asText();

        return new ConditionUpdate(decodeEventId(eventIdNode.asText()), method, comment);
    }

    /**
     * Turns the textual {@code eventId} back into the bytes the server issued.
     * <p>
     * The northbound converter renders a {@code ByteString} as base64, so that is what a client echoes back
     * and what is tried first. A value that is not base64 is taken literally: a server that mints printable
     * event ids would otherwise be impossible to acknowledge by hand.
     */
    private static @NotNull ByteString decodeEventId(final @NotNull String eventId) {
        try {
            return new ByteString(Base64.getDecoder().decode(eventId));
        } catch (final IllegalArgumentException notBase64) {
            return new ByteString(eventId.getBytes(StandardCharsets.UTF_8));
        }
    }
}
