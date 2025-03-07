//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_2_4;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;


/**
 * The filter for the data item
 * 
 * <p>Java class for DataItemFilterType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="DataItemFilterType">
 *   <simpleContent>
 *     <extension base="<urn:mtconnect.org:MTConnectAssets:2.4>FilterValueType">
 *       <attribute name="type" use="required" type="{urn:mtconnect.org:MTConnectAssets:2.4}DataItemFilterEnumType" />
 *     </extension>
 *   </simpleContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "DataItemFilterType")
@XmlType(name = "DataItemFilterType", propOrder = {
    "value"
})
public class DataItemFilterType {

    /**
     * The minimum limit on the change in a value
     * 
     */
    @XmlValue
    protected float value;
    /**
     * The type of filter, ABSOLUTE or PERCENT
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "type")
    @XmlAttribute(name = "type", required = true)
    protected DataItemFilterEnumType type;

    /**
     * The minimum limit on the change in a value
     * 
     */
    public float getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     */
    public void setValue(float value) {
        this.value = value;
    }

    /**
     * The type of filter, ABSOLUTE or PERCENT
     * 
     * @return
     *     possible object is
     *     {@link DataItemFilterEnumType }
     *     
     */
    public DataItemFilterEnumType getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link DataItemFilterEnumType }
     *     
     * @see #getType()
     */
    public void setType(DataItemFilterEnumType value) {
        this.type = value;
    }

}
