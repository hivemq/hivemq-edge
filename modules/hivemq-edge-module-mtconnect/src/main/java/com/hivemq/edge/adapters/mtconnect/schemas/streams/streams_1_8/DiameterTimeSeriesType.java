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
 * Time series of The measured dimension of a diameter.
 * 
 * <p>Java class for DiameterTimeSeriesType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="DiameterTimeSeriesType">
 *   <simpleContent>
 *     <extension base="<urn:mtconnect.org:MTConnectStreams:1.8>TimeSeriesType">
 *     </extension>
 *   </simpleContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "DiameterTimeSeriesType")
@XmlType(name = "DiameterTimeSeriesType")
public class DiameterTimeSeriesType
    extends TimeSeriesType
{


}
