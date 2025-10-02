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
import com.hivemq.edge.adapters.opcua.Constants;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityDeserializerTest {

    private final @NotNull ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserialize_emptyString_usesDefaultPolicy() throws Exception {
        final String json = "\"\"";
        final Security security = mapper.readValue(json, Security.class);
        assertThat(security).isNotNull();
        assertThat(security.policy()).isEqualTo(Constants.DEFAULT_SECURITY_POLICY);
    }

    @Test
    void deserialize_emptyMap_usesDefaultPolicy() throws Exception {
        final String json = "{}";
        final Security security = mapper.readValue(json, Security.class);
        assertThat(security).isNotNull();
        assertThat(security.policy()).isEqualTo(Constants.DEFAULT_SECURITY_POLICY);
    }

    @Test
    void deserialize_nullValue_usesDefaultPolicy() throws Exception {
        final Map<String, Object> map = new HashMap<>();
        map.put("security", null);
        final String json = mapper.writeValueAsString(map);
        final Map<String, Object> result = mapper.readValue(json, Map.class);
        final Security security = result.get("security") == null ?
                new Security(null) :
                mapper.convertValue(result.get("security"), Security.class);
        assertThat(security).isNotNull();
        assertThat(security.policy()).isEqualTo(Constants.DEFAULT_SECURITY_POLICY);
    }

    @Test
    void deserialize_validPolicy_parsesCorrectly() throws Exception {
        final String json = "{\"policy\":\"BASIC128RSA15\"}";
        final Security security = mapper.readValue(json, Security.class);
        assertThat(security).isNotNull();
        assertThat(security.policy()).isEqualTo(SecPolicy.BASIC128RSA15);
    }

    @Test
    void deserialize_nonePolicy_parsesCorrectly() throws Exception {
        final String json = "{\"policy\":\"NONE\"}";
        final Security security = mapper.readValue(json, Security.class);
        assertThat(security).isNotNull();
        assertThat(security.policy()).isEqualTo(SecPolicy.NONE);
    }

    @Test
    void deserialize_mapWithoutPolicy_usesDefaultPolicy() throws Exception {
        final String json = "{\"someOtherField\":\"value\"}";
        final Security security = mapper.readValue(json, Security.class);
        assertThat(security).isNotNull();
        assertThat(security.policy()).isEqualTo(Constants.DEFAULT_SECURITY_POLICY);
    }

    @Test
    void convertValue_emptyString_usesDefaultPolicy() {
        final Map<String, Object> configMap = new HashMap<>();
        configMap.put("security", "");
        final Security security = mapper.convertValue(configMap.get("security"), Security.class);
        assertThat(security).isNotNull();
        assertThat(security.policy()).isEqualTo(Constants.DEFAULT_SECURITY_POLICY);
    }
}
