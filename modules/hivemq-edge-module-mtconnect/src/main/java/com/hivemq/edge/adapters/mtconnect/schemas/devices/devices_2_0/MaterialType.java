//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_2_0;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;


/**
 * {{block(Resource)}} composed of material that is consumed or used by the
 *         piece of equipment for production of parts, materials, or other types of
 *         goods.
 * 
 * <p>Java class for MaterialType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="MaterialType">
 *   <complexContent>
 *     <extension base="{urn:mtconnect.org:MTConnectDevices:2.0}ResourceType">
 *     </extension>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "MaterialType")
@XmlType(name = "MaterialType")
@XmlSeeAlso({
    StockType.class
})
public class MaterialType
    extends ResourceType
{


}
