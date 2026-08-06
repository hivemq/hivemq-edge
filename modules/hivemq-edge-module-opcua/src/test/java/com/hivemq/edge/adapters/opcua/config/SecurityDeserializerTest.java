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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.edge.adapters.opcua.Constants;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

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
        final Security security = result.get("security") == null
                ? new Security(null)
                : mapper.convertValue(result.get("security"), Security.class);
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
    void deserialize_modeOnly_usesDefaultPolicy() throws Exception {
        final String json = "{\"messageSecurityMode\":\"SIGN\"}";
        final Security security = mapper.readValue(json, Security.class);
        assertThat(security).isNotNull();
        assertThat(security.policy()).isEqualTo(Constants.DEFAULT_SECURITY_POLICY);
        assertThat(security.messageSecurityMode()).isEqualTo(MsgSecurityMode.SIGN);
    }

    @Test
    void deserialize_unknownChild_isRejectedNamingTheEntryAndTheKnownOnes() {
        // <polciy> is not <policy>. This deserializer reads a raw map, so the application-wide
        // unknown-setting handling never sees the entry - without the rejection it would silently
        // become policy NONE, weaker than whatever the operator wrote.
        assertThatThrownBy(() -> mapper.readValue("{\"polciy\":\"BASIC256SHA256\"}", Security.class))
                .hasMessageContaining("'polciy'")
                .hasMessageContaining("policy, messageSecurityMode");
    }

    @Test
    void deserialize_nonTextPolicyValue_isRejectedNamingTheElement() {
        // A present-but-non-text value must not quietly become policy NONE.
        assertThatThrownBy(() -> mapper.readValue("{\"policy\":{\"nested\":\"NONE\"}}", Security.class))
                .hasMessageContaining("'policy'")
                .hasMessageContaining("Permitted values")
                .hasMessageContaining("BASIC256SHA256");
    }

    @Test
    void deserialize_misspelledPolicyValue_isRejectedNamingThePermittedValues() {
        // SecPolicy.valueOf's bare "No enum constant" is not operator-facing; the rejection names
        // the offending value and everything that would have been accepted.
        assertThatThrownBy(() -> mapper.readValue("{\"policy\":\"BASIC256SHA25\"}", Security.class))
                .hasMessageContaining("'BASIC256SHA25'")
                .hasMessageContaining("Permitted values")
                .hasMessageContaining("BASIC256SHA256")
                .hasMessageContaining("NONE");
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
