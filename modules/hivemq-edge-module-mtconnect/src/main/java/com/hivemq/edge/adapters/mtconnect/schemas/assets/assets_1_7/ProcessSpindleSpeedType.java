//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_1_7;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;


/**
 * The constrained process spindle speed for this tool.
 * 
 * <p>Java class for ProcessSpindleSpeedType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="ProcessSpindleSpeedType">
 *   <simpleContent>
 *     <extension base="<urn:mtconnect.org:MTConnectAssets:1.7>SpeedType">
 *       <attribute name="maximum" type="{urn:mtconnect.org:MTConnectAssets:1.7}MaximumType" />
 *       <attribute name="minimum" type="{urn:mtconnect.org:MTConnectAssets:1.7}MinimumType" />
 *       <attribute name="nominal" type="{urn:mtconnect.org:MTConnectAssets:1.7}NominalType" />
 *     </extension>
 *   </simpleContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "ProcessSpindleSpeedType")
@XmlType(name = "ProcessSpindleSpeedType", propOrder = {
    "value"
})
public class ProcessSpindleSpeedType {

    /**
     * A speed in RPM or mm/s
     * 
     */
    @XmlValue
    protected String value;
    /**
     * If the data reported for a data item is a range of numeric values,
     *               the expected value reported **MAY** be described with an upper
     *               limit defined by this constraint.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "maximum")
    @XmlAttribute(name = "maximum")
    protected Float maximum;
    /**
     * If the data reported for a data item is a range of numeric values,
     *               the expected value reported **MAY** be described with a lower
     *               limit defined by this constraint.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "minimum")
    @XmlAttribute(name = "minimum")
    protected Float minimum;
    /**
     * The target or expected value for this data item.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "nominal")
    @XmlAttribute(name = "nominal")
    protected Float nominal;

    /**
     * A speed in RPM or mm/s
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
     * If the data reported for a data item is a range of numeric values,
     *               the expected value reported **MAY** be described with an upper
     *               limit defined by this constraint.
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public Float getMaximum() {
        return maximum;
    }

    /**
     * Sets the value of the maximum property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     * @see #getMaximum()
     */
    public void setMaximum(Float value) {
        this.maximum = value;
    }

    /**
     * If the data reported for a data item is a range of numeric values,
     *               the expected value reported **MAY** be described with a lower
     *               limit defined by this constraint.
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public Float getMinimum() {
        return minimum;
    }

    /**
     * Sets the value of the minimum property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     * @see #getMinimum()
     */
    public void setMinimum(Float value) {
        this.minimum = value;
    }

    /**
     * The target or expected value for this data item.
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public Float getNominal() {
        return nominal;
    }

    /**
     * Sets the value of the nominal property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     * @see #getNominal()
     */
    public void setNominal(Float value) {
        this.nominal = value;
    }

}
