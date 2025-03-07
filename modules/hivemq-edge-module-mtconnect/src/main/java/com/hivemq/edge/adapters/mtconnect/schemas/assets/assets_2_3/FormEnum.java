//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_2_3;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * form of the {{term(raw material)}}.
 * 
 * <p>Java class for FormEnum</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>{@code
 * <simpleType name="FormEnum">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="BAR"/>
 *     <enumeration value="SHEET"/>
 *     <enumeration value="BLOCK"/>
 *     <enumeration value="CASTING"/>
 *     <enumeration value="POWDER"/>
 *     <enumeration value="LIQUID"/>
 *     <enumeration value="GEL"/>
 *     <enumeration value="FILAMENT"/>
 *     <enumeration value="GAS"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@com.fasterxml.jackson.annotation.JsonTypeName(value = "FormEnum")
@XmlType(name = "FormEnum")
@XmlEnum
public enum FormEnum {

    BAR,
    SHEET,
    BLOCK,
    CASTING,
    POWDER,
    LIQUID,
    GEL,
    FILAMENT,
    GAS;

    public String value() {
        return name();
    }

    public static FormEnum fromValue(String v) {
        return valueOf(v);
    }

}
