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
import com.hivemq.configuration.entity.listener.TlsWebsocketListenerEntity;
import com.hivemq.configuration.entity.listener.UDPBroadcastListenerEntity;
import com.hivemq.configuration.entity.listener.UDPListenerEntity;
import com.hivemq.configuration.entity.listener.WebsocketListenerEntity;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.SchemaOutputResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
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

    public static void main(final String[] args) throws Exception {
        final var outputPath = args.length > 0 ? args[0] : "build/generated-xsd/config-generated.xsd";
        final var outputFile = new File(outputPath);

        final var parentDir = outputFile.getParentFile();
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
    public static void generateSchema(final File outputFile) throws Exception {
        // Step 1: Generate base schema from JAXB
        final var tempFile = File.createTempFile("schema", ".xsd");
        tempFile.deleteOnExit();
        generateBaseSchema(tempFile);

        // Step 2: Load and post-process the schema
        final var doc = loadXmlDocument(tempFile);
        postProcessSchema(doc);

        // Step 3: Write the final schema
        writeXmlDocument(doc, outputFile);
    }

    private static void generateBaseSchema(final File outputFile) throws JAXBException, IOException {
        final var context = JAXBContext.newInstance(SCHEMA_CLASSES);
        context.generateSchema(new SchemaOutputResolver() {
            @Override
            public Result createOutput(final String namespaceUri, final String suggestedFileName) {
                final var result = new StreamResult(outputFile);
                result.setSystemId(outputFile.toURI().toString());
                return result;
            }
        });
    }

    private static Document loadXmlDocument(final File file) throws Exception {
        final var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        final var builder = factory.newDocumentBuilder();
        return builder.parse(file);
    }

    private static void writeXmlDocument(final Document doc, final File outputFile) throws Exception {
        final var transformerFactory = TransformerFactory.newInstance();
        final var transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(new DOMSource(doc), new StreamResult(outputFile));
    }

    /**
     * Applies all post-processing transformations to make the schema backwards compatible.
     */
    private static void postProcessSchema(final Document doc) {
        replaceSequenceWithAllForRootEntity(doc);
        makeConfigVersionOptional(doc);
        fixMqttListeners(doc);
        fixMqttSnListeners(doc);
        fixProtocolAdapters(doc);
        fixModules(doc);
        fixDataCombinerEntity(doc);
        fixEmptyElementTypes(doc);
        fixExpiryConfigTypes(doc);
        fixMqttConfigTypes(doc);
        addMixedContentToSequenceTypes(doc);
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
    private static void makeConfigVersionOptional(final Document doc) {
        final var complexType = findComplexTypeByName(doc, "hiveMQConfigEntity");
        if (complexType == null) return;

        final var configVersionElement = findChildElementByName(complexType, "config-version");
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
    private static void replaceSequenceWithAllForRootEntity(final Document doc) {
        final var complexType = findComplexTypeByName(doc, "hiveMQConfigEntity");
        if (complexType == null) {
            System.err.println("Warning: Could not find hiveMQConfigEntity complexType");
            return;
        }

        // Find the xs:sequence child
        final var children = complexType.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final var child = children.item(i);
            if (child instanceof Element && "sequence".equals(child.getLocalName())) {
                // Create new xs:all element
                final var allElement = doc.createElementNS(XS_NAMESPACE, "xs:all");

                // Move all children from sequence to all
                final var sequenceChildren = child.getChildNodes();
                while (sequenceChildren.getLength() > 0) {
                    final var seqChild = sequenceChildren.item(0);
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
    private static void fixMqttListeners(final Document doc) {
        fixListenerElement(doc, "mqtt-listeners", new String[]{
                "tcp-listener", "tls-tcp-listener", "websocket-listener", "tls-websocket-listener"
        });
    }

    /**
     * Fixes mqtt-sn-listeners to use xs:choice with appropriate listener types.
     * <p>
     * See {@link #fixMqttListeners(Document)} for explanation of why this post-processing is needed.
     */
    private static void fixMqttSnListeners(final Document doc) {
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
    private static void fixListenerElement(final Document doc, final String elementName, final String[] listenerTypes) {
        // Find the element in hiveMQConfigEntity
        final var complexType = findComplexTypeByName(doc, "hiveMQConfigEntity");
        if (complexType == null) return;

        final var listenersElement = findChildElementByName(complexType, elementName);
        if (listenersElement == null) return;

        // Find the inner complexType
        final var innerComplexType = findFirstChildElement(listenersElement, "complexType");
        if (innerComplexType == null) return;

        // Replace the content with xs:choice
        // Remove existing children (xs:sequence with xs:element ref="address")
        while (innerComplexType.hasChildNodes()) {
            innerComplexType.removeChild(innerComplexType.getFirstChild());
        }

        // Create xs:choice with all listener types
        final var choice = doc.createElementNS(XS_NAMESPACE, "xs:choice");
        choice.setAttribute("minOccurs", "0");
        choice.setAttribute("maxOccurs", "unbounded");

        for (final var listenerType : listenerTypes) {
            final var elementRef = doc.createElementNS(XS_NAMESPACE, "xs:element");
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
    private static void fixProtocolAdapters(final Document doc) {
        final var complexType = findComplexTypeByName(doc, "hiveMQConfigEntity");
        if (complexType == null) return;

        final var adaptersElement = findChildElementByName(complexType, "protocol-adapters");
        if (adaptersElement == null) return;

        final var innerComplexType = findFirstChildElement(adaptersElement, "complexType");
        if (innerComplexType == null) return;

        // Clear existing content
        while (innerComplexType.hasChildNodes()) {
            innerComplexType.removeChild(innerComplexType.getFirstChild());
        }

        // Create xs:sequence with xs:any (skip validation for all adapter elements)
        // This allows both <protocol-adapter> and legacy adapter-specific elements like <simulation>
        final var sequence = doc.createElementNS(XS_NAMESPACE, "xs:sequence");

        final var choice = doc.createElementNS(XS_NAMESPACE, "xs:choice");
        choice.setAttribute("minOccurs", "0");
        choice.setAttribute("maxOccurs", "unbounded");

        // Use xs:any alone to avoid non-determinism
        final var anyElement = doc.createElementNS(XS_NAMESPACE, "xs:any");
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
    private static void fixModules(final Document doc) {
        final var complexType = findComplexTypeByName(doc, "hiveMQConfigEntity");
        if (complexType == null) return;

        final var modulesElement = findChildElementByName(complexType, "modules");
        if (modulesElement == null) return;

        // Remove the type attribute and add inline complexType with xs:any
        modulesElement.removeAttribute("type");

        // Create inline complexType
        final var innerComplexType = doc.createElementNS(XS_NAMESPACE, "xs:complexType");
        final var sequence = doc.createElementNS(XS_NAMESPACE, "xs:sequence");

        final var anyElement = doc.createElementNS(XS_NAMESPACE, "xs:any");
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
    private static void fixDataCombinerEntity(final Document doc) {
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
    private static void makeElementOptionalInType(final Document doc, final String typeName, final String elementName) {
        final var complexType = findComplexTypeByName(doc, typeName);
        if (complexType == null) return;

        final var element = findChildElementByName(complexType, elementName);
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
    private static void replaceSequenceWithAll(final Document doc, final String typeName) {
        final var complexType = findComplexTypeByName(doc, typeName);
        if (complexType == null) return;

        final var children = complexType.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final var child = children.item(i);
            if (child instanceof Element && "sequence".equals(child.getLocalName())) {
                final var allElement = doc.createElementNS(XS_NAMESPACE, "xs:all");

                // Move all children from sequence to all
                final var sequenceChildren = child.getChildNodes();
                while (sequenceChildren.getLength() > 0) {
                    final var seqChild = sequenceChildren.item(0);
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
    private static void fixEmptyElementTypes(final Document doc) {
        final String[] typesToFix = {
                "mqttSnConfigEntity",
                "adminApiEntity",
                "dynamicConfigEntity",
                "usageTrackingConfigEntity",
                // Base types that are inherited by types above
                "enabledEntity"
        };

        for (final var typeName : typesToFix) {
            makeAllChildrenOptional(doc, typeName);
        }
    }

    /**
     * Makes all child elements optional (minOccurs="0") in the specified complex type.
     *
     * @param doc      the XSD document to modify
     * @param typeName the name of the complex type whose children should be made optional
     */
    private static void makeAllChildrenOptional(final Document doc, final String typeName) {
        final var complexType = findComplexTypeByName(doc, typeName);
        if (complexType == null) return;

        final var elements = complexType.getElementsByTagNameNS(XS_NAMESPACE, "element");
        for (int i = 0; i < elements.getLength(); i++) {
            final var element = (Element) elements.item(i);
            if (!element.hasAttribute("minOccurs")) {
                element.setAttribute("minOccurs", "0");
            }
        }
    }

    /**
     * Fixes expiry config types to make the max-interval element optional.
     * <p>
     * <b>Why this is needed:</b>
     * The session-expiry and message-expiry elements support both simple text format
     * ({@code <session-expiry>123</session-expiry>}) and nested element format
     * ({@code <session-expiry><max-interval>123</max-interval></session-expiry>}).
     * The max-interval element must be optional to support the simple text format.
     * <p>
     * <b>Why JAXB cannot express this directly:</b>
     * The entity uses {@code @XmlMixed} + {@code @XmlAnyElement} for flexible reading
     * and {@code @XmlElement} for structured writing. JAXB generates the max-interval
     * as required by default.
     */
    private static void fixExpiryConfigTypes(final Document doc) {
        makeElementOptionalInType(doc, "sessionExpiryConfigEntity", "max-interval");
        makeElementOptionalInType(doc, "messageExpiryConfigEntity", "max-interval");
    }

    /**
     * Fixes MQTT configuration types to use xs:all instead of xs:sequence.
     * <p>
     * <b>Why this is needed:</b>
     * The old hand-written XSD used {@code xs:all} for MQTT config types, allowing elements
     * in any order. Existing config files rely on this flexibility. The generated XSD uses
     * {@code xs:sequence} which requires strict element ordering.
     * <p>
     * <b>Why JAXB cannot express this directly:</b>
     * See {@link #replaceSequenceWithAllForRootEntity(Document)} for the explanation.
     */
    private static void fixMqttConfigTypes(final Document doc) {
        // MQTT configuration types that need flexible element ordering
        final String[] typesToFix = {
                "keepAliveConfigEntity",
                "packetsConfigEntity",
                "qoSConfigEntity",
                "queuedMessagesConfigEntity",
                "receiveMaximumConfigEntity",
                "retainedMessagesConfigEntity",
                "sharedSubscriptionsConfigEntity",
                "subscriptionIdentifierConfigEntity",
                "topicAliasConfigEntity",
                "wildcardSubscriptionsConfigEntity",
                // Restriction types
                "restrictionsEntity",
                // Bridge types that need flexible element ordering
                "mqttBridgeEntity",
                "remoteBrokerEntity",
                "remoteSubscriptionEntity",
                "localSubscriptionEntity",
                "loopPreventionEntity",
                "bridgeMqttEntity",
                "forwardedTopicEntity",
                // Security config types
                "securityConfigEntity",
                // LDAP authentication types - elements can appear in any order
                "ldapAuthenticationEntity",
                "ldapServerEntity",
                "ldapSimpleBindEntity",
                // Pulse config types - note: these use hyphenated names in XSD
                "managed-asset"
        };

        for (final var typeName : typesToFix) {
            replaceSequenceWithAll(doc, typeName);
        }

        // Make bridge elements optional that have defaults
        makeAllChildrenOptional(doc, "mqttBridgeEntity");
        makeAllChildrenOptional(doc, "remoteBrokerEntity");
        makeAllChildrenOptional(doc, "remoteSubscriptionEntity");
        makeAllChildrenOptional(doc, "localSubscriptionEntity");
        makeAllChildrenOptional(doc, "bridgeMqttEntity");
        makeAllChildrenOptional(doc, "forwardedTopicEntity");

        // Make LDAP elements optional that have defaults
        makeAllChildrenOptional(doc, "ldapAuthenticationEntity");
        makeAllChildrenOptional(doc, "ldapServerEntity");
        makeAllChildrenOptional(doc, "ldapSimpleBindEntity");

        // Fix element references in remoteBrokerEntity to use proper types instead of global refs
        // JAXB generates ref="mqtt" which references the global mqtt element with xs:anyType
        // We need to replace it with an inline element of type bridgeMqttEntity
        replaceElementRefWithTypedElement(doc, "remoteBrokerEntity", "mqtt", "bridgeMqttEntity");

        // API listener entity: bind-address is optional (has default "0.0.0.0" in Java)
        // The Java entity has required=true but the old XSD allowed it to be optional
        makeElementOptionalWithDefault(doc, "apiListenerEntity", "bind-address", "0.0.0.0");
    }

    /**
     * Adds mixed="true" attribute to complex types that use xs:sequence to allow whitespace
     * between elements when JAXB marshals with pretty-printing.
     * <p>
     * <b>Why this is needed:</b>
     * By default, XSD complex types with {@code xs:sequence} have "element-only" content model,
     * which means text nodes (including whitespace) are not allowed between child elements.
     * When JAXB marshals XML with formatting enabled, it adds newlines and indentation as text
     * nodes, which violates the schema constraint.
     * <p>
     * <b>Why JAXB cannot express this directly:</b>
     * There is no JAXB annotation to generate {@code mixed="true"} on complex types.
     * JAXB assumes element-only content for most types. The {@code @XmlMixed} annotation exists
     * but only works with {@code @XmlAnyElement} for capturing arbitrary mixed content, not for
     * simply allowing whitespace in formatted output.
     * <p>
     * Adding {@code mixed="true"} tells the XSD validator to allow text content (whitespace)
     * between child elements, making the schema compatible with pretty-printed XML output.
     * <p>
     * <b>Important:</b> When a type extends another type via {@code xs:complexContent/xs:extension},
     * both the base and derived types must have the same mixed content model. This method handles
     * both direct sequence types and types that extend other types.
     */
    private static void addMixedContentToSequenceTypes(final Document doc) {
        final var complexTypes = doc.getElementsByTagNameNS(XS_NAMESPACE, "complexType");
        for (int i = 0; i < complexTypes.getLength(); i++) {
            final var complexType = (Element) complexTypes.item(i);

            // Check if this complex type has a direct xs:sequence or xs:all child
            if (hasDirectSequenceOrAllChild(complexType)) {
                complexType.setAttribute("mixed", "true");
                continue;
            }

            // Check if this complex type extends another type (xs:complexContent/xs:extension)
            // If so, it must also be mixed to match the base type
            if (hasComplexContentExtension(complexType)) {
                complexType.setAttribute("mixed", "true");
            }
        }
    }

    /**
     * Checks if a complex type has a direct xs:sequence or xs:all child element.
     * Both need mixed="true" to allow whitespace (formatting) between child elements.
     */
    private static boolean hasDirectSequenceOrAllChild(final Element complexType) {
        final var children = complexType.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final var child = children.item(i);
            if (child instanceof Element) {
                final String localName = child.getLocalName();
                if ("sequence".equals(localName) || "all".equals(localName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if a complex type uses xs:complexContent with xs:extension (type inheritance).
     */
    private static boolean hasComplexContentExtension(final Element complexType) {
        final var children = complexType.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final var child = children.item(i);
            if (child instanceof Element && "complexContent".equals(child.getLocalName())) {
                // Check for xs:extension child
                final var complexContentChildren = child.getChildNodes();
                for (int j = 0; j < complexContentChildren.getLength(); j++) {
                    final var ccChild = complexContentChildren.item(j);
                    if (ccChild instanceof Element && "extension".equals(ccChild.getLocalName())) {
                        return true;
                    }
                }
            }
        }
        return false;
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
    private static void addCustomSimpleTypes(final Document doc) {
        final var schemaElement = doc.getDocumentElement();

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

        // MQTT configuration value constraints
        // max-packet-size: 15 to 268435460 (MQTT spec)
        addSimpleTypeIfNotExists(doc, schemaElement, "maxPacketSizeType", "xs:int",
                new String[]{"minInclusive", "15"}, new String[]{"maxInclusive", "268435460"});

        // server-receive-maximum and max-per-client: 1 to 65535 (MQTT spec)
        addSimpleTypeIfNotExists(doc, schemaElement, "uint16NonZeroType", "xs:int",
                new String[]{"minInclusive", "1"}, new String[]{"maxInclusive", "65535"});

        // max-keep-alive: 1 to 65535 (MQTT spec)
        addSimpleTypeIfNotExists(doc, schemaElement, "keepAliveType", "xs:int",
                new String[]{"minInclusive", "1"}, new String[]{"maxInclusive", "65535"});

        // session-expiry max-interval: 0 to 4294967295 (MQTT spec - uint32)
        addSimpleTypeIfNotExists(doc, schemaElement, "sessionExpiryIntervalType", "xs:long",
                new String[]{"minInclusive", "0"}, new String[]{"maxInclusive", "4294967295"});

        // message-expiry max-interval: 0 to 4294967296 (allows disabling with max+1)
        addSimpleTypeIfNotExists(doc, schemaElement, "messageExpiryIntervalType", "xs:long",
                new String[]{"minInclusive", "0"}, new String[]{"maxInclusive", "4294967296"});

        // max-queue-size: 1 or more
        addSimpleTypeIfNotExists(doc, schemaElement, "maxQueueSizeType", "xs:long",
                new String[]{"minInclusive", "1"});

        // Restriction constraints
        // max-connections: -1 (unlimited) or positive
        addSimpleTypeIfNotExists(doc, schemaElement, "maxConnectionsType", "xs:long",
                new String[]{"minInclusive", "-1"});

        // max-client-id-length, max-topic-length: 1 to 65535
        addSimpleTypeIfNotExists(doc, schemaElement, "maxLengthType", "xs:int",
                new String[]{"minInclusive", "1"}, new String[]{"maxInclusive", "65535"});

        // no-connect-idle-timeout: 0 or more
        addSimpleTypeIfNotExists(doc, schemaElement, "timeoutMsType", "xs:int",
                new String[]{"minInclusive", "0"});

        // incoming-bandwidth-throttling: 0 or more
        addSimpleTypeIfNotExists(doc, schemaElement, "bandwidthType", "xs:long",
                new String[]{"minInclusive", "0"});

        // Apply the custom types to the relevant elements
        applyValueConstraints(doc);
    }

    /**
     * Applies value constraint types to specific elements in the XSD.
     * <p>
     * <b>Why this is needed:</b>
     * JAXB generates elements with basic types (xs:int, xs:long) without value constraints.
     * The old hand-written XSD had inline simpleType restrictions for many elements to enforce
     * valid value ranges. This method replaces the basic types with custom constrained types.
     */
    private static void applyValueConstraints(final Document doc) {
        // packetsConfigEntity: max-packet-size
        changeElementTypeInComplexType(doc, "packetsConfigEntity", "max-packet-size", "maxPacketSizeType");

        // receiveMaximumConfigEntity: server-receive-maximum
        changeElementTypeInComplexType(doc, "receiveMaximumConfigEntity", "server-receive-maximum", "uint16NonZeroType");

        // topicAliasConfigEntity: max-per-client
        changeElementTypeInComplexType(doc, "topicAliasConfigEntity", "max-per-client", "uint16NonZeroType");

        // keepAliveConfigEntity: max-keep-alive
        changeElementTypeInComplexType(doc, "keepAliveConfigEntity", "max-keep-alive", "keepAliveType");

        // queuedMessagesConfigEntity: max-queue-size
        changeElementTypeInComplexType(doc, "queuedMessagesConfigEntity", "max-queue-size", "maxQueueSizeType");

        // sessionExpiryConfigEntity: max-interval
        changeElementTypeInComplexType(doc, "sessionExpiryConfigEntity", "max-interval", "sessionExpiryIntervalType");

        // messageExpiryConfigEntity: max-interval
        changeElementTypeInComplexType(doc, "messageExpiryConfigEntity", "max-interval", "messageExpiryIntervalType");

        // restrictionsEntity: various constraints
        changeElementTypeInComplexType(doc, "restrictionsEntity", "max-connections", "maxConnectionsType");
        changeElementTypeInComplexType(doc, "restrictionsEntity", "max-client-id-length", "maxLengthType");
        changeElementTypeInComplexType(doc, "restrictionsEntity", "max-topic-length", "maxLengthType");
        changeElementTypeInComplexType(doc, "restrictionsEntity", "no-connect-idle-timeout", "timeoutMsType");
        changeElementTypeInComplexType(doc, "restrictionsEntity", "incoming-bandwidth-throttling", "bandwidthType");

        // listenerEntity: name must be non-empty (whitespace collapses to empty string)
        changeElementTypeInComplexType(doc, "listenerEntity", "name", "nonEmptyString");

        // Pulse managed-asset: id attribute must be UUID
        changeAttributeTypeInComplexType(doc, "managed-asset", "id", "uuidType");

        // Pulse mapping: id attribute must be UUID
        changeAttributeTypeInComplexType(doc, "mapping", "id", "uuidType");
    }

    /**
     * Changes the type attribute of a specific element within a complex type.
     */
    private static void changeElementTypeInComplexType(
            final Document doc,
            final String complexTypeName,
            final String elementName,
            final String newType) {
        final var complexType = findComplexTypeByName(doc, complexTypeName);
        if (complexType == null) {
            return;
        }

        final var elements = complexType.getElementsByTagNameNS(XS_NAMESPACE, "element");
        for (int i = 0; i < elements.getLength(); i++) {
            final var element = (Element) elements.item(i);
            if (elementName.equals(element.getAttribute("name"))) {
                element.setAttribute("type", newType);
                break;
            }
        }
    }

    /**
     * Changes the type attribute of a specific attribute within a complex type.
     */
    private static void changeAttributeTypeInComplexType(
            final Document doc,
            final String complexTypeName,
            final String attributeName,
            final String newType) {
        final var complexType = findComplexTypeByName(doc, complexTypeName);
        if (complexType == null) {
            return;
        }

        final var attributes = complexType.getElementsByTagNameNS(XS_NAMESPACE, "attribute");
        for (int i = 0; i < attributes.getLength(); i++) {
            final var attr = (Element) attributes.item(i);
            if (attributeName.equals(attr.getAttribute("name"))) {
                attr.setAttribute("type", newType);
                break;
            }
        }
    }

    /**
     * Makes a specific element optional (minOccurs="0") with a default value in a complex type.
     */
    private static void makeElementOptionalWithDefault(
            final Document doc,
            final String complexTypeName,
            final String elementName,
            final String defaultValue) {
        final var complexType = findComplexTypeByName(doc, complexTypeName);
        if (complexType == null) {
            return;
        }

        final var elements = complexType.getElementsByTagNameNS(XS_NAMESPACE, "element");
        for (int i = 0; i < elements.getLength(); i++) {
            final var element = (Element) elements.item(i);
            if (elementName.equals(element.getAttribute("name"))) {
                element.setAttribute("minOccurs", "0");
                element.setAttribute("default", defaultValue);
                break;
            }
        }
    }

    private static boolean simpleTypeExists(final Document doc, final String name) {
        final var simpleTypes = doc.getElementsByTagNameNS(XS_NAMESPACE, "simpleType");
        for (int i = 0; i < simpleTypes.getLength(); i++) {
            final var st = (Element) simpleTypes.item(i);
            if (name.equals(st.getAttribute("name"))) {
                return true;
            }
        }
        return false;
    }

    private static void addSimpleTypeIfNotExists(final Document doc, final Element parent, final String name, final String baseType, final String[]... facets) {
        if (simpleTypeExists(doc, name)) {
            return;
        }
        addSimpleType(doc, parent, name, baseType, facets);
    }

    private static void addSimpleTypeWithEnumerationIfNotExists(final Document doc, final Element parent, final String name, final String baseType, final String... values) {
        if (simpleTypeExists(doc, name)) {
            return;
        }
        addSimpleTypeWithEnumeration(doc, parent, name, baseType, values);
    }

    private static void addSimpleType(final Document doc, final Element parent, final String name, final String baseType, final String[]... facets) {
        final var simpleType = doc.createElementNS(XS_NAMESPACE, "xs:simpleType");
        simpleType.setAttribute("name", name);

        final var restriction = doc.createElementNS(XS_NAMESPACE, "xs:restriction");
        restriction.setAttribute("base", baseType);

        for (final String[] facet : facets) {
            final var facetElement = doc.createElementNS(XS_NAMESPACE, "xs:" + facet[0]);
            facetElement.setAttribute("value", facet[1]);
            restriction.appendChild(facetElement);
        }

        simpleType.appendChild(restriction);
        parent.appendChild(simpleType);
    }

    private static void addSimpleTypeWithEnumeration(final Document doc, final Element parent, final String name, final String baseType, final String... values) {
        final var simpleType = doc.createElementNS(XS_NAMESPACE, "xs:simpleType");
        simpleType.setAttribute("name", name);

        final var restriction = doc.createElementNS(XS_NAMESPACE, "xs:restriction");
        restriction.setAttribute("base", baseType);

        for (final String value : values) {
            final var enumElement = doc.createElementNS(XS_NAMESPACE, "xs:enumeration");
            enumElement.setAttribute("value", value);
            restriction.appendChild(enumElement);
        }

        simpleType.appendChild(restriction);
        parent.appendChild(simpleType);
    }

    // Helper methods for DOM traversal

    private static Element findComplexTypeByName(final Document doc, final String name) {
        final var complexTypes = doc.getElementsByTagNameNS(XS_NAMESPACE, "complexType");
        for (int i = 0; i < complexTypes.getLength(); i++) {
            final var ct = (Element) complexTypes.item(i);
            if (name.equals(ct.getAttribute("name"))) {
                return ct;
            }
        }
        return null;
    }

    private static Element findChildElementByName(final Element parent, final String elementName) {
        // Search through xs:all or xs:sequence children
        final var children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final var child = children.item(i);
            if (child instanceof final Element childElement) {
                final var localName = childElement.getLocalName();
                if ("all".equals(localName) || "sequence".equals(localName)) {
                    // Search within all/sequence
                    final var found = findElementWithNameAttribute(childElement, elementName);
                    if (found != null) return found;
                }
            }
        }
        return null;
    }

    private static Element findElementWithNameAttribute(final Element parent, final String nameValue) {
        final var children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final var child = children.item(i);
            if (child instanceof final Element childElement) {
                if ("element".equals(childElement.getLocalName())) {
                    // Check both "name" attribute and "ref" attribute
                    final var name = childElement.getAttribute("name");
                    final var ref = childElement.getAttribute("ref");
                    if (nameValue.equals(name) || nameValue.equals(ref)) {
                        return childElement;
                    }
                }
            }
        }
        return null;
    }

    private static Element findFirstChildElement(final Element parent, final String localName) {
        final var children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final var child = children.item(i);
            if (child instanceof Element && localName.equals(child.getLocalName())) {
                return (Element) child;
            }
        }
        return null;
    }

    /**
     * Replaces an element reference (ref="elementName") with an inline typed element in a complex type.
     * <p>
     * <b>Why this is needed:</b>
     * JAXB generates element references (ref="...") for {@code @XmlElementRef} annotated fields.
     * This references the global element declaration which may have an inappropriate type
     * (e.g., xs:anyType). For proper validation, we need inline elements with specific types.
     *
     * @param doc              the XSD document to modify
     * @param complexTypeName  the name of the complex type containing the element reference
     * @param elementName      the element name to replace (the ref value)
     * @param newTypeName      the type to use for the new inline element
     */
    private static void replaceElementRefWithTypedElement(
            final Document doc,
            final String complexTypeName,
            final String elementName,
            final String newTypeName) {
        final var complexType = findComplexTypeByName(doc, complexTypeName);
        if (complexType == null) return;

        // Find the element with ref="elementName" in the complex type
        final var refElement = findChildElementByName(complexType, elementName);
        if (refElement == null || !refElement.hasAttribute("ref")) {
            return;
        }

        // Preserve the minOccurs attribute if it exists
        final String minOccurs = refElement.getAttribute("minOccurs");

        // Remove ref attribute and set name and type instead
        refElement.removeAttribute("ref");
        refElement.setAttribute("name", elementName);
        refElement.setAttribute("type", newTypeName);

        // Restore minOccurs if it was present
        if (!minOccurs.isEmpty()) {
            refElement.setAttribute("minOccurs", minOccurs);
        }
    }
}
