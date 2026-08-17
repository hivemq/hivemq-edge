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

    // ── blank means unset, on every optional field (EDG-894 P8) ─────────────────────────────────────

    /**
     * The three node-id fields have always read blank as "no narrowing", through {@code blankToNull}. The three
     * enum fields did not, and refused the tag instead — and because that refusal happens while converting the
     * adapter's configuration, the running adapter was left untouched: the write was accepted, nothing was
     * reported, and the tag was absent on read-back. A blank string is exactly what a cleared UI field and an
     * unset optional in a config generator produce, so this was the ordinary path rather than an odd one.
     */
    @Test
    void whenFilterTypeIsBlank_thenThereIsNoTypePredicate() throws Exception {
        // The finding verbatim: a query tag whose filter box was cleared.
        final OpcuaTagDefinition definition = mapper.readValue(
                "{\"node\":\"ns=1;i=9100\",\"kind\":\"EVENT_SUBSCRIPTION\",\"filterType\":\"   \"}",
                OpcuaTagDefinition.class);

        assertThat(definition.getFilterType())
                .as("a blank filterType is the documented 'every event type the notifier carries'")
                .isNull();
        assertThat(definition.getKind()).isEqualTo(OpcuaTagKind.EVENT_SUBSCRIPTION);
    }

    @Test
    void andAnEmptyFilterTypeMeansTheSame() throws Exception {
        assertThat(mapper.readValue("{\"node\":\"ns=1;i=9100\",\"filterType\":\"\"}", OpcuaTagDefinition.class)
                        .getFilterType())
                .isNull();
    }

    @Test
    void whenTheNodeTypeIsBlank_thenItFallsBackToItsDocumentedDefault() throws Exception {
        // The same defect on the sibling field, which the finding did not name: `type` reaches the same creator,
        // so a cleared type box refused the tag too. Absent already meant AlarmConditionType, and blank now
        // agrees with absent.
        for (final String blank : new String[] {"\"   \"", "\"\"", "null"}) {
            assertThat(mapper.readValue("{\"node\":\"ns=1;i=1\",\"type\":" + blank + "}", OpcuaTagDefinition.class)
                            .getType())
                    .as("type=%s must mean unset, like an omitted type", blank)
                    .isEqualTo(OpcuaConditionType.ALARM_CONDITION);
        }
    }

    @Test
    void whenTheKindIsBlank_thenTheTagIsAValue() throws Exception {
        // And the third, which failed by a different route -- no creator at all, so Jackson's own coercion
        // refused with "Cannot coerce empty String to OpcuaTagKind value". Worth its own case because `kind`
        // decides what the tag is: a silently dropped kind is a dropped tag.
        for (final String blank : new String[] {"\"   \"", "\"\"", "null"}) {
            assertThat(mapper.readValue("{\"node\":\"ns=1;i=1\",\"kind\":" + blank + "}", OpcuaTagDefinition.class)
                            .getKind())
                    .as("kind=%s must mean unset, like an omitted kind", blank)
                    .isEqualTo(OpcuaTagKind.VALUE);
        }
    }

    @Test
    void whenEveryOptionalFieldIsBlank_thenTheTagEqualsOneThatOmitsThemAll() throws Exception {
        // The round-trip the finding asks for, over all six optional fields at once: this is the document a UI
        // submits when nothing optional was filled in, and it must mean the same as the minimal document.
        final OpcuaTagDefinition allBlank = mapper.readValue("""
                {"node":"ns=1;i=1","kind":"  ","type":"  ","notifierNode":"  ",
                 "sourceNode":"  ","conditionNode":"  ","filterType":"  "}
                """, OpcuaTagDefinition.class);
        final OpcuaTagDefinition allOmitted = mapper.readValue("{\"node\":\"ns=1;i=1\"}", OpcuaTagDefinition.class);

        assertThat(allBlank).isEqualTo(allOmitted);
    }

    @Test
    void butANonBlankValueThatNamesNothingIsStillRefused() throws Exception {
        // Blank-tolerance must not become anything-tolerance. A typo is a different thing from an empty box: an
        // unset field resolves to a usable default, so quietly treating 'AlarmConditonType' as unset would
        // publish a different shape than was written, and a mistyped kind would subscribe to an alarm node's
        // Value attribute and publish nothing at all.
        assertThatThrownBy(() -> mapper.readValue(
                        "{\"node\":\"ns=1;i=1\",\"filterType\":\"AlarmConditonType\"}", OpcuaTagDefinition.class))
                .hasMessageContaining("AlarmConditonType")
                .hasMessageContaining("AlarmConditionType");
        assertThatThrownBy(() ->
                        mapper.readValue("{\"node\":\"ns=1;i=1\",\"kind\":\"CONDTION\"}", OpcuaTagDefinition.class))
                .hasMessageContaining("CONDTION")
                .hasMessageContaining("EVENT_SUBSCRIPTION");
    }

    @Test
    void andSurroundingWhitespaceDoesNotHideAValidValue() throws Exception {
        // Jackson trimmed before matching an enum, so taking that decision over in a creator had to keep it --
        // otherwise adding the creator to `kind` would have turned " VALUE " from working into a dropped tag.
        assertThat(mapper.readValue("{\"node\":\"ns=1;i=1\",\"kind\":\" CONDITION \"}", OpcuaTagDefinition.class)
                        .getKind())
                .isEqualTo(OpcuaTagKind.CONDITION);
        assertThat(mapper.readValue(
                                "{\"node\":\"ns=1;i=1\",\"type\":\" ExclusiveLevelAlarmType \"}",
                                OpcuaTagDefinition.class)
                        .getType())
                .isEqualTo(OpcuaConditionType.EXCLUSIVE_LEVEL_ALARM);
    }
}
