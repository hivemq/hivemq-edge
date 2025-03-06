//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_1;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;


/**
 * The descriptive information for this component. This can be manufacturer
 *         specific
 * 
 * <p>Java class for ComponentDescriptionType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="ComponentDescriptionType">
 *   <simpleContent>
 *     <extension base="<urn:mtconnect.org:MTConnectDevices:1.1>DescriptionTextType">
 *       <attribute name="manufacturer" type="{urn:mtconnect.org:MTConnectDevices:1.1}NameType" />
 *       <attribute name="serialNumber" type="{urn:mtconnect.org:MTConnectDevices:1.1}SerialNumberType" />
 *       <attribute name="station" type="{urn:mtconnect.org:MTConnectDevices:1.1}StationType" />
 *     </extension>
 *   </simpleContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "ComponentDescriptionType")
@XmlType(name = "ComponentDescriptionType", propOrder = {
    "value"
})
public class ComponentDescriptionType {

    /**
     * A description
     * 
     */
    @XmlValue
    protected String value;
    @com.fasterxml.jackson.annotation.JsonProperty(value = "manufacturer")
    @XmlAttribute(name = "manufacturer")
    protected String manufacturer;
    @com.fasterxml.jackson.annotation.JsonProperty(value = "serialNumber")
    @XmlAttribute(name = "serialNumber")
    protected String serialNumber;
    @com.fasterxml.jackson.annotation.JsonProperty(value = "station")
    @XmlAttribute(name = "station")
    protected String station;

    /**
     * A description
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getValue()
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the value of the manufacturer property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getManufacturer() {
        return manufacturer;
    }

    /**
     * Sets the value of the manufacturer property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setManufacturer(String value) {
        this.manufacturer = value;
    }

    /**
     * Gets the value of the serialNumber property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSerialNumber() {
        return serialNumber;
    }

    /**
     * Sets the value of the serialNumber property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSerialNumber(String value) {
        this.serialNumber = value;
    }

    /**
     * Gets the value of the station property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStation() {
        return station;
    }

    /**
     * Sets the value of the station property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStation(String value) {
        this.station = value;
    }

}
