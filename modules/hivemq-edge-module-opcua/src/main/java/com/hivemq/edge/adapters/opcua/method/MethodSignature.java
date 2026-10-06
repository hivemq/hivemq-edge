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
        /// Input arguments in wire order. Empty for a method taking none -- see the class note on absence.
        @NotNull List<Argument> inputArguments,
        /// Output arguments in wire order, naming what a successful call returns.
        @NotNull List<Argument> outputArguments) {

    public MethodSignature {
        inputArguments = List.copyOf(inputArguments);
        outputArguments = List.copyOf(outputArguments);
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
