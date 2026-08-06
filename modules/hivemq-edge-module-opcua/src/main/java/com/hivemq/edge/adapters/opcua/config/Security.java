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
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.edge.adapters.opcua.Constants;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@JsonDeserialize(using = Security.SecurityDeserializer.class)
public record Security(
        @JsonProperty("policy")
        @ModuleConfigField(
                title = "OPC UA security policy",
                description = "Security policy to use for communication with the server.",
                defaultValue = "NONE")
        @NotNull
        SecPolicy policy,

        @JsonProperty("messageSecurityMode")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @ModuleConfigField(
                title = "Message Security Mode",
                description =
                        "Message security mode (None, Sign, SignAndEncrypt). If not specified, defaults based on the select OPC UA Security Policy: None→None, others→SignAndEncrypt.",
                defaultValue = "NONE")
        @Nullable
        MsgSecurityMode messageSecurityMode) {

    public Security(
            @JsonProperty("policy") final @Nullable SecPolicy policy,
            @JsonProperty("messageSecurityMode") final @Nullable MsgSecurityMode messageSecurityMode) {
        this.policy = Objects.requireNonNullElse(policy, Constants.DEFAULT_SECURITY_POLICY);
        this.messageSecurityMode = Objects.requireNonNullElse(messageSecurityMode, MsgSecurityMode.IGNORED);
    }

    // Backwards compatibility constructor
    public Security(final @Nullable SecPolicy policy) {
        this(policy, MsgSecurityMode.IGNORED);
    }

    @Override
    public @NotNull SecPolicy policy() {
        return policy;
    }

    static class SecurityDeserializer extends JsonDeserializer<Security> {

        /** Quoted back when an unknown child is refused. */
        private static final @NotNull String KNOWN_SETTINGS = "policy, messageSecurityMode";

        @Override
        public @NotNull Security deserialize(
                final @NotNull JsonParser parser, final @NotNull DeserializationContext context) throws IOException {
            final String text = parser.getText();
            if (text != null && text.isEmpty()) {
                // <security/> collapses to the empty String and means the default policy.
                return new Security(Constants.DEFAULT_SECURITY_POLICY, MsgSecurityMode.IGNORED);
            }

            final Map<String, Object> map;
            try {
                map = parser.readValueAs(Map.class);
            } catch (final IOException e) {
                // Not swallowed: defaulting here would run the adapter with policy NONE in place of
                // security configuration that could not be read.
                throw JsonMappingException.from(
                        parser,
                        "The 'security' configuration could not be read and the adapter configuration has been "
                                + "rejected. Correct the 'security' element.",
                        e);
            }
            if (map == null || map.isEmpty()) {
                return new Security(Constants.DEFAULT_SECURITY_POLICY, MsgSecurityMode.IGNORED);
            }

            // This deserializer reads a raw map, so the application-wide handling for unknown adapter
            // settings never sees these children. Silently dropping one would run the adapter with
            // policy NONE in place of whatever the entry was meant to configure, so the configuration
            // is rejected with the mistake named. The rejection is contained: ProtocolAdapterManager
            // converts each adapter's configuration in isolation, and a running instance stays
            // unchanged.
            for (final String child : map.keySet()) {
                if (!"policy".equals(child) && !"messageSecurityMode".equals(child)) {
                    throw new IllegalArgumentException(("The 'security' configuration contains '%s', which is not a "
                                    + "setting it has. It cannot be applied, and dropping it could mean running with "
                                    + "weaker security than was written, so the configuration is rejected instead. "
                                    + "Correct or remove the entry. Known settings: %s.")
                            .formatted(child, KNOWN_SETTINGS));
                }
            }

            final Object policyValue = map.get("policy");
            final SecPolicy policy;
            if (policyValue == null) {
                policy = Constants.DEFAULT_SECURITY_POLICY;
            } else if (policyValue instanceof String policyString) {
                try {
                    policy = SecPolicy.valueOf(policyString);
                } catch (final IllegalArgumentException e) {
                    // SecPolicy.valueOf's bare "No enum constant" is not operator-facing.
                    throw new IllegalArgumentException(
                            ("'%s' is not a permitted value of the 'security' setting "
                                            + "'policy' and the adapter configuration has been rejected. Permitted values: "
                                            + "%s.")
                                    .formatted(policyString, permittedPolicies()),
                            e);
                }
            } else {
                // A present-but-non-text value must not quietly become policy NONE.
                throw new IllegalArgumentException(("The 'policy' setting of 'security' could not be read: '%s' is "
                                + "not a single text value, so the adapter configuration has been rejected. "
                                + "Permitted values: %s.")
                        .formatted(policyValue, permittedPolicies()));
            }

            final Object modeValue = map.get("messageSecurityMode");
            final MsgSecurityMode messageSecurityMode;
            if (modeValue instanceof String modeString) {
                messageSecurityMode = MsgSecurityMode.fromString(modeString);
            } else {
                messageSecurityMode = MsgSecurityMode.IGNORED;
            }

            return new Security(policy, messageSecurityMode);
        }

        private static @NotNull String permittedPolicies() {
            return Arrays.stream(SecPolicy.values()).map(Enum::name).collect(Collectors.joining(", "));
        }
    }
}
