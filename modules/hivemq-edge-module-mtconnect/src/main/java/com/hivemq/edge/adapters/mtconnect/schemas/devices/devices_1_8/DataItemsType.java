//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_8;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for DataItemsType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="DataItemsType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <all>
 *         <element name="DataItem" type="{urn:mtconnect.org:MTConnectDevices:1.8}DataItemType" maxOccurs="unbounded"/>
 *       </all>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "DataItemsType")
@XmlType(name = "DataItemsType", propOrder = {

})
public class DataItemsType {

    /**
     * {{block(DataItem)}} describes a piece of information reported about
     *             a piece of equipment.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "DataItem")
    @XmlElement(name = "DataItem", required = true)
    protected List<DataItemType> dataItem;

    /**
     * {{block(DataItem)}} describes a piece of information reported about
     *             a piece of equipment.
     * 
     * Gets the value of the dataItem property.
     * 
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the dataItem property.</p>
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * </p>
     * <pre>
     * getDataItem().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DataItemType }
     * </p>
     * 
     * 
     * @return
     *     The value of the dataItem property.
     */
    public List<DataItemType> getDataItem() {
        if (dataItem == null) {
            dataItem = new ArrayList<>();
        }
        return this.dataItem;
    }

}
