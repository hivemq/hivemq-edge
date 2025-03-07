//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_2_3;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * DataSet of {{def(EventEnum::WAIT_STATE)}} When
 *         {{property(Execution::result)}} is not `WAIT`,
 *         {{property(Observation::isUnavailable)}} of {{block(WaitState)}}
 *         **MUST** be `true`.
 * 
 * <p>Java class for WaitStateEntryType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="WaitStateEntryType">
 *   <simpleContent>
 *     <restriction base="<urn:mtconnect.org:MTConnectStreams:2.3>EntryType">
 *     </restriction>
 *   </simpleContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "WaitStateEntryType")
@XmlType(name = "WaitStateEntryType")
public class WaitStateEntryType
    extends EntryType
{


}
