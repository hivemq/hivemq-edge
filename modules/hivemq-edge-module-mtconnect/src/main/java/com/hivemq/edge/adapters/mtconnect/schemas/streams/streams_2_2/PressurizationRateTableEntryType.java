//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_2_2;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * Table Entry of {{def(SampleEnum:PRESSURIZATION_RATE)}}
 * 
 * <p>Java class for PressurizationRateTableEntryType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="PressurizationRateTableEntryType">
 *   <complexContent>
 *     <extension base="{urn:mtconnect.org:MTConnectStreams:2.2}EntryType">
 *       <sequence>
 *         <element name="Cell" type="{urn:mtconnect.org:MTConnectStreams:2.2}PressurizationRateCellType" maxOccurs="unbounded" minOccurs="0"/>
 *       </sequence>
 *     </extension>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "PressurizationRateTableEntryType")
@XmlType(name = "PressurizationRateTableEntryType")
public class PressurizationRateTableEntryType
    extends EntryType
{


}
