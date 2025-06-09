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
package com.hivemq.adapter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Preconditions;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.adapter.sdk.api.tag.TagDefinition;
import com.hivemq.api.json.CustomConfigSchemaGenerator;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


/**
 * @author Jochen Mader
 */
@SuppressWarnings("DataFlowIssue")
public class TagSchemaNodeGeneratorTest {

    private static final @NotNull ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void beforeStart(){
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Test
    public void testCanProduceSchemaForTag() {
        final CustomConfigSchemaGenerator generator = new CustomConfigSchemaGenerator();
        final JsonNode node = generator.generateJsonSchema(TestTag.class);
        final JsonNode propertiesNode = node.get("properties");
        final JsonNode definition = findFirstChild(propertiesNode, "definition");
        final JsonNode definitionProperties = findFirstChild(definition, "properties");
        Assertions.assertTrue(hasImmediateChild(definitionProperties, "address"), "TagDefintiion should have an address type");
    }

    private static JsonNode findFirstChild(final @NotNull JsonNode parent, final @NotNull String nodeName){
        Preconditions.checkNotNull(parent);
        JsonNode child = parent.get(nodeName);
        if(child != null){
            return child;
        } else {
            for (final JsonNode jsonNode : parent) {
                if ((child = findFirstChild(jsonNode, nodeName)) != null) {
                    return child;
                }
            }
        }
        return null;
    }

    private static boolean hasImmediateChild(final @NotNull JsonNode parent, final @NotNull String nodeName){
        Preconditions.checkNotNull(parent);
        return parent.get(nodeName) != null;
    }

    @JsonPropertyOrder({"startIdx", "endIdx"})
    static class TestTag implements Tag {

        @JsonProperty(value = "name")
        @ModuleConfigField(title = "Name")
        final @NotNull String name;

        @JsonProperty(value = "description")
        @ModuleConfigField(title = "Description")
        final @NotNull String description;

        @JsonProperty(value = "definition")
        @ModuleConfigField(title = "Definition")
        final @NotNull TestTagDefinition definition;

        public TestTag(final @com.hivemq.extension.sdk.api.annotations.NotNull String name, final @com.hivemq.extension.sdk.api.annotations.NotNull String description, final @com.hivemq.extension.sdk.api.annotations.NotNull TestTagDefinition definition) {
            this.name = name;
            this.description = description;
            this.definition = definition;
        }

        @Override
        public @NotNull TestTagDefinition definition() {
            return definition;
        }

        @Override
        public @NotNull String name() {
            return name;
        }

        @Override
        public @NotNull String description() {
            return description;
        }
    }

    static class TestTagDefinition implements TagDefinition {

        @JsonProperty(value = "address")
        @ModuleConfigField(title = "Address")
        private final @NotNull String address;


        @JsonProperty(value = "dataType")
        @ModuleConfigField(title = "Data Type")
        private final @NotNull DataType dataType;

        public TestTagDefinition(final @NotNull String address, final @NotNull DataType dataType) {
            this.address = address;
            this.dataType = dataType;
        }
    }

    public enum DataType{
        INT,
        INT32,
        INT64
    }

}
