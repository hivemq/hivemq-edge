//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_1_8;

import java.math.BigInteger;
import javax.xml.datatype.XMLGregorianCalendar;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlIDREF;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * An indicator of the ability of a piece of equipment or
 *         {{term(Component)}} to function to specification.
 * 
 * <p>Java class for ConditionType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="ConditionType">
 *   <simpleContent>
 *     <extension base="<urn:mtconnect.org:MTConnectAssets:1.8>ConditionDescriptionType">
 *       <attGroup ref="{urn:mtconnect.org:MTConnectAssets:1.8}ObservationType"/>
 *       <attribute name="type" use="required" type="{urn:mtconnect.org:MTConnectAssets:1.8}DataItemEnumType" />
 *       <attribute name="nativeCode" type="{urn:mtconnect.org:MTConnectAssets:1.8}NativeCodeType" />
 *       <attribute name="nativeSeverity" type="{urn:mtconnect.org:MTConnectAssets:1.8}NativeSeverityType" />
 *       <attribute name="qualifier" type="{urn:mtconnect.org:MTConnectAssets:1.8}QualifierType" />
 *       <attribute name="statistic" type="{urn:mtconnect.org:MTConnectAssets:1.8}DataItemStatisticsType" />
 *     </extension>
 *   </simpleContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "ConditionType")
@XmlType(name = "ConditionType", propOrder = {
    "value"
})
@XmlSeeAlso({
    UnavailableType.class,
    NormalType.class,
    WarningType.class,
    FaultType.class
})
public abstract class ConditionType {

    /**
     * The description of the Condition
     * 
     */
    @XmlValue
    protected String value;
    /**
     * The type of either a {{term(Structural Element)}} or a
     *               {{block(DataItem)}} being measured.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "type")
    @XmlAttribute(name = "type", required = true)
    protected String type;
    /**
     * The component specific Notifcation code
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "nativeCode")
    @XmlAttribute(name = "nativeCode")
    protected String nativeCode;
    /**
     * The component specific Notifcation code
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "nativeSeverity")
    @XmlAttribute(name = "nativeSeverity")
    protected String nativeSeverity;
    /**
     * An optional attribute that helps qualify the condition
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "qualifier")
    @XmlAttribute(name = "qualifier")
    protected QualifierType qualifier;
    /**
     * The statistical operation on this data
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "statistic")
    @XmlAttribute(name = "statistic")
    protected String statistic;
    /**
     * The events sequence number
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "sequence")
    @XmlAttribute(name = "sequence", required = true)
    protected BigInteger sequence;
    /**
     * The event subtype corresponding to the measurement subtype
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "subType")
    @XmlAttribute(name = "subType")
    protected String subType;
    /**
     * The time the event occurred or recorded
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "timestamp")
    @XmlAttribute(name = "timestamp", required = true)
    protected XMLGregorianCalendar timestamp;
    /**
     * The name of the event corresponding to the measurement
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "name")
    @XmlAttribute(name = "name")
    protected String name;
    /**
     * The unique identifier of the item being produced
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "dataItemId")
    @XmlAttribute(name = "dataItemId", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String dataItemId;
    /**
     * The identifier of the sub-element this result is in reference to
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "compositionId")
    @XmlAttribute(name = "compositionId")
    @XmlIDREF
    protected Object compositionId;

    /**
     * The description of the Condition
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
     * The type of either a {{term(Structural Element)}} or a
     *               {{block(DataItem)}} being measured.
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
     * @see #getType()
     */
    public void setType(String value) {
        this.type = value;
    }

    /**
     * The component specific Notifcation code
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNativeCode() {
        return nativeCode;
    }

    /**
     * Sets the value of the nativeCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getNativeCode()
     */
    public void setNativeCode(String value) {
        this.nativeCode = value;
    }

    /**
     * The component specific Notifcation code
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNativeSeverity() {
        return nativeSeverity;
    }

    /**
     * Sets the value of the nativeSeverity property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getNativeSeverity()
     */
    public void setNativeSeverity(String value) {
        this.nativeSeverity = value;
    }

    /**
     * An optional attribute that helps qualify the condition
     * 
     * @return
     *     possible object is
     *     {@link QualifierType }
     *     
     */
    public QualifierType getQualifier() {
        return qualifier;
    }

    /**
     * Sets the value of the qualifier property.
     * 
     * @param value
     *     allowed object is
     *     {@link QualifierType }
     *     
     * @see #getQualifier()
     */
    public void setQualifier(QualifierType value) {
        this.qualifier = value;
    }

    /**
     * The statistical operation on this data
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
     * @see #getStatistic()
     */
    public void setStatistic(String value) {
        this.statistic = value;
    }

    /**
     * The events sequence number
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getSequence() {
        return sequence;
    }

    /**
     * Sets the value of the sequence property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     * @see #getSequence()
     */
    public void setSequence(BigInteger value) {
        this.sequence = value;
    }

    /**
     * The event subtype corresponding to the measurement subtype
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
     * @see #getSubType()
     */
    public void setSubType(String value) {
        this.subType = value;
    }

    /**
     * The time the event occurred or recorded
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the value of the timestamp property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     * @see #getTimestamp()
     */
    public void setTimestamp(XMLGregorianCalendar value) {
        this.timestamp = value;
    }

    /**
     * The name of the event corresponding to the measurement
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

    /**
     * The unique identifier of the item being produced
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDataItemId() {
        return dataItemId;
    }

    /**
     * Sets the value of the dataItemId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getDataItemId()
     */
    public void setDataItemId(String value) {
        this.dataItemId = value;
    }

    /**
     * The identifier of the sub-element this result is in reference to
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getCompositionId() {
        return compositionId;
    }

    /**
     * Sets the value of the compositionId property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     * @see #getCompositionId()
     */
    public void setCompositionId(Object value) {
        this.compositionId = value;
    }

}
