//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_1_7;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * The diameter of a circle on which the defined point Pk located on this
 *         Cutting Tool. The normal of the machined peripheral surface points
 *         towards the axis of the Cutting Tool.
 * 
 * <p>Java class for CuttingDiameterType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="CuttingDiameterType">
 *   <simpleContent>
 *     <extension base="<urn:mtconnect.org:MTConnectAssets:1.7>CuttingItemMeasurementType">
 *     </extension>
 *   </simpleContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "CuttingDiameterType")
@XmlType(name = "CuttingDiameterType")
public class CuttingDiameterType
    extends CuttingItemMeasurementType
{


}
