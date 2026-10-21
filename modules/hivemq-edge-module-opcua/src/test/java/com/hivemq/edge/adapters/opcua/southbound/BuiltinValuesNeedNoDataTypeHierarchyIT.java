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
package com.hivemq.edge.adapters.opcua.southbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import util.EmbeddedOpcUaServerExtension;
import util.TestNamespace;

/// A value of a builtin type is written without the server's DataType hierarchy.
///
/// The hierarchy is a recursive browse of the whole type tree, and a server may stay connected while refusing
/// or timing out that one browse -- an authorization rule on the type nodes is enough. Reading it in the
/// converter's constructor made that failure fatal to *every* write, including a write of an `Int32`, whose
/// type is fixed by OPC 10000-6 and needs no server at all. It also killed method calls on the same adapter,
/// because the converter was built before the code branched on tag kind.
///
/// Reported by Miguel as R6-3 on EDG-1087 for the method path; the same eagerness was on this path, and
/// having one rule for both is the point.
class BuiltinValuesNeedNoDataTypeHierarchyIT {

    @RegisterExtension
    public final @NotNull EmbeddedOpcUaServerExtension opcUaServerExtension = new EmbeddedOpcUaServerExtension();

    private @NotNull OpcUaClient client;

    @BeforeEach
    void setUp() throws Exception {
        client = OpcUaClient.create(opcUaServerExtension.getServerUri());
        client.connect();
    }

    @AfterEach
    void tearDown() throws Exception {
        client.disconnect();
    }

    private @NotNull TestNamespace namespace() {
        return Objects.requireNonNull(opcUaServerExtension.getTestNamespace());
    }

    @Test
    void anIntegerIsConverted_whileTheDataTypeHierarchyCannotBeRead() throws Exception {
        final String nodeId = namespace().addNode("Setpoint", NodeIds.Int32, () -> 0, 9201L);

        // A spy on the real connected client: every browse and read is genuine and only the hierarchy read
        // fails, which is the shape of the real fault.
        final OpcUaClient treeless = spy(client);
        doThrow(new UaException(StatusCodes.Bad_UserAccessDenied))
                .when(treeless)
                .getDataTypeTree();

        final Object converted = VariableWrites.valueFor(
                        treeless, NodeId.parse(nodeId), new ObjectMapper().readTree("180"))
                .getValue();

        assertThat(converted)
                .withFailMessage("an Int32 is identified by its node id alone, so an unreadable hierarchy "
                        + "must not stop the write")
                .isEqualTo(180);
    }

    @Test
    void theHierarchyIsNotReadAtAll_forABuiltinValue() throws Exception {
        // Not merely survivable but never asked for: the builtin table answers, so the round trip does not
        // happen. On a device that serves metadata slowly that is the difference the laziness buys.
        final String nodeId = namespace().addNode("Flow", NodeIds.Double, () -> 0.0, 9202L);
        final OpcUaClient watched = spy(client);

        VariableWrites.valueFor(watched, NodeId.parse(nodeId), new ObjectMapper().readTree("1.5"));

        verify(watched, never()).getDataTypeTree();
    }

    @Test
    void constructingTheConverter_readsNothing() throws Exception {
        // The constructor used to read the hierarchy, so building the object could throw before any value had
        // been looked at -- which is what took the method path down with it.
        final OpcUaClient watched = spy(client);

        JsonToOpcUaValueConverter.of(watched);

        verify(watched, never()).getDataTypeTree();
        verify(watched, never()).getDynamicEncodingContext();
        verify(watched, never()).getAddressSpace();
    }

    @Test
    void anArrayOfABuiltin_alsoNeedsNoHierarchy() throws Exception {
        final String nodeId = namespace().addArrayNode("Profile", NodeIds.Int32, () -> new int[] {0, 0, 0}, 9203L, 3);
        final OpcUaClient watched = spy(client);

        final Object converted = VariableWrites.valueFor(
                        watched, NodeId.parse(nodeId), new ObjectMapper().readTree("[1, 2, 3]"))
                .getValue();

        assertThat(converted).isEqualTo(new Integer[] {1, 2, 3});
        verify(watched, never()).getDataTypeTree();
    }

    @Test
    void aStructure_stillReadsTheHierarchy_andStillFailsWithoutIt() throws Exception {
        // The other half of the rule: a structure genuinely needs the hierarchy, so an unreadable one is still
        // a failure there. Nothing about this change makes a structure encodable without it.
        final String nodeId = namespace().addNode("Setpoint2", NodeIds.Int32, () -> 0, 9204L);
        final OpcUaClient treeless = spy(client);
        doThrow(new UaException(StatusCodes.Bad_UserAccessDenied))
                .when(treeless)
                .getDataTypeTree();

        // Int32 passes without the hierarchy, proving the spy is wired and the failure is reachable only past
        // the builtin table.
        assertThat(VariableWrites.valueFor(treeless, NodeId.parse(nodeId), new ObjectMapper().readTree("1"))
                        .getValue())
                .isEqualTo(1);
        verify(treeless, never()).getDataTypeTree();

        // And the hierarchy is reached when a type is not in the table: ServerStatus is a structure.
        final NodeId structureNode = NodeIds.Server_ServerStatus;
        assertThat(catchThrowableOf(treeless, structureNode))
                .withFailMessage("a structure cannot be encoded without the hierarchy, and must say so")
                .isNotNull();
        verify(treeless).getDataTypeTree();
    }

    private @NotNull Throwable catchThrowableOf(final @NotNull OpcUaClient client, final @NotNull NodeId node) {
        try {
            VariableWrites.valueFor(client, node, new ObjectMapper().readTree("{}"));
            return null;
        } catch (final Exception e) {
            return e;
        }
    }
}
