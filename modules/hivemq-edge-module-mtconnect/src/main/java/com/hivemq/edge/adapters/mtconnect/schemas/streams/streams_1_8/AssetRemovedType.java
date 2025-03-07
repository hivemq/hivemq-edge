//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_8;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;


/**
 * An {{term(MTConnect Asset)}} has been removed. The value **MUST** be the
 *         {{property(assetId)}} of the {{block(Asset)}} that has been removed.
 * 
 * <p>Java class for AssetRemovedType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="AssetRemovedType">
 *   <simpleContent>
 *     <extension base="<urn:mtconnect.org:MTConnectStreams:1.8>StringEventType">
 *       <attribute name="assetType" type="{urn:mtconnect.org:MTConnectStreams:1.8}AssetAttrTypeType" />
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
    @XmlAttribute(name = "assetType")
    protected String assetType;

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

}
