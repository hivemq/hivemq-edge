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

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.api.json.CustomConfigSchemaGenerator;
import org.junit.jupiter.api.Test;

/**
 * The JSON schema is the UI's view of the configuration, and the deserialization-internal machinery
 * must not appear in it: a field the schema advertises is a field the form renders and writes back.
 */
class TlsSchemaSurfaceTest {

    @Test
    void theDeserializationInternalsDoNotAppearInTheSchema() {
        final String schema = new CustomConfigSchemaGenerator()
                .generateJsonSchema(OpcUaSpecificAdapterConfig.class)
                .toString();

        assertThat(schema)
                .contains("tlsChecks")
                .contains("tlsChecksFull")
                .contains("allowList")
                .doesNotContain("unknownSettings")
                .doesNotContain("collapsedText");
    }

    @Test
    void neitherDoorNorAnyAxisCarriesASchemaDefault() {
        // A schema `default` is not documentation: React JSON Schema Form materializes defaults into
        // the form data it submits, so a default on either door would make the UI set both doors at
        // once on adapters that had set neither, and defaults on axes would fill in axes the operator
        // deliberately left omitted. The defaults live in the descriptions instead.
        final JsonNode schema = new CustomConfigSchemaGenerator().generateJsonSchema(OpcUaSpecificAdapterConfig.class);
        final JsonNode tls = schema.at("/properties/tls/properties");

        assertThat(tls.isMissingNode())
                .as("tls properties present in the schema")
                .isFalse();
        for (final String door : new String[] {"tlsChecks", "tlsChecksFull"}) {
            assertThat(tls.at("/" + door).findParents("default"))
                    .as("no default anywhere under %s", door)
                    .isEmpty();
        }
    }

    /**
     * EDG-891 P4's neighbour, and the same rule as the axes above — kept here because the concern is
     * identical: what the schema advertises is what the form writes back.
     *
     * <p>{@code messageSecurityMode} advertised {@code "NONE"} while its runtime default is
     * {@code IGNORED}, so a form save could write the one value the operator had not chosen. The two
     * are not interchangeable: unset means the policy decides, and picks {@code SignAndEncrypt} for
     * every policy other than {@code NONE}, whereas an explicit {@code NONE} against a secured policy
     * matches no endpoint the server offers and stops the adapter connecting.
     */
    @Test
    void messageSecurityModeCarriesNoSchemaDefault() {
        final JsonNode security = new CustomConfigSchemaGenerator()
                .generateJsonSchema(OpcUaSpecificAdapterConfig.class)
                .at("/properties/security/properties");

        assertThat(security.isMissingNode())
                .as("security properties present in the schema")
                .isFalse();
        assertThat(security.at("/messageSecurityMode/default").isMissingNode())
                .as("messageSecurityMode must advertise no default; its real default is IGNORED, "
                        + "which is stated in the description")
                .isTrue();
        assertThat(security.at("/messageSecurityMode/enum"))
                .as("IGNORED stays selectable, so an operator can still say it explicitly")
                .isNotEmpty();
    }

    /**
     * The neighbouring policy field keeps its default: there it is the truth ({@code SecPolicy.NONE}
     * really is the runtime default), so materialising it writes a value identical to omitting it.
     * Pinned so that removing it becomes a deliberate act rather than tidying.
     */
    @Test
    void policyKeepsItsDefaultBecauseItMatchesTheRuntime() {
        final JsonNode security = new CustomConfigSchemaGenerator()
                .generateJsonSchema(OpcUaSpecificAdapterConfig.class)
                .at("/properties/security/properties");

        assertThat(security.at("/policy/default").asText()).isEqualTo("NONE");
    }
}
