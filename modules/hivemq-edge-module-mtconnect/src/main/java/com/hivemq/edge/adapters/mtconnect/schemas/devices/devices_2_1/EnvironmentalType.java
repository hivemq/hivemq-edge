//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_2_1;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;


/**
 * {{block(Auxiliary)}} that monitors, manages, or conditions the
 *         environment around or within a piece of equipment.
 * 
 * <p>Java class for EnvironmentalType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="EnvironmentalType">
 *   <complexContent>
 *     <extension base="{urn:mtconnect.org:MTConnectDevices:2.1}AuxiliaryType">
 *     </extension>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "EnvironmentalType")
@XmlType(name = "EnvironmentalType")
@XmlSeeAlso({
    HeatingType.class,
    VacuumType.class,
    CoolingType.class,
    PressureType.class
})
public class EnvironmentalType
    extends AuxiliaryType
{


}
