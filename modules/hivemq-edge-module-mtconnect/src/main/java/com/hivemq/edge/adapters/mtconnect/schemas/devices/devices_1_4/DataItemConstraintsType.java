//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_4;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * A set of limits for a data item
 * 
 * <p>Java class for DataItemConstraintsType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="DataItemConstraintsType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <choice minOccurs="0">
 *           <sequence>
 *             <element name="Value" type="{urn:mtconnect.org:MTConnectDevices:1.4}DataItemValueElementType" maxOccurs="unbounded"/>
 *           </sequence>
 *           <sequence>
 *             <element name="Minimum" type="{urn:mtconnect.org:MTConnectDevices:1.4}DataItemNumericValueType" minOccurs="0"/>
 *             <element name="Maximum" type="{urn:mtconnect.org:MTConnectDevices:1.4}DataItemNumericValueType" minOccurs="0"/>
 *             <element name="Nominal" type="{urn:mtconnect.org:MTConnectDevices:1.4}DataItemNumericValueType" minOccurs="0"/>
 *           </sequence>
 *         </choice>
 *         <element name="Filter" type="{urn:mtconnect.org:MTConnectDevices:1.4}DataItemFilterType" minOccurs="0"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "DataItemConstraintsType")
@XmlType(name = "DataItemConstraintsType", propOrder = {
    "value",
    "minimum",
    "maximum",
    "nominal",
    "filter"
})
public class DataItemConstraintsType {

    /**
     * A possible value for this data item. Used for controlled
     *                 vocabularies.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Value")
    @XmlElement(name = "Value")
    protected List<DataItemValueElementType> value;
    /**
     * A minimum value for this data item.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Minimum")
    @XmlElement(name = "Minimum")
    protected Float minimum;
    /**
     * A maximum value for this data item.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Maximum")
    @XmlElement(name = "Maximum")
    protected Float maximum;
    /**
     * A nominal value for this data item.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Nominal")
    @XmlElement(name = "Nominal")
    protected Float nominal;
    /**
     * DEPRECATED: A limit on the amount of data by specifying the minimal
     *             delta required.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Filter")
    @XmlElement(name = "Filter")
    protected DataItemFilterType filter;

    /**
     * A possible value for this data item. Used for controlled
     *                 vocabularies.
     * 
     * Gets the value of the value property.
     * 
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the value property.</p>
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * </p>
     * <pre>
     * getValue().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DataItemValueElementType }
     * </p>
     * 
     * 
     * @return
     *     The value of the value property.
     */
    public List<DataItemValueElementType> getValue() {
        if (value == null) {
            value = new ArrayList<>();
        }
        return this.value;
    }

    /**
     * A minimum value for this data item.
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public Float getMinimum() {
        return minimum;
    }

    /**
     * Sets the value of the minimum property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     * @see #getMinimum()
     */
    public void setMinimum(Float value) {
        this.minimum = value;
    }

    /**
     * A maximum value for this data item.
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public Float getMaximum() {
        return maximum;
    }

    /**
     * Sets the value of the maximum property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     * @see #getMaximum()
     */
    public void setMaximum(Float value) {
        this.maximum = value;
    }

    /**
     * A nominal value for this data item.
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public Float getNominal() {
        return nominal;
    }

    /**
     * Sets the value of the nominal property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     * @see #getNominal()
     */
    public void setNominal(Float value) {
        this.nominal = value;
    }

    /**
     * DEPRECATED: A limit on the amount of data by specifying the minimal
     *             delta required.
     * 
     * @return
     *     possible object is
     *     {@link DataItemFilterType }
     *     
     */
    public DataItemFilterType getFilter() {
        return filter;
    }

    /**
     * Sets the value of the filter property.
     * 
     * @param value
     *     allowed object is
     *     {@link DataItemFilterType }
     *     
     * @see #getFilter()
     */
    public void setFilter(DataItemFilterType value) {
        this.filter = value;
    }

}
