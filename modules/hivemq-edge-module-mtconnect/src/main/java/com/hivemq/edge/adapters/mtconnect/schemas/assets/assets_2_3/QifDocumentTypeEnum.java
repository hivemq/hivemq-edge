//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_2_3;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * contained QIF Document type as defined in the QIF Standard.
 * 
 * <p>Java class for QifDocumentTypeEnum</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>{@code
 * <simpleType name="QifDocumentTypeEnum">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="MEASUREMENT_RESOURCE"/>
 *     <enumeration value="PLAN"/>
 *     <enumeration value="PRODUCT"/>
 *     <enumeration value="RESULTS"/>
 *     <enumeration value="RULES"/>
 *     <enumeration value="STATISTICS"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@com.fasterxml.jackson.annotation.JsonTypeName(value = "QifDocumentTypeEnum")
@XmlType(name = "QifDocumentTypeEnum")
@XmlEnum
public enum QifDocumentTypeEnum {

    MEASUREMENT_RESOURCE,
    PLAN,
    PRODUCT,
    RESULTS,
    RULES,
    STATISTICS;

    public String value() {
        return name();
    }

    public static QifDocumentTypeEnum fromValue(String v) {
        return valueOf(v);
    }

}
