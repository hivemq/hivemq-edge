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
 * Cell of rotations about X, Y, and Z axes are expressed in A, B, and C
 *         respectively within a 3-dimensional vector.
 * 
 * <p>Java class for RotationCellType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="RotationCellType">
 *   <simpleContent>
 *     <restriction base="<urn:mtconnect.org:MTConnectStreams:2.0>TableCellType">
 *     </restriction>
 *   </simpleContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "RotationCellType")
@XmlType(name = "RotationCellType")
public class RotationCellType
    extends TableCellType
{


}
