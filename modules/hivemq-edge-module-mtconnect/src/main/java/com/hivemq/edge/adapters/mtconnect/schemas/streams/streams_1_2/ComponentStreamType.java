//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_2;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * The stream of data for a component
 * 
 * <p>Java class for ComponentStreamType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="ComponentStreamType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="Samples" type="{urn:mtconnect.org:MTConnectStreams:1.2}SamplesType" minOccurs="0"/>
 *         <element name="Events" type="{urn:mtconnect.org:MTConnectStreams:1.2}EventsType" minOccurs="0"/>
 *         <element name="Condition" type="{urn:mtconnect.org:MTConnectStreams:1.2}ConditionListType" minOccurs="0"/>
 *       </sequence>
 *       <attribute name="componentId" use="required" type="{urn:mtconnect.org:MTConnectStreams:1.2}ComponentIdType" />
 *       <attribute name="name" use="required" type="{urn:mtconnect.org:MTConnectStreams:1.2}NameType" />
 *       <attribute name="nativeName" type="{urn:mtconnect.org:MTConnectStreams:1.2}NameType" />
 *       <attribute name="component" use="required" type="{urn:mtconnect.org:MTConnectStreams:1.2}NameType" />
 *       <attribute name="uuid" type="{urn:mtconnect.org:MTConnectStreams:1.2}UuidType" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "ComponentStreamType")
@XmlType(name = "ComponentStreamType", propOrder = {
    "samples",
    "events",
    "condition"
})
public class ComponentStreamType {

    /**
     * A collection of samples
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Samples")
    @XmlElement(name = "Samples")
    protected SamplesType samples;
    /**
     * A collection of events
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Events")
    @XmlElement(name = "Events")
    protected EventsType events;
    /**
     * The representation of the devices condition
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Condition")
    @XmlElement(name = "Condition")
    protected ConditionListType condition;
    @com.fasterxml.jackson.annotation.JsonProperty(value = "componentId")
    @XmlAttribute(name = "componentId", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    protected String componentId;
    @com.fasterxml.jackson.annotation.JsonProperty(value = "name")
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @com.fasterxml.jackson.annotation.JsonProperty(value = "nativeName")
    @XmlAttribute(name = "nativeName")
    protected String nativeName;
    @com.fasterxml.jackson.annotation.JsonProperty(value = "component")
    @XmlAttribute(name = "component", required = true)
    protected String component;
    @com.fasterxml.jackson.annotation.JsonProperty(value = "uuid")
    @XmlAttribute(name = "uuid")
    protected String uuid;

    /**
     * A collection of samples
     * 
     * @return
     *     possible object is
     *     {@link SamplesType }
     *     
     */
    public SamplesType getSamples() {
        return samples;
    }

    /**
     * Sets the value of the samples property.
     * 
     * @param value
     *     allowed object is
     *     {@link SamplesType }
     *     
     * @see #getSamples()
     */
    public void setSamples(SamplesType value) {
        this.samples = value;
    }

    /**
     * A collection of events
     * 
     * @return
     *     possible object is
     *     {@link EventsType }
     *     
     */
    public EventsType getEvents() {
        return events;
    }

    /**
     * Sets the value of the events property.
     * 
     * @param value
     *     allowed object is
     *     {@link EventsType }
     *     
     * @see #getEvents()
     */
    public void setEvents(EventsType value) {
        this.events = value;
    }

    /**
     * The representation of the devices condition
     * 
     * @return
     *     possible object is
     *     {@link ConditionListType }
     *     
     */
    public ConditionListType getCondition() {
        return condition;
    }

    /**
     * Sets the value of the condition property.
     * 
     * @param value
     *     allowed object is
     *     {@link ConditionListType }
     *     
     * @see #getCondition()
     */
    public void setCondition(ConditionListType value) {
        this.condition = value;
    }

    /**
     * Gets the value of the componentId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getComponentId() {
        return componentId;
    }

    /**
     * Sets the value of the componentId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setComponentId(String value) {
        this.componentId = value;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the nativeName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNativeName() {
        return nativeName;
    }

    /**
     * Sets the value of the nativeName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNativeName(String value) {
        this.nativeName = value;
    }

    /**
     * Gets the value of the component property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getComponent() {
        return component;
    }

    /**
     * Sets the value of the component property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setComponent(String value) {
        this.component = value;
    }

    /**
     * Gets the value of the uuid property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * Sets the value of the uuid property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUuid(String value) {
        this.uuid = value;
    }

}
