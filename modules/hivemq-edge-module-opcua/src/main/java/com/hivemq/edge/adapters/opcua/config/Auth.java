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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import java.io.IOException;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@JsonDeserialize(using = Auth.AuthDeserializer.class)
public record Auth(
        @JsonProperty("basic")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @ModuleConfigField(title = "Basic Authentication", description = "Username / password based authentication")
        @Nullable
        BasicAuth basicAuth,

        @JsonProperty("x509")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @ModuleConfigField(
                title = "X509 Authentication",
                description = "Authentication based on certificate / private key")
        @Nullable
        X509Auth x509Auth) {

    @JsonCreator
    public Auth {}

    private static <T> @Nullable T fetch(
            final @NotNull Map<String, Object> map,
            final @NotNull String key,
            final @NotNull Class<T> clazz,
            final @NotNull ObjectMapper mapper) {
        return map.containsKey(key) ? mapper.convertValue(map.get(key), clazz) : null;
    }

    @Override
    public @Nullable BasicAuth basicAuth() {
        return basicAuth;
    }

    @Override
    public @Nullable X509Auth x509Auth() {
        return x509Auth;
    }

    static class AuthDeserializer extends JsonDeserializer<Auth> {

        /** Quoted back when an unknown child is refused. */
        private static final @NotNull String KNOWN_SETTINGS = "basic, x509";

        @Override
        public @NotNull Auth deserialize(
                final @NotNull JsonParser parser, final @NotNull DeserializationContext context) throws IOException {
            final String text = parser.getText();
            if (text != null && text.isEmpty()) {
                // <auth/> collapses to the empty String and means no authentication configured.
                return new Auth(null, null);
            }

            final Map<String, Object> map;
            try {
                map = parser.readValueAs(Map.class);
            } catch (final IOException e) {
                // Not swallowed: defaulting here would connect the adapter anonymously in place of
                // authentication configuration that could not be read.
                throw JsonMappingException.from(
                        parser,
                        "The 'auth' configuration could not be read and the adapter configuration has been "
                                + "rejected. Correct the 'auth' element.",
                        e);
            }
            if (map == null || map.isEmpty()) {
                return new Auth(null, null);
            }

            // This deserializer reads a raw map, so the application-wide handling for unknown adapter
            // settings never sees these children. Silently dropping one would connect the adapter
            // anonymously in place of the credentials the entry was meant to configure, so the
            // configuration is rejected with the mistake named. The rejection is contained:
            // ProtocolAdapterManager converts each adapter's configuration in isolation, and a running
            // instance stays unchanged.
            for (final String child : map.keySet()) {
                if (!"basic".equals(child) && !"x509".equals(child)) {
                    throw new IllegalArgumentException(("The 'auth' configuration contains '%s', which is not a "
                                    + "setting it has. It cannot be applied, and dropping it could mean connecting "
                                    + "anonymously where credentials were written, so the configuration is rejected "
                                    + "instead. Correct or remove the entry. Known settings: %s.")
                            .formatted(child, KNOWN_SETTINGS));
                }
            }

            final ObjectMapper mapper = (ObjectMapper) parser.getCodec();
            final BasicAuth basicAuth = fetch(map, "basic", BasicAuth.class, mapper);
            final X509Auth x509Auth = fetch(map, "x509", X509Auth.class, mapper);
            return new Auth(basicAuth, x509Auth);
        }
    }
}
