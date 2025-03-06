//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_3;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;


/**
 * The units supported
 * 
 * <p>Java class for UnitsTypeEnum</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>{@code
 * <simpleType name="UnitsTypeEnum">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="AMPERE"/>
 *     <enumeration value="CELSIUS"/>
 *     <enumeration value="COUNT"/>
 *     <enumeration value="DEGREE"/>
 *     <enumeration value="DEGREE/SECOND"/>
 *     <enumeration value="DEGREE/SECOND^2"/>
 *     <enumeration value="HERTZ"/>
 *     <enumeration value="JOULE"/>
 *     <enumeration value="KILOGRAM"/>
 *     <enumeration value="LITER"/>
 *     <enumeration value="LITER/SECOND"/>
 *     <enumeration value="MILLIMETER"/>
 *     <enumeration value="MILLIMETER/SECOND"/>
 *     <enumeration value="MILLIMETER/SECOND^2"/>
 *     <enumeration value="MILLIMETER_3D"/>
 *     <enumeration value="NEWTON"/>
 *     <enumeration value="NEWTON_METER"/>
 *     <enumeration value="PASCAL"/>
 *     <enumeration value="PERCENT"/>
 *     <enumeration value="PH"/>
 *     <enumeration value="REVOLUTION/MINUTE"/>
 *     <enumeration value="SECOND"/>
 *     <enumeration value="VOLT"/>
 *     <enumeration value="WATT"/>
 *     <enumeration value="OHM"/>
 *     <enumeration value="SOUND_LEVEL"/>
 *     <enumeration value="SIEMENS/METER"/>
 *     <enumeration value="MICRO_RADIAN"/>
 *     <enumeration value="PASCAL_SECOND"/>
 *     <enumeration value="VOLT_AMPERE"/>
 *     <enumeration value="VOLT_AMPERE_REACTIVE"/>
 *     <enumeration value="WATT_SECOND"/>
 *     <enumeration value="DECIBEL"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@com.fasterxml.jackson.annotation.JsonTypeName(value = "UnitsTypeEnum")
@XmlType(name = "UnitsTypeEnum")
@XmlEnum
public enum UnitsTypeEnum {

    AMPERE("AMPERE"),
    CELSIUS("CELSIUS"),
    COUNT("COUNT"),
    DEGREE("DEGREE"),
    @XmlEnumValue("DEGREE/SECOND")
    DEGREE_SECOND("DEGREE/SECOND"),
    @XmlEnumValue("DEGREE/SECOND^2")
    DEGREE_SECOND_2("DEGREE/SECOND^2"),
    HERTZ("HERTZ"),
    JOULE("JOULE"),
    KILOGRAM("KILOGRAM"),
    LITER("LITER"),
    @XmlEnumValue("LITER/SECOND")
    LITER_SECOND("LITER/SECOND"),
    MILLIMETER("MILLIMETER"),
    @XmlEnumValue("MILLIMETER/SECOND")
    MILLIMETER_SECOND("MILLIMETER/SECOND"),
    @XmlEnumValue("MILLIMETER/SECOND^2")
    MILLIMETER_SECOND_2("MILLIMETER/SECOND^2"),
    @XmlEnumValue("MILLIMETER_3D")
    MILLIMETER_3_D("MILLIMETER_3D"),
    NEWTON("NEWTON"),
    NEWTON_METER("NEWTON_METER"),
    PASCAL("PASCAL"),
    PERCENT("PERCENT"),
    PH("PH"),
    @XmlEnumValue("REVOLUTION/MINUTE")
    REVOLUTION_MINUTE("REVOLUTION/MINUTE"),
    SECOND("SECOND"),
    VOLT("VOLT"),
    WATT("WATT"),
    OHM("OHM"),
    SOUND_LEVEL("SOUND_LEVEL"),
    @XmlEnumValue("SIEMENS/METER")
    SIEMENS_METER("SIEMENS/METER"),
    MICRO_RADIAN("MICRO_RADIAN"),
    PASCAL_SECOND("PASCAL_SECOND"),
    VOLT_AMPERE("VOLT_AMPERE"),
    VOLT_AMPERE_REACTIVE("VOLT_AMPERE_REACTIVE"),
    WATT_SECOND("WATT_SECOND"),
    DECIBEL("DECIBEL");
    private final String value;

    UnitsTypeEnum(String v) {
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
    public static UnitsTypeEnum fromValue(String v) {
        for (UnitsTypeEnum c: UnitsTypeEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
