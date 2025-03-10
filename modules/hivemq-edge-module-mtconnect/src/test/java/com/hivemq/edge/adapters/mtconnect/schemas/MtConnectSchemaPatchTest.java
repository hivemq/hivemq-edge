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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class MtConnectSchemaPatchTest {
    private static final @NotNull File SCHEMA_FOLDER_FILE = new File("schema");
    private static final @NotNull File PATCH_SCRIPT_FILE = new File(SCHEMA_FOLDER_FILE, "patch-script.sh");
    private final boolean schemaFound;

    public MtConnectSchemaPatchTest() {
        schemaFound = SCHEMA_FOLDER_FILE.exists();
    }

    @BeforeEach
    public void setUp() {
        if (schemaFound) {
            assertThat(SCHEMA_FOLDER_FILE.isDirectory()).isTrue();
        }
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
            final boolean patchRequired;
            switch (schema.getType()) {
                case Devices -> {
                    if ((schema.getMajorVersion() == 1 && schema.getMinorVersion() >= 5) ||
                            schema.getMajorVersion() == 2) {
                        patchRequired = true;
                    } else {
                        patchRequired = false;
                    }
                }
                default -> patchRequired = false;
            }
            if (patchRequired) {
                sb.append("-b ").append(getPatchSchemaFileName(schema));
            }
            sb.append("MTConnect")
                    .append(schema.getType().name())
                    .append("_")
                    .append(schema.getMajorVersion())
                    .append(".")
                    .append(schema.getMinorVersion())
                    .append(".xsd\n");
        });
        String content = sb.toString();
        if (!Objects.equals(originalContent, content)) {
            Files.writeString(PATCH_SCRIPT_FILE.toPath(), content, StandardCharsets.UTF_8);
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("linux") || os.contains("mac")) {
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

    @Test
    public void generatePatchJaxbForDevices() throws IOException {
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
                                    <!-- Customise the package name -->
                                    <schemaBindings>
                                        <package name="com.hivemq.edge.adapters.mtconnect.schemas.devices"/>
                                    </schemaBindings>
                            
                                    <!-- rename the value element -->
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
}
