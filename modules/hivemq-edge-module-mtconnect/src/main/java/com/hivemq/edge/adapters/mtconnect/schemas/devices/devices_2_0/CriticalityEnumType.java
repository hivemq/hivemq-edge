//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_2_0;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * The criticality
 * 
 * <p>Java class for CriticalityEnumType</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>{@code
 * <simpleType name="CriticalityEnumType">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="CRITICAL"/>
 *     <enumeration value="NONCRITICAL"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@com.fasterxml.jackson.annotation.JsonTypeName(value = "CriticalityEnumType")
@XmlType(name = "CriticalityEnumType")
@XmlEnum
public enum CriticalityEnumType {


    /**
     * critical
     * 
     */
    CRITICAL,

    /**
     * Not critical
     * 
     */
    NONCRITICAL;

    public String value() {
        return name();
    }

    public static CriticalityEnumType fromValue(String v) {
        return valueOf(v);
    }

}
