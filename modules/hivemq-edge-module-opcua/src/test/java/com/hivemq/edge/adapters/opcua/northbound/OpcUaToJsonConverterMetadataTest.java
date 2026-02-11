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
package com.hivemq.edge.adapters.opcua.northbound;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.milo.opcua.stack.core.encoding.EncodingContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OpcUaToJsonConverterMetadataTest {

    private static final EncodingContext ENCODING_CONTEXT = mock(EncodingContext.class);

    @Test
    void whenIncludeMetadataTrue_thenOutputContainsSourceTimestamp() {
        final Instant now = Instant.now();
        final DataValue dataValue = new DataValue(new Variant(42), StatusCode.GOOD, new DateTime(now), null);

        final ByteBuffer result = OpcUaToJsonConverter.convertPayload(ENCODING_CONTEXT, dataValue, true);
        final String json = new String(result.array(), StandardCharsets.UTF_8);
        final JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();

        assertThat(jsonObject.has("sourceTime")).isTrue();
        assertThat(jsonObject.get("sourceTime").getAsString()).isNotEmpty();
        assertThat(jsonObject.has("value")).isTrue();
        assertThat(jsonObject.get("value").getAsInt()).isEqualTo(42);
    }

    @Test
    void whenIncludeMetadataTrue_thenOutputContainsServerTimestamp() {
        final Instant now = Instant.now();
        final DataValue dataValue =
                new DataValue(new Variant(42), StatusCode.GOOD, null, UShort.valueOf(0), new DateTime(now), null);

        final ByteBuffer result = OpcUaToJsonConverter.convertPayload(ENCODING_CONTEXT, dataValue, true);
        final String json = new String(result.array(), StandardCharsets.UTF_8);
        final JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();

        assertThat(jsonObject.has("serverTime")).isTrue();
        assertThat(jsonObject.get("serverTime").getAsString()).isNotEmpty();
    }

    @Test
    void whenIncludeMetadataTrue_thenOutputContainsPicoseconds() {
        final DataValue dataValue = new DataValue(new Variant(42),
                StatusCode.GOOD,
                new DateTime(Instant.now()),
                UShort.valueOf(12345),
                new DateTime(Instant.now()),
                UShort.valueOf(54321));

        final ByteBuffer result = OpcUaToJsonConverter.convertPayload(ENCODING_CONTEXT, dataValue, true);
        final String json = new String(result.array(), StandardCharsets.UTF_8);
        final JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();

        assertThat(jsonObject.has("sourcePicoseconds")).isTrue();
        assertThat(jsonObject.get("sourcePicoseconds").getAsInt()).isEqualTo(12345);
        assertThat(jsonObject.has("serverPicoseconds")).isTrue();
        assertThat(jsonObject.get("serverPicoseconds").getAsInt()).isEqualTo(54321);
    }

    @Test
    void whenIncludeMetadataTrueAndStatusCodeNotGood_thenOutputContainsStatusCode() {
        final DataValue dataValue = new DataValue(new Variant(42), new StatusCode(0x80000000L), // Bad status
                new DateTime(Instant.now()), null);

        final ByteBuffer result = OpcUaToJsonConverter.convertPayload(ENCODING_CONTEXT, dataValue, true);
        final String json = new String(result.array(), StandardCharsets.UTF_8);
        final JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();

        assertThat(jsonObject.has("statusCode")).isTrue();
        final JsonObject statusCode = jsonObject.getAsJsonObject("statusCode");
        assertThat(statusCode.has("code")).isTrue();
        assertThat(statusCode.get("code").getAsLong()).isEqualTo(0x80000000L);
    }

    @Test
    void whenIncludeMetadataTrueAndStatusCodeGood_thenStatusCodeIsThereAndGood() {
        final DataValue dataValue = new DataValue(new Variant(42), StatusCode.GOOD, // Good status (value = 0)
                new DateTime(Instant.now()), null);

        final ByteBuffer result = OpcUaToJsonConverter.convertPayload(ENCODING_CONTEXT, dataValue, true);
        final String json = new String(result.array(), StandardCharsets.UTF_8);
        final JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();

        // StatusCode is omitted when value is 0 (Good)
        assertThat(jsonObject.has("statusCode")).isTrue();
        final var status = jsonObject.getAsJsonObject("statusCode");
        assertThat(status.has("code")).isTrue();
        assertThat(status.get("code").getAsInt()).isEqualTo(0);
        assertThat(status.get("symbol").getAsString()).isEqualTo("Good");
    }

    @Test
    void whenIncludeMetadataFalse_thenOutputDoesNotContainMetadata() {
        final DataValue dataValue = new DataValue(new Variant(42),
                new StatusCode(0x80000000L),
                new DateTime(Instant.now()),
                UShort.valueOf(12345),
                new DateTime(Instant.now()),
                UShort.valueOf(54321));

        final ByteBuffer result = OpcUaToJsonConverter.convertPayload(ENCODING_CONTEXT, dataValue, false);
        final String json = new String(result.array(), StandardCharsets.UTF_8);
        final JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();

        assertThat(jsonObject.has("sourceTime")).isFalse();
        assertThat(jsonObject.has("serverTime")).isFalse();
        assertThat(jsonObject.has("sourcePicoseconds")).isFalse();
        assertThat(jsonObject.has("serverPicoseconds")).isFalse();
        assertThat(jsonObject.has("statusCode")).isFalse();
        assertThat(jsonObject.has("value")).isTrue();
        assertThat(jsonObject.get("value").getAsInt()).isEqualTo(42);
    }

    @Test
    void whenIncludeMetadataDefaultOverload_thenBehavesLikeFalse() {
        final DataValue dataValue = new DataValue(new Variant(42),
                new StatusCode(0x80000000L),
                new DateTime(Instant.now()),
                UShort.valueOf(12345),
                new DateTime(Instant.now()),
                UShort.valueOf(54321));

        final ByteBuffer result = OpcUaToJsonConverter.convertPayload(ENCODING_CONTEXT, dataValue);
        final String json = new String(result.array(), StandardCharsets.UTF_8);
        final JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();

        assertThat(jsonObject.has("sourceTime")).isFalse();
        assertThat(jsonObject.has("serverTime")).isFalse();
        assertThat(jsonObject.has("value")).isTrue();
    }

    @Test
    void whenIncludeMetadataTrueWithAllMetadata_thenOutputContainsAllFields() {
        final Instant sourceTime = Instant.parse("2024-01-15T10:30:00Z");
        final Instant serverTime = Instant.parse("2024-01-15T10:30:01Z");
        final DataValue dataValue =
                new DataValue(new Variant("test-value"), new StatusCode(0x80010000L), // Bad_UnexpectedError
                        new DateTime(sourceTime), UShort.valueOf(100), new DateTime(serverTime), UShort.valueOf(200));

        final ByteBuffer result = OpcUaToJsonConverter.convertPayload(ENCODING_CONTEXT, dataValue, true);
        final String json = new String(result.array(), StandardCharsets.UTF_8);
        final JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();

        assertThat(jsonObject.get("value").getAsString()).isEqualTo("test-value");
        assertThat(jsonObject.get("sourceTime").getAsString()).isEqualTo("2024-01-15T10:30:00Z");
        assertThat(jsonObject.get("serverTime").getAsString()).isEqualTo("2024-01-15T10:30:01Z");
        assertThat(jsonObject.get("sourcePicoseconds").getAsInt()).isEqualTo(100);
        assertThat(jsonObject.get("serverPicoseconds").getAsInt()).isEqualTo(200);
        assertThat(jsonObject.getAsJsonObject("statusCode").get("code").getAsLong()).isEqualTo(0x80010000L);
    }

    @Test
    void whenValueIsNull_thenOutputContainsNoValue() {
        final DataValue dataValue =
                new DataValue(new Variant(null), StatusCode.GOOD, new DateTime(Instant.now()), null);

        final ByteBuffer result = OpcUaToJsonConverter.convertPayload(ENCODING_CONTEXT, dataValue, true);

        final String json = new String(result.array(), StandardCharsets.UTF_8);
        final JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();

        assertThat(jsonObject.has("value")).isFalse();
        assertThat(jsonObject.has("statusCode")).isTrue();
        assertThat(jsonObject.has("sourceTime")).isTrue();
    }

    @Test
    void whenIncludeMetadataTrueWithNullTimestamps_thenOnlyNonNullFieldsIncluded() {
        final DataValue dataValue = new DataValue(new Variant(42), new StatusCode(0x80000000L), null, // no source time
                null, // no source picoseconds
                null, // no server time
                null  // no server picoseconds
        );

        final ByteBuffer result = OpcUaToJsonConverter.convertPayload(ENCODING_CONTEXT, dataValue, true);
        final String json = new String(result.array(), StandardCharsets.UTF_8);
        final JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();

        assertThat(jsonObject.has("value")).isTrue();
        assertThat(jsonObject.has("statusCode")).isTrue();
        assertThat(jsonObject.has("sourceTime")).isFalse();
        assertThat(jsonObject.has("serverTime")).isFalse();
        assertThat(jsonObject.has("sourcePicoseconds")).isFalse();
        assertThat(jsonObject.has("serverPicoseconds")).isFalse();
    }
}
