/*
 * Copyright 2019-present HiveMQ GmbH
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
package com.hivemq.protocols.tag;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.schema.SchemaBuilder;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationOutput;
import com.hivemq.persistence.mappings.SouthboundMapping;
import com.hivemq.persistence.mappings.fieldmapping.FieldMapping;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/// Which request fields a tag's schema declares, resolved across every southbound mapping that names the tag.
///
/// A tag has **one** schema but may be reached by several mappings, each with its own settings. So the question
/// "does a request to this tag carry a correlation token?" is not answerable from any single mapping, and a
/// caller asking for a tag's schema supplies only the tag name.
///
/// Whether a tag carries method calls is a property of the tag, so the mappings are expected to agree; one
/// method mapping makes it a method tag. The two optional fields are declared only where every method mapping
/// reads them, because a field one mapping requires and another discards cannot be mandatory.
class RequestFieldsAcrossMappingsTest {

    private static @NotNull SouthboundMapping mapping(
            final @NotNull String tagName,
            final boolean methodCalls,
            final boolean correlationData,
            final boolean responseTopic) {
        return new SouthboundMapping(
                tagName,
                "plant/" + tagName + "/call",
                FieldMapping.DEFAULT_FIELD_MAPPING,
                "{}",
                methodCalls,
                correlationData,
                responseTopic);
    }

    private static @NotNull Schema value() {
        return new SchemaBuilder()
                .startObject()
                .property("setpoint")
                .scalar(ScalarType.LONG)
                .writable()
                .endObject()
                .writable()
                .build();
    }

    /// The southbound document for `tagName`, resolved across `mappings`.
    private static @NotNull JsonNode southbound(
            final @NotNull List<SouthboundMapping> mappings, final @NotNull String tagName) throws Exception {
        final var output = TagSchemaCreationOutputImpl.forTag(mappings, tagName);
        output.finish(new TagSchemaCreationOutput.DataPointSchema(value(), null, null));
        return output.getSchema(TagSchemaDirection.SOUTHBOUND);
    }

    @Test
    void noMappingNamesTheTag_soTheDocumentIsValueOnly() throws Exception {
        // The discovery and browse paths: a tag described with no mapping behind it at all.
        final JsonNode schema = southbound(List.of(mapping("other", true, true, true)), "boiler1");

        assertThat(schema.get("properties").properties()).hasSize(1);
        assertThat(schema.get("properties").has("value")).isTrue();
    }

    @Test
    void onlyNonMethodMappings_soTheDocumentIsValueOnly() throws Exception {
        // An ordinary value write is unchanged by method calls existing elsewhere in the adapter.
        final JsonNode schema = southbound(
                List.of(mapping("boiler1", false, false, false), mapping("boiler1", false, false, false)), "boiler1");

        assertThat(schema.get("properties").properties()).hasSize(1);
    }

    @Test
    void oneMethodMapping_declaresWhatThatMappingReads() throws Exception {
        final JsonNode schema = southbound(List.of(mapping("boiler1", true, true, false)), "boiler1");

        assertThat(schema.get("required").toString()).contains("methodName").contains("correlationData");
        assertThat(schema.get("properties").has("responseTopic")).isFalse();
    }

    @Test
    void mappingsAgreeing_declareTheFieldsBothRead() throws Exception {
        final JsonNode schema = southbound(
                List.of(mapping("boiler1", true, true, true), mapping("boiler1", true, true, true)), "boiler1");

        assertThat(schema.get("required").toString())
                .contains("methodName")
                .contains("correlationData")
                .contains("responseTopic");
    }

    @Test
    void mappingsDisagreeingOnAnOptionalField_leaveItOut() throws Exception {
        // The decisive case. Requiring a token because one mapping reads it would refuse a caller publishing to
        // the other, for omitting a field that mapping would have discarded anyway. The wrapper is open, so a
        // caller may still send it to the mapping that wants it.
        final JsonNode schema = southbound(
                List.of(mapping("boiler1", true, true, false), mapping("boiler1", true, false, false)), "boiler1");

        assertThat(schema.get("properties").has("correlationData"))
                .withFailMessage("a field only one mapping reads was made mandatory: %s", schema)
                .isFalse();
        // methodName is a property of the tag, so one method mapping is enough.
        assertThat(schema.get("required").toString()).contains("methodName");
    }

    @Test
    void aMethodMappingBesideAValueMapping_stillMakesItAMethodTag() throws Exception {
        // The tag's kind records the same fact and nothing checks the two against each other, so this takes the
        // permissive reading: declare the field, and let a request that omits it fail loudly.
        final JsonNode schema = southbound(
                List.of(mapping("boiler1", false, false, false), mapping("boiler1", true, false, false)), "boiler1");

        assertThat(schema.get("required").toString()).contains("methodName");
    }

    @Test
    void mappingsForOtherTags_areIgnored() throws Exception {
        final JsonNode schema = southbound(
                List.of(mapping("boiler1", true, false, false), mapping("boiler2", true, true, true)), "boiler1");

        assertThat(schema.get("properties").has("correlationData")).isFalse();
        assertThat(schema.get("properties").has("responseTopic")).isFalse();
    }
}
