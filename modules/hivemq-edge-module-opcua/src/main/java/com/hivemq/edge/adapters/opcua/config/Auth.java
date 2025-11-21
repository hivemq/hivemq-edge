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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;

@JsonDeserialize(using = Auth.AuthDeserializer.class)
public record Auth(@JsonProperty("basic") @JsonInclude(JsonInclude.Include.NON_NULL) @ModuleConfigField(title = "Basic Authentication",
                                                                                                        description = "Username / password based authentication") @Nullable BasicAuth basicAuth,
                   @JsonProperty("x509") @JsonInclude(JsonInclude.Include.NON_NULL) @ModuleConfigField(title = "X509 Authentication",
                                                            description = "Authentication based on certificate / private key") @Nullable X509Auth x509Auth) {

    @JsonCreator
    public Auth {
    }

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
        @Override
        public @NotNull Auth deserialize(final @NotNull JsonParser parser, final @NotNull DeserializationContext context)
                throws IOException {
            final String text = parser.getText();
            if (text != null && text.isEmpty()) {
                return new Auth(null, null);
            }

            try {
                final Map<String, Object> map = parser.readValueAs(Map.class);
                if (map == null || map.isEmpty()) {
                    return new Auth(null, null);
                }

                final ObjectMapper mapper = (ObjectMapper) parser.getCodec();
                final BasicAuth basicAuth = fetch(map, "basic", BasicAuth.class, mapper);
                final X509Auth x509Auth = fetch(map, "x509", X509Auth.class, mapper);
                return new Auth(basicAuth, x509Auth);
            } catch (final IOException e) {
                return new Auth(null, null);
            }
        }
    }
}
