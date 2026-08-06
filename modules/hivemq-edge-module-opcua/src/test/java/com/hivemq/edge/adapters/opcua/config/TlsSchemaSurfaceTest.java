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
}
