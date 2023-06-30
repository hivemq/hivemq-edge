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
package com.hivemq.protocols;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.hivemq.api.json.CustomConfigSchemaGenerator;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.IOException;
import java.util.Set;

/**
 * @author Simon L Johnson
 */
public class ProtocolAdapterSchemaManager {

    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ObjectMapper objectMapper;
    private final @NotNull CustomConfigSchemaGenerator customConfigSchemaGenerator;
    private JsonNode schemaNode;
    private JsonSchema schema;

    public ProtocolAdapterSchemaManager(@NotNull final ObjectMapper objectMapper,
                                        @NotNull final ProtocolAdapterInformation adapterInformation) {
        this.adapterInformation = adapterInformation;
        this.objectMapper = objectMapper;
        this.customConfigSchemaGenerator = new CustomConfigSchemaGenerator();
    }

    public synchronized JsonNode generateSchemaNode(){
        if(schemaNode == null){
            schemaNode = customConfigSchemaGenerator.generateJsonSchema(adapterInformation.getConfigClass());
        }
        return schemaNode;
    }

    public synchronized JsonSchema generateSchema(){
        if(schema == null){
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
            schema = factory.getSchema(generateSchemaNode());
            schema.initializeValidators();
        }
        return schema;
    }

    public Set<ValidationMessage> validateJsonDocument(@NotNull final byte[] jsonDocument) throws IOException {
        JsonSchema schema = generateSchema();
        Preconditions.checkNotNull(jsonDocument);
        JsonNode node = objectMapper.readTree(jsonDocument);
        return schema.validate(node);
    }

    public Set<ValidationMessage> validateObject(@NotNull final Object o) {
        Preconditions.checkNotNull(o);
        JsonNode node;
        if(o instanceof JsonNode){
            node = (JsonNode) o;
        }
        else {
            node = objectMapper.valueToTree(o);
        }
        return generateSchema().validate(node);
    }

//    public static void main(String[] args) throws IOException {
//        ProtocolAdapterSchemaManager manager = new ProtocolAdapterSchemaManager(new ObjectMapper(),
//                SimulationProtocolAdapterInformation.INSTANCE);
//
//
//        Set<ValidationMessage> set = manager.validateJsonDocument(("{\n" +
//                        "  \"id\": \"simulation1\",\n" +
//                        "  \"host\": \"localhost\",\n" +
//                        "  \"pollingIntervalMillis\": 300,\n" +
//                        "  \"port\": 8080,\n" +
//                        "  \"subscriptions\": [\n" +
//                        "    {\n" +
//                        "      \"destination\": \"topic\",\n" +
//                        "      \"qos\": 1,\n" +
//                        "      \"filter\": \"path\"\n" +
//                        "    }\n" +
//                        "  ]\n" +
//                        "}").getBytes());
//        set.stream().forEach(e -> System.err.println(e.getMessage()));
//    }
}
