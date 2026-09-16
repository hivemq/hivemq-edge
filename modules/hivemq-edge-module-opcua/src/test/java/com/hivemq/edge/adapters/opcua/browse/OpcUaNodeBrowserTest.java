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
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hivemq.edge.adapters.browse.BrowseException;
import com.hivemq.edge.adapters.browse.BrowsedNode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.jetbrains.annotations.NotNull;
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

    // --- deterministic collision suffixes ---

    @Test
    void browse_samePathNodes_tagNameDefaultSuffixIndependentOfArrivalOrder() throws BrowseException {
        // Two variables under the same browse path collide on tagNameDefault and get "-2" appended to one of
        // them. The order the async browse callbacks deliver them in varies between browses, so the suffix
        // must be decided by a stable key (the NodeId), not by arrival order — otherwise a CSV exported from
        // one browse names a different node than the next browse would.
        final ReferenceDescription first = variable("ns=2;s=Sim1/Max", "Max Value");
        final ReferenceDescription second = variable("ns=2;s=Sim2/Max", "Max Value");

        final Map<String, String> forward = tagNameDefaultsByNodeId(browse(first, second));
        final Map<String, String> reversed = tagNameDefaultsByNodeId(browse(second, first));

        assertThat(forward)
                .containsEntry("ns=2;s=Sim1/Max", "max-value")
                .containsEntry("ns=2;s=Sim2/Max", "max-value-2");
        assertThat(reversed)
                .as("the same node keeps the same default whichever order the server delivered the references")
                .isEqualTo(forward);
    }

    private static @NotNull ReferenceDescription variable(final @NotNull String nodeId, final @NotNull String name) {
        return new ReferenceDescription(
                NodeIds.HasComponent,
                true,
                ExpandedNodeId.parse(nodeId),
                new QualifiedName(2, name),
                LocalizedText.english(name),
                NodeClass.Variable,
                ExpandedNodeId.NULL_VALUE);
    }

    /** Browse a root whose only children are {@code refs}; every other node is a leaf and reads answer null. */
    private static @NotNull List<BrowsedNode> browse(final @NotNull ReferenceDescription... refs)
            throws BrowseException {
        final OpcUaClient client = mock(OpcUaClient.class);
        final NamespaceTable nsTable = new NamespaceTable();
        nsTable.add("urn:test");
        nsTable.add("urn:test:sim");
        when(client.getNamespaceTable()).thenReturn(nsTable);
        final BrowseResult root = new BrowseResult(StatusCode.GOOD, ByteString.NULL_VALUE, refs);
        final BrowseResult leaf = new BrowseResult(StatusCode.GOOD, ByteString.NULL_VALUE, new ReferenceDescription[0]);
        when(client.browseAsync(any(BrowseDescription.class))).thenAnswer(invocation -> {
            final BrowseDescription bd = invocation.getArgument(0);
            return CompletableFuture.completedFuture(NodeIds.ObjectsFolder.equals(bd.getNodeId()) ? root : leaf);
        });
        when(client.readAsync(anyDouble(), any(), anyList())).thenAnswer(invocation -> {
            final List<ReadValueId> ids = invocation.getArgument(2);
            final DataValue[] values = new DataValue[ids.size()];
            Arrays.fill(values, new DataValue(Variant.NULL_VALUE));
            return CompletableFuture.completedFuture(new ReadResponse(null, values, null));
        });
        return new OpcUaNodeBrowser(client, "adapter").browse(null, 0).collect(Collectors.toList());
    }

    private static @NotNull Map<String, String> tagNameDefaultsByNodeId(final @NotNull List<BrowsedNode> nodes) {
        return nodes.stream().collect(Collectors.toMap(BrowsedNode::nodeId, BrowsedNode::tagNameDefault));
    }

    // --- adapter-scoped browse serialisation (EDG-576) ---

    @Test
    void browse_sharedSemaphore_serialisesBrowseAsyncAcrossBrowsers() throws Exception {
        // EDG-576: two distinct browsers sharing the adapter-owned permit must never have their browseAsync
        // calls overlap on the shared client. The peak in-flight count therefore stays at 1.
        final OpcUaClient client = mock(OpcUaClient.class);
        final AtomicInteger inFlight = new AtomicInteger();
        final AtomicInteger peak = new AtomicInteger();
        final BrowseResult empty =
                new BrowseResult(StatusCode.GOOD, ByteString.NULL_VALUE, new ReferenceDescription[0]);
        when(client.browseAsync(any(BrowseDescription.class))).thenAnswer(invocation -> {
            peak.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
            try {
                Thread.sleep(100); // hold the permit long enough that an overlap would be observable
            } finally {
                inFlight.decrementAndGet();
            }
            return CompletableFuture.completedFuture(empty);
        });

        final Semaphore shared = new Semaphore(1);
        final OpcUaNodeBrowser first = new OpcUaNodeBrowser(client, "adapter", 0, shared);
        final OpcUaNodeBrowser second = new OpcUaNodeBrowser(client, "adapter", 0, shared);

        final ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            final var f1 = pool.submit(() -> first.browse(null, 0).count());
            final var f2 = pool.submit(() -> second.browse(null, 0).count());
            f1.get(10, TimeUnit.SECONDS);
            f2.get(10, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        assertThat(peak.get())
                .as("the shared adapter-scoped permit serialises browseAsync across concurrent browsers")
                .isEqualTo(1);
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

    @Test
    void sanitize_leadingAndTrailingSpecialChars_produceNoDashes() {
        // Runs of non-alphanumeric characters at either edge collapse away entirely.
        assertThat(OpcUaNodeBrowser.sanitize("!!!ABC!!!")).isEqualTo("abc");
    }

    @Test
    void sanitize_singleCharacterInputs() {
        assertThat(OpcUaNodeBrowser.sanitize("A")).isEqualTo("a");
        assertThat(OpcUaNodeBrowser.sanitize("1")).isEqualTo("1");
        assertThat(OpcUaNodeBrowser.sanitize("-")).isEqualTo("");
    }

    @Test
    void sanitize_internalDashesCollapse() {
        // Multiple kinds of non-alphanumeric runs collapse into a single dash.
        assertThat(OpcUaNodeBrowser.sanitize("foo !!!   bar")).isEqualTo("foo-bar");
    }

    @Test
    void sanitize_alreadyKebabCase_isIdempotent() {
        assertThat(OpcUaNodeBrowser.sanitize("already-kebab-case")).isEqualTo("already-kebab-case");
    }

    @Test
    void sanitize_unicodeFallsBackToDashes() {
        // Non-ASCII alphanumeric characters are not preserved; they collapse like punctuation.
        assertThat(OpcUaNodeBrowser.sanitize("caf\u00e9-con-leche")).isEqualTo("caf-con-leche");
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
