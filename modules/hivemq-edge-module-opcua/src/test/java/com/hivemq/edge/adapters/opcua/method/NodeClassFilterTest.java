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

import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/// Who filters a browse by node class: the server, or us.
///
/// **EDG-1100 P12.** `BrowseDescription` can ask a server to return only Method nodes, and OPC 10000-4 section
/// 5.8.2 is unambiguous that it must -- "Only TargetNodes with the selected NodeClasses are returned". A
/// Siemens S7-1500 (firmware V02.08.03) does not honour it for a user method in an instance DB: it reports
/// that node's class as **Object**, so a `Method` mask returns nothing while an unfiltered browse lists the
/// method correctly. With the mask in place, no user-written method on that PLC could be found -- the object
/// resolved to zero methods, the generated schema admitted no arguments, and every call was quarantined while
/// the adapter stayed connected.
///
/// **Why this is a unit test and not an integration one.** The embedded OPC-UA server cannot imitate the
/// device: Milo derives a browse's reported node class from the real node, so a method node is always reported
/// as a method. The two halves of the decision -- what we ask the server for, and what we keep from its answer
/// -- are therefore asserted directly. `MethodVerifierIT` covers the other side, that a conformant server
/// still works.
class NodeClassFilterTest {

    private static @NotNull ReferenceDescription reportedAs(final @NotNull NodeClass nodeClass) {
        return new ReferenceDescription(
                NodeIds.HasComponent,
                true,
                ExpandedNodeId.of("Boiler.Method"),
                new QualifiedName(2, "Method"),
                LocalizedText.english("Method"),
                nodeClass,
                NodeIds.BaseObjectType.expanded());
    }

    @Test
    void theBrowseAsksTheServerToFilterNothing() {
        // The decisive assertion. A non-zero mask here is what made the S7-1500's user methods invisible, and
        // nothing else in the test suite would notice the regression: a conformant server answers the same
        // either way, so every other test passes with the mask restored.
        final BrowseDescription browse = MethodVerifier.methodsOf(NodeId.parse("ns=2;s=Boiler"));

        assertThat(browse.getNodeClassMask().intValue())
                .as("a server that mis-reports a method's node class must not be able to hide it")
                .isZero();
    }

    @Test
    void theBrowseStillAsksOnlyForComponentsOfTheNode() {
        // Dropping the node-class filter must not widen anything else: a method hangs off its object by
        // HasComponent (OPC 10000-3 section 4.5.3), and subtypes stay included because a server may use a more
        // specific reference type.
        final BrowseDescription browse = MethodVerifier.methodsOf(NodeId.parse("ns=2;s=Boiler"));

        assertThat(browse.getReferenceTypeId()).isEqualTo(NodeIds.HasComponent);
        assertThat(browse.getIncludeSubtypes()).isTrue();
        assertThat(browse.getBrowseDirection()).isEqualTo(BrowseDirection.Forward);
    }

    @Test
    void aReferenceTheServerCallsAMethod_isResolved() {
        assertThat(MethodVerifier.isMethod(reportedAs(NodeClass.Method))).isTrue();
    }

    @Test
    void aReferenceTheServerCallsAnObject_isResolvedToo() {
        // Exactly the S7-1500's behaviour, and the case the whole change exists for. Being strict here would
        // reinstate the defect one layer further in: the mask would be gone from the request and the same node
        // would be dropped from the answer.
        assertThat(MethodVerifier.isMethod(reportedAs(NodeClass.Object))).isTrue();
    }

    @Test
    void aReferenceWhoseClassTheServerDoesNotState_isResolvedRatherThanDropped() {
        // A server that under-reports costs a round trip, not a method. readSignature resolves the node and
        // discards anything that is not a method node, so being permissive here is safe.
        assertThat(MethodVerifier.isMethod(reportedAs(NodeClass.Unspecified))).isTrue();
    }

    @ParameterizedTest
    @EnumSource(
            value = NodeClass.class,
            names = {"Variable", "VariableType", "ObjectType", "ReferenceType", "DataType", "View"})
    void aReferenceThatIsPositivelySomethingElse_isDropped(final @NotNull NodeClass nodeClass) {
        // The economy of the change: an unfiltered browse returns a node's variables too, and resolving each
        // of them would cost a round trip per variable per tag. Only a class that cannot be a mis-reported
        // method is dropped here.
        assertThat(MethodVerifier.isMethod(reportedAs(nodeClass))).isFalse();
    }
}
