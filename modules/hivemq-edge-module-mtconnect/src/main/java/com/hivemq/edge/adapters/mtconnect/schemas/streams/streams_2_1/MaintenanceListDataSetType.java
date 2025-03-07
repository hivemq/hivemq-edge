//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_2_1;

import java.math.BigInteger;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;


/**
 * DataSet of {{def(EventEnum:MAINTENANCE_LIST)}} If the
 *         {{property(INTERVAL)}} {{property(key)}} is not provided, it is assumed
 *         `ABSOLUTE`. If the {{property(DIRECTION)}} {{property(key)}} is not
 *         provided, it is assumed `UP`. If the {{property(UNITS)}}
 *         {{property(key)}} is not provided, it is assumed to be `COUNT`.
 * 
 * <p>Java class for MaintenanceListDataSetType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="MaintenanceListDataSetType">
 *   <complexContent>
 *     <extension base="{urn:mtconnect.org:MTConnectStreams:2.1}EventType">
 *       <sequence>
 *         <element name="Entry" type="{urn:mtconnect.org:MTConnectStreams:2.1}MaintenanceListEntryType" maxOccurs="unbounded" minOccurs="0"/>
 *       </sequence>
 *       <attribute name="count" use="required" type="{urn:mtconnect.org:MTConnectStreams:2.1}CountValueType" />
 *     </extension>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "MaintenanceListDataSetType")
@XmlType(name = "MaintenanceListDataSetType")
public class MaintenanceListDataSetType
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
