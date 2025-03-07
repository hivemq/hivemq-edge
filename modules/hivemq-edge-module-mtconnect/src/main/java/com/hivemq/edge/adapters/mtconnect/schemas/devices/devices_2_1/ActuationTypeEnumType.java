//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_2_1;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * The actuation of this component
 * 
 * <p>Java class for ActuationTypeEnumType</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>{@code
 * <simpleType name="ActuationTypeEnumType">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="DIRECT"/>
 *     <enumeration value="VIRTUAL"/>
 *     <enumeration value="NONE"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@com.fasterxml.jackson.annotation.JsonTypeName(value = "ActuationTypeEnumType")
@XmlType(name = "ActuationTypeEnumType")
@XmlEnum
public enum ActuationTypeEnumType {


    /**
     * The movement is initiated by the component.
     * 
     */
    DIRECT,

    /**
     * The motion is computed and is used for expressing an imaginary
     *             movement.
     * 
     */
    VIRTUAL,

    /**
     * There is no actuation of this Axis.
     * 
     */
    NONE;

    public String value() {
        return name();
    }

    public static ActuationTypeEnumType fromValue(String v) {
        return valueOf(v);
    }

}
