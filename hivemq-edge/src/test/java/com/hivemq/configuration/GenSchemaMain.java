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
import com.hivemq.configuration.entity.listener.TCPListenerEntity;
import com.hivemq.configuration.entity.listener.TlsTCPListenerEntity;
import com.hivemq.configuration.entity.listener.WebsocketListenerEntity;
import com.hivemq.configuration.entity.listener.TlsWebsocketListenerEntity;
import com.hivemq.configuration.entity.listener.UDPListenerEntity;
import com.hivemq.configuration.entity.listener.UDPBroadcastListenerEntity;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.SchemaOutputResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;

/**
 * Utility class to generate XSD schema from JAXB-annotated configuration entity classes.
 * <p>
 * This generates the structural XSD from the Java annotations and post-processes it
 * to ensure backwards compatibility with existing configuration files.
 * <p>
 * Post-processing includes:
 * <ul>
 *   <li>Replacing xs:sequence with xs:all for flexible element ordering</li>
 *   <li>Adding proper listener type choices (tcp-listener, tls-tcp-listener, etc.)</li>
 *   <li>Adding xs:any for protocol-adapters to support legacy adapter configs</li>
 *   <li>Adding custom simple types for value constraints</li>
 * </ul>
 * <p>
 * Usage: Run the main method with an optional output file path as argument.
 * If no argument is provided, outputs to build/generated-xsd/config-generated.xsd
 */
public class GenSchemaMain {

    private static final String XS_NAMESPACE = "http://www.w3.org/2001/XMLSchema";

    /**
     * All classes that need to be included in the schema generation.
     */
    private static final Class<?>[] SCHEMA_CLASSES = {
            HiveMQConfigEntity.class,
            // Data combiner entities
            DataCombinerEntity.class,
            DataCombiningEntity.class,
            DataCombiningSourcesEntity.class,
            DataCombiningDestinationEntity.class,
            DataIdentifierReferenceEntity.class,
            EntityReferenceEntity.class,
            InstructionEntity.class,
            // MQTT Listener entities
            TCPListenerEntity.class,
            TlsTCPListenerEntity.class,
            WebsocketListenerEntity.class,
            TlsWebsocketListenerEntity.class,
            // MQTT-SN Listener entities
            UDPListenerEntity.class,
            UDPBroadcastListenerEntity.class
    };

    public static void main(String[] args) throws Exception {
        String outputPath = args.length > 0 ? args[0] : "build/generated-xsd/config-generated.xsd";
        File outputFile = new File(outputPath);

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
     * Generates the XSD schema to the specified file with all post-processing applied.
     */
    public static void generateSchema(File outputFile) throws Exception {
        // Step 1: Generate base schema from JAXB
        File tempFile = File.createTempFile("schema", ".xsd");
        tempFile.deleteOnExit();
        generateBaseSchema(tempFile);

        // Step 2: Load and post-process the schema
        Document doc = loadXmlDocument(tempFile);
        postProcessSchema(doc);

        // Step 3: Write the final schema
        writeXmlDocument(doc, outputFile);
    }

    private static void generateBaseSchema(File outputFile) throws JAXBException, IOException {
        JAXBContext context = JAXBContext.newInstance(SCHEMA_CLASSES);
        context.generateSchema(new SchemaOutputResolver() {
            @Override
            public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
                StreamResult result = new StreamResult(outputFile);
                result.setSystemId(outputFile.toURI().toString());
                return result;
            }
        });
    }

    private static Document loadXmlDocument(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(file);
    }

