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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthDeserializerTest {

    private final @NotNull ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserialize_emptyString_returnsNullAuth() throws Exception {
        final String json = "\"\"";
        final Auth auth = mapper.readValue(json, Auth.class);
        assertThat(auth).isNotNull();
        assertThat(auth.basicAuth()).isNull();
        assertThat(auth.x509Auth()).isNull();
    }

    @Test
    void deserialize_emptyMap_returnsNullAuth() throws Exception {
        final String json = "{}";
        final Auth auth = mapper.readValue(json, Auth.class);
        assertThat(auth).isNotNull();
        assertThat(auth.basicAuth()).isNull();
        assertThat(auth.x509Auth()).isNull();
    }

    @Test
    void deserialize_basicAuthOnly_parsesCorrectly() throws Exception {
        final String json = "{\"basic\":{\"username\":\"testuser\",\"password\":\"testpass\"}}";
        final Auth auth = mapper.readValue(json, Auth.class);
        assertThat(auth).isNotNull();
        assertThat(auth.basicAuth()).isNotNull();
        assertThat(auth.basicAuth().username()).isEqualTo("testuser");
        assertThat(auth.basicAuth().password()).isEqualTo("testpass");
        assertThat(auth.x509Auth()).isNull();
    }

    @Test
    void deserialize_x509AuthOnly_parsesCorrectly() throws Exception {
        final String json = "{\"x509\":{\"enabled\":true}}";
        final Auth auth = mapper.readValue(json, Auth.class);
        assertThat(auth).isNotNull();
        assertThat(auth.basicAuth()).isNull();
        assertThat(auth.x509Auth()).isNotNull();
        assertThat(auth.x509Auth().enabled()).isTrue();
    }

    @Test
    void deserialize_bothAuthTypes_parsesCorrectly() throws Exception {
        final String json = "{\"basic\":{\"username\":\"user\",\"password\":\"pass\"},\"x509\":{\"enabled\":true}}";
        final Auth auth = mapper.readValue(json, Auth.class);
        assertThat(auth).isNotNull();
        assertThat(auth.basicAuth()).isNotNull();
        assertThat(auth.basicAuth().username()).isEqualTo("user");
        assertThat(auth.basicAuth().password()).isEqualTo("pass");
        assertThat(auth.x509Auth()).isNotNull();
        assertThat(auth.x509Auth().enabled()).isTrue();
    }

    @Test
    void deserialize_mapWithUnrelatedFields_ignoresThemAndReturnsNullAuth() throws Exception {
        final String json = "{\"someOtherField\":\"value\"}";
        final Auth auth = mapper.readValue(json, Auth.class);
        assertThat(auth).isNotNull();
        assertThat(auth.basicAuth()).isNull();
        assertThat(auth.x509Auth()).isNull();
    }

    @Test
    void convertValue_emptyString_returnsNullAuth() {
        final Map<String, Object> configMap = new HashMap<>();
        configMap.put("auth", "");
        final Auth auth = mapper.convertValue(configMap.get("auth"), Auth.class);
        assertThat(auth).isNotNull();
        assertThat(auth.basicAuth()).isNull();
        assertThat(auth.x509Auth()).isNull();
    }

    @Test
    void deserialize_nullBasicAuth_parsesCorrectly() throws Exception {
        final String json = "{\"basic\":null,\"x509\":{\"enabled\":true}}";
        final Auth auth = mapper.readValue(json, Auth.class);
        assertThat(auth).isNotNull();
        assertThat(auth.basicAuth()).isNull();
        assertThat(auth.x509Auth()).isNotNull();
        assertThat(auth.x509Auth().enabled()).isTrue();
    }

    @Test
    void deserialize_nullX509Auth_parsesCorrectly() throws Exception {
        final String json = "{\"basic\":{\"username\":\"user\",\"password\":\"pass\"},\"x509\":null}";
        final Auth auth = mapper.readValue(json, Auth.class);
        assertThat(auth).isNotNull();
        assertThat(auth.basicAuth()).isNotNull();
        assertThat(auth.basicAuth().username()).isEqualTo("user");
        assertThat(auth.x509Auth()).isNull();
    }
}
