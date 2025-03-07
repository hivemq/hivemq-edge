//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_2_0;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * Controlled vocabulary for EmergencyStop
 * 
 * <p>Java class for EmergencyStopValueType</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>{@code
 * <simpleType name="EmergencyStopValueType">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="ARMED"/>
 *     <enumeration value="TRIGGERED"/>
 *     <enumeration value="UNAVAILABLE"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@com.fasterxml.jackson.annotation.JsonTypeName(value = "EmergencyStopValueType")
@XmlType(name = "EmergencyStopValueType")
@XmlEnum
public enum EmergencyStopValueType {


    /**
     * emergency stop circuit is complete and the piece of equipment,
     *             component, or composition is allowed to operate.
     * 
     */
    ARMED,

    /**
     * operation of the piece of equipment, component, or composition is
     *             inhibited.
     * 
     */
    TRIGGERED,

    /**
     * Value is indeterminate
     * 
     */
    UNAVAILABLE;

    public String value() {
        return name();
    }

    public static EmergencyStopValueType fromValue(String v) {
        return valueOf(v);
    }

}
