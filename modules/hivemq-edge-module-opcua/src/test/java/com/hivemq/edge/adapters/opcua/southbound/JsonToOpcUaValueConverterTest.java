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
package com.hivemq.edge.adapters.opcua.southbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.datapoint.DataPointWithMetadata;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.northbound.OpcUaToJsonConverter;
import com.hivemq.edge.adapters.opcua.southbound.JsonToOpcUaValueConverter.ConversionException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.core.types.DynamicEnumType;
import org.eclipse.milo.opcua.sdk.core.types.DynamicStructType;
import org.eclipse.milo.opcua.sdk.core.types.codec.DynamicStructCodec;
import org.eclipse.milo.opcua.sdk.core.typetree.DataType;
import org.eclipse.milo.opcua.sdk.core.typetree.DataTypeTree;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.ServerTable;
import org.eclipse.milo.opcua.stack.core.channel.EncodingLimits;
import org.eclipse.milo.opcua.stack.core.encoding.DefaultEncodingContext;
import org.eclipse.milo.opcua.stack.core.encoding.EncodingContext;
import org.eclipse.milo.opcua.stack.core.encoding.EncodingManager;
import org.eclipse.milo.opcua.stack.core.types.DataTypeManager;
import org.eclipse.milo.opcua.stack.core.types.DefaultDataTypeManager;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExtensionObject;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.StructureType;
import org.eclipse.milo.opcua.stack.core.types.structured.DataTypeDefinition;
import org.eclipse.milo.opcua.stack.core.types.structured.EnumDefinition;
import org.eclipse.milo.opcua.stack.core.types.structured.EnumField;
import org.eclipse.milo.opcua.stack.core.types.structured.StructureDefinition;
import org.eclipse.milo.opcua.stack.core.types.structured.StructureField;
import org.eclipse.milo.opcua.stack.core.util.Tree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

/// The one southbound converter, for the kinds of value only the server's type hierarchy can describe: enumerations
/// and structures.
///
/// The builtin rules are pinned by `MethodArgumentsTest`, `MethodArgumentsArrayTest` and
/// `StructuredArgumentRoundTripTest`, which now run through this converter. This covers what was added to it.
///
/// **Against an offline hierarchy and a real codec.** The hierarchy here is built by hand, and the encoding is
/// Milo's own `DynamicStructCodec` -- the codec a connected client creates for a server's structure -- so an
/// encoded value is decoded again and its members checked, rather than the converter's intermediate form trusted.
class JsonToOpcUaValueConverterTest {

    private static final @NotNull ObjectMapper MAPPER = new ObjectMapper();

    // region The hierarchy: an enumeration, two structures, a union, an abstract structure

    private static final @NotNull NodeId MACHINE_MODE = new NodeId(2, 3001);
    private static final @NotNull NodeId LEGACY_MODE = new NodeId(2, 3002);
    private static final @NotNull NodeId STEP_TYPE = new NodeId(2, 3010);
    private static final @NotNull NodeId STEP_ENCODING = new NodeId(2, 3110);
    private static final @NotNull NodeId RECIPE_TYPE = new NodeId(2, 3011);
    private static final @NotNull NodeId RECIPE_ENCODING = new NodeId(2, 3111);
    private static final @NotNull NodeId SETPOINT_UNION = new NodeId(2, 3012);
    private static final @NotNull NodeId ABSTRACT_RECORD = new NodeId(2, 3013);
    private static final @NotNull NodeId OPTIONALS_TYPE = new NodeId(2, 3014);
    private static final @NotNull NodeId OPTIONALS_ENCODING = new NodeId(2, 3114);
    private static final @NotNull NodeId GAP_TYPE = new NodeId(2, 3015);
    private static final @NotNull NodeId GAP_ENCODING = new NodeId(2, 3115);

    /// `MachineMode`: Off = 0, Running = 2, Maintenance = 5. The gaps are deliberate: an undefined value in
    /// between must be refused.
    private static final @NotNull EnumDefinition MACHINE_MODE_DEFINITION = new EnumDefinition(new EnumField[] {
        new EnumField(0L, LocalizedText.english("Off"), LocalizedText.NULL_VALUE, "Off"),
        new EnumField(2L, LocalizedText.english("Running"), LocalizedText.NULL_VALUE, "Running"),
        new EnumField(5L, LocalizedText.english("Maintenance"), LocalizedText.NULL_VALUE, "Maintenance")
    });

