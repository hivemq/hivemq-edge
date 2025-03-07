//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_2_2;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * Compositions Model
 * 
 * <p>Java class for CompositionsType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="CompositionsType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="Composition" type="{urn:mtconnect.org:MTConnectDevices:2.2}CompositionType" maxOccurs="unbounded"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "CompositionsType")
@XmlType(name = "CompositionsType", propOrder = {
    "composition"
})
public class CompositionsType {

    /**
     * Description
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Composition")
    @XmlElement(name = "Composition", required = true)
    protected List<CompositionType> composition;

    /**
     * Description
     * 
     * Gets the value of the composition property.
     * 
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the composition property.</p>
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * </p>
     * <pre>
     * getComposition().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CompositionType }
     * </p>
     * 
     * 
     * @return
     *     The value of the composition property.
     */
    public List<CompositionType> getComposition() {
        if (composition == null) {
            composition = new ArrayList<>();
        }
        return this.composition;
    }

}
