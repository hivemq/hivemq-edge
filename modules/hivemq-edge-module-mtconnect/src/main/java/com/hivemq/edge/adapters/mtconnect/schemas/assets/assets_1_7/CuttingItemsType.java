//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_1_7;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * {{block(CuttingItems)}} {{termplural(organize)}} {{block(CuttingItem)}}
 *         elements.
 * 
 * <p>Java class for CuttingItemsType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="CuttingItemsType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <all>
 *         <element name="CuttingItem" type="{urn:mtconnect.org:MTConnectAssets:1.7}CuttingItemType" maxOccurs="unbounded"/>
 *       </all>
 *       <attribute name="count" use="required" type="{urn:mtconnect.org:MTConnectAssets:1.7}EdgeCountType" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "CuttingItemsType")
@XmlType(name = "CuttingItemsType", propOrder = {

})
public class CuttingItemsType {

    /**
     * A {{block(CuttingItem)}} is the portion of the tool that physically
     *             removes the material from the workpiece by shear deformation.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "CuttingItem")
    @XmlElement(name = "CuttingItem", required = true)
    protected List<CuttingItemType> cuttingItem;
    /**
     * The number of edges
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "count")
    @XmlAttribute(name = "count", required = true)
    protected BigInteger count;

    /**
     * A {{block(CuttingItem)}} is the portion of the tool that physically
     *             removes the material from the workpiece by shear deformation.
     * 
     * Gets the value of the cuttingItem property.
     * 
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the cuttingItem property.</p>
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * </p>
     * <pre>
     * getCuttingItem().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CuttingItemType }
     * </p>
     * 
     * 
     * @return
     *     The value of the cuttingItem property.
     */
    public List<CuttingItemType> getCuttingItem() {
        if (cuttingItem == null) {
            cuttingItem = new ArrayList<>();
        }
        return this.cuttingItem;
    }

    /**
     * The number of edges
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
