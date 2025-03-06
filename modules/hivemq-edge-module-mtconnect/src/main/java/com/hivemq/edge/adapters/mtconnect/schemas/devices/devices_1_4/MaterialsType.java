//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_4;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;


/**
 * materials is an XML container that provides information about materials
 * 			or other items consumed or used by the piece of equipment for production
 * 			of parts, materials, or other types of goods.
 * 
 * <p>Java class for MaterialsType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="MaterialsType">
 *   <complexContent>
 *     <extension base="{urn:mtconnect.org:MTConnectDevices:1.4}ResourcesType">
 *     </extension>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "MaterialsType")
@XmlType(name = "MaterialsType")
@XmlSeeAlso({
    StockType.class
})
public class MaterialsType
    extends ResourcesType
{


}
