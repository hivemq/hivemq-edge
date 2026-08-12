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
package com.hivemq.edge.adapters.opcua.condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hivemq.edge.adapters.opcua.config.tag.OpcuaConditionType;
import java.util.concurrent.CompletableFuture;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Type verification when a vendor names its own type after a standard one.
 * <p>
 * Review finding 7. A {@code QualifiedName} is a namespace <em>and</em> a string, and OPC 10000-3 §5.2.4 says
 * why both are needed: "different organizations may use the same string having a slightly different meaning".
 * Specification names live in namespace 0 (OPC 10000-5 §5.4.2). Matching on the string alone accepted a
 * namespace-2 type called {@code AlarmConditionType} as the standard one outright — skipping the
 * {@code HasSubtype} walk that exists precisely for vendor types, and subscribing with a field set the real
 * type need not have.
 * <p>
 * The consequence is quiet rather than loud: the server rejects the select-clause entries it cannot resolve,
 * so the tag comes up, streams, and publishes permanently null fields.
 * <p>
 * {@code Browsing.isStandardName} already applied this rule to method lookups. These pin it where it decides
 * a tag's entire published shape.
 */
class ConditionTypeVerifierNamespaceTest {

    private static final @NotNull NodeId CONDITION = NodeId.parse("ns=2;s=Boiler1.HighTemp");

    private @NotNull OpcUaClient client;

    @BeforeEach
    void setUp() {
        client = mock(OpcUaClient.class);
        when(client.getNamespaceTable()).thenReturn(new NamespaceTable());
    }

    @Test
    void aVendorTypeNamedLikeAStandardOneIsNotTakenForIt() {
        // Namespace 2, the standard name, and ancestry that leads nowhere. Accepting it would mean
        // publishing AlarmConditionType's whole field set against a type that shares nothing but a string.
        typeDefinition(new QualifiedName(2, "AlarmConditionType"), "ns=2;i=5000");
        hasNoSupertype();

        final ConditionTypeVerifier.Result result = verify(OpcuaConditionType.ALARM_CONDITION);

        assertThat(result).isInstanceOf(ConditionTypeVerifier.Result.Rejected.class);
        assertThat(((ConditionTypeVerifier.Result.Rejected) result).reason())
                // The namespace belongs in the message. Without it the reason reads as a flat contradiction
                // -- "points at a node of type 'AlarmConditionType' [...] none of the types it derives from
                // is a standard condition type" -- and an operator has no way to see that the type they are
                // looking at in the address space is not the one they think it is.
                .contains("2:AlarmConditionType")
                .contains("none of the types it derives from is a standard OPC UA condition type");
    }

    @Test
    void aVendorTypeNamedLikeAStandardOneIsStillVerifiedThroughItsRealAncestry() {
        // The other half: the name collision must not stop the ordinary vendor-subtype path from working.
        // OPC 10000-9 §5.5 says to expect vendor types, so one deriving from a standard ancestor is
        // conformant and must be accepted -- on the strength of its ancestry, not its name.
        typeDefinition(new QualifiedName(2, "AlarmConditionType"), "ns=2;i=5000");
        hasSupertype(new QualifiedName(0, "ExclusiveLevelAlarmType"), NodeIds.ExclusiveLevelAlarmType);

        final ConditionTypeVerifier.Result result = verify(OpcuaConditionType.ALARM_CONDITION);

        assertThat(result).isInstanceOf(ConditionTypeVerifier.Result.Verified.class);
        assertThat(((ConditionTypeVerifier.Result.Verified) result).deviceType())
                .as("the standard ancestor is what decides the published fields")
                .isEqualTo(OpcuaConditionType.EXCLUSIVE_LEVEL_ALARM);
    }

    @Test
    void aVendorSupertypeNamedLikeAStandardOneDoesNotEndTheWalkEarly() {
        // The same rule one level up. A vendor supertype called LimitAlarmType in its own namespace would
        // otherwise satisfy the walk and hand back LimitAlarmType's sixteen limits, none of which the real
        // hierarchy promises.
        typeDefinition(new QualifiedName(2, "VendorAlarmType"), "ns=2;i=5000");
        hasSupertype(new QualifiedName(3, "LimitAlarmType"), NodeId.parse("ns=3;i=6000"));

        final ConditionTypeVerifier.Result result = verify(OpcuaConditionType.ALARM_CONDITION);

        assertThat(result)
                .as("a namespace-3 LimitAlarmType is not the standard one, so the walk must continue past it")
                .isInstanceOf(ConditionTypeVerifier.Result.Rejected.class);
    }

    @Test
    void aGenuinelyStandardTypeIsAcceptedWithoutWalkingAnything() {
        typeDefinition(
                new QualifiedName(0, "ExclusiveLevelAlarmType"), NodeIds.ExclusiveLevelAlarmType.toParseableString());

        final ConditionTypeVerifier.Result result = verify(OpcuaConditionType.ALARM_CONDITION);

        assertThat(result).isInstanceOf(ConditionTypeVerifier.Result.Verified.class);
    }

