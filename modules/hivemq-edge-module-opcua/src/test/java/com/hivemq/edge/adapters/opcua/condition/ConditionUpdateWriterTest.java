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
 * The type-level MethodId fallback, which exists for servers that keep their conditions out of the
 * AddressSpace. Its scope is the whole question: the specification names such an id for exactly two methods,
 * and extending the table beyond them would be asserting a contract OPC 10000-9 does not state.
 * <p>
 * Checked by reflection because the table is private and has no behavioural surface reachable without a live
 * server — and a server that exposes no condition instance is precisely what Milo cannot model, since its
 * {@code findMethodNode} resolves a type-level id only when the instance still carries the method node.
 */
class ConditionUpdateWriterTest {

    @SuppressWarnings("unchecked")
    private @NotNull Map<ConditionUpdate.Method, NodeId> typeLevelMethods() throws Exception {
        final Field field = ConditionUpdateWriter.class.getDeclaredField("SPECIFIED_TYPE_LEVEL_METHODS");
        field.setAccessible(true);
        return (Map<ConditionUpdate.Method, NodeId>) field.get(null);
    }

    @Test
    void onlyEnableAndDisableHaveASpecifiedTypeLevelMethodId() throws Exception {
        // OPC 10000-9 §5.5.4 and §5.5.5 are the only clauses that say which MethodId to pass when the
        // condition instance is absent. An exhaustive sweep of Part 9 for "the MethodId ..." returns three
        // statements: these two, and one describing the ordinary instance case for shelving.
        //
        // The other twelve methods do say the ConditionId may be used as the ObjectId, which is what makes it
        // tempting to add them here from a vendor library's constant table. That would be inventing a
        // contract: no clause names their MethodId, so there is nothing to fall back TO.
        assertThat(typeLevelMethods()).containsOnlyKeys(ConditionUpdate.Method.ENABLE, ConditionUpdate.Method.DISABLE);
    }

    @Test
    void theFallbackIdsAreTheConditionTypesOwn() throws Exception {
        // "the MethodId ... shall be the NodeId of the Disable Method on the ConditionType" -- so these are
        // ConditionType's methods (i=9027, i=9028), not AcknowledgeableConditionType's or the instance's.
        assertThat(typeLevelMethods().get(ConditionUpdate.Method.ENABLE)).isEqualTo(NodeIds.ConditionType_Enable);
        assertThat(typeLevelMethods().get(ConditionUpdate.Method.DISABLE)).isEqualTo(NodeIds.ConditionType_Disable);
    }

    @Test
    void noShelvingMethodFallsBackToATypeLevelId() throws Exception {
        // The shelving clauses (§5.8.42 and neighbours) permit the ConditionId as ObjectId but explicitly
        // forbid the ShelvedStateMachineType node as ObjectId, and name no MethodId at all. Milo happens to
        // define ShelvedStateMachineType_Unshelve, which is exactly the sort of available-but-unblessed
        // constant this table must not absorb.
        assertThat(typeLevelMethods())
                .doesNotContainKeys(
                        ConditionUpdate.Method.UNSHELVE,
                        ConditionUpdate.Method.ONE_SHOT_SHELVE,
                        ConditionUpdate.Method.TIMED_SHELVE);
    }

    @Test
    void acknowledgeIsNotInTheTableDespiteBeingTheObviousCandidate() throws Exception {
        // Worth pinning on its own: Acknowledge is the method operators actually use, Milo names
        // AcknowledgeableConditionType_Acknowledge, and §5.7.3 says the ConditionId may be the ObjectId. It
        // still names no MethodId, so it is browsed from the instance like the rest and reported as an error
        // when absent.
        assertThat(typeLevelMethods()).doesNotContainKey(ConditionUpdate.Method.ACKNOWLEDGE);
    }
}
