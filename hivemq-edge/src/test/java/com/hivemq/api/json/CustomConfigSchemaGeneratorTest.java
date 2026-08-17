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
package com.hivemq.api.json;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.annotations.MutuallyExclusiveFields;
import com.hivemq.edge.modules.adapters.simulation.tag.SimulationTagDefinition;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

class CustomConfigSchemaGeneratorTest {

    private final @NotNull CustomConfigSchemaGenerator generator = new CustomConfigSchemaGenerator();

    @Test
    void mutuallyExclusiveFields_emitsOneOfWithDefaultAndBranches() {
        final JsonNode schema = generator.generateJsonSchema(TestGroupPojo.class);

        assertThat(schema.path("title").asText()).isEqualTo("Pick one");
        final JsonNode oneOf = schema.path("oneOf");
        assertThat(oneOf.isArray()).isTrue();
        assertThat(oneOf.size()).isEqualTo(3);

        assertThat(oneOf.get(0).path("title").asText()).isEqualTo("None");
        assertThat(oneOf.get(0).path("type").asText()).isEqualTo("object");
        assertThat(oneOf.get(0).path("properties").size()).isZero();
        assertThat(oneOf.get(0).path("additionalProperties").asBoolean()).isFalse();

        // Top-level default matches the default branch so new instances are unambiguous.
        assertThat(schema.path("default").size()).isZero();
        assertThat(schema.path("default").isObject()).isTrue();

        assertThat(oneOf.get(1).path("title").asText()).isEqualTo("Alpha");
        assertThat(oneOf.get(1).path("type").asText()).isEqualTo("object");
        assertThat(oneOf.get(1).path("properties").has("alpha")).isTrue();
        assertThat(oneOf.get(1).path("required").get(0).asText()).isEqualTo("alpha");
        assertThat(oneOf.get(1).path("additionalProperties").asBoolean()).isFalse();

        assertThat(oneOf.get(2).path("title").asText()).isEqualTo("Beta");
        assertThat(oneOf.get(2).path("properties").has("beta")).isTrue();
        assertThat(oneOf.get(2).path("required").get(0).asText()).isEqualTo("beta");
        assertThat(oneOf.get(2).path("additionalProperties").asBoolean()).isFalse();

        // Top-level properties should no longer contain the group members.
        final JsonNode props = schema.path("properties");
        if (!props.isMissingNode()) {
            assertThat(props.has("alpha")).isFalse();
            assertThat(props.has("beta")).isFalse();
        }
    }

    @Test
    void unannotatedClass_emitsFlatSchemaUntouched() {
        final JsonNode schema = generator.generateJsonSchema(PlainPojo.class);
        assertThat(schema.has("oneOf")).isFalse();
        assertThat(schema.path("properties").has("foo")).isTrue();
        assertThat(schema.path("properties").has("bar")).isTrue();
    }

    @Test
    void generatedSchema_inlinesAllDefs() {
        // The frontend (useTagManager.ts) re-nests the returned schema under `definitions.TagSchema`,
        // which would invalidate any top-level `$defs` referenced via `#/$defs/...`. Every generated
        // schema must be fully self-contained.
        final JsonNode tagSchema =
                generator.generateJsonSchema(com.hivemq.edge.modules.adapters.simulation.tag.SimulationTag.class);
        assertThat(tagSchema.has("$defs")).isFalse();
        assertThat(tagSchema.toString()).doesNotContain("\"$ref\"");
    }

    @Test
    void simulationTagDefinition_emitsExpectedThreeBranches() {
        final JsonNode schema = generator.generateJsonSchema(SimulationTagDefinition.class);

        assertThat(schema.path("title").asText()).isEqualTo("Tag Configuration");
        final JsonNode oneOf = schema.path("oneOf");
        assertThat(oneOf.isArray()).isTrue();
        assertThat(oneOf.size()).isEqualTo(3);

        assertThat(oneOf.get(0).path("title").asText()).isEqualTo("Default (adapter-level random double)");
        assertThat(oneOf.get(0).path("properties").size()).isZero();
        assertThat(oneOf.get(0).path("additionalProperties").asBoolean()).isFalse();

        assertThat(oneOf.get(1).path("title").asText()).isEqualTo("Random Value");
        assertThat(oneOf.get(1).path("properties").has("randomValue")).isTrue();
        assertThat(oneOf.get(1).path("required").get(0).asText()).isEqualTo("randomValue");
        // Nested sub-schema should carry through with its fields.
        final JsonNode randomSub = oneOf.get(1).path("properties").path("randomValue");
        assertThat(randomSub.path("properties").has("minValue")).isTrue();
        assertThat(randomSub.path("properties").has("maxValue")).isTrue();
        assertThat(randomSub.path("properties").has("valueType")).isTrue();

        assertThat(oneOf.get(2).path("title").asText()).isEqualTo("Static Value");
        assertThat(oneOf.get(2).path("properties").has("staticValue")).isTrue();
        assertThat(oneOf.get(2).path("required").get(0).asText()).isEqualTo("staticValue");
        final JsonNode staticSub = oneOf.get(2).path("properties").path("staticValue");
        assertThat(staticSub.path("properties").has("valueType")).isTrue();
        assertThat(staticSub.path("properties").has("value")).isTrue();

        // Top-level properties should not re-expose the group members.
        final JsonNode props = schema.path("properties");
        if (!props.isMissingNode()) {
            assertThat(props.has("randomValue")).isFalse();
            assertThat(props.has("staticValue")).isFalse();
        }
    }

