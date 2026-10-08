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

    /// One method, with the node it would be called on.
    private static @NotNull MethodSignature method(
            final @NotNull String name, final @NotNull NodeId owner, final @NotNull String... inputs) {

        return new MethodSignature(
                name,
                owner.equals(OBJECT) ? NodeId.parse("ns=2;s=Boiler1." + name) : NodeId.parse("ns=2;s=Type." + name),
                owner,
                List.of(inputs).stream()
                        .map(input -> new Argument(input, NodeIds.Int32, -1, null, LocalizedText.NULL_VALUE))
                        .toList(),
                List.of());
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
    void theObjectsOwnMethodWins_andKeepsItsOwnNodeAndObject() {
        // Nearest wins. A Mandatory declaration is mirrored onto the instance, so the same name appears at both
        // levels -- and the instance's node is the one to call. Keeping the type's copy instead would send the
        // call to the type, which for a mirrored method is not where the instance's state lives.
        final List<MethodSignature> merged =
                MethodVerifier.keepingNearest(List.of(method("Start", OBJECT)), List.of(method("Start", TYPE)));

        assertThat(merged).hasSize(1);
        assertThat(merged.get(0).objectId()).isEqualTo(OBJECT);
        assertThat(merged.get(0).nodeId()).isEqualTo(NodeId.parse("ns=2;s=Boiler1.Start"));
    }

    @Test
    void shadowingIsByNameAlone_notBySignature() {
        // Sound because the specification constrains what a subtype may change: it may not remove an inherited
        // argument, retype one declared with a concrete DataType, or add a mandatory one. So the nearer node of
        // the same name is the same method specialised, and comparing arguments would only let a server's
        // sloppy modelling produce two entries a caller cannot tell apart.
        final List<MethodSignature> merged = MethodVerifier.keepingNearest(
                List.of(method("Start", OBJECT, "setpoint")), List.of(method("Start", TYPE, "setpoint", "ramp")));

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
    void theNearestOfThreeLevelsWins() {
        // A name at every level resolves to the object's, not to whichever level was browsed last.
        final List<MethodSignature> withType =
                MethodVerifier.keepingNearest(List.of(method("Start", OBJECT)), List.of(method("Start", TYPE)));
        final List<MethodSignature> withSupertype =
                MethodVerifier.keepingNearest(withType, List.of(method("Start", SUPERTYPE)));

        assertThat(withSupertype).hasSize(1);
        assertThat(withSupertype.get(0).objectId()).isEqualTo(OBJECT);
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
        // object itself are both kept, and a request naming them unqualified is refused with both node ids. A
        // further-away method of that name is still shadowed -- there is already something nearer.
        final MethodSignature first =
                new MethodSignature("Start", NodeId.parse("ns=1;s=A.Start"), OBJECT, List.of(), List.of());
        final MethodSignature second =
                new MethodSignature("Start", NodeId.parse("ns=2;s=B.Start"), OBJECT, List.of(), List.of());

        final List<MethodSignature> merged =
                MethodVerifier.keepingNearest(List.of(first, second), List.of(method("Start", TYPE)));

        assertThat(merged).hasSize(2);
        assertThat(merged).allSatisfy(method -> assertThat(method.objectId()).isEqualTo(OBJECT));
    }
}
