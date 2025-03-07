//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_2_1;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * How the data items are related
 * 
 * <p>Java class for SpecificationRelationshipTypeEnumType</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>{@code
 * <simpleType name="SpecificationRelationshipTypeEnumType">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="LIMIT"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@com.fasterxml.jackson.annotation.JsonTypeName(value = "SpecificationRelationshipTypeEnumType")
@XmlType(name = "SpecificationRelationshipTypeEnumType")
@XmlEnum
public enum SpecificationRelationshipTypeEnumType {


    /**
     * The referenced DataItem provides process limits.
     * 
     */
    LIMIT;

    public String value() {
        return name();
    }

    public static SpecificationRelationshipTypeEnumType fromValue(String v) {
        return valueOf(v);
    }

}