    @MutuallyExclusiveFields(
            value = {"alpha", "beta"},
            titles = {"Alpha", "Beta"},
            includeDefault = true,
            defaultTitle = "None",
            groupTitle = "Pick one")
    public static final class TestGroupPojo {

        @JsonProperty("alpha")
        @ModuleConfigField(title = "Alpha")
        private final @Nullable AlphaPayload alpha;

        @JsonProperty("beta")
        @ModuleConfigField(title = "Beta")
        private final @Nullable BetaPayload beta;

        @JsonCreator
        public TestGroupPojo(
                @JsonProperty("alpha") final @Nullable AlphaPayload alpha,
                @JsonProperty("beta") final @Nullable BetaPayload beta) {
            this.alpha = alpha;
            this.beta = beta;
        }

        public @Nullable AlphaPayload getAlpha() {
            return alpha;
        }

        public @Nullable BetaPayload getBeta() {
            return beta;
        }
    }

    public static final class AlphaPayload {

        @JsonProperty("count")
        @ModuleConfigField(title = "Count")
        private final int count;

        @JsonCreator
        public AlphaPayload(@JsonProperty("count") final int count) {
            this.count = count;
        }

        public int getCount() {
            return count;
        }
    }

    public static final class BetaPayload {

        @JsonProperty("label")
        @ModuleConfigField(title = "Label")
        private final @NotNull String label;

        @JsonCreator
        public BetaPayload(@JsonProperty("label") final @NotNull String label) {
            this.label = label;
        }

        public @NotNull String getLabel() {
            return label;
        }
    }

    public static final class PlainPojo {

        @JsonProperty("foo")
        @ModuleConfigField(title = "Foo")
        private final @Nullable String foo;

        @JsonProperty("bar")
        @ModuleConfigField(title = "Bar")
        private final @Nullable String bar;

        @JsonCreator
        public PlainPojo(
                @JsonProperty("foo") final @Nullable String foo, @JsonProperty("bar") final @Nullable String bar) {
            this.foo = foo;
            this.bar = bar;
        }

        public @Nullable String getFoo() {
            return foo;
        }

        public @Nullable String getBar() {
            return bar;
        }
    }

    @Test
    void enumWithJsonValue_emitsWireFormNotConstantNames() {
        final JsonNode schema = generator.generateJsonSchema(EnumPojo.class);
        final JsonNode field = schema.path("properties").path("flavour");

        // Jackson reads and writes the @JsonValue form, so that is what the schema has to advertise.
        // Emitting the constant names instead makes every offered value fail deserialization.
        final List<String> values = new ArrayList<>();
        field.path("enum").forEach(node -> values.add(node.asText()));
        assertThat(values).containsExactly("plain-vanilla", "double-chocolate");

        // A declared default has to be one of the values the same schema offers, or the form both
        // pre-fills and rejects it.
        assertThat(field.path("default").asText()).isEqualTo("plain-vanilla");
        assertThat(values).contains(field.path("default").asText());
    }

    @Test
    void enumWithoutJsonValue_keepsConstantNames() {
        final JsonNode schema = generator.generateJsonSchema(EnumPojo.class);

        final List<String> values = new ArrayList<>();
        schema.path("properties").path("size").path("enum").forEach(node -> values.add(node.asText()));
        assertThat(values).containsExactly("SMALL", "LARGE");
    }

    public enum Flavour {
        VANILLA("plain-vanilla"),
        CHOCOLATE("double-chocolate");

        private final @NotNull String wireName;

        Flavour(final @NotNull String wireName) {
            this.wireName = wireName;
        }

        @JsonValue
        public @NotNull String wireName() {
            return wireName;
        }
    }

    public enum Size {
        SMALL,
        LARGE
    }

    public static final class EnumPojo {

        @JsonProperty("flavour")
        @ModuleConfigField(title = "Flavour", defaultValue = "plain-vanilla")
        private final @Nullable Flavour flavour;

        @JsonProperty("size")
        @ModuleConfigField(title = "Size")
        private final @Nullable Size size;

        @JsonCreator
        public EnumPojo(
                @JsonProperty("flavour") final @Nullable Flavour flavour,
                @JsonProperty("size") final @Nullable Size size) {
            this.flavour = flavour;
            this.size = size;
        }

        public @Nullable Flavour getFlavour() {
            return flavour;
        }

        public @Nullable Size getSize() {
            return size;
        }
    }
}
