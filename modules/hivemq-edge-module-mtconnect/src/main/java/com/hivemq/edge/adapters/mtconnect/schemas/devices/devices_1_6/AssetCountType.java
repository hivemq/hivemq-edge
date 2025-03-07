//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_6;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;


/**
 * The total number of mtconnect asset in anagent.
 * 
 * <p>Java class for AssetCountType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="AssetCountType">
 *   <simpleContent>
 *     <extension base="<urn:mtconnect.org:MTConnectDevices:1.6>AssetCountValueType">
 *       <attribute name="assetType" use="required" type="{urn:mtconnect.org:MTConnectDevices:1.6}AssetAttrTypeType" />
 *     </extension>
 *   </simpleContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "AssetCountType")
@XmlType(name = "AssetCountType", propOrder = {
    "value"
})
public class AssetCountType {

    /**
     * The number of assets
     * 
     */
    @XmlValue
    protected long value;
    /**
     * The type of asset that was updated.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "assetType")
    @XmlAttribute(name = "assetType", required = true)
    protected String assetType;

    /**
     * The number of assets
     * 
     */
    public long getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     */
    public void setValue(long value) {
        this.value = value;
    }

    /**
     * The type of asset that was updated.
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
