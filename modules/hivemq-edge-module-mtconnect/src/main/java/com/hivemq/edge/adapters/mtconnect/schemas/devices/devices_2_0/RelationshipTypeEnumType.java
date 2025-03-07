//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_2_0;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * The list of possible association types
 * 
 * <p>Java class for RelationshipTypeEnumType</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>{@code
 * <simpleType name="RelationshipTypeEnumType">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="PARENT"/>
 *     <enumeration value="CHILD"/>
 *     <enumeration value="PEER"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@com.fasterxml.jackson.annotation.JsonTypeName(value = "RelationshipTypeEnumType")
@XmlType(name = "RelationshipTypeEnumType")
@XmlEnum
public enum RelationshipTypeEnumType {


    /**
     * The related entity is a parent
     * 
     */
    PARENT,

    /**
     * The related entity is a child
     * 
     */
    CHILD,

    /**
     * The related entity is a peer
     * 
     */
    PEER;

    public String value() {
        return name();
    }

    public static RelationshipTypeEnumType fromValue(String v) {
        return valueOf(v);
    }

}
