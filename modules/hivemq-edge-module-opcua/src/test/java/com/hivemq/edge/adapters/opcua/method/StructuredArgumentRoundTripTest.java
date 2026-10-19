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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.schema.SchemaJsonRepresentation;
import com.hivemq.datapoint.DataPointWithMetadata;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.northbound.OpcUaToJsonConverter;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.encoding.DefaultEncodingContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/// A value read from a device, sent straight back as a method argument, reaches the device unchanged.
///
/// **This is the property the object forms exist for**, and the one no test checked. Each structured type had a
/// test for what the read path publishes, and a test for what the write path accepts, both passing -- while the
/// composition of the two silently altered values, because each half was written against its own idea of the
/// shape rather than against the other half's output.
///
/// Three defects lived in that gap, all found in review rather than here:
///
///   - a Guid node identifier came back as a String identifier, naming a different node, because the write path
///     inferred the identifier kind from the JSON type of `id` and a Guid reaches JSON as text;
///   - an Opaque node identifier likewise;
///   - a localised text with no locale came back carrying `en`, which for a condition's comment is the
///     difference between "leave the existing comment alone" and "reset it".
///
/// So the assertion here is deliberately crude: equality on the Milo value, not on its rendering. A value whose
/// text matches but whose type or absent fields do not still fails.
class StructuredArgumentRoundTripTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("publishedValues")
    void whenAPublishedValueIsSentBackAsAnArgument_thenItArrivesUnchanged(
            final @NotNull String name, final @NotNull Object value, final @NotNull NodeId dataType) throws Exception {

        final JsonNode published = publish(value);
        final var arguments = MethodArguments.forCall(signature(dataType), request(published));

        assertThat(arguments[0].value()).isEqualTo(value);
    }

    private static @NotNull Stream<Arguments> publishedValues() {
        return Stream.of(
                // Node identifiers: all four kinds the protocol defines. Guid and Opaque both reach JSON as
                // text, which is what made inferring the kind from the JSON type wrong.
                Arguments.of("NodeId, Numeric", new NodeId(2, 9482), NodeIds.NodeId),
                Arguments.of("NodeId, String", new NodeId(2, "Boiler1.Temperature"), NodeIds.NodeId),
                Arguments.of(
                        "NodeId, Guid",
                        new NodeId(2, UUID.fromString("72962b91-fa75-4ae6-8d28-b404dc7daf63")),
                        NodeIds.NodeId),
                Arguments.of("NodeId, Opaque", new NodeId(2, ByteString.of(new byte[] {1, 2, 3})), NodeIds.NodeId),

                // Localised text: both fields, and each of the absences the read path renders by omission.
                Arguments.of(
                        "LocalizedText, both fields", new LocalizedText("de-DE", "quittiert"), NodeIds.LocalizedText),
                Arguments.of(
                        "LocalizedText, no locale", new LocalizedText(null, "acknowledged"), NodeIds.LocalizedText),
                Arguments.of("LocalizedText, no text", new LocalizedText("en", null), NodeIds.LocalizedText),
                Arguments.of("LocalizedText, neither", LocalizedText.NULL_VALUE, NodeIds.LocalizedText),
                Arguments.of(
                        "LocalizedText, empty text with a locale", new LocalizedText("en", ""), NodeIds.LocalizedText),

                // Qualified names: namespace zero is rendered by omitting the index, so it is the asymmetric
                // case here too.
                Arguments.of("QualifiedName, namespace 2", new QualifiedName(2, "Start"), NodeIds.QualifiedName),
                Arguments.of("QualifiedName, namespace 0", new QualifiedName(0, "Start"), NodeIds.QualifiedName),

                // StatusCode: a high-bit value, because the code is a UInt32 and a signed reading of it is the
                // mistake waiting to be made.
                Arguments.of("StatusCode, Good", StatusCode.GOOD, NodeIds.StatusCode),
                Arguments.of("StatusCode, Bad with the high bit set", new StatusCode(0x80340000L), NodeIds.StatusCode));
    }

    /// Runs one value through the real read converter and returns the published `value` node.
    private static @NotNull JsonNode publish(final @NotNull Object value) {
        final DataValue dataValue = new DataValue(new Variant(value), StatusCode.GOOD, null);
        final var builder = (DataPointWithMetadata.DataPointBuilderImpl<Void>) DataPointWithMetadata.<Void>builder(
                new OpcuaTag("test-tag", "", new OpcuaTagDefinition("ns=2;i=1001")), b -> null);

        OpcUaToJsonConverter.convertPayload(() -> DefaultEncodingContext.INSTANCE, dataValue, builder);

        return builder.build("test-adapter").getTagValue();
    }

    private static @NotNull MethodSignature signature(final @NotNull NodeId dataType) {
        final NodeId method = new NodeId(2, "Boiler1.Act");
        return MethodSignature.ofObject(
                "Act",
                method,
                method,
                List.of(new Argument("argument", dataType, -1, null, LocalizedText.NULL_VALUE)),
                List.of());
    }

    private static @NotNull JsonNode request(final @NotNull JsonNode published) {
        return new ObjectMapper().createObjectNode().set("argument", published);
    }

    /// An **array** of a structured builtin passes the gate its own schema puts in front of it.
    ///
    /// The same property as the round trip above, one level further in, and it failed for every structured
    /// type. Describing a property consults a table of the types accepted in two shapes -- the object a read
    /// publishes and a shorthand a person types -- and answers `any`, because the schema model cannot express
    /// an alternation. The array branch is tested first and never reached that table, so elements kept the
    /// plain scalar mapping: a `StatusCode[]` declared integer elements while a read publishes
    /// `{"code":0,"symbol":"Good"}`, and a `QualifiedName[]` required a `namespaceIndex` a read omits for
    /// namespace 0.
    ///
    /// Validated with the validator Edge itself uses, against the schema the adapter really generates, because
    /// the defect is only visible where the two meet -- the conversion accepted these elements all along.
    @ParameterizedTest(name = "{0}")
    @MethodSource("publishedArrays")
    void whenAPublishedArrayIsSentBack_thenItsOwnSchemaAcceptsIt(
            final @NotNull String name, final @NotNull Object array, final @NotNull NodeId dataType) throws Exception {

        final JsonNode published = publish(array);
        final JsonNode schema = SchemaJsonRepresentation.INSTANCE.toJsonSchemaDocument(
                MethodSchemas.requestSchema(List.of(arraySignature(dataType))));

        final Set<ValidationMessage> errors = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                .getSchema(schema)
                .validate(request(published));

        assertThat(errors)
                .withFailMessage(
                        "the schema refuses an array this adapter published and accepts: %s%nagainst: %s",
                        published, schema)
                .isEmpty();
    }

    private static @NotNull Stream<Arguments> publishedArrays() {
        return Stream.of(
                Arguments.of(
                        "StatusCode[]",
                        new StatusCode[] {StatusCode.GOOD, new StatusCode(0x80340000L)},
                        NodeIds.StatusCode),
                Arguments.of("NodeId[]", new NodeId[] {new NodeId(2, "result"), new NodeId(2, 9482)}, NodeIds.NodeId),
                Arguments.of(
                        "LocalizedText[]",
                        new LocalizedText[] {new LocalizedText("en", "ok"), new LocalizedText(null, "unspecified")},
                        NodeIds.LocalizedText),
                Arguments.of(
                        "QualifiedName[], namespace 0",
                        new QualifiedName[] {new QualifiedName(0, "result"), new QualifiedName(2, "Start")},
                        NodeIds.QualifiedName));
    }

    /// The same single argument as {@link #signature}, declared one-dimensional.
    private static @NotNull MethodSignature arraySignature(final @NotNull NodeId dataType) {
        final NodeId method = new NodeId(2, "Boiler1.ActAll");
        return MethodSignature.ofObject(
                "ActAll",
                method,
                method,
                List.of(new Argument(
                        "argument", dataType, 1, new UInteger[] {Unsigned.uint(0)}, LocalizedText.NULL_VALUE)),
                List.of());
    }
}
