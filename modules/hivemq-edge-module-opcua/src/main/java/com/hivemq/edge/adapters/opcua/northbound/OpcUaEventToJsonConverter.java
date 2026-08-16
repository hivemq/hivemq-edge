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
package com.hivemq.edge.adapters.opcua.northbound;

import com.hivemq.adapter.sdk.api.datapoint.DataPointBuilder;
import com.hivemq.edge.adapters.opcua.config.tag.EventFieldSet;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaConditionType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.encoding.EncodingContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Converts one condition event into a data point.
 * <p>
 * An event notification is not a value, it is a <em>transition report</em>: the server fires it when a
 * condition's state changes, and it carries a snapshot of the resulting state. The fields arrive as a
 * {@link Variant} array whose entries correspond positionally to the select clause registered with the
 * monitored item, so the values are matched against the declared condition type's field list by index.
 * <p>
 * Every selected field is emitted, including those the server left null — a field that does not exist on a
 * particular event type comes back null rather than failing, and a consumer reading a fixed shape is better
 * served by an explicit null than by an absent key.
 */
public final class OpcUaEventToJsonConverter {

    /**
     * The key naming fields the server declined to give a value for. Not a specification browse name — every
     * other key in the payload is one, so the camel case marks this as Edge's own.
     */
    public static final @NotNull String UNAVAILABLE_FIELDS = "unavailableFields";

    /**
     * Fields whose declared type <em>is</em> a {@code StatusCode}, so a status code arriving for them is the
     * value rather than a substitution for it.
     */
    private static final @NotNull Set<String> STATUS_CODE_VALUED_FIELDS = Set.of("Quality");

    private OpcUaEventToJsonConverter() {}

    /**
     * Writes the event's fields into the value of the data point.
     *
     * @param ctx     the encoding context, for structures nested inside a field.
     * @param values  the event field values, positionally matching the declared condition type's field list.
     * @param builder the data point being built.
     */
    public static void convertPayload(
            final @NotNull EncodingContext ctx,
            final @NotNull EventFieldSet publishedFields,
            final @NotNull Variant[] values,
            final @NotNull DataPointBuilder<?> builder) {

        // The same list that built the select clause, so position i here is the field selected at i.
        final List<OpcuaConditionType.SelectedField> fields = publishedFields.selectedFields();

        // Collected before anything is written: the builder streams, so the companion object naming the
        // unavailable fields cannot be appended after the fields themselves have been emitted.
        final Map<String, String> unavailable = unavailableFields(fields, values);

        final var valueBuilder = builder.startObjectValue();
        if (!unavailable.isEmpty()) {
            final var reasons = valueBuilder.startObject(UNAVAILABLE_FIELDS);
            unavailable.forEach(reasons::put);
            reasons.endObject();
        }
        for (int i = 0; i < fields.size(); i++) {
            final OpcuaConditionType.SelectedField field = fields.get(i);
            if (field.isStateId()) {
                // Written with its state below, not on its own.
                continue;
            }
            final Object value = available(field, valueAt(values, i));
            // A two-state field's Id is selected immediately after the state itself, so it is one position
            // ahead. Folding it in has to happen here rather than in the generic converter: the builder
            // streams, so the state's object is closed before the next value is seen, and a second pass
            // could not reopen it.
            final boolean hasStateId =
                    i + 1 < fields.size() && fields.get(i + 1).isStateId();
            if (hasStateId) {
                final Object id = available(fields.get(i + 1), valueAt(values, i + 1));
                writeStateWithId(valueBuilder, field.publishedAs(), value, id, ctx);
            } else {
                OpcUaToJsonConverter.addValueToObject(valueBuilder, field.publishedAs(), value, ctx);
            }
        }
        valueBuilder.endObject();
    }

