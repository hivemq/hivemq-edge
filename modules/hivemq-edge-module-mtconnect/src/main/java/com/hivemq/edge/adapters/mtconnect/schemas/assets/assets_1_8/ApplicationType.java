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
 * Software or a program that is specific to the solution of an application
 *         problem. {{cite(ISO/IEC 20944-1:2013)}}
 * 
 * <p>Java class for ApplicationType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="ApplicationType">
 *   <simpleContent>
 *     <extension base="<urn:mtconnect.org:MTConnectAssets:1.8>StringEventType">
 *     </extension>
 *   </simpleContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "ApplicationType")
@XmlType(name = "ApplicationType")
public class ApplicationType
    extends StringEventType
{


}
