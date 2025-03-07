//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_2_4;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * DataSet of {{def(EventEnum::MAINTENANCE_LIST)}} If
 *         {{property(MaintenanceList::result::Interval)}} `key` is not provided,
 *         it is assumed `ABSOLUTE`. If
 *         {{property(MaintenanceList::result::Direction)}} `key` is not provided,
 *         it is assumed `UP`. If {{property(MaintenanceList::result::Units)}}
 *         `key` is not provided, it is assumed to be `COUNT`.
 * 
 * <p>Java class for MaintenanceListEntryType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="MaintenanceListEntryType">
 *   <simpleContent>
 *     <restriction base="<urn:mtconnect.org:MTConnectStreams:2.4>EntryType">
 *     </restriction>
 *   </simpleContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "MaintenanceListEntryType")
@XmlType(name = "MaintenanceListEntryType")
public class MaintenanceListEntryType
    extends EntryType
{


}
