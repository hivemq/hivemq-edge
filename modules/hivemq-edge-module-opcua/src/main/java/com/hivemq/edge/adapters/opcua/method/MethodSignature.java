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

import java.util.List;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// One method of an object, as the device describes it.
///
/// This is what a browse of a `METHOD` tag's object yields per method, and what a call needs: the method's own
/// NodeId, and its arguments in the order the wire expects them.
///
/// **Why the NodeId is here rather than derived.** A condition's methods are standard, so their NodeIds are
/// fixed by the specification and a static table suffices
/// ({@code ConditionUpdateWriter.CONDITION_TYPE_METHODS}). A user-defined method has no such table: its NodeId
/// is whatever the server assigned, so browsing is the only way to learn it. That is why a method tag whose
/// browse fails is unusable rather than merely undescribed.
///
/// **Why the arguments are a list and not a map.** A `Call` passes its arguments positionally, while a request
/// names them -- so the order here *is* the mapping, and {@link #inputPosition(String)} is the only translation
/// needed. Keeping a list also preserves what a map would lose: two arguments may share a name on a
/// badly-described server, and silently dropping one would make a call succeed with the wrong values.
public record MethodSignature(
        /// The method's browse name, as a request names it.
        @NotNull String name,
        /// The method node itself, passed as the `methodId` of a `Call`.
        @NotNull NodeId nodeId,
        /// What to pass as the `objectId` of a `Call`: the **type** for a static method, and the tag's own
        /// object for everything else.
        ///
        /// OPC 10000-4, Call: the object id "shall be that of the Object or ObjectType on which the Method is
        /// invoked", and when it names an Object the method must hang off that Object, its ObjectType, or a
        /// supertype of it. A method a type keeps to itself carries no modelling rule, is mirrored onto no
        /// instance, and is therefore invoked by naming the type.
        ///
        /// **Only {@link Origin#TYPE_STATIC} names the type.** A declaration found on the type
        /// ({@link Origin#TYPE_DECLARATION}) is called on the object, because the method it describes belongs
        /// to the instance. Naming the type for one of those was the defect: on a server whose type node is
        /// executable, a caller asking about one boiler had something run against the class instead.
        @NotNull NodeId objectId,
        /// Where this method node was found, which decides what a bare `methodName` may resolve to.
        @NotNull Origin origin,
        /// Input arguments in wire order. Empty for a method taking none -- see the class note on absence.
        @NotNull List<Argument> inputArguments,
        /// Output arguments in wire order, naming what a successful call returns.
        @NotNull List<Argument> outputArguments) {

    public MethodSignature {
        inputArguments = List.copyOf(inputArguments);
        outputArguments = List.copyOf(outputArguments);
    }

    /// A method the tag's own object declares -- the ordinary case, and the only one a test usually needs.
    public static @NotNull MethodSignature ofObject(
            final @NotNull String name,
            final @NotNull NodeId nodeId,
            final @NotNull NodeId objectId,
            final @NotNull List<Argument> inputArguments,
            final @NotNull List<Argument> outputArguments) {

        return new MethodSignature(name, nodeId, objectId, Origin.OBJECT, inputArguments, outputArguments);
    }

    /// Where a method node was found, and therefore what it is.
    ///
    /// Three cases, and they are not interchangeable. OPC 10000-3 section 6.4.4.4 gives a node on a type an
    /// optional **modelling rule** saying what instances do with it, and section 6.4.4.4.4 is explicit for
    /// methods: a declaration marked Optional is a *template*, and an instance "does not implement the Method",
    /// implements it "with no arguments", or implements it "accepting a mode argument" -- so the declaration's
    /// own argument list is not the instance's. A node carrying **no** modelling rule is different again:
    /// section 6.4.4.2 says it "is neither considered for instantiation of a type nor for subtyping", so it
    /// lives on the type alone, which is the "static method" of section 4.8.
    public enum Origin {
        /// Declared by the tag's own object. The method exists on the device; call it on that object.
        OBJECT,
        /// Found on the type carrying no modelling rule: it exists only there, so the **type** is what a call
        /// names as its object.
        TYPE_STATIC,
        /// Found on the type carrying a modelling rule (Mandatory or Optional). A declaration rather than a
        /// method: if this instance implements it, the object's own browse found the real node. Still offered,
        /// and called on the **object** -- a device that does not implement it refuses, and that refusal is a
        /// better answer than claiming no such method exists.
        TYPE_DECLARATION
    }

    /// The wire position of the input argument with this name, or `null` when the method declares none such.
    ///
    /// A `null` answer is how a request naming an argument the method does not have is caught, which is the
    /// check that turns a server-side `Bad_InvalidArgument` into a message naming the offending field.
    public @Nullable Integer inputPosition(final @NotNull String argumentName) {
        for (int i = 0; i < inputArguments.size(); i++) {
            if (argumentName.equals(inputArguments.get(i).getName())) {
                return i;
            }
        }
        return null;
    }

    /// The input argument names in wire order, for building a request's schema and for naming what is missing.
    public @NotNull List<String> inputArgumentNames() {
        return inputArguments.stream().map(Argument::getName).toList();
    }

    /// The output argument names in wire order, used to key a response's `value` object.
    public @NotNull List<String> outputArgumentNames() {
        return outputArguments.stream().map(Argument::getName).toList();
    }

    /// Whether the device described this method's inputs at all.
    ///
    /// A method genuinely taking no arguments and one whose `InputArguments` property the server omits are
    /// indistinguishable here -- Milo reports both as empty. Callable either way; the difference only shows in
    /// how much its generated schema can say.
    public boolean declaresNoInputs() {
        return inputArguments.isEmpty();
    }
}
