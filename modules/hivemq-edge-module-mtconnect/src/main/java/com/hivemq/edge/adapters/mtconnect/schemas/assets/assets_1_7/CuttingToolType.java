//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_1_7;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementRefs;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * {{block(CuttingTool)}} {{block(Asset)}} type.
 * 
 * <p>Java class for CuttingToolType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="CuttingToolType">
 *   <complexContent>
 *     <extension base="{urn:mtconnect.org:MTConnectAssets:1.7}AssetType">
 *       <sequence>
 *         <element name="Description" type="{urn:mtconnect.org:MTConnectAssets:1.7}AssetDescriptionType" minOccurs="0"/>
 *         <choice>
 *           <sequence>
 *             <element name="CuttingToolDefinition" type="{urn:mtconnect.org:MTConnectAssets:1.7}CuttingToolDefinitionType"/>
 *             <element name="CuttingToolLifeCycle" type="{urn:mtconnect.org:MTConnectAssets:1.7}CuttingToolLifeCycleType" minOccurs="0"/>
 *           </sequence>
 *           <element name="CuttingToolLifeCycle" type="{urn:mtconnect.org:MTConnectAssets:1.7}CuttingToolLifeCycleType"/>
 *         </choice>
 *       </sequence>
 *       <attribute name="serialNumber" use="required" type="{urn:mtconnect.org:MTConnectAssets:1.7}SerialNumberAttrType" />
 *       <attribute name="manufacturers" type="{urn:mtconnect.org:MTConnectAssets:1.7}ManufacturersType" />
 *       <attribute name="toolId" use="required" type="{urn:mtconnect.org:MTConnectAssets:1.7}CuttingToolIdType" />
 *     </extension>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "CuttingToolType")
@XmlType(name = "CuttingToolType", propOrder = {
    "rest"
})
public class CuttingToolType
    extends AssetType
{

    /**
     * Gets the rest of the content model. 
     * 
     * <p>
     * You are getting this "catch-all" property because of the following reason: 
     * The field name "CuttingToolLifeCycle" is used by two different parts of a schema. See: 
     * line 12076 of file:/Users/yingda.cao/coding/HiveMQ/hivemq-edge/modules/hivemq-edge-module-mtconnect/schema/MTConnectAssets_1.7.xsd
     * line 12063 of file:/Users/yingda.cao/coding/HiveMQ/hivemq-edge/modules/hivemq-edge-module-mtconnect/schema/MTConnectAssets_1.7.xsd
     * <p>
     * To get rid of this property, apply a property customization to one 
     * of both of the following declarations to change their names:
     * 
     */
    @XmlElementRefs({
        @XmlElementRef(name = "Description", namespace = "urn:mtconnect.org:MTConnectAssets:1.7", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "CuttingToolDefinition", namespace = "urn:mtconnect.org:MTConnectAssets:1.7", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "CuttingToolLifeCycle", namespace = "urn:mtconnect.org:MTConnectAssets:1.7", type = JAXBElement.class, required = false)
    })
    protected List<JAXBElement<?>> rest;
    /**
     * The serial number associated with a {{block(Component)}},
     *               {{block(Asset)}}, or {{block(Device)}}.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "serialNumber")
    @XmlAttribute(name = "serialNumber", required = true)
    protected String serialNumber;
    /**
     * The manufacturer of this asset
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "manufacturers")
    @XmlAttribute(name = "manufacturers")
    protected String manufacturers;
    /**
     * **DEPRECATED** in *Version 1.2.0*. See {{block(ToolAssetId)}}. The
     *               identifier of the tool currently in use for a given `Path`.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "toolId")
    @XmlAttribute(name = "toolId", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String toolId;

    /**
     * Gets the rest of the content model. 
     * 
     * <p>
     * You are getting this "catch-all" property because of the following reason: 
     * The field name "CuttingToolLifeCycle" is used by two different parts of a schema. See: 
     * line 12076 of file:/Users/yingda.cao/coding/HiveMQ/hivemq-edge/modules/hivemq-edge-module-mtconnect/schema/MTConnectAssets_1.7.xsd
     * line 12063 of file:/Users/yingda.cao/coding/HiveMQ/hivemq-edge/modules/hivemq-edge-module-mtconnect/schema/MTConnectAssets_1.7.xsd
     * <p>
     * To get rid of this property, apply a property customization to one 
     * of both of the following declarations to change their names:
     * 
     * Gets the value of the rest property.
     * 
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the rest property.</p>
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * </p>
     * <pre>
     * getRest().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link AssetDescriptionType }{@code >}
     * {@link JAXBElement }{@code <}{@link CuttingToolDefinitionType }{@code >}
     * {@link JAXBElement }{@code <}{@link CuttingToolLifeCycleType }{@code >}
     * </p>
     * 
     * 
     * @return
     *     The value of the rest property.
     */
    public List<JAXBElement<?>> getRest() {
        if (rest == null) {
            rest = new ArrayList<>();
        }
        return this.rest;
    }

    /**
     * The serial number associated with a {{block(Component)}},
     *               {{block(Asset)}}, or {{block(Device)}}.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSerialNumber() {
        return serialNumber;
    }

    /**
     * Sets the value of the serialNumber property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getSerialNumber()
     */
    public void setSerialNumber(String value) {
        this.serialNumber = value;
    }

    /**
     * The manufacturer of this asset
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getManufacturers() {
        return manufacturers;
    }

    /**
     * Sets the value of the manufacturers property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getManufacturers()
     */
    public void setManufacturers(String value) {
        this.manufacturers = value;
    }

    /**
     * **DEPRECATED** in *Version 1.2.0*. See {{block(ToolAssetId)}}. The
     *               identifier of the tool currently in use for a given `Path`.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getToolId() {
        return toolId;
    }

    /**
     * Sets the value of the toolId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getToolId()
     */
    public void setToolId(String value) {
        this.toolId = value;
    }

}
