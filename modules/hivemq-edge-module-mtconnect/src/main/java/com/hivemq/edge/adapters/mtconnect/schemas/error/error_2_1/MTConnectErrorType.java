//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.error.error_2_1;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * root entity of an {{term(MTConnectErrors Response Document)}} that
 *         contains the {{term(Error Information Model)}}.
 *         ![MTConnectError](figures/MTConnectError.png
 *         "MTConnectError"){: width="0.8"} > Note:
 *         Additional properties of {{block(MTConnectError)}} **MAY** be defined
 *         for schema and namespace declaration. See {{sect(Schema and Namespace
 *         Declaration Information)}} for an {{term(XML)}} example.
 * 
 * <p>Java class for MTConnectErrorType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="MTConnectErrorType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="Header" type="{urn:mtconnect.org:MTConnectError:2.1}HeaderType"/>
 *         <choice>
 *           <element name="Error" type="{urn:mtconnect.org:MTConnectError:2.1}ErrorType"/>
 *           <element name="Errors" type="{urn:mtconnect.org:MTConnectError:2.1}ErrorsType"/>
 *         </choice>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "MTConnectErrorType")
@XmlType(name = "MTConnectErrorType", propOrder = {
    "header",
    "error",
    "errors"
})
public class MTConnectErrorType {

    /**
     * provides information from an {{term(agent)}} defining version
     *             information, storage capacity, and parameters associated with the
     *             data management within the {{term(agent)}}.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Header")
    @XmlElement(name = "Header", required = true)
    protected HeaderType header;
    /**
     * error encountered by an {{term(agent)}} when responding to a
     *               {{term(request)}}.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Error")
    @XmlElement(name = "Error")
    protected ErrorType error;
    /**
     * {{block(Errors)}} groups one or more {{block(Error)}} entities.
     *               See {{sect(Error)}}. > Note: When compatibility with Version
     *               1.0.1 and earlier of the MTConnect Standard is required for an
     *               implementation, the {{term(MTConnectErrors Response Document)}}
     *               contains only a single {{block(Error)}} entity and the
     *               {{block(Errors)}} entity **MUST NOT** appear in the document.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Errors")
    @XmlElement(name = "Errors")
    protected ErrorsType errors;

    /**
     * provides information from an {{term(agent)}} defining version
     *             information, storage capacity, and parameters associated with the
     *             data management within the {{term(agent)}}.
     * 
     * @return
     *     possible object is
     *     {@link HeaderType }
     *     
     */
    public HeaderType getHeader() {
        return header;
    }

    /**
     * Sets the value of the header property.
     * 
     * @param value
     *     allowed object is
     *     {@link HeaderType }
     *     
     * @see #getHeader()
     */
    public void setHeader(HeaderType value) {
        this.header = value;
    }

    /**
     * error encountered by an {{term(agent)}} when responding to a
     *               {{term(request)}}.
     * 
     * @return
     *     possible object is
     *     {@link ErrorType }
     *     
     */
    public ErrorType getError() {
        return error;
    }

    /**
     * Sets the value of the error property.
     * 
     * @param value
     *     allowed object is
     *     {@link ErrorType }
     *     
     * @see #getError()
     */
    public void setError(ErrorType value) {
        this.error = value;
    }

    /**
     * {{block(Errors)}} groups one or more {{block(Error)}} entities.
     *               See {{sect(Error)}}. > Note: When compatibility with Version
     *               1.0.1 and earlier of the MTConnect Standard is required for an
     *               implementation, the {{term(MTConnectErrors Response Document)}}
     *               contains only a single {{block(Error)}} entity and the
     *               {{block(Errors)}} entity **MUST NOT** appear in the document.
     * 
     * @return
     *     possible object is
     *     {@link ErrorsType }
     *     
     */
    public ErrorsType getErrors() {
        return errors;
    }

    /**
     * Sets the value of the errors property.
     * 
     * @param value
     *     allowed object is
     *     {@link ErrorsType }
     *     
     * @see #getErrors()
     */
    public void setErrors(ErrorsType value) {
        this.errors = value;
    }

}
