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

import java.util.List;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/// Resolving a request's `methodName` to one of the object's methods.
///
/// A browse name is **not** unique on an object. A `QualifiedName` is a namespace index plus a string, and OPC
/// 10000-3 requires unique browse names only for TypeDefinitionNodes and InstanceDeclarations, "not for other
/// instances" -- so two Interfaces (§4.9), each published by a different organisation in its own namespace, may
/// both contribute a `Start` to one object. Keying the lookup on the string alone let the later browse result
/// replace the earlier, and a request then invoked whichever the server happened to report second. If both
/// methods take the same arguments, nothing revealed the substitution.
///
/// So an unadorned name resolves only when it is unique, and a node id is how a caller names one of two
/// same-named methods. These pin both spellings and every refusal.
class MethodReferenceTest {

    private static final @NotNull String VENDOR_A = "http://vendor-a.example/Boiler";
    private static final @NotNull String VENDOR_B = "http://vendor-b.example/Boiler";

    private @NotNull NamespaceTable namespaces;

    @BeforeEach
    void setUp() {
        // Index 0 is always the OPC UA namespace; the two vendor namespaces land at 1 and 2.
        namespaces = new NamespaceTable();
        namespaces.add(VENDOR_A);
        namespaces.add(VENDOR_B);
    }

    /// The object these methods hang off. Resolution never reads it -- it matches on the method node -- but a
    /// signature carries it because a method found on the object's type is called with the type as the object.
    private static final @NotNull NodeId OBJECT = new NodeId(1, "Boiler1");

    private static @NotNull MethodSignature method(final @NotNull String name, final @NotNull NodeId nodeId) {
        return method(name, nodeId, MethodSignature.Origin.OBJECT);
    }

    private static @NotNull MethodSignature method(
            final @NotNull String name, final @NotNull NodeId nodeId, final MethodSignature.@NotNull Origin origin) {

        final List<Argument> inputArguments =
                List.of(new Argument("input1", NodeIds.Int32, -1, null, LocalizedText.NULL_VALUE));
        // The browse name takes the node's own namespace, which is what a server does: a method contributed by
        // namespace 2 has both its node id and its browse name there. That makes the collisions below genuine
        // cross-namespace ones rather than two methods sharing a fully qualified name.
        return new MethodSignature(
                new QualifiedName(nodeId.getNamespaceIndex().intValue(), name),
                nodeId,
                OBJECT,
                origin,
                inputArguments,
                List.of(),
                MethodSignature.builtinsOf(inputArguments),
                true);
    }

    /// Two methods both called `Start`, contributed by two namespaces -- the case the whole class exists for.
    private @NotNull List<MethodSignature> collidingStarts() {
        return List.of(
                method("Start", new NodeId(1, "A.Start")),
                method("Start", new NodeId(2, "B.Start")),
                method("Stop", new NodeId(1, "A.Stop")));
    }

    @Test
    void anUnadornedName_resolvesWhenExactlyOneMethodCarriesIt() {
        final var resolution = MethodReference.resolve(collidingStarts(), "Stop", namespaces);

        assertThat(resolution)
                .asInstanceOf(
                        org.assertj.core.api.InstanceOfAssertFactories.type(MethodReference.Resolution.Matched.class))
                .extracting(matched -> matched.method().nodeId())
                .isEqualTo(new NodeId(1, "A.Stop"));
    }

    @Test
    void anUnadornedName_twoMethodsCarryIt_isRefusedNamingBothNodeIds() {
        // The decisive test. Before this, the lookup was a map keyed on the browse name and one of these two
        // silently won by browse order -- reversing the browse changed which method an identical request
        // invoked. Refused rather than resolved: the request did not say which method it meant, and guessing
        // sends a command to the wrong one.
        final var resolution = MethodReference.resolve(collidingStarts(), "Start", namespaces);

        assertThat(resolution).isInstanceOf(MethodReference.Resolution.Ambiguous.class);
        final String reason = ((MethodReference.Resolution.Ambiguous) resolution).reason();
        // Both node ids, so an operator can paste one straight back into `methodName` -- in the URI form,
        // which is the spelling that survives a server renumbering its namespace table.
        assertThat(reason).contains("nsu=" + VENDOR_A + ";s=A.Start").contains("nsu=" + VENDOR_B + ";s=B.Start");
        assertThat(reason).contains("'Start'");
    }

