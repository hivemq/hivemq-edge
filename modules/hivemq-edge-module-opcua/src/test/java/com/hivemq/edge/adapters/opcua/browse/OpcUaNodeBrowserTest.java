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
        assertThat(OpcUaNodeBrowser.sanitizePath("/Data/Static/Int32"))
                .isEqualTo("data/static/int32");
    }

    @Test
    void sanitizePath_handlesNoLeadingSlash() {
        assertThat(OpcUaNodeBrowser.sanitizePath("Data/Static/Int32"))
                .isEqualTo("data/static/int32");
    }

    @Test
    void sanitizePath_sanitizesEachSegment() {
        assertThat(OpcUaNodeBrowser.sanitizePath("/My Folder/Node Name!"))
                .isEqualTo("my-folder/node-name");
    }

    @Test
    void sanitizePath_emptyPath() {
        assertThat(OpcUaNodeBrowser.sanitizePath("")).isEqualTo("");
    }

    @Test
    void sanitizePath_singleSegment() {
        assertThat(OpcUaNodeBrowser.sanitizePath("/Objects")).isEqualTo("objects");
    }

    // --- Default generation ---

    @ParameterizedTest
    @CsvSource({
            "my-opcua, Int32Node, my-opcua-int32node",
            "adapter1, CamelCase, adapter1-camelcase",
            "opc, My Node, opc-my-node"
    })
    void generateTagNameDefault(final String adapterId, final String browseName, final String expected) {
        final OpcUaNodeBrowser browser = new OpcUaNodeBrowser(null, adapterId);
        assertThat(browser.generateTagNameDefault(browseName)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "my-opcua, /Data/Static/Int32, my-opcua/data/static/int32",
            "adapter1, /Objects, adapter1/objects"
    })
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