    private static void writeXmlDocument(Document doc, File outputFile) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(new DOMSource(doc), new StreamResult(outputFile));
    }

    /**
     * Applies all post-processing transformations to make the schema backwards compatible.
     */
    private static void postProcessSchema(Document doc) {
        replaceSequenceWithAllForRootEntity(doc);
        makeConfigVersionOptional(doc);
        fixMqttListeners(doc);
        fixMqttSnListeners(doc);
        fixProtocolAdapters(doc);
        fixModules(doc);
        fixDataCombinerEntity(doc);
        fixEmptyElementTypes(doc);
        addCustomSimpleTypes(doc);
    }

    /**
     * Makes the config-version element optional (minOccurs="0").
     */
    private static void makeConfigVersionOptional(Document doc) {
        Element complexType = findComplexTypeByName(doc, "hiveMQConfigEntity");
        if (complexType == null) return;

        Element configVersionElement = findChildElementByName(complexType, "config-version");
        if (configVersionElement != null) {
            configVersionElement.setAttribute("minOccurs", "0");
        }
    }

    /**
     * Replaces xs:sequence with xs:all in hiveMQConfigEntity to allow elements in any order.
     */
    private static void replaceSequenceWithAllForRootEntity(Document doc) {
        Element complexType = findComplexTypeByName(doc, "hiveMQConfigEntity");
        if (complexType == null) {
            System.err.println("Warning: Could not find hiveMQConfigEntity complexType");
            return;
        }

        // Find the xs:sequence child
        NodeList children = complexType.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element && "sequence".equals(child.getLocalName())) {
                // Create new xs:all element
                Element allElement = doc.createElementNS(XS_NAMESPACE, "xs:all");

                // Move all children from sequence to all
                NodeList sequenceChildren = child.getChildNodes();
                while (sequenceChildren.getLength() > 0) {
                    Node seqChild = sequenceChildren.item(0);
                    allElement.appendChild(seqChild);
                }

                // Replace sequence with all
                complexType.replaceChild(allElement, child);
                break;
            }
        }
    }

    /**
     * Fixes mqtt-listeners to use xs:choice with all listener types.
     */
    private static void fixMqttListeners(Document doc) {
        fixListenerElement(doc, "mqtt-listeners", new String[]{
                "tcp-listener", "tls-tcp-listener", "websocket-listener", "tls-websocket-listener"
        });
    }

    /**
     * Fixes mqtt-sn-listeners to use xs:choice with appropriate listener types.
     */
    private static void fixMqttSnListeners(Document doc) {
        fixListenerElement(doc, "mqtt-sn-listeners", new String[]{
                "udp-listener", "udp-broadcast-listener"
        });
    }

    /**
     * Generic method to fix listener wrapper elements with proper xs:choice.
     */
    private static void fixListenerElement(Document doc, String elementName, String[] listenerTypes) {
        // Find the element in hiveMQConfigEntity
        Element complexType = findComplexTypeByName(doc, "hiveMQConfigEntity");
        if (complexType == null) return;

        Element listenersElement = findChildElementByName(complexType, elementName);
        if (listenersElement == null) return;

        // Find the inner complexType
        Element innerComplexType = findFirstChildElement(listenersElement, "complexType");
        if (innerComplexType == null) return;

        // Replace the content with xs:choice
        // Remove existing children (xs:sequence with xs:element ref="address")
        while (innerComplexType.hasChildNodes()) {
            innerComplexType.removeChild(innerComplexType.getFirstChild());
        }

        // Create xs:choice with all listener types
        Element choice = doc.createElementNS(XS_NAMESPACE, "xs:choice");
        choice.setAttribute("minOccurs", "0");
        choice.setAttribute("maxOccurs", "unbounded");

        for (String listenerType : listenerTypes) {
            Element elementRef = doc.createElementNS(XS_NAMESPACE, "xs:element");
            elementRef.setAttribute("ref", listenerType);
            choice.appendChild(elementRef);
        }

        innerComplexType.appendChild(choice);
    }

    /**
     * Fixes protocol-adapters to use xs:any for both new and legacy adapter formats.
     * Using xs:any alone avoids non-determinism issues that occur when mixing
     * named elements with xs:any in a choice.
     */
    private static void fixProtocolAdapters(Document doc) {
        Element complexType = findComplexTypeByName(doc, "hiveMQConfigEntity");
        if (complexType == null) return;

        Element adaptersElement = findChildElementByName(complexType, "protocol-adapters");
        if (adaptersElement == null) return;

        Element innerComplexType = findFirstChildElement(adaptersElement, "complexType");
        if (innerComplexType == null) return;

        // Clear existing content
        while (innerComplexType.hasChildNodes()) {
            innerComplexType.removeChild(innerComplexType.getFirstChild());
        }

        // Create xs:sequence with xs:any (skip validation for all adapter elements)
        // This allows both <protocol-adapter> and legacy adapter-specific elements like <simulation>
        Element sequence = doc.createElementNS(XS_NAMESPACE, "xs:sequence");

        Element choice = doc.createElementNS(XS_NAMESPACE, "xs:choice");
        choice.setAttribute("minOccurs", "0");
        choice.setAttribute("maxOccurs", "unbounded");

        // Use xs:any alone to avoid non-determinism
        Element anyElement = doc.createElementNS(XS_NAMESPACE, "xs:any");
        anyElement.setAttribute("processContents", "skip");
        choice.appendChild(anyElement);

        sequence.appendChild(choice);
        innerComplexType.appendChild(sequence);
    }

    /**
     * Fixes modules element to use xs:any for arbitrary module configurations.
     */
    private static void fixModules(Document doc) {
        Element complexType = findComplexTypeByName(doc, "hiveMQConfigEntity");
        if (complexType == null) return;

        Element modulesElement = findChildElementByName(complexType, "modules");
        if (modulesElement == null) return;

        // Remove the type attribute and add inline complexType with xs:any
        modulesElement.removeAttribute("type");

        // Create inline complexType
        Element innerComplexType = doc.createElementNS(XS_NAMESPACE, "xs:complexType");
        Element sequence = doc.createElementNS(XS_NAMESPACE, "xs:sequence");

        Element anyElement = doc.createElementNS(XS_NAMESPACE, "xs:any");
        anyElement.setAttribute("processContents", "skip");
        anyElement.setAttribute("minOccurs", "0");
        anyElement.setAttribute("maxOccurs", "unbounded");

        sequence.appendChild(anyElement);
        innerComplexType.appendChild(sequence);
        modulesElement.appendChild(innerComplexType);
    }

    /**
     * Fixes dataCombinerEntity and dataCombiningEntity to use xs:all instead of xs:sequence
     * for flexible element ordering, and makes wrapper elements optional.
     */
    private static void fixDataCombinerEntity(Document doc) {
        replaceSequenceWithAll(doc, "dataCombinerEntity");
        replaceSequenceWithAll(doc, "dataCombiningEntity");

        // Make wrapper elements optional in dataCombinerEntity
        makeElementOptionalInType(doc, "dataCombinerEntity", "entity-references");
        makeElementOptionalInType(doc, "dataCombinerEntity", "data-combinings");

        // Make sources optional in dataCombiningEntity
        makeElementOptionalInType(doc, "dataCombiningEntity", "sources");
    }

    /**
     * Makes a specific element optional (minOccurs="0") within a complex type.
     */
    private static void makeElementOptionalInType(Document doc, String typeName, String elementName) {
        Element complexType = findComplexTypeByName(doc, typeName);
        if (complexType == null) return;

        Element element = findChildElementByName(complexType, elementName);
        if (element != null) {
            element.setAttribute("minOccurs", "0");
        }
    }

    /**
     * Replaces xs:sequence with xs:all in the specified complex type.
     */
    private static void replaceSequenceWithAll(Document doc, String typeName) {
        Element complexType = findComplexTypeByName(doc, typeName);
        if (complexType == null) return;

        NodeList children = complexType.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element && "sequence".equals(child.getLocalName())) {
                Element allElement = doc.createElementNS(XS_NAMESPACE, "xs:all");

                // Move all children from sequence to all
                NodeList sequenceChildren = child.getChildNodes();
                while (sequenceChildren.getLength() > 0) {
                    Node seqChild = sequenceChildren.item(0);
                    allElement.appendChild(seqChild);
                }

                complexType.replaceChild(allElement, child);
                break;
            }
        }
    }

    /**
     * Fixes complex types that can appear as empty elements by making all children optional.
     * This allows configurations like {@code <mqtt-sn/>} or {@code <admin-api/>} to validate.
     */
    private static void fixEmptyElementTypes(Document doc) {
        String[] typesToFix = {
                "mqttSnConfigEntity",
                "adminApiEntity",
                "dynamicConfigEntity",
                "usageTrackingConfigEntity",
                // Base types that are inherited by types above
                "enabledEntity"
        };

        for (String typeName : typesToFix) {
            makeAllChildrenOptional(doc, typeName);
        }
    }

    /**
     * Makes all child elements optional (minOccurs="0") in the specified complex type.
     */
    private static void makeAllChildrenOptional(Document doc, String typeName) {
        Element complexType = findComplexTypeByName(doc, typeName);
        if (complexType == null) return;

        NodeList elements = complexType.getElementsByTagNameNS(XS_NAMESPACE, "element");
        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element) elements.item(i);
            if (!element.hasAttribute("minOccurs")) {
                element.setAttribute("minOccurs", "0");
            }
        }
    }

    /**
     * Adds custom simple types for value constraints (only if they don't already exist).
     */
    private static void addCustomSimpleTypes(Document doc) {
        Element schemaElement = doc.getDocumentElement();

        addSimpleTypeIfNotExists(doc, schemaElement, "port", "xs:int",
                new String[]{"minInclusive", "0"}, new String[]{"maxInclusive", "65535"});

        addSimpleTypeIfNotExists(doc, schemaElement, "nonEmptyString", "xs:string",
                new String[]{"minLength", "1"}, new String[]{"whiteSpace", "collapse"});

        addSimpleTypeIfNotExists(doc, schemaElement, "uuidType", "xs:string",
                new String[]{"pattern", "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}"});

        // clientAuthenticationMode is often generated by TLS entities, only add if missing
        addSimpleTypeWithEnumerationIfNotExists(doc, schemaElement, "clientAuthenticationMode", "xs:string",
                "OPTIONAL", "REQUIRED", "NONE");

        addSimpleTypeIfNotExists(doc, schemaElement, "qosType", "xs:int",
                new String[]{"minInclusive", "0"}, new String[]{"maxInclusive", "2"});

        addSimpleTypeIfNotExists(doc, schemaElement, "positiveInteger", "xs:int",
                new String[]{"minInclusive", "1"});

        addSimpleTypeIfNotExists(doc, schemaElement, "nonNegativeInteger", "xs:int",
                new String[]{"minInclusive", "0"});

        addSimpleTypeIfNotExists(doc, schemaElement, "nonNegativeLong", "xs:long",
                new String[]{"minInclusive", "0"});
    }

    private static boolean simpleTypeExists(Document doc, String name) {
        NodeList simpleTypes = doc.getElementsByTagNameNS(XS_NAMESPACE, "simpleType");
        for (int i = 0; i < simpleTypes.getLength(); i++) {
            Element st = (Element) simpleTypes.item(i);
            if (name.equals(st.getAttribute("name"))) {
                return true;
            }
        }
        return false;
    }

    private static void addSimpleTypeIfNotExists(Document doc, Element parent, String name, String baseType, String[]... facets) {
        if (simpleTypeExists(doc, name)) {
            return;
        }
        addSimpleType(doc, parent, name, baseType, facets);
    }

    private static void addSimpleTypeWithEnumerationIfNotExists(Document doc, Element parent, String name, String baseType, String... values) {
        if (simpleTypeExists(doc, name)) {
            return;
        }
        addSimpleTypeWithEnumeration(doc, parent, name, baseType, values);
    }

    private static void addSimpleType(Document doc, Element parent, String name, String baseType, String[]... facets) {
        Element simpleType = doc.createElementNS(XS_NAMESPACE, "xs:simpleType");
        simpleType.setAttribute("name", name);

        Element restriction = doc.createElementNS(XS_NAMESPACE, "xs:restriction");
        restriction.setAttribute("base", baseType);

        for (String[] facet : facets) {
            Element facetElement = doc.createElementNS(XS_NAMESPACE, "xs:" + facet[0]);
            facetElement.setAttribute("value", facet[1]);
            restriction.appendChild(facetElement);
        }

        simpleType.appendChild(restriction);
        parent.appendChild(simpleType);
    }

    private static void addSimpleTypeWithEnumeration(Document doc, Element parent, String name, String baseType, String... values) {
        Element simpleType = doc.createElementNS(XS_NAMESPACE, "xs:simpleType");
        simpleType.setAttribute("name", name);

        Element restriction = doc.createElementNS(XS_NAMESPACE, "xs:restriction");
        restriction.setAttribute("base", baseType);

        for (String value : values) {
            Element enumElement = doc.createElementNS(XS_NAMESPACE, "xs:enumeration");
            enumElement.setAttribute("value", value);
            restriction.appendChild(enumElement);
        }

        simpleType.appendChild(restriction);
        parent.appendChild(simpleType);
    }

    // Helper methods for DOM traversal

    private static Element findComplexTypeByName(Document doc, String name) {
        NodeList complexTypes = doc.getElementsByTagNameNS(XS_NAMESPACE, "complexType");
        for (int i = 0; i < complexTypes.getLength(); i++) {
            Element ct = (Element) complexTypes.item(i);
            if (name.equals(ct.getAttribute("name"))) {
                return ct;
            }
        }
        return null;
    }

    private static Element findChildElementByName(Element parent, String elementName) {
        // Search through xs:all or xs:sequence children
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                Element childElement = (Element) child;
                String localName = childElement.getLocalName();
                if ("all".equals(localName) || "sequence".equals(localName)) {
                    // Search within all/sequence
                    Element found = findElementWithNameAttribute(childElement, elementName);
                    if (found != null) return found;
                }
            }
        }
        return null;
    }

    private static Element findElementWithNameAttribute(Element parent, String nameValue) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                Element childElement = (Element) child;
                if ("element".equals(childElement.getLocalName())) {
                    // Check both "name" attribute and "ref" attribute
                    String name = childElement.getAttribute("name");
                    String ref = childElement.getAttribute("ref");
                    if (nameValue.equals(name) || nameValue.equals(ref)) {
                        return childElement;
                    }
                }
            }
        }
        return null;
    }

    private static Element findFirstChildElement(Element parent, String localName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element && localName.equals(child.getLocalName())) {
                return (Element) child;
            }
        }
        return null;
    }
}
