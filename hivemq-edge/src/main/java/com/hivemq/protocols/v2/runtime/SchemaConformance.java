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
package com.hivemq.protocols.v2.runtime;

import com.hivemq.adapter.sdk.api.schema.ScalarSchema;
import com.hivemq.adapter.sdk.api.schema.Schema;
import java.math.BigInteger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Enforcement of a tag's declared value schema at the northbound routing point (EDG-824 #6): the
 * same {@link Schema} REST projects for the tag is checked against every value the adapter reports, so
 * <i>declared</i> and <i>enforced</i> are the one schema. Scalar type and the SDK's range contract (inclusive
 * {@code minimum}, exclusive {@code maximum}) are enforced for scalar schemas; structured and unconstrained schemas
 * pass through — projection-only, as before.
 */
public final class SchemaConformance {

    private SchemaConformance() {}

    /**
     * Check a reported value against the tag's declared schema.
     *
     * @param value  the raw tag value as reported by the adapter.
     * @param schema the tag's declared schema — the one REST projects.
     * @return a human-readable description of the violation, or {@code null} when the value conforms.
     */
    public static @Nullable String violationOf(final @Nullable Object value, final @NotNull Schema schema) {
        if (!(schema instanceof final ScalarSchema scalar)) {
            // Structured (object/array) and any-schemas carry no scalar constraints to enforce here.
            return null;
        }
        if (scalar.minimum() == null && scalar.maximum() == null) {
            // Only a RANGE-CONSTRAINED scalar is unambiguously a value declaration. The tag's schema slot is
            // populated from the adapter type's nodeDefinitionSchema, which for most adapters describes the node
            // CONFIG FORM, not the value shape — enforcing bare type there refuses legitimate values (verified by
            // the v2 end-to-end suite). A declared range is the adapter saying "values look like this" — that is
            // enforced, type included.
            return null;
        }
        if (value == null) {
            // The SDK declares tag values non-null; a sloppy adapter must not take the whole wrapper down.
            return "the adapter reported a null value";
        }
        return switch (scalar.type()) {
            case BOOLEAN -> value instanceof Boolean ? null : wrongType(value, scalar);
            case STRING -> value instanceof CharSequence ? null : wrongType(value, scalar);
            case BINARY -> value instanceof byte[] ? null : wrongType(value, scalar);
            case LONG -> integralViolation(value, scalar, false);
            case ULONG -> integralViolation(value, scalar, true);
            case DOUBLE ->
                value instanceof final Number number
                        ? rangeViolation(number.doubleValue(), scalar)
                        : wrongType(value, scalar);
            // Temporal types have protocol-specific carriers; no conformance rule is enforced for them here.
            case INSTANT, LOCAL_DATE, LOCAL_TIME, LOCAL_DATE_TIME, DURATION -> null;
        };
    }

    private static @Nullable String integralViolation(
            final @NotNull Object value, final @NotNull ScalarSchema scalar, final boolean unsigned) {
        if (!(value instanceof Long
                || value instanceof Integer
                || value instanceof Short
                || value instanceof Byte
                || value instanceof BigInteger)) {
            return wrongType(value, scalar);
        }
        if (value instanceof final BigInteger bigInteger) {
            if (unsigned && bigInteger.signum() < 0) {
                return "value " + value + " is negative but the declared type is ULONG";
            }
            // Representability first: LONG carries at most 63 magnitude bits (+ sign), ULONG at most 64 unsigned
            // bits. A value beyond that can never conform — without this check the biggest (most out-of-range)
            // values would be the ones that slip through.
            if (!unsigned && bigInteger.bitLength() > 63) {
                return "value " + value + " is not representable as a 64-bit LONG";
            }
            if (unsigned && bigInteger.bitLength() > 64) {
                return "value " + value + " is not representable as a 64-bit ULONG";
            }
            return rangeViolation(bigInteger.doubleValue(), scalar);
        }
        final long longValue = ((Number) value).longValue();
        if (unsigned && longValue < 0) {
            return "value " + value + " is negative but the declared type is ULONG";
        }
        return rangeViolation(longValue, scalar);
    }

    private static @Nullable String rangeViolation(final double value, final @NotNull ScalarSchema scalar) {
        final boolean constrained = scalar.minimum() != null || scalar.maximum() != null;
        if (!constrained) {
            return null;
        }
        if (Double.isNaN(value)) {
            return "value NaN is outside the declared range";
        }
        if (scalar.minimum() != null && value < scalar.minimum().doubleValue()) {
            return "value " + value + " is below the declared minimum " + scalar.minimum();
        }
        if (scalar.maximum() != null && value >= scalar.maximum().doubleValue()) {
            return "value " + value + " is not below the declared (exclusive) maximum " + scalar.maximum();
        }
        return null;
    }

    private static @NotNull String wrongType(final @NotNull Object value, final @NotNull ScalarSchema scalar) {
        return "value of type " + value.getClass().getSimpleName() + " does not conform to the declared scalar type "
                + scalar.type();
    }
}
