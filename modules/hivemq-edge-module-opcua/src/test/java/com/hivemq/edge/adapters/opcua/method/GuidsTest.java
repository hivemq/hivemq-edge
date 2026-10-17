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
package com.hivemq.edge.adapters.opcua.method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class GuidsTest {

    private static final UUID SAMPLE = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Test
    void aGuidWithAShortGroup_isRefusedRatherThanPadded() {
        // The finding. UUID.fromString zero-fills a short group, so each of these parses to a DIFFERENT valid
        // Guid -- a node the caller did not name, indistinguishable afterwards from one they meant.
        assertThat(UUID.fromString("72962b91-fa75-4ae6-8d28-b404dc7daf6"))
                .isEqualTo(UUID.fromString("72962b91-fa75-4ae6-8d28-0b404dc7daf6"));

        assertThatThrownBy(() -> Guids.parse("72962b91-fa75-4ae6-8d28-b404dc7daf6"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8-4-4-4-12");
        assertThatThrownBy(() -> Guids.parse("1-1-1-1-1")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theSpellingTheReadPathProduces_isAccepted() {
        // The round trip this has to keep working: populateNodeId writes UUID.toString().
        assertThat(Guids.parse(SAMPLE.toString())).isEqualTo(SAMPLE);
        assertThat(Guids.parse("123E4567-E89B-12D3-A456-426614174000")).isEqualTo(SAMPLE);
    }

    @Test
    void theSpellingsAVendorToolShows_areAccepted() {
        assertThat(Guids.parse("123e4567e89b12d3a456426614174000")).isEqualTo(SAMPLE);
        assertThat(Guids.parse("{123e4567-e89b-12d3-a456-426614174000}")).isEqualTo(SAMPLE);
        assertThat(Guids.parse("(123e4567-e89b-12d3-a456-426614174000)")).isEqualTo(SAMPLE);
        assertThat(Guids.parse("urn:uuid:123e4567-e89b-12d3-a456-426614174000")).isEqualTo(SAMPLE);
        assertThat(Guids.parse("{0x123e4567,0xe89b,0x12d3,{0xa4,0x56,0x42,0x66,0x14,0x17,0x40,0x00}}"))
                .isEqualTo(SAMPLE);
        assertThat(Guids.parse("{ 0x123E4567, 0xE89B, 0x12D3, { 0xA4, 0x56, 0x42, 0x66, 0x14, 0x17, 0x40, 0x0 } }"))
                .isEqualTo(SAMPLE);
        assertThat(Guids.parse("2.25.24249434048109030647017182301789831168")).isEqualTo(SAMPLE);
        assertThat(Guids.parse("urn:oid:2.25.24249434048109030647017182301789831168"))
                .isEqualTo(SAMPLE);
    }

    @Test
    void theBoundaryValues_areAccepted() {
        assertThat(Guids.parse("2.25.0")).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        assertThat(Guids.parse("2.25.340282366920938463463374607431768211455"))
                .isEqualTo(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));
        assertThat(Guids.parse("ffffffff-ffff-ffff-ffff-ffffffffffff"))
                .isEqualTo(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));
    }

    @Test
    void aSpellingThatIsNoneOfTheForms_isRefused() {
        assertThatThrownBy(() -> Guids.parse("123e4567-e89b-12d3-a456-4266141740000"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Guids.parse("123e4567e89b-12d3-a456-426614174000"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Guids.parse("{123e4567-e89b-12d3-a456-426614174000)"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Guids.parse("2.25.340282366920938463463374607431768211456"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Guids.parse("2.25.0123")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Guids.parse("123e4567-e89b-12d3-a456-42661417400g"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Guids.parse("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aCompactBinaryEncoding_isRefusedRatherThanGuessed() {
        // Base64, base32 and base58 of the sample. A 22-character base64 string is also a valid base58 string,
        // so guessing between them would reintroduce the silent-wrong-value failure this class exists to stop.
        assertThatThrownBy(() -> Guids.parse("Ej5FZ+ibEtOkVkJmFBdAAA")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Guids.parse("CI7EKZ7ITMJNHJCWIJTBIF2AAA"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Guids.parse("3FfGK34vwMvVFDedyb2nkf")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requireCanonical_takesOnlyTheHyphenatedForm() {
        // For a string handed on to Milo, which parses it again. Only the spelling Milo reads correctly passes.
        assertThat(Guids.requireCanonical("123e4567-e89b-12d3-a456-426614174000"))
                .isEqualTo("123e4567-e89b-12d3-a456-426614174000");
        assertThatThrownBy(() -> Guids.requireCanonical("123e4567e89b12d3a456426614174000"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Guids.requireCanonical("72962b91-fa75-4ae6-8d28-b404dc7daf6"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
