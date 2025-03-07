//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_6;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * An indication of the state of an interlock function or control logic
 *         state intended to prevent the associated chuck component from being
 *         operated.
 * 
 * <p>Java class for ChuckInterlockType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="ChuckInterlockType">
 *   <simpleContent>
 *     <restriction base="<urn:mtconnect.org:MTConnectStreams:1.6>EventType">
 *     </restriction>
 *   </simpleContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "ChuckInterlockType")
@XmlType(name = "ChuckInterlockType")
public class ChuckInterlockType
    extends EventType
{


}
