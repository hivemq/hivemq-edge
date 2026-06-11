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
import org.jetbrains.annotations.NotNull;

public final class TagSchemaMapper {

    private TagSchemaMapper() {}

    public static @NotNull Schema buildScalarSchema(final @NotNull CipDataType type) {
        final SchemaBuilder builder = new SchemaBuilder();
        applyScalarType(builder, type);
        return builder.readable().writable().build();
    }

    public static @NotNull Schema buildReadOnlyScalarSchema(final @NotNull CipDataType type) {
        final SchemaBuilder builder = new SchemaBuilder();
        applyScalarType(builder, type);
        return builder.readable().build();
    }

    private static void applyScalarType(final @NotNull SchemaBuilder builder, final @NotNull CipDataType type) {
        switch (type) {
            case BOOL -> builder.scalar(ScalarType.BOOLEAN);
            case SINT -> builder.scalar(ScalarType.LONG).minimum(-128L).maximum(127L);
            case USINT -> builder.scalar(ScalarType.ULONG).minimum(0L).maximum(255L);
            case INT -> builder.scalar(ScalarType.LONG).minimum(-32_768L).maximum(32_767L);
            case UINT -> builder.scalar(ScalarType.ULONG).minimum(0L).maximum(65_535L);
            case DINT ->
                builder.scalar(ScalarType.LONG).minimum(-2_147_483_648L).maximum(2_147_483_647L);
            case UDINT -> builder.scalar(ScalarType.ULONG).minimum(0L).maximum(4_294_967_295L);
            case LINT -> builder.scalar(ScalarType.LONG).minimum(Long.MIN_VALUE).maximum(Long.MAX_VALUE);
            // Java `long` cannot represent 2^64 - 1, so no upper bound is set for ULINT.
            case ULINT -> builder.scalar(ScalarType.ULONG).minimum(0L);
            case REAL ->
                builder.scalar(ScalarType.DOUBLE).minimum(-3.4028235e38d).maximum(3.4028235e38d);
            case LREAL ->
                builder.scalar(ScalarType.DOUBLE)
                        .minimum(-1.7976931348623157e308d)
                        .maximum(1.7976931348623157e308d);
            case SSTRING, STRING -> builder.scalar(ScalarType.STRING);
            case COMPOSITE ->
                throw new IllegalArgumentException(
                        "COMPOSITE is not a scalar CipDataType; build an ObjectSchema from sibling scalars instead.");
        }
    }
}
