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

import com.hivemq.adapter.sdk.api.schema.ObjectSchemaBuilder;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.schema.SchemaBuilder;
import com.hivemq.edge.adapters.opcua.southbound.JsonSchemaGenerator;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.milo.opcua.stack.core.OpcUaDataType;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// The schemas a `METHOD` tag publishes, derived from the methods its object exposes.
///
/// ## Why a union rather than one shape per method
///
/// One tag serves every method of its object, so a single schema has to describe requests for all of them. The
/// exact contract -- *these* arguments when the method is `Start`, *those* when it is `Stop` -- needs a schema
/// offering alternatives selected by a field, which the schema builder cannot express yet. What it can express
/// is every argument of every method as an **optional** property.
///
/// So the schema is **incomplete, not wrong**: every type it states is true, and what it omits is the pairing of
/// arguments to methods. A request naming `Stop` while carrying `Start`'s arguments passes the schema and is
/// rejected by the adapter instead, with a message naming the mismatch.
///
/// ## Where two methods disagree
///
/// Two methods may declare an argument of the same name and different types. Narrowing to one of them would make
/// the schema state something false about the other, so the union says `any` -- less information, still true.
/// That is rare in practice and is the only place the union loses a type it could have stated.
public final class MethodSchemas {

    private MethodSchemas() {}

    /// The southbound schema: what a request may carry.
    ///
    /// `methodName` is required -- a request that names no method cannot be dispatched. The three fields Edge
    /// reads from a request are **read-only**, which is what keeps them out of the mapping editor's destination
    /// list: the editor offers the writable leaves of this schema as places a field mapping may write to, and
    /// these are not destinations a user wires, they are the request's own envelope.
    public static @NotNull Schema requestSchema(final @NotNull Collection<MethodSignature> methods) {
        final ObjectSchemaBuilder<SchemaBuilder> root = new SchemaBuilder().startObject();

        root.property("methodName")
                .required()
                .scalar(ScalarType.STRING)
                .description("the method to call, as the device names it")
                .writable(false);
        root.property("correlationData")
                .scalar(ScalarType.STRING)
                .description("a token echoed unchanged on the response, so a caller can match the two")
                .writable(false);
        root.property("responseTopic")
                .scalar(ScalarType.STRING)
                .description("where to publish the response, overriding the northbound mapping's own topic")
                .writable(false);

        final ObjectSchemaBuilder<?> value = root.property("value").required().startObject();
        describeArguments(value, unionOfInputs(methods));
        value.endObject();

        return root.endObject().writable().build();
    }

    /// The northbound schema: what a response carries.
    ///
    /// `value` holds the output arguments of whichever method ran, so it is the union of every method's outputs
    /// for the same reason the request's is. `errorCode` and `statusInfo` appear only on a failure, and `value`
    /// is null then -- which is why none of the three is required.
    public static @NotNull Schema responseSchema(final @NotNull Collection<MethodSignature> methods) {
        final ObjectSchemaBuilder<SchemaBuilder> root = new SchemaBuilder().startObject();

        root.property("methodName")
                .required()
                .scalar(ScalarType.STRING)
                .description("the method this is the answer to");

        final ObjectSchemaBuilder<?> value = root.property("value").nullable().startObject();
        describeArguments(value, unionOfOutputs(methods));
        value.endObject();

        root.property("correlationData")
                .scalar(ScalarType.STRING)
                .description("the token the request carried, unchanged");
        root.property("errorCode")
                .scalar(ScalarType.STRING)
                .description("the OPC UA status code by name when the call failed; absent when it succeeded");

        // Two optional halves rather than one shape, because OPC UA reports a failure's detail in two unrelated
        // forms: a status per input argument, and whatever the method itself returned to explain the refusal.
        // Each appears only when the device supplied it.
        final ObjectSchemaBuilder<?> statusInfo = root.property("statusInfo").startObject();
        statusInfo
                .property("arguments")
                .description("per-argument status, keyed by the argument name the request used")
                .startObject()
                .endObject();
        statusInfo
                .property("deviceOutput")
                .description("what the method returned to explain the failure")
                .any();
        statusInfo.endObject();

        return root.endObject().build();
    }

    /// Describes each argument of the union as an optional property of `value`.
    private static void describeArguments(
            final @NotNull ObjectSchemaBuilder<?> value, final @NotNull Map<String, OpcUaDataType> arguments) {
        arguments.forEach((name, type) -> {
            if (type == null) {
                // Two methods disagreed about this argument's type. `any` is the honest answer.
                value.property(name).description("type varies by method").any();
            } else {
                JsonSchemaGenerator.describeAs(value.property(name), type);
            }
        });
    }

    /// The union of every method's input arguments, mapping a name to its type -- or to null where two methods
    /// declare that name with different types.
    static @NotNull Map<String, @Nullable OpcUaDataType> unionOfInputs(
            final @NotNull Collection<MethodSignature> methods) {
        return union(methods, MethodSignature::inputArguments);
    }

    /// The union of every method's output arguments, with the same collision rule as the inputs.
    static @NotNull Map<String, @Nullable OpcUaDataType> unionOfOutputs(
            final @NotNull Collection<MethodSignature> methods) {
        return union(methods, MethodSignature::outputArguments);
    }

    private static @NotNull Map<String, @Nullable OpcUaDataType> union(
            final @NotNull Collection<MethodSignature> methods,
            final @NotNull java.util.function.Function<MethodSignature, java.util.List<Argument>> arguments) {

        // Insertion-ordered, so the schema lists arguments in the order the device reported its methods. That is
        // stable for a given server, which is what makes a schema diff readable when the device changes.
        final Map<String, OpcUaDataType> types = new LinkedHashMap<>();

        for (final MethodSignature method : methods) {
            for (final Argument argument : arguments.apply(method)) {
                final String name = argument.getName();
                if (name == null || name.isEmpty()) {
                    // An argument the device did not name cannot be addressed by a request, so it cannot appear
                    // in the schema either. It is still callable positionally -- the signature keeps it.
                    continue;
                }
                final OpcUaDataType type = builtinTypeOf(argument);
                if (!types.containsKey(name)) {
                    types.put(name, type);
                } else if (types.get(name) != type) {
                    // Disagreement is absorbing: null never equals a later method's real type, so once a name
                    // collapses to `any` it stays there however many methods follow. No separate record of which
                    // names conflicted is needed -- and one was tried, then removed when no test could tell the
                    // two versions apart.
                    types.put(name, null);
                }
            }
        }
        return types;
    }

    /// The builtin type of an argument's DataType, or null when it is not one of the builtins.
    ///
    /// A DataType naming a custom structure resolves to no builtin, and `any` is then the only true answer --
    /// the same answer a collision gets, for the same reason.
    private static @Nullable OpcUaDataType builtinTypeOf(final @NotNull Argument argument) {
        if (argument.getDataType() == null) {
            return null;
        }
        return OpcUaDataType.fromNodeId(argument.getDataType());
    }
}
