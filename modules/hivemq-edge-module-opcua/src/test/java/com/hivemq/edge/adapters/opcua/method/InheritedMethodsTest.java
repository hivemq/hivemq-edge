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
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/// Merging the methods an object exposes with those only its type declares.
///
/// **Why the type is consulted at all.** A method need not be mirrored onto the instances of its type. OPC
/// 10000-3 section 4.5.3 describes one whose scope is the ObjectType, "similar to static methods of a class";
/// carrying no modelling rule, section 6.2.2 keeps it off every instance. OPC 10000-4's Call service expects a
/// client to find it anyway -- the object id may name "the Object or ObjectType on which the Method is invoked",
/// and for an Object the method may hang off that Object, "the ObjectType of the Object or a super type of that
/// ObjectType". Browsing the instance alone reported such a method as absent.
///
/// Mandatory and Optional declarations *are* mirrored onto the instance (section 6.4.4.4.1), which is why the
/// common case worked before this and why the climb adds nothing for it.
///
/// **What these test.** The merge rule, which decides what a caller can reach, and the object id each method
/// carries. The climb around it needs a live address space, so it is covered by the integration tests rather
/// than here.
class InheritedMethodsTest {

    private static final @NotNull NodeId OBJECT = NodeId.parse("ns=2;s=Boiler1");
    private static final @NotNull NodeId TYPE = NodeId.parse("ns=2;i=5000");
    private static final @NotNull NodeId SUPERTYPE = NodeId.parse("ns=2;i=4000");

    /// One method, with the node it would be called on. `owner` being OBJECT makes it the object's own; anything
    /// else makes it a **static** type method, which is the case the climb exists for.
    private static @NotNull MethodSignature method(
            final @NotNull String name, final @NotNull NodeId owner, final @NotNull String... inputs) {

        return method(
                name,
                owner,
                owner.equals(OBJECT) ? MethodSignature.Origin.OBJECT : MethodSignature.Origin.TYPE_STATIC,
                inputs);
    }

    /// One method with its origin stated, for the cases where that is what is under test.
    ///
    /// The call target follows from the origin rather than from where the node was found, exactly as discovery
    /// sets it: only a **static** type method names the type, because only it exists nowhere else. A
    /// declaration describes a method of the instance, so it is called on the object.
    private static @NotNull MethodSignature method(
            final @NotNull String name,
            final @NotNull NodeId owner,
            final MethodSignature.@NotNull Origin origin,
            final @NotNull String... inputs) {

        return new MethodSignature(
                name,
                owner.equals(OBJECT)
                        ? NodeId.parse("ns=2;s=Boiler1." + name)
                        : NodeId.parse("ns=2;s=" + owner.getIdentifier() + "." + name),
                origin == MethodSignature.Origin.TYPE_STATIC ? owner : OBJECT,
                origin,
                List.of(inputs).stream()
                        .map(input -> new Argument(input, NodeIds.Int32, -1, null, LocalizedText.NULL_VALUE))
                        .toList(),
                List.of());
    }

    /// A Mandatory or Optional declaration on the type: a template for the instance's own method. Called on the
    /// **object**, never on the type -- see MethodSignature.Origin.
    private static @NotNull MethodSignature declaredOnType(
            final @NotNull String name, final @NotNull String... inputs) {

        return method(name, TYPE, MethodSignature.Origin.TYPE_DECLARATION, inputs);
    }

    private static @NotNull List<String> namesOf(final @NotNull List<MethodSignature> methods) {
        return methods.stream().map(MethodSignature::name).toList();
    }

    @Test
    void aMethodOnlyTheTypeDeclares_becomesCallable() {
        // The decisive test. Before the climb, an object exposing nothing had no methods at all and a request
        // naming Calibrate was answered "it offers: none" -- for a method the server does support.
        final List<MethodSignature> merged =
                MethodVerifier.keepingNearest(List.of(), List.of(method("Calibrate", TYPE)));

        assertThat(namesOf(merged)).containsExactly("Calibrate");
    }

    @Test
    void itIsCalledOnTheTypeRatherThanTheObject() {
        // The part that makes it actually work. A method found only on the type is invoked by naming the type,
        // which is why the object id travels with the signature instead of being taken from the tag.
        final List<MethodSignature> merged =
                MethodVerifier.keepingNearest(List.of(), List.of(method("Calibrate", TYPE)));

        assertThat(merged.get(0).objectId()).isEqualTo(TYPE);
    }

