//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_2_2;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * This section provides semantic information for the {{block(Device)}}
 *         types.
 * 
 * <p>Java class for DevicesType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="DevicesType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="Agent" type="{urn:mtconnect.org:MTConnectDevices:2.2}AgentType" minOccurs="0"/>
 *         <element name="Device" type="{urn:mtconnect.org:MTConnectDevices:2.2}DeviceType" maxOccurs="unbounded"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "DevicesType")
@XmlType(name = "DevicesType", propOrder = {
    "agent",
    "device"
})
public class DevicesType {

    /**
     * Description
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Agent")
    @XmlElement(name = "Agent")
    protected AgentType agent;
    /**
     * Description
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Device")
    @XmlElement(name = "Device", required = true)
    protected List<DeviceType> device;

    /**
     * Description
     * 
     * @return
     *     possible object is
     *     {@link AgentType }
     *     
     */
    public AgentType getAgent() {
        return agent;
    }

    /**
     * Sets the value of the agent property.
     * 
     * @param value
     *     allowed object is
     *     {@link AgentType }
     *     
     * @see #getAgent()
     */
    public void setAgent(AgentType value) {
        this.agent = value;
    }

    /**
     * Description
     * 
     * Gets the value of the device property.
     * 
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the device property.</p>
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * </p>
     * <pre>
     * getDevice().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DeviceType }
     * </p>
     * 
     * 
     * @return
     *     The value of the device property.
     */
    public List<DeviceType> getDevice() {
        if (device == null) {
            device = new ArrayList<>();
        }
        return this.device;
    }

}
