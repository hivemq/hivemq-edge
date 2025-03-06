//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_2;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;


/**
 * The abstract waveform
 * 
 * <p>Java class for AbsTimeSeriesType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="AbsTimeSeriesType">
 *   <simpleContent>
 *     <extension base="<urn:mtconnect.org:MTConnectStreams:1.2>SampleType">
 *       <attribute name="sampleCount" use="required" type="{urn:mtconnect.org:MTConnectStreams:1.2}CountValueType" />
 *     </extension>
 *   </simpleContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "AbsTimeSeriesType")
@XmlType(name = "AbsTimeSeriesType")
@XmlSeeAlso({
    TimeSeriesType.class
})
public abstract class AbsTimeSeriesType
    extends SampleType
{

    @com.fasterxml.jackson.annotation.JsonProperty(value = "sampleCount")
    @XmlAttribute(name = "sampleCount", required = true)
    protected String sampleCount;

    /**
     * Gets the value of the sampleCount property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSampleCount() {
        return sampleCount;
    }

    /**
     * Sets the value of the sampleCount property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSampleCount(String value) {
        this.sampleCount = value;
    }

}
