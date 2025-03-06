//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.error.error_1_1;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * The types of measurements available
 * 
 * <p>Java class for DataItemEnumTypeEnum</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>{@code
 * <simpleType name="DataItemEnumTypeEnum">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="ACCELERATION"/>
 *     <enumeration value="ACTIVE_AXES"/>
 *     <enumeration value="ALARM"/>
 *     <enumeration value="AMPERAGE"/>
 *     <enumeration value="ANGLE"/>
 *     <enumeration value="ANGULAR_ACCELERATION"/>
 *     <enumeration value="ANGULAR_VELOCITY"/>
 *     <enumeration value="AVAILABILITY"/>
 *     <enumeration value="BLOCK"/>
 *     <enumeration value="CODE"/>
 *     <enumeration value="DISPLACEMENT"/>
 *     <enumeration value="DIRECTION"/>
 *     <enumeration value="DOOR_STATE"/>
 *     <enumeration value="EMERGENCY_STOP"/>
 *     <enumeration value="EXECUTION"/>
 *     <enumeration value="FREQUENCY"/>
 *     <enumeration value="PART_COUNT"/>
 *     <enumeration value="PART_ID"/>
 *     <enumeration value="PATH_FEEDRATE"/>
 *     <enumeration value="PATH_POSITION"/>
 *     <enumeration value="AXIS_FEEDRATE"/>
 *     <enumeration value="PATH_MODE"/>
 *     <enumeration value="LINE"/>
 *     <enumeration value="CONTROLLER_MODE"/>
 *     <enumeration value="LOAD"/>
 *     <enumeration value="MESSAGE"/>
 *     <enumeration value="POSITION"/>
 *     <enumeration value="POWER_STATUS"/>
 *     <enumeration value="POWER_STATE"/>
 *     <enumeration value="PRESSURE"/>
 *     <enumeration value="PROGRAM"/>
 *     <enumeration value="ROTARY_MODE"/>
 *     <enumeration value="COUPLED_AXES"/>
 *     <enumeration value="AXIS_COUPLING"/>
 *     <enumeration value="SPINDLE_SPEED"/>
 *     <enumeration value="TEMPERATURE"/>
 *     <enumeration value="TORQUE"/>
 *     <enumeration value="TOOL_ID"/>
 *     <enumeration value="VELOCITY"/>
 *     <enumeration value="VIBRATION"/>
 *     <enumeration value="VOLTAGE"/>
 *     <enumeration value="WATTAGE"/>
 *     <enumeration value="WORKHOLDING_ID"/>
 *     <enumeration value="COMMUNICATIONS"/>
 *     <enumeration value="LOGIC_PROGRAM"/>
 *     <enumeration value="MOTION_PROGRAM"/>
 *     <enumeration value="HARDWARE"/>
 *     <enumeration value="SYSTEM"/>
 *     <enumeration value="LEVEL"/>
 *     <enumeration value="ACTUATOR"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@com.fasterxml.jackson.annotation.JsonTypeName(value = "DataItemEnumTypeEnum")
@XmlType(name = "DataItemEnumTypeEnum")
@XmlEnum
public enum DataItemEnumTypeEnum {

    ACCELERATION,
    ACTIVE_AXES,
    ALARM,
    AMPERAGE,
    ANGLE,
    ANGULAR_ACCELERATION,
    ANGULAR_VELOCITY,
    AVAILABILITY,
    BLOCK,
    CODE,
    DISPLACEMENT,
    DIRECTION,
    DOOR_STATE,
    EMERGENCY_STOP,
    EXECUTION,
    FREQUENCY,
    PART_COUNT,
    PART_ID,
    PATH_FEEDRATE,
    PATH_POSITION,
    AXIS_FEEDRATE,
    PATH_MODE,
    LINE,
    CONTROLLER_MODE,
    LOAD,
    MESSAGE,
    POSITION,
    POWER_STATUS,
    POWER_STATE,
    PRESSURE,
    PROGRAM,
    ROTARY_MODE,
    COUPLED_AXES,
    AXIS_COUPLING,
    SPINDLE_SPEED,
    TEMPERATURE,
    TORQUE,
    TOOL_ID,
    VELOCITY,
    VIBRATION,
    VOLTAGE,
    WATTAGE,
    WORKHOLDING_ID,
    COMMUNICATIONS,
    LOGIC_PROGRAM,
    MOTION_PROGRAM,
    HARDWARE,
    SYSTEM,
    LEVEL,
    ACTUATOR;

    public String value() {
        return name();
    }

    public static DataItemEnumTypeEnum fromValue(String v) {
        return valueOf(v);
    }

}
