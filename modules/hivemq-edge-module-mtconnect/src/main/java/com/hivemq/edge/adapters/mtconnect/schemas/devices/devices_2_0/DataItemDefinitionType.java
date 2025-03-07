//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_2_0;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * The DataItem Definition
 * 
 * <p>Java class for DataItemDefinitionType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="DataItemDefinitionType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <all>
 *         <element name="Description" type="{urn:mtconnect.org:MTConnectDevices:2.0}DataItemDescriptionType" minOccurs="0"/>
 *         <element name="EntryDefinitions" type="{urn:mtconnect.org:MTConnectDevices:2.0}EntryDefinitionsType" minOccurs="0"/>
 *         <element name="CellDefinitions" type="{urn:mtconnect.org:MTConnectDevices:2.0}CellDefinitionsType" minOccurs="0"/>
 *       </all>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "DataItemDefinitionType")
@XmlType(name = "DataItemDefinitionType", propOrder = {

})
public class DataItemDefinitionType {

    /**
     * See {{sect(Description)}}.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "Description")
    @XmlElement(name = "Description")
    protected DataItemDescriptionType description;
    /**
     * {{block(EntryDefinitions)}} groups one or more
     *             {{block(EntryDefinition)}} entities. See {{sect(EntryDefinition)}}.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "EntryDefinitions")
    @XmlElement(name = "EntryDefinitions")
    protected EntryDefinitionsType entryDefinitions;
    /**
     * {{block(CellDefinitions)}} groups one or more
     *             {{block(CellDefinition)}} entities. See {{sect(CellDefinition)}}.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "CellDefinitions")
    @XmlElement(name = "CellDefinitions")
    protected CellDefinitionsType cellDefinitions;

    /**
     * See {{sect(Description)}}.
     * 
     * @return
     *     possible object is
     *     {@link DataItemDescriptionType }
     *     
     */
    public DataItemDescriptionType getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link DataItemDescriptionType }
     *     
     * @see #getDescription()
     */
    public void setDescription(DataItemDescriptionType value) {
        this.description = value;
    }

    /**
     * {{block(EntryDefinitions)}} groups one or more
     *             {{block(EntryDefinition)}} entities. See {{sect(EntryDefinition)}}.
     * 
     * @return
     *     possible object is
     *     {@link EntryDefinitionsType }
     *     
     */
    public EntryDefinitionsType getEntryDefinitions() {
        return entryDefinitions;
    }

    /**
     * Sets the value of the entryDefinitions property.
     * 
     * @param value
     *     allowed object is
     *     {@link EntryDefinitionsType }
     *     
     * @see #getEntryDefinitions()
     */
    public void setEntryDefinitions(EntryDefinitionsType value) {
        this.entryDefinitions = value;
    }

    /**
     * {{block(CellDefinitions)}} groups one or more
     *             {{block(CellDefinition)}} entities. See {{sect(CellDefinition)}}.
     * 
     * @return
     *     possible object is
     *     {@link CellDefinitionsType }
     *     
     */
    public CellDefinitionsType getCellDefinitions() {
        return cellDefinitions;
    }

    /**
     * Sets the value of the cellDefinitions property.
     * 
     * @param value
     *     allowed object is
     *     {@link CellDefinitionsType }
     *     
     * @see #getCellDefinitions()
     */
    public void setCellDefinitions(CellDefinitionsType value) {
        this.cellDefinitions = value;
    }

}
