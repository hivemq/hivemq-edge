//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_7;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * {{block(DeviceStream)}} {{termplural(organize)}} data reported from a
 *         single piece of equipment. A {{block(DeviceStream)}} element **MUST** be
 *         provided for each piece of equipment reporting data in an
 *         {{block(MTConnectStreams)}} {{term(Response Document)}}.
 * 
 * <p>Java class for DeviceStreamType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="DeviceStreamType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <all>
 *         <element name="ComponentStream" type="{urn:mtconnect.org:MTConnectStreams:1.7}ComponentStreamType" maxOccurs="unbounded" minOccurs="0"/>
 *       </all>
 *       <attribute name="name" use="required" type="{urn:mtconnect.org:MTConnectStreams:1.7}NameType" />
 *       <attribute name="uuid" use="required" type="{urn:mtconnect.org:MTConnectStreams:1.7}UuidType" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "DeviceStreamType")
@XmlType(name = "DeviceStreamType", propOrder = {

})
public class DeviceStreamType {

    /**
     * See the following {{sect(ComponentStream)}} for details.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "ComponentStream")
    @XmlElement(name = "ComponentStream")
    protected List<ComponentStreamType> componentStream;
    /**
     * The component name
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "name")
    @XmlAttribute(name = "name", required = true)
    protected String name;
    /**
     * The unque identifier for this device
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "uuid")
    @XmlAttribute(name = "uuid", required = true)
    protected String uuid;

    /**
     * See the following {{sect(ComponentStream)}} for details.
     * 
     * Gets the value of the componentStream property.
     * 
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the componentStream property.</p>
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * </p>
     * <pre>
     * getComponentStream().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ComponentStreamType }
     * </p>
     * 
     * 
     * @return
     *     The value of the componentStream property.
     */
    public List<ComponentStreamType> getComponentStream() {
        if (componentStream == null) {
            componentStream = new ArrayList<>();
        }
        return this.componentStream;
    }

    /**
     * The component name
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getName()
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * The unque identifier for this device
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * Sets the value of the uuid property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getUuid()
     */
    public void setUuid(String value) {
        this.uuid = value;
    }

}
