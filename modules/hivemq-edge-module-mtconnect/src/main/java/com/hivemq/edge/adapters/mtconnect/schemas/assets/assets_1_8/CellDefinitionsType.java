//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_1_8;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * {{block(CellDefinitions)}} {{termplural(organize)}}
 *         {{block(CellDefinition)}} elements.
 * 
 * <p>Java class for CellDefinitionsType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="CellDefinitionsType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <all>
 *         <element name="CellDefinition" type="{urn:mtconnect.org:MTConnectAssets:1.8}CellDefinitionType" maxOccurs="unbounded"/>
 *       </all>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "CellDefinitionsType")
@XmlType(name = "CellDefinitionsType", propOrder = {

})
public class CellDefinitionsType {

    /**
     * The semantic definition of a {{block(Cell)}}.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "CellDefinition")
    @XmlElement(name = "CellDefinition", required = true)
    protected List<CellDefinitionType> cellDefinition;

    /**
     * The semantic definition of a {{block(Cell)}}.
     * 
     * Gets the value of the cellDefinition property.
     * 
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the cellDefinition property.</p>
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * </p>
     * <pre>
     * getCellDefinition().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CellDefinitionType }
     * </p>
     * 
     * 
     * @return
     *     The value of the cellDefinition property.
     */
    public List<CellDefinitionType> getCellDefinition() {
        if (cellDefinition == null) {
            cellDefinition = new ArrayList<>();
        }
        return this.cellDefinition;
    }

}