    @Test
    void aDeclarationOfTheObjectsOwnMethod_isKeptRatherThanDropped() {
        // Changed deliberately (Miguel's M7). A Mandatory declaration is mirrored onto the instance, so the
        // same name appears at both levels -- and this used to drop the type's copy here, which made preferring
        // the object's node a side effect of merge order rather than a stated rule. Both are kept now, and
        // MethodReference decides: a bare name matching one object method and one declaration resolves to the
        // object's. Keeping both is what lets resolution tell that ordinary pairing from two rival methods.
        final List<MethodSignature> merged =
                MethodVerifier.keepingNearest(List.of(method("Start", OBJECT)), List.of(declaredOnType("Start")));

        assertThat(merged).hasSize(2);
        assertThat(merged)
                .extracting(MethodSignature::origin)
                .containsExactly(MethodSignature.Origin.OBJECT, MethodSignature.Origin.TYPE_DECLARATION);
    }

    @Test
    void aStaticTypeMethodSharingAnObjectMethodsName_isAlsoKept() {
        // Not the same as the pairing above, and not resolvable by a bare name: one acts on the device and the
        // other on the class, so they are genuinely different methods. Kept here; refused at resolution.
        final List<MethodSignature> merged =
                MethodVerifier.keepingNearest(List.of(method("Start", OBJECT)), List.of(method("Start", TYPE)));

        assertThat(merged).hasSize(2);
    }

    @Test
    void aNearerTypeLevelStillShadowsAFurtherOne() {
        // Unchanged, and the shadowing the merge is actually for: two levels of the hierarchy declaring one
        // name. The nearer is the one a subtype specialised. The specification constrains what a subtype may
        // change -- it may not remove an inherited argument, retype one declared with a concrete DataType, or
        // add a mandatory one -- so the nearer node is the same method specialised.
        final List<MethodSignature> merged = MethodVerifier.keepingNearest(
                List.of(method("Start", TYPE, "setpoint")), List.of(method("Start", SUPERTYPE, "setpoint", "ramp")));

        assertThat(merged).hasSize(1);
        assertThat(merged.get(0).inputArgumentNames()).containsExactly("setpoint");
    }

    @Test
    void methodsFromSeveralLevels_areAllKept() {
        // The climb merges level by level, nearest first, so a three-level hierarchy contributes all three.
        final List<MethodSignature> fromObject = List.of(method("Start", OBJECT));
        final List<MethodSignature> withType =
                MethodVerifier.keepingNearest(fromObject, List.of(method("Calibrate", TYPE)));
        final List<MethodSignature> withSupertype =
                MethodVerifier.keepingNearest(withType, List.of(method("Reset", SUPERTYPE)));

        assertThat(namesOf(withSupertype)).containsExactly("Start", "Calibrate", "Reset");
    }

    @Test
    void theNearestOfTwoDeclarationLevelsWins_andTheObjectsIsKeptBeside() {
        // A declaration at every level: the two type levels shadow each other, nearest first, and the object's
        // own method is kept alongside the surviving one rather than suppressing it.
        final List<MethodSignature> withType =
                MethodVerifier.keepingNearest(List.of(method("Start", OBJECT)), List.of(declaredOnType("Start")));
        final List<MethodSignature> withSupertype = MethodVerifier.keepingNearest(
                withType, List.of(method("Start", SUPERTYPE, MethodSignature.Origin.TYPE_DECLARATION)));

        assertThat(withSupertype).hasSize(2);
        assertThat(withSupertype).extracting(MethodSignature::objectId).containsOnly(OBJECT);
    }

    @Test
    void nothingFurtherAway_leavesTheObjectsMethodsAlone() {
        // The ordinary case: a type that adds nothing, or a climb that found nothing, must not disturb what the
        // object exposes.
        final List<MethodSignature> fromObject = List.of(method("Start", OBJECT), method("Stop", OBJECT));

        assertThat(namesOf(MethodVerifier.keepingNearest(fromObject, List.of())))
                .containsExactly("Start", "Stop");
    }

    @Test
    void twoMethodsSharingANameOnTheObject_bothSurvive() {
        // The namespace-collision case must not be collapsed by this merge: two methods of one name on the
        // object itself are both kept, and a request naming them unqualified is refused with both node ids.
        final MethodSignature first =
                MethodSignature.ofObject("Start", NodeId.parse("ns=1;s=A.Start"), OBJECT, List.of(), List.of());
        final MethodSignature second =
                MethodSignature.ofObject("Start", NodeId.parse("ns=2;s=B.Start"), OBJECT, List.of(), List.of());

        final List<MethodSignature> merged =
                MethodVerifier.keepingNearest(List.of(first, second), List.of(declaredOnType("Start")));

        assertThat(merged)
                .extracting(MethodSignature::origin)
                .as("the object's two are kept, and so is the type's declaration -- resolution refuses the name")
                .containsExactly(
                        MethodSignature.Origin.OBJECT,
                        MethodSignature.Origin.OBJECT,
                        MethodSignature.Origin.TYPE_DECLARATION);
    }
}
