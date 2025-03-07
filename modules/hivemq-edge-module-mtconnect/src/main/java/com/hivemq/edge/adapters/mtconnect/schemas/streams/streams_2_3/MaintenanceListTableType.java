//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_2_3;

import java.math.BigInteger;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;


/**
 * Table of {{def(EventEnum::MAINTENANCE_LIST)}} If
 *         {{property(MaintenanceList::result::Interval)}} `key` is not provided,
 *         it is assumed `ABSOLUTE`. If
 *         {{property(MaintenanceList::result::Direction)}} `key` is not provided,
 *         it is assumed `UP`. If {{property(MaintenanceList::result::Units)}}
 *         `key` is not provided, it is assumed to be `COUNT`.
 * 
 * <p>Java class for MaintenanceListTableType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="MaintenanceListTableType">
 *   <complexContent>
 *     <extension base="{urn:mtconnect.org:MTConnectStreams:2.3}EventType">
 *       <sequence>
 *         <element name="Entry" type="{urn:mtconnect.org:MTConnectStreams:2.3}MaintenanceListTableEntryType" maxOccurs="unbounded" minOccurs="0"/>
 *       </sequence>
 *       <attribute name="count" use="required" type="{urn:mtconnect.org:MTConnectStreams:2.3}CountValueType" />
 *     </extension>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "MaintenanceListTableType")
@XmlType(name = "MaintenanceListTableType")
public class MaintenanceListTableType
    extends EventType
{

    /**
     * The number of entries
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "count")
    @XmlAttribute(name = "count", required = true)
    protected BigInteger count;

    /**
     * The number of entries
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getCount() {
        return count;
    }

    /**
     * Sets the value of the count property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     * @see #getCount()
     */
    public void setCount(BigInteger value) {
        this.count = value;
    }

}
