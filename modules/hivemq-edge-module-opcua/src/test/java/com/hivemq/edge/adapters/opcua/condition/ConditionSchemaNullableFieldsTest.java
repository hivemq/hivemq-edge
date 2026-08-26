/*
 * Copyright 2023-present HiveMQ GmbH
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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.schema.SchemaJsonRepresentation;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationOutput;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaConditionType;
import com.hivemq.protocols.tag.TagSchemaCreationOutputImpl;
import com.hivemq.protocols.tag.TagSchemaDirection;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * Which fields of a condition command admit {@code null}, and — the part worth a test of its own — which
 * deliberately do not.
 * <p>
 * <b>{@code comment} does, and that is EDG-835 QA finding P4.3.</b> The other tests in this package check the
 * schema by reading it and the parser by running payloads through it, and neither could find that defect: the
 * schema said {@code comment} was a string and meant it, {@code fromJson} read an explicit {@code null} as
 * "leave the existing comment alone" and meant that, and both were right on their own. What was wrong was only
 * visible <em>between</em> them — the gate refused the payload before the parser that understood it was reached.
 * So this test drives the gate itself: the same networknt validator Edge puts in front of a southbound write,
 * against the same composed document a client fetches from
 * {@code GET .../writing-schema?direction=SOUTHBOUND}. The assertions are about acceptance rather than about a
 * rendered type, because a type array is a proxy for what broke and what broke is a validator answering no.
 * <p>
 * <b>{@code eventId}, {@code duration} and {@code selectedResponse} do not, and that is a decision rather than
 * an oversight.</b> They are OPC UA method arguments, required by the specification wherever they apply
 * ({@code Acknowledge}'s {@code EventId} §5.7.2, {@code TimedShelve}'s {@code ShelvingTime} §5.8.10.4,
 * {@code Respond}'s {@code SelectedResponse} §5.6.3), and there is no null value of any of them that means
 * anything. A caller with nothing to say about such a field omits the key. This half of the test exists because
 * the argument for relaxing them is easy to make and was made — a client that serialises a command struct emits
 * {@code "field": null} for every unset field by default, so these refusals are met in practice — and the
 * answer is that the client suppresses its null keys, not that the schema advertises a value no method accepts.
 * Recorded here so a future reader meets the reasoning rather than an inconsistency.
 */
class ConditionSchemaNullableFieldsTest {

    private static final @NotNull ObjectMapper MAPPER = new ObjectMapper();

    /** A real base64 EventId, since that is what a client echoes back from a northbound message. */
    private static final @NotNull String EVENT_ID = "aGVsbG8gd29ybGQ=";

    /**
     * The three fields that are OPC UA method arguments. Optional in this schema — most methods take none of
     * them — but never nullable, which is the distinction this test is about.
     */
    private static final @NotNull List<String> ARGUMENT_FIELDS = List.of(
            ConditionUpdate.FIELD_EVENT_ID, ConditionUpdate.FIELD_DURATION, ConditionUpdate.FIELD_SELECTED_RESPONSE);

    // ── the finding: comment admits null ─────────────────────────────────────────────────────────────

    @Test
    void anExplicitNullCommentIsAcceptedAsTheAbsentIntent() throws Exception {
        // The finding verbatim. Before the fix this failed with
        // "$.value.comment: null found, string expected" -- the schema refusing an intent the specification
        // defines, the parser implements and the documentation promises.
        assertThat(gateErrors("{\"method\": \"ACKNOWLEDGE\", \"eventId\": \"" + EVENT_ID + "\", \"comment\": null}"))
                .as("a null comment is the 'leave the existing comment alone' intent, not a malformed command")
                .isEmpty();
    }

    @Test
    void andTheParserBehindTheGateReadsItThatWay() throws Exception {
        // The other half, so the pair states the whole contract rather than half of it: the gate lets it
        // through, and what is behind the gate does the right thing with it. Pinned on its own in
        // ConditionUpdateMethodFieldTest; repeated here because "accepted" is only good news if the intent
        // survives the parse.
        final ConditionUpdate update = ConditionUpdate.fromJson(
                json("{\"method\": \"ACKNOWLEDGE\", \"eventId\": \"" + EVENT_ID + "\", \"comment\": null}"));

        assertThat(update.comment()).isNull();
    }

