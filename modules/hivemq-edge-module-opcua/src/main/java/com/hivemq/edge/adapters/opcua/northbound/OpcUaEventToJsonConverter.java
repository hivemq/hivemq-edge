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
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaConditionType;
import java.util.List;
import org.eclipse.milo.opcua.stack.core.encoding.EncodingContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
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
            final @NotNull OpcuaConditionType conditionType,
            final @NotNull Variant[] values,
            final @NotNull DataPointBuilder<?> builder) {

        // The same list that built the select clause, so position i here is the field selected at i.
        final List<OpcuaConditionType.SelectedField> fields = conditionType.selectedFields();
        final var valueBuilder = builder.startObjectValue();
        for (int i = 0; i < fields.size(); i++) {
            final OpcuaConditionType.SelectedField field = fields.get(i);
            if (field.isStateId()) {
                // Written with its state below, not on its own.
                continue;
            }
            final Object value = valueAt(values, i);
            // A two-state field's Id is selected immediately after the state itself, so it is one position
            // ahead. Folding it in has to happen here rather than in the generic converter: the builder
            // streams, so the state's object is closed before the next value is seen, and a second pass
            // could not reopen it.
            final boolean hasStateId =
                    i + 1 < fields.size() && fields.get(i + 1).isStateId();
            if (hasStateId) {
                writeStateWithId(valueBuilder, field.publishedAs(), value, valueAt(values, i + 1), ctx);
            } else {
                OpcUaToJsonConverter.addValueToObject(valueBuilder, field.publishedAs(), value, ctx);
            }
        }
        valueBuilder.endObject();
    }

    /** A server may return fewer values than were selected; treat the tail as null rather than failing. */
    private static @Nullable Object valueAt(final @NotNull Variant[] values, final int index) {
        final Variant variant = index < values.length ? values[index] : null;
        return variant == null ? null : variant.getValue();
    }

    /**
     * Writes a two-state field as {@code {locale, text, id}} — the display text and the Boolean beside it.
     * <p>
     * The {@code Value} of such a field is a human readable name whose wording is locale- and
     * vendor-dependent (OPC 10000-9 §5.2 gives {@code "Enabled"}/{@code "Disabled"} only as an example), so a
     * consumer deciding anything from the text alone is matching a string that a German session or a
     * different vendor will spell differently. {@code id} is the same state as a Boolean.
     * <p>
     * {@code id} is omitted rather than written null when the server did not return it. It is Mandatory on
     * {@code TwoStateVariableType}, so its absence means the server did not honour the two-element browse
     * path — and an absent key says that more honestly than a null, which would read as "the state is
     * unknown".
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
            OpcUaToJsonConverter.addValueToObject(nested, "text", state, ctx);
        }
        if (id instanceof final Boolean bool) {
            nested.put("id", bool);
        }
        nested.endObject();
    }
}