    /// `StepType`: a plain structure of three builtins.
    private static final @NotNull StructureDefinition STEP_DEFINITION =
            new StructureDefinition(STEP_ENCODING, NodeIds.Structure, StructureType.Structure, new StructureField[] {
                field("name", NodeIds.String, ValueRanks.Scalar, false),
                field("temperature", NodeIds.Double, ValueRanks.Scalar, false),
                field("durationMs", NodeIds.UInt32, ValueRanks.Scalar, false)
            });

    /// `RecipeType`: optional fields, an enumeration field, and an array of structures.
    private static final @NotNull StructureDefinition RECIPE_DEFINITION = new StructureDefinition(
            RECIPE_ENCODING, NodeIds.Structure, StructureType.StructureWithOptionalFields, new StructureField[] {
                field("id", NodeIds.Int32, ValueRanks.Scalar, false),
                field("mode", MACHINE_MODE, ValueRanks.Scalar, false),
                field("steps", STEP_TYPE, ValueRanks.OneDimension, false),
                field("note", NodeIds.LocalizedText, ValueRanks.Scalar, true),
                field("tags", NodeIds.String, ValueRanks.OneDimension, true)
            });

    /// `OptionalsType`: a required `x`, then an optional `a`, then an optional `b`, all Int32 -- the smallest
    /// structure in which a left-out optional field can precede a given one.
    private static final @NotNull StructureDefinition OPTIONALS_DEFINITION = new StructureDefinition(
            OPTIONALS_ENCODING, NodeIds.Structure, StructureType.StructureWithOptionalFields, new StructureField[] {
                field("x", NodeIds.Int32, ValueRanks.Scalar, false),
                field("a", NodeIds.Int32, ValueRanks.Scalar, true),
                field("b", NodeIds.Int32, ValueRanks.Scalar, true)
            });

    /// `GapType`: a required `x`, an optional `a`, a required `y`.
    private static final @NotNull StructureDefinition GAP_DEFINITION = new StructureDefinition(
            GAP_ENCODING, NodeIds.Structure, StructureType.StructureWithOptionalFields, new StructureField[] {
                field("x", NodeIds.Int32, ValueRanks.Scalar, false),
                field("a", NodeIds.Int32, ValueRanks.Scalar, true),
                field("y", NodeIds.Int32, ValueRanks.Scalar, false)
            });

    private static final @NotNull StructureDefinition UNION_DEFINITION =
            new StructureDefinition(NodeId.NULL_VALUE, NodeIds.Structure, StructureType.Union, new StructureField[] {
                field("celsius", NodeIds.Double, ValueRanks.Scalar, false),
                field("percent", NodeIds.Double, ValueRanks.Scalar, false)
            });

    private static @NotNull StructureField field(
            final @NotNull String name, final @NotNull NodeId dataType, final int valueRank, final boolean optional) {
        return new StructureField(
                name,
                LocalizedText.NULL_VALUE,
                dataType,
                valueRank,
                valueRank >= 1 ? new UInteger[] {UInteger.valueOf(0)} : null,
                UInteger.valueOf(0),
                optional);
    }

    private static final @NotNull TestDataType STEP =
            new TestDataType("StepType", STEP_TYPE, STEP_ENCODING, STEP_DEFINITION, false);
    private static final @NotNull TestDataType RECIPE =
            new TestDataType("RecipeType", RECIPE_TYPE, RECIPE_ENCODING, RECIPE_DEFINITION, false);
    private static final @NotNull TestDataType OPTIONALS =
            new TestDataType("OptionalsType", OPTIONALS_TYPE, OPTIONALS_ENCODING, OPTIONALS_DEFINITION, false);
    private static final @NotNull TestDataType GAP =
            new TestDataType("GapType", GAP_TYPE, GAP_ENCODING, GAP_DEFINITION, false);

    private static final @NotNull DataTypeTree TREE = buildTree();

