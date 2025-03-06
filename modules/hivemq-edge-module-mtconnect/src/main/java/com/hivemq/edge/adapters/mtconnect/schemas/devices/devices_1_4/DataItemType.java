//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_4;

import java.math.BigInteger;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * A abstract measurement
 * 
 * <p>Java class for DataItemType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="DataItemType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="Source" type="{urn:mtconnect.org:MTConnectDevices:1.4}DataItemSourceType" minOccurs="0"/>
 *         <element name="Constraints" type="{urn:mtconnect.org:MTConnectDevices:1.4}DataItemConstraintsType" minOccurs="0"/>
 *         <element name="Filters" type="{urn:mtconnect.org:MTConnectDevices:1.4}FiltersType" minOccurs="0"/>
 *         <element name="InitialValue" type="{urn:mtconnect.org:MTConnectDevices:1.4}DataItemNumericValueType" minOccurs="0"/>
 *         <element name="ResetTrigger" type="{urn:mtconnect.org:MTConnectDevices:1.4}DataItemResetValueType" minOccurs="0"/>
 *       </sequence>
 *       <attribute name="name" type="{urn:mtconnect.org:MTConnectDevices:1.4}NameType" />
 *       <attribute name="id" use="required" type="{urn:mtconnect.org:MTConnectDevices:1.4}IDType" />
 *       <attribute name="type" use="required" type="{urn:mtconnect.org:MTConnectDevices:1.4}DataItemEnumType" />
 *       <attribute name="subType" type="{urn:mtconnect.org:MTConnectDevices:1.4}DataItemSubEnumType" />
 *       <attribute name="statistic" type="{urn:mtconnect.org:MTConnectDevices:1.4}DataItemStatisticsType" />
 *       <attribute name="units" type="{urn:mtconnect.org:MTConnectDevices:1.4}UnitsType" />
 *       <attribute name="nativeUnits" type="{urn:mtconnect.org:MTConnectDevices:1.4}NativeUnitsType" />
 *       <attribute name="nativeScale" type="{urn:mtconnect.org:MTConnectDevices:1.4}NativeScaleType" />
 *       <attribute name="category" use="required" type="{urn:mtconnect.org:MTConnectDevices:1.4}CategoryType" />
 *       <attribute name="coordinateSystem" type="{urn:mtconnect.org:MTConnectDevices:1.4}CoordinateSystemType" />
 *       <attribute name="compositionId" type="{urn:mtconnect.org:MTConnectDevices:1.4}CompositionIdType" />
 *       <attribute name="sampleRate" type="{urn:mtconnect.org:MTConnectDevices:1.4}DataItemSampleRateType" />
 *       <attribute name="representation" type="{urn:mtconnect.org:MTConnectDevices:1.4}RepresentationType" default="VALUE" />
 *       <attribute name="significantDigits" type="{urn:mtconnect.org:MTConnectDevices:1.4}SignificantDigitsValueType" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "DataItemType")
@XmlType(name = "DataItemType", propOrder = {
    "source",
    "constraints",
    "filters",
    "initialValue",
    "resetTrigger"
})
public class DataItemType {

    /**
     * Additional information about the component, channel, register,
     *             etc... that collects the data.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Source")
    @XmlElement(name = "Source")
    protected DataItemSourceType source;
    /**
     * Limits on the set of possible values
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Constraints")
    @XmlElement(name = "Constraints")
    protected DataItemConstraintsType constraints;
    /**
     * Limits on the set of possible values
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Filters")
    @XmlElement(name = "Filters")
    protected FiltersType filters;
    /**
     * The initial value for counters
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "InitialValue")
    @XmlElement(name = "InitialValue")
    protected Float initialValue;
    /**
     * The event that triggers the resetting of this counter
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "ResetTrigger")
    @XmlElement(name = "ResetTrigger")
    protected String resetTrigger;
    @com.fasterxml.jackson.annotation.JsonProperty(value = "name")
    @XmlAttribute(name = "name")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String name;
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id")
    @XmlAttribute(name = "id", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    protected String id;
    @com.fasterxml.jackson.annotation.JsonProperty(value = "type")
    @XmlAttribute(name = "type", required = true)
    protected String type;
    @com.fasterxml.jackson.annotation.JsonProperty(value = "subType")
    @XmlAttribute(name = "subType")
    protected String subType;
    @com.fasterxml.jackson.annotation.JsonProperty(value = "statistic")
    @XmlAttribute(name = "statistic")
    protected String statistic;
    @com.fasterxml.jackson.annotation.JsonProperty(value = "units")
    @XmlAttribute(name = "units")
    protected String units;
    @com.fasterxml.jackson.annotation.JsonProperty(value = "nativeUnits")
    @XmlAttribute(name = "nativeUnits")
    protected String nativeUnits;
    @com.fasterxml.jackson.annotation.JsonProperty(value = "nativeScale")
    @XmlAttribute(name = "nativeScale")
    protected Float nativeScale;
    @com.fasterxml.jackson.annotation.JsonProperty(value = "category")
    @XmlAttribute(name = "category", required = true)
    protected CategoryType category;
    @com.fasterxml.jackson.annotation.JsonProperty(value = "coordinateSystem")
    @XmlAttribute(name = "coordinateSystem")
    protected CoordinateSystemType coordinateSystem;
    @com.fasterxml.jackson.annotation.JsonProperty(value = "compositionId")
    @XmlAttribute(name = "compositionId")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String compositionId;
    @com.fasterxml.jackson.annotation.JsonProperty(value = "sampleRate")
    @XmlAttribute(name = "sampleRate")
    protected Float sampleRate;
    @com.fasterxml.jackson.annotation.JsonProperty(value = "representation")
    @XmlAttribute(name = "representation")
    protected RepresentationType representation;
    @com.fasterxml.jackson.annotation.JsonProperty(value = "significantDigits")
    @XmlAttribute(name = "significantDigits")
    protected BigInteger significantDigits;

    /**
     * Additional information about the component, channel, register,
     *             etc... that collects the data.
     * 
     * @return
     *     possible object is
     *     {@link DataItemSourceType }
     *     
     */
    public DataItemSourceType getSource() {
        return source;
    }

