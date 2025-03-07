//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_2_0;

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
 * Description
 * 
 * <p>Java class for ComponentType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="ComponentType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <all>
 *         <element name="Description" type="{urn:mtconnect.org:MTConnectDevices:2.0}ComponentDescriptionType" minOccurs="0"/>
 *         <element name="Configuration" type="{urn:mtconnect.org:MTConnectDevices:2.0}ComponentConfigurationType" minOccurs="0"/>
 *         <element name="DataItems" type="{urn:mtconnect.org:MTConnectDevices:2.0}DataItemsType" minOccurs="0"/>
 *         <element name="Components" type="{urn:mtconnect.org:MTConnectDevices:2.0}ComponentsType" minOccurs="0"/>
 *         <element name="Compositions" type="{urn:mtconnect.org:MTConnectDevices:2.0}CompositionsType" minOccurs="0"/>
 *         <element name="References" type="{urn:mtconnect.org:MTConnectDevices:2.0}ReferencesType" minOccurs="0"/>
 *       </all>
 *       <attribute name="id" use="required" type="{urn:mtconnect.org:MTConnectDevices:2.0}IDType" />
 *       <attribute name="nativeName" type="{urn:mtconnect.org:MTConnectDevices:2.0}NameType" />
 *       <attribute name="sampleInterval" type="{urn:mtconnect.org:MTConnectDevices:2.0}SampleIntervalType" />
 *       <attribute name="sampleRate" type="{urn:mtconnect.org:MTConnectDevices:2.0}DataItemSampleRateType" />
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

})
@XmlSeeAlso({
    DeviceType.class,
    CommonComponentType.class
})
public abstract class ComponentType {

    /**
     * See {{sect(Description)}}.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Description")
    @XmlElement(name = "Description")
    protected ComponentDescriptionType description;
    /**
     * technical information about an entity describing its physical
     *             layout, functional characteristics, and relationships with other
     *             entities.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Configuration")
    @XmlElement(name = "Configuration")
    protected ComponentConfigurationType configuration;
    @com.fasterxml.jackson.annotation.JsonProperty(value = "DataItems")
    @XmlElement(name = "DataItems")
    protected DataItemsType dataItems;
    /**
     * {{block(Components)}} groups one or more {{block(Component)}}
     *             entities.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Components")
    @XmlElement(name = "Components")
    protected ComponentsType components;
    /**
     * Compositions Model
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Compositions")
    @XmlElement(name = "Compositions")
    protected CompositionsType compositions;
    /**
     * References Model
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "References")
    @XmlElement(name = "References")
    protected ReferencesType references;
    /**
     * unique identifier for this element.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id")
    @XmlAttribute(name = "id", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    protected String id;
    /**
     * The device manufacturer component name
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "nativeName")
    @XmlAttribute(name = "nativeName")
    protected String nativeName;
    /**
     * The rate at which the data is sampled from the component
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "sampleInterval")
    @XmlAttribute(name = "sampleInterval")
    protected Float sampleInterval;
    /**
     * DEPRECATED: The rate at which the data is sampled from the component
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "sampleRate")
    @XmlAttribute(name = "sampleRate")
    protected Float sampleRate;

    /**
     * See {{sect(Description)}}.
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
     * technical information about an entity describing its physical
     *             layout, functional characteristics, and relationships with other
     *             entities.
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
     * Gets the value of the dataItems property.
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
     */
    public void setDataItems(DataItemsType value) {
        this.dataItems = value;
    }

    /**
     * {{block(Components)}} groups one or more {{block(Component)}}
     *             entities.
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
     * Compositions Model
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
     * References Model
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
     * unique identifier for this element.
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
     * @see #getId()
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * The device manufacturer component name
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
     * @see #getNativeName()
     */
    public void setNativeName(String value) {
        this.nativeName = value;
    }

    /**
     * The rate at which the data is sampled from the component
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
     * @see #getSampleInterval()
     */
    public void setSampleInterval(Float value) {
        this.sampleInterval = value;
    }

    /**
     * DEPRECATED: The rate at which the data is sampled from the component
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
     * @see #getSampleRate()
     */
    public void setSampleRate(Float value) {
        this.sampleRate = value;
    }

}
