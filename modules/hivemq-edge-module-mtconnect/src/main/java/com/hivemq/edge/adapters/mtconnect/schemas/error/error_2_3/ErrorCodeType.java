//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.error.error_2_3;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * The error code
 * 
 * <p>Java class for ErrorCodeType</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>{@code
 * <simpleType name="ErrorCodeType">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="UNAUTHORIZED"/>
 *     <enumeration value="NO_DEVICE"/>
 *     <enumeration value="OUT_OF_RANGE"/>
 *     <enumeration value="TOO_MANY"/>
 *     <enumeration value="INVALID_URI"/>
 *     <enumeration value="INVALID_REQUEST"/>
 *     <enumeration value="INTERNAL_ERROR"/>
 *     <enumeration value="INVALID_PATH"/>
 *     <enumeration value="UNSUPPORTED"/>
 *     <enumeration value="ASSET_NOT_FOUND"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@com.fasterxml.jackson.annotation.JsonTypeName(value = "ErrorCodeType")
@XmlType(name = "ErrorCodeType")
@XmlEnum
public enum ErrorCodeType {


    /**
     * The request did not have sufficient permissions to perform the
     *             request.
     * 
     */
    UNAUTHORIZED,

    /**
     * The device specified in the URI could not be found.
     * 
     */
    NO_DEVICE,

    /**
     * The sequence number was beyond the end of the buffer.
     * 
     */
    OUT_OF_RANGE,

    /**
     * The count given is too large.
     * 
     */
    TOO_MANY,

    /**
     * The URI provided was incorrect.
     * 
     */
    INVALID_URI,

    /**
     * The request was not one of the three specified requests.
     * 
     */
    INVALID_REQUEST,

    /**
     * Contact the software provider, the agent did not behave correctly.
     * 
     */
    INTERNAL_ERROR,

    /**
     * The xpath could not be parsed. Invalid syntax.
     * 
     */
    INVALID_PATH,

    /**
     * The request is not supported by this implementation
     * 
     */
    UNSUPPORTED,

    /**
     * The asset ID cannot be found
     * 
     */
    ASSET_NOT_FOUND;

    public String value() {
        return name();
    }

    public static ErrorCodeType fromValue(String v) {
        return valueOf(v);
    }

}