    @Test
    void aStandardNameInNamespaceZeroWithTheWrongNodeIdIsTreatedAsVendorDefined() {
        // Stronger than the namespace rule alone, and cheap: namespace 0 is reserved, so a standard name in
        // it should be the standard node. A server saying otherwise is doing something no client should
        // follow on trust.
        typeDefinition(new QualifiedName(0, "AlarmConditionType"), "ns=0;i=99999");
        hasNoSupertype();

        final ConditionTypeVerifier.Result result = verify(OpcuaConditionType.ALARM_CONDITION);

        assertThat(result).isInstanceOf(ConditionTypeVerifier.Result.Rejected.class);
    }

    @Test
    void aBrowseFailureIsReportedAsUnreadableRatherThanAsAMissingTypeDefinition() {
        // Review finding 8, at this caller's boundary. Browsing used to swallow every failure into an empty
        // list, so a disconnect reached compare() as "the device returned no type definition" -- which tells
        // an operator to check their node id when the real problem was the connection. The tag is dropped
        // either way; the instruction is what differs.
        when(client.browseAsync(any(BrowseDescription.class)))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("not connected")));

        final ConditionTypeVerifier.Result result = verify(OpcuaConditionType.ALARM_CONDITION);

        assertThat(result).isInstanceOf(ConditionTypeVerifier.Result.Rejected.class);
        assertThat(((ConditionTypeVerifier.Result.Rejected) result).reason())
                .contains("could not read the type")
                .contains("not connected");
    }

    @Test
    void aConditionTheServerDoesNotExposeIsUnverifiableRatherThanRejected() {
        // Review-03 finding 1, at the subscription boundary. OPC 10000-9 §4.3 permits a server to keep its
        // condition instances out of the address space, and such a server answers this browse with
        // Bad_NodeIdUnknown by design. Folding that in with transport errors made the tag impossible to
        // subscribe -- so the writer's type-level fallback, added for the same finding one review earlier,
        // could never be reached: the tag never got as far as being subscribed.
        when(client.browseAsync(any(BrowseDescription.class)))
                .thenReturn(CompletableFuture.completedFuture(
                        new BrowseResult(new StatusCode(StatusCodes.Bad_NodeIdUnknown), ByteString.NULL_VALUE, null)));

        final ConditionTypeVerifier.Result result = verify(OpcuaConditionType.ALARM_CONDITION);

        assertThat(result).isInstanceOf(ConditionTypeVerifier.Result.Unverifiable.class);
        assertThat(((ConditionTypeVerifier.Result.Unverifiable) result).reason())
                .as("the operator needs the node id and the reason, not a verdict about the declaration")
                .contains("does not expose node")
                .contains("§4.3");
    }

    @Test
    void anyOtherRefusalIsStillARejection() {
        // The boundary. A server that will not let this session browse the node has said nothing about
        // whether the node exists, so the tag is dropped rather than waved through unverified.
        when(client.browseAsync(any(BrowseDescription.class)))
                .thenReturn(CompletableFuture.completedFuture(new BrowseResult(
                        new StatusCode(StatusCodes.Bad_UserAccessDenied), ByteString.NULL_VALUE, null)));

        final ConditionTypeVerifier.Result result = verify(OpcuaConditionType.ALARM_CONDITION);

        assertThat(result).isInstanceOf(ConditionTypeVerifier.Result.Rejected.class);
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────

    private @NotNull ConditionTypeVerifier.Result verify(final @NotNull OpcuaConditionType declared) {
        return ConditionTypeVerifier.verify(client, CONDITION, declared, "boiler-high-temp")
                .join();
    }

    /** Stubs the forward {@code HasTypeDefinition} browse the verifier starts from. */
    private void typeDefinition(final @NotNull QualifiedName browseName, final @NotNull String nodeId) {
        when(client.browseAsync(matching(BrowseDirection.Forward)))
                .thenReturn(CompletableFuture.completedFuture(page(reference(browseName, nodeId))));
    }

    /** Stubs the inverse {@code HasSubtype} browse the ancestry walk uses. */
    private void hasSupertype(final @NotNull QualifiedName browseName, final @NotNull NodeId nodeId) {
        when(client.browseAsync(matching(BrowseDirection.Inverse)))
                .thenReturn(CompletableFuture.completedFuture(page(reference(browseName, nodeId.toParseableString()))));
    }

    private void hasNoSupertype() {
        when(client.browseAsync(matching(BrowseDirection.Inverse)))
                .thenReturn(CompletableFuture.completedFuture(page()));
    }

    /**
     * Matches a browse by direction, which is what separates the verifier's two questions: forward for "what
     * type is this node", inverse for "what does that type derive from".
     */
    private static @NotNull BrowseDescription matching(final @NotNull BrowseDirection direction) {
        return org.mockito.ArgumentMatchers.argThat(
                browse -> browse != null && browse.getBrowseDirection() == direction);
    }

    private static @NotNull BrowseResult page(final @NotNull ReferenceDescription... references) {
        return new BrowseResult(StatusCode.GOOD, ByteString.NULL_VALUE, references);
    }

    private static @NotNull ReferenceDescription reference(
            final @NotNull QualifiedName browseName, final @NotNull String nodeId) {
        return new ReferenceDescription(
                NodeIds.HasTypeDefinition,
                true,
                ExpandedNodeId.parse(nodeId),
                browseName,
                LocalizedText.english(String.valueOf(browseName.getName())),
                NodeClass.ObjectType,
                ExpandedNodeId.NULL_VALUE);
    }
}
