//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_2_1;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlType;


/**
 * A collection of conditions
 * 
 * <p>Java class for ConditionListType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="ConditionListType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element ref="{urn:mtconnect.org:MTConnectStreams:2.1}Condition" maxOccurs="unbounded"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "ConditionListType")
@XmlType(name = "ConditionListType", propOrder = {
    "condition"
})
public class ConditionListType {

    /**
     * Description
     * 
     */
    @XmlElementRef(name = "Condition", namespace = "urn:mtconnect.org:MTConnectStreams:2.1", type = JAXBElement.class)
    protected List<JAXBElement<? extends ConditionType>> condition;

    /**
     * Description
     * 
     * Gets the value of the condition property.
     * 
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the condition property.</p>
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * </p>
     * <pre>
     * getCondition().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link ConditionType }{@code >}
     * {@link JAXBElement }{@code <}{@link FaultType }{@code >}
     * {@link JAXBElement }{@code <}{@link NormalType }{@code >}
     * {@link JAXBElement }{@code <}{@link UnavailableType }{@code >}
     * {@link JAXBElement }{@code <}{@link WarningType }{@code >}
     * </p>
     * 
     * 
     * @return
     *     The value of the condition property.
     */
    public List<JAXBElement<? extends ConditionType>> getCondition() {
        if (condition == null) {
            condition = new ArrayList<>();
        }
        return this.condition;
    }

}
