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
package com.hivemq.persistence.mappings;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.persistence.mappings.fieldmapping.FieldMapping;
import com.hivemq.persistence.mappings.fieldmapping.Instruction;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/// What makes a mapping "the same mapping", which decides whether an adapter is restarted.
///
/// **EDG-1087 M2.** `ProtocolAdapterManager.refreshUpdatedAdapters` skips an adapter whose new configuration
/// equals the running one. That guard reaches `ProtocolAdapterConfig.equals`, which compares the mapping lists,
/// which compare their field mappings. `FieldMapping` had no `equals`, so it fell back to identity -- and a
/// configuration is parsed afresh on every change, so the guard could never fire. Every adapter with a
/// southbound mapping was restarted on every configuration edit, measured live at ~23 s for an S7-1500, with a
/// method call in flight losing its answer.
///
/// **Both halves are pinned here together, because fixing one alone is worse than fixing neither.** Giving
/// `FieldMapping` value equality is what makes the guard work -- and the moment it works, a mapping flag that
/// `equals` ignores becomes a silent no-op: stored, compared by the persisted entity, and never applied because
/// no restart happened. The "unchanged" tests below would pass with the flags missing; the "changed" ones are
/// what hold them.
class MappingEqualityTest {

    private static @NotNull FieldMapping valueToValue() {
        return new FieldMapping(List.of(new Instruction("value", "value", null)));
    }

    private static @NotNull SouthboundMapping southbound(
            final boolean methodCalls, final boolean correlationData, final boolean responseTopic) {

        return new SouthboundMapping(
                "boiler", "plant/boiler1/call", valueToValue(), "{}", methodCalls, correlationData, responseTopic);
    }

    private static @NotNull NorthboundMapping northbound(final boolean writeCorrelationData) {
        return new NorthboundMapping(
                "boiler", "plant/boiler1/result", 1, true, true, false, List.of(), null, writeCorrelationData);
    }

    // ── A freshly parsed configuration must compare equal ───────────────────────────────────────

    @Test
    void twoSeparatelyBuiltFieldMappings_areEqual() {
        // The decisive one. Identity comparison here is what restarted every adapter on every edit: a
        // configuration is parsed afresh, so the instances always differ.
        assertThat(valueToValue())
                .as("a field mapping describing the same transform is the same field mapping")
                .isEqualTo(valueToValue());
        assertThat(valueToValue()).hasSameHashCodeAs(valueToValue());
    }

    @Test
    void twoSeparatelyBuiltSouthboundMappings_areEqual() {
        assertThat(southbound(true, true, true)).isEqualTo(southbound(true, true, true));
    }

    @Test
    void twoSeparatelyBuiltNorthboundMappings_areEqual() {
        assertThat(northbound(true)).isEqualTo(northbound(true));
    }

    // ── A changed flag must compare as changed ──────────────────────────────────────────────────
    //
    // These are the tests that matter, and the direction that is dangerous. A flag the comparison ignores is
    // stored in the configuration, compared by the persisted entity, and then never applied -- because the
    // adapter was judged unchanged and not restarted. Nothing reports the disagreement.

    @Test
    void aSouthboundMappingDifferingOnlyInMethodCalls_isNotEqual() {
        assertThat(southbound(true, false, false)).isNotEqualTo(southbound(false, false, false));
    }

    @Test
    void aSouthboundMappingDifferingOnlyInCorrelationData_isNotEqual() {
        assertThat(southbound(true, true, false)).isNotEqualTo(southbound(true, false, false));
    }

    @Test
    void aSouthboundMappingDifferingOnlyInResponseTopic_isNotEqual() {
        assertThat(southbound(true, false, true)).isNotEqualTo(southbound(true, false, false));
    }

    @Test
    void aNorthboundMappingDifferingOnlyInWriteCorrelationData_isNotEqual() {
        assertThat(northbound(true)).isNotEqualTo(northbound(false));
    }

    @Test
    void aDifferentTransform_isNotEqual() {
        final FieldMapping other = new FieldMapping(List.of(new Instruction("value", "setpoint", null)));

        assertThat(valueToValue()).isNotEqualTo(other);
    }

    @Test
    void aDifferentNumberOfInstructions_isNotEqual() {
        final FieldMapping two = new FieldMapping(
                List.of(new Instruction("value", "value", null), new Instruction("extra", "extra", null)));

        assertThat(valueToValue()).isNotEqualTo(two);
    }

    @Test
    void instructionOrder_isPartOfTheTransform() {
        // The generated script applies instructions in order, so two orderings are two transforms.
        final FieldMapping forward =
                new FieldMapping(List.of(new Instruction("a", "x", null), new Instruction("b", "y", null)));
        final FieldMapping reversed =
                new FieldMapping(List.of(new Instruction("b", "y", null), new Instruction("a", "x", null)));

        assertThat(forward).isNotEqualTo(reversed);
    }

    @Test
    void aNorthboundMappingDifferingInMetadata_hasADifferentHashCode() {
        // `includeMetadata` was compared by equals and left out of hashCode. Legal, but it degrades every
        // hash-based lookup and is the kind of drift that appears when the two are edited separately.
        final NorthboundMapping withMetadata =
                new NorthboundMapping("boiler", "plant/boiler1/result", 1, true, true, true, List.of(), null, false);
        final NorthboundMapping withoutMetadata =
                new NorthboundMapping("boiler", "plant/boiler1/result", 1, true, true, false, List.of(), null, false);

        assertThat(withMetadata).isNotEqualTo(withoutMetadata);
        assertThat(withMetadata.hashCode()).isNotEqualTo(withoutMetadata.hashCode());
    }
}
