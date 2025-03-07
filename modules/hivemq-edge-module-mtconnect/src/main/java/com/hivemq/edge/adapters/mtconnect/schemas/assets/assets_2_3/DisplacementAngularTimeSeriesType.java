//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_2_3;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * Time series of {{def(SampleEnum::DISPLACEMENT_ANGULAR)}} > Note: The
 *         displacement vector **MAY** be defined by the motion of the owning
 *         {{block(Component)}}.
 * 
 * <p>Java class for DisplacementAngularTimeSeriesType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="DisplacementAngularTimeSeriesType">
 *   <simpleContent>
 *     <extension base="<urn:mtconnect.org:MTConnectAssets:2.3>TimeSeriesType">
 *     </extension>
 *   </simpleContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "DisplacementAngularTimeSeriesType")
@XmlType(name = "DisplacementAngularTimeSeriesType")
public class DisplacementAngularTimeSeriesType
    extends TimeSeriesType
{


}
