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
import java.util.Locale;
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
     * The transitions Edge can request, named by the enum constant: {@code "ACKNOWLEDGE"}, {@code "SUPPRESS"}
     * and so on, matched case-insensitively. That name is the whole contract — both schemas declare
     * {@code method} a string, and nothing else is accepted.
     * <p>
     * There is deliberately no numeric form. An earlier version assigned each constant an integer and called
     * it "the wire form", which it never was: no server sees it, since the wire carries a NodeId resolved
     * from {@link #browseName()}. The numbers identified a method only inside this enum, and only
     * {@code ConditionUpdate} honoured them — {@code RefreshCommand}, documented as deliberately the same
     * shape, always required a string.
     */
    public enum Method {

        // --- (EventId, Comment): act on one specific transition -------------------------------------------
        // These three take a Comment in their base form, so they need no "2" variant.
        ACKNOWLEDGE("Acknowledge", null, Arguments.EVENT_AND_COMMENT, Location.CONDITION),
        CONFIRM("Confirm", null, Arguments.EVENT_AND_COMMENT, Location.CONDITION),
        ADD_COMMENT("AddComment", null, Arguments.EVENT_AND_COMMENT, Location.CONDITION),

        // --- no arguments: act on the condition as a whole ------------------------------------------------
        // Most have a "2" variant that is the same operation plus an optional Comment. Enable, Disable and
        // Silence do not: the specification defines no Enable2, Disable2 or Silence2, so a comment sent with
        // those can never reach any server.
        ENABLE("Enable", null, Arguments.NONE, Location.CONDITION),
        DISABLE("Disable", null, Arguments.NONE, Location.CONDITION),
        SILENCE("Silence", null, Arguments.NONE, Location.CONDITION),
        SUPPRESS("Suppress", "Suppress2", Arguments.NONE, Location.CONDITION),
        UNSUPPRESS("Unsuppress", "Unsuppress2", Arguments.NONE, Location.CONDITION),
        REMOVE_FROM_SERVICE("RemoveFromService", "RemoveFromService2", Arguments.NONE, Location.CONDITION),
        PLACE_IN_SERVICE("PlaceInService", "PlaceInService2", Arguments.NONE, Location.CONDITION),
        RESET("Reset", "Reset2", Arguments.NONE, Location.CONDITION),

        // --- shelving: on the condition's ShelvingState object, not on the condition ----------------------
        UNSHELVE("Unshelve", "Unshelve2", Arguments.NONE, Location.SHELVING_STATE),
        ONE_SHOT_SHELVE("OneShotShelve", "OneShotShelve2", Arguments.NONE, Location.SHELVING_STATE),
        TIMED_SHELVE("TimedShelve", "TimedShelve2", Arguments.DURATION, Location.SHELVING_STATE);

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

        private final @NotNull String browseName;
        private final @Nullable String commentedBrowseName;
        private final @NotNull Arguments arguments;
        private final @NotNull Location location;

        Method(
                final @NotNull String browseName,
                final @Nullable String commentedBrowseName,
                final @NotNull Arguments arguments,
                final @NotNull Location location) {
            this.browseName = browseName;
            this.commentedBrowseName = commentedBrowseName;
            this.arguments = arguments;
            this.location = location;
        }

        /**
         * The browse name of the variant that also takes a {@code Comment}, or null when there is none.
         * <p>
         * OPC UA grew these in two passes. The original methods take no arguments at all — {@code Suppress()}
         * has nowhere to put a note — so the specification later added {@code Suppress2(Comment)}, the same
         * operation plus the comment. Both are Optional and independent (Table 40 lists them separately), so
         * a server may expose either or both, and which to call is a per-device question.
         * <p>
         * Null for {@code ACKNOWLEDGE}, {@code CONFIRM} and {@code ADD_COMMENT}, which take a comment in
         * their base form, and for {@code ENABLE}, {@code DISABLE} and {@code SILENCE}, for which the
         * specification defines no "2" variant at all — a comment sent with those can reach no server.
         */
        public @Nullable String commentedBrowseName() {
            return commentedBrowseName;
        }

        /** Whether a comment can ever accompany this method, on any server. */
        public boolean acceptsComment() {
            return arguments == Arguments.EVENT_AND_COMMENT || commentedBrowseName != null;
        }

        /**
         * The browse name of the OPC UA method, fixed by the specification. The method is looked up by this
         * name on the condition instance, which is the form every server accepts — though not the only legal
         * one: OPC 10000-4 §5.12.2.2 Table 59 also permits "the NodeId of the Method in the ObjectType that
         * defines the Method", which is what every one of these falls back to on a server that keeps its
         * conditions out of the AddressSpace.
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
                known.append(method.name());
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
        //
        // A string, and only a string. The read and write schemas both declare it STRING, and the schema is
        // the contract Edge publishes -- an alias accepted by the parser but promised by nothing is found by
        // accident and then depended on. An earlier version also took an integer, but those integers were
        // Edge's own invention rather than anything OPC-UA defines: no server ever sees them, the wire
        // carries a NodeId resolved from the browse name. So "13" identified SUPPRESS only within this enum
        // and meant nothing anywhere else, while RefreshCommand -- documented as deliberately the same shape
        // -- rejected numbers outright. Three positions, two of them undocumented.
        final JsonNode methodNode = payload.get(FIELD_METHOD);
        if (methodNode == null || methodNode.isNull() || !methodNode.isTextual()) {
            throw new IllegalArgumentException("A condition update needs a '" + FIELD_METHOD
                    + "' field naming the method, for example \"" + Method.ACKNOWLEDGE.name() + "\"");
        }
        final Method method = Method.fromName(methodNode.asText());

        // Absent and empty are kept apart, because the specification gives them opposite meanings (§5.7.3:
        // "If the comment field is NULL [...] any existing comments will remain unchanged. To reset the
        // comment, an empty text with a locale shall be provided."). JSON already draws that distinction, so
        // it costs nothing to carry: no key means leave it alone, "" means erase it.
        //
        // An explicit `"comment": null` is treated as absent rather than as an erase. It reads as "I am not
        // saying anything about the comment", and serialisers that emit nulls for unset fields are common
        // enough that reading it as "erase" would surprise a caller who never thought about the field.
        //
        // A string, and only a string. `asText()` used to stand here, and Jackson's coercions make that
        // materially unsafe rather than merely lax: it renders a number or a boolean as its text, and -- the
        // dangerous one -- returns the *empty string* for an object or an array. So `"comment": {}`, which is
        // nothing anyone means, parsed as the deliberate erase form and wiped the condition's existing
        // comment. Not a local mistake either: OPC 10000-9 §5.5.2 fires a fresh event on any comment change,
        // so every other client watching that alarm is told the note is gone, and the audit trail cannot be
        // recovered. Rejecting is the only safe reading of a payload nobody meant to send.
        final JsonNode commentNode = payload.get(FIELD_COMMENT);
        final String comment;
        if (commentNode == null || commentNode.isNull()) {
            comment = null;
        } else if (commentNode.isTextual()) {
            comment = commentNode.textValue();
        } else {
            throw new IllegalArgumentException("'" + FIELD_COMMENT
                    + "' must be a string, or absent to leave the existing comment unchanged. An empty "
                    + "string erases it. Received: "
                    + commentNode.getNodeType().name().toLowerCase(Locale.ROOT));
        }

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
