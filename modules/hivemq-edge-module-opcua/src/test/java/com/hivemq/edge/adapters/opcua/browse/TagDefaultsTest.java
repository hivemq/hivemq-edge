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

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** {@link TagDefaults}: the pure string functions behind tag names and topics. */
class TagDefaultsTest {

    @Test
    void sanitize_lowercasesInput() {
        assertThat(TagDefaults.sanitize("Int32Node")).isEqualTo("int32node");
    }

    @Test
    void sanitize_replacesNonAlphanumericWithDash() {
        assertThat(TagDefaults.sanitize("My Node!@#")).isEqualTo("my-node");
    }

    @Test
    void sanitize_collapsesConsecutiveDashes() {
        assertThat(TagDefaults.sanitize("a---b")).isEqualTo("a-b");
    }

    @Test
    void sanitize_stripsLeadingAndTrailingDashes() {
        assertThat(TagDefaults.sanitize("-test-")).isEqualTo("test");
    }

    @Test
    void sanitize_mixedSpecialChars() {
        assertThat(TagDefaults.sanitize("CamelCase_Node.Name")).isEqualTo("camelcase-node-name");
    }

    @Test
    void sanitize_allDigits() {
        assertThat(TagDefaults.sanitize("12345")).isEqualTo("12345");
    }

    @Test
    void sanitize_emptyInput() {
        assertThat(TagDefaults.sanitize("")).isEqualTo("");
    }

    @Test
    void sanitize_onlySpecialChars() {
        assertThat(TagDefaults.sanitize("!@#$%")).isEqualTo("");
    }

    @Test
    void sanitize_leadingAndTrailingSpecialChars_produceNoDashes() {
        // Runs of non-alphanumeric characters at either edge collapse away entirely.
        assertThat(TagDefaults.sanitize("!!!ABC!!!")).isEqualTo("abc");
    }

    @Test
    void sanitize_singleCharacterInputs() {
        assertThat(TagDefaults.sanitize("A")).isEqualTo("a");
        assertThat(TagDefaults.sanitize("1")).isEqualTo("1");
        assertThat(TagDefaults.sanitize("-")).isEqualTo("");
    }

    @Test
    void sanitize_internalDashesCollapse() {
        // Multiple kinds of non-alphanumeric runs collapse into a single dash.
        assertThat(TagDefaults.sanitize("foo !!!   bar")).isEqualTo("foo-bar");
    }

    @Test
    void sanitize_alreadyKebabCase_isIdempotent() {
        assertThat(TagDefaults.sanitize("already-kebab-case")).isEqualTo("already-kebab-case");
    }

    @Test
    void sanitize_unicodeFallsBackToDashes() {
        // Non-ASCII alphanumeric characters are not preserved; they collapse like punctuation.
        assertThat(TagDefaults.sanitize("caf\u00e9-con-leche")).isEqualTo("caf-con-leche");
    }

    @Test
    void sanitizePath_stripsLeadingSlash() {
        assertThat(TagDefaults.sanitizePath("/Data/Static/Int32")).isEqualTo("data/static/int32");
    }

    @Test
    void sanitizePath_handlesNoLeadingSlash() {
        assertThat(TagDefaults.sanitizePath("Data/Static/Int32")).isEqualTo("data/static/int32");
    }

    @Test
    void sanitizePath_sanitizesEachSegment() {
        assertThat(TagDefaults.sanitizePath("/My Folder/Node Name!")).isEqualTo("my-folder/node-name");
    }

    @Test
    void sanitizePath_emptyPath() {
        assertThat(TagDefaults.sanitizePath("")).isEqualTo("");
    }

    @Test
    void sanitizePath_singleSegment() {
        assertThat(TagDefaults.sanitizePath("/Objects")).isEqualTo("objects");
    }

    @ParameterizedTest
    @CsvSource({
        "/Data/Static/Int32Node,                                  data-static-int32node",
        "/S7-1500/DataBlocksGlobal/Icon,                          s7-1500-datablocksglobal-icon",
        "/S7-1500/DataBlocksInstance/Icon,                        s7-1500-datablocksinstance-icon",
        "/Objects/My Node,                                        objects-my-node",
        "/Aliases/FindAlias/InputArguments,                       aliases-findalias-inputarguments",
        "/Aliases/TagVariables/FindAlias/InputArguments,          aliases-tagvariables-findalias-inputarguments",
    })
    void tagName_usesFullPath(final String path, final String expected) {
        assertThat(TagDefaults.tagName(path)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "/Int32Node,    int32node",
        "/Variable,     variable",
    })
    void tagName_singleSegment(final String path, final String expected) {
        assertThat(TagDefaults.tagName(path)).isEqualTo(expected);
    }

