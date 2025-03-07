//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_2_3;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * The reset intervals
 * 
 * <p>Java class for DataItemResetValueEnum</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>{@code
 * <simpleType name="DataItemResetValueEnum">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="ACTION_COMPLETE"/>
 *     <enumeration value="ANNUAL"/>
 *     <enumeration value="DAY"/>
 *     <enumeration value="LIFE"/>
 *     <enumeration value="MAINTENANCE"/>
 *     <enumeration value="MONTH"/>
 *     <enumeration value="POWER_ON"/>
 *     <enumeration value="SHIFT"/>
 *     <enumeration value="WEEK"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@com.fasterxml.jackson.annotation.JsonTypeName(value = "DataItemResetValueEnum")
@XmlType(name = "DataItemResetValueEnum")
@XmlEnum
public enum DataItemResetValueEnum {


    /**
     * {{term(observation)}} of the {{block(DataItem)}} that is measuring
     *             an action or operation is to be reset upon completion of that action
     *             or operation.
     * 
     */
    ACTION_COMPLETE,

    /**
     * {{term(observation)}} of the {{block(DataItem)}} is to be reset at
     *             the end of a 12-month period.
     * 
     */
    ANNUAL,

    /**
     * {{term(observation)}} of the {{block(DataItem)}} is to be reset at
     *             the end of a 24-hour period.
     * 
     */
    DAY,

    /**
     * {{term(observation)}} of the {{block(DataItem)}} is not reset and
     *             accumulates for the entire life of the piece of equipment.
     * 
     */
    LIFE,

    /**
     * {{term(observation)}} of the {{block(DataItem)}} is to be reset upon
     *             completion of a maintenance event.
     * 
     */
    MAINTENANCE,

    /**
     * {{term(observation)}} of the {{block(DataItem)}} is to be reset at
     *             the end of a monthly period.
     * 
     */
    MONTH,

    /**
     * {{term(observation)}} of the {{block(DataItem)}} is to be reset when
     *             power was applied to the piece of equipment after a planned or
     *             unplanned interruption of power has occurred.
     * 
     */
    POWER_ON,

    /**
     * {{term(observation)}} of the {{block(DataItem)}} is to be reset at
     *             the end of a work shift.
     * 
     */
    SHIFT,

    /**
     * {{term(observation)}} of the {{block(DataItem)}} is to be reset at
     *             the end of a 7-day period.
     * 
     */
    WEEK;

    public String value() {
        return name();
    }

    public static DataItemResetValueEnum fromValue(String v) {
        return valueOf(v);
    }

}
