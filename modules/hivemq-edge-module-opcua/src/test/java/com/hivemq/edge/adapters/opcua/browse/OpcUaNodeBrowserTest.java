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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hivemq.edge.adapters.browse.BrowseException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OpcUaNodeBrowserTest {

    // --- Browse with non-Good status code ---

    @Test
    void browse_nonGoodStatusCode_throwsBrowseException() {
        final OpcUaClient client = mock(OpcUaClient.class);
        // Server returns BadTooManyOperations (simulates server-side throttling under concurrent browse load)
        final BrowseResult badResult = new BrowseResult(
                new StatusCode(StatusCodes.Bad_TooManyOperations), ByteString.NULL_VALUE, new ReferenceDescription[0]);
        when(client.browseAsync(any(BrowseDescription.class))).thenReturn(CompletableFuture.completedFuture(badResult));

        final OpcUaNodeBrowser browser = new OpcUaNodeBrowser(client, "test-adapter");

        assertThatThrownBy(() -> browser.browse(null, 0))
                .isInstanceOf(BrowseException.class)
                .cause()
                .hasMessageContaining("non-Good status")
                .hasMessageContaining("Bad_TooManyOperations");
    }

    @Test
    void browse_goodStatusCode_emptyResult_succeeds() throws BrowseException {
        final OpcUaClient client = mock(OpcUaClient.class);
        // Good status but no references (empty node)
        final BrowseResult goodResult =
                new BrowseResult(StatusCode.GOOD, ByteString.NULL_VALUE, new ReferenceDescription[0]);
        when(client.browseAsync(any(BrowseDescription.class)))
                .thenReturn(CompletableFuture.completedFuture(goodResult));

        final OpcUaNodeBrowser browser = new OpcUaNodeBrowser(client, "test-adapter");

        assertThat(browser.browse(null, 0).count()).isEqualTo(0);
    }

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

    // --- generateTagNameDefault (full path) ---

    @ParameterizedTest
    @CsvSource({
        "/Data/Static/Int32Node,                                  data-static-int32node",
        "/S7-1500/DataBlocksGlobal/Icon,                          s7-1500-datablocksglobal-icon",
        "/S7-1500/DataBlocksInstance/Icon,                        s7-1500-datablocksinstance-icon",
        "/Objects/My Node,                                        objects-my-node",
        "/Aliases/FindAlias/InputArguments,                       aliases-findalias-inputarguments",
        "/Aliases/TagVariables/FindAlias/InputArguments,          aliases-tagvariables-findalias-inputarguments",
    })
    void generateTagNameDefault_usesFullPath(final String path, final String expected) {
        final OpcUaNodeBrowser browser = new OpcUaNodeBrowser(null, "any");
        assertThat(browser.generateTagNameDefault(path)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "/Int32Node,    int32node",
        "/Variable,     variable",
    })
    void generateTagNameDefault_singleSegment(final String path, final String expected) {
        final OpcUaNodeBrowser browser = new OpcUaNodeBrowser(null, "any");
        assertThat(browser.generateTagNameDefault(path)).isEqualTo(expected);
    }

    @Test
    void generateTagNameDefault_emptyPath() {
        final OpcUaNodeBrowser browser = new OpcUaNodeBrowser(null, "any");
        assertThat(browser.generateTagNameDefault("")).isEqualTo("");
        assertThat(browser.generateTagNameDefault("/")).isEqualTo("");
    }

    @Test
    void generateTagNameDefault_duplicateDisplayNames_disambiguated() {
        final OpcUaNodeBrowser browser = new OpcUaNodeBrowser(null, "s7");
        // Same display name "Icon" in different folders → different tag_name_default
        assertThat(browser.generateTagNameDefault("/S7-1500/DataBlocksGlobal/Icon"))
                .isEqualTo("s7-1500-datablocksglobal-icon");
        assertThat(browser.generateTagNameDefault("/S7-1500/DataBlocksInstance/Icon"))
                .isEqualTo("s7-1500-datablocksinstance-icon");
        assertThat(browser.generateTagNameDefault("/S7-1500/TechnologicalObjects/Icon"))
                .isEqualTo("s7-1500-technologicalobjects-icon");

        // All three are unique
        assertThat(browser.generateTagNameDefault("/S7-1500/DataBlocksGlobal/Icon"))
                .isNotEqualTo(browser.generateTagNameDefault("/S7-1500/DataBlocksInstance/Icon"));
    }

    @Test
    void generateTagNameDefault_deepNesting_disambiguated() {
        final OpcUaNodeBrowser browser = new OpcUaNodeBrowser(null, "opc");
        // Same parent folder + same name, but different ancestors → unique
        assertThat(browser.generateTagNameDefault("/Aliases/FindAlias/InputArguments"))
                .isNotEqualTo(browser.generateTagNameDefault("/Aliases/TagVariables/FindAlias/InputArguments"));
    }

    @Test
    void generateTagNameDefault_specialCharsInSegments() {
        final OpcUaNodeBrowser browser = new OpcUaNodeBrowser(null, "opc");
        assertThat(browser.generateTagNameDefault("/My Folder/Sub.Folder/Node Name!"))
                .isEqualTo("my-folder-sub-folder-node-name");
    }

    // --- deduplicateDefaults ---

    @Test
    void deduplicateDefaults_noDuplicates_unchanged() {
        final List<String> input = List.of("alpha", "beta", "gamma");
        assertThat(OpcUaNodeBrowser.deduplicateDefaults(input)).containsExactly("alpha", "beta", "gamma");
    }

    @Test
    void deduplicateDefaults_allDuplicates_appendsSuffix() {
        final List<String> input = List.of("tag", "tag", "tag");
        assertThat(OpcUaNodeBrowser.deduplicateDefaults(input)).containsExactly("tag", "tag-2", "tag-3");
    }

    @Test
    void deduplicateDefaults_mixedDuplicates() {
        final List<String> input = List.of("alpha", "beta", "alpha", "gamma", "beta", "alpha");
        assertThat(OpcUaNodeBrowser.deduplicateDefaults(input))
                .containsExactly("alpha", "beta", "alpha-2", "gamma", "beta-2", "alpha-3");
    }

    @Test
    void deduplicateDefaults_emptyList() {
        assertThat(OpcUaNodeBrowser.deduplicateDefaults(List.of())).isEmpty();
    }

    @Test
    void deduplicateDefaults_singleElement() {
        assertThat(OpcUaNodeBrowser.deduplicateDefaults(List.of("only"))).containsExactly("only");
    }

    @Test
    void deduplicateDefaults_prosysSimulationScenario() {
        // Simulates the real-world case: 6 simulation instances with same path produce same default
        final List<String> input = List.of(
                "server-valuesimulations-valuesimulation-max-value",
                "server-valuesimulations-valuesimulation-max-value",
                "server-valuesimulations-valuesimulation-max-value",
                "server-valuesimulations-valuesimulation-max-value",
                "server-valuesimulations-valuesimulation-max-value",
                "server-valuesimulations-valuesimulation-max-value");
        final List<String> result = OpcUaNodeBrowser.deduplicateDefaults(input);
        assertThat(result).hasSize(6);
        assertThat(result.get(0)).isEqualTo("server-valuesimulations-valuesimulation-max-value");
        assertThat(result.get(1)).isEqualTo("server-valuesimulations-valuesimulation-max-value-2");
        assertThat(result.get(5)).isEqualTo("server-valuesimulations-valuesimulation-max-value-6");
        // All unique
        assertThat(result).doesNotHaveDuplicates();
    }

    // --- generateNorthboundTopicDefault / generateSouthboundTopicDefault ---

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
