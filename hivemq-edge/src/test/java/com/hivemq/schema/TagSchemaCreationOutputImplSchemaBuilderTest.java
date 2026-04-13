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
package com.hivemq.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.SchemaBuilder;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationOutput;
import com.hivemq.protocols.tag.TagSchemaCreationOutputImpl;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

class TagSchemaCreationOutputImplSchemaBuilderTest {

    @Test
    void test_tagSchemaBuilder_build_completesTheFuture()
            throws ExecutionException, InterruptedException, TimeoutException {
        final var output = new TagSchemaCreationOutputImpl();

        output.finish(new TagSchemaCreationOutput.DataPointSchema(
                new SchemaBuilder().scalar(ScalarType.LONG).title("RPM").build(),
                null,
                null
        ));

        final JsonNode result = output.getFuture().get(1, TimeUnit.SECONDS);
        assertThat(result).isNotNull();
        assertThat(result.get("properties").get("value").get("type").asText()).isEqualTo("integer");
        assertThat(result.get("properties").get("value").get("title").asText()).isEqualTo("RPM");
    }

    @Test
    void test_tagSchemaBuilder_object_completesWithJsonSchema()
            throws ExecutionException, InterruptedException, TimeoutException {
        final var output = new TagSchemaCreationOutputImpl();


        output.finish(new TagSchemaCreationOutput.DataPointSchema(
                new SchemaBuilder()
                        .startObject()
                        .property("temperature")
                        .required()
                        .scalar(ScalarType.DOUBLE)
                        .title("Temperature")
                        .property("unit")
                        .scalar(ScalarType.STRING)
                        .readable(true)
                        .writable(false)
                        .endObject()
                        .build(),
                null,
                null
        ));

        final JsonNode result = output.getFuture().get(1, TimeUnit.SECONDS);
        System.out.println(result);
        assertThat(result.get("type").asText()).isEqualTo("object");
        assertThat(result.get("properties").get("value").get("properties").has("temperature")).isTrue();
        assertThat(result.get("properties").get("value").get("properties").has("unit")).isTrue();
        assertThat(result.get("properties").get("value").get("required").get(0).asText()).isEqualTo("temperature");
    }

    @Test
    void test_tagSchemaBuilder_buildReturnsSchemaObject() {
        final SchemaBuilder builder = new SchemaBuilder();

        final var schema = builder.scalar(ScalarType.LONG).build();

        assertThat(schema).isNotNull();
        assertThat(schema.title()).isNull();
    }

    @Test
    void test_tagSchemaBuilder_statusRemainsSuccess()
            throws ExecutionException, InterruptedException, TimeoutException {
        final var output = new TagSchemaCreationOutputImpl();

        output.finish(new TagSchemaCreationOutput.DataPointSchema(
                new SchemaBuilder().scalar(ScalarType.BOOLEAN).build(),
                null,
                null
        ));

        output.getFuture().get(1, TimeUnit.SECONDS);
        assertThat(output.getStatus()).isEqualTo(TagSchemaCreationOutputImpl.Status.SUCCESS);
    }
}
