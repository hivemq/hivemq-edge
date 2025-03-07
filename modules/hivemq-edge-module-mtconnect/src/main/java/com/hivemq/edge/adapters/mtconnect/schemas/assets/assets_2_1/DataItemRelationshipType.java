//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_2_1;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;


/**
 * {{block(AbstractDataItemRelationship)}} that provides a semantic
 *         reference to another {{block(DataItem)}} described by the
 *         {{property(type)}} property.
 * 
 * <p>Java class for DataItemRelationshipType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="DataItemRelationshipType">
 *   <complexContent>
 *     <extension base="{urn:mtconnect.org:MTConnectAssets:2.1}AbstractDataItemRelationshipType">
 *       <attribute name="type" use="required" type="{urn:mtconnect.org:MTConnectAssets:2.1}DataItemRelationshipTypeEnumType" />
 *     </extension>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "DataItemRelationshipType")
@XmlType(name = "DataItemRelationshipType")
public class DataItemRelationshipType
    extends AbstractDataItemRelationshipType
{

    /**
     * defines the authority that this piece of equipment has relative to
     *               the associated piece of equipment.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "type")
    @XmlAttribute(name = "type", required = true)
    protected DataItemRelationshipTypeEnumType type;

    /**
     * defines the authority that this piece of equipment has relative to
     *               the associated piece of equipment.
     * 
     * @return
     *     possible object is
     *     {@link DataItemRelationshipTypeEnumType }
     *     
     */
    public DataItemRelationshipTypeEnumType getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link DataItemRelationshipTypeEnumType }
     *     
     * @see #getType()
     */
    public void setType(DataItemRelationshipTypeEnumType value) {
        this.type = value;
    }

}
