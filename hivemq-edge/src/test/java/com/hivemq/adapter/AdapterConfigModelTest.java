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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Preconditions;
import com.hivemq.edge.modules.config.impl.AdapterSubscriptionImpl;
import com.hivemq.edge.modules.config.impl.UserPropertyImpl;
import com.hivemq.extension.sdk.api.adapters.annotations.ModuleConfigField;
import com.hivemq.extension.sdk.api.adapters.config.AdapterSubscription;
import com.hivemq.extension.sdk.api.adapters.config.ProtocolAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * @author Simon L Johnson
 */
public class AdapterConfigModelTest {

    private static ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void beforeStart(){
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Test
    public void testUserPropertiesAppearOnSubscription() throws JsonProcessingException {

        AdapterConfiguration entity = new AdapterConfiguration();
        entity.subscriptions = new ArrayList<>();
        entity.subscriptions.add(new AdapterSubscriptionImpl("some/path",1,List.of(new UserPropertyImpl("propertyName", "propertyValue"))));
        String marhslaled = mapper.writeValueAsString(entity);
        System.err.println(marhslaled);

        JsonNode node = mapper.readTree(marhslaled);

        JsonNode subscriptions = findFirstChild(node, "subscriptions");
        Assertions.assertFalse(hasImmediateChild(subscriptions, "userProperties"), "Wrapped typed should not have a duplicate title");
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

    static class AdapterConfiguration implements ProtocolAdapterConfig {

        @JsonProperty("subscriptions")
        @ModuleConfigField(title = "Subscriptions",
                           description = "List of subscriptions for the simulation",
                           required = true)
        private @NotNull List<AdapterSubscription> subscriptions = new ArrayList<>();

        @Override
        public @NotNull String getId() {
            return "id";
        }
    }
}