    /**
     * Which fields the server declined to give a value for, and why.
     * <p>
     * OPC 10000-4 §7.22.3 is explicit that this is <em>conforming</em> behaviour, not a broken server:
     * <blockquote>"If the selected field is supported but not available at the time of the event
     * notification, the event field shall contain a StatusCode that indicates the reason for the
     * unavailability. For example, the Server shall set the event field to Bad_UserAccessDenied if the value
     * is not accessible to the user associated with the Session. If a Value Attribute has an uncertain or
     * bad StatusCode associated with it then the Server shall provide the StatusCode instead of the Value
     * Attribute."</blockquote>
     * So <em>any</em> field, of any declared type, can arrive as a {@link StatusCode} standing in for the
     * value. Published as-is that status is indistinguishable from data — a {@code Severity} declared UInt16
     * becomes a status object, and on a two-state field the status lands under {@code text} where a display
     * string was promised while {@code id} silently vanishes.
     * <p>
     * Such a field is therefore published as null, which is what it is: a field with no value. Every field in
     * the read schema is already nullable, so this needs no consumer change. What would be lost that way is
     * the <em>reason</em> — and "null because this transition does not carry it" and "null because your
     * session may not read it" are very different facts, the second being a configuration problem someone
     * should fix. This companion object keeps them apart, and is absent entirely on the ordinary path.
     * <p>
     * {@code Quality} is deliberately not exempted even though its declared type <em>is</em> a StatusCode:
     * see {@link #isSubstituted}.
     * <p>
     * <b>Keyed by the member, not by the field, wherever the two differ.</b> A two-state field is two
     * selected entries publishing under one key — {@code ActiveState} and {@code ActiveState/Id} both land
     * under {@code ActiveState} — and naming that shared key was ambiguous in a way that mattered. Since the
     * two halves are made available independently, a server may withhold one and give the other, and the
     * result was a payload saying {@code "unavailableFields": {"ActiveState": "Bad_UserAccessDenied"}} beside
     * an {@code ActiveState} carrying a perfectly good {@code "Active"}. A consumer treating the map as a
     * list of null fields threw that away. Worse, two halves withheld for <em>different</em> reasons
     * collapsed under {@code putIfAbsent} and only the first survived.
     * <p>
     * So a composite's halves are named {@code ActiveState.text} and {@code ActiveState.id}. That is not a
     * key the payload has — the earlier reasoning against {@code ActiveState/Id} was right about that — but
     * this map was never a set of payload keys: it is Edge's own diagnostic, marked as such by being the one
     * camel-case key in a payload of browse names. Naming the half is the only way to answer the question a
     * consumer actually has, which is whether the {@code id} they were told to branch on is the one that is
     * missing.
     */
    private static @NotNull Map<String, String> unavailableFields(
            final @NotNull List<OpcuaConditionType.SelectedField> fields, final @NotNull Variant[] values) {

        final Map<String, String> unavailable = new LinkedHashMap<>();
        for (int i = 0; i < fields.size(); i++) {
            final OpcuaConditionType.SelectedField field = fields.get(i);
            if (!(valueAt(values, i) instanceof final StatusCode status)) {
                continue;
            }
            if (!isSubstituted(field)) {
                continue;
            }
            unavailable.put(diagnosticKeyFor(fields, i), describe(status));
        }
        return unavailable;
    }

    /**
     * What to call the entry at {@code index} when reporting it as unavailable.
     * <p>
     * The look-ahead matches the one in {@link #convertPayload}, and has to: a field is a composite exactly
     * when the next selected entry is its {@code Id}, so the two must agree about which entries pair up or a
     * diagnostic would name a member the payload does not have.
     */
    private static @NotNull String diagnosticKeyFor(
            final @NotNull List<OpcuaConditionType.SelectedField> fields, final int index) {

        final OpcuaConditionType.SelectedField field = fields.get(index);
        if (field.isStateId()) {
            return field.publishedAs() + ".id";
        }
        final boolean hasStateId =
                index + 1 < fields.size() && fields.get(index + 1).isStateId();
        // `.text` rather than the bare name even though `locale` sits beside it: `text` is the half a
        // consumer reads, and the two are one value the server either gave or did not.
        return hasStateId ? field.publishedAs() + ".text" : field.publishedAs();
    }

