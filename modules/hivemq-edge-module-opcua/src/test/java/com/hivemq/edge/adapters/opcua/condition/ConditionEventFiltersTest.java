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

import com.hivemq.edge.adapters.opcua.config.tag.OpcuaConditionType;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.milo.opcua.stack.core.encoding.DefaultEncodingContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExtensionObject;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.enumerated.FilterOperator;
import org.eclipse.milo.opcua.stack.core.types.structured.ContentFilter;
import org.eclipse.milo.opcua.stack.core.types.structured.ContentFilterElement;
import org.eclipse.milo.opcua.stack.core.types.structured.ElementOperand;
import org.eclipse.milo.opcua.stack.core.types.structured.EventFilter;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * A {@code ContentFilter} is a flat array whose root is element 0, and whose elements refer to each other by
 * index. Getting that wrong produces an array a server accepts and then evaluates only in part, so these
 * check the structure the specification actually requires rather than the shape Milo happens to tolerate.
 */
class ConditionEventFiltersTest {

    private static final @NotNull NodeId SOURCE = new NodeId(2, "source");
    private static final @NotNull NodeId CONDITION = new NodeId(2, "condition");

    @Test
    void oneOfEachPredicateCountIsRootedAtElementZeroAndFullyReachable() {
        // OPC 10000-4 §7.7.1: evaluation starts at the first entry, and "if an element cannot be traced back
        // to the starting element it is ignored". An unreachable predicate does not fail -- it silently
        // stops narrowing, so the tag over-publishes with no error anywhere to notice.
        assertEveryElementIsReachable(
                whereClauseOf(ConditionEventFilters.forQuery(SOURCE, null, null, OpcuaConditionType.ALARM_CONDITION)));
        assertEveryElementIsReachable(whereClauseOf(
                ConditionEventFilters.forQuery(SOURCE, CONDITION, null, OpcuaConditionType.ALARM_CONDITION)));
        assertEveryElementIsReachable(whereClauseOf(ConditionEventFilters.forQuery(
                SOURCE, CONDITION, OpcuaConditionType.LIMIT_ALARM, OpcuaConditionType.ALARM_CONDITION)));
    }

    @Test
    void everyPredicateSurvivesTheCombination() {
        // The defect this guards against loses predicates without losing elements: the array still holds all
        // three, they are simply not reachable. So counting elements proves nothing -- what matters is that
        // each non-And element is among those traced from the root.
        final ContentFilter filter = whereClauseOf(ConditionEventFilters.forQuery(
                SOURCE, CONDITION, OpcuaConditionType.LIMIT_ALARM, OpcuaConditionType.ALARM_CONDITION));

        final Set<Integer> reachable = reachableFrom(filter);
        final ContentFilterElement[] elements = filter.getElements();
        assertThat(elements).hasSize(5); // three leaves, two Ands

        long reachableLeaves = 0;
        for (final Integer index : reachable) {
            if (elements[index].getFilterOperator() != FilterOperator.And) {
                reachableLeaves++;
            }
        }
        assertThat(reachableLeaves)
                .as("all three narrowing predicates must be reachable, not just the first")
                .isEqualTo(3);
    }

    @Test
    void theRootIsAnAndWheneverThereIsMoreThanOnePredicate() {
        final ContentFilter two = whereClauseOf(
                ConditionEventFilters.forQuery(SOURCE, CONDITION, null, OpcuaConditionType.ALARM_CONDITION));

        assertThat(two.getElements()[0].getFilterOperator())
                .as("element 0 is where evaluation starts, so it must be the conjunction, not a leaf")
                .isEqualTo(FilterOperator.And);
    }

    @Test
    void aSinglePredicateNeedsNoConjunction() {
        final ContentFilter one =
                whereClauseOf(ConditionEventFilters.forQuery(SOURCE, null, null, OpcuaConditionType.ALARM_CONDITION));

        assertThat(one.getElements()).hasSize(1);
        assertThat(one.getElements()[0].getFilterOperator()).isEqualTo(FilterOperator.Equals);
    }

    @Test
    void noNarrowingYieldsANullWhereClauseRatherThanAnEmptyOne() {
        // Null means "everything"; an empty element array is a different statement, and not the one intended.
        final ContentFilter none =
                whereClauseOf(ConditionEventFilters.forQuery(null, null, null, OpcuaConditionType.ALARM_CONDITION));

        assertThat(none.getElements()).isNull();
    }

    private @NotNull ContentFilter whereClauseOf(final @NotNull EventFilter filter) {
        return filter.getWhereClause();
    }

    private void assertEveryElementIsReachable(final @NotNull ContentFilter filter) {
        final ContentFilterElement[] elements = filter.getElements();
        if (elements == null) {
            return;
        }
        assertThat(reachableFrom(filter))
                .as("every element must be traceable from element 0, or the server ignores it")
                .hasSize(elements.length);
    }

    /** The indices reachable from element 0 by following ElementOperands — the server's own traversal. */
    private @NotNull Set<Integer> reachableFrom(final @NotNull ContentFilter filter) {
        final ContentFilterElement[] elements = filter.getElements();
        final Set<Integer> seen = new HashSet<>();
        final Deque<Integer> pending = new ArrayDeque<>();
        pending.push(0);
        while (!pending.isEmpty()) {
            final int index = pending.pop();
            if (!seen.add(index)) {
                continue;
            }
            for (final ExtensionObject operand : elements[index].getFilterOperands()) {
                final Object decoded = operand.decode(DefaultEncodingContext.INSTANCE);
                if (decoded instanceof ElementOperand elementOperand) {
                    pending.push(elementOperand.getIndex().intValue());
                }
            }
        }
        return seen;
    }
}
