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
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import com.hivemq.api.json.CustomConfigSchemaGenerator;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.hivemq.edge.HiveMQEdgeConstants.ID_REGEX;


/**
 * @author Simon L Johnson
 */
@SuppressWarnings("DataFlowIssue")
public class SchemaNodeGeneratorTest {

    private static final @NotNull ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void beforeStart(){
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Test
    public void testPropertyOrdering() {

        final CustomConfigSchemaGenerator generator = new CustomConfigSchemaGenerator();
        final JsonNode node = generator.generateJsonSchema(TestOrderingConfigSpecific.class);
        final JsonNode firstNode = node.get("properties");
        final Iterator<JsonNode> nodes = firstNode.iterator();
        Assertions.assertEquals("Start Index", nodes.next().get("title").textValue(), "Start Index Should be First");
        Assertions.assertEquals("End Index", nodes.next().get("title").textValue(), "End Index Should be First");
        Assertions.assertEquals("Identifier", nodes.next().get("title").textValue(), "Identifier Should be Last");
    }

    @Test
    public void testNestedEntitySchemaDuplication() {

        final CustomConfigSchemaGenerator generator = new CustomConfigSchemaGenerator();
        final JsonNode node = generator.generateJsonSchema(TestNestedEntity.class);
//        System.err.println(mapper.writeValueAsString(node));
        final JsonNode propertiesNode = node.get("properties");
        final JsonNode subscriptions = findFirstChild(propertiesNode, "subscriptions");
        final JsonNode subscriptionItems = findFirstChild(subscriptions, "items");
        Assertions.assertFalse(hasImmediateChild(subscriptionItems, "title"), "Wrapped typed should not have a duplicate title");
        Assertions.assertFalse(hasImmediateChild(subscriptionItems, "description"), "Wrapped typed should not have a duplicate description");
    }

    @Test
    public void testCustomAttributesAppearInSchema() {

        final CustomConfigSchemaGenerator generator = new CustomConfigSchemaGenerator();
        final JsonNode node = generator.generateJsonSchema(TestNestedEntity.class);
        final JsonNode propertiesNode = node.get("properties");
        final JsonNode subscriptions = findFirstChild(propertiesNode, "subscriptions");
        final JsonNode subscriptionItems = findFirstChild(subscriptions, "items");
        Assertions.assertTrue(hasImmediateChild(subscriptionItems, "testAttributeName"), "Wrapped typed should have a test-attribute");
    }

    private static JsonNode findFirstChild(final @NotNull JsonNode parent, final @NotNull String nodeName){
        Preconditions.checkNotNull(parent);
        JsonNode child = parent.get(nodeName);
        if(child != null){
            return child;
        } else {
            final Iterator<JsonNode> nodes = parent.iterator();
            while (nodes.hasNext()){
                if((child = findFirstChild(nodes.next(), nodeName)) != null){
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
    static class TestOrderingConfigSpecific implements ProtocolSpecificAdapterConfig {

        @JsonProperty(value = "id", required = true)
        @ModuleConfigField(title = "Identifier",
                           description = "Unique identifier for this protocol adapter",
                           format = ModuleConfigField.FieldType.IDENTIFIER,
                           required = true,
                           stringPattern = ID_REGEX,
                           stringMinLength = 1,
                           stringMaxLength = 1024)
        protected @NotNull String id;

        @JsonProperty(value = "startIdx")
        @ModuleConfigField(title = "Start Index")
        int startIdx;

        @JsonProperty(value = "endIdx")
        @ModuleConfigField(title = "End Index")
        int endIdx;
    }

    static class TestNestedEntity implements ProtocolSpecificAdapterConfig {

        @JsonProperty("subscriptions")
        @ModuleConfigField(title = "Subscriptions",
                           description = "Map your sensor data to MQTT Topics", customAttributes = {
                @ModuleConfigField.CustomAttribute(name = "testAttributeName", value = "testAttributeValue")
        })
        private @NotNull List<PollingContext> subscriptions = new ArrayList<>();


        @JsonProperty(value = "id", required = true)
        @ModuleConfigField(title = "Identifier",
                           description = "Unique identifier for this protocol adapter",
                           format = ModuleConfigField.FieldType.IDENTIFIER,
                           required = true,
                           stringPattern = ID_REGEX,
                           stringMinLength = 1,
                           stringMaxLength = 1024)
        protected @NotNull String id;
    }
}
