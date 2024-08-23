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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Preconditions;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import com.hivemq.adapter.sdk.api.config.UserProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
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
        entity.subscriptions.add(new TestContext("some/path",1,List.of(new UserProperty("propertyName", "propertyValue"))));
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
        private @NotNull List<PollingContext> subscriptions = new ArrayList<>();

        @Override
        public @NotNull String getId() {
            return "id";
        }
    }

    private static class TestContext implements PollingContext {

        @JsonProperty(value = "destination", required = true)
        @ModuleConfigField(title = "Destination Topic",
                           description = "The topic to publish data on",
                           required = true,
                           format = ModuleConfigField.FieldType.MQTT_TOPIC)
        protected @Nullable String destination;

        @JsonProperty(value = "qos", required = true)
        @ModuleConfigField(title = "QoS",
                           description = "MQTT Quality of Service level",
                           required = true,
                           numberMin = 0,
                           numberMax = 2,
                           defaultValue = "0")
        protected int qos = 0;

        @JsonProperty(value = "messageHandlingOptions")
        @ModuleConfigField(title = "Message Handling Options",
                           description = "This setting defines the format of the resulting MQTT message, either a message per changed tag or a message per subscription that may include multiple data points per sample",
                           enumDisplayValues = {
                                   "MQTT Message Per Device Tag",
                                   "MQTT Message Per Subscription (Potentially Multiple Data Points Per Sample)"},
                           defaultValue = "MQTTMessagePerTag")
        protected @NotNull MessageHandlingOptions messageHandlingOptions = MessageHandlingOptions.MQTTMessagePerTag;

        @JsonProperty(value = "includeTimestamp")
        @ModuleConfigField(title = "Include Sample Timestamp In Publish?",
                           description = "Include the unix timestamp of the sample time in the resulting MQTT message",
                           defaultValue = "true")
        protected @NotNull Boolean includeTimestamp = Boolean.TRUE;

        @JsonProperty(value = "includeTagNames")
        @ModuleConfigField(title = "Include Tag Names In Publish?",
                           description = "Include the names of the tags in the resulting MQTT publish",
                           defaultValue = "false")
        protected @NotNull Boolean includeTagNames = Boolean.FALSE;

        @JsonProperty(value = "userProperties")
        @ModuleConfigField(title = "User Properties",
                           description = "Arbitrary properties to associate with the subscription",
                           arrayMaxItems = 10)
        private @NotNull List<UserProperty> userProperties = new ArrayList<>();

        @JsonCreator
        public TestContext(
                @JsonProperty("destination") @Nullable final String destination,
                @JsonProperty("qos") final int qos,
                @JsonProperty("userProperties") @Nullable List<UserProperty> userProperties) {
            this.destination = destination;
            this.qos = qos;
            if (userProperties != null) {
                this.userProperties = userProperties;
            }
        }

        @Override
        public @Nullable String getMqttTopic() {
            return destination;
        }

        @Override
        public int getQos() {
            return qos;
        }

        @Override
        public @NotNull MessageHandlingOptions getMessageHandlingOptions() {
            return messageHandlingOptions;
        }

        @Override
        public @NotNull Boolean getIncludeTimestamp() {
            return includeTimestamp;
        }

        @Override
        public @NotNull Boolean getIncludeTagNames() {
            return includeTagNames;
        }

        @Override
        public @NotNull List<UserProperty> getUserProperties() {
            return userProperties;
        }
    }
}
