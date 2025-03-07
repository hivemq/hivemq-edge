//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_2_4;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;


/**
 * {{def(EventEnum::ASSET_REMOVED)}}
 * 
 * <p>Java class for AssetRemovedType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="AssetRemovedType">
 *   <simpleContent>
 *     <extension base="<urn:mtconnect.org:MTConnectStreams:2.4>StringEventType">
 *       <attribute name="assetType" use="required" type="{urn:mtconnect.org:MTConnectStreams:2.4}AssetAttrTypeType" />
 *       <attribute name="hash" type="{urn:mtconnect.org:MTConnectStreams:2.4}HashType" />
 *     </extension>
 *   </simpleContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "AssetRemovedType")
@XmlType(name = "AssetRemovedType")
public class AssetRemovedType
    extends StringEventType
{

    /**
     * The type of asset
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "assetType")
    @XmlAttribute(name = "assetType", required = true)
    protected String assetType;
    /**
     * condensed message digest from a secure one-way hash function.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "hash")
    @XmlAttribute(name = "hash")
    protected String hash;

    /**
     * The type of asset
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAssetType() {
        return assetType;
    }

    /**
     * Sets the value of the assetType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getAssetType()
     */
    public void setAssetType(String value) {
        this.assetType = value;
    }

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
