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

package com.hivemq.configuration;

import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.adapter.fieldmapping.InstructionEntity;
import com.hivemq.configuration.entity.combining.DataCombinerEntity;
import com.hivemq.configuration.entity.combining.DataCombiningDestinationEntity;
import com.hivemq.configuration.entity.combining.DataCombiningEntity;
import com.hivemq.configuration.entity.combining.DataCombiningSourcesEntity;
import com.hivemq.configuration.entity.combining.DataIdentifierReferenceEntity;
import com.hivemq.configuration.entity.combining.EntityReferenceEntity;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.SchemaOutputResolver;

import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;

/**
 * Utility class to generate XSD schema from JAXB-annotated configuration entity classes.
 * <p>
 * This generates the structural XSD from the Java annotations and adds custom simple types
 * for value constraints that JAXB cannot express.
 * <p>
 * Usage: Run the main method with an optional output file path as argument.
 * If no argument is provided, outputs to build/generated-xsd/config-generated.xsd
 */
public class GenSchemaMain {

    /**
     * All classes that need to be included in the schema generation.
     * This includes the root entity and all referenced entities that use @XmlRootElement.
     */
    private static final Class<?>[] SCHEMA_CLASSES = {
            HiveMQConfigEntity.class,
            // Data combiner entities (for full XSD compliance)
            DataCombinerEntity.class,
            DataCombiningEntity.class,
            DataCombiningSourcesEntity.class,
            DataCombiningDestinationEntity.class,
            DataIdentifierReferenceEntity.class,
            EntityReferenceEntity.class,
            InstructionEntity.class
    };

    /**
     * Custom simple types to add to the generated schema.
     * These provide value constraints that JAXB cannot express through annotations.
     */
    private static final String CUSTOM_SIMPLE_TYPES = """

              <!-- Custom simple types for value constraints -->
              <xs:simpleType name="port">
                <xs:restriction base="xs:int">
                  <xs:minInclusive value="0"/>
                  <xs:maxInclusive value="65535"/>
                </xs:restriction>
              </xs:simpleType>

              <xs:simpleType name="nonEmptyString">
                <xs:restriction base="xs:string">
                  <xs:minLength value="1"/>
                  <xs:whiteSpace value="collapse"/>
                </xs:restriction>
              </xs:simpleType>

              <xs:simpleType name="uuidType">
                <xs:restriction base="xs:string">
                  <xs:pattern value="[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}"/>
                </xs:restriction>
              </xs:simpleType>

              <xs:simpleType name="clientAuthenticationMode">
                <xs:restriction base="xs:string">
                  <xs:enumeration value="OPTIONAL"/>
                  <xs:enumeration value="REQUIRED"/>
                  <xs:enumeration value="NONE"/>
                </xs:restriction>
              </xs:simpleType>

              <xs:simpleType name="qosType">
                <xs:restriction base="xs:int">
                  <xs:minInclusive value="0"/>
                  <xs:maxInclusive value="2"/>
                </xs:restriction>
              </xs:simpleType>

              <xs:simpleType name="positiveInteger">
                <xs:restriction base="xs:int">
                  <xs:minInclusive value="1"/>
                </xs:restriction>
              </xs:simpleType>

              <xs:simpleType name="nonNegativeInteger">
                <xs:restriction base="xs:int">
                  <xs:minInclusive value="0"/>
                </xs:restriction>
              </xs:simpleType>

              <xs:simpleType name="nonNegativeLong">
                <xs:restriction base="xs:long">
                  <xs:minInclusive value="0"/>
                </xs:restriction>
              </xs:simpleType>

            """;

    public static void main(String[] args) throws Exception {
        String outputPath = args.length > 0 ? args[0] : "build/generated-xsd/config-generated.xsd";
        File outputFile = new File(outputPath);

        // Ensure parent directory exists
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("Failed to create directory: " + parentDir.getAbsolutePath());
            }
        }

        generateSchema(outputFile);
        System.out.println("XSD schema generated successfully: " + outputFile.getAbsolutePath());
    }

    /**
     * Generates the XSD schema to the specified file.
     *
     * @param outputFile the file to write the schema to
     * @throws JAXBException if JAXB context creation fails
     * @throws IOException   if file writing fails
     */
    public static void generateSchema(File outputFile) throws JAXBException, IOException {
        // Generate to a temporary file first
        File tempFile = File.createTempFile("schema", ".xsd");
        tempFile.deleteOnExit();

        JAXBContext context = JAXBContext.newInstance(SCHEMA_CLASSES);

        context.generateSchema(new SchemaOutputResolver() {
            @Override
            public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
                StreamResult result = new StreamResult(tempFile);
                result.setSystemId(tempFile.toURI().toString());
                return result;
            }
        });

        // Post-process to add custom simple types
        addCustomSimpleTypes(tempFile, outputFile);
    }

    /**
     * Adds custom simple types to the generated XSD by inserting them before the closing schema tag.
     *
     * @param inputFile  the generated XSD file
     * @param outputFile the final output file with custom types added
     * @throws IOException if file operations fail
     */
    private static void addCustomSimpleTypes(File inputFile, File outputFile) throws IOException {
        String content = Files.readString(inputFile.toPath());

        // Find the closing </xs:schema> tag and insert custom types before it
        int closingTagIndex = content.lastIndexOf("</xs:schema>");
        if (closingTagIndex == -1) {
            throw new IOException("Could not find closing </xs:schema> tag in generated XSD");
        }

        String modifiedContent = content.substring(0, closingTagIndex) +
                CUSTOM_SIMPLE_TYPES +
                content.substring(closingTagIndex);

        Files.writeString(outputFile.toPath(), modifiedContent);
    }
}
