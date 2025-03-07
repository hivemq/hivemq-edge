//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_2_0;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlIDREF;
import jakarta.xml.bind.annotation.XmlType;


/**
 * {{block(Relationship)}} that describes the association between two
 *         components within a piece of equipment that function independently but
 *         together perform a capability or service within a piece of equipment.
 * 
 * <p>Java class for ComponentRelationshipType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="ComponentRelationshipType">
 *   <complexContent>
 *     <extension base="{urn:mtconnect.org:MTConnectDevices:2.0}RelationshipType">
 *       <attribute name="idRef" use="required" type="{urn:mtconnect.org:MTConnectDevices:2.0}IdRefType" />
 *     </extension>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "ComponentRelationshipType")
@XmlType(name = "ComponentRelationshipType")
public class ComponentRelationshipType
    extends RelationshipType
{

    /**
     * A reference to the device uuid
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "idRef")
    @XmlAttribute(name = "idRef", required = true)
    @XmlIDREF
    protected Object idRef;

    /**
     * A reference to the device uuid
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getIdRef() {
        return idRef;
    }

    /**
     * Sets the value of the idRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     * @see #getIdRef()
     */
    public void setIdRef(Object value) {
        this.idRef = value;
    }

}
