//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_6;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * An XML container consisting of one or more types of filter XML elements.
 * 
 * <p>Java class for FiltersType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="FiltersType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="Filter" type="{urn:mtconnect.org:MTConnectDevices:1.6}DataItemFilterType" maxOccurs="unbounded"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "FiltersType")
@XmlType(name = "FiltersType", propOrder = {
    "filter"
})
public class FiltersType {

    /**
     * filter provides a means to control when an agent records updated
     *             information for a data item.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Filter")
    @XmlElement(name = "Filter", required = true)
    protected List<DataItemFilterType> filter;

    /**
     * filter provides a means to control when an agent records updated
     *             information for a data item.
     * 
     * Gets the value of the filter property.
     * 
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the filter property.</p>
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * </p>
     * <pre>
     * getFilter().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DataItemFilterType }
     * </p>
     * 
     * 
     * @return
     *     The value of the filter property.
     */
    public List<DataItemFilterType> getFilter() {
        if (filter == null) {
            filter = new ArrayList<>();
        }
        return this.filter;
    }

}