    @Test
    void theThreeCommentIntentsRemainDistinctAndAllValid() throws Exception {
        // Admitting null must not collapse the three meanings into two. All three shapes pass the gate, and the
        // parser still tells them apart -- absent and null leave the comment alone, "" erases it.
        assertThat(gateErrors("{\"method\": \"ENABLE\"}")).isEmpty();
        assertThat(gateErrors("{\"method\": \"ENABLE\", \"comment\": null}")).isEmpty();
        assertThat(gateErrors("{\"method\": \"ENABLE\", \"comment\": \"\"}")).isEmpty();
        assertThat(gateErrors("{\"method\": \"ENABLE\", \"comment\": \"seen it\"}"))
                .isEmpty();

        assertThat(ConditionUpdate.fromJson(json("{\"method\": \"ENABLE\"}")).comment())
                .isNull();
        assertThat(ConditionUpdate.fromJson(json("{\"method\": \"ENABLE\", \"comment\": null}"))
                        .comment())
                .isNull();
        assertThat(ConditionUpdate.fromJson(json("{\"method\": \"ENABLE\", \"comment\": \"\"}"))
                        .comment())
                .isEmpty();
    }

    // ── the decision: the argument fields do not ─────────────────────────────────────────────────────

    @Test
    void commentIsTheOnlyFieldThatAdmitsNull() throws Exception {
        // The contract in one place. `comment` takes a null because OPC 10000-9 §5.7.3 gives null a meaning
        // there; an argument field does not, because there is no null EventId, shelving time or response index
        // that means anything. Driven off the list so a new argument field has to declare which side it is on.
        assertThat(gateErrors("{\"method\": \"ENABLE\", \"comment\": null}"))
                .as("comment carries the specification's 'leave it unchanged' intent as a null")
                .isEmpty();

        for (final String field : ARGUMENT_FIELDS) {
            assertThat(gateErrors("{\"method\": \"ENABLE\", \"" + field + "\": null}"))
                    .as("'%s' is an OPC UA method argument, so null is not a value it can carry -- omit the key", field)
                    .isNotEmpty();
        }
    }

    @Test
    void aStructSerialisedWithItsNullKeysIntactIsRefusedOnThoseFields() throws Exception {
        // The case a client meets first, asserted so the refusal reads as intended rather than as a bug someone
        // finds again later. Jackson, JSON.stringify and json.dumps all emit "field": null for a field that was
        // never assigned, so an ACKNOWLEDGE built as a five-field struct arrives carrying nulls for the two
        // arguments it does not use -- and is refused on both.
        assertThat(gateErrors("{\"method\": \"ACKNOWLEDGE\", \"eventId\": \"" + EVENT_ID + "\", "
                        + "\"comment\": null, \"duration\": null, \"selectedResponse\": null}"))
                .as("the argument fields are refused; the caller suppresses null keys rather than the schema "
                        + "admitting a value no method accepts")
                .isNotEmpty();
    }

    @Test
    void andTheSameCommandWithItsNullKeysSuppressedIsAccepted() throws Exception {
        // The other side of the same coin, and the actionable half: @JsonInclude(NON_NULL), a JSON.stringify
        // replacer or exclude_none produces this, and it passes. `comment` keeps its null, because that one is
        // a value the field genuinely carries rather than an unset key.
        assertThat(gateErrors("{\"method\": \"ACKNOWLEDGE\", \"eventId\": \"" + EVENT_ID + "\", \"comment\": null}"))
                .isEmpty();
        assertThat(gateErrors("{\"method\": \"TIMED_SHELVE\", \"duration\": 60000, \"comment\": null}"))
                .isEmpty();
        assertThat(gateErrors("{\"method\": \"RESPOND\", \"selectedResponse\": 1, \"comment\": null}"))
                .isEmpty();
        assertThat(gateErrors("{\"method\": \"ENABLE\"}")).isEmpty();
    }

