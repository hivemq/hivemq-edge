/*
 * Copyright 2023-present HiveMQ GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.hivemq.edge.adapters.mtconnect.schemas;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class MtConnectSchemaPatchTest {
    private static final @NotNull File SCHEMA_FOLDER_FILE = new File("schema");
    private static final @NotNull File PATCH_SCRIPT_FILE = new File(SCHEMA_FOLDER_FILE, "patch-script.sh");
    private static final @NotNull Map<MtConnectSchema, List<MixedTypeNameGroup>> MIXED_SCHEMA_TO_TYPE_NAME_GROUPS_MAP =
            Map.of(MtConnectSchema.Streams_1_5,
                    List.of(new MixedTypeNameGroup("EventType", List.of("VariableDataSetType"))),
                    MtConnectSchema.Streams_1_6,
                    List.of(new MixedTypeNameGroup("EventType",
                                    List.of("VariableDataSetType", "WorkOffsetTableType", "ToolOffsetTableType")),
                            new MixedTypeNameGroup("EntryType",
                                    List.of("TableEntryType", "WorkOffsetTableEntryType", "ToolOffsetTableEntryType"))),
                    MtConnectSchema.Streams_1_7,
                    List.of(new MixedTypeNameGroup("EventType",
                                    List.of("VariableDataSetType", "WorkOffsetTableType", "ToolOffsetTableType")),
                            new MixedTypeNameGroup("EntryType",
                                    List.of("TableEntryType", "WorkOffsetTableEntryType", "ToolOffsetTableEntryType"))),
                    MtConnectSchema.Streams_1_8,
                    List.of(new MixedTypeNameGroup("EventType",
                                    List.of("VariableDataSetType", "WorkOffsetTableType", "ToolOffsetTableType")),
                            new MixedTypeNameGroup("EntryType",
                                    List.of("TableEntryType",
                                            "WorkOffsetTableEntryType",
                                            "ToolOffsetTableEntryType"))));
    private final boolean schemaFound;

    public MtConnectSchemaPatchTest() {
        schemaFound = SCHEMA_FOLDER_FILE.exists();
    }

    @BeforeEach
    public void setUp() {
        if (schemaFound) {
            assertThat(SCHEMA_FOLDER_FILE.isDirectory()).isTrue();
            String os = System.getProperty("os.name").toLowerCase();
            if (!os.contains("linux") && !os.contains("mac")) {
                fail("Only Linux and MacOS are supported.");
            }
        }
    }

    protected @NotNull String getPatchCommand(final @NotNull MtConnectSchema schema, final String fileName) {
        StringBuilder sb = new StringBuilder();
        sb.append("../jaxb-ri/bin/xjc.sh ")
                .append("-classpath \"${CLASSPATH}:../xerces-2_12_2-xml-schema-1.1/xml-apis.jar:../xerces-2_12_2-xml-schema-1.1/xercesImpl.jar\" ")
                .append("-d ../src/main/java ")
                .append("-p com.hivemq.edge.adapters.mtconnect.schemas.")
                .append(schema.getType().name().toLowerCase())
                .append(".")
                .append(schema.getType().name().toLowerCase())
                .append("_")
                .append(schema.getMajorVersion())
                .append("_")
                .append(schema.getMinorVersion())
                .append(" ");
        if (schema.getType() == MtConnectSchemaType.Devices) {
            if ((schema.getMajorVersion() == 1 && schema.getMinorVersion() >= 5) || schema.getMajorVersion() == 2) {
                sb.append("-b ").append(getPatchSchemaFileName(schema)).append(" ");
            }
        }
        sb.append(fileName);
        return sb.toString();
    }

    protected @NotNull String getPatchSchemaFileName(final @NotNull MtConnectSchema schema) {
        return "patch-" +
                schema.getType().name().toLowerCase() +
                "-" +
                schema.getMajorVersion() +
                "-" +
                schema.getMinorVersion() +
                ".xml";
    }

    protected @NotNull String getSchemaFileName(final @NotNull MtConnectSchema schema) {
        return "MTConnect" +
                schema.getType().name() +
                "_" +
                schema.getMajorVersion() +
                "." +
                schema.getMinorVersion() +
                ".xsd";
    }

    @Test
    public void generatePatchScript() throws IOException {
        if (!schemaFound) {
            return;
        }
        final String originalContent;
        if (PATCH_SCRIPT_FILE.exists()) {
            assertThat(PATCH_SCRIPT_FILE.isFile()).isTrue();
            assertThat(PATCH_SCRIPT_FILE.canRead()).isTrue();
            assertThat(PATCH_SCRIPT_FILE.canWrite()).isTrue();
            originalContent = Files.readString(PATCH_SCRIPT_FILE.toPath(), StandardCharsets.UTF_8);
        } else {
            originalContent = null;
        }
        final StringBuilder sb = new StringBuilder();
        Stream.of(MtConnectSchema.values()).forEach(schema -> {
            sb.append("# ")
                    .append(schema.getType().name())
                    .append(" ")
                    .append(schema.getMajorVersion())
                    .append(".")
                    .append(schema.getMinorVersion())
                    .append("\n");
            final var typeNameGroups = MIXED_SCHEMA_TO_TYPE_NAME_GROUPS_MAP.get(schema);
            if (typeNameGroups != null) {
                assertThat(typeNameGroups.size()).isGreaterThan(0);
                sb.append(getPatchCommand(schema, "patched-" + getSchemaFileName(schema))).append("\n");
                final String relativePath = "../src/main/java/com/hivemq/edge/adapters/mtconnect/schemas/" +
                        schema.getType().name().toLowerCase() +
                        "/" +
                        schema.getType().name().toLowerCase() +
                        "_" +
                        schema.getMajorVersion() +
                        "_" +
                        schema.getMinorVersion();
                sb.append("mkdir -p ").append(relativePath).append("/backup\n");
                sb.append("cp -f");
                typeNameGroups.stream()
                        .flatMap(typeNameGroup -> typeNameGroup.derivedTypeNames.stream())
                        .forEach(typeName -> sb.append(" ")
                                .append(relativePath)
                                .append("/")
                                .append(typeName)
                                .append(".java"));
                sb.append(" ").append(relativePath).append("/backup\n");
                sb.append(getPatchCommand(schema, getSchemaFileName(schema))).append("\n");
                sb.append("cp -f ").append(relativePath).append("/backup/*.java ").append(relativePath).append("\n");
                sb.append("rm -rf ").append(relativePath).append("/backup\n");
            } else {
                sb.append(getPatchCommand(schema, getSchemaFileName(schema))).append("\n");
            }
        });
        String content = sb.toString();
        if (!Objects.equals(originalContent, content)) {
            Files.writeString(PATCH_SCRIPT_FILE.toPath(), content, StandardCharsets.UTF_8);
            Files.setPosixFilePermissions(PATCH_SCRIPT_FILE.toPath(),
                    Set.of(PosixFilePermission.OWNER_WRITE,
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.GROUP_READ,
                            PosixFilePermission.OTHERS_READ,
                            PosixFilePermission.OWNER_EXECUTE,
                            PosixFilePermission.GROUP_EXECUTE,
                            PosixFilePermission.OTHERS_EXECUTE));
        }
    }

    @Test
    public void generatePatchJaxbForDevices() throws IOException {
        if (!schemaFound) {
            return;
        }
        for (var schema : Stream.of(MtConnectSchema.values())
                .filter(schema -> schema.getType() == MtConnectSchemaType.Devices)
                .filter(schema -> (schema.getMajorVersion() == 1 && schema.getMinorVersion() >= 5) ||
                        schema.getMajorVersion() == 2)
                .toList()) {
            final File patchJaxbFile = new File(SCHEMA_FOLDER_FILE, getPatchSchemaFileName(schema));
            final String originalContent;
            if (patchJaxbFile.exists()) {
                assertThat(patchJaxbFile.isFile()).isTrue();
                assertThat(patchJaxbFile.canRead()).isTrue();
                assertThat(patchJaxbFile.canWrite()).isTrue();
                originalContent = Files.readString(patchJaxbFile.toPath(), StandardCharsets.UTF_8);
            } else {
                originalContent = null;
            }
            final StringBuilder sb = new StringBuilder();
            sb.append("""
                            <?xml version='1.0' encoding='UTF-8'?>
                            <bindings xmlns="http://java.sun.com/xml/ns/jaxb"
                                xmlns:xsi="http://www.w3.org/2000/10/XMLSchema-instance"
                                xmlns:xs="http://www.w3.org/2001/XMLSchema" version="2.1">
                                <bindings schemaLocation="MTConnectDevices_""".trim())
                    .append(schema.getMajorVersion())
                    .append(".")
                    .append(schema.getMinorVersion())
                    .append("""
                                    .xsd" version="1.0">
                                    <bindings node="//xs:complexType[@name='DeviceRelationshipType']">
                                        <bindings node=".//xs:attribute[@ref='xlink:type']">
                                            <property name="typeOfDeviceRelationship"/>
                                        </bindings>
                                    </bindings>
                                </bindings>
                            </bindings>""".trim());
            if (!Objects.equals(originalContent, sb.toString())) {
                Files.writeString(patchJaxbFile.toPath(), sb.toString(), StandardCharsets.UTF_8);
            }
        }
    }

    @Test
    public void generatePatchJaxbForStreams() throws Exception {
        if (!schemaFound) {
            return;
        }
        for (final var entry : MIXED_SCHEMA_TO_TYPE_NAME_GROUPS_MAP.entrySet()) {
            final var schema = entry.getKey();
            final var typeNameGroups = entry.getValue();
            final File schemaFile = new File(SCHEMA_FOLDER_FILE, getSchemaFileName(schema));
            assertThat(schemaFile.exists()).isTrue();
            assertThat(schemaFile.isFile()).isTrue();
            assertThat(schemaFile.canRead()).isTrue();
            assertThat(schemaFile.canWrite()).isTrue();
            final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            final Document document = documentBuilder.parse(schemaFile);
            document.getDocumentElement().normalize();
            final XPathFactory xPathfactory = XPathFactory.newInstance();
            final XPath xPath = xPathfactory.newXPath();
            for (final var typeNameGroup : typeNameGroups) {
                for (final String typeName : typeNameGroup.getAllTypeNames()) {
                    final XPathExpression xPathExpression =
                            xPath.compile("//*[local-name()='complexType' and @mixed='true' and @name='" +
                                    typeName +
                                    "']");
                    final NodeList nodeList = (NodeList) xPathExpression.evaluate(document, XPathConstants.NODESET);
                    assertThat(nodeList).isNotNull();
                    final int length = nodeList.getLength();
                    assertThat(length).isOne();
                    assertThat(nodeList.item(0)).isInstanceOf(Element.class);
                    final Element element = (Element) nodeList.item(0);
                    assertThat(element.getAttribute("mixed")).isEqualTo("true");
                    element.setAttribute("mixed", "false");
                }
            }
            final TransformerFactory transformerFactory = TransformerFactory.newInstance();
            final Transformer transformer = transformerFactory.newTransformer();
            try (final StringWriter stringWriter = new StringWriter()) {
                final StreamResult streamResult = new StreamResult(stringWriter);
                transformer.transform(new DOMSource(document), streamResult);
                final String patchedContent = stringWriter.getBuffer().toString();
                final File patchedSchemaFile = new File(SCHEMA_FOLDER_FILE, "patched-" + getSchemaFileName(schema));
                final String originalContent;
                if (patchedSchemaFile.exists()) {
                    assertThat(patchedSchemaFile.isFile()).isTrue();
                    assertThat(patchedSchemaFile.canRead()).isTrue();
                    assertThat(patchedSchemaFile.canWrite()).isTrue();
                    originalContent = Files.readString(patchedSchemaFile.toPath(), StandardCharsets.UTF_8);
                } else {
                    originalContent = null;
                }
                if (!Objects.equals(originalContent, patchedContent)) {
                    Files.writeString(patchedSchemaFile.toPath(), patchedContent, StandardCharsets.UTF_8);
                }
            }
        }
    }

    protected record MixedTypeNameGroup(String baseTypeName, List<String> derivedTypeNames) {
        public @NotNull List<String> getAllTypeNames() {
            return Stream.concat(Stream.of(baseTypeName), derivedTypeNames.stream()).toList();
        }
    }
}