    private static @NotNull DataTypeTree buildTree() {
        final Tree<DataType> root =
                new Tree<>(null, new TestDataType("BaseDataType", NodeIds.BaseDataType, null, null, true));
        final Tree<DataType> structure =
                root.addChild(new TestDataType("Structure", NodeIds.Structure, null, null, true));
        structure.addChild(STEP);
        structure.addChild(RECIPE);
        structure.addChild(OPTIONALS);
        structure.addChild(GAP);
        structure.addChild(new TestDataType("SetpointUnion", SETPOINT_UNION, null, UNION_DEFINITION, false));
        structure.addChild(new TestDataType("AbstractRecord", ABSTRACT_RECORD, null, null, true));
        final Tree<DataType> enumeration =
                root.addChild(new TestDataType("Enumeration", NodeIds.Enumeration, null, null, true));
        enumeration.addChild(new TestDataType("MachineMode", MACHINE_MODE, null, MACHINE_MODE_DEFINITION, false));
        // An enumeration described only through EnumStrings, as a server predating OPC UA 1.04 does: the
        // hierarchy knows it is an enumeration, but not its names.
        enumeration.addChild(new TestDataType("LegacyMode", LEGACY_MODE, null, null, false));
        final Tree<DataType> number = root.addChild(new TestDataType("Number", NodeIds.Number, null, null, true));
        number.addChild(new TestDataType("Double", NodeIds.Double, null, null, false))
                .addChild(new TestDataType("Duration", NodeIds.Duration, null, null, false));
        return new DataTypeTree(root);
    }

    /// The encoding context a client builds for a server's structures: the builtin codecs plus a dynamic codec
    /// per structure.
    private static final @NotNull EncodingContext CONTEXT = buildContext();

    private static @NotNull EncodingContext buildContext() {
        final NamespaceTable namespaces = new NamespaceTable();
        namespaces.add("urn:test:one");
        namespaces.add("urn:test:two");
        final DataTypeManager types = DefaultDataTypeManager.createAndInitialize(namespaces);
        types.registerType(STEP_TYPE, new DynamicStructCodec(STEP, TREE), STEP_ENCODING, null, null);
        types.registerType(RECIPE_TYPE, new DynamicStructCodec(RECIPE, TREE), RECIPE_ENCODING, null, null);
        types.registerType(OPTIONALS_TYPE, new DynamicStructCodec(OPTIONALS, TREE), OPTIONALS_ENCODING, null, null);
        types.registerType(GAP_TYPE, new DynamicStructCodec(GAP, TREE), GAP_ENCODING, null, null);
        return new EncodingContext() {
            @Override
            public @NotNull DataTypeManager getDataTypeManager() {
                return types;
            }

            @Override
            public @NotNull EncodingManager getEncodingManager() {
                return DefaultEncodingContext.INSTANCE.getEncodingManager();
            }

            @Override
            public @NotNull EncodingLimits getEncodingLimits() {
                return DefaultEncodingContext.INSTANCE.getEncodingLimits();
            }

            @Override
            public @NotNull NamespaceTable getNamespaceTable() {
                return namespaces;
            }

            @Override
            public @NotNull ServerTable getServerTable() {
                return DefaultEncodingContext.INSTANCE.getServerTable();
            }
        };
    }

    private static final @NotNull JsonToOpcUaValueConverter CONVERTER =
            new JsonToOpcUaValueConverter(() -> TREE, () -> CONTEXT);

    // endregion

    // region Structures

    private static final @NotNull String RECIPE_JSON = """
            {
              "id": 7,
              "mode": "Running",
              "steps": [
                {"name": "heat", "temperature": 80.5, "durationMs": 60000},
                {"name": "hold", "temperature": 80, "durationMs": 120000}
              ],
              "note": {"locale": "de-DE", "text": "Charge 12"}
            }
            """;

