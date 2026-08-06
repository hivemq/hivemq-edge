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
package com.hivemq.edge.adapters.opcua.config.tag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The condition type table is what the northbound shape rests on: it decides both the read schema and the
 * event filter's select clause. These check the properties the rest of the feature assumes of it.
 */
class OpcuaConditionTypeTest {

    @Test
    void everyTypeCarriesTheBaseEventFields() {
        // EventId in particular: without it a transition cannot be acknowledged, so no type may omit it.
        assertThat(OpcuaConditionType.values()).allSatisfy(type -> assertThat(type.allFields())
                .as("%s must carry the base event fields", type.browseName())
                .containsAll(OpcuaConditionType.BASE_EVENT_FIELDS));
    }

    @Test
    void aSubtypeCarriesEverythingItsParentDoes() {
        // The hierarchy is strictly additive; a subtype narrowing its parent would break the claim that
        // declaring a supertype is a safe widening.
        assertThat(OpcuaConditionType.values())
                .allSatisfy(type -> type.parent().ifPresent(parent -> assertThat(type.allFields())
                        .as("%s must carry everything %s does", type.browseName(), parent.browseName())
                        .containsAll(parent.allFields())));
    }

    @Test
    void fieldsAreNotRepeated() {
        // A subtype may re-declare a field its parent already has; the published shape must still list it once.
        assertThat(OpcuaConditionType.values()).allSatisfy(type -> assertThat(type.allFields())
                .as("%s must not repeat a field", type.browseName())
                .doesNotHaveDuplicates());
    }

    @Test
    void aLevelAlarmCarriesItsLimits() {
        // The case that motivated declaring the type at all: a fixed field list drops these silently, and for
        // a level alarm the limits are the interesting part.
        final OpcuaConditionType levelAlarm =
                OpcuaConditionType.fromBrowseName("ExclusiveLevelAlarmType").orElseThrow();

        assertThat(levelAlarm.allFields())
                .contains("HighLimit", "LowLimit", "HighHighLimit", "LowLowLimit")
                .contains("ActiveState", "AckedState")
                .contains("EventId");
    }

    @Test
    void aPlainAlarmDoesNotCarryLimits() {
        final OpcuaConditionType alarm =
                OpcuaConditionType.fromBrowseName("AlarmConditionType").orElseThrow();

        assertThat(alarm.allFields()).doesNotContain("HighLimit", "LowLimit");
    }

    @Test
    void anExclusiveLimitAlarmSaysWhichLimitIsViolated() {
        // EDG-835: LimitState is the one member OPC 10000-9 Table 96 adds, and it is Mandatory. Its absence
        // left the type publishing that an alarm is active and nothing about which threshold tripped, while
        // NonExclusiveLimitAlarmType carried all four limit states -- two ways to model the same alarm, only
        // one of them usable.
        final OpcuaConditionType exclusive =
                OpcuaConditionType.fromBrowseName("ExclusiveLimitAlarmType").orElseThrow();

        assertThat(exclusive.allFields()).contains("LimitState");
    }

    @Test
    void aStateMachineIsSelectedThroughItsCurrentState() {
        // A state machine is an Object node, which has no Value attribute at all (OPC 10000-4 Table 129), so
        // selecting it by name would ask for something that cannot be returned. The readable state is one
        // level down, and its node id one level below that.
        final OpcuaConditionType exclusive =
                OpcuaConditionType.fromBrowseName("ExclusiveLimitAlarmType").orElseThrow();

        assertThat(exclusive.selectedFields())
                .as("LimitState must never be selected as a bare one-element path")
                .noneMatch(f -> f.path().equals(List.of("LimitState")));

        assertThat(exclusive.selectedFields())
                .filteredOn(f -> f.publishedAs().equals("LimitState"))
                .extracting(OpcuaConditionType.SelectedField::path)
                .containsExactly(List.of("LimitState", "CurrentState"), List.of("LimitState", "CurrentState", "Id"));
    }

