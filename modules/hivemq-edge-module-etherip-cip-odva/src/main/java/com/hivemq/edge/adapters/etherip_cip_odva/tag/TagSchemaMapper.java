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
package com.hivemq.edge.adapters.etherip_cip_odva.tag;

import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.schema.SchemaBuilder;
import com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType;
import com.hivemq.edge.adapters.etherip_cip_odva.config.CipNumericRange;
import org.jetbrains.annotations.NotNull;

public final class TagSchemaMapper {

    private TagSchemaMapper() {}

    /**
     * Builds a scalar schema whose readable/writable annotations reflect the tag's configured direction
     * ({@code CipReadWrite}), rather than assuming a fixed direction.
     */
    public static @NotNull Schema buildScalarSchema(
            final @NotNull CipDataType type, final boolean readable, final boolean writable) {
        final SchemaBuilder builder = new SchemaBuilder();
        applyScalarType(builder, type);
        return builder.readable(readable).writable(writable).build();
    }

    private static void applyScalarType(final @NotNull SchemaBuilder builder, final @NotNull CipDataType type) {
        switch (type) {
            case BOOL -> builder.scalar(ScalarType.BOOLEAN);
            case SINT, INT, DINT, LINT -> applyIntegerType(builder, ScalarType.LONG, type);
            case USINT, UINT, UDINT, ULINT -> applyIntegerType(builder, ScalarType.ULONG, type);
            case REAL, LREAL -> applyFloatType(builder, type);
            case SSTRING, STRING -> builder.scalar(ScalarType.STRING);
            case COMPOSITE ->
                throw new IllegalArgumentException(
                        "COMPOSITE is not a scalar CipDataType; build an ObjectSchema from sibling scalars instead.");
        }
    }

    private static void applyIntegerType(
            final @NotNull SchemaBuilder builder,
            final @NotNull ScalarType scalarType,
            final @NotNull CipDataType type) {
        final CipNumericRange.IntegerRange range = CipNumericRange.integerRange(type);
        builder.scalar(scalarType).minimum(range.minimum());
        // ULINT's maximum (2^64 - 1) is not representable as a long, so it has no upper bound in the schema.
        if (range.maximum() != null) {
            builder.maximum(range.maximum());
        }
    }

    private static void applyFloatType(final @NotNull SchemaBuilder builder, final @NotNull CipDataType type) {
        final CipNumericRange.FloatRange range = CipNumericRange.floatRange(type);
        builder.scalar(ScalarType.DOUBLE).minimum(range.minimum()).maximum(range.maximum());
    }
}
