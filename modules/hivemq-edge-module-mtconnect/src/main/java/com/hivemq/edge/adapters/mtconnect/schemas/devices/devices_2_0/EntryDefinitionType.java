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
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * semantic definition of an {{block(Entry)}}.
 * 
 * <p>Java class for EntryDefinitionType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="EntryDefinitionType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <all>
 *         <element name="Description" type="{urn:mtconnect.org:MTConnectDevices:2.0}DataItemDescriptionType" minOccurs="0"/>
 *         <element name="CellDefinitions" type="{urn:mtconnect.org:MTConnectDevices:2.0}CellDefinitionsType" minOccurs="0"/>
 *       </all>
 *       <attGroup ref="{urn:mtconnect.org:MTConnectDevices:2.0}DefinitionAttrsType"/>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "EntryDefinitionType")
@XmlType(name = "EntryDefinitionType", propOrder = {

})
public class EntryDefinitionType {

    /**
     * See {{sect(Description)}}.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Description")
    @XmlElement(name = "Description")
    protected DataItemDescriptionType description;
    /**
     * {{block(CellDefinitions)}} groups one or more
     *             {{block(CellDefinition)}} entities. See {{sect(CellDefinition)}}.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "CellDefinitions")
    @XmlElement(name = "CellDefinitions")
    protected CellDefinitionsType cellDefinitions;
    /**
     * unique key
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "key")
    @XmlAttribute(name = "key")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String key;
    /**
     * The type of measurement
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "type")
    @XmlAttribute(name = "type")
    protected String type;
    /**
     * The type of measurement
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "keyType")
    @XmlAttribute(name = "keyType")
    protected String keyType;
    /**
     * The sub type for the measurement
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "subType")
    @XmlAttribute(name = "subType")
    protected String subType;
    /**
     * The units of the measurement
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "units")
    @XmlAttribute(name = "units")
    protected String units;

    /**
     * See {{sect(Description)}}.
     * 
     * @return
     *     possible object is
     *     {@link DataItemDescriptionType }
     *     
     */
    public DataItemDescriptionType getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link DataItemDescriptionType }
     *     
     * @see #getDescription()
     */
    public void setDescription(DataItemDescriptionType value) {
        this.description = value;
    }

    /**
     * {{block(CellDefinitions)}} groups one or more
     *             {{block(CellDefinition)}} entities. See {{sect(CellDefinition)}}.
     * 
     * @return
     *     possible object is
     *     {@link CellDefinitionsType }
     *     
     */
    public CellDefinitionsType getCellDefinitions() {
        return cellDefinitions;
    }

    /**
     * Sets the value of the cellDefinitions property.
     * 
     * @param value
     *     allowed object is
     *     {@link CellDefinitionsType }
     *     
     * @see #getCellDefinitions()
     */
    public void setCellDefinitions(CellDefinitionsType value) {
        this.cellDefinitions = value;
    }

    /**
     * unique key
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the value of the key property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getKey()
     */
    public void setKey(String value) {
        this.key = value;
    }

    /**
     * The type of measurement
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getType()
     */
    public void setType(String value) {
        this.type = value;
    }

    /**
     * The type of measurement
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getKeyType() {
        return keyType;
    }

    /**
     * Sets the value of the keyType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getKeyType()
     */
    public void setKeyType(String value) {
        this.keyType = value;
    }

    /**
     * The sub type for the measurement
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSubType() {
        return subType;
    }

    /**
     * Sets the value of the subType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getSubType()
     */
    public void setSubType(String value) {
        this.subType = value;
    }

    /**
     * The units of the measurement
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUnits() {
        return units;
    }

    /**
     * Sets the value of the units property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getUnits()
     */
    public void setUnits(String value) {
        this.units = value;
    }

}
