//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_1;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * The cnc mode value
 * 
 * <p>Java class for ControllerModeValueType</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>{@code
 * <simpleType name="ControllerModeValueType">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="AUTOMATIC"/>
 *     <enumeration value="MANUAL"/>
 *     <enumeration value="MANUAL_DATA_INPUT"/>
 *     <enumeration value="SEMI_AUTOMATIC"/>
 *     <enumeration value="UNAVAILABLE"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@com.fasterxml.jackson.annotation.JsonTypeName(value = "ControllerModeValueType")
@XmlType(name = "ControllerModeValueType")
@XmlEnum
public enum ControllerModeValueType {

    AUTOMATIC,
    MANUAL,
    MANUAL_DATA_INPUT,
    SEMI_AUTOMATIC,
    UNAVAILABLE;

    public String value() {
        return name();
    }

    public static ControllerModeValueType fromValue(String v) {
        return valueOf(v);
    }

}
