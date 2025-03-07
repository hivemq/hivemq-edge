//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.error.error_2_3;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * {{block(Errors)}} groups one or more {{block(Error)}} entities. See
 *         {{sect(Error)}}. > Note: When compatibility with Version 1.0.1 and
 *         earlier of the MTConnect Standard is required for an implementation, the
 *         {{term(MTConnectErrors Response Document)}} contains only a single
 *         {{block(Error)}} entity and the {{block(Errors)}} entity **MUST NOT**
 *         appear in the document.
 * 
 * <p>Java class for ErrorsType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="ErrorsType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="Error" type="{urn:mtconnect.org:MTConnectError:2.3}ErrorType" maxOccurs="unbounded"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "ErrorsType")
@XmlType(name = "ErrorsType", propOrder = {
    "error"
})
public class ErrorsType {

    /**
     * error encountered by an {{term(agent)}} when responding to a
     *             {{term(request)}}.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Error")
    @XmlElement(name = "Error", required = true)
    protected List<ErrorType> error;

    /**
     * error encountered by an {{term(agent)}} when responding to a
     *             {{term(request)}}.
     * 
     * Gets the value of the error property.
     * 
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the error property.</p>
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * </p>
     * <pre>
     * getError().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ErrorType }
     * </p>
     * 
     * 
     * @return
     *     The value of the error property.
     */
    public List<ErrorType> getError() {
        if (error == null) {
            error = new ArrayList<>();
        }
        return this.error;
    }

}
