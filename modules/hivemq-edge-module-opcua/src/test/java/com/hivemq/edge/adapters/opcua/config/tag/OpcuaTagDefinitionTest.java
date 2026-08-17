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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * Round-trips a tag definition through Jackson, which is how tags reach the adapter from the config file.
 * <p>
 * The {@code type} field is what makes a tag a condition rather than a plain value, so it has to survive
 * serialisation in both directions. The case that matters most is its <em>absence</em>: every tag written
 * before conditions existed omits it, and such a tag must keep behaving as a VALUE.
 */
class OpcuaTagDefinitionTest {

    private final @NotNull ObjectMapper mapper = new ObjectMapper();

    @Test
    void whenKindIsAbsent_thenTheTagIsAValue() throws Exception {
        final OpcuaTagDefinition definition = mapper.readValue("{\"node\":\"ns=1;i=1004\"}", OpcuaTagDefinition.class);

        // Back compatibility: a tag written before the kind existed still reads as an ordinary value.
        assertThat(definition.getKind()).isEqualTo(OpcuaTagKind.VALUE);
        assertThat(definition.getNode()).isEqualTo("ns=1;i=1004");
    }

    @Test
    void whenKindIsExplicitlyNull_thenTheTagIsAValue() throws Exception {
        final OpcuaTagDefinition definition =
                mapper.readValue("{\"node\":\"ns=1;i=1004\",\"kind\":null}", OpcuaTagDefinition.class);

        assertThat(definition.getKind()).isEqualTo(OpcuaTagKind.VALUE);
    }

    @Test
    void whenKindIsCondition_thenItIsParsed() throws Exception {
        final OpcuaTagDefinition definition =
                mapper.readValue("{\"node\":\"ns=1;i=9100\",\"kind\":\"CONDITION\"}", OpcuaTagDefinition.class);

        assertThat(definition.getKind()).isEqualTo(OpcuaTagKind.CONDITION);
        assertThat(definition.getNode()).isEqualTo("ns=1;i=9100");
    }

    @Test
    void whenKindIsEventSubscription_thenItIsParsed() throws Exception {
        final OpcuaTagDefinition definition = mapper.readValue(
                "{\"node\":\"ns=1;i=9100\",\"kind\":\"EVENT_SUBSCRIPTION\"}", OpcuaTagDefinition.class);

        assertThat(definition.getKind()).isEqualTo(OpcuaTagKind.EVENT_SUBSCRIPTION);
    }

    @Test
    void whenTheNodeTypeIsNamed_thenItDecidesThePublishedShape() throws Exception {
        // `type` is the node's type -- the input to schema generation -- as distinct from `kind`, which says
        // how the node is observed. The two were once one field, and confusing them is the mistake this
        // guards against.
        final OpcuaTagDefinition definition = mapper.readValue(
                "{\"node\":\"ns=1;i=9100\",\"kind\":\"CONDITION\",\"type\":\"ExclusiveLevelAlarmType\"}",
                OpcuaTagDefinition.class);

        assertThat(definition.getKind()).isEqualTo(OpcuaTagKind.CONDITION);
        assertThat(definition.getType()).isEqualTo(OpcuaConditionType.EXCLUSIVE_LEVEL_ALARM);
    }

    @Test
    void whenADefinitionIsWrittenAndReadBack_thenItIsUnchanged() throws Exception {
        for (final OpcuaTagKind kind : OpcuaTagKind.values()) {
            final OpcuaTagDefinition original = new OpcuaTagDefinition("ns=1;i=9100", kind);

            final OpcuaTagDefinition roundTripped =
                    mapper.readValue(mapper.writeValueAsString(original), OpcuaTagDefinition.class);

            assertThat(roundTripped)
                    .as("a definition of kind %s must survive a write/read cycle", kind)
                    .isEqualTo(original);
        }
    }

    @Test
    void whenTheTypeIsUnknown_thenParsingFails() {
        // A typo in the config should be reported, not silently downgraded to a value.
        assertThatThrownBy(() ->
                        mapper.readValue("{\"node\":\"ns=1;i=1\",\"type\":\"CONDITIONS\"}", OpcuaTagDefinition.class))
                .isInstanceOf(Exception.class);
    }

    @Test
    void whenTheNodeIsMissing_thenParsingFails() {
        assertThatThrownBy(() -> mapper.readValue("{\"type\":\"CONDITION\"}", OpcuaTagDefinition.class))
                .isInstanceOf(Exception.class);
    }
}