    @Test
    void aStructure_isEncodedSoThatTheDeviceDecodesWhatWasSent() throws Exception {
        final Variant variant = CONVERTER.toVariant(json(RECIPE_JSON), RECIPE_TYPE, ValueRanks.Scalar);

        assertThat(variant.getValue()).isInstanceOf(ExtensionObject.class);
        final DynamicStructType recipe = decode((ExtensionObject) variant.getValue());

        assertThat(recipe.getMembers().get("id")).isEqualTo(7);
        assertThat(((DynamicEnumType) recipe.getMembers().get("mode")).getValue())
                .isEqualTo(2);
        assertThat(recipe.getMembers().get("note")).isEqualTo(new LocalizedText("de-DE", "Charge 12"));

        final DynamicStructType[] steps =
                (DynamicStructType[]) recipe.getMembers().get("steps");
        assertThat(steps).hasSize(2);
        assertThat(steps[0].getMembers().get("name")).isEqualTo("heat");
        assertThat(steps[0].getMembers().get("temperature")).isEqualTo(80.5);
        assertThat(steps[0].getMembers().get("durationMs")).isEqualTo(UInteger.valueOf(60000));
        // A JSON integer for a Double field: the same conversion a Double variable gets.
        assertThat(steps[1].getMembers().get("temperature")).isEqualTo(80.0);
    }

    @Test
    void anAbsentOptionalField_isLeftOutRatherThanSentAsNull() throws Exception {
        // `tags` is absent and `note` is null: both are left out of the encoding mask, which is what the device
        // reads as "not supplied". Putting either in as null would set its bit and send a null value instead.
        final Variant variant = CONVERTER.toVariant(
                json("{\"id\": 1, \"mode\": 0, \"steps\": [], \"note\": null}"), RECIPE_TYPE, ValueRanks.Scalar);

        final DynamicStructType recipe = decode((ExtensionObject) variant.getValue());
        assertThat(recipe.getMembers()).containsOnlyKeys("id", "mode", "steps");
    }

    @Test
    void optionalFieldsLeftOutAtTheEnd_arriveAsSent() {
        for (final Map<String, Object> sent :
                List.<Map<String, Object>>of(Map.of("x", 1, "a", 2, "b", 3), Map.of("x", 1, "a", 2), Map.of("x", 1))) {
            final DynamicStructType decoded = decode((ExtensionObject) CONVERTER
                    .toVariant(MAPPER.valueToTree(sent), OPTIONALS_TYPE, ValueRanks.Scalar)
                    .getValue());

            assertThat(decoded.getMembers()).isEqualTo(sent);
        }
    }

    @Test
    void anOptionalFieldLeftOutBeforeAGivenOne_isRefused() {
        assertThatThrownBy(() -> CONVERTER.toVariant(json("{\"x\": 1, \"b\": 42}"), OPTIONALS_TYPE, ValueRanks.Scalar))
                .isInstanceOfSatisfying(ConversionException.class, e -> assertThat(e.reason())
                        .isEqualTo("field 'a': is left out, but the later field 'b' is given; a structure may leave"
                                + " out optional fields only at its end"));
    }

    /// A required field after a left-out optional one breaks the prefix as well, whatever the codec would make
    /// of it.
    @Test
    void aRequiredFieldAfterALeftOutOptionalOne_isRefused() {
        assertThatThrownBy(() -> CONVERTER.toVariant(json("{\"x\": 1, \"y\": 2}"), GAP_TYPE, ValueRanks.Scalar))
                .isInstanceOfSatisfying(ConversionException.class, e -> assertThat(e.reason())
                        .startsWith("field 'a': is left out, but the later field 'y' is given"));
    }

    /// What the restriction protects against, pinned on Milo's codec itself: a gap moves `b` onto `a`'s bit. When
    /// this starts failing, Edge uses a Milo with the corrected numbering, and the restriction can go.
    @Test
    void aGap_isCorruptedByTheCodec_whichIsWhyItIsRefused() {
        final LinkedHashMap<String, Object> members = new LinkedHashMap<>();
        members.put("x", 1);
        members.put("b", 42);

        final DynamicStructType decoded =
                decode(ExtensionObject.encode(CONTEXT, new DynamicStructType(OPTIONALS, members)));

        assertThat(decoded.getMembers()).isEqualTo(Map.of("x", 1, "a", 0));
    }

    @Test
    void anArrayOfStructures_isAnArrayOfEncodedStructures() {
        final Variant variant = CONVERTER.toVariant(
                json("[{\"name\": \"a\", \"temperature\": 1, \"durationMs\": 2}]"), STEP_TYPE, ValueRanks.OneDimension);

        assertThat(variant.getValue()).isInstanceOf(ExtensionObject[].class);
        assertThat(decode(((ExtensionObject[]) variant.getValue())[0])
                        .getMembers()
                        .get("name"))
                .isEqualTo("a");
    }

