//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_2_0;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlType;


/**
 * {{block(Relationships)}} groups one or more {{block(Relationship)}}
 *         types. See {{package(Relationships)}}.
 * 
 * <p>Java class for RelationshipsType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="RelationshipsType">
 *   <complexContent>
 *     <extension base="{urn:mtconnect.org:MTConnectDevices:2.0}AbstractConfigurationType">
 *       <sequence>
 *         <element ref="{urn:mtconnect.org:MTConnectDevices:2.0}Relationship" maxOccurs="unbounded"/>
 *       </sequence>
 *     </extension>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "RelationshipsType")
@XmlType(name = "RelationshipsType", propOrder = {
    "relationship"
})
public class RelationshipsType
    extends AbstractConfigurationType
{

    /**
     * Description
     * 
     */
    @XmlElementRef(name = "Relationship", namespace = "urn:mtconnect.org:MTConnectDevices:2.0", type = JAXBElement.class)
    protected List<JAXBElement<? extends RelationshipType>> relationship;

    /**
     * Description
     * 
     * Gets the value of the relationship property.
     * 
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the relationship property.</p>
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * </p>
     * <pre>
     * getRelationship().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link ComponentRelationshipType }{@code >}
     * {@link JAXBElement }{@code <}{@link DeviceRelationshipType }{@code >}
     * {@link JAXBElement }{@code <}{@link RelationshipType }{@code >}
     * </p>
     * 
     * 
     * @return
     *     The value of the relationship property.
     */
    public List<JAXBElement<? extends RelationshipType>> getRelationship() {
        if (relationship == null) {
            relationship = new ArrayList<>();
        }
        return this.relationship;
    }

}
