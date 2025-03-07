//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_2_3;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * root entity of an {{term(MTConnectDevices Response Document)}} that
 *         contains the {{term(Device Information Model)}} of one or more
 *         {{block(Device)}} entities.
 *         ![MTConnectDevices](figures/MTConnectDevices.png
 *         "MTConnectDevices"){: width="0.8"} > Note:
 *         Additional properties of {{block(MTConnectDevices)}} **MAY** be defined
 *         for schema and namespace declaration. See {{sect(Schema and Namespace
 *         Declaration Information)}} for an {{term(XML)}} example.
 * 
 * <p>Java class for MTConnectDevicesType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="MTConnectDevicesType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <all>
 *         <element name="Header" type="{urn:mtconnect.org:MTConnectDevices:2.3}HeaderType"/>
 *         <element name="Devices" type="{urn:mtconnect.org:MTConnectDevices:2.3}DevicesType"/>
 *       </all>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "MTConnectDevicesType")
@XmlType(name = "MTConnectDevicesType", propOrder = {

})
public class MTConnectDevicesType {

    /**
     * provides information from an {{term(agent)}} defining version
     *             information, storage capacity, and parameters associated with the
     *             data management within the {{term(agent)}}.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Header")
    @XmlElement(name = "Header", required = true)
    protected HeaderType header;
    /**
     * This section provides semantic information for the {{block(Device)}}
     *             types.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Devices")
    @XmlElement(name = "Devices", required = true)
    protected DevicesType devices;

    /**
     * provides information from an {{term(agent)}} defining version
     *             information, storage capacity, and parameters associated with the
     *             data management within the {{term(agent)}}.
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
     * This section provides semantic information for the {{block(Device)}}
     *             types.
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