    @Test
    void aMissingRequiredField_isRefusedNamingIt() {
        assertThatThrownBy(
                        () -> CONVERTER.toVariant(json("{\"mode\": 0, \"steps\": []}"), RECIPE_TYPE, ValueRanks.Scalar))
                .isInstanceOfSatisfying(ConversionException.class, e -> {
                    assertThat(e.fault()).isEqualTo(ConversionException.Fault.VALUE);
                    assertThat(e.expected()).isEqualTo("RecipeType");
                    assertThat(e.reason()).startsWith("field 'id': is required");
                });
    }

    @Test
    void aFieldTheStructureDoesNotHave_isRefusedRatherThanIgnored() {
        // `temprature` is a typo. Ignoring it would send the structure without the caller's value -- and for an
        // optional field, succeed doing so.
        assertThatThrownBy(() -> CONVERTER.toVariant(
                        json("{\"name\": \"a\", \"temperature\": 1, \"durationMs\": 2, \"temprature\": 3}"),
                        STEP_TYPE,
                        ValueRanks.Scalar))
                .isInstanceOfSatisfying(ConversionException.class, e -> assertThat(e.reason())
                        .contains("'temprature' is not a field of a StepType"));
    }

    @Test
    void aWrongValueDeepInside_isRefusedWithThePathToIt() {
        final String wrong = RECIPE_JSON.replace("\"temperature\": 80,", "\"temperature\": \"hot\",");

        assertThatThrownBy(() -> CONVERTER.toVariant(json(wrong), RECIPE_TYPE, ValueRanks.Scalar))
                .isInstanceOfSatisfying(ConversionException.class, e -> assertThat(e.reason())
                        .isEqualTo("field 'steps[1].temperature': expected a number, got STRING"));
    }

    @Test
    void aStructureReadFromADevice_canBeSentBackUnchanged() throws Exception {
        // The round trip the read path's object forms exist for, one level up: a structure published by the
        // real read converter, converted back, encoded and decoded, publishes identically. `RecipeType` carries
        // an enumeration field, an array of structures and an optional localised text.
        final JsonNode published = publish(CONVERTER
                .toVariant(json(RECIPE_JSON), RECIPE_TYPE, ValueRanks.Scalar)
                .getValue());

        final JsonNode republished = publish(
                CONVERTER.toVariant(published, RECIPE_TYPE, ValueRanks.Scalar).getValue());

        assertThat(republished).isEqualTo(published);
        assertThat(published.get("steps").get(0).get("temperature").asDouble()).isEqualTo(80.5);
    }

    @Test
    void anEnumerationFieldOfAStructure_isPublishedAsNameAndValue() throws Exception {
        // The read side rendered a structure's scalar enumeration field with Java's toString --
        // `DynamicEnumType[dataType=ns=2;i=3001, name=Running, value=2]` -- which no writer accepts back. It is
        // now the object an enumeration in an array field was already published as.
        final JsonNode published = publish(CONVERTER
                .toVariant(json(RECIPE_JSON), RECIPE_TYPE, ValueRanks.Scalar)
                .getValue());

        assertThat(published.get("mode")).isEqualTo(json("{\"name\": \"Running\", \"value\": 2}"));
    }

    // endregion

    // region Enumerations

    @Test
    void anEnumeration_acceptsTheNumberTheNameAndThePublishedObject() throws Exception {
        assertThat(CONVERTER
                        .toVariant(json("2"), MACHINE_MODE, ValueRanks.Scalar)
                        .getValue())
                .isEqualTo(2);
        assertThat(CONVERTER
                        .toVariant(json("\"Running\""), MACHINE_MODE, ValueRanks.Scalar)
                        .getValue())
                .isEqualTo(2);
        assertThat(CONVERTER
                        .toVariant(json("{\"name\": \"Running\", \"value\": 2}"), MACHINE_MODE, ValueRanks.Scalar)
                        .getValue())
                .isEqualTo(2);
        assertThat(CONVERTER
                        .toVariant(json("{\"value\": 5}"), MACHINE_MODE, ValueRanks.Scalar)
                        .getValue())
                .isEqualTo(5);
    }

