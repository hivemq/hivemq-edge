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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Preconditions;
import com.hivemq.api.json.CustomConfigSchemaGenerator;
import com.hivemq.edge.modules.adapters.annotations.ModuleConfigField;
import com.hivemq.edge.modules.config.impl.AbstractProtocolAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import wiremock.org.custommonkey.xmlunit.NodeTestException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * @author Simon L Johnson
 */
public class SchemaNodeGeneratorTest {

    private static ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void beforeStart(){
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Test
    public void testPropertyOrdering() {

        CustomConfigSchemaGenerator generator = new CustomConfigSchemaGenerator();
        JsonNode node = generator.generateJsonSchema(TestOrderingConfig.class);
        JsonNode firstNode = node.get("properties");
        Iterator<JsonNode> nodes = firstNode.iterator();
        Assertions.assertEquals("Start Index", nodes.next().get("title").textValue(), "Start Index Should be First");
        Assertions.assertEquals("End Index", nodes.next().get("title").textValue(), "End Index Should be First");
        Assertions.assertEquals("Identifier", nodes.next().get("title").textValue(), "Identifier Should be Last");
    }

    @Test
    public void testNestedEntitySchemaDuplication() {

        CustomConfigSchemaGenerator generator = new CustomConfigSchemaGenerator();
        JsonNode node = generator.generateJsonSchema(TestNestedEntity.class);
//        System.err.println(mapper.writeValueAsString(node));
        JsonNode propertiesNode = node.get("properties");
        JsonNode subscriptions = findFirstChild(propertiesNode, "subscriptions");
        JsonNode subscriptionItems = findFirstChild(subscriptions, "items");
        Assertions.assertFalse(hasImmediateChild(subscriptionItems, "title"), "Wrapped typed should not have a duplicate title");
        Assertions.assertFalse(hasImmediateChild(subscriptionItems, "description"), "Wrapped typed should not have a duplicate description");
    }

    @Test
    public void testCustomAttributesAppearInSchema() {

        CustomConfigSchemaGenerator generator = new CustomConfigSchemaGenerator();
        JsonNode node = generator.generateJsonSchema(TestNestedEntity.class);
        JsonNode propertiesNode = node.get("properties");
        JsonNode subscriptions = findFirstChild(propertiesNode, "subscriptions");
        JsonNode subscriptionItems = findFirstChild(subscriptions, "items");
        Assertions.assertTrue(hasImmediateChild(subscriptionItems, "testAttributeName"), "Wrapped typed should have a test-attribute");
    }

    private static JsonNode findFirstChild(final @NotNull JsonNode parent, final @NotNull String nodeName){
        Preconditions.checkNotNull(parent);
        JsonNode child = parent.get(nodeName);
        if(child != null){
            return child;
        } else {
            Iterator<JsonNode> nodes = parent.iterator();
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
    static class TestOrderingConfig extends AbstractProtocolAdapterConfig {

        @JsonProperty(value = "startIdx")
        @ModuleConfigField(title = "Start Index")
        int startIdx;

        @JsonProperty(value = "endIdx")
        @ModuleConfigField(title = "End Index")
        int endIdx;
    }

    static class TestNestedEntity extends AbstractProtocolAdapterConfig {

        @JsonProperty("subscriptions")
        @ModuleConfigField(title = "Subscriptions",
                           description = "Map your sensor data to MQTT Topics", customAttributes = {
                @ModuleConfigField.CustomAttribute(name = "testAttributeName", value = "testAttributeValue")
        })
        private @NotNull List<Subscription> subscriptions = new ArrayList<>();

    }
}