    // ── what deliberately did not change ────────────────────────────────────────────────────────────

    @Test
    void aNullMethodIsStillRefusedByTheGate() throws Exception {
        // `method` decides which of the others apply, so a command that does not name an action is not an
        // under-specified command but a different kind of thing. Refused as early as it can be, which is here.
        assertThat(gateErrors("{\"method\": null}"))
                .as("a command that names no action must not reach the adapter")
                .isNotEmpty();
    }

    @Test
    void aMistypedFieldIsStillRefused() throws Exception {
        // Admitting a null on one field is not admitting anything anywhere. A number where a note belongs is
        // still a defect the gate catches -- and `comment` is the field where a coercion would be dangerous,
        // since the empty string an object or a number can coerce to is the deliberate erase.
        assertThat(gateErrors("{\"method\": \"ENABLE\", \"comment\": 42}")).isNotEmpty();
        assertThat(gateErrors("{\"method\": \"ENABLE\", \"comment\": {}}")).isNotEmpty();
        assertThat(gateErrors("{\"method\": \"TIMED_SHELVE\", \"duration\": \"soon\"}"))
                .isNotEmpty();
    }

    @Test
    void selectedResponseKeepsItsInt32Bounds() throws Exception {
        // The bounds exist so a generated client cannot build a request the protocol cannot express. Asserted
        // here because this is the field where a nullable type array would have sat beside them, and the check
        // is worth keeping whichever way that went.
        assertThat(gateErrors("{\"method\": \"RESPOND\", \"selectedResponse\": 1}"))
                .isEmpty();
        assertThat(gateErrors("{\"method\": \"RESPOND\", \"selectedResponse\": 4294967296}"))
                .as("an Int32 index cannot exceed %d", Integer.MAX_VALUE)
                .isNotEmpty();
        assertThat(gateErrors("{\"method\": \"RESPOND\", \"selectedResponse\": -1}"))
                .as("an index into ResponseOptionSet cannot be negative")
                .isNotEmpty();
    }

    @Test
    void theRefreshCommandStillRequiresTheOneFieldItHas() throws Exception {
        // The refresh command is deliberately shaped like a condition command, and its single field is the same
        // kind of field as `method`: the action itself. It has nothing nullable to gain, so nothing here changed
        // -- stated so that "a command field admits null" is not read as applying to it too.
        final ObjectNode refresh =
                SchemaJsonRepresentation.INSTANCE.toJsonSchemaDocument(ConditionSchemas.refreshCommandSchema());

        assertThat(validate(refresh, json("{\"method\": \"REFRESH\"}"))).isEmpty();
        assertThat(validate(refresh, json("{\"method\": null}"))).isNotEmpty();
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────

    /**
     * Runs a condition command through the gate and returns its complaints — empty meaning accepted.
     * <p>
     * The document is the one a client actually fetches for a condition tag:
     * {@link ConditionSchemas#writeSchema()} composed by {@link TagSchemaCreationOutputImpl}, so the error paths
     * are the ones an operator sees ({@code $.value.comment}) and the composition cannot be what breaks the
     * contract. The command is wrapped in the {@code value} envelope that document describes.
     */
    private static @NotNull Set<ValidationMessage> gateErrors(final @NotNull String command) throws Exception {
        final var output = new TagSchemaCreationOutputImpl();
        output.finish(new TagSchemaCreationOutput.DataPointSchema(
                ConditionSchemas.readSchema(
                        OpcuaConditionType.fromBrowseName("AlarmConditionType").orElseThrow()),
                null,
                null,
                ConditionSchemas.writeSchema()));

        return validate(output.getSchema(TagSchemaDirection.SOUTHBOUND), json("{\"value\": " + command + "}"));
    }

    private static @NotNull Set<ValidationMessage> validate(
            final @NotNull ObjectNode document, final @NotNull JsonNode message) {
        return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909)
                .getSchema(document)
                .validate(message);
    }

    private static @NotNull JsonNode json(final @NotNull String raw) throws JsonProcessingException {
        return MAPPER.readTree(raw);
    }
}
