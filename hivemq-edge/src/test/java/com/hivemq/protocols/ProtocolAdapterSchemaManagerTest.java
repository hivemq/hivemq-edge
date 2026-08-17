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
package com.hivemq.protocols;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterValidationFailure;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * EDG-891 P2. The REST door is the canonical one: it accepts the enum spellings the generated schema
 * declares, and nothing else. Being strict is the intended contract — the config file's tolerance of
 * lower case and missing underscores exists only so configurations written for earlier versions keep
 * working. What was wrong was not the strictness but the report: the caller was told "Invalid user
 * supplied data" against a path through the *schema*, neither of which says what to type instead.
 */
class ProtocolAdapterSchemaManagerTest {

    private final @NotNull ObjectMapper objectMapper = new ObjectMapper();
    private final @NotNull ProtocolAdapterSchemaManager schemaManager =
            new ProtocolAdapterSchemaManager(objectMapper, TestAdapterConfig.class);

    @Test
    void aRejectedEnumNamesThePermittedValues() {
        final List<ProtocolAdapterValidationFailure> failures =
                schemaManager.validateObject(objectMapper.valueToTree(Map.of("trustMode", "any_cert")));

        assertThat(failures).isNotEmpty();
        assertThat(failures.get(0).getMessage())
                .as("an operator who typed the wrong spelling needs to be told the right ones")
                .contains("ANY_CERT")
                .contains("CHAIN");
    }

    /**
     * The failure is reported against the payload the caller sent, not against the schema that judged
     * it. The evaluation path — {@code $.properties.trustMode.enum} — names nodes that exist only
     * inside the generated schema, so it cannot be found in anything the operator wrote.
     */
    @Test
    void theReportedPathIsThroughThePayload_notThroughTheSchema() {
        final List<ProtocolAdapterValidationFailure> failures =
                schemaManager.validateObject(objectMapper.valueToTree(Map.of("trustMode", "any_cert")));

        assertThat(failures.get(0).getFieldName())
                .isEqualTo("$.trustMode")
                .doesNotContain("properties")
                .doesNotContain("enum");
    }

    @Test
    void theCanonicalSpellingIsAccepted() {
        assertThat(schemaManager.validateObject(objectMapper.valueToTree(Map.of("trustMode", "ANY_CERT"))))
                .isEmpty();
    }

    /**
     * The message must not echo the value that was submitted. A rejected field can be a password, and a
     * validation failure is rendered into API responses and logs — the same disclosure this ticket's
     * round-04 review closed for the configuration-collapse messages.
     */
    @Test
    void theSubmittedValueIsNotEchoedBack() {
        final List<ProtocolAdapterValidationFailure> failures =
                schemaManager.validateObject(objectMapper.valueToTree(Map.of("trustMode", "hunter2")));

        assertThat(failures).isNotEmpty();
        assertThat(failures.get(0).getMessage()).doesNotContain("hunter2");
    }

    @SuppressWarnings("unused") // the schema generator reads the accessors reflectively
    static class TestAdapterConfig implements ProtocolSpecificAdapterConfig {

        private final @NotNull TestTrustMode trustMode;

        @JsonCreator
        TestAdapterConfig(@JsonProperty("trustMode") final @NotNull TestTrustMode trustMode) {
            this.trustMode = trustMode;
        }

        @JsonProperty("trustMode")
        public @NotNull TestTrustMode getTrustMode() {
            return trustMode;
        }
    }

    /** Mirrors the shape of the OPC UA axes: canonical names, a lenient creator behind them. */
    enum TestTrustMode {
        @JsonProperty("CHAIN")
        CHAIN,
        @JsonProperty("ALLOW_LIST")
        ALLOW_LIST,
        @JsonProperty("ANY_CERT")
        ANY_CERT;

        @JsonCreator
        static @NotNull TestTrustMode fromString(final @NotNull String value) {
            for (final TestTrustMode candidate : values()) {
                if (candidate.name().replace("_", "").equalsIgnoreCase(value.replace("_", ""))) {
                    return candidate;
                }
            }
            throw new IllegalArgumentException("unknown trust mode " + value);
        }
    }
}
