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

import java.lang.reflect.Field;
import java.util.Map;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The type-level MethodId tables, which exist for servers that keep their conditions out of the AddressSpace.
 * <p>
 * Review-02 finding 1. These used to hold Enable and Disable alone, on the reading that OPC 10000-9 names a
 * type-level MethodId for those two and for nothing else — so every other command was answered with a
 * client-minted {@code Bad_NotSupported} without a Call being sent. That reading was wrong in a way worth
 * pinning: Part 9's silence is not a prohibition, because the rule is stated once and generally in OPC
 * 10000-4 §5.12.2.2 Table 59 — "the methodId is either the NodeId of the Method that is a component of the
 * Object instance or the NodeId of the Method in the ObjectType that defines the Method". Part 9 does not
 * repeat it per method because Part 4 already covers every method there is.
 * <p>
 * Checked by reflection because the tables are private and a server that exposes no condition instance is
 * precisely what Milo cannot model — its {@code findMethodNode} resolves a type-level id only when the
 * instance still carries the method node. The behaviour those ids produce is covered by
 * {@link ConditionUpdateWriterMethodResolutionTest}; this file is about the ids themselves being the standard
 * nodeset's and being complete.
 */
class ConditionUpdateWriterTest {

    @SuppressWarnings("unchecked")
    private @NotNull Map<String, NodeId> table(final @NotNull String name) throws Exception {
        final Field field = ConditionUpdateWriter.class.getDeclaredField(name);
        field.setAccessible(true);
        return (Map<String, NodeId>) field.get(null);
    }

    private @NotNull Map<String, NodeId> conditionTypeMethods() throws Exception {
        return table("CONDITION_TYPE_METHODS");
    }

    private @NotNull Map<String, NodeId> shelvedStateMachineMethods() throws Exception {
        return table("SHELVED_STATE_MACHINE_METHODS");
    }

    @Test
    void everyMethodEdgeOffersHasATypeLevelIdToFallBackTo() throws Exception {
        // The finding, stated as the property that closes it. A command with no entry here cannot reach a
        // server that hides its condition instances, whatever the server supports -- which is what made
        // ACKNOWLEDGE, the method operators actually use, unreachable on a permitted server model.
        for (final ConditionUpdate.Method method : ConditionUpdate.Method.values()) {
            assertThat(conditionTypeMethods())
                    .as("%s must have a type-level MethodId for a call on the condition", method.name())
                    .containsKey(method.browseName());
        }
    }

    @Test
    void andSoDoesEveryCommentedVariantOfOne() throws Exception {
        // The "2" forms are separate method nodes with their own ids, so a table covering only the base
        // forms would drop the comment on exactly the servers that cannot be browsed to check.
        for (final ConditionUpdate.Method method : ConditionUpdate.Method.values()) {
            final String commented = method.commentedBrowseName();
            if (commented == null) {
                continue;
            }
            assertThat(conditionTypeMethods())
                    .as("%s's commented variant %s must have a type-level MethodId", method.name(), commented)
                    .containsKey(commented);
        }
    }

    @Test
    void theIdsAreTheOnesTheDeclaringStandardTypeDefines() throws Exception {
        // Not "an id Milo happens to define" but "the id of the type that declares the operation", which is
        // what Part 4's rule points at. Four standard types declare the operations between them --
        // ConditionType, AcknowledgeableConditionType, AlarmConditionType and DialogConditionType -- and one
        // representative of each is pinned by name. Named rather than counted: the two tests above already
        // assert the extent over Method.values(), and a count written out here is one more thing to forget.
        assertThat(conditionTypeMethods().get("Enable"))
                .as("Enable is declared by ConditionType (§5.5.4)")
                .isEqualTo(NodeIds.ConditionType_Enable);
        assertThat(conditionTypeMethods().get("Acknowledge"))
                .as("Acknowledge is declared by AcknowledgeableConditionType (§5.7)")
                .isEqualTo(NodeIds.AcknowledgeableConditionType_Acknowledge);
        assertThat(conditionTypeMethods().get("Suppress"))
                .as("Suppress is declared by AlarmConditionType (§5.8)")
                .isEqualTo(NodeIds.AlarmConditionType_Suppress);
        assertThat(conditionTypeMethods().get("AddComment"))
                .as("AddComment is declared by ConditionType (§5.5.6)")
                .isEqualTo(NodeIds.ConditionType_AddComment);
    }

    @Test
    void shelvingIsReachedThroughWhicheverObjectTheCallNames() throws Exception {
        // The one place the defining type depends on the ObjectId. A ShelvingState object is a
        // ShelvedStateMachineType; the condition it hangs off is an AlarmConditionType, which declares the
        // same operation one level down. Both are standard nodes, and picking the wrong one would name a
        // method the object's type does not define.
        assertThat(shelvedStateMachineMethods().get("Unshelve"))
                .as("called on the ShelvingState object")
                .isEqualTo(NodeIds.ShelvedStateMachineType_Unshelve);
        assertThat(conditionTypeMethods().get("Unshelve"))
                .as("called on the condition, because this server exposes no ShelvingState")
                .isEqualTo(NodeIds.AlarmConditionType_ShelvingState_Unshelve);

        assertThat(shelvedStateMachineMethods().get("TimedShelve2"))
                .isEqualTo(NodeIds.ShelvedStateMachineType_TimedShelve2);
        assertThat(conditionTypeMethods().get("TimedShelve2"))
                .isEqualTo(NodeIds.AlarmConditionType_ShelvingState_TimedShelve2);
    }

    @Test
    void theShelvingTableCoversTheShelvingMethodsAndNothingElse() throws Exception {
        // It is consulted only when the Call names the ShelvingState object, so an entry for a method that
        // does not live there would never be reached and would suggest one that could be.
        assertThat(shelvedStateMachineMethods())
                .containsOnlyKeys(
                        "Unshelve", "Unshelve2", "OneShotShelve", "OneShotShelve2", "TimedShelve", "TimedShelve2");
    }

    @Test
    void noEntryPointsAtAMethodOfTheWrongNamespace() throws Exception {
        // Every id here is a standard nodeset node, so all of them are namespace 0 and numeric. A vendor
        // library constant that slipped in would not be, and that is the failure this table must not have.
        for (final Map.Entry<String, NodeId> entry : conditionTypeMethods().entrySet()) {
            assertThat(entry.getValue().getNamespaceIndex().intValue())
                    .as("%s must be a standard nodeset id", entry.getKey())
                    .isZero();
        }
        for (final Map.Entry<String, NodeId> entry :
                shelvedStateMachineMethods().entrySet()) {
            assertThat(entry.getValue().getNamespaceIndex().intValue())
                    .as("%s must be a standard nodeset id", entry.getKey())
                    .isZero();
        }
    }
}
