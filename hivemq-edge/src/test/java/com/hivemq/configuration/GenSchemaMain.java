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
     * <p>
     * <b>Why this is needed:</b>
     * The {@code config-version} element has a default value in the Java entity, but JAXB generates
     * schema elements as required by default. Existing config files may omit this element entirely.
     * <p>
     * <b>Why JAXB cannot express this directly:</b>
     * JAXB's {@code @XmlElement(required = false)} only affects marshalling behavior (whether null
     * values are written), not schema generation. JAXB always generates elements without
     * {@code minOccurs="0"} unless they are part of a collection. There is no annotation to
     * explicitly set {@code minOccurs} in the generated schema.
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
     * <p>
     * <b>Why this is needed:</b>
     * JAXB always generates {@code xs:sequence} for complex types, which requires XML elements
     * to appear in a specific order matching the field declaration order in the Java class.
     * However, existing config files have elements in arbitrary order, so we need {@code xs:all}
     * which allows elements in any order.
     * <p>
     * <b>Why JAXB cannot express this directly:</b>
     * <ul>
     *   <li>There is no JAXB annotation to generate {@code xs:all} instead of {@code xs:sequence}.
     *       {@code @XmlType(propOrder = {...})} controls element order within a sequence but
     *       cannot switch to {@code xs:all}.</li>
     *   <li>JAXB was designed primarily for marshalling/unmarshalling Java objects, not for
     *       schema-first design. The Java object model naturally maps to sequences (ordered fields).</li>
     *   <li>In XSD 1.0, {@code xs:all} has restrictions (each element can appear at most once,
     *       cannot be nested within other model groups) that make it less suitable for JAXB's
     *       general-purpose schema generation.</li>
     * </ul>
     * <p>
     * Post-processing the generated XSD is the standard workaround for this JAXB limitation.
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
     * <p>
     * <b>Why this is needed:</b>
     * The mqtt-listeners wrapper contains polymorphic listener elements (tcp-listener, tls-tcp-listener,
     * websocket-listener, tls-websocket-listener). The schema needs {@code xs:choice} to allow any of
     * these element types in any order and quantity.
     * <p>
     * <b>Why JAXB cannot express this directly:</b>
     * The Java entity uses {@code @XmlElementRef} on a {@code List<ListenerEntity>} where
     * {@code ListenerEntity} is a base class. JAXB generates a reference to an abstract type or
     * a single element reference, not an {@code xs:choice} with all concrete subtypes. JAXB's
     * {@code @XmlElements} annotation can list multiple types but generates {@code xs:choice}
     * only at the element level, not properly handling the inheritance hierarchy with
     * {@code @XmlElementWrapper}.
     */
    private static void fixMqttListeners(Document doc) {
        fixListenerElement(doc, "mqtt-listeners", new String[]{
                "tcp-listener", "tls-tcp-listener", "websocket-listener", "tls-websocket-listener"
        });
    }

    /**
     * Fixes mqtt-sn-listeners to use xs:choice with appropriate listener types.
     * <p>
     * See {@link #fixMqttListeners(Document)} for explanation of why this post-processing is needed.
     */
    private static void fixMqttSnListeners(Document doc) {
        fixListenerElement(doc, "mqtt-sn-listeners", new String[]{
                "udp-listener", "udp-broadcast-listener"
        });
    }

    /**
     * Generic method to fix listener wrapper elements with proper xs:choice.
     *
     * @param doc           the XSD document to modify
     * @param elementName   the wrapper element name (e.g., "mqtt-listeners")
     * @param listenerTypes the concrete listener element names to include in the choice
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
     * <p>
     * <b>Why this is needed:</b>
     * Protocol adapters can be configured in two ways: the new format uses {@code <protocol-adapter>}
     * elements, while legacy configs use adapter-specific element names like {@code <simulation>},
     * {@code <opcua>}, etc. The schema must accept any element within protocol-adapters.
     * <p>
     * <b>Why JAXB cannot express this directly:</b>
     * <ul>
     *   <li>JAXB has {@code @XmlAnyElement} which generates {@code xs:any}, but it cannot be
     *       combined with typed elements in the same collection.</li>
     *   <li>The Java entity uses {@code @XmlElement(name = "protocol-adapter")} for the new format,
     *       which generates a specific element reference, not a wildcard.</li>
     *   <li>To support both formats, we need {@code xs:any processContents="skip"} which tells
     *       the validator to accept any element without validation - this cannot be expressed
     *       through JAXB annotations while keeping the typed Java binding.</li>
     * </ul>
     * <p>
     * Note: Using {@code xs:any} alone avoids non-determinism issues that occur when mixing
     * named elements with {@code xs:any} in a choice.
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
     * <p>
     * <b>Why this is needed:</b>
     * The modules element contains arbitrary configuration for dynamically loaded modules.
     * Each module can define its own XML structure, so the schema cannot know the element names
     * or structure in advance.
     * <p>
     * <b>Why JAXB cannot express this directly:</b>
     * The Java entity uses {@code @XmlJavaTypeAdapter(ArbitraryValuesMapAdapter.class)} to handle
     * the dynamic content as a {@code Map<String, Object>}. JAXB generates a reference to the
     * adapter's mapped type, not {@code xs:any}. While {@code @XmlAnyElement} exists, it requires
     * the field to be {@code List<Element>} or similar DOM types, which would change the Java API.
     * The adapter approach provides a cleaner Java API but requires schema post-processing.
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
     * <p>
     * <b>Why this is needed:</b>
     * Like the root entity, data combiner configurations may have elements in any order in
     * existing config files. Additionally, wrapper elements like {@code entity-references} and
     * {@code data-combinings} should be optional when empty.
     * <p>
     * <b>Why JAXB cannot express this directly:</b>
     * See {@link #replaceSequenceWithAllForRootEntity(Document)} for the explanation of why
     * {@code xs:all} cannot be generated by JAXB. For the optional wrapper elements, JAXB's
     * {@code @XmlElementWrapper} does not support a {@code required} attribute - wrappers are
     * always generated as required in the schema even when the collection can be empty.
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
     *
     * @param doc         the XSD document to modify
     * @param typeName    the name of the complex type containing the element
     * @param elementName the name of the element to make optional
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
     * <p>
     * See {@link #replaceSequenceWithAllForRootEntity(Document)} for explanation of why this is needed.
     *
     * @param doc      the XSD document to modify
     * @param typeName the name of the complex type to modify
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
     * <p>
     * <b>Why this is needed:</b>
     * Some configuration sections can be specified as empty self-closing elements like
     * {@code <mqtt-sn/>} or {@code <admin-api/>} to use all default values. The schema must
     * allow these elements to have no children.
     * <p>
     * <b>Why JAXB cannot express this directly:</b>
     * JAXB generates child elements as required by default. While {@code @XmlElement(required = false)}
     * exists, it only affects marshalling behavior (whether to write null values), not the
     * {@code minOccurs} attribute in the generated schema. There is no JAXB annotation to set
     * {@code minOccurs="0"} on generated elements. Additionally, for types using inheritance
     * (like {@code adminApiEntity} extending {@code enabledEntity}), the base type's elements
     * also need to be made optional, which requires modifying multiple generated complex types.
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
     *
     * @param doc      the XSD document to modify
     * @param typeName the name of the complex type whose children should be made optional
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
     * <p>
     * <b>Why this is needed:</b>
     * XSD simple types with restrictions (like port numbers 0-65535, non-empty strings, UUIDs)
     * provide better validation and documentation than plain {@code xs:string} or {@code xs:int}.
     * <p>
     * <b>Why JAXB cannot express this directly:</b>
     * JAXB maps Java types directly to XSD built-in types (String → xs:string, int → xs:int).
     * There is no annotation to specify XSD facets like {@code minInclusive}, {@code maxInclusive},
     * {@code pattern}, or {@code minLength}. While custom {@code XmlAdapter} implementations can
     * transform values during marshalling/unmarshalling, they do not affect schema generation.
     * Bean Validation annotations (like {@code @Min}, {@code @Max}, {@code @Pattern}) are also
     * not translated to XSD constraints by JAXB.
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
