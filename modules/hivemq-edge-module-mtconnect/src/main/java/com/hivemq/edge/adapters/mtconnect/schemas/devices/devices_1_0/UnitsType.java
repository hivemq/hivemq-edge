//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_0;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;


/**
 * The units supported
 * 
 * <p>Java class for UnitsType</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>{@code
 * <simpleType name="UnitsType">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="STATUS"/>
 *     <enumeration value="MILLIMETER"/>
 *     <enumeration value="DEGREE"/>
 *     <enumeration value="MILLIMETER/SECOND"/>
 *     <enumeration value="DEGREE/SECOND"/>
 *     <enumeration value="MILLIMETER/SECOND^2"/>
 *     <enumeration value="DEGREE/SECOND^2"/>
 *     <enumeration value="LITER"/>
 *     <enumeration value="KILOGRAM"/>
 *     <enumeration value="NEWTON"/>
 *     <enumeration value="CELSIUS"/>
 *     <enumeration value="REVOLUTION/MINUTE"/>
 *     <enumeration value="VOLT"/>
 *     <enumeration value="AMPERE"/>
 *     <enumeration value="WATT"/>
 *     <enumeration value="PART_COUNT"/>
 *     <enumeration value="PASCAL"/>
 *     <enumeration value="PERCENT"/>
 *     <enumeration value="NEWTON_METER"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@com.fasterxml.jackson.annotation.JsonTypeName(value = "UnitsType")
@XmlType(name = "UnitsType")
@XmlEnum
public enum UnitsType {

    STATUS("STATUS"),
    MILLIMETER("MILLIMETER"),
    DEGREE("DEGREE"),
    @XmlEnumValue("MILLIMETER/SECOND")
    MILLIMETER_SECOND("MILLIMETER/SECOND"),
    @XmlEnumValue("DEGREE/SECOND")
    DEGREE_SECOND("DEGREE/SECOND"),
    @XmlEnumValue("MILLIMETER/SECOND^2")
    MILLIMETER_SECOND_2("MILLIMETER/SECOND^2"),
    @XmlEnumValue("DEGREE/SECOND^2")
    DEGREE_SECOND_2("DEGREE/SECOND^2"),
    LITER("LITER"),
    KILOGRAM("KILOGRAM"),
    NEWTON("NEWTON"),
    CELSIUS("CELSIUS"),
    @XmlEnumValue("REVOLUTION/MINUTE")
    REVOLUTION_MINUTE("REVOLUTION/MINUTE"),
    VOLT("VOLT"),
    AMPERE("AMPERE"),
    WATT("WATT"),
    PART_COUNT("PART_COUNT"),
    PASCAL("PASCAL"),
    PERCENT("PERCENT"),
    NEWTON_METER("NEWTON_METER");
    private final String value;

    UnitsType(String v) {
        value = v;
    }

    /**
     * Gets the value associated to the enum constant.
     * 
     * @return
     *     The value linked to the enum.
     */
    public String value() {
        return value;
    }

    /**
     * Gets the enum associated to the value passed as parameter.
     * 
     * @param v
     *     The value to get the enum from.
     * @return
     *     The enum which corresponds to the value, if it exists.
     * @throws IllegalArgumentException
     *     If no value matches in the enum declaration.
     */
    public static UnitsType fromValue(String v) {
        for (UnitsType c: UnitsType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