    @Test
    void anEnumerationArray_isAnArrayOfInt32() throws Exception {
        assertThat(CONVERTER
                        .toVariant(json("[\"Off\", 5]"), MACHINE_MODE, ValueRanks.OneDimension)
                        .getValue())
                .isEqualTo(new Integer[] {0, 5});
    }

    @Test
    void anEnumeration_refusesAValueItDoesNotDefine() {
        assertThatThrownBy(() -> CONVERTER.toVariant(json("1"), MACHINE_MODE, ValueRanks.Scalar))
                .isInstanceOfSatisfying(ConversionException.class, e -> assertThat(e.reason())
                        .isEqualTo("MachineMode defines no value 1. It defines Off=0, Running=2, Maintenance=5"));
        assertThatThrownBy(() -> CONVERTER.toVariant(json("\"Stopped\""), MACHINE_MODE, ValueRanks.Scalar))
                .hasMessageContaining("has no value named 'Stopped'");
    }

    @Test
    void anEnumeration_refusesANameAndAValueThatDisagree() {
        assertThatThrownBy(() ->
                        CONVERTER.toVariant(json("{\"name\": \"Off\", \"value\": 2}"), MACHINE_MODE, ValueRanks.Scalar))
                .hasMessageContaining("gives to different values");
    }

    @Test
    void anEnumerationWithoutADefinition_acceptsOnlyTheNumber() throws Exception {
        assertThat(CONVERTER
                        .toVariant(json("3"), LEGACY_MODE, ValueRanks.Scalar)
                        .getValue())
                .isEqualTo(3);
        assertThatThrownBy(() -> CONVERTER.toVariant(json("\"Running\""), LEGACY_MODE, ValueRanks.Scalar))
                .hasMessageContaining("does not describe the names of LegacyMode");
    }

    // endregion

    // region What is refused, and when the hierarchy is read

    @Test
    void theAnyValueTypes_unionsAndAbstractTypes_areRefusedAsUnsupported() {
        assertUnsupported(NodeIds.BaseDataType, "accepts a value of any type");
        assertUnsupported(NodeIds.Structure, "abstract Structure");
        assertUnsupported(NodeIds.Enumeration, "abstract");
        assertUnsupported(ABSTRACT_RECORD, "abstract");
        assertUnsupported(SETPOINT_UNION, "union");
        assertUnsupported(new NodeId(2, 9999), "not in the server's type hierarchy");
    }

    private static void assertUnsupported(final @NotNull NodeId dataType, final @NotNull String because) {
        assertThatThrownBy(() -> CONVERTER.toVariant(json("1"), dataType, ValueRanks.Scalar))
                .isInstanceOfSatisfying(ConversionException.class, e -> {
                    assertThat(e.fault()).isEqualTo(ConversionException.Fault.UNSUPPORTED);
                    assertThat(e.reason()).contains(because);
                });
    }

    @Test
    void aBuiltinValue_doesNotReadTheHierarchy() throws Exception {
        // Miguel's R6-3 on EDG-1087: a value of a builtin type must not depend on a browse it does not need.
        final AtomicInteger reads = new AtomicInteger();
        final JsonToOpcUaValueConverter counting = new JsonToOpcUaValueConverter(
                () -> {
                    reads.incrementAndGet();
                    return TREE;
                },
                () -> CONTEXT);

        counting.toVariant(json("42"), NodeIds.Int32, ValueRanks.Scalar);
        counting.toVariant(json("[1, 2]"), NodeIds.Double, ValueRanks.OneDimension);
        assertThat(reads).hasValue(0);

        counting.toVariant(json("1.5"), NodeIds.Duration, ValueRanks.Scalar);
        assertThat(reads).hasValue(1);
    }

    @Test
    void aStructure_withoutTheHierarchy_isRefusedAsUnresolvable() {
        assertThatThrownBy(() -> JsonToOpcUaValueConverter.builtinOnly()
                        .toVariant(json(RECIPE_JSON), RECIPE_TYPE, ValueRanks.Scalar))
                .isInstanceOfSatisfying(ConversionException.class, e -> {
                    assertThat(e.fault()).isEqualTo(ConversionException.Fault.UNSUPPORTED);
                    assertThat(e.reason()).contains("type hierarchy could not be read");
                });
    }

