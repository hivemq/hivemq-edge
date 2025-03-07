//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_2_3;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * {{block(Filters)}} groups one or more {{block(Filter)}} entities
 *         associated with the {{block(DataItem)}}.
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
 *         <element name="Filter" type="{urn:mtconnect.org:MTConnectAssets:2.3}DataItemFilterType" maxOccurs="unbounded"/>
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
     * **DEPRECATED** in *MTConnect Version 1.4*. Moved to the
     *             {{block(Filters)}}. See {{package(Properties of DataItem)}}.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Filter")
    @XmlElement(name = "Filter", required = true)
    protected List<DataItemFilterType> filter;

    /**
     * **DEPRECATED** in *MTConnect Version 1.4*. Moved to the
     *             {{block(Filters)}}. See {{package(Properties of DataItem)}}.
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