    /**
     * Whether a {@link StatusCode} arriving for this field is a substitution rather than the value itself.
     * <p>
     * It almost always is, but not for {@code Quality}, whose declared type is {@code StatusCode} (OPC
     * 10000-9 §5.5.2 Table 8) — there a status code is the value, and treating it as unavailable would blank
     * a field present in every event of every type. The two cases are genuinely ambiguous on the wire: a bad
     * {@code Quality} is both a legitimate value and, read the other way, a substitution. Resolved in favour
     * of the declared type, because a consumer asking "what is the quality of this condition" gets an answer
     * either way, whereas blanking it would lose one they cannot recover.
     */
    private static boolean isSubstituted(final @NotNull OpcuaConditionType.SelectedField field) {
        return !STATUS_CODE_VALUED_FIELDS.contains(field.publishedAs());
    }

    /**
     * The value to publish for a field: the value itself, or null where a {@link StatusCode} stands in for
     * one. The reason is recorded under {@link #UNAVAILABLE_FIELDS} rather than lost.
     */
    private static @Nullable Object available(
            final @NotNull OpcuaConditionType.SelectedField field, final @Nullable Object value) {
        return value instanceof StatusCode && isSubstituted(field) ? null : value;
    }

    /** A status code as its symbolic name where one is known, else its numeric code. */
    private static @NotNull String describe(final @NotNull StatusCode status) {
        return StatusCodes.lookup(status.getValue())
                .map(names -> names[0])
                .orElseGet(() -> "0x" + Long.toHexString(status.getValue()));
    }

    /** A server may return fewer values than were selected; treat the tail as null rather than failing. */
    private static @Nullable Object valueAt(final @NotNull Variant[] values, final int index) {
        final Variant variant = index < values.length ? values[index] : null;
        return variant == null ? null : variant.getValue();
    }

    /**
     * Writes a state as {@code {locale, text, id}} — the display text and the machine-readable id beside it.
     * <p>
     * The {@code Value} of such a field is a human readable name whose wording is locale- and
     * vendor-dependent (OPC 10000-9 §5.2 gives {@code "Enabled"}/{@code "Disabled"} only as an example), so a
     * consumer deciding anything from the text alone is matching a string that a German session or a
     * different vendor will spell differently. {@code id} is the same state, machine-readable.
     * <p>
     * What {@code id} <em>is</em> depends on the field. Beneath a two-state field it is a {@code Boolean};
     * beneath a state machine's {@code CurrentState} it is a {@code NodeId} identifying the active state
     * node. Both are written by the generic converter rather than by type here, so neither is privileged and
     * a third kind would need no change.
     * <p>
     * {@code id} is omitted rather than written null when the server did not return it. It is Mandatory in
     * both cases, so its absence means the server did not honour the browse path — and an absent key says
     * that more honestly than a null, which would read as "the state is unknown".
     */
    private static void writeStateWithId(
            final @NotNull DataPointBuilder.ObjectBuilder<?> object,
            final @NotNull String key,
            final @Nullable Object state,
            final @Nullable Object id,
            final @NotNull EncodingContext ctx) {

        if (state == null && id == null) {
            object.putNull(key);
            return;
        }
        final var nested = object.startObject(key);
        if (state instanceof final LocalizedText text) {
            if (text.getLocale() != null) {
                nested.put("locale", text.getLocale());
            }
            if (text.getText() != null) {
                nested.put("text", text.getText());
            }
        } else if (state != null) {
            // Not a LocalizedText, which the type table says it should be. Publish what arrived rather than
            // dropping it, so a non-conforming server is visible instead of silently lossy.
            //
            // A StatusCode standing in for the value does NOT reach here -- that is conforming behaviour
            // (OPC 10000-4 §7.22.3), and it is nulled upstream and named under `unavailableFields` instead.
            // Writing it here would put a status object under `text`, where a display string is promised,
            // and `id` would silently vanish with it.
            OpcUaToJsonConverter.addValueToObject(nested, "text", state, ctx);
        }
        if (id != null) {
            OpcUaToJsonConverter.addValueToObject(nested, "id", id, ctx);
        }
        nested.endObject();
    }
}