    // endregion

    // region What a condition update needs

    @Test
    void theFourConditionUpdateValues_convertToWhatTheConditionWriterSends() throws Exception {
        // `ConditionUpdateWriter` builds its arguments by hand: the event id as the ByteString the server issued,
        // a TimedShelve duration as a Double, a dialog response as an Int32, a comment as English text. The
        // converter produces the same values from the same JSON, so nothing about those four needs a route of
        // its own. What stays with the condition writer is which of them a method requires, and what an absent
        // comment means -- the method's contract, not the values' types.
        final byte[] eventId = {1, 2, 3, 4};
        final String encoded = Base64.getEncoder().encodeToString(eventId);

        assertThat(CONVERTER
                        .toVariant(json("\"" + encoded + "\""), NodeIds.ByteString, ValueRanks.Scalar)
                        .getValue())
                .isEqualTo(ByteString.of(eventId));
        assertThat(CONVERTER
                        .toVariant(json("60000"), NodeIds.Duration, ValueRanks.Scalar)
                        .getValue())
                .isEqualTo(60000.0);
        assertThat(CONVERTER
                        .toVariant(json("1"), NodeIds.Int32, ValueRanks.Scalar)
                        .getValue())
                .isEqualTo(1);
        assertThat(CONVERTER
                        .toVariant(json("\"checked\""), NodeIds.LocalizedText, ValueRanks.Scalar)
                        .getValue())
                .isEqualTo(LocalizedText.english("checked"));
    }

    // endregion

    // region Helpers

    private static @NotNull JsonNode json(final @NotNull String raw) {
        try {
            return MAPPER.readTree(raw);
        } catch (final Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static @NotNull DynamicStructType decode(final @NotNull ExtensionObject encoded) {
        return (DynamicStructType) encoded.decode(CONTEXT);
    }

    /// Runs one value through the real read converter and returns the published `value` node.
    private static @NotNull JsonNode publish(final @NotNull Object value) {
        final DataValue dataValue = new DataValue(new Variant(value), StatusCode.GOOD, null);
        final var builder = (DataPointWithMetadata.DataPointBuilderImpl<Void>) DataPointWithMetadata.<Void>builder(
                new OpcuaTag("test-tag", "", new OpcuaTagDefinition("ns=2;i=1001")), b -> null);
        OpcUaToJsonConverter.convertPayload(() -> CONTEXT, dataValue, builder);
        return builder.build("test-adapter").getTagValue();
    }

    /// A DataType as the hierarchy describes one. Milo's own implementation is package-private to its client.
    private static final class TestDataType implements DataType {
        private final @NotNull QualifiedName browseName;
        private final @NotNull NodeId nodeId;
        private final @Nullable NodeId binaryEncodingId;
        private final @Nullable DataTypeDefinition definition;
        private final boolean isAbstract;

        TestDataType(
                final @NotNull String name,
                final @NotNull NodeId nodeId,
                final @Nullable NodeId binaryEncodingId,
                final @Nullable DataTypeDefinition definition,
                final boolean isAbstract) {
            this.browseName = new QualifiedName(nodeId.getNamespaceIndex(), name);
            this.nodeId = nodeId;
            this.binaryEncodingId = binaryEncodingId;
            this.definition = definition;
            this.isAbstract = isAbstract;
        }

        @Override
        public @NotNull QualifiedName getBrowseName() {
            return browseName;
        }

        @Override
        public @NotNull NodeId getNodeId() {
            return nodeId;
        }

        @Override
        public @Nullable NodeId getBinaryEncodingId() {
            return binaryEncodingId;
        }

        @Override
        public @Nullable NodeId getXmlEncodingId() {
            return null;
        }

        @Override
        public @Nullable NodeId getJsonEncodingId() {
            return null;
        }

        @Override
        public @Nullable DataTypeDefinition getDataTypeDefinition() {
            return definition;
        }

        @Override
        public @NotNull Boolean isAbstract() {
            return isAbstract;
        }

        @Override
        public @NotNull String toString() {
            return browseName.getName();
        }
    }

    // endregion
}
