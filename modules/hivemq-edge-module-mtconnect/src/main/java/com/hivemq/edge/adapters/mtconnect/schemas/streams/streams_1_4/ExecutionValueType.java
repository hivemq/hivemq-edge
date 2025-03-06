//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_4;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * The execution value
 * 
 * <p>Java class for ExecutionValueType</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>{@code
 * <simpleType name="ExecutionValueType">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="READY"/>
 *     <enumeration value="INTERRUPTED"/>
 *     <enumeration value="ACTIVE"/>
 *     <enumeration value="STOPPED"/>
 *     <enumeration value="FEED_HOLD"/>
 *     <enumeration value="PROGRAM_COMPLETED"/>
 *     <enumeration value="PROGRAM_STOPPED"/>
 *     <enumeration value="OPTIONAL_STOP"/>
 *     <enumeration value="UNAVAILABLE"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@com.fasterxml.jackson.annotation.JsonTypeName(value = "ExecutionValueType")
@XmlType(name = "ExecutionValueType")
@XmlEnum
public enum ExecutionValueType {

    READY,
    INTERRUPTED,
    ACTIVE,
    STOPPED,
    FEED_HOLD,
    PROGRAM_COMPLETED,
    PROGRAM_STOPPED,
    OPTIONAL_STOP,
    UNAVAILABLE;

    public String value() {
        return name();
    }

    public static ExecutionValueType fromValue(String v) {
        return valueOf(v);
    }

}