    @Test
    void aNameSharedByTheObjectAndTheTypesDeclarationOfIt_resolvesToTheObjects() {
        // The ordinary shape of a conventional device, not an edge case: a type declares a method Mandatory,
        // every instance is required to have its own node for it, and both get browsed -- so the name appears
        // twice. The two are not rivals. The declaration on the type is a template; the object's node is the
        // real method, with the device's own arguments. Refusing this would force a caller to node ids for a
        // device that is modelled exactly as the specification intends.
        final MethodSignature itsOwn = method("Start", new NodeId(1, "Boiler1.Start"));
        final MethodSignature declaration =
                method("Start", new NodeId(1, "BoilerType.Start"), MethodSignature.Origin.TYPE_DECLARATION);

        final var resolution = MethodReference.resolve(List.of(itsOwn, declaration), "Start", namespaces);

        assertThat(resolution).isInstanceOf(MethodReference.Resolution.Matched.class);
        assertThat(((MethodReference.Resolution.Matched) resolution).method().nodeId())
                .as("the object's own node is the one the device actually has")
                .isEqualTo(new NodeId(1, "Boiler1.Start"));
    }

    @Test
    void theSamePairInEitherOrder_resolvesTheSameWay() {
        // Browse order must not decide it, which is the mistake the whole class exists to prevent.
        final MethodSignature itsOwn = method("Start", new NodeId(1, "Boiler1.Start"));
        final MethodSignature declaration =
                method("Start", new NodeId(1, "BoilerType.Start"), MethodSignature.Origin.TYPE_DECLARATION);

        final var reversed = MethodReference.resolve(List.of(declaration, itsOwn), "Start", namespaces);

        assertThat(reversed).isInstanceOf(MethodReference.Resolution.Matched.class);
        assertThat(((MethodReference.Resolution.Matched) reversed).method().nodeId())
                .isEqualTo(new NodeId(1, "Boiler1.Start"));
    }

    @Test
    void aNameSharedByAnObjectMethodAndAStaticTypeMethod_isRefused() {
        // The boundary, and why the exception above is narrow. A static type method is not a template for the
        // object's method -- it is a different method, invoked on the class rather than on the device. A bare
        // name cannot say which is meant, so the caller is asked for a node id.
        final MethodSignature itsOwn = method("Start", new NodeId(1, "Boiler1.Start"));
        final MethodSignature onTheType =
                method("Start", new NodeId(1, "BoilerType.Start"), MethodSignature.Origin.TYPE_STATIC);

        final var resolution = MethodReference.resolve(List.of(itsOwn, onTheType), "Start", namespaces);

        assertThat(resolution).isInstanceOf(MethodReference.Resolution.Ambiguous.class);
    }

    @Test
    void twoDeclarationsOfOneNameWithNoObjectMethod_areStillRefused() {
        // Two supertypes contributing the same name, and nothing on the object to prefer. The exception needs
        // an object method to resolve to; without one there is nothing to choose.
        final MethodSignature first =
                method("Start", new NodeId(1, "TypeA.Start"), MethodSignature.Origin.TYPE_DECLARATION);
        final MethodSignature second =
                method("Start", new NodeId(2, "TypeB.Start"), MethodSignature.Origin.TYPE_DECLARATION);

        final var resolution = MethodReference.resolve(List.of(first, second), "Start", namespaces);

        assertThat(resolution).isInstanceOf(MethodReference.Resolution.Ambiguous.class);
    }

    @Test
    void aNodeId_picksTheMethodItNames() {
        // How a caller says which of two same-named methods it wants. The URI is resolved against the live
        // namespace table, so the same request works whatever index the server gives that namespace.
        final var byVendorB = MethodReference.resolve(collidingStarts(), "nsu=" + VENDOR_B + ";s=B.Start", namespaces);

        assertThat(byVendorB).isInstanceOf(MethodReference.Resolution.Matched.class);
        assertThat(((MethodReference.Resolution.Matched) byVendorB).method().nodeId())
                .isEqualTo(new NodeId(2, "B.Start"));

        final var byVendorA = MethodReference.resolve(collidingStarts(), "nsu=" + VENDOR_A + ";s=A.Start", namespaces);

        assertThat(((MethodReference.Resolution.Matched) byVendorA).method().nodeId())
                .isEqualTo(new NodeId(1, "A.Start"));
    }

