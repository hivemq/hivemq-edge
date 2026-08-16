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
package com.hivemq.edge.adapters.opcua.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.schema.SchemaJsonRepresentation;
import com.hivemq.edge.adapters.opcua.condition.ConditionSchemas;
import com.hivemq.edge.adapters.opcua.condition.ConditionUpdate;
import com.hivemq.edge.adapters.opcua.config.tag.BaseEventFieldSet;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaConditionType;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagKind;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * The prose Edge generates about its own configuration, checked against the code that implements it.
 * <p>
 * Review-03 finding 8. The user documentation, the issue, the PR body and the generated configuration help
 * had drifted from the implementation and from each other, and the fixes in this pass moved the
 * implementation again — so the help now describes a REFRESH tag that no longer exists. Two of its statements
 * were not merely stale but contradicted the code beside them: the {@code type} field promised that control
 * events "always publish the ConditionType shape" while {@code getPublishedFields()} returned the base event
 * set, and the {@code kind} field promised that the refresh channel carries the queue-overflow event that
 * says transitions were lost, which OPC 10000-4 §7.22.3 delivers only to the item whose queue filled.
 * <p>
 * <b>These are not comments.</b> Every string here is rendered into the JSON Schema the UI builds its form
 * from, so it is the only description most operators will ever read — which makes a wrong one a support case
 * rather than an untidy file.
 * <p>
 * Prose cannot be checked exhaustively, and this does not try. It pins the specific claims that were wrong,
 * pairs each with the behaviour it describes so the two cannot drift apart silently, and enumerates what has
 * to be listed from the enum rather than by hand.
 */
class GeneratedConfigurationHelpTest {

    @ParameterizedTest
    @EnumSource(ConditionUpdate.Method.class)
    void everyCommandIsListedInTheSchemaAnOperatorReads(final ConditionUpdate.Method method) {
        // Generated from the enum, so this really asserts that the generation still happens -- but that is
        // the property worth holding. The count in this file's prose went stale the moment RESPOND was added;
        // a list built by hand would have too, and an operator cannot invoke a command nobody documented.
        assertThat(SchemaJsonRepresentation.INSTANCE
                        .toJsonSchemaDocument(ConditionSchemas.writeSchema())
                        .toString())
                .as("a command absent from the write schema is one no operator can discover")
                .contains(method.name());
    }

    @Test
    void andThereIsNoNumericFormToDiscover() {
        // Review-03 finding 6 at the one place Edge controls. The user documentation draft still shows a
        // numeric "Wire" column; the parser accepts enum names and nothing else, and the schema says so by
        // declaring the field a string. The draft itself is a Linear document and is corrected separately.
        assertThat(SchemaJsonRepresentation.INSTANCE
                        .toJsonSchemaDocument(ConditionSchemas.writeSchema())
                        .get("properties")
                        .get(ConditionUpdate.FIELD_METHOD)
                        .get("type")
                        .asText())
                .isEqualTo("string");
    }

    @Test
    void theTypeFieldDoesNotPromiseAConditionShapeForControlEvents() {
        // The contradiction, and the pairing that keeps it from returning: the claim and the code that
        // decides it are asserted together.
        final String description = descriptionOf("type");

        assertThat(description)
                .as("control events are BaseEventType events; ConditionType's members are not among them")
                .doesNotContain("ConditionType shape");
        assertThat(new OpcuaTagDefinition("ns=0;i=2253", OpcuaTagKind.REFRESH, OpcuaConditionType.ALARM_CONDITION)
                        .getPublishedFields())
                .as("and this is what the field is describing")
                .isEqualTo(BaseEventFieldSet.INSTANCE);
    }

    @Test
    void theKindFieldDoesNotPromiseQueueOverflowOnTheRefreshChannel() {
        // The other contradiction. OPC 10000-4 §7.22.3: overflow events "are only published to the
        // MonitoredItems in the Subscription that produced" them -- so a refresh tag's own item sees only its
        // own overflow, and the loss on a condition tag is reported against that tag. The help said the
        // opposite, which would have an operator watching the wrong channel for the event that says alarms
        // were lost.
        final String description = descriptionOf("kind");

        assertThat(description)
                .doesNotContain("including the queue-overflow event")
                .as("and it should say where the event does go, not merely omit the wrong claim")
                .contains("the tag that lost the transitions");
    }

    @Test
    void theNotifierFieldStillExplainsTheEscapeHatchTheHiddenInstancePathNeeds() {
        // Review-03 finding 1 made notifierNode load-bearing: it is the operator's statement that a condition
        // the server does not expose is meant to be unbrowsable rather than mistyped. The help has to keep
        // saying when to set it.
        assertThat(descriptionOf("notifierNode"))
                .contains("Leave this")
                .contains("when the server does not publish the references that walk needs");
    }

    @Test
    void queryNarrowingFieldsExplainTheirNotifierHierarchyRequirement() {
        assertThat(descriptionOf("conditionNode"))
                .contains("event-notifier hierarchy")
                .contains("rejects a browsable mismatch")
                .contains("subscribes with a warning");
        assertThat(descriptionOf("sourceNode"))
                .contains("event-notifier hierarchy")
                .contains("rejects a browsable mismatch")
                .contains("subscribes with a warning");
    }

    private static @NotNull String descriptionOf(final @NotNull String fieldName) {
        try {
            final Field field = OpcuaTagDefinition.class.getDeclaredField(fieldName);
            final ModuleConfigField annotation = field.getAnnotation(ModuleConfigField.class);
            assertThat(annotation)
                    .as("'%s' must carry the annotation the UI builds its form from", fieldName)
                    .isNotNull();
            return String.valueOf(annotation.description());
        } catch (final NoSuchFieldException e) {
            throw new LinkageError("OpcuaTagDefinition no longer has a '" + fieldName + "' field", e);
        }
    }

    @Test
    void everyConfigurableFieldIsDescribedAtAll() {
        // The floor beneath the specific claims above. A field with no description renders as a blank line in
        // the UI form, which is how three of these came to be wrong without anyone noticing.
        assertThat(Arrays.stream(OpcuaTagDefinition.class.getDeclaredFields())
                        .filter(field -> field.isAnnotationPresent(ModuleConfigField.class))
                        .filter(field -> String.valueOf(field.getAnnotation(ModuleConfigField.class)
                                        .description())
                                .isBlank())
                        .map(Field::getName))
                .as("every configurable field needs help text")
                .isEmpty();
    }
}
