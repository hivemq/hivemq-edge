//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_1_8;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * **DEPRECATED** in *Version 1.6*. Replaced by {{block(AmperageAC)}} and
 *         {{block(AmperageDC)}}. The measurement of electrical current.
 * 
 * <p>Java class for AmperageType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="AmperageType">
 *   <simpleContent>
 *     <extension base="<urn:mtconnect.org:MTConnectAssets:1.8>CommonSampleType">
 *     </extension>
 *   </simpleContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "AmperageType")
@XmlType(name = "AmperageType")
public class AmperageType
    extends CommonSampleType
{


}
