//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_2_4;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * A relationship between this component and something else
 * 
 * <p>Java class for RelationshipType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="RelationshipType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <attribute name="id" use="required" type="{urn:mtconnect.org:MTConnectDevices:2.4}IDType" />
 *       <attribute name="name" type="{urn:mtconnect.org:MTConnectDevices:2.4}NameType" />
 *       <attribute name="type" use="required" type="{urn:mtconnect.org:MTConnectDevices:2.4}RelationshipTypeEnumType" />
 *       <attribute name="criticality" type="{urn:mtconnect.org:MTConnectDevices:2.4}CriticalityEnumType" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "RelationshipType")
@XmlType(name = "RelationshipType")
@XmlSeeAlso({
    ComponentRelationshipType.class,
    DeviceRelationshipType.class
})
public abstract class RelationshipType {

    /**
     * The relationship identifier
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id")
    @XmlAttribute(name = "id", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    protected String id;
    /**
     * identifier of the maintenance activity.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "name")
    @XmlAttribute(name = "name")
    protected String name;
    /**
     * The assciation type
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "type")
    @XmlAttribute(name = "type", required = true)
    protected RelationshipTypeEnumType type;
    /**
     * Criticality
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "criticality")
    @XmlAttribute(name = "criticality")
    protected CriticalityEnumType criticality;

    /**
     * The relationship identifier
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
     * identifier of the maintenance activity.
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
     * @see #getName()
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * The assciation type
     * 
     * @return
     *     possible object is
     *     {@link RelationshipTypeEnumType }
     *     
     */
    public RelationshipTypeEnumType getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link RelationshipTypeEnumType }
     *     
     * @see #getType()
     */
    public void setType(RelationshipTypeEnumType value) {
        this.type = value;
    }

    /**
     * Criticality
     * 
     * @return
     *     possible object is
     *     {@link CriticalityEnumType }
     *     
     */
    public CriticalityEnumType getCriticality() {
        return criticality;
    }

    /**
     * Sets the value of the criticality property.
     * 
     * @param value
     *     allowed object is
     *     {@link CriticalityEnumType }
     *     
     * @see #getCriticality()
     */
    public void setCriticality(CriticalityEnumType value) {
        this.criticality = value;
    }

}
