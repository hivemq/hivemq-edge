//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_2_1;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * Controlled vocabulary for ChuckInterlock
 * 
 * <p>Java class for ChuckInterlockValueType</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>{@code
 * <simpleType name="ChuckInterlockValueType">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="ACTIVE"/>
 *     <enumeration value="INACTIVE"/>
 *     <enumeration value="UNAVAILABLE"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@com.fasterxml.jackson.annotation.JsonTypeName(value = "ChuckInterlockValueType")
@XmlType(name = "ChuckInterlockValueType")
@XmlEnum
public enum ChuckInterlockValueType {


    /**
     * chuck cannot be unclamped.
     * 
     */
    ACTIVE,

    /**
     * chuck can be unclamped.
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

    public static ChuckInterlockValueType fromValue(String v) {
        return valueOf(v);
    }

}
