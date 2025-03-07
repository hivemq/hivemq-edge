//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_6;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;


/**
 * An unfaceted string event
 * 
 * <p>Java class for StringEventType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="StringEventType">
 *   <simpleContent>
 *     <restriction base="<urn:mtconnect.org:MTConnectStreams:1.6>EventType">
 *     </restriction>
 *   </simpleContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "StringEventType")
@XmlType(name = "StringEventType")
@XmlSeeAlso({
    CodeType.class,
    CompositionStateType.class,
    DirectionType.class,
    LineType.class,
    LineLabelType.class,
    MaterialType.class,
    OperatorIdType.class,
    PartIdType.class,
    PartNumberType.class,
    PowerStatusType.class,
    ProgramType.class,
    ProgramCommentType.class,
    ProgramEditNameType.class,
    ProgramHeaderType.class,
    SerialNumberType.class,
    UserType.class,
    WireType.class,
    WorkholdingIdType.class,
    AssetChangedType.class,
    AssetRemovedType.class,
    OpenDoorType.class,
    CloseDoorType.class,
    OpenChuckType.class,
    CloseChuckType.class,
    MaterialFeedType.class,
    MaterialChangeType.class,
    MaterialRetractType.class,
    PartChangeType.class,
    MaterialLoadType.class,
    MaterialUnloadType.class,
    ProcessTimeType.class,
    DateCodeType.class,
    MaterialLayerType.class,
    WaitStateType.class,
    PartDetectType.class,
    DeviceUuidType.class,
    ProgramNestLevelType.class,
    ProgramLocationTypeType.class,
    ProgramLocationType.class,
    ToolGroupType.class,
    VariableType.class,
    OperatingSystemType.class,
    FirmwareType.class,
    ApplicationType.class,
    LibraryType.class,
    NetworkType.class,
    RotationType.class,
    TranslationType.class,
    ToolIdType.class,
    ToolNumberType.class,
    ToolAssetIdType.class,
    PalletIdType.class,
    MessageType.class,
    BlockType.class,
    AlarmType.class
})
public class StringEventType
    extends EventType
{


}
