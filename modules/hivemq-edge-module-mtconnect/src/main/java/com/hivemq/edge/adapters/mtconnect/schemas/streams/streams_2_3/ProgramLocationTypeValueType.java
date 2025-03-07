//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_2_3;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * Controlled vocabulary for ProgramLocationType
 * 
 * <p>Java class for ProgramLocationTypeValueType</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>{@code
 * <simpleType name="ProgramLocationTypeValueType">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="LOCAL"/>
 *     <enumeration value="EXTERNAL"/>
 *     <enumeration value="UNAVAILABLE"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@com.fasterxml.jackson.annotation.JsonTypeName(value = "ProgramLocationTypeValueType")
@XmlType(name = "ProgramLocationTypeValueType")
@XmlEnum
public enum ProgramLocationTypeValueType {


    /**
     * managed by the controller.
     * 
     */
    LOCAL,

    /**
     * not managed by the controller.
     * 
     */
    EXTERNAL,

    /**
     * Value is indeterminate
     * 
     */
    UNAVAILABLE;

    public String value() {
        return name();
    }

    public static ProgramLocationTypeValueType fromValue(String v) {
        return valueOf(v);
    }

}
