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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BinaryNode;
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
 * <p>
 * A value is checked <b>independently of the carrier the adapter's {@code DataPoint} happens to use</b>: the SDK
 * offers two, and both are production paths. {@code DataPointFactory} yields the raw Java value, while the
 * {@code DataPointBuilder} path (OPC-UA, EtherIP CIP/ODVA) yields a {@code DataPointWithMetadata} whose
 * {@code getTagValue()} returns a Jackson {@link JsonNode}. Scalar JSON carriers are therefore unwrapped to their
 * logical Java value before the type and range are judged — see {@link #logicalValueOf}.
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
        // The declared type describes the VALUE, not the carrier it arrived in: unwrap a Jackson scalar node to the
        // Java value it stands for before judging either type or range.
        final Object logical = logicalValueOf(value);
        if (logical == null) {
            // The SDK declares tag values non-null; a sloppy adapter must not take the whole wrapper down.
            return "the adapter reported a null value";
        }
        return switch (scalar.type()) {
            case BOOLEAN -> logical instanceof Boolean ? null : wrongType(logical, scalar);
            case STRING -> logical instanceof CharSequence ? null : wrongType(logical, scalar);
            case BINARY -> logical instanceof byte[] ? null : wrongType(logical, scalar);
            case LONG -> integralViolation(logical, scalar, false);
            case ULONG -> integralViolation(logical, scalar, true);
            case DOUBLE ->
                logical instanceof final Number number
                        ? rangeViolation(number.doubleValue(), scalar)
                        : wrongType(logical, scalar);
            // Temporal types have protocol-specific carriers; no conformance rule is enforced for them here.
            case INSTANT, LOCAL_DATE, LOCAL_TIME, LOCAL_DATE_TIME, DURATION -> null;
        };
    }

    /**
     * Unwrap a Jackson scalar carrier to the Java value it represents; any other object is returned unchanged.
     * <p>
     * Numeric nodes go through {@link JsonNode#numberValue()}, which <b>preserves the integral/floating
     * distinction</b> — {@code ShortNode}/{@code IntNode}/{@code LongNode}/{@code BigIntegerNode} become
     * {@code Short}/{@code Integer}/{@code Long}/{@code BigInteger} and
     * {@code FloatNode}/{@code DoubleNode}/{@code DecimalNode} become {@code Float}/{@code Double}/
     * {@code BigDecimal} — so a fractional value still fails a {@code LONG} declaration exactly as the raw Java
     * carrier does, and the exact-{@link BigInteger} integral comparison keeps operating on integral carriers.
     * <p>
     * A JSON {@code null} (or a missing node) is the same statement as a null value. Object and array nodes are
     * deliberately NOT unwrapped: against a range-constrained scalar declaration they are a genuine type violation,
     * and reporting the node type is the honest diagnostic.
     */
    private static @Nullable Object logicalValueOf(final @Nullable Object value) {
        if (!(value instanceof final JsonNode node)) {
            return value;
        }
        if (node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isNumber()) {
            return node.numberValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        if (node.isTextual()) {
            return node.textValue();
        }
        if (node instanceof final BinaryNode binary) {
            // BinaryNode narrows binaryValue() to an unchecked override; JsonNode's declares IOException.
            return binary.binaryValue();
        }
        return node;
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
            return integralRangeViolation(bigInteger, scalar);
        }
        final long longValue = ((Number) value).longValue();
        if (unsigned && longValue < 0) {
            return "value " + value + " is negative but the declared type is ULONG";
        }
        return integralRangeViolation(BigInteger.valueOf(longValue), scalar);
    }

    /**
     * Exact range check for INTEGRAL values (LONG, ULONG). Value and bounds are compared as {@link BigInteger} so
     * adjacent 64-bit integers above 2^53 stay distinct — a {@code double} collapses them onto the same value and
     * would mis-report a violation at the boundary. Semantics match {@link #rangeViolation}: inclusive minimum,
     * exclusive maximum.
     */
    private static @Nullable String integralRangeViolation(
            final @NotNull BigInteger value, final @NotNull ScalarSchema scalar) {
        if (scalar.minimum() != null && value.compareTo(toBigInteger(scalar.minimum())) < 0) {
            return "value " + value + " is below the declared minimum " + scalar.minimum();
        }
        if (scalar.maximum() != null && value.compareTo(toBigInteger(scalar.maximum())) >= 0) {
            return "value " + value + " is not below the declared (exclusive) maximum " + scalar.maximum();
        }
        return null;
    }

    /**
     * Convert an integral bound to an exact {@link BigInteger}. Bounds on integral scalars are themselves integral;
     * {@code longValue()} is exact for the fixed-width carriers and {@code BigInteger}/{@code BigDecimal} are taken
     * verbatim — no {@code double} round-trip, so large boundaries survive intact.
     */
    private static @NotNull BigInteger toBigInteger(final @NotNull Number bound) {
        if (bound instanceof final BigInteger bigInteger) {
            return bigInteger;
        }
        if (bound instanceof final java.math.BigDecimal bigDecimal) {
            return bigDecimal.toBigInteger();
        }
        return BigInteger.valueOf(bound.longValue());
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
