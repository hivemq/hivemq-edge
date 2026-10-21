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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.schema.SchemaJsonRepresentation;
import com.hivemq.datapoint.DataPointWithMetadata;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.northbound.Decoding;
import com.hivemq.edge.adapters.opcua.northbound.OpcUaToJsonConverter;
import com.hivemq.edge.adapters.opcua.southbound.JsonToOpcUaValueConverter;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.encoding.DefaultEncodingContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExtensionObject;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodResult;
import org.eclipse.milo.opcua.stack.core.types.structured.Range;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import util.EmbeddedOpcUaServerExtension;
import util.TestNamespace;

/// A method taking a structure, end to end against the embedded server: the browse describes the structure, the
/// request schema states its fields, and a call delivers the structure the caller sent.
///
/// The structure is `Range`, the standard `{low, high}` pair, because the embedded server describes it without a
/// custom type being registered.
class StructureArgumentIT {

    private static final @NotNull ObjectMapper MAPPER = new ObjectMapper();

    @RegisterExtension
    public final @NotNull EmbeddedOpcUaServerExtension opcUaServerExtension = new EmbeddedOpcUaServerExtension();

    private @NotNull OpcUaClient client;
    private @NotNull NodeId objectId;

    @BeforeEach
    void setUp() throws Exception {
        objectId = namespace().addStructureMethodObject("Furnace");
        client = OpcUaClient.create(opcUaServerExtension.getServerUri());
        client.connect();
    }

    @AfterEach
    void tearDown() throws Exception {
        client.disconnect();
    }

    private @NotNull TestNamespace namespace() {
        return Objects.requireNonNull(opcUaServerExtension.getTestNamespace());
    }

    private @NotNull List<MethodSignature> verified(final @NotNull OpcUaClient using) {
        final MethodVerifier.Result result =
                MethodVerifier.verify(using, objectId, "furnace").join();
        assertThat(result).isInstanceOf(MethodVerifier.Result.Verified.class);
        return ((MethodVerifier.Result.Verified) result).methods();
    }

