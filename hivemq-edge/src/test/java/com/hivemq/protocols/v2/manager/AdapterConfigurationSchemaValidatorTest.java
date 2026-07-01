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
package com.hivemq.protocols.v2.manager;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.schema.AnySchema;
import com.hivemq.adapter.sdk.api.schema.ObjectSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.Schema;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * Proves an adapter's instance configuration is validated against its type's configuration schema before the adapter
 * is constructed: a matching configuration passes, an unconstrained schema accepts anything, and a configuration
 * that violates the schema is reported as a {@link ProtocolAdapterConfigException} naming the adapter.
 */
class AdapterConfigurationSchemaValidatorTest {

    private static final @NotNull ObjectMapper MAPPER = new ObjectMapper();

    private static @NotNull Schema requiresStringHost() {
        return new ObjectSchema(
                Map.of("host", new ScalarSchema(ScalarType.STRING, null, null, null, null, false, true, false)),
                List.of("host"),
                true,
                null,
                null,
                false,
                true,
                false);
    }

    @Test
    void anySchema_acceptsAnyConfiguration() {
        assertThatCode(() -> AdapterConfigurationSchemaValidator.validate(
                        "a", Map.of("whatever", 1), new AnySchema(null, null, false, true, false), MAPPER))
                .doesNotThrowAnyException();
    }

    @Test
    void matchingConfiguration_passes() {
        assertThatCode(() -> AdapterConfigurationSchemaValidator.validate(
                        "a", Map.of("host", "localhost"), requiresStringHost(), MAPPER))
                .doesNotThrowAnyException();
    }

    @Test
    void missingRequiredProperty_isRejectedWithAClearMessageNamingTheAdapter() {
        assertThatThrownBy(
                        () -> AdapterConfigurationSchemaValidator.validate("a", Map.of(), requiresStringHost(), MAPPER))
                .isInstanceOf(ProtocolAdapterConfigException.class)
                .hasMessageContaining("adapter [a]")
                .hasMessageContaining("host");
    }

    @Test
    void wrongPropertyType_isRejected() {
        assertThatThrownBy(() -> AdapterConfigurationSchemaValidator.validate(
                        "a", Map.of("host", 42), requiresStringHost(), MAPPER))
                .isInstanceOf(ProtocolAdapterConfigException.class);
    }
}
