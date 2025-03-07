//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_2_1;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * The type classification of a file
 * 
 * <p>Java class for ApplicationTypeEnum</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>{@code
 * <simpleType name="ApplicationTypeEnum">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="DESIGN"/>
 *     <enumeration value="DATA"/>
 *     <enumeration value="DOCUMENTATION"/>
 *     <enumeration value="INSTRUCTIONS"/>
 *     <enumeration value="LOG"/>
 *     <enumeration value="PRODUCTION_PROGRAM"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@com.fasterxml.jackson.annotation.JsonTypeName(value = "ApplicationTypeEnum")
@XmlType(name = "ApplicationTypeEnum")
@XmlEnum
public enum ApplicationTypeEnum {


    /**
     * Computer aided design files or drawings
     * 
     */
    DESIGN,

    /**
     * Generic data
     * 
     */
    DATA,

    /**
     * Documentation regarding a category of file
     * 
     */
    DOCUMENTATION,

    /**
     * User instructions regarding the execution of a task
     * 
     */
    INSTRUCTIONS,

    /**
     * The data related to the history of a machine or process
     * 
     */
    LOG,

    /**
     * Machine instructions to perform a process
     * 
     */
    PRODUCTION_PROGRAM;

    public String value() {
        return name();
    }

    public static ApplicationTypeEnum fromValue(String v) {
        return valueOf(v);
    }

}
