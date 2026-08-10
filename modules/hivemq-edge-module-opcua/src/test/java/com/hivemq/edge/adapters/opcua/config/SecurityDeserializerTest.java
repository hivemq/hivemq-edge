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

import static com.hivemq.protocols.ProtocolAdapterUtils.createProtocolAdapterMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.edge.adapters.opcua.Constants;
import com.hivemq.edge.adapters.opcua.OpcUaProtocolAdapterFactory;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

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

    // -- messageSecurityMode ---------------------------------------------------------------------
    //
    // The last value in this block that resolved a misspelling to a default. IGNORED does not mean
    // "no message security": it means "let the policy decide", and under policy NONE the policy
    // decides MessageSecurityMode.None. So 'SING' used to produce an unsigned, unencrypted
    // connection where the correctly spelled 'SIGN' matches no endpoint at all and refuses to
    // connect - a typo turning a safe failure into an insecure success.

    @Test
    void deserialize_misspelledMode_isRejectedNamingThePermittedValues() {
        assertThatThrownBy(() -> mapper.readValue("{\"messageSecurityMode\":\"SING\"}", Security.class))
                .hasMessageContaining("'SING'")
                .hasMessageContaining("MsgSecurityMode")
                .hasMessageContaining("SIGN_AND_ENCRYPT");
    }

    @Test
    void deserialize_misspelledModeUnderPolicyNone_isRejectedRatherThanRunUnsigned() {
        // The exact fail-open shape, pinned as one case: the pairing is what makes the typo
        // dangerous, so the pairing is what has to be refused.
        assertThatThrownBy(() ->
                        mapper.readValue("{\"policy\":\"NONE\",\"messageSecurityMode\":\"SING\"}", Security.class))
                .hasMessageContaining("'SING'");
    }

    @Test
    void deserialize_nonTextModeValue_isRejectedNamingTheSetting() {
        // A present-but-non-text value took the defaulting branch directly, without even reaching
        // the enum parser.
        assertThatThrownBy(() -> mapper.readValue("{\"messageSecurityMode\":{\"nested\":\"SIGN\"}}", Security.class))
                .hasMessageContaining("'messageSecurityMode'")
                .hasMessageContaining("Permitted values")
                .hasMessageContaining("SIGN_AND_ENCRYPT");
    }

    @Test
    void deserialize_nonTextModeValue_isRejectedWhateverTheShape() {
        assertThatThrownBy(() -> mapper.readValue("{\"messageSecurityMode\":[\"SIGN\"]}", Security.class))
                .hasMessageContaining("'messageSecurityMode'");
        assertThatThrownBy(() -> mapper.readValue("{\"messageSecurityMode\":3}", Security.class))
                .hasMessageContaining("'messageSecurityMode'");
        assertThatThrownBy(() -> mapper.readValue("{\"messageSecurityMode\":true}", Security.class))
                .hasMessageContaining("'messageSecurityMode'");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void deserialize_blankMode_isUnsetAndMeansIgnored(final @NotNull String written) throws Exception {
        // Blank stays "unset" - the compatibility case an empty element produces. Only a present,
        // non-blank, unrecognized value is a configuration error.
        final Security security = mapper.readValue("{\"messageSecurityMode\":\"" + written + "\"}", Security.class);

        assertThat(security.messageSecurityMode()).isEqualTo(MsgSecurityMode.IGNORED);
    }

    @Test
    void deserialize_absentOrNullMode_meansIgnored() throws Exception {
        assertThat(mapper.readValue("{\"policy\":\"NONE\"}", Security.class).messageSecurityMode())
                .isEqualTo(MsgSecurityMode.IGNORED);
        assertThat(mapper.readValue("{\"messageSecurityMode\":null}", Security.class)
                        .messageSecurityMode())
                .isEqualTo(MsgSecurityMode.IGNORED);
    }

    @Test
    void deserialize_explicitlyIgnoredMode_isStillAccepted() throws Exception {
        // IGNORED is a value released versions accept and write back; rejecting it would break
        // configurations that round-tripped through the API.
        assertThat(mapper.readValue("{\"messageSecurityMode\":\"IGNORED\"}", Security.class)
                        .messageSecurityMode())
                .isEqualTo(MsgSecurityMode.IGNORED);
    }

    @ParameterizedTest
    @CsvSource({
        "SIGN, SIGN",
        "sign, SIGN",
        "SIGN_AND_ENCRYPT, SIGN_AND_ENCRYPT",
        "SignAndEncrypt, SIGN_AND_ENCRYPT",
        "signandencrypt, SIGN_AND_ENCRYPT",
        "'  Sign  ', SIGN",
        "None, NONE"
    })
    void deserialize_modeParsing_staysForgivingAboutCaseAndUnderscores(
            final @NotNull String written, final @NotNull MsgSecurityMode expected) throws Exception {
        // The leniency the released spellings rely on - the integration suite writes 'Sign' and
        // 'SignAndEncrypt' - must survive the strictness. Trimming is new, and is the shared parser's.
        assertThat(mapper.readValue("{\"messageSecurityMode\":\"" + written + "\"}", Security.class)
                        .messageSecurityMode())
                .isEqualTo(expected);
    }

    // -- the same values through the production mapper, in the shape the REST API delivers ---------

    @Test
    void restMapper_misspelledMode_isRejected() {
        assertThatThrownBy(() -> convertRestConfig(Map.of("policy", "NONE", "messageSecurityMode", "SING")))
                .hasMessageContaining("'SING'")
                .hasMessageContaining("SIGN_AND_ENCRYPT");
    }

    @Test
    void restMapper_nonTextMode_isRejected() {
        assertThatThrownBy(() -> convertRestConfig(Map.of("messageSecurityMode", Map.of("value", "SIGN"))))
                .hasMessageContaining("'messageSecurityMode'");
    }

    @Test
    void restMapper_validMode_isAccepted() {
        final OpcUaSpecificAdapterConfig config =
                convertRestConfig(Map.of("policy", "BASIC256SHA256", "messageSecurityMode", "SIGN"));

        assertThat(config.getSecurity().policy()).isEqualTo(SecPolicy.BASIC256SHA256);
        assertThat(config.getSecurity().messageSecurityMode()).isEqualTo(MsgSecurityMode.SIGN);
    }

    /**
     * The REST path: a JSON-shaped map, native types, through the production adapter mapper — not the
     * all-Strings map the XML configuration file produces. Same factory entry point the config
     * converter uses.
     */
    private @NotNull OpcUaSpecificAdapterConfig convertRestConfig(final @NotNull Map<String, Object> security) {
        final ProtocolAdapterFactoryInput input = mock(ProtocolAdapterFactoryInput.class);
        when(input.isWritingEnabled()).thenReturn(true);
        return (OpcUaSpecificAdapterConfig) new OpcUaProtocolAdapterFactory(input)
                .convertConfigObject(
                        createProtocolAdapterMapper(new ObjectMapper()),
                        Map.of("uri", "opc.tcp://machine.local:4840", "security", security),
                        true);
    }
}