    @Test
    void aNodeId_isResolvedThroughTheLiveNamespaceTable_notAStoredIndex() {
        // Why the qualified form carries a URI rather than an index. A server may renumber its namespace table
        // between sessions, and OPC 10000-5 tells clients to re-verify cached indexes for exactly that reason.
        // Here the same request text is resolved against a table where the vendor namespace moved from 2 to 1,
        // and it still reaches the right method -- which an index in the request could not do.
        final NamespaceTable renumbered = new NamespaceTable();
        renumbered.add(VENDOR_B);

        final List<MethodSignature> methods = List.of(method("Start", new NodeId(1, "B.Start")));

        final var resolution = MethodReference.resolve(methods, "nsu=" + VENDOR_B + ";s=B.Start", renumbered);

        assertThat(resolution).isInstanceOf(MethodReference.Resolution.Matched.class);
    }

    @Test
    void aNamespaceIndex_isAcceptedLikeATagsOwnNodeId() {
        // Reversed deliberately (EDG-1087 M9a). This used to be refused, on the sound-sounding grounds that an
        // index is a per-session handle and a stored request carrying one can come to name a different method.
        // The hazard is real; refusing it here was still wrong. A tag's own node id is parsed with
        // `NodeId.parse`, so `ns=2;s=Boiler` is what every configuration already contains -- an operator who
        // writes that for a tag and then has `methodName` refuse the same spelling meets an inconsistency Edge
        // invented, while the exposure stays exactly where it was.
        final var resolution = MethodReference.resolve(collidingStarts(), "ns=2;s=B.Start", namespaces);

        assertThat(resolution).isInstanceOf(MethodReference.Resolution.Matched.class);
        assertThat(((MethodReference.Resolution.Matched) resolution).method().nodeId())
                .isEqualTo(new NodeId(2, "B.Start"));
    }

    @Test
    void aRefusalPrintsTheUriForm_notTheIndexForm() {
        // The other half of M9a, and the part that made the refusal self-defeating: the message asked for
        // `nsu=` and then listed candidates rendered as `ns=2;s=...`, so pasting one back earned a second
        // refusal. Both forms parse now, but a message is an invitation to copy -- so it offers the spelling
        // that survives a renumbered namespace table.
        final var resolution = MethodReference.resolve(collidingStarts(), "Start", namespaces);

        final String reason = ((MethodReference.Resolution.Ambiguous) resolution).reason();
        assertThat(reason)
                .as("the node ids a caller is told to paste back must be the stable spelling")
                .contains("nsu=" + VENDOR_A)
                .contains("nsu=" + VENDOR_B);
        assertThat(reason).doesNotContain("ns=1;").doesNotContain("ns=2;");
    }

    @Test
    void everyIdentifierTypeIsAccepted() {
        // OPC 10000-6 gives a node identifier four forms. A method's identifier is the server's choice, so
        // refusing any of them would make some methods unaddressable.
        final NodeId numeric =
                new NodeId(1, org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint(42));
        final NodeId guid = new NodeId(1, java.util.UUID.fromString("09087e75-8e5e-499b-954f-f2a9603db28a"));
        final NodeId opaque =
                new NodeId(1, org.eclipse.milo.opcua.stack.core.types.builtin.ByteString.of(new byte[] {0, 1, 2}));

        final List<MethodSignature> methods =
                List.of(method("Start", numeric), method("Start", guid), method("Start", opaque));

        assertThat(MethodReference.resolve(methods, "nsu=" + VENDOR_A + ";i=42", namespaces))
                .isInstanceOf(MethodReference.Resolution.Matched.class);
        assertThat(MethodReference.resolve(
                        methods, "nsu=" + VENDOR_A + ";g=09087e75-8e5e-499b-954f-f2a9603db28a", namespaces))
                .isInstanceOf(MethodReference.Resolution.Matched.class);
        assertThat(MethodReference.resolve(methods, "nsu=" + VENDOR_A + ";b=AAEC", namespaces))
                .isInstanceOf(MethodReference.Resolution.Matched.class);
    }

    @Test
    void aNamespaceTheServerDoesNotPublish_isNotGuessedAt() {
        // The URI resolves to no index on this server, so the request names no node on it. Reported as no such
        // method rather than resolved against some other namespace.
        final var resolution =
                MethodReference.resolve(collidingStarts(), "nsu=http://unknown.example/X;s=Start", namespaces);

        assertThat(resolution).isInstanceOf(MethodReference.Resolution.NoSuchMethod.class);
        assertThat(((MethodReference.Resolution.NoSuchMethod) resolution).reason())
                .contains("http://unknown.example/X");
    }

