//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_1_8;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * File state
 * 
 * <p>Java class for FileStateType</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>{@code
 * <simpleType name="FileStateType">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="EXPERIMENTAL"/>
 *     <enumeration value="PRODUCTION"/>
 *     <enumeration value="REVISION"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@com.fasterxml.jackson.annotation.JsonTypeName(value = "FileStateType")
@XmlType(name = "FileStateType")
@XmlEnum
public enum FileStateType {


    /**
     * used for processes other than production or otherwise defined.
     * 
     */
    EXPERIMENTAL,

    /**
     * used for production processes
     * 
     */
    PRODUCTION,

    /**
     * the content is modified from PRODUCTION or EXPERIMENTAL (Note: To
     *             capture northbound changes. execution -> prod engineering)
     * 
     */
    REVISION;

    public String value() {
        return name();
    }

    public static FileStateType fromValue(String v) {
        return valueOf(v);
    }

}
