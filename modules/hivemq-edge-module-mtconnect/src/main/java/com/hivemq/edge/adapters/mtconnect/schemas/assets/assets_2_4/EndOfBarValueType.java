//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_2_4;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * Controlled vocabulary for EndOfBar
 * 
 * <p>Java class for EndOfBarValueType</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>{@code
 * <simpleType name="EndOfBarValueType">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="YES"/>
 *     <enumeration value="NO"/>
 *     <enumeration value="UNAVAILABLE"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@com.fasterxml.jackson.annotation.JsonTypeName(value = "EndOfBarValueType")
@XmlType(name = "EndOfBarValueType")
@XmlEnum
public enum EndOfBarValueType {


    /**
     * {{block(EndOfBar)}} has been reached.
     * 
     */
    YES,

    /**
     * {{block(EndOfBar)}} has not been reached.
     * 
     */
    NO,

    /**
     * Value is indeterminate
     * 
     */
    UNAVAILABLE;

    public String value() {
        return name();
    }

    public static EndOfBarValueType fromValue(String v) {
        return valueOf(v);
    }

}
