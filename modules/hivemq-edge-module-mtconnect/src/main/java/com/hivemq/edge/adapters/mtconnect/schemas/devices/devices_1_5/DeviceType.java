//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_5;

import java.math.BigInteger;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;


/**
 * The primary container element for each piece of equipment. device is
 *         organized within the devices container.
 * 
 * <p>Java class for DeviceType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="DeviceType">
 *   <complexContent>
 *     <extension base="{urn:mtconnect.org:MTConnectDevices:1.5}ComponentType">
 *       <attribute name="iso841Class" type="{urn:mtconnect.org:MTConnectDevices:1.5}Iso841ClassType" />
 *       <attribute name="uuid" use="required" type="{urn:mtconnect.org:MTConnectDevices:1.5}UuidType" />
 *       <attribute name="name" use="required" type="{urn:mtconnect.org:MTConnectDevices:1.5}NameType" />
 *     </extension>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "DeviceType")
@XmlType(name = "DeviceType")
public class DeviceType
    extends ComponentType
{

    /**
     * DEPRECATED in MTConnect Version 1.1.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "iso841Class")
    @XmlAttribute(name = "iso841Class")
    protected BigInteger iso841Class;
    /**
     * The unique identifier for an XML element.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "uuid")
    @XmlAttribute(name = "uuid", required = true)
    protected String uuid;
    /**
     * The name of an element or a piece of equipment.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "name")
    @XmlAttribute(name = "name", required = true)
    protected String name;

    /**
     * DEPRECATED in MTConnect Version 1.1.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getIso841Class() {
        return iso841Class;
    }

    /**
     * Sets the value of the iso841Class property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     * @see #getIso841Class()
     */
    public void setIso841Class(BigInteger value) {
        this.iso841Class = value;
    }

    /**
     * The unique identifier for an XML element.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * Sets the value of the uuid property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getUuid()
     */
    public void setUuid(String value) {
        this.uuid = value;
    }

    /**
     * The name of an element or a piece of equipment.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getName()
     */
    public void setName(String value) {
        this.name = value;
    }

}
