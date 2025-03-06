//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_2;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * The number of assets by type
 * 
 * <p>Java class for AssetCountsType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="AssetCountsType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="AssetCount" type="{urn:mtconnect.org:MTConnectDevices:1.2}AssetCountType" maxOccurs="unbounded"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "AssetCountsType")
@XmlType(name = "AssetCountsType", propOrder = {
    "assetCount"
})
public class AssetCountsType {

    /**
     * The number of assets for a given type
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "AssetCount")
    @XmlElement(name = "AssetCount", required = true)
    protected List<AssetCountType> assetCount;

    /**
     * The number of assets for a given type
     * 
     * Gets the value of the assetCount property.
     * 
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the assetCount property.</p>
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * </p>
     * <pre>
     * getAssetCount().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AssetCountType }
     * </p>
     * 
     * 
     * @return
     *     The value of the assetCount property.
     */
    public List<AssetCountType> getAssetCount() {
        if (assetCount == null) {
            assetCount = new ArrayList<>();
        }
        return this.assetCount;
    }

}