    /**
     * Sets the value of the source property.
     * 
     * @param value
     *     allowed object is
     *     {@link DataItemSourceType }
     *     
     * @see #getSource()
     */
    public void setSource(DataItemSourceType value) {
        this.source = value;
    }

    /**
     * Limits on the set of possible values
     * 
     * @return
     *     possible object is
     *     {@link DataItemConstraintsType }
     *     
     */
    public DataItemConstraintsType getConstraints() {
        return constraints;
    }

    /**
     * Sets the value of the constraints property.
     * 
     * @param value
     *     allowed object is
     *     {@link DataItemConstraintsType }
     *     
     * @see #getConstraints()
     */
    public void setConstraints(DataItemConstraintsType value) {
        this.constraints = value;
    }

    /**
     * Limits on the set of possible values
     * 
     * @return
     *     possible object is
     *     {@link FiltersType }
     *     
     */
    public FiltersType getFilters() {
        return filters;
    }

    /**
     * Sets the value of the filters property.
     * 
     * @param value
     *     allowed object is
     *     {@link FiltersType }
     *     
     * @see #getFilters()
     */
    public void setFilters(FiltersType value) {
        this.filters = value;
    }

    /**
     * The initial value for counters
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public Float getInitialValue() {
        return initialValue;
    }

    /**
     * Sets the value of the initialValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     * @see #getInitialValue()
     */
    public void setInitialValue(Float value) {
        this.initialValue = value;
    }

    /**
     * The event that triggers the resetting of this counter
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getResetTrigger() {
        return resetTrigger;
    }

    /**
     * Sets the value of the resetTrigger property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getResetTrigger()
     */
    public void setResetTrigger(String value) {
        this.resetTrigger = value;
    }

    /**
     * Gets the value of the name property.
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
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setType(String value) {
        this.type = value;
    }

    /**
     * Gets the value of the subType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSubType() {
        return subType;
    }

    /**
     * Sets the value of the subType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSubType(String value) {
        this.subType = value;
    }

    /**
     * Gets the value of the statistic property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStatistic() {
        return statistic;
    }

    /**
     * Sets the value of the statistic property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStatistic(String value) {
        this.statistic = value;
    }

    /**
     * Gets the value of the units property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUnits() {
        return units;
    }

    /**
     * Sets the value of the units property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUnits(String value) {
        this.units = value;
    }

    /**
     * Gets the value of the nativeUnits property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNativeUnits() {
        return nativeUnits;
    }

    /**
     * Sets the value of the nativeUnits property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNativeUnits(String value) {
        this.nativeUnits = value;
    }

    /**
     * Gets the value of the nativeScale property.
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public Float getNativeScale() {
        return nativeScale;
    }

    /**
     * Sets the value of the nativeScale property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     */
    public void setNativeScale(Float value) {
        this.nativeScale = value;
    }

    /**
     * Gets the value of the category property.
     * 
     * @return
     *     possible object is
     *     {@link CategoryType }
     *     
     */
    public CategoryType getCategory() {
        return category;
    }

    /**
     * Sets the value of the category property.
     * 
     * @param value
     *     allowed object is
     *     {@link CategoryType }
     *     
     */
    public void setCategory(CategoryType value) {
        this.category = value;
    }

    /**
     * Gets the value of the coordinateSystem property.
     * 
     * @return
     *     possible object is
     *     {@link CoordinateSystemType }
     *     
     */
    public CoordinateSystemType getCoordinateSystem() {
        return coordinateSystem;
    }

    /**
     * Sets the value of the coordinateSystem property.
     * 
     * @param value
     *     allowed object is
     *     {@link CoordinateSystemType }
     *     
     */
    public void setCoordinateSystem(CoordinateSystemType value) {
        this.coordinateSystem = value;
    }

    /**
     * Gets the value of the compositionId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCompositionId() {
        return compositionId;
    }

    /**
     * Sets the value of the compositionId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCompositionId(String value) {
        this.compositionId = value;
    }

    /**
     * Gets the value of the sampleRate property.
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public Float getSampleRate() {
        return sampleRate;
    }

    /**
     * Sets the value of the sampleRate property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     */
    public void setSampleRate(Float value) {
        this.sampleRate = value;
    }

    /**
     * Gets the value of the representation property.
     * 
     * @return
     *     possible object is
     *     {@link RepresentationType }
     *     
     */
    public RepresentationType getRepresentation() {
        if (representation == null) {
            return RepresentationType.VALUE;
        } else {
            return representation;
        }
    }

    /**
     * Sets the value of the representation property.
     * 
     * @param value
     *     allowed object is
     *     {@link RepresentationType }
     *     
     */
    public void setRepresentation(RepresentationType value) {
        this.representation = value;
    }

    /**
     * Gets the value of the significantDigits property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getSignificantDigits() {
        return significantDigits;
    }

    /**
     * Sets the value of the significantDigits property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setSignificantDigits(BigInteger value) {
        this.significantDigits = value;
    }

}
