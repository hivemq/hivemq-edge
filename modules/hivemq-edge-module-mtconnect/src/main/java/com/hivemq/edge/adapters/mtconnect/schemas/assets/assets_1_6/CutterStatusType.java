//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_1_6;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * The status of this assembly.
 * 
 * <p>Java class for CutterStatusType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="CutterStatusType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="Status" type="{urn:mtconnect.org:MTConnectAssets:1.6}CutterStatusValueType" maxOccurs="unbounded"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "CutterStatusType")
@XmlType(name = "CutterStatusType", propOrder = {
    "status"
})
public class CutterStatusType {

    /**
     * The status of the Cutting Tool.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Status")
    @XmlElement(name = "Status", required = true)
    @XmlSchemaType(name = "string")
    protected List<CutterStatusValueType> status;

    /**
     * The status of the Cutting Tool.
     * 
     * Gets the value of the status property.
     * 
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the status property.</p>
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * </p>
     * <pre>
     * getStatus().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CutterStatusValueType }
     * </p>
     * 
     * 
     * @return
     *     The value of the status property.
     */
    public List<CutterStatusValueType> getStatus() {
        if (status == null) {
            status = new ArrayList<>();
        }
        return this.status;
    }

}
