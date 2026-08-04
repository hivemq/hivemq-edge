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
import java.util.Base64;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A request to move a condition's state machine — what a southbound write to a condition tag means.
 * <p>
 * Edge exposes <em>one</em> action here, {@code update_state}, with {@code method} selecting which of the
 * condition's methods to invoke, rather than one tag or schema per OPC UA method. Every method the condition
 * offers is available: Edge does not decide which of the server's own operations an operator may call.
 * <p>
 * The methods do <em>not</em> share a signature — three take {@code (EventId, Comment)}, ten take no arguments
 * at all, and {@code TimedShelve} takes a duration. That is why every field but {@code method} is optional:
 * which of them apply, and which are required, follows from the method. The check is made here rather than
 * left to the server, so a command that cannot be carried out is rejected before it becomes a call.
 * <p>
 * {@code GetGroupMemberships} is deliberately absent: it returns data instead of requesting a transition, so a
 * southbound write is the wrong shape for it.
 *
 * @param method   which method to invoke. The only always-required field.
 * @param eventId  identifies the <em>transition</em> being responded to, as taken from the northbound message.
 *                 Not the condition and not a state: the server mints a fresh one per event, so a late
 *                 acknowledgement cannot be applied to a newer transition. Null for methods that act on the
 *                 condition as a whole.
 * @param comment  free text recorded by the server alongside the transition. <b>Null and empty mean different
 *                 things</b> — null leaves any existing comment untouched, empty erases it. See
 *                 {@link #fromJson}.
 * @param duration the shelving time in milliseconds; null for every method except {@code TimedShelve}.
 */
public record ConditionUpdate(
        @NotNull Method method,
        @Nullable ByteString eventId,
        @Nullable String comment,
        @Nullable Double duration) {

    public static final @NotNull String FIELD_EVENT_ID = "eventId";
    public static final @NotNull String FIELD_METHOD = "method";
    public static final @NotNull String FIELD_COMMENT = "comment";
    public static final @NotNull String FIELD_DURATION = "duration";

    /**
     * The transitions Edge can request. The wire form is the integer {@code method} field; the name is
     * accepted too, since a hand-written command is easier to read as {@code "ACKNOWLEDGE"} than as {@code 0}.
     */
    public enum Method {

        // --- (EventId, Comment): act on one specific transition -------------------------------------------
        ACKNOWLEDGE(0, "Acknowledge", Arguments.EVENT_AND_COMMENT, Location.CONDITION),
        CONFIRM(1, "Confirm", Arguments.EVENT_AND_COMMENT, Location.CONDITION),
        ADD_COMMENT(2, "AddComment", Arguments.EVENT_AND_COMMENT, Location.CONDITION),

        // --- no arguments: act on the condition as a whole ------------------------------------------------
        ENABLE(10, "Enable", Arguments.NONE, Location.CONDITION),
        DISABLE(11, "Disable", Arguments.NONE, Location.CONDITION),
        SILENCE(12, "Silence", Arguments.NONE, Location.CONDITION),
        SUPPRESS(13, "Suppress", Arguments.NONE, Location.CONDITION),
        UNSUPPRESS(14, "Unsuppress", Arguments.NONE, Location.CONDITION),
        REMOVE_FROM_SERVICE(15, "RemoveFromService", Arguments.NONE, Location.CONDITION),
        PLACE_IN_SERVICE(16, "PlaceInService", Arguments.NONE, Location.CONDITION),
        RESET(17, "Reset", Arguments.NONE, Location.CONDITION),

        // --- shelving: on the condition's ShelvingState object, not on the condition ----------------------
        UNSHELVE(20, "Unshelve", Arguments.NONE, Location.SHELVING_STATE),
        ONE_SHOT_SHELVE(21, "OneShotShelve", Arguments.NONE, Location.SHELVING_STATE),
        TIMED_SHELVE(22, "TimedShelve", Arguments.DURATION, Location.SHELVING_STATE);

        /**
         * What the method takes. Ten of the fourteen take nothing at all, which is why the command's fields
         * are optional rather than the command being split into several shapes.
         */
        public enum Arguments {
            /** {@code (EventId, Comment)} — the method acts on the transition named by the event id. */
            EVENT_AND_COMMENT,
            /** No input arguments; the method acts on the condition as a whole. */
            NONE,
            /** {@code (ShelvingTime)} — a duration in milliseconds. */
            DURATION
        }

        /** Where the method node hangs, which decides how it is resolved on the instance. */
        public enum Location {
            /** A direct component of the condition instance. */
            CONDITION,
            /** A component of the condition's {@code ShelvingState} object, one level deeper. */
            SHELVING_STATE
        }

        private final int wireValue;
        private final @NotNull String browseName;
        private final @NotNull Arguments arguments;
        private final @NotNull Location location;

        Method(
                final int wireValue,
                final @NotNull String browseName,
                final @NotNull Arguments arguments,
                final @NotNull Location location) {
            this.wireValue = wireValue;
            this.browseName = browseName;
            this.arguments = arguments;
            this.location = location;
        }

        public int wireValue() {
            return wireValue;
        }

        /**
         * The browse name of the OPC UA method, fixed by the specification. The method is looked up by this
         * name on the condition instance, which is the form every server accepts — though not the only legal
         * one: OPC 10000-4 §5.12.2.2 also permits the type's method id, which is what Enable and Disable fall
         * back to on servers that keep their conditions out of the AddressSpace.
         */
        public @NotNull String browseName() {
            return browseName;
        }

        public @NotNull Arguments arguments() {
            return arguments;
        }

        public @NotNull Location location() {
            return location;
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
            throw new IllegalArgumentException(
                    "A condition update must be an object with at least a '" + FIELD_METHOD + "' field");
        }

        // `method` is the only field that is always required: it determines which of the others apply.
        final JsonNode methodNode = payload.get(FIELD_METHOD);
        if (methodNode == null || methodNode.isNull()) {
            throw new IllegalArgumentException("A condition update needs a '" + FIELD_METHOD + "' field");
        }
        final Method method =
                methodNode.isNumber() ? Method.fromWireValue(methodNode.asInt()) : Method.fromName(methodNode.asText());

        // Absent and empty are kept apart, because the specification gives them opposite meanings (§5.7.3:
        // "If the comment field is NULL [...] any existing comments will remain unchanged. To reset the
        // comment, an empty text with a locale shall be provided."). JSON already draws that distinction, so
        // it costs nothing to carry: no key means leave it alone, "" means erase it.
        //
        // An explicit `"comment": null` is treated as absent rather than as an erase. It reads as "I am not
        // saying anything about the comment", and serialisers that emit nulls for unset fields are common
        // enough that reading it as "erase" would surprise a caller who never thought about the field.
        final JsonNode commentNode = payload.get(FIELD_COMMENT);
        final String comment = commentNode == null || commentNode.isNull() ? null : commentNode.asText();

        final ByteString eventId = readEventId(payload, method);
        final Double duration = readDuration(payload, method);

        return new ConditionUpdate(method, eventId, comment, duration);
    }

    /**
     * Reads {@code eventId}, required exactly for the methods that act on a single transition.
     * <p>
     * For the other methods it is not merely optional but meaningless — {@code Suppress} applies to the
     * condition, not to one of its transitions — so a value supplied there is ignored rather than passed on.
     */
    private static @Nullable ByteString readEventId(final @NotNull JsonNode payload, final @NotNull Method method) {
        if (method.arguments() != Method.Arguments.EVENT_AND_COMMENT) {
            return null;
        }
        final JsonNode eventIdNode = payload.get(FIELD_EVENT_ID);
        if (eventIdNode == null
                || !eventIdNode.isTextual()
                || eventIdNode.asText().isEmpty()) {
            throw new IllegalArgumentException("'" + method.name() + "' needs a non-empty '" + FIELD_EVENT_ID
                    + "', taken from the northbound message it responds to");
        }
        return decodeEventId(eventIdNode.asText());
    }

    /**
     * Reads {@code duration}, required exactly for {@code TimedShelve} and meaningless elsewhere.
     */
    private static @Nullable Double readDuration(final @NotNull JsonNode payload, final @NotNull Method method) {
        if (method.arguments() != Method.Arguments.DURATION) {
            return null;
        }
        final JsonNode durationNode = payload.get(FIELD_DURATION);
        if (durationNode == null || !durationNode.isNumber()) {
            throw new IllegalArgumentException(
                    "'" + method.name() + "' needs a numeric '" + FIELD_DURATION + "' in milliseconds");
        }
        return durationNode.asDouble();
    }

    /**
     * Turns the textual {@code eventId} back into the bytes the server issued.
     * <p>
     * Base64, always: an {@code EventId} is an opaque {@code ByteString} the northbound converter renders as
     * base64, and the schema declares it {@code BINARY} — {@code contentEncoding: base64} — so a client
     * echoes back exactly what it received. There is deliberately no fallback for a value that fails to
     * decode. Base64 cannot be told apart from arbitrary text by inspection, so guessing would silently turn
     * a mistyped id into a different one; rejecting says plainly what went wrong.
     */
    private static @NotNull ByteString decodeEventId(final @NotNull String eventId) {
        try {
            return new ByteString(Base64.getDecoder().decode(eventId));
        } catch (final IllegalArgumentException notBase64) {
            throw new IllegalArgumentException("'" + FIELD_EVENT_ID
                    + "' must be the base64 value from the northbound message, echoed back unchanged: "
                    + notBase64.getMessage());
        }
    }
}
