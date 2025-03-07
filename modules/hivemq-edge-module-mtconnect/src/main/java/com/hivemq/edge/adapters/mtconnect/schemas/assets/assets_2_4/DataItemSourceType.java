//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_2_4;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlIDREF;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * A native data source
 * 
 * <p>Java class for DataItemSourceType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="DataItemSourceType">
 *   <simpleContent>
 *     <extension base="<urn:mtconnect.org:MTConnectAssets:2.4>ItemSourceType">
 *       <attribute name="dataItemId" type="{urn:mtconnect.org:MTConnectAssets:2.4}SourceDataItemIdType" />
 *       <attribute name="componentId" type="{urn:mtconnect.org:MTConnectAssets:2.4}SourceComponentIdType" />
 *       <attribute name="compositionId" type="{urn:mtconnect.org:MTConnectAssets:2.4}CompositionIdType" />
 *     </extension>
 *   </simpleContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "DataItemSourceType")
@XmlType(name = "DataItemSourceType", propOrder = {
    "value"
})
public class DataItemSourceType {

    /**
     * The measurement source
     * 
     */
    @XmlValue
    protected String value;
    /**
     * The optional data item within the source component that provides
     *               the underlying data
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "dataItemId")
    @XmlAttribute(name = "dataItemId")
    @XmlIDREF
    protected Object dataItemId;
    /**
     * The component that is collecting the data associated with this
     *               data item
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "componentId")
    @XmlAttribute(name = "componentId")
    @XmlIDREF
    protected Object componentId;
    /**
     * The optional composition identifier for the source of this data
     *               item
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "compositionId")
    @XmlAttribute(name = "compositionId")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String compositionId;

    /**
     * The measurement source
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
     * The optional data item within the source component that provides
     *               the underlying data
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getDataItemId() {
        return dataItemId;
    }

    /**
     * Sets the value of the dataItemId property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     * @see #getDataItemId()
     */
    public void setDataItemId(Object value) {
        this.dataItemId = value;
    }

    /**
     * The component that is collecting the data associated with this
     *               data item
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getComponentId() {
        return componentId;
    }

    /**
     * Sets the value of the componentId property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     * @see #getComponentId()
     */
    public void setComponentId(Object value) {
        this.componentId = value;
    }

    /**
     * The optional composition identifier for the source of this data
     *               item
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCompositionId() {
        return compositionId;
    }

    /**
     * Sets the value of the compositionId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getCompositionId()
     */
    public void setCompositionId(String value) {
        this.compositionId = value;
    }

}
