//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_2_4;

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
 *     <restriction base="<urn:mtconnect.org:MTConnectStreams:2.4>EventType">
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
    ActiveAxesType.class,
    AssetChangedType.class,
    AssetRemovedType.class,
    CodeType.class,
    CompositionStateType.class,
    CoupledAxesType.class,
    DeviceUuidType.class,
    LineType.class,
    LineLabelType.class,
    MaterialType.class,
    OperatorIdType.class,
    PartIdType.class,
    PartNumberType.class,
    ProcessTimeType.class,
    ProgramType.class,
    ProgramCommentType.class,
    ProgramEditNameType.class,
    ProgramHeaderType.class,
    ProgramLocationType.class,
    SerialNumberType.class,
    ToolGroupType.class,
    UserType.class,
    VariableType.class,
    WireType.class,
    WorkholdingIdType.class,
    WorkOffsetType.class,
    OperatingSystemType.class,
    FirmwareType.class,
    ApplicationType.class,
    LibraryType.class,
    HardwareType.class,
    NetworkType.class,
    ProcessKindIdType.class,
    AlarmLimitType.class,
    ProcessAggregateIdType.class,
    PartKindIdType.class,
    AdapterURIType.class,
    DeviceRemovedType.class,
    DeviceChangedType.class,
    SpecificationLimitType.class,
    AdapterSoftwareVersionType.class,
    SensorAttachmentType.class,
    ControlLimitType.class,
    DeviceAddedType.class,
    MTConnectVersionType.class,
    ProcessOccurrenceIdType.class,
    PartGroupIdType.class,
    PartUniqueIdType.class,
    MaintenanceListType.class,
    FixtureIdType.class,
    HostNameType.class,
    FeaturePersisitentIdType.class,
    SensorStateType.class,
    ComponentDataType.class,
    WorkOffsetsType.class,
    ToolOffsetsType.class,
    FeatureMeasurementType.class,
    CharacteristicPersistentIdType.class,
    MeasurementTypeType.class,
    MeasurementUnitsType.class,
    AlarmLimitsType.class,
    ControlLimitsType.class,
    SpecificationLimitsType.class,
    ToolCuttingItemType.class,
    LocationAddressType.class,
    ActivePowerSourceType.class,
    LocationNarrativeType.class,
    LocationSpatialGeographicType.class,
    MaterialFeedType.class,
    MaterialChangeType.class,
    MaterialRetractType.class,
    MaterialLoadType.class,
    MaterialUnloadType.class,
    OpenChuckType.class,
    OpenDoorType.class,
    PartChangeType.class,
    CloseDoorType.class,
    CloseChuckType.class,
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
