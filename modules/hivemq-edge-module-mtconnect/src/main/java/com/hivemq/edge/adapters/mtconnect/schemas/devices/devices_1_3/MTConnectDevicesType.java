//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_3;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * The root node for MTConnect
 * 
 * <p>Java class for MTConnectDevicesType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="MTConnectDevicesType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="Header" type="{urn:mtconnect.org:MTConnectDevices:1.3}HeaderType"/>
 *         <element name="Devices" type="{urn:mtconnect.org:MTConnectDevices:1.3}DevicesType"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MTConnectDevicesType", propOrder = {
    "header",
    "devices"
})
public class MTConnectDevicesType {

    /**
     * Protocol dependent information
     * 
     */
    @XmlElement(name = "Header", required = true)
    protected HeaderType header;
    /**
     * The equipment
     * 
     */
    @XmlElement(name = "Devices", required = true)
    protected DevicesType devices;

    /**
     * Protocol dependent information
     * 
     * @return
     *     possible object is
     *     {@link HeaderType }
     *     
     */
    public HeaderType getHeader() {
        return header;
    }

    /**
     * Sets the value of the header property.
     * 
     * @param value
     *     allowed object is
     *     {@link HeaderType }
     *     
     * @see #getHeader()
     */
    public void setHeader(HeaderType value) {
        this.header = value;
    }

    /**
     * The equipment
     * 
     * @return
     *     possible object is
     *     {@link DevicesType }
     *     
     */
    public DevicesType getDevices() {
        return devices;
    }

    /**
     * Sets the value of the devices property.
     * 
     * @param value
     *     allowed object is
     *     {@link DevicesType }
     *     
     * @see #getDevices()
     */
    public void setDevices(DevicesType value) {
        this.devices = value;
    }

}
