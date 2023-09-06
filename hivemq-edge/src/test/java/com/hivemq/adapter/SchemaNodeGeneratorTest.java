package com.hivemq.adapter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hivemq.api.json.CustomConfigSchemaGenerator;
import com.hivemq.edge.modules.adapters.annotations.ModuleConfigField;
import com.hivemq.edge.modules.config.impl.AbstractProtocolAdapterConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Iterator;


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

//    @Test
//    public void testHttpConfig() throws JsonProcessingException {
//
//        CustomConfigSchemaGenerator generator = new CustomConfigSchemaGenerator();
//        JsonNode node = generator.generateJsonSchema(HttpAdapterConfig.class);
//        System.err.println(mapper.writeValueAsString(node));
//    }

    @JsonPropertyOrder({"startIdx", "endIdx"})
    static class TestOrderingConfig extends AbstractProtocolAdapterConfig {

        @JsonProperty(value = "startIdx")
        @ModuleConfigField(title = "Start Index")
        int startIdx;

        @JsonProperty(value = "endIdx")
        @ModuleConfigField(title = "End Index")
        int endIdx;
    }
}
