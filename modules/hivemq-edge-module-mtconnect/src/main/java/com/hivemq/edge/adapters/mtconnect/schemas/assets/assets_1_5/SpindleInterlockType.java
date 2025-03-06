//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_1_5;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * An indication of the status of the spindle for a piece of equipment when
 *         power has been removed and it is free to rotate.
 * 
 * <p>Java class for SpindleInterlockType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="SpindleInterlockType">
 *   <simpleContent>
 *     <restriction base="<urn:mtconnect.org:MTConnectAssets:1.5>EventType">
 *     </restriction>
 *   </simpleContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "SpindleInterlockType")
@XmlType(name = "SpindleInterlockType")
public class SpindleInterlockType
    extends EventType
{


}
