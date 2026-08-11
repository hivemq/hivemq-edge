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
package com.hivemq.edge.adapters.opcua.condition;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.schema.SchemaJsonRepresentation;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaConditionType;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Whether an operator can see the shelving state they are allowed to command.
 * <p>
 * Review-02 finding 11. Edge exposes {@code UNSHELVE}, {@code ONE_SHOT_SHELVE} and {@code TIMED_SHELVE}
 * southbound, and published nothing northbound that says which of those three states an alarm is in. The
 * field table left {@code ShelvingState} out on the grounds that shelving was "a separate concern" — which it
 * is not, from the write side: the one thing a command needs is a way to confirm it took effect.
 * <p>
 * {@code SuppressedOrShelved} is not a substitute, and that is the sharp part of the finding. It is a single
 * Boolean, so it says neither which of suppressed-or-shelved it means, nor — when it means shelved — whether
 * the alarm is {@code TimedShelved} or {@code OneShotShelved}. Three commands, one bit, and the bit does not
 * distinguish any of them.
 */
class ShelvingStateIsObservableTest {

    @Test
    void everyShelvingCommandHasAPublishedStateToConfirmIt() {
        // The finding, stated as the property that closes it.
        assertThat(OpcuaConditionType.ALARM_CONDITION.allFields())
                .as("an alarm that can be shelved has to say that it is")
                .contains("ShelvingState");
    }

    @Test
    void itIsReadAsAStateMachineRatherThanAValue() {
        // A state machine is an Object node, and an Object has no Value attribute at all -- selecting the
        // field itself returns nothing. What is readable is the FiniteStateMachineType variable one level
        // down, exactly as for LimitState.
        final List<String> paths = OpcuaConditionType.ALARM_CONDITION.selectedFields().stream()
                .filter(field -> "ShelvingState".equals(field.publishedAs()))
                .map(field -> String.join("/", field.path()))
                .toList();

        assertThat(paths)
                .as("the current state, and the machine-readable id beneath it")
                .containsExactly("ShelvingState/CurrentState", "ShelvingState/CurrentState/Id");
    }

    @Test
    void andItsIdIsWhatAConsumerBranchesOn() {
        // CurrentState is a LocalizedText -- what the server calls the state, in the session's locale. A rule
        // written against "OneShotShelved" breaks on a German-language server. The Id is a NodeId naming the
        // state node, and is the same everywhere.
        assertThat(OpcuaConditionType.ALARM_CONDITION.selectedFields())
                .filteredOn(field -> "ShelvingState".equals(field.publishedAs()))
                .anySatisfy(field -> assertThat(field.isStateId()).isTrue());
    }

    @Test
    void aTimedShelveSaysWhenItEnds() {
        // UnshelveTime, ShelvedStateMachineType's own property (OPC 10000-9 §5.8.17 Table 73) and Mandatory
        // there. TIMED_SHELVE takes a duration, so without this a consumer can see that an alarm is
        // TimedShelved but not when it returns -- the state is observable and its one parameter is not.
        assertThat(OpcuaConditionType.ALARM_CONDITION.allFields()).contains("UnshelveTime");

        final List<String> path = OpcuaConditionType.ALARM_CONDITION.selectedFields().stream()
                .filter(field -> "UnshelveTime".equals(field.publishedAs()))
                .map(field -> String.join("/", field.path()))
                .toList();

        assertThat(path)
                .as("a property of the machine, not of the alarm, so the path is two elements")
                .containsExactly("ShelvingState/UnshelveTime");
    }

    @Test
    void bothAppearInTheSchemaWithTheRightShapes() {
        // The user-visible half: a consumer generating a model from the read schema gets a state machine
        // object for the state and a number for the duration.
        final ObjectNode properties = (ObjectNode) SchemaJsonRepresentation.INSTANCE
                .toJsonSchemaDocument(ConditionSchemas.readSchema(OpcuaConditionType.ALARM_CONDITION))
                .get("properties");

        assertThat(properties.has("ShelvingState")).isTrue();
        assertThat(properties.get("ShelvingState").toString())
                .as("the state machine shape: display text plus a machine-readable id")
                .contains("text")
                .contains("id");
        assertThat(properties.has("UnshelveTime")).isTrue();
    }

    @Test
    void aSubtypeOfAlarmConditionInheritsBoth() {
        // Placed on AlarmConditionType rather than on a leaf, because that is where the specification puts
        // it -- so every alarm that can be shelved carries it, not only the one type tested above.
        assertThat(OpcuaConditionType.EXCLUSIVE_LEVEL_ALARM.allFields()).contains("ShelvingState", "UnshelveTime");
    }

    @Test
    void aPlainConditionDoesNotPretendToHaveOne() {
        // Shelving is an alarm concern. ConditionType and AcknowledgeableConditionType have no shelving
        // state machine, and advertising one would be the same defect as review-02 finding 10 in miniature.
        assertThat(OpcuaConditionType.CONDITION.allFields()).doesNotContain("ShelvingState", "UnshelveTime");
        assertThat(OpcuaConditionType.ACKNOWLEDGEABLE_CONDITION.allFields())
                .doesNotContain("ShelvingState", "UnshelveTime");
    }

    @Test
    void theBaseEventFieldsAreStillTheFirstThingSelected() {
        // Adding fields to AlarmConditionType must not disturb the prefix the handler reads EventId and
        // EventType out of by cached index. They are added to the type's own contribution, which is appended
        // after the base fields -- this is what says so rather than assuming it.
        final List<String> selected = OpcuaConditionType.ALARM_CONDITION.selectedFields().stream()
                .map(OpcuaConditionType.SelectedField::publishedAs)
                .toList();

        assertThat(selected.subList(0, OpcuaConditionType.BASE_EVENT_FIELDS.size()))
                .isEqualTo(OpcuaConditionType.BASE_EVENT_FIELDS);
    }
}
