//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_4;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * The root node for MTConnect
 * 
 * <p>Java class for MTConnectStreamsType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="MTConnectStreamsType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="Header" type="{urn:mtconnect.org:MTConnectStreams:1.4}HeaderType"/>
 *         <element name="Streams" type="{urn:mtconnect.org:MTConnectStreams:1.4}StreamsType"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "MTConnectStreamsType")
@XmlType(name = "MTConnectStreamsType", propOrder = {
    "header",
    "streams"
})
public class MTConnectStreamsType {

    /**
     * Protocol dependent information
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Header")
    @XmlElement(name = "Header", required = true)
    protected HeaderType header;
    /**
     * Streams of data for each piece of equipment
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Streams")
    @XmlElement(name = "Streams", required = true)
    protected StreamsType streams;

    /**
     * Protocol dependent information
     * 
     * @return
     *     possible object is
     *     {@link HeaderType }
     *     
     */
    public HeaderType getHeader() {
        return header;
    }

    /**
     * Sets the value of the header property.
     * 
     * @param value
     *     allowed object is
     *     {@link HeaderType }
     *     
     * @see #getHeader()
     */
    public void setHeader(HeaderType value) {
        this.header = value;
    }

    /**
     * Streams of data for each piece of equipment
     * 
     * @return
     *     possible object is
     *     {@link StreamsType }
     *     
     */
    public StreamsType getStreams() {
        return streams;
    }

    /**
     * Sets the value of the streams property.
     * 
     * @param value
     *     allowed object is
     *     {@link StreamsType }
     *     
     * @see #getStreams()
     */
    public void setStreams(StreamsType value) {
        this.streams = value;
    }

}
