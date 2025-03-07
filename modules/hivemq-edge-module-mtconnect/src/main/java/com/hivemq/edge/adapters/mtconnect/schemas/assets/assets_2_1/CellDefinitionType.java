//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_2_1;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * semantic definition of a {{block(Cell)}}.
 * 
 * <p>Java class for CellDefinitionType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="CellDefinitionType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <all>
 *         <element name="Description" type="{urn:mtconnect.org:MTConnectAssets:2.1}DataItemDescriptionType" minOccurs="0"/>
 *       </all>
 *       <attGroup ref="{urn:mtconnect.org:MTConnectAssets:2.1}DefinitionAttrsType"/>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "CellDefinitionType")
@XmlType(name = "CellDefinitionType", propOrder = {

})
public class CellDefinitionType {

    /**
     * See {{sect(Description)}}.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Description")
    @XmlElement(name = "Description")
    protected DataItemDescriptionType description;
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
