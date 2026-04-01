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
package com.hivemq.edge.adapters.opcua.browse;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OpcUaNodeBrowserTest {

    // --- sanitize() ---

    @Test
    void sanitize_lowercasesInput() {
        assertThat(OpcUaNodeBrowser.sanitize("Int32Node")).isEqualTo("int32node");
    }

    @Test
    void sanitize_replacesNonAlphanumericWithDash() {
        assertThat(OpcUaNodeBrowser.sanitize("My Node!@#")).isEqualTo("my-node");
    }

    @Test
    void sanitize_collapsesConsecutiveDashes() {
        assertThat(OpcUaNodeBrowser.sanitize("a---b")).isEqualTo("a-b");
    }

    @Test
    void sanitize_stripsLeadingAndTrailingDashes() {
        assertThat(OpcUaNodeBrowser.sanitize("-test-")).isEqualTo("test");
    }

    @Test
    void sanitize_mixedSpecialChars() {
        assertThat(OpcUaNodeBrowser.sanitize("CamelCase_Node.Name")).isEqualTo("camelcase-node-name");
    }

    @Test
    void sanitize_allDigits() {
        assertThat(OpcUaNodeBrowser.sanitize("12345")).isEqualTo("12345");
    }

    @Test
    void sanitize_emptyInput() {
        assertThat(OpcUaNodeBrowser.sanitize("")).isEqualTo("");
    }

    @Test
    void sanitize_onlySpecialChars() {
        assertThat(OpcUaNodeBrowser.sanitize("!@#$%")).isEqualTo("");
    }

    // --- sanitizePath() ---

    @Test
    void sanitizePath_stripsLeadingSlash() {
        assertThat(OpcUaNodeBrowser.sanitizePath("/Data/Static/Int32")).isEqualTo("data/static/int32");
    }

    @Test
    void sanitizePath_handlesNoLeadingSlash() {
        assertThat(OpcUaNodeBrowser.sanitizePath("Data/Static/Int32")).isEqualTo("data/static/int32");
    }

    @Test
    void sanitizePath_sanitizesEachSegment() {
        assertThat(OpcUaNodeBrowser.sanitizePath("/My Folder/Node Name!")).isEqualTo("my-folder/node-name");
    }

    @Test
    void sanitizePath_emptyPath() {
        assertThat(OpcUaNodeBrowser.sanitizePath("")).isEqualTo("");
    }

    @Test
    void sanitizePath_singleSegment() {
        assertThat(OpcUaNodeBrowser.sanitizePath("/Objects")).isEqualTo("objects");
    }

    // --- extractParentSegment() ---

    @ParameterizedTest
    @CsvSource({
        "'/S7-1500/DataBlocksGlobal/Icon',    DataBlocksGlobal",
        "'/S7-1500/DataBlocksInstance/Icon',   DataBlocksInstance",
        "'/Data/Static/Int32',                 Static",
        "'/Objects/Variable',                  Objects",
    })
    void extractParentSegment_multipleSegments(final String path, final String expected) {
        assertThat(OpcUaNodeBrowser.extractParentSegment(path)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "'/Variable',  ''",
        "'Variable',   ''",
        "'',           ''",
    })
    void extractParentSegment_noParent(final String path, final String expected) {
        assertThat(OpcUaNodeBrowser.extractParentSegment(path)).isEqualTo(expected);
    }

    // --- Default generation ---

    @ParameterizedTest
    @CsvSource({
        "my-opcua, /Data/Static/Int32Node,                   static-int32node",
        "adapter1, /S7-1500/DataBlocksGlobal/Icon,           datablocksglobal-icon",
        "adapter1, /S7-1500/DataBlocksInstance/Icon,         datablocksinstance-icon",
        "opc,      /Objects/My Node,                          objects-my-node",
    })
    void generateTagNameDefault_withParent(final String adapterId, final String path, final String expected) {
        final OpcUaNodeBrowser browser = new OpcUaNodeBrowser(null, adapterId);
        final String browseName = path.substring(path.lastIndexOf('/') + 1);
        assertThat(browser.generateTagNameDefault(path, browseName)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "my-opcua, /Int32Node,    int32node",
        "opc,      /Variable,     variable",
    })
    void generateTagNameDefault_rootLevel_noPrefixAdded(
            final String adapterId, final String path, final String expected) {
        final OpcUaNodeBrowser browser = new OpcUaNodeBrowser(null, adapterId);
        final String browseName = path.substring(path.lastIndexOf('/') + 1);
        assertThat(browser.generateTagNameDefault(path, browseName)).isEqualTo(expected);
    }

    @Test
    void generateTagNameDefault_duplicateDisplayNames_disambiguated() {
        final OpcUaNodeBrowser browser = new OpcUaNodeBrowser(null, "s7");
        // Same display name "Icon" in different folders → different tag_name_default
        assertThat(browser.generateTagNameDefault("/S7-1500/DataBlocksGlobal/Icon", "Icon"))
                .isEqualTo("datablocksglobal-icon");
        assertThat(browser.generateTagNameDefault("/S7-1500/DataBlocksInstance/Icon", "Icon"))
                .isEqualTo("datablocksinstance-icon");
        assertThat(browser.generateTagNameDefault("/S7-1500/TechnologicalObjects/Icon", "Icon"))
                .isEqualTo("technologicalobjects-icon");

        // All three are unique
        assertThat(browser.generateTagNameDefault("/S7-1500/DataBlocksGlobal/Icon", "Icon"))
                .isNotEqualTo(browser.generateTagNameDefault("/S7-1500/DataBlocksInstance/Icon", "Icon"));
    }

    @ParameterizedTest
    @CsvSource({"my-opcua, /Data/Static/Int32, my-opcua/data/static/int32", "adapter1, /Objects, adapter1/objects"})
    void generateNorthboundTopicDefault(final String adapterId, final String path, final String expected) {
        final OpcUaNodeBrowser browser = new OpcUaNodeBrowser(null, adapterId);
        assertThat(browser.generateNorthboundTopicDefault(path)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "my-opcua, /Data/Static/Int32, my-opcua/write/data/static/int32",
        "adapter1, /Objects, adapter1/write/objects"
    })
    void generateSouthboundTopicDefault(final String adapterId, final String path, final String expected) {
        final OpcUaNodeBrowser browser = new OpcUaNodeBrowser(null, adapterId);
        assertThat(browser.generateSouthboundTopicDefault(path)).isEqualTo(expected);
    }
}
