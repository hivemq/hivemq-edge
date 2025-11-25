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
package com.hivemq.edge.adapters.opcua.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.edge.adapters.opcua.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

@JsonDeserialize(using = Security.SecurityDeserializer.class)
public record Security(@JsonProperty("policy") @ModuleConfigField(title = "OPC UA security policy",
                                                                  description = "Security policy to use for communication with the server.",
                                                                  defaultValue = "NONE") @NotNull SecPolicy policy,
                       @JsonProperty("messageSecurityMode")
                       @JsonInclude(JsonInclude.Include.NON_NULL)
                       @ModuleConfigField(title = "Message Security Mode",
                                         description = "Message security mode (None, Sign, SignAndEncrypt). If not specified, defaults based on the select OPC UA Security Policy: None→None, others→SignAndEncrypt.") @Nullable MsgSecurityMode messageSecurityMode) {

    public Security(@JsonProperty("policy") final @Nullable SecPolicy policy,
                    @JsonProperty("messageSecurityMode") final @Nullable MsgSecurityMode messageSecurityMode) {
        this.policy = Objects.requireNonNullElse(policy, Constants.DEFAULT_SECURITY_POLICY);
        this.messageSecurityMode = Objects.requireNonNullElse(messageSecurityMode, MsgSecurityMode.NONE);
    }

    // Backwards compatibility constructor
    public Security(final @Nullable SecPolicy policy) {
        this(policy, null);
    }

    @Override
    public @NotNull SecPolicy policy() {
        return policy;
    }

    static class SecurityDeserializer extends JsonDeserializer<Security> {
        @Override
        public @NotNull Security deserialize(
                final @NotNull JsonParser parser,
                final @NotNull DeserializationContext context) throws IOException {
            final String text = parser.getText();
            if (text != null && text.isEmpty()) {
                return new Security(Constants.DEFAULT_SECURITY_POLICY, MsgSecurityMode.NONE);
            }

            try {
                final Map<String, Object> map = parser.readValueAs(Map.class);
                if (map == null || map.isEmpty()) {
                    return new Security(Constants.DEFAULT_SECURITY_POLICY, MsgSecurityMode.NONE);
                }

                final Object policyValue = map.get("policy");
                final SecPolicy policy;
                if (policyValue instanceof String) {
                    policy = SecPolicy.valueOf((String) policyValue);
                } else {
                    policy = Constants.DEFAULT_SECURITY_POLICY;
                }

                final Object modeValue = map.get("messageSecurityMode");
                final MsgSecurityMode messageSecurityMode;
                if (modeValue instanceof String) {
                    messageSecurityMode = MsgSecurityMode.fromString((String) modeValue);
                } else {
                    messageSecurityMode = MsgSecurityMode.NONE;
                }

                return new Security(policy, messageSecurityMode);
            } catch (final IOException e) {
                return new Security(Constants.DEFAULT_SECURITY_POLICY, MsgSecurityMode.NONE);
            }
        }
    }
}
