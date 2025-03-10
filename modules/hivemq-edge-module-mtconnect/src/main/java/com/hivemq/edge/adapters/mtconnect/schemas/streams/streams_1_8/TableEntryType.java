//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_8;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlType;


/**
 * A {{term(key-value pair)}} published as part of a {{term(Table)}}
 *         {{term(observation)}}. Note: Represented as {{block(Entry)}} in XML.
 * 
 * <p>Java class for TableEntryType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="TableEntryType">
 *   <complexContent>
 *     <extension base="{urn:mtconnect.org:MTConnectStreams:1.8}EntryType">
 *       <sequence>
 *         <element ref="{urn:mtconnect.org:MTConnectStreams:1.8}TableCell" maxOccurs="unbounded" minOccurs="0"/>
 *       </sequence>
 *     </extension>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "TableEntryType")
@XmlType(name = "TableEntryType", propOrder = {
    "tableCell"
})
public class TableEntryType
    extends EntryType
{

    /**
     * A cell of a table
     * 
     */
    @XmlElementRef(name = "TableCell", namespace = "urn:mtconnect.org:MTConnectStreams:1.8", type = JAXBElement.class, required = false)
    protected List<JAXBElement<? extends TableCellType>> tableCell;

    /**
     * A cell of a table
     * 
     * Gets the value of the tableCell property.
     * 
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the tableCell property.</p>
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * </p>
     * <pre>
     * getTableCell().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link TableCellType }{@code >}
     * {@link JAXBElement }{@code <}{@link ToolOffsetCellType }{@code >}
     * {@link JAXBElement }{@code <}{@link WorkOffsetCellType }{@code >}
     * </p>
     * 
     * 
     * @return
     *     The value of the tableCell property.
     */
    public List<JAXBElement<? extends TableCellType>> getTableCell() {
        if (tableCell == null) {
            tableCell = new ArrayList<>();
        }
        return this.tableCell;
    }

}
