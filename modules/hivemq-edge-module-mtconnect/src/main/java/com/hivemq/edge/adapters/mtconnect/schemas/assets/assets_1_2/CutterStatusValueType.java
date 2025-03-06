//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_1_2;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * The state of the tool. These can be combined to define the complete
 *         cutting tool state
 * 
 * <p>Java class for CutterStatusValueType</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>{@code
 * <simpleType name="CutterStatusValueType">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="NEW"/>
 *     <enumeration value="AVAILABLE"/>
 *     <enumeration value="UNAVAILABLE"/>
 *     <enumeration value="ALLOCATED"/>
 *     <enumeration value="UNALLOCATED"/>
 *     <enumeration value="MEASURED"/>
 *     <enumeration value="NOT_REGISTERED"/>
 *     <enumeration value="RECONDITIONED"/>
 *     <enumeration value="USED"/>
 *     <enumeration value="EXPIRED"/>
 *     <enumeration value="TAGGED_OUT"/>
 *     <enumeration value="BROKEN"/>
 *     <enumeration value="UNKNOWN"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@com.fasterxml.jackson.annotation.JsonTypeName(value = "CutterStatusValueType")
@XmlType(name = "CutterStatusValueType")
@XmlEnum
public enum CutterStatusValueType {

    NEW,
    AVAILABLE,
    UNAVAILABLE,
    ALLOCATED,
    UNALLOCATED,
    MEASURED,
    NOT_REGISTERED,
    RECONDITIONED,
    USED,
    EXPIRED,
    TAGGED_OUT,
    BROKEN,
    UNKNOWN;

    public String value() {
        return name();
    }

    public static CutterStatusValueType fromValue(String v) {
        return valueOf(v);
    }

}
