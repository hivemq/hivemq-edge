//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_2;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * A collection of channel calibration data
 * 
 * <p>Java class for ChannelsType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="ChannelsType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="Channel" type="{urn:mtconnect.org:MTConnectDevices:1.2}ChannelType" maxOccurs="unbounded"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "ChannelsType")
@XmlType(name = "ChannelsType", propOrder = {
    "channel"
})
public class ChannelsType {

    /**
     * A calabration channel
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Channel")
    @XmlElement(name = "Channel", required = true)
    protected List<ChannelType> channel;

    /**
     * A calabration channel
     * 
     * Gets the value of the channel property.
     * 
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the channel property.</p>
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * </p>
     * <pre>
     * getChannel().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ChannelType }
     * </p>
     * 
     * 
     * @return
     *     The value of the channel property.
     */
    public List<ChannelType> getChannel() {
        if (channel == null) {
            channel = new ArrayList<>();
        }
        return this.channel;
    }

}
