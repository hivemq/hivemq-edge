//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_1_8;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * Controlled vocabulary for PathMode
 * 
 * <p>Java class for PathModeValueType</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>{@code
 * <simpleType name="PathModeValueType">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="INDEPENDENT"/>
 *     <enumeration value="MASTER"/>
 *     <enumeration value="SYNCHRONOUS"/>
 *     <enumeration value="MIRROR"/>
 *     <enumeration value="UNAVAILABLE"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@com.fasterxml.jackson.annotation.JsonTypeName(value = "PathModeValueType")
@XmlType(name = "PathModeValueType")
@XmlEnum
public enum PathModeValueType {


    /**
     * The path is operating independently and without the influence of
     *             another path.
     * 
     */
    INDEPENDENT,

    /**
     * It provides information or state values that influences the
     *             operation of other {{block(DataItem)}} of similar type.
     * 
     */
    MASTER,

    /**
     * Physical or logical parts which are not physically connected to each
     *             other but are operating together.
     * 
     */
    SYNCHRONOUS,

    /**
     * The axes associated with the path are mirroring the motion of the
     *             {{block(MASTER)}} path.
     * 
     */
    MIRROR,

    /**
     * Value is indeterminate
     * 
     */
    UNAVAILABLE;

    public String value() {
        return name();
    }

    public static PathModeValueType fromValue(String v) {
        return valueOf(v);
    }

}
