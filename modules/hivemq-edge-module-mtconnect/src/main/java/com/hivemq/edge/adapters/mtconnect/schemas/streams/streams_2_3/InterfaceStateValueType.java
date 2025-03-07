//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_2_3;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * Controlled vocabulary for InterfaceState
 * 
 * <p>Java class for InterfaceStateValueType</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>{@code
 * <simpleType name="InterfaceStateValueType">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="ENABLED"/>
 *     <enumeration value="DISABLED"/>
 *     <enumeration value="UNAVAILABLE"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@com.fasterxml.jackson.annotation.JsonTypeName(value = "InterfaceStateValueType")
@XmlType(name = "InterfaceStateValueType")
@XmlEnum
public enum InterfaceStateValueType {


    /**
     * {{block(Interface)}} is currently operational and performing as
     *             expected.
     * 
     */
    ENABLED,

    /**
     * {{block(Interface)}} is currently not operational.
     * 
     */
    DISABLED,

    /**
     * Value is indeterminate
     * 
     */
    UNAVAILABLE;

    public String value() {
        return name();
    }

    public static InterfaceStateValueType fromValue(String v) {
        return valueOf(v);
    }

}
