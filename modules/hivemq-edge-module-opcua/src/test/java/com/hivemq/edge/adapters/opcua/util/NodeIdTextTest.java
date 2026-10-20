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
package com.hivemq.edge.adapters.opcua.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.junit.jupiter.api.Test;

class NodeIdTextTest {

    private static final String BOILER = "http://vendor.example/Boiler";
    private static final String GUID = "72962b91-fa75-4ae6-8d28-0b404dc7daf6";

    /// Index 0 is the OPC UA namespace, index 1 the vendor's.
    private static NamespaceTable table() {
        final NamespaceTable table = new NamespaceTable();
        table.add(BOILER);
        return table;
    }

    // --- resolve: a node id that addresses a node ---

    @Test
    void resolve_uriForm_givesThisSessionsIndex() {
        assertThat(NodeIdText.resolve("nsu=" + BOILER + ";s=Start", table()))
                .isEqualTo(new NodeId(UShort.valueOf(1), "Start"));
        assertThat(NodeIdText.resolve("nsu=" + BOILER + ";i=42", table())).isEqualTo(new NodeId(UShort.valueOf(1), 42));
        assertThat(NodeIdText.resolve("nsu=http://opcfoundation.org/UA/;i=85", table()))
                .isEqualTo(new NodeId(UShort.valueOf(0), 85));
    }

    @Test
    void resolve_indexForm_givesWhatNodeIdParseGave_evenForAnIndexTheTableLacks() {
        for (final String text :
                new String[] {"ns=1;s=Start", "i=85", "ns=0;i=85", "ns=7;s=X", "ns=1;g=" + GUID, "ns=1;b=AQID"}) {
            assertThat(NodeIdText.resolve(text, table())).as(text).isEqualTo(NodeId.parse(text));
        }
    }

    @Test
    void resolve_unpublishedUri_refusedNamingTheUri() {
        assertThatThrownBy(() -> NodeIdText.resolve("nsu=http://nope/;s=X", table()))
                .isInstanceOfSatisfying(
                        NodeIdText.UnpublishedNamespaceException.class,
                        e -> assertThat(e.namespaceUri()).isEqualTo("http://nope/"))
                .hasMessage("'nsu=http://nope/;s=X' names the namespace 'http://nope/', which the server does not "
                        + "publish");
    }

    @Test
    void resolve_nodeOnAnotherServer_refused() {
        assertThatThrownBy(() -> NodeIdText.resolve("svr=1;ns=1;s=X", table()))
                .isInstanceOf(IllegalArgumentException.class)
                .isNotInstanceOf(NodeIdText.UnpublishedNamespaceException.class)
                .hasMessageContaining("another server");
    }

    // --- every entry point: the syntax and the Guid guard ---

    @Test
    void notANodeId_refusedWithTheReasonAndBothSpellings() {
        for (final String text : new String[] {"Boiler1.Temperature", "nsu=" + BOILER, " ns=1;s=X"}) {
            assertThatThrownBy(() -> NodeIdText.expanded(text))
                    .as(text)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageStartingWith("'" + text + "' is not a valid OPC UA node id: ")
                    .hasMessageContaining("nsu=<namespace uri>;<identifier>")
                    .hasMessageContaining("ns=<index>;<identifier>");
        }
    }

    @Test
    void shortGuid_refusedInEverySpellingRatherThanPadded() {
        // Milo would read each as 0b404dc7-...-padded, a different valid Guid.
        final String shortGuid = "72962b91-fa75-4ae6-8d28-b404dc7daf6";
        for (final String text :
                new String[] {"g=" + shortGuid, "ns=1;g=" + shortGuid, "nsu=" + BOILER + ";g=" + shortGuid}) {
            assertThatThrownBy(() -> NodeIdText.expanded(text))
                    .as(text)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("8-4-4-4-12");
            assertThatThrownBy(() -> NodeIdText.resolve(text, table())).as(text).hasMessageContaining("8-4-4-4-12");
        }
    }

    @Test
    void canonicalGuid_accepted() {
        assertThat(NodeIdText.resolve("nsu=" + BOILER + ";g=" + GUID, table()))
                .isEqualTo(new NodeId(UShort.valueOf(1), UUID.fromString(GUID)));
    }

    @Test
    void expanded_keepsTheUriAndTheServer_forAnExpandedNodeIdValue() {
        assertThat(NodeIdText.expanded("nsu=" + BOILER + ";s=Start").getNamespaceUri())
                .isEqualTo(BOILER);
        assertThat(NodeIdText.expanded("svr=1;ns=1;s=X").isLocal()).isFalse();
    }

    @Test
    void resolve_everyIdentifierKind_byUri() {
        assertThat(NodeIdText.resolve("nsu=" + BOILER + ";g=" + GUID, table()))
                .isEqualTo(new NodeId(UShort.valueOf(1), UUID.fromString(GUID)));
        assertThat(NodeIdText.resolve("nsu=" + BOILER + ";b=AQID", table()))
                .isEqualTo(new NodeId(UShort.valueOf(1), ByteString.of(new byte[] {1, 2, 3})));
    }

    // --- render and matchKey ---

    @Test
    void render_prefersTheUri_fallsBackToTheIndex_neverThrows() {
        assertThat(NodeIdText.render(new NodeId(UShort.valueOf(1), "Start"), table()))
                .isEqualTo("nsu=" + BOILER + ";s=Start");
        assertThat(NodeIdText.render(NodeId.parse("ns=7;s=X"), table())).isEqualTo("ns=7;s=X");
    }

    @Test
    void matchKey_bothSpellingsOfOneNode_giveOneKey() {
        assertThat(NodeIdText.matchKey("nsu=" + BOILER + ";s=Start", table()))
                .isEqualTo(NodeIdText.matchKey("ns=1;s=Start", table()))
                .isEqualTo("ns=1;s=Start");
        assertThat(NodeIdText.matchKey("ns=0;i=85", table())).isEqualTo(NodeIdText.matchKey("i=85", table()));
    }

    @Test
    void matchKey_unresolvable_isTheTextItself() {
        assertThat(NodeIdText.matchKey("nsu=http://nope/;s=X", table())).isEqualTo("nsu=http://nope/;s=X");
        assertThat(NodeIdText.matchKey("garbage", table())).isEqualTo("garbage");
    }
}
