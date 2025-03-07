//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_2_3;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;


/**
 * {{def(EventEnum::DEVICE_REMOVED)}}
 * 
 * <p>Java class for DeviceRemovedType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="DeviceRemovedType">
 *   <simpleContent>
 *     <extension base="<urn:mtconnect.org:MTConnectAssets:2.3>StringEventType">
 *       <attribute name="hash" type="{urn:mtconnect.org:MTConnectAssets:2.3}HashType" />
 *     </extension>
 *   </simpleContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "DeviceRemovedType")
@XmlType(name = "DeviceRemovedType")
public class DeviceRemovedType
    extends StringEventType
{

    /**
     * condensed message digest from a secure one-way hash function.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "hash")
    @XmlAttribute(name = "hash")
    protected String hash;

    /**
     * condensed message digest from a secure one-way hash function.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getHash() {
        return hash;
    }

    /**
     * Sets the value of the hash property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getHash()
     */
    public void setHash(String value) {
        this.hash = value;
    }

}
