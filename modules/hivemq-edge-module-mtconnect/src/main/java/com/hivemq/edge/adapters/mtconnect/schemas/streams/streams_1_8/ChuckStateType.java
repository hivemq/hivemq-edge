//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_8;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * An indication of the operating state of a mechanism that holds a part or
 *         stock material during a manufacturing process. It may also represent a
 *         mechanism that holds any other mechanism in place within a piece of
 *         equipment.
 * 
 * <p>Java class for ChuckStateType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="ChuckStateType">
 *   <simpleContent>
 *     <restriction base="<urn:mtconnect.org:MTConnectStreams:1.8>EventType">
 *     </restriction>
 *   </simpleContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "ChuckStateType")
@XmlType(name = "ChuckStateType")
public class ChuckStateType
    extends EventType
{


}
