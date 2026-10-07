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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.milo.opcua.stack.core.OpcUaDataType;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
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

    /// The southbound schema: what a request's arguments may carry.
    ///
    /// ## Why this describes only the arguments
    ///
    /// A `DataPointSchema`'s southbound schema describes the **contents of `value`**, not a whole message:
    /// `TagSchemaCreationOutputImpl.southboundSchema` wraps whatever an adapter supplies in a `value` property.
    /// A condition tag is the precedent -- `ConditionSchemas.writeSchema()` declares `method`, `eventId`,
    /// `comment` and `duration` at its root, they render under `value`, and the adapter reads them back out of
    /// `payload.value()`.
    ///
    /// A method request's other three fields are **not** inside `value`. They are siblings of it: the whole MQTT
    /// payload is deserialised into {@code OpcUaPayload}, whose `methodName`, `correlationData` and
    /// `responseTopic` are top-level record components, and {@link com.hivemq.protocols.MethodRequestInstructions}
    /// writes them to the payload root for exactly that reason. So they cannot be declared here without landing a
    /// level too deep -- which is what an earlier version of this method did, producing a schema that required
    /// `methodName` inside `value` while every writer put it outside, so every request failed validation with
    /// `$.value: required property 'methodName' not found`.
    ///
    /// They are therefore not described by a schema at all. Nothing is lost: they are not destinations a user
    /// wires -- the synthetic instructions carry them -- and the mapping editor offers this schema's writable
    /// leaves, which are the arguments and only the arguments.
    public static @NotNull Schema requestSchema(final @NotNull Collection<MethodSignature> methods) {
        final ObjectSchemaBuilder<SchemaBuilder> root = new SchemaBuilder().startObject();
        describeArguments(root, unionOfInputs(methods));
        return root.endObject().writable().build();
    }

    /// The northbound value schema: the output arguments a response carries.
    ///
    /// Describes only the contents of `value`, for the mirror of the reason given on
    /// {@link #requestSchema}: `TagSchemaCreationOutputImpl.northboundSchema` wraps an adapter's `valueSchema`
    /// in a `value` property, so declaring `value` here would land the outputs one level too deep.
    ///
    /// The union of every method's outputs, since one tag answers for all of them. On a failure `value` is
    /// published as null -- see {@link MethodResponse} -- which the nullable root states.
    ///
    /// Everything else a response carries (`methodName`, `correlationData`, `errorCode`, `statusInfo`) is not in
    /// `value`: {@link MethodResponse} writes it into the datapoint's `context`, which is a separate slot of a
    /// `DataPointSchema` and is described by {@link #responseContextSchema}.
    public static @NotNull Schema responseSchema(final @NotNull Collection<MethodSignature> methods) {
        final ObjectSchemaBuilder<SchemaBuilder> root = new SchemaBuilder().startObject();
        describeArguments(root, unionOfOutputs(methods));
        return root.endObject().nullable().build();
    }

    /// The northbound context schema: what a response carries beside its outputs.
    ///
    /// These are the fields {@link MethodResponse} puts in the datapoint's `context` child, which the northbound
    /// consumer flattens onto the published message beside `value`. `errorCode` and `statusInfo` appear only on a
    /// failure and `correlationData` only when the caller sent one, so none is required.
    public static @NotNull Schema responseContextSchema() {
        final ObjectSchemaBuilder<SchemaBuilder> root = new SchemaBuilder().startObject();

        root.property("methodName")
                .required()
                .scalar(ScalarType.STRING)
                .description("the method this is the answer to");
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

    /// What one argument name resolves to across every method that declares it.
    ///
    /// `type` is the element type -- null meaning "cannot be stated", from either a custom structure or a
    /// disagreement between two methods. `dimensions` is empty for a scalar and otherwise holds one entry per
    /// dimension, so it carries the OPC UA value rank in the form {@link JsonSchemaGenerator#describeAsArray}
    /// consumes.
    ///
    /// Shape is part of the identity, not an attribute of the type: two methods declaring `data` as an Int32
    /// scalar and an Int32 array disagree just as surely as if the types differed, and the honest answer for
    /// both is the same `any`. Keeping the rank alongside the type is what lets that comparison happen at all --
    /// an earlier version kept only the type, so an array argument was described as a scalar of its element type
    /// and every correct array request was rejected by Edge before it could reach the device.
    record ArgumentShape(
            @Nullable OpcUaDataType type, @NotNull List<UInteger> dimensions) {

        /// Null type and no dimensions: nothing true can be said about this name.
        static final @NotNull ArgumentShape UNSTATEABLE = new ArgumentShape(null, List.of());

        boolean isArray() {
            return !dimensions.isEmpty();
        }

        /// The dimensions in the form {@link JsonSchemaGenerator#describeAsArray} takes.
        @NotNull
        UInteger @NotNull [] dimensionsArray() {
            return dimensions.toArray(new UInteger[0]);
        }

        /// Whether two declarations of one name describe the same shape. Array-ness and depth count, individual
        /// dimension lengths do not: a method taking five elements and another taking three still agree that
        // the
        /// argument is a one-dimensional array of the same type, and the union states that rather than giving
        // up.
        boolean agreesWith(final @NotNull ArgumentShape other) {
            return type == other.type && dimensions.size() == other.dimensions.size();
        }

        /// The shape of an argument as the device declared it.
        static @NotNull ArgumentShape of(final @NotNull Argument argument) {
            final OpcUaDataType type =
                    argument.getDataType() == null ? null : OpcUaDataType.fromNodeId(argument.getDataType());

            // A rank of -1 (scalar), -2 (any) or -3 (scalar or one dimension) is not a declaration of an array,
            // and only a positive rank is. OPC 10000-3 section 5.6.2: a positive rank is the number of
            // dimensions. ArrayDimensions may be absent or shorter than the rank claims, so the rank decides the
            // nesting depth and the dimensions only supply lengths where they are given.
            final Integer rank = argument.getValueRank();
            if (rank == null || rank < 1) {
                return new ArgumentShape(type, List.of());
            }

            final UInteger[] declared = argument.getArrayDimensions();
            final List<UInteger> dimensions = new ArrayList<>(rank);
            for (int i = 0; i < rank; i++) {
                // Zero means "any length" in OPC UA and constrains nothing, which is also what a dimension the
                // device left out has to mean.
                dimensions.add(
                        declared != null && i < declared.length && declared[i] != null
                                ? declared[i]
                                : UInteger.valueOf(0));
            }
            return new ArgumentShape(type, List.copyOf(dimensions));
        }
    }

    /// Describes each argument of the union as an optional property of `value`.
    private static void describeArguments(
            final @NotNull ObjectSchemaBuilder<?> value, final @NotNull Map<String, ArgumentShape> arguments) {
        arguments.forEach((name, shape) -> {
            if (shape.isArray()) {
                // Element type may still be null -- an array of a custom structure. describeAsArray answers that
                // with `any` elements, which keeps the array-ness (the part it can state) rather than losing it.
                JsonSchemaGenerator.describeAsArray(value.property(name), shape.type(), shape.dimensionsArray());
            } else if (shape.type() == null) {
                // Two methods disagreed, or the type is a custom structure. `any` is the honest answer.
                value.property(name).description("type varies by method").any();
            } else {
                JsonSchemaGenerator.describeAs(value.property(name), shape.type());
            }
        });
    }

    /// The union of every method's input arguments, mapping a name to its shape -- or to
    /// {@link ArgumentShape#UNSTATEABLE} where two methods declare that name differently.
    static @NotNull Map<String, ArgumentShape> unionOfInputs(final @NotNull Collection<MethodSignature> methods) {
        return union(methods, MethodSignature::inputArguments);
    }

    /// The union of every method's output arguments, with the same collision rule as the inputs.
    static @NotNull Map<String, ArgumentShape> unionOfOutputs(final @NotNull Collection<MethodSignature> methods) {
        return union(methods, MethodSignature::outputArguments);
    }

    private static @NotNull Map<String, ArgumentShape> union(
            final @NotNull Collection<MethodSignature> methods,
            final @NotNull java.util.function.Function<MethodSignature, java.util.List<Argument>> arguments) {

        // Insertion-ordered, so the schema lists arguments in the order the device reported its methods. That is
        // stable for a given server, which is what makes a schema diff readable when the device changes.
        final Map<String, ArgumentShape> shapes = new LinkedHashMap<>();

        for (final MethodSignature method : methods) {
            for (final Argument argument : arguments.apply(method)) {
                final String name = argument.getName();
                if (name == null || name.isEmpty()) {
                    // An argument the device did not name cannot be addressed by a request, so it cannot appear
                    // in the schema either. It is still callable positionally -- the signature keeps it.
                    continue;
                }
                final ArgumentShape shape = ArgumentShape.of(argument);
                final ArgumentShape seen = shapes.get(name);
                if (seen == null) {
                    shapes.put(name, shape);
                } else if (!seen.agreesWith(shape)) {
                    // Disagreement is absorbing: UNSTATEABLE never agrees with a later method's real shape, so
                    // once a name collapses to `any` it stays there however many methods follow.
                    shapes.put(name, ArgumentShape.UNSTATEABLE);
                }
            }
        }
        return shapes;
    }
}
