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

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.adapter.sdk.api.schema.AnySchema;
import com.hivemq.adapter.sdk.api.schema.ScalarSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import java.math.BigInteger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * The declared-schema conformance rules (EDG-824 #6). Enforcement applies ONLY to range-constrained
 * scalar schemas — an unambiguous value declaration like {@code DOUBLE[0,100)} — with the SDK range contract:
 * inclusive {@code minimum}, exclusive {@code maximum}. Unconstrained scalars and structured schemas pass through:
 * for most adapter types the tag's schema slot carries the node CONFIG-FORM schema, which says nothing about the
 * value shape.
 */
class SchemaConformanceTest {

    private static @NotNull ScalarSchema doubleRange(final @Nullable Number minimum, final @Nullable Number maximum) {
        return new ScalarSchema(ScalarType.DOUBLE, minimum, maximum, null, null, false, true, false);
    }

    private static @NotNull ScalarSchema longRange(final @Nullable Number minimum, final @Nullable Number maximum) {
        return new ScalarSchema(ScalarType.LONG, minimum, maximum, null, null, false, true, false);
    }

    private static @NotNull ScalarSchema unconstrained(final @NotNull ScalarType type) {
        return new ScalarSchema(type, null, null, null, null, false, true, false);
    }

    @Test
    void conformingValues_pass() {
        assertThat(SchemaConformance.violationOf(21.5, doubleRange(0, 100))).isNull();
        assertThat(SchemaConformance.violationOf(0, doubleRange(0, 100))).isNull(); // minimum is inclusive
        assertThat(SchemaConformance.violationOf(42L, longRange(0, 100))).isNull();
    }

    @Test
    void outOfRange_isAViolation() {
        assertThat(SchemaConformance.violationOf(-0.1, doubleRange(0, 100))).contains("below the declared minimum");
        assertThat(SchemaConformance.violationOf(100.0, doubleRange(0, 100)))
                .contains("exclusive"); // maximum is exclusive per the SDK contract
        assertThat(SchemaConformance.violationOf(250L, doubleRange(0, 100))).isNotNull();
    }

    @Test
    void wrongType_againstAConstrainedScalar_isAViolation() {
        assertThat(SchemaConformance.violationOf("garbage", doubleRange(0, 100)))
                .contains("does not conform to the declared scalar type DOUBLE");
        assertThat(SchemaConformance.violationOf(1.5, longRange(0, 100))).isNotNull();
        assertThat(SchemaConformance.violationOf(null, doubleRange(0, 100))).contains("null");
    }

    @Test
    void nanWithARangeConstraint_isAViolation() {
        assertThat(SchemaConformance.violationOf(Double.NaN, doubleRange(0, 100)))
                .contains("NaN");
        // unconstrained DOUBLE still carries NaN/Infinity byte-identically — the verified-robust guarantee
        assertThat(SchemaConformance.violationOf(Double.NaN, unconstrained(ScalarType.DOUBLE)))
                .isNull();
        assertThat(SchemaConformance.violationOf(Double.POSITIVE_INFINITY, unconstrained(ScalarType.DOUBLE)))
                .isNull();
    }

    @Test
    void oversizedIntegerCarriers_cannotEscapeAConstrainedRange() {
        assertThat(SchemaConformance.violationOf(BigInteger.TWO.pow(70), longRange(0, 100)))
                .contains("not representable");
        assertThat(SchemaConformance.violationOf(
                        BigInteger.valueOf(-1),
                        new ScalarSchema(ScalarType.ULONG, 0, null, null, null, false, true, false)))
                .contains("negative");
    }

    // The tag's schema slot is populated from the adapter type's nodeDefinitionSchema — for most types a CONFIG
    // FORM, not a value shape. Anything without a declared range is projection-only.
    @Test
    void unconstrainedScalarsAndStructuredSchemas_passThrough() {
        assertThat(SchemaConformance.violationOf(21.5, unconstrained(ScalarType.STRING)))
                .isNull(); // the chaos/file/http adapters' case — a Double against a STRING form schema
        assertThat(SchemaConformance.violationOf("text", unconstrained(ScalarType.DOUBLE)))
                .isNull();
        assertThat(SchemaConformance.violationOf(12345, unconstrained(ScalarType.DOUBLE)))
                .isNull();
        assertThat(SchemaConformance.violationOf("anything", new AnySchema(null, null, false, true, false)))
                .isNull();
        assertThat(SchemaConformance.violationOf(BigInteger.TWO.pow(64), unconstrained(ScalarType.ULONG)))
                .isNull();
    }
}
