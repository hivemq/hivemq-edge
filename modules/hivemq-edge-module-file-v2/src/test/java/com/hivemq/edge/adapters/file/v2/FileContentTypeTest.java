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
package com.hivemq.edge.adapters.file.v2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class FileContentTypeTest {

    @Test
    void binaryDecodesToBase64EncodedBytes() {
        final byte[] raw = {0, 1, 2, 3, (byte) 0xFF};

        final Object value = FileContentType.BINARY.map(raw);

        assertThat(value).isInstanceOf(byte[].class);
        assertThat((byte[]) value).isEqualTo(Base64.getEncoder().encode(raw));
    }

    @Test
    void textPlainDecodesToAUtf8String() {
        final Object value = FileContentType.TEXT_PLAIN.map("hëllo".getBytes(StandardCharsets.UTF_8));

        assertThat(value).isEqualTo("hëllo");
    }

    @Test
    void textJsonDecodesToAJsonNode() {
        final Object value = FileContentType.TEXT_JSON.map("{\"temperature\":21}".getBytes(StandardCharsets.UTF_8));

        assertThat(value).isInstanceOf(JsonNode.class);
        assertThat(((JsonNode) value).get("temperature").asInt()).isEqualTo(21);
    }

    @Test
    void textXmlAndTextCsvDecodeToUtf8Strings() {
        assertThat(FileContentType.TEXT_XML.map("<a>1</a>".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("<a>1</a>");
        assertThat(FileContentType.TEXT_CSV.map("a,b,c".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("a,b,c");
    }

    @Test
    void malformedJsonRaisesAMappingException() {
        assertThatThrownBy(() -> FileContentType.TEXT_JSON.map("{not-json".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(MappingException.class);
    }
}
