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
