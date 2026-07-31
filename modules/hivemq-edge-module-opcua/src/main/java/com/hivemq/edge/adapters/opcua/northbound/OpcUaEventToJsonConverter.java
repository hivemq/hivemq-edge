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
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.jetbrains.annotations.NotNull;

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

        // The same field list that built the select clause, so position i here is the field selected at i.
        final List<String> fields = conditionType.allFields();
        final var valueBuilder = builder.startObjectValue();
        for (int i = 0; i < fields.size(); i++) {
            // A server may return fewer values than were selected; treat the tail as null rather than failing.
            final Variant variant = i < values.length ? values[i] : null;
            final Object value = variant == null ? null : variant.getValue();
            OpcUaToJsonConverter.addValueToObject(valueBuilder, fields.get(i), value, ctx);
        }
        valueBuilder.endObject();
    }
}
