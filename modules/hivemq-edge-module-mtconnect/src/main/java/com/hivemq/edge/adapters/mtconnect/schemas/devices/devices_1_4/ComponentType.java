//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_4;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * An abstract component type. This is a placeholder for all components
 * 
 * <p>Java class for ComponentType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="ComponentType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="Description" type="{urn:mtconnect.org:MTConnectDevices:1.4}ComponentDescriptionType" minOccurs="0"/>
 *         <element name="Configuration" type="{urn:mtconnect.org:MTConnectDevices:1.4}ComponentConfigurationType" minOccurs="0"/>
 *         <element name="DataItems" type="{urn:mtconnect.org:MTConnectDevices:1.4}DataItemsType" minOccurs="0"/>
 *         <element name="Components" type="{urn:mtconnect.org:MTConnectDevices:1.4}ComponentsType" minOccurs="0"/>
 *         <element name="Compositions" type="{urn:mtconnect.org:MTConnectDevices:1.4}CompositionsType" minOccurs="0"/>
 *         <element name="References" type="{urn:mtconnect.org:MTConnectDevices:1.4}ReferencesType" minOccurs="0"/>
 *       </sequence>
 *       <attribute name="id" use="required" type="{urn:mtconnect.org:MTConnectDevices:1.4}IDType" />
 *       <attribute name="nativeName" type="{urn:mtconnect.org:MTConnectDevices:1.4}NameType" />
 *       <attribute name="sampleInterval" type="{urn:mtconnect.org:MTConnectDevices:1.4}SampleIntervalType" />
 *       <attribute name="sampleRate" type="{urn:mtconnect.org:MTConnectDevices:1.4}DataItemSampleRateType" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "ComponentType")
@XmlType(name = "ComponentType", propOrder = {
    "description",
    "configuration",
    "dataItems",
    "components",
    "compositions",
    "references"
})
@XmlSeeAlso({
    DeviceType.class,
    CommonComponentType.class
})
public abstract class ComponentType {

    /**
     * The descriptive information about this component
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Description")
    @XmlElement(name = "Description")
    protected ComponentDescriptionType description;
    /**
     * The configuration information about this component
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Configuration")
    @XmlElement(name = "Configuration")
    protected ComponentConfigurationType configuration;
    /**
     * The component's Data Items
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "DataItems")
    @XmlElement(name = "DataItems")
    protected DataItemsType dataItems;
    /**
     * The sub components
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Components")
    @XmlElement(name = "Components")
    protected ComponentsType components;
    /**
     * A list of composition elements
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Compositions")
    @XmlElement(name = "Compositions")
    protected CompositionsType compositions;
    /**
     * A list of references
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "References")
    @XmlElement(name = "References")
    protected ReferencesType references;
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id")
    @XmlAttribute(name = "id", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    protected String id;
    @com.fasterxml.jackson.annotation.JsonProperty(value = "nativeName")
    @XmlAttribute(name = "nativeName")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String nativeName;
    @com.fasterxml.jackson.annotation.JsonProperty(value = "sampleInterval")
    @XmlAttribute(name = "sampleInterval")
    protected Float sampleInterval;
    @com.fasterxml.jackson.annotation.JsonProperty(value = "sampleRate")
    @XmlAttribute(name = "sampleRate")
    protected Float sampleRate;

    /**
     * The descriptive information about this component
     * 
     * @return
     *     possible object is
     *     {@link ComponentDescriptionType }
     *     
     */
    public ComponentDescriptionType getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link ComponentDescriptionType }
     *     
     * @see #getDescription()
     */
    public void setDescription(ComponentDescriptionType value) {
        this.description = value;
    }

    /**
     * The configuration information about this component
     * 
     * @return
     *     possible object is
     *     {@link ComponentConfigurationType }
     *     
     */
    public ComponentConfigurationType getConfiguration() {
        return configuration;
    }

    /**
     * Sets the value of the configuration property.
     * 
     * @param value
     *     allowed object is
     *     {@link ComponentConfigurationType }
     *     
     * @see #getConfiguration()
     */
    public void setConfiguration(ComponentConfigurationType value) {
        this.configuration = value;
    }

    /**
     * The component's Data Items
     * 
     * @return
     *     possible object is
     *     {@link DataItemsType }
     *     
     */
    public DataItemsType getDataItems() {
        return dataItems;
    }

    /**
     * Sets the value of the dataItems property.
     * 
     * @param value
     *     allowed object is
     *     {@link DataItemsType }
     *     
     * @see #getDataItems()
     */
    public void setDataItems(DataItemsType value) {
        this.dataItems = value;
    }

    /**
     * The sub components
     * 
     * @return
     *     possible object is
     *     {@link ComponentsType }
     *     
     */
    public ComponentsType getComponents() {
        return components;
    }

    /**
     * Sets the value of the components property.
     * 
     * @param value
     *     allowed object is
     *     {@link ComponentsType }
     *     
     * @see #getComponents()
     */
    public void setComponents(ComponentsType value) {
        this.components = value;
    }

    /**
     * A list of composition elements
     * 
     * @return
     *     possible object is
     *     {@link CompositionsType }
     *     
     */
    public CompositionsType getCompositions() {
        return compositions;
    }

    /**
     * Sets the value of the compositions property.
     * 
     * @param value
     *     allowed object is
     *     {@link CompositionsType }
     *     
     * @see #getCompositions()
     */
    public void setCompositions(CompositionsType value) {
        this.compositions = value;
    }

    /**
     * A list of references
     * 
     * @return
     *     possible object is
     *     {@link ReferencesType }
     *     
     */
    public ReferencesType getReferences() {
        return references;
    }

    /**
     * Sets the value of the references property.
     * 
     * @param value
     *     allowed object is
     *     {@link ReferencesType }
     *     
     * @see #getReferences()
     */
    public void setReferences(ReferencesType value) {
        this.references = value;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
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
     * Gets the value of the sampleInterval property.
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public Float getSampleInterval() {
        return sampleInterval;
    }

    /**
     * Sets the value of the sampleInterval property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     */
    public void setSampleInterval(Float value) {
        this.sampleInterval = value;
    }

    /**
     * Gets the value of the sampleRate property.
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public Float getSampleRate() {
        return sampleRate;
    }

    /**
     * Sets the value of the sampleRate property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     */
    public void setSampleRate(Float value) {
        this.sampleRate = value;
    }

}
