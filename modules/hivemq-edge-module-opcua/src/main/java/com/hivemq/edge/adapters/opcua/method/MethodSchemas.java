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
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
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

    /// The builtin types an argument may be written in either of two shapes, and what to send for each.
    ///
    /// Each is a structure on the wire that the northbound converter publishes as a JSON object, and each has
    /// an obvious simple form a caller would write by hand. Accepting only one of the two would refuse a value
    /// read from this very device, or refuse the form a person would type -- so the schema says `any` and this
    /// text is the only description a caller gets. Membership and description are one map rather than two, so
    /// a type cannot be in the set and lack its text.
    private static final @NotNull Map<OpcUaDataType, String> ACCEPTED_FORMS = Map.of(
            OpcUaDataType.LocalizedText, "a string, taken as English, or {\"locale\": \"en-US\", \"text\": \"...\"}",
            OpcUaDataType.NodeId, "a node id as text, ns=2;s=Boiler, or {\"namespaceIndex\": 2, \"id\": \"Boiler\"}",
            OpcUaDataType.ExpandedNodeId, "a node id as text, nsu=<namespace uri>;s=Boiler",
            OpcUaDataType.QualifiedName,
                    "a string, taken as namespace 0, or {\"namespaceIndex\": 1, \"name\": \"Start\"}",
            OpcUaDataType.StatusCode,
                    "the numeric status code, for example 0 for Good, or {\"code\": 0} as a read publishes it",
            OpcUaDataType.XmlElement, "the XML fragment as a string");

    // @formatter:off -- the property declarations below are instances of one pattern, and the formatter's
    // one-call-per-line rendering hides that. The breaks are chosen to show it: declaration and type on one
    // line, prose on the next.

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
    /// A method request's other three fields are **not** inside `value`. They are siblings of it: a caller
    /// publishes `methodName`, `correlationData` and `responseTopic` at the payload root, and the generated
    /// schemas declare them there. So they cannot be declared here without landing a level too deep -- which is
    /// what an earlier version of this method did, producing a schema that required `methodName` inside `value`
    /// while every writer put it outside, so every request failed validation with
    /// `$.value: required property 'methodName' not found`.
    ///
    /// So they are declared **inside** `INTERNAL_REQUEST_PARAMETERS` below, the field that carries them across
    /// the boundary between Edge and this adapter, and Edge lifts them from there to the document root while
    /// assembling the southbound schema -- see `TagSchemaCreationOutputImpl.southboundSchema`, which iterates
    /// that object's properties and declares each at the root with its schema unchanged.
    ///
    /// Declaring them here rather than in Edge keeps their names in one place: the same constants name them in
    /// the schema, in the payload the writer assembles, and in the adapter that reads them back.
    ///
    /// ## Why the object is closed
    ///
    /// `additionalProperties(false)`: a name this schema does not list is **not** an argument of any method on the
    /// object, so a request carrying one is wrong however it is spelled. Listing properties in JSON Schema is
    /// permissive by default -- it constrains the names it mentions and says nothing about the rest -- which left
    /// the union stating every argument's type while silently admitting any other name beside them. The case that
    /// matters is the common one: a misspelled argument passed validation, reached the adapter, and was refused
    /// there instead of at the gate whose whole purpose is to catch it.
    ///
    /// An object with no arguments is then a schema that permits nothing, which is the true statement about a
    /// method taking none -- the same construction and the same reason as
    /// {@link com.hivemq.edge.adapters.opcua.condition.ConditionSchemas#unwritableSchema()}. A closed object does
    /// not cost the union anything: every name it carries gets a declared property, including one whose type two
    /// methods disagree about, so nothing legitimately named is rejected.
    public static @NotNull Schema requestSchema(final @NotNull Collection<MethodSignature> methods) {
        final ObjectSchemaBuilder<SchemaBuilder> requestSchema = new SchemaBuilder().startObject();
        describeArguments(requestSchema, unionOfInputs(methods), true);

        // The parameters a request carries, in the field that takes them across the adapter boundary. Edge
        // lifts them to the document root by iterating this, so the names live here and nowhere else.
        // `methodName` is required and the other two are not: either may arrive as an MQTT 5 property with
        // nothing in the payload.
        //
        // Each is `writable()` because each is something a caller sends. The builder defaults a property to
        // not writable, and lifting carries each property's schema through verbatim, so omitting this put
        // `readOnly: true` on all three in the published southbound document -- on `methodName` beside
        // `required`, telling a form to demand a field it must not offer. Nothing enforces the annotation
        // today, which is why no request was rejected and no test noticed; it is read by whoever builds a
        // form from the schema.
        final ObjectSchemaBuilder<?> parameters =
                requestSchema.property(MethodRequestFieldsOpcUa.INTERNAL_REQUEST_PARAMETERS).startObject();
        parameters.property(MethodRequestFieldsOpcUa.METHOD_NAME).required().writable().scalar(ScalarType.STRING)
                .description("the method to call. Its name, which must be unique among the object's methods, or "
                        + "the method node itself as nsu=<namespace uri>;<identifier> when two methods share a "
                        + "name");
        parameters.property(MethodRequestFieldsOpcUa.CORRELATION_DATA).writable().scalar(ScalarType.STRING)
                .description("correlationData, echoed unchanged from the request to its response");
        parameters.property(MethodRequestFieldsOpcUa.RESPONSE_TOPIC).writable().scalar(ScalarType.STRING)
                .description("the topic the response is published to, instead of the mapping's own");
        parameters.endObject();

        return requestSchema.additionalProperties(false).endObject().writable().build();
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
    /// Everything else a response carries (`methodName`, `correlationData`, `errorCode`, `statusInfo`) is not a
    /// field of `value` on the wire. It crosses the adapter border inside `value`, in the parameter object
    /// declared below, which Edge lifts to the message root.
    public static @NotNull Schema responseSchema(final @NotNull Collection<MethodSignature> methods) {
        final ObjectSchemaBuilder<SchemaBuilder> responseSchema = new SchemaBuilder().startObject();
        // Not writable: a response's outputs are what the device produced, and nobody writes them.
        describeArguments(responseSchema, unionOfOutputs(methods), false);

        // The parameters a response carries, all optional -- the failure fields appear only on a failure and
        // the token only when the caller sent one.
        //
        // `responseTopic` is deliberately absent while the payload carries it anyway: Edge needs it to choose
        // the publish topic, and a published response must not carry a routing instruction for itself. Sound
        // because nothing validates the payload while this field is present, and it confines the exception to
        // the northbound lifting, which publishes what this declares and routes on what it does not.
        final ObjectSchemaBuilder<?> parameters =
                responseSchema.property(MethodRequestFieldsOpcUa.INTERNAL_RESPONSE_PARAMETERS).startObject();
        parameters.property(MethodRequestFieldsOpcUa.RESPONSE_METHOD_NAME).scalar(ScalarType.STRING)
                .description("the method an answer belongs to -- read as \"response from methodName\". Named "
                        + "differently from a request's methodName so that a response is not a valid request: "
                        + "see MethodRequestFieldsOpcUa");
        parameters.property(MethodRequestFieldsOpcUa.CORRELATION_DATA).scalar(ScalarType.STRING)
                .description("a token echoed unchanged from the request to its response");
        parameters.property(MethodRequestFieldsOpcUa.ERROR_CODE).scalar(ScalarType.STRING)
                .description("the OPC UA status code by name when the call failed");

        // Two optional halves, because OPC UA reports a failure's detail in two unrelated forms: a status per
        // input argument, and whatever the method returned. Each appears only when the device supplied it.
        final ObjectSchemaBuilder<?> statusInfo = parameters.property(MethodRequestFieldsOpcUa.STATUS_INFO).startObject();
        statusInfo.property("arguments")
                .description("per-argument status, keyed by the argument name the request used")
                .startObject().endObject();
        statusInfo.property("deviceOutput")
                .description("what the method returned to explain the failure")
                .any();
        // Declared `any` deliberately. Its entries are keyed by output argument name, so no fixed set of
        // properties describes it, and each carries the body as the device sent it -- base64 bytes, XML or
        // JSON text -- which is a union this schema model cannot state. The reason this is `any` here rather
        // than a shape beside the output itself: an output's own field keeps the type its signature declares
        // and merely goes null when it cannot be read, so a consumer's contract for a successful answer is
        // unaffected by a failure mode. See MethodResponse#undecodedBody.
        statusInfo.property("undecodedOutputs")
                .description("outputs this client could not decode, each with the reason, the type the server "
                        + "named, the body's encoding, and the body as the device sent it. The output's own "
                        + "field is null in that case")
                .any();
        statusInfo.endObject();

        parameters.endObject();

        return responseSchema.endObject().nullable().build();
    }

    // @formatter:on

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

        /// Whether two declarations of one name describe the same shape. Array-ness and depth count,
        /// individual dimension lengths do not: a method taking five elements and another taking three still
        /// agree that the argument is a one-dimensional array of the same type, and the union states that
        /// rather than giving up.
        ///
        /// Lengths now count for nothing anywhere -- the generated schema states none, because the keywords it
        /// could reach were inert and said the wrong thing besides (see
        /// {@code JsonSchemaGenerator.applyArrayType}). So this comparison no longer hides a disagreement the
        /// schema would have had to resolve; it simply agrees with what the schema says.
        boolean agreesWith(final @NotNull ArgumentShape other) {
            return type == other.type && dimensions.size() == other.dimensions.size();
        }

        /// The shape of an argument as the device declared it.
        ///
        /// The builtin type is resolved by the browse that discovered the method and passed in, rather than
        /// looked up here: an argument declared as a **Simple DataType** -- `Duration`, `UtcTime`, `IntegerId`
        /// and the rest -- is a subtype of a builtin and encodes identically to it (OPC 10000-3 section 5.8.2),
        /// but reducing it to that builtin needs the server's DataType hierarchy, which this has no client to
        /// walk. Looking the id up in the table of the 25 builtins answered null for every one of them, and the
        /// argument rendered as `any`.
        static @NotNull ArgumentShape of(final @NotNull Argument argument, final @Nullable OpcUaDataType type) {

            // The rank decides the shape, and OPC 10000-3 table 28 gives it six values -- the same table as for
            // a Variable, because a method argument is described by the same attribute:
            //
            //   n >= 1  an array with exactly n dimensions
            //   0       an array, one dimension or more; never a scalar
            //   -1      a scalar, never an array
            //   -2      a scalar OR an array of any number of dimensions
            //   -3      a scalar OR a one-dimensional array
            //
            // Only -1 is a scalar declaration. The three vague ones cannot be stated by this schema model: it has
            // no union, so "a scalar or an array" has no form, and 0 promises an array of a depth the server
            // declined to fix. Each becomes UNSTATEABLE, which renders as `any` -- the same answer already given
            // to an argument of a custom structure and to one two methods declare differently.
            //
            // The alternative was to keep describing them as scalars, which is what this did. For rank 0 that
            // made every legal value invalid, because an array is the only thing such an argument accepts: the
            // method could not be called at all. `any` accepts anything and leaves the judgement to the device,
            // which is the only party that knows the real signature.
            final Integer rank = argument.getValueRank();
            if (rank == null || rank == ValueRanks.Scalar) {
                return new ArgumentShape(type, List.of());
            }
            if (rank < 1) {
                return ArgumentShape.UNSTATEABLE;
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
    ///
    /// @param writable whether these arguments can be written. True for a request, whose arguments are exactly
    ///                 what a caller supplies; false for a response, whose outputs the device produces and nobody
    ///                 writes. The builder defaults every node to not-writable, which renders `readOnly: true`, so
    ///                 a request's arguments had to say otherwise explicitly -- without it every argument of every
    ///                 method was marked read-only, which is the opposite of what a request's own fields are.
    ///                 <p>
    ///                 **It also decides nullability, inverted.** An *output* may be published as null whatever
    ///                 its declared type: when this client cannot decode it, the field is nulled and the body
    ///                 the device sent moves to `statusInfo.undecodedOutputs` (see
    ///                 {@link MethodResponse#undecodedBody}). So every output is nullable, and stating it only
    ///                 for the structured ones would understate it -- a decode can fail for a reason the type
    ///                 does not predict. An *input* is not nullable: a caller sending null for an argument has
    ///                 not supplied it, and the adapter refuses that rather than inventing a value.
    private static void describeArguments(
            final @NotNull ObjectSchemaBuilder<?> value,
            final @NotNull Map<String, ArgumentShape> arguments,
            final boolean writable) {

        arguments.forEach((name, shape) -> {
            // Set before the structure, because the describe helpers consume the property builder and return void.
            // Annotations and structure are independent in the builder, so the order does not otherwise matter.
            final var property = value.property(name).writable(writable).nullable(!writable);
            if (shape.isArray()) {
                // Element type may still be null -- an array of a custom structure. describeAsArray answers that
                // with `any` elements, which keeps the array-ness (the part it can state) rather than losing it.
                //
                // A structured builtin's elements are deliberately passed as null for the same reason the
                // scalar case below says `any`: the type is accepted in two shapes and the schema model cannot
                // say "either of these". Describing the elements concretely stated one shape and refused the
                // other -- a StatusCode[] declared integer elements while the read path publishes
                // {"code":0,"symbol":"Good"}, and a QualifiedName[] required a namespaceIndex the read path
                // omits for namespace 0. Both refused a value read from this very device, at the gate, before
                // the adapter could accept it. The description carries the forms, as it does for the scalar.
                final String acceptedForms = shape.type() == null ? null : ACCEPTED_FORMS.get(shape.type());
                if (acceptedForms != null) {
                    property.description(acceptedForms + ", per element");
                }
                JsonSchemaGenerator.describeAsArray(
                        property, acceptedForms != null ? null : shape.type(), shape.dimensionsArray(), writable);
            } else if (shape.type() == null) {
                // Two methods disagreed, or the type is a custom structure. `any` is the honest answer.
                property.description("type varies by method").any();
            } else if (ACCEPTED_FORMS.get(shape.type()) instanceof final String acceptedForms) {
                // A type the adapter accepts in two shapes: the object the read path publishes, and a simple
                // form a caller can write by hand. The schema model has no alternation -- its node kinds are
                // any, scalar, object and array, and the interface is sealed -- so `any` is the only way to
                // describe "either of these". Stating one of the two would refuse the other at the gate.
                //
                // The cost is that the gate stops checking this property at all, and the adapter's own
                // conversion becomes the only check. That is the same trade as the two cases above.
                property.description(acceptedForms).any();
            } else {
                JsonSchemaGenerator.describeAs(property, shape.type());
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
                final ArgumentShape shape = ArgumentShape.of(argument, method.builtinTypeOf(argument));
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
