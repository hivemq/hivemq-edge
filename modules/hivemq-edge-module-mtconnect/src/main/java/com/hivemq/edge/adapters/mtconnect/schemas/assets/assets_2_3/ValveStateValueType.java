//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_2_3;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * Controlled vocabulary for ValveState
 * 
 * <p>Java class for ValveStateValueType</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>{@code
 * <simpleType name="ValveStateValueType">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="OPEN"/>
 *     <enumeration value="OPENING"/>
 *     <enumeration value="CLOSED"/>
 *     <enumeration value="CLOSING"/>
 *     <enumeration value="UNAVAILABLE"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@com.fasterxml.jackson.annotation.JsonTypeName(value = "ValveStateValueType")
@XmlType(name = "ValveStateValueType")
@XmlEnum
public enum ValveStateValueType {


    /**
     * {{block(ValveState)}} where flow is allowed and the aperture is
     *             static. > Note: For a binary value, `OPEN` indicates the valve
     *             has the maximum possible aperture.
     * 
     */
    OPEN,

    /**
     * valve is transitioning from a `CLOSED` state to an `OPEN` state.
     * 
     */
    OPENING,

    /**
     * {{block(ValveState)}} where flow is not possible, the aperture is
     *             static, and the valve is completely shut.
     * 
     */
    CLOSED,

    /**
     * valve is transitioning from an `OPEN` state to a `CLOSED` state.
     * 
     */
    CLOSING,

    /**
     * Value is indeterminate
     * 
     */
    UNAVAILABLE;

    public String value() {
        return name();
    }

    public static ValveStateValueType fromValue(String v) {
        return valueOf(v);
    }

}
