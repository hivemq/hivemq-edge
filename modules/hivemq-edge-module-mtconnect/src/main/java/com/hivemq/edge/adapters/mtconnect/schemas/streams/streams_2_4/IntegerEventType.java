//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_2_4;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;


/**
 * An event with an integer value
 * 
 * <p>Java class for IntegerEventType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="IntegerEventType">
 *   <simpleContent>
 *     <restriction base="<urn:mtconnect.org:MTConnectStreams:2.4>EventType">
 *     </restriction>
 *   </simpleContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "IntegerEventType")
@XmlType(name = "IntegerEventType")
@XmlSeeAlso({
    BlockCountType.class,
    LineNumberType.class,
    MaterialLayerType.class,
    ProgramNestLevelType.class,
    ActivationCountType.class,
    DeactivationCountType.class,
    TransferCountType.class,
    LoadCountType.class,
    UnloadCountType.class,
    CycleCountType.class,
    AssetCountType.class,
    NetworkPortType.class,
    PartCountType.class
})
public class IntegerEventType
    extends EventType
{


}
