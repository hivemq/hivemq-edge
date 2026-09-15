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
package com.hivemq.edge.adapters.opcua.listeners;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.edge.adapters.opcua.config.tag.OpcuaConditionType;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The positional assumption the event routing rests on, which it depends on but does not own.
 * <p>
 * {@code OpcUaSubscriptionLifecycleHandler} reads {@code EventId} and {@code EventType} out of a
 * notification's value array by index, and it now holds those indices as constants rather than looking them
 * up per read. That is only correct while three things hold, none of them enforced by the type system:
 * <ol>
 *   <li>every select clause begins with {@code BASE_EVENT_FIELDS}, whatever type the tag declares;</li>
 *   <li>the entries stay in that order;</li>
 *   <li>none of them acquires an {@code Id} companion, which {@code selectedFields()} inserts immediately
 *       after its field and which would shift every entry after it.</li>
 * </ol>
 * All three are properties of {@link OpcuaConditionType}, a different class. Break any of them and the
 * routing does not fail — it reads the wrong field, so a control event is published as an alarm and a
 * {@code RefreshRequired} is never acted on. Silent, and only on a server that sends control events, which is
 * the class of defect this whole review pass has been about.
 */
class OpcUaSubscriptionEventFieldPositionsTest {

    @Test
    void theBaseEventFieldsAreThePrefixOfEverySelectClause() {
        // Claim 1 and 2 together, for all 22 types. The field list is built root-down, so this is how it
        // already works -- the test is here to keep it that way.
        for (final OpcuaConditionType type : OpcuaConditionType.values()) {
            final List<String> selected = type.selectedFields().stream()
                    .map(OpcuaConditionType.SelectedField::publishedAs)
                    .limit(OpcuaConditionType.BASE_EVENT_FIELDS.size())
                    .toList();

            assertThat(selected)
                    .as("%s must select the base event fields first, in order", type.browseName())
                    .isEqualTo(OpcuaConditionType.BASE_EVENT_FIELDS);
        }
    }

    @Test
    void noBaseEventFieldCarriesAnIdCompanion() {
        // Claim 3. An Id entry is inserted immediately after the field it belongs to, so a base field
        // acquiring one would push everything after it down by one -- and EventType is second, so almost
        // everything is after it.
        for (final OpcuaConditionType type : OpcuaConditionType.values()) {
            final List<OpcuaConditionType.SelectedField> selected = type.selectedFields();

            for (int i = 0; i < OpcuaConditionType.BASE_EVENT_FIELDS.size(); i++) {
                assertThat(selected.get(i).isStateId())
                        .as(
                                "%s: base field '%s' must not carry an Id companion -- one here shifts every "
                                        + "field after it, and the handler reads EventId and EventType by index",
                                type.browseName(), OpcuaConditionType.BASE_EVENT_FIELDS.get(i))
                        .isFalse();
            }
        }
    }

    @Test
    void theIndicesTheHandlerCachesAreTheOnesTheSelectClauseUses() {
        // The constants themselves. They are private, so this asserts the values they are computed from --
        // which is the same statement, and fails for the same reason if either name is ever renamed away.
        assertThat(OpcuaConditionType.BASE_EVENT_FIELDS.indexOf("EventId"))
                .as("EventId must be resolvable, or the RefreshRequired deduplication silently stops working")
                .isNotNegative();
        assertThat(OpcuaConditionType.BASE_EVENT_FIELDS.indexOf("EventType"))
                .as("EventType must be resolvable, or no control event is ever recognised")
                .isNotNegative();

        for (final OpcuaConditionType type : OpcuaConditionType.values()) {
            final List<OpcuaConditionType.SelectedField> selected = type.selectedFields();
            assertThat(selected.get(OpcuaConditionType.BASE_EVENT_FIELDS.indexOf("EventId"))
                            .publishedAs())
                    .as("%s selects EventId where the handler reads it", type.browseName())
                    .isEqualTo("EventId");
            assertThat(selected.get(OpcuaConditionType.BASE_EVENT_FIELDS.indexOf("EventType"))
                            .publishedAs())
                    .as("%s selects EventType where the handler reads it", type.browseName())
                    .isEqualTo("EventType");
        }
    }
}
