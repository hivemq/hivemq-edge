//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_2_3;

import java.math.BigInteger;
import javax.xml.datatype.XMLGregorianCalendar;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * Description
 * 
 * <p>Java class for SampleType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="SampleType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <attGroup ref="{urn:mtconnect.org:MTConnectStreams:2.3}ObservationType"/>
 *       <attribute name="sampleRate" type="{urn:mtconnect.org:MTConnectStreams:2.3}SampleRateType" />
 *       <attribute name="resetTriggered" type="{urn:mtconnect.org:MTConnectStreams:2.3}DataItemResetValueType" />
 *       <attribute name="statistic" type="{urn:mtconnect.org:MTConnectStreams:2.3}DataItemStatisticsType" />
 *       <attribute name="duration" type="{urn:mtconnect.org:MTConnectStreams:2.3}DurationTimeType" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "SampleType")
@XmlType(name = "SampleType", propOrder = {
    "content"
})
@XmlSeeAlso({
    CommonSampleType.class,
    AbsTimeSeriesType.class,
    ThreeSpaceSampleType.class
})
public abstract class SampleType {

    /**
     * Description
     * 
     */
    @XmlValue
    protected String content;
    /**
     * The rate the waveform was sampled at, default back to the value given
     *           in the data item
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "sampleRate")
    @XmlAttribute(name = "sampleRate")
    protected Float sampleRate;
    /**
     * An optional indicator that the event or sample was reset
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "resetTriggered")
    @XmlAttribute(name = "resetTriggered")
    protected String resetTriggered;
    /**
     * The statistical operation on this data
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "statistic")
    @XmlAttribute(name = "statistic")
    protected String statistic;
    /**
     * The number of seconds since the reset of the statistic
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "duration")
    @XmlAttribute(name = "duration")
    protected Float duration;
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
     * identifier of the maintenance activity.
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
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String compositionId;

    /**
     * Description
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the value of the content property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getContent()
     */
    public void setContent(String value) {
        this.content = value;
    }

    /**
     * The rate the waveform was sampled at, default back to the value given
     *           in the data item
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
     * @see #getSampleRate()
     */
    public void setSampleRate(Float value) {
        this.sampleRate = value;
    }

    /**
     * An optional indicator that the event or sample was reset
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getResetTriggered() {
        return resetTriggered;
    }

    /**
     * Sets the value of the resetTriggered property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getResetTriggered()
     */
    public void setResetTriggered(String value) {
        this.resetTriggered = value;
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
     * The number of seconds since the reset of the statistic
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public Float getDuration() {
        return duration;
    }

    /**
     * Sets the value of the duration property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     * @see #getDuration()
     */
    public void setDuration(Float value) {
        this.duration = value;
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
     * identifier of the maintenance activity.
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
     * @see #getCompositionId()
     */
    public void setCompositionId(String value) {
        this.compositionId = value;
    }

}
