//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_1_5;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * Controlled vocabulary for RotaryMode
 * 
 * <p>Java class for RotaryModeValueType</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>{@code
 * <simpleType name="RotaryModeValueType">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="SPINDLE"/>
 *     <enumeration value="INDEX"/>
 *     <enumeration value="CONTOUR"/>
 *     <enumeration value="UNAVAILABLE"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@com.fasterxml.jackson.annotation.JsonTypeName(value = "RotaryModeValueType")
@XmlType(name = "RotaryModeValueType")
@XmlEnum
public enum RotaryModeValueType {


    /**
     * The axis is functioning as a spindle.
     * 
     */
    SPINDLE,

    /**
     * The axis is configured to index.
     * 
     */
    INDEX,

    /**
     * The position of the axis is being interpolated.
     * 
     */
    CONTOUR,

    /**
     * Value is indeterminate
     * 
     */
    UNAVAILABLE;

    public String value() {
        return name();
    }

    public static RotaryModeValueType fromValue(String v) {
        return valueOf(v);
    }

}
