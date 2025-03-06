//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_3;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * The possible values for an interface event
 * 
 * <p>Java class for InterfaceEventValuesType</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>{@code
 * <simpleType name="InterfaceEventValuesType">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="UNAVAILABLE"/>
 *     <enumeration value="NOT_READY"/>
 *     <enumeration value="READY"/>
 *     <enumeration value="ACTIVE"/>
 *     <enumeration value="COMPLETE"/>
 *     <enumeration value="FAIL"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@com.fasterxml.jackson.annotation.JsonTypeName(value = "InterfaceEventValuesType")
@XmlType(name = "InterfaceEventValuesType")
@XmlEnum
public enum InterfaceEventValuesType {

    UNAVAILABLE,
    NOT_READY,
    READY,
    ACTIVE,
    COMPLETE,
    FAIL;

    public String value() {
        return name();
    }

    public static InterfaceEventValuesType fromValue(String v) {
        return valueOf(v);
    }

}
