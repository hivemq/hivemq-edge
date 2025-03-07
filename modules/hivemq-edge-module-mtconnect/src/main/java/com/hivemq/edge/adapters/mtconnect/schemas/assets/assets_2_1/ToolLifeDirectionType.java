//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_2_1;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * The direction of tool life count
 * 
 * <p>Java class for ToolLifeDirectionType</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>{@code
 * <simpleType name="ToolLifeDirectionType">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="UP"/>
 *     <enumeration value="DOWN"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@com.fasterxml.jackson.annotation.JsonTypeName(value = "ToolLifeDirectionType")
@XmlType(name = "ToolLifeDirectionType")
@XmlEnum
public enum ToolLifeDirectionType {


    /**
     * The tool life counts up from the 0 to maximum
     * 
     */
    UP,

    /**
     * The tool life counts down from maximum to 0
     * 
     */
    DOWN;

    public String value() {
        return name();
    }

    public static ToolLifeDirectionType fromValue(String v) {
        return valueOf(v);
    }

}
