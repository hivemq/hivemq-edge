//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.error.error_1_1;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;


/**
 * An error description
 * 
 * <p>Java class for ErrorType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="ErrorType">
 *   <simpleContent>
 *     <extension base="<urn:mtconnect.org:MTConnectError:1.1>ErrorDescriptionType">
 *       <attribute name="errorCode" use="required" type="{urn:mtconnect.org:MTConnectError:1.1}ErrorCodeType" />
 *     </extension>
 *   </simpleContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "ErrorType")
@XmlType(name = "ErrorType", propOrder = {
    "value"
})
public class ErrorType {

    /**
     * The text description of the error
     * 
     */
    @XmlValue
    protected String value;
    @com.fasterxml.jackson.annotation.JsonProperty(value = "errorCode")
    @XmlAttribute(name = "errorCode", required = true)
    protected ErrorCodeType errorCode;

    /**
     * The text description of the error
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getValue()
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the value of the errorCode property.
     * 
     * @return
     *     possible object is
     *     {@link ErrorCodeType }
     *     
     */
    public ErrorCodeType getErrorCode() {
        return errorCode;
    }

    /**
     * Sets the value of the errorCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link ErrorCodeType }
     *     
     */
    public void setErrorCode(ErrorCodeType value) {
        this.errorCode = value;
    }

}
