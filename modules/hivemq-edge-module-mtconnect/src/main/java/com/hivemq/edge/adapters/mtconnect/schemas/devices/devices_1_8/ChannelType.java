//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_8;

import java.math.BigInteger;
import javax.xml.datatype.XMLGregorianCalendar;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * When {{block(Sensor)}} represents multiple {{termplural(sensing
 *         element)}}, each {{term(sensing element)}} is represented by a
 *         {{block(Channel)}} for the {{block(Sensor)}}.
 * 
 * <p>Java class for ChannelType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="ChannelType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <all>
 *         <element name="Description" type="{urn:mtconnect.org:MTConnectDevices:1.8}DescriptionTextType" minOccurs="0"/>
 *         <element name="CalibrationDate" type="{urn:mtconnect.org:MTConnectDevices:1.8}CalibrationDateType" minOccurs="0"/>
 *         <element name="NextCalibrationDate" type="{urn:mtconnect.org:MTConnectDevices:1.8}NextCalibrationDateType" minOccurs="0"/>
 *         <element name="CalibrationInitials" type="{urn:mtconnect.org:MTConnectDevices:1.8}CalibrationInitialsType" minOccurs="0"/>
 *       </all>
 *       <attribute name="number" use="required" type="{urn:mtconnect.org:MTConnectDevices:1.8}ChannelNumberType" />
 *       <attribute name="name" type="{urn:mtconnect.org:MTConnectDevices:1.8}NameType" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "ChannelType")
@XmlType(name = "ChannelType", propOrder = {

})
public class ChannelType {

    /**
     * The {{block(Description)}} of the {{block(Definition)}}. See
     *             {{block(Component)}} {{block(Description)}}.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Description")
    @XmlElement(name = "Description")
    protected String description;
    /**
     * Date upon which the {{term(sensor unit)}} was last calibrated to the
     *             {{term(sensor element)}}.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "CalibrationDate")
    @XmlElement(name = "CalibrationDate")
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar calibrationDate;
    /**
     * Date upon which the {{term(sensor element)}} is next scheduled to be
     *             calibrated with the {{term(sensor unit)}}.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "NextCalibrationDate")
    @XmlElement(name = "NextCalibrationDate")
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar nextCalibrationDate;
    /**
     * The initials of the person verifying the validity of the calibration
     *             data.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "CalibrationInitials")
    @XmlElement(name = "CalibrationInitials")
    protected String calibrationInitials;
    /**
     * The channel id
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "number")
    @XmlAttribute(name = "number", required = true)
    protected BigInteger number;
    /**
     * The channel name
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "name")
    @XmlAttribute(name = "name")
    protected String name;

    /**
     * The {{block(Description)}} of the {{block(Definition)}}. See
     *             {{block(Component)}} {{block(Description)}}.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getDescription()
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Date upon which the {{term(sensor unit)}} was last calibrated to the
     *             {{term(sensor element)}}.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getCalibrationDate() {
        return calibrationDate;
    }

    /**
     * Sets the value of the calibrationDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     * @see #getCalibrationDate()
     */
    public void setCalibrationDate(XMLGregorianCalendar value) {
        this.calibrationDate = value;
    }

    /**
     * Date upon which the {{term(sensor element)}} is next scheduled to be
     *             calibrated with the {{term(sensor unit)}}.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getNextCalibrationDate() {
        return nextCalibrationDate;
    }

    /**
     * Sets the value of the nextCalibrationDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     * @see #getNextCalibrationDate()
     */
    public void setNextCalibrationDate(XMLGregorianCalendar value) {
        this.nextCalibrationDate = value;
    }

    /**
     * The initials of the person verifying the validity of the calibration
     *             data.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCalibrationInitials() {
        return calibrationInitials;
    }

    /**
     * Sets the value of the calibrationInitials property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getCalibrationInitials()
     */
    public void setCalibrationInitials(String value) {
        this.calibrationInitials = value;
    }

    /**
     * The channel id
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getNumber() {
        return number;
    }

    /**
     * Sets the value of the number property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     * @see #getNumber()
     */
    public void setNumber(BigInteger value) {
        this.number = value;
    }

    /**
     * The channel name
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
