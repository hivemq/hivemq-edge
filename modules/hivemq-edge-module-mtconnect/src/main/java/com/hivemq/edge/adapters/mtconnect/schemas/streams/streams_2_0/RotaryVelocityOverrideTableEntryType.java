//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_2_0;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * Table Entry of {{def(EventEnum:ROTARY_VELOCITY_OVERRIDE)}} This command
 *         represents a percentage change to the velocity calculated by a logic or
 *         motion program or set by a switch for a {{block(Rotary)}} type axis.
 * 
 * <p>Java class for RotaryVelocityOverrideTableEntryType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="RotaryVelocityOverrideTableEntryType">
 *   <complexContent>
 *     <extension base="{urn:mtconnect.org:MTConnectStreams:2.0}EntryType">
 *       <sequence>
 *         <element name="Cell" type="{urn:mtconnect.org:MTConnectStreams:2.0}RotaryVelocityOverrideCellType" maxOccurs="unbounded" minOccurs="0"/>
 *       </sequence>
 *     </extension>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "RotaryVelocityOverrideTableEntryType")
@XmlType(name = "RotaryVelocityOverrideTableEntryType")
public class RotaryVelocityOverrideTableEntryType
    extends EntryType
{


}
