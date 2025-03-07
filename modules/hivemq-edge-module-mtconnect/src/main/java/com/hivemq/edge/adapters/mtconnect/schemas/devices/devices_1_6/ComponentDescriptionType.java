//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_6;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlMixed;
import jakarta.xml.bind.annotation.XmlType;
import org.w3c.dom.Element;


/**
 * The descriptive information. This can be manufacturer specific
 * 
 * <p>Java class for ComponentDescriptionType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="ComponentDescriptionType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <any processContents='lax' maxOccurs="unbounded" minOccurs="0"/>
 *       </sequence>
 *       <attribute name="manufacturer" type="{urn:mtconnect.org:MTConnectDevices:1.6}NameType" />
 *       <attribute name="model" type="{urn:mtconnect.org:MTConnectDevices:1.6}ModelType" />
 *       <attribute name="serialNumber" type="{urn:mtconnect.org:MTConnectDevices:1.6}SerialNumberAttrType" />
 *       <attribute name="station" type="{urn:mtconnect.org:MTConnectDevices:1.6}StationType" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "ComponentDescriptionType")
@XmlType(name = "ComponentDescriptionType", propOrder = {
    "content"
})
public class ComponentDescriptionType {

    /**
     * The descriptive information. This can be manufacturer specific
     * 
     */
    @XmlMixed
    @XmlAnyElement(lax = true)
    protected List<Object> content;
    /**
     * The name of the manufacturer of the physical or logical part of a
     *           piece of equipment represented by an XML element.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "manufacturer")
    @XmlAttribute(name = "manufacturer")
    protected String manufacturer;
    /**
     * The model description of the physical part or logical function of a
     *           piece of equipment represented by this XML element.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "model")
    @XmlAttribute(name = "model")
    protected String model;
    /**
     * The serial number associated with a piece of equipment.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "serialNumber")
    @XmlAttribute(name = "serialNumber")
    protected String serialNumber;
    /**
     * The station where the physical part or logical function of a piece of
     *           equipment is located when it is part of a manufacturing unit or cell
     *           with multiple stations.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "station")
    @XmlAttribute(name = "station")
    protected String station;

    /**
     * The descriptive information. This can be manufacturer specific
     * 
     * Gets the value of the content property.
     * 
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the content property.</p>
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * </p>
     * <pre>
     * getContent().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Object }
     * {@link String }
     * {@link Element }
     * </p>
     * 
     * 
     * @return
     *     The value of the content property.
     */
    public List<Object> getContent() {
        if (content == null) {
            content = new ArrayList<>();
        }
        return this.content;
    }

    /**
     * The name of the manufacturer of the physical or logical part of a
     *           piece of equipment represented by an XML element.
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
     * @see #getManufacturer()
     */
    public void setManufacturer(String value) {
        this.manufacturer = value;
    }

    /**
     * The model description of the physical part or logical function of a
     *           piece of equipment represented by this XML element.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getModel() {
        return model;
    }

    /**
     * Sets the value of the model property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getModel()
     */
    public void setModel(String value) {
        this.model = value;
    }

    /**
     * The serial number associated with a piece of equipment.
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
     * @see #getSerialNumber()
     */
    public void setSerialNumber(String value) {
        this.serialNumber = value;
    }

    /**
     * The station where the physical part or logical function of a piece of
     *           equipment is located when it is part of a manufacturing unit or cell
     *           with multiple stations.
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
     * @see #getStation()
     */
    public void setStation(String value) {
        this.station = value;
    }

}