    @Test
    void tagName_emptyPath() {
        assertThat(TagDefaults.tagName("")).isEqualTo("");
        assertThat(TagDefaults.tagName("/")).isEqualTo("");
    }

    @Test
    void tagName_duplicateDisplayNames_disambiguated() {
        // Same display name "Icon" in different folders → different tag_name_default
        assertThat(TagDefaults.tagName("/S7-1500/DataBlocksGlobal/Icon")).isEqualTo("s7-1500-datablocksglobal-icon");
        assertThat(TagDefaults.tagName("/S7-1500/DataBlocksInstance/Icon"))
                .isEqualTo("s7-1500-datablocksinstance-icon");
        assertThat(TagDefaults.tagName("/S7-1500/TechnologicalObjects/Icon"))
                .isEqualTo("s7-1500-technologicalobjects-icon");

        // All three are unique
        assertThat(TagDefaults.tagName("/S7-1500/DataBlocksGlobal/Icon"))
                .isNotEqualTo(TagDefaults.tagName("/S7-1500/DataBlocksInstance/Icon"));
    }

    @Test
    void tagName_deepNesting_disambiguated() {
        // Same parent folder + same name, but different ancestors → unique
        assertThat(TagDefaults.tagName("/Aliases/FindAlias/InputArguments"))
                .isNotEqualTo(TagDefaults.tagName("/Aliases/TagVariables/FindAlias/InputArguments"));
    }

    @Test
    void tagName_specialCharsInSegments() {
        assertThat(TagDefaults.tagName("/My Folder/Sub.Folder/Node Name!")).isEqualTo("my-folder-sub-folder-node-name");
    }

    @Test
    void deduplicate_noDuplicates_unchanged() {
        final List<String> input = List.of("alpha", "beta", "gamma");
        assertThat(TagDefaults.deduplicate(input)).containsExactly("alpha", "beta", "gamma");
    }

    @Test
    void deduplicate_allDuplicates_appendsSuffix() {
        final List<String> input = List.of("tag", "tag", "tag");
        assertThat(TagDefaults.deduplicate(input)).containsExactly("tag", "tag-2", "tag-3");
    }

    @Test
    void deduplicate_mixedDuplicates() {
        final List<String> input = List.of("alpha", "beta", "alpha", "gamma", "beta", "alpha");
        assertThat(TagDefaults.deduplicate(input))
                .containsExactly("alpha", "beta", "alpha-2", "gamma", "beta-2", "alpha-3");
    }

    @Test
    void deduplicate_emptyList() {
        assertThat(TagDefaults.deduplicate(List.of())).isEmpty();
    }

    @Test
    void deduplicate_singleElement() {
        assertThat(TagDefaults.deduplicate(List.of("only"))).containsExactly("only");
    }

    @Test
    void deduplicate_prosysSimulationScenario() {
        // Simulates the real-world case: 6 simulation instances with same path produce same default
        final List<String> input = List.of(
                "server-valuesimulations-valuesimulation-max-value",
                "server-valuesimulations-valuesimulation-max-value",
                "server-valuesimulations-valuesimulation-max-value",
                "server-valuesimulations-valuesimulation-max-value",
                "server-valuesimulations-valuesimulation-max-value",
                "server-valuesimulations-valuesimulation-max-value");
        final List<String> result = TagDefaults.deduplicate(input);
        assertThat(result).hasSize(6);
        assertThat(result.get(0)).isEqualTo("server-valuesimulations-valuesimulation-max-value");
        assertThat(result.get(1)).isEqualTo("server-valuesimulations-valuesimulation-max-value-2");
        assertThat(result.get(5)).isEqualTo("server-valuesimulations-valuesimulation-max-value-6");
        // All unique
        assertThat(result).doesNotHaveDuplicates();
    }

    @ParameterizedTest
    @CsvSource({"my-opcua, /Data/Static/Int32, my-opcua/data/static/int32", "adapter1, /Objects, adapter1/objects"})
    void northboundTopic(final String adapterId, final String path, final String expected) {
        assertThat(TagDefaults.northboundTopic(adapterId, path)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "my-opcua, /Data/Static/Int32, my-opcua/write/data/static/int32",
        "adapter1, /Objects, adapter1/write/objects"
    })
    void southboundTopic(final String adapterId, final String path, final String expected) {
        assertThat(TagDefaults.southboundTopic(adapterId, path)).isEqualTo(expected);
    }
}