    @Test
    void aNamespaceIndexTheServerDoesNotPublish_isAnsweredRatherThanThrown() {
        // The companion to the test above, and the one that used to escape. An index needs no lookup to become
        // a node id, so resolution correctly found no match -- and then the refusal message asked the namespace
        // table to render the caller's node id by URI, which threw `namespace index=7 not found`. The throw left
        // the write path with no answer at all and held that tag's writer until it timed out thirty seconds
        // later, so later requests for the tag queued behind a message already known to be unanswerable.
        final var resolution = MethodReference.resolve(collidingStarts(), "ns=7;s=Start", namespaces);

        assertThat(resolution).isInstanceOf(MethodReference.Resolution.NoSuchMethod.class);
        assertThat(((MethodReference.Resolution.NoSuchMethod) resolution).reason())
                .withFailMessage("the refusal must quote the address the caller actually sent")
                .contains("ns=7;s=Start");
    }

    @Test
    void aNodeIdNoMethodOnTheObjectHas_isRefused() {
        // A node id that resolves on this server but is not one of this object's methods. Refused here rather
        // than sent, because a Call naming a method of some other object is a request Edge can see is wrong.
        final var resolution =
                MethodReference.resolve(collidingStarts(), "nsu=" + VENDOR_A + ";s=Elsewhere", namespaces);

        assertThat(resolution).isInstanceOf(MethodReference.Resolution.NoSuchMethod.class);
        assertThat(((MethodReference.Resolution.NoSuchMethod) resolution).reason())
                .contains("nsu=" + VENDOR_A + ";s=Elsewhere");
    }

    @Test
    void anUnknownUnadornedName_namesWhatTheObjectOffers() {
        final var resolution = MethodReference.resolve(collidingStarts(), "Halt", namespaces);

        assertThat(resolution).isInstanceOf(MethodReference.Resolution.NoSuchMethod.class);
        final String reason = ((MethodReference.Resolution.NoSuchMethod) resolution).reason();
        assertThat(reason).contains("'Halt'").contains("Stop");
        // A name two methods share is listed with its node id, because the name alone would not get the caller
        // there -- naming it bare would send them straight back to the ambiguity refusal.
        assertThat(reason).contains("nsu=" + VENDOR_A + ";s=A.Start").contains("nsu=" + VENDOR_B + ";s=B.Start");
    }

    @Test
    void aNameThatResolvesIsNotAnnotatedWithANodeId() {
        // The listing and the resolution rule have to agree. A name carried by the object's own method and by
        // its type's declaration of it resolves to the object's -- so printing a node id beside it would offer
        // a caller something they do not need, and imply the bare name will be refused when it will not.
        final MethodSignature itsOwn = method("Start", new NodeId(1, "Boiler1.Start"));
        final MethodSignature declaration =
                method("Start", new NodeId(1, "BoilerType.Start"), MethodSignature.Origin.TYPE_DECLARATION);

        final var resolution = MethodReference.resolve(List.of(itsOwn, declaration), "Halt", namespaces);

        final String reason = ((MethodReference.Resolution.NoSuchMethod) resolution).reason();
        assertThat(reason).contains("Start");
        assertThat(reason)
                .as("the name resolves, so no node id is needed beside it")
                .doesNotContain("Boiler1.Start")
                .doesNotContain("BoilerType.Start");
    }

    @Test
    void anObjectWithNoMethodsSaysSo() {
        final var resolution = MethodReference.resolve(List.of(), "Start", namespaces);

        assertThat(((MethodReference.Resolution.NoSuchMethod) resolution).reason())
                .contains("none");
    }

    @Test
    void aValueWithASemicolonThatIsNotANodeId_isReportedAsMalformed() {
        // The `;` is what selects the node-id spelling, so anything containing one is held to it rather than
        // being tried as a browse name -- a method genuinely named `a;b` would be unaddressable either way, and
        // silently falling back would hide the caller's mistake.
        final var resolution = MethodReference.resolve(collidingStarts(), "Start;nonsense", namespaces);

        assertThat(resolution).isInstanceOf(MethodReference.Resolution.Malformed.class);
        assertThat(((MethodReference.Resolution.Malformed) resolution).reason()).contains("nsu=");
    }

    @Test
    void aMethodNamedByAShortGuid_isReportedAsMalformedRatherThanResolvingElsewhere() {
        // Milo's parse pads a short Guid, so this would name a different method node -- and a method does
        // exist at the padded id as often as not, which is what makes the silence dangerous.
        final var resolution =
                MethodReference.resolve(collidingStarts(), "ns=2;g=72962b91-fa75-4ae6-8d28-b404dc7daf6", namespaces);

        assertThat(resolution).isInstanceOf(MethodReference.Resolution.Malformed.class);
        assertThat(((MethodReference.Resolution.Malformed) resolution).reason()).contains("8-4-4-4-12");
    }
}
