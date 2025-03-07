//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_2_4;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * distance from the gauge plane or from the end of the shank to the
 *         furthest point on the tool, if a gauge plane does not exist, to the
 *         cutting reference point determined by the main function of the tool. The
 *         {{block(CuttingTool)}} functional length will be the length of the
 *         entire tool, not a single cutting item. Each {{block(CuttingItem)}} can
 *         have an independent {{block(FunctionalLength)}} represented in its
 *         measurements.
 * 
 * <p>Java class for FunctionalLengthType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="FunctionalLengthType">
 *   <simpleContent>
 *     <extension base="<urn:mtconnect.org:MTConnectAssets:2.4>CommonMeasurementType">
 *     </extension>
 *   </simpleContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "FunctionalLengthType")
@XmlType(name = "FunctionalLengthType")
public class FunctionalLengthType
    extends CommonMeasurementType
{


}
