//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_5;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * Controlled vocabulary for SpindleInterlock
 * 
 * <p>Java class for SpindleInterlockValueType</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>{@code
 * <simpleType name="SpindleInterlockValueType">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="ACTIVE"/>
 *     <enumeration value="INACTIVE"/>
 *     <enumeration value="UNAVAILABLE"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@com.fasterxml.jackson.annotation.JsonTypeName(value = "SpindleInterlockValueType")
@XmlType(name = "SpindleInterlockValueType")
@XmlEnum
public enum SpindleInterlockValueType {


    /**
     * The value of the data entity that is engaging.
     * 
     */
    ACTIVE,

    /**
     * The value of the data entity that is not engaging.
     * 
     */
    INACTIVE,

    /**
     * Value is indeterminate
     * 
     */
    UNAVAILABLE;

    public String value() {
        return name();
    }

    public static SpindleInterlockValueType fromValue(String v) {
        return valueOf(v);
    }

}
