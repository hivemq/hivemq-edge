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
package com.hivemq.edge.adapters.etherip_cip_odva.config;

import java.util.EnumMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The value range of a numeric {@link CipDataType}. This is the single source of truth for the numeric bounds
 * of each CIP type: it is used both to advertise {@code minimum}/{@code maximum} in the tag schema and to
 * reject out-of-range values before they are encoded (a wider Java {@code long}/{@code double} narrowed to the
 * CIP width would otherwise wrap silently).
 * <p>
 * Integer types carry a {@code long} range; a {@code null} {@link IntegerRange#maximum()} means the upper
 * bound is not representable as a signed {@code long} (only {@code ULINT}, whose maximum is 2^64 - 1).
 * Floating-point types carry a {@code double} range. Non-numeric types ({@code BOOL}, strings,
 * {@code COMPOSITE}) have no range.
 */
public final class CipNumericRange {

    /** An integer range, both bounds inclusive. {@code maximum} is {@code null} for {@code ULINT}. */
    public record IntegerRange(long minimum, @Nullable Long maximum) {}

    /** A floating-point range, both bounds inclusive. */
    public record FloatRange(double minimum, double maximum) {}

    private static final @NotNull Map<CipDataType, IntegerRange> INTEGER_RANGES = new EnumMap<>(CipDataType.class);
    private static final @NotNull Map<CipDataType, FloatRange> FLOAT_RANGES = new EnumMap<>(CipDataType.class);

    static {
        INTEGER_RANGES.put(CipDataType.SINT, new IntegerRange(-128L, 127L));
        INTEGER_RANGES.put(CipDataType.USINT, new IntegerRange(0L, 255L));
        INTEGER_RANGES.put(CipDataType.INT, new IntegerRange(-32_768L, 32_767L));
        INTEGER_RANGES.put(CipDataType.UINT, new IntegerRange(0L, 65_535L));
        INTEGER_RANGES.put(CipDataType.DINT, new IntegerRange(-2_147_483_648L, 2_147_483_647L));
        INTEGER_RANGES.put(CipDataType.UDINT, new IntegerRange(0L, 4_294_967_295L));
        INTEGER_RANGES.put(CipDataType.LINT, new IntegerRange(Long.MIN_VALUE, Long.MAX_VALUE));
        // ULINT's maximum is 2^64 - 1, which does not fit in a signed long; a null upper bound means
        // "any non-negative long", which is the widest range a JSON integer (parsed as a long) can express.
        INTEGER_RANGES.put(CipDataType.ULINT, new IntegerRange(0L, null));

        FLOAT_RANGES.put(CipDataType.REAL, new FloatRange(-3.4028235e38d, 3.4028235e38d));
        FLOAT_RANGES.put(CipDataType.LREAL, new FloatRange(-1.7976931348623157e308d, 1.7976931348623157e308d));
    }

    private CipNumericRange() {}

    /**
     * The integer range of {@code type}.
     *
     * @throws IllegalArgumentException if {@code type} is not an integer type
     */
    public static @NotNull IntegerRange integerRange(final @NotNull CipDataType type) {
        final IntegerRange range = INTEGER_RANGES.get(type);
        if (range == null) {
            throw new IllegalArgumentException(type + " is not an integer CipDataType.");
        }
        return range;
    }

    /**
     * The floating-point range of {@code type}.
     *
     * @throws IllegalArgumentException if {@code type} is not a floating-point type
     */
    public static @NotNull FloatRange floatRange(final @NotNull CipDataType type) {
        final FloatRange range = FLOAT_RANGES.get(type);
        if (range == null) {
            throw new IllegalArgumentException(type + " is not a floating-point CipDataType.");
        }
        return range;
    }
}
