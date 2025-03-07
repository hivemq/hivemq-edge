//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_7;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * The current state of the emergency stop signal for a piece of equipment,
 *         controller path, or any other component or subsystem of a piece of
 *         equipment.
 * 
 * <p>Java class for EmergencyStopType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="EmergencyStopType">
 *   <simpleContent>
 *     <restriction base="<urn:mtconnect.org:MTConnectStreams:1.7>EventType">
 *     </restriction>
 *   </simpleContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "EmergencyStopType")
@XmlType(name = "EmergencyStopType")
public class EmergencyStopType
    extends EventType
{


}