    @Test
    void anIdIsFoldedIntoTheFieldItBelongsTo() {
        // Both kinds of Id -- a two-state field's Boolean and a state machine's node id -- are published
        // inside the parent's object rather than beside it, so they repeat the parent's key and are marked
        // ID rather than VALUE.
        final OpcuaConditionType exclusive =
                OpcuaConditionType.fromBrowseName("ExclusiveLimitAlarmType").orElseThrow();

        // An Id entry publishes under the key of the entry before it, which is what makes the converter's
        // positional fold correct. If one ever introduced a key of its own, that fold would write it into
        // the wrong object.
        final List<OpcuaConditionType.SelectedField> fields = exclusive.selectedFields();
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).isStateId()) {
                assertThat(i).as("an Id entry is never first").isPositive();
                assertThat(fields.get(i).publishedAs())
                        .as("the Id at %d publishes under the preceding entry's key", i)
                        .isEqualTo(fields.get(i - 1).publishedAs());
            }
        }

        assertThat(exclusive.selectedFields())
                .filteredOn(f -> f.path().equals(List.of("LimitState", "CurrentState", "Id")))
                .singleElement()
                .satisfies(f -> {
                    assertThat(f.role()).isEqualTo(OpcuaConditionType.FieldRole.ID);
                    assertThat(f.publishedAs()).isEqualTo("LimitState");
                });

        assertThat(exclusive.selectedFields())
                .filteredOn(f -> f.path().equals(List.of("ActiveState", "Id")))
                .singleElement()
                .satisfies(f -> assertThat(f.role()).isEqualTo(OpcuaConditionType.FieldRole.ID));
    }

    @Test
    void everyAlarmCarriesItsInputNode() {
        // Mandatory on AlarmConditionType (OPC 10000-9 Table 40), and the field that answers what the alarm
        // is watching -- so it is on every alarm we publish, not just some.
        assertThat(OpcuaConditionType.values())
                .filteredOn(type -> OpcuaConditionType.fromBrowseName("AlarmConditionType")
                        .orElseThrow()
                        .isSatisfiedBy(type))
                .allSatisfy(type -> assertThat(type.allFields())
                        .as("%s is an alarm, so it must carry InputNode", type.browseName())
                        .contains("InputNode"));
    }

    @Test
    void nodeReferencingFieldsKeepTheirNodeSuffix() {
        // These properties hold a NodeId pointing at a variable, never the variable's value. Dropping the
        // suffix would name a field the specification does not define and promise a number where a reference
        // is sent -- a mistake invisible until a consumer reads it. OPC 10000-9 Tables 101, 102 and 112.
        final OpcuaConditionType deviation =
                OpcuaConditionType.fromBrowseName("ExclusiveDeviationAlarmType").orElseThrow();
        assertThat(deviation.allFields())
                .contains("SetpointNode", "BaseSetpointNode")
                .doesNotContain("Setpoint", "BaseSetpoint");

        final OpcuaConditionType nonExclusive = OpcuaConditionType.fromBrowseName("NonExclusiveDeviationAlarmType")
                .orElseThrow();
        assertThat(nonExclusive.allFields())
                .contains("SetpointNode", "BaseSetpointNode")
                .doesNotContain("Setpoint", "BaseSetpoint");

        final OpcuaConditionType discrepancy =
                OpcuaConditionType.fromBrowseName("DiscrepancyAlarmType").orElseThrow();
        assertThat(discrepancy.allFields()).contains("TargetValueNode").doesNotContain("TargetValue");
    }

    @Test
    void aSupertypeIsSatisfiedByItsSubtype() {
        final OpcuaConditionType alarm =
                OpcuaConditionType.fromBrowseName("AlarmConditionType").orElseThrow();
        final OpcuaConditionType levelAlarm =
                OpcuaConditionType.fromBrowseName("ExclusiveLevelAlarmType").orElseThrow();

        // Declaring the supertype is a valid widening: every declared field exists on the device.
        assertThat(alarm.isSatisfiedBy(levelAlarm)).isTrue();
        assertThat(alarm.isSatisfiedBy(alarm)).isTrue();

        // The reverse is not: a tag promising limits cannot be served by a device without them.
        assertThat(levelAlarm.isSatisfiedBy(alarm)).isFalse();
    }

    @Test
    void theHierarchyIsRootedAtConditionType() {
        assertThat(OpcuaConditionType.values()).allSatisfy(type -> {
            final List<String> lineage = new java.util.ArrayList<>();
            OpcuaConditionType current = type;
            while (current != null) {
                lineage.add(current.browseName());
                current = current.parent().orElse(null);
            }
            assertThat(lineage)
                    .as("%s must derive from ConditionType", type.browseName())
                    .endsWith("ConditionType");
        });
    }

    @Test
    void unknownTypeNamesAreRejected() {
        assertThat(OpcuaConditionType.fromBrowseName("NoSuchAlarmType")).isEmpty();
        assertThat(OpcuaConditionType.fromBrowseName(null)).isEmpty();
    }
}