    private static @NotNull MethodSignature named(
            final @NotNull List<MethodSignature> methods, final @NotNull String name) {
        return methods.stream()
                .filter(method -> method.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no method named '" + name + "'"));
    }

    // region The schema

    @Test
    void theRequestSchema_describesAStructureArgumentsFields() throws Exception {
        final JsonNode schema = requestSchema(verified(client));
        final JsonNode range = schema.at("/properties/range");

        assertThat(range.get("type").asText()).isEqualTo("object");
        assertThat(range.at("/properties/Low/type").asText()).isEqualTo("number");
        assertThat(range.at("/properties/High/type").asText()).isEqualTo("number");
        assertThat(range.get("required")).extracting(JsonNode::asText).containsExactlyInAnyOrder("Low", "High");

        final JsonNode ranges = schema.at("/properties/ranges");
        assertThat(ranges.get("type").asText()).isEqualTo("array");
        assertThat(ranges.at("/items/type").asText()).isEqualTo("object");
        assertThat(ranges.at("/items/properties/Low/type").asText()).isEqualTo("number");
    }

    @Test
    void theRequestSchema_acceptsAStructureAndRefusesAMalformedOne() throws Exception {
        final JsonNode schema = requestSchema(verified(client));

        assertThat(validate(schema, "{\"range\": {\"Low\": 1.5, \"High\": 9.0}}"))
                .isEmpty();
        assertThat(validate(schema, "{\"ranges\": [{\"Low\": 0, \"High\": 1}, {\"Low\": 2, \"High\": 3}]}"))
                .isEmpty();
        assertThat(validate(schema, "{\"range\": {\"Low\": \"cold\", \"High\": 9.0}}"))
                .isNotEmpty();
        assertThat(validate(schema, "{\"range\": {\"Low\": 1.5}}")).isNotEmpty();
    }

    @Test
    void aStructureArgument_withoutTheServersTypeHierarchy_isDescribedAsAny() throws Exception {
        // The browse must not fail because a structure cannot be described: the argument loses its detail and
        // nothing else.
        final OpcUaClient treeless = spy(client);
        doThrow(new UaException(StatusCodes.Bad_UserAccessDenied))
                .when(treeless)
                .getDataTypeTree();

        final JsonNode range = requestSchema(verified(treeless)).at("/properties/range");

        assertThat(range.has("type")).as("an `any` schema states no type").isFalse();
    }

    // endregion

    // region The call

    @Test
    void aCallWithAStructureArgument_deliversTheStructureSent() throws Exception {
        final MethodSignature setRange = named(verified(client), "SetRange");

        final Variant[] arguments = MethodArguments.forCall(
                setRange, json("{\"range\": {\"Low\": 1.5, \"High\": 9.0}}"), JsonToOpcUaValueConverter.of(client));
        call(setRange, arguments);

        final Variant[] received = namespace().structureCallArguments().get(0);
        assertThat(decode((ExtensionObject) received[0].getValue())).isEqualTo(new Range(1.5, 9.0));
    }

    @Test
    void aCallWithAnArrayOfStructures_deliversEveryElement() throws Exception {
        final MethodSignature setRanges = named(verified(client), "SetRanges");

        final Variant[] arguments = MethodArguments.forCall(
                setRanges,
                json("{\"ranges\": [{\"Low\": 0, \"High\": 1}, {\"Low\": 2, \"High\": 3}]}"),
                JsonToOpcUaValueConverter.of(client));
        call(setRanges, arguments);

        final ExtensionObject[] received = (ExtensionObject[])
                namespace().structureCallArguments().get(0)[0].getValue();
        assertThat(received).hasSize(2);
        assertThat(decode(received[0])).isEqualTo(new Range(0.0, 1.0));
        assertThat(decode(received[1])).isEqualTo(new Range(2.0, 3.0));
    }

    @Test
    void aStructureTheReadSidePublished_canBeSentBackUnchanged() throws Exception {
        // The round trip for a standard structure: published by the real read converter, which renders it from
        // its definition, then sent as an argument and decoded by the server.
        final MethodSignature setRange = named(verified(client), "SetRange");
        final JsonNode published = publish(new Range(1.5, 9.0));

        final ObjectNode request = MAPPER.createObjectNode();
        request.set("range", published);
        call(setRange, MethodArguments.forCall(setRange, request, JsonToOpcUaValueConverter.of(client)));

        final Variant[] received = namespace().structureCallArguments().get(0);
        assertThat(decode((ExtensionObject) received[0].getValue())).isEqualTo(new Range(1.5, 9.0));
    }

    private @NotNull JsonNode publish(final @NotNull Object value) {
        final var builder = (DataPointWithMetadata.DataPointBuilderImpl<Void>) DataPointWithMetadata.<Void>builder(
                new OpcuaTag("test-tag", "", new OpcuaTagDefinition("ns=2;i=1001")), b -> null);
        OpcUaToJsonConverter.convertPayload(
                Decoding.of(client), new DataValue(new Variant(value), StatusCode.GOOD, null), builder);
        return builder.build("test-adapter").getTagValue();
    }

    private void call(final @NotNull MethodSignature method, final @NotNull Variant @NotNull [] arguments)
            throws Exception {
        final CallMethodResult result = client.callAsync(
                        List.of(new CallMethodRequest(method.objectId(), method.nodeId(), arguments)))
                .get()
                .getResults()[0];
        assertThat(result.getStatusCode().isGood())
                .withFailMessage("the call was refused: %s", result.getStatusCode())
                .isTrue();
    }

    /// Decoded with the static context, which knows `Range` as Milo's generated class -- what any server
    /// implementing the standard type decodes it as.
    private static @NotNull Object decode(final @NotNull ExtensionObject encoded) {
        return encoded.decode(DefaultEncodingContext.INSTANCE);
    }

    // endregion

    private static @NotNull JsonNode requestSchema(final @NotNull List<MethodSignature> methods) {
        return SchemaJsonRepresentation.INSTANCE.toJsonSchemaDocument(MethodSchemas.requestSchema(methods));
    }

    private static @NotNull Set<ValidationMessage> validate(final @NotNull JsonNode schema, final @NotNull String raw)
            throws Exception {
        return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                .getSchema(schema)
                .validate(json(raw));
    }

    private static @NotNull JsonNode json(final @NotNull String raw) throws Exception {
        return MAPPER.readTree(raw);
    }
}
