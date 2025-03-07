//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_2_2;

import java.math.BigInteger;
import javax.xml.datatype.XMLGregorianCalendar;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * Description
 * 
 * <p>Java class for EventType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="EventType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <attGroup ref="{urn:mtconnect.org:MTConnectStreams:2.2}ObservationType"/>
 *       <attribute name="resetTriggered" type="{urn:mtconnect.org:MTConnectStreams:2.2}DataItemResetValueType" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "EventType")
@XmlType(name = "EventType", propOrder = {
    "content"
})
@XmlSeeAlso({
    StringListEventType.class,
    ActuatorStateType.class,
    AvailabilityType.class,
    AxisCouplingType.class,
    AxisInterlockType.class,
    AxisStateType.class,
    ChuckInterlockType.class,
    ChuckStateType.class,
    ControllerModeType.class,
    ControllerModeOverrideType.class,
    DirectionType.class,
    DoorStateType.class,
    EmergencyStopType.class,
    EndOfBarType.class,
    EquipmentModeType.class,
    ExecutionType.class,
    FunctionalModeType.class,
    PartDetectType.class,
    PathModeType.class,
    PowerStateType.class,
    PowerStatusType.class,
    ProgramEditType.class,
    ProgramLocationTypeType.class,
    RotaryModeType.class,
    SpindleInterlockType.class,
    WaitStateType.class,
    PartStatusType.class,
    ConnectionStatusType.class,
    PartProcessingStateType.class,
    ProcessStateType.class,
    ValveStateType.class,
    LockStateType.class,
    OperatingModeType.class,
    PartCountTypeType.class,
    DateTimeEventType.class,
    LeakDetectType.class,
    BatteryStateType.class,
    CharacteristicStatusType.class,
    UncertaintyTypeType.class,
    FloatEventType.class,
    InterfaceStateType.class,
    IntegerEventType.class,
    ActiveAxesDataSetType.class,
    ActuatorStateDataSetType.class,
    AssetChangedDataSetType.class,
    AssetRemovedDataSetType.class,
    AvailabilityDataSetType.class,
    AxisCouplingDataSetType.class,
    AxisFeedrateOverrideDataSetType.class,
    AxisInterlockDataSetType.class,
    AxisStateDataSetType.class,
    BlockDataSetType.class,
    BlockCountDataSetType.class,
    ChuckInterlockDataSetType.class,
    ChuckStateDataSetType.class,
    CodeDataSetType.class,
    CompositionStateDataSetType.class,
    ControllerModeDataSetType.class,
    ControllerModeOverrideDataSetType.class,
    CoupledAxesDataSetType.class,
    DateCodeDataSetType.class,
    DeviceUuidDataSetType.class,
    DirectionDataSetType.class,
    DoorStateDataSetType.class,
    EmergencyStopDataSetType.class,
    EndOfBarDataSetType.class,
    EquipmentModeDataSetType.class,
    ExecutionDataSetType.class,
    FunctionalModeDataSetType.class,
    HardnessDataSetType.class,
    LineDataSetType.class,
    LineLabelDataSetType.class,
    LineNumberDataSetType.class,
    MaterialDataSetType.class,
    MaterialLayerDataSetType.class,
    MessageDataSetType.class,
    OperatorIdDataSetType.class,
    PalletIdDataSetType.class,
    PartCountDataSetType.class,
    PartDetectDataSetType.class,
    PartIdDataSetType.class,
    PartNumberDataSetType.class,
    PathFeedrateOverrideDataSetType.class,
    PathModeDataSetType.class,
    PowerStateDataSetType.class,
    PowerStatusDataSetType.class,
    ProcessTimeDataSetType.class,
    ProgramDataSetType.class,
    ProgramCommentDataSetType.class,
    ProgramEditDataSetType.class,
    ProgramEditNameDataSetType.class,
    ProgramHeaderDataSetType.class,
    ProgramLocationDataSetType.class,
    ProgramLocationTypeDataSetType.class,
    ProgramNestLevelDataSetType.class,
    RotaryModeDataSetType.class,
    RotaryVelocityOverrideDataSetType.class,
    SerialNumberDataSetType.class,
    SpindleInterlockDataSetType.class,
    ToolAssetIdDataSetType.class,
    ToolGroupDataSetType.class,
    ToolIdDataSetType.class,
    ToolNumberDataSetType.class,
    ToolOffsetDataSetType.class,
    UserDataSetType.class,
    VariableDataSetType.class,
    WaitStateDataSetType.class,
    WireDataSetType.class,
    WorkholdingIdDataSetType.class,
    WorkOffsetDataSetType.class,
    OperatingSystemDataSetType.class,
    FirmwareDataSetType.class,
    ApplicationDataSetType.class,
    LibraryDataSetType.class,
    HardwareDataSetType.class,
    NetworkDataSetType.class,
    RotationDataSetType.class,
    TranslationDataSetType.class,
    ProcessKindIdDataSetType.class,
    PartStatusDataSetType.class,
    AlarmLimitDataSetType.class,
    ProcessAggregateIdDataSetType.class,
    PartKindIdDataSetType.class,
    AdapterURIDataSetType.class,
    DeviceRemovedDataSetType.class,
    DeviceChangedDataSetType.class,
    SpecificationLimitDataSetType.class,
    ConnectionStatusDataSetType.class,
    AdapterSoftwareVersionDataSetType.class,
    SensorAttachmentDataSetType.class,
    ControlLimitDataSetType.class,
    DeviceAddedDataSetType.class,
    MTConnectVersionDataSetType.class,
    ProcessOccurrenceIdDataSetType.class,
    PartGroupIdDataSetType.class,
    PartUniqueIdDataSetType.class,
    ActivationCountDataSetType.class,
    DeactivationCountDataSetType.class,
    TransferCountDataSetType.class,
    LoadCountDataSetType.class,
    PartProcessingStateDataSetType.class,
    ProcessStateDataSetType.class,
    ValveStateDataSetType.class,
    LockStateDataSetType.class,
    UnloadCountDataSetType.class,
    CycleCountDataSetType.class,
    OperatingModeDataSetType.class,
    AssetCountDataSetType.class,
    MaintenanceListDataSetType.class,
    FixtureIdDataSetType.class,
    PartCountTypeDataSetType.class,
    ClockTimeDataSetType.class,
    NetworkPortDataSetType.class,
    HostNameDataSetType.class,
    LeakDetectDataSetType.class,
    BatteryStateDataSetType.class,
    FeaturePersisitentIdDataSetType.class,
    SensorStateDataSetType.class,
    ComponentDataDataSetType.class,
    WorkOffsetsDataSetType.class,
    ToolOffsetsDataSetType.class,
    FeatureMeasurementDataSetType.class,
    CharacteristicPersistentIdDataSetType.class,
    MeasurementTypeDataSetType.class,
    MeasurementValueDataSetType.class,
    MeasurementUnitsDataSetType.class,
    CharacteristicStatusDataSetType.class,
    UncertaintyTypeDataSetType.class,
    UncertaintyDataSetType.class,
    MaterialFeedDataSetType.class,
    MaterialChangeDataSetType.class,
    MaterialRetractDataSetType.class,
    MaterialLoadDataSetType.class,
    MaterialUnloadDataSetType.class,
    OpenChuckDataSetType.class,
    OpenDoorDataSetType.class,
    PartChangeDataSetType.class,
    CloseDoorDataSetType.class,
    CloseChuckDataSetType.class,
    InterfaceStateDataSetType.class,
    AccelerationDataSetType.class,
    AccumulatedTimeDataSetType.class,
    AmperageDataSetType.class,
    AngleDataSetType.class,
    AngularAccelerationDataSetType.class,
    AngularVelocityDataSetType.class,
    AxisFeedrateDataSetType.class,
    CapacityFluidDataSetType.class,
    CapacitySpatialDataSetType.class,
    ConcentrationDataSetType.class,
    ConductivityDataSetType.class,
    CuttingSpeedDataSetType.class,
    DensityDataSetType.class,
    DepositionAccelerationVolumetricDataSetType.class,
    DepositionDensityDataSetType.class,
    DepositionMassDataSetType.class,
    DepositionRateVolumetricDataSetType.class,
    DepositionVolumeDataSetType.class,
    DisplacementDataSetType.class,
    ElectricalEnergyDataSetType.class,
    EquipmentTimerDataSetType.class,
    FillLevelDataSetType.class,
    FlowDataSetType.class,
    FrequencyDataSetType.class,
    GlobalPositionDataSetType.class,
    LengthDataSetType.class,
    LevelDataSetType.class,
    LinearForceDataSetType.class,
    LoadDataSetType.class,
    MassDataSetType.class,
    PathFeedrateDataSetType.class,
    PathFeedratePerRevolutionDataSetType.class,
    PathPositionDataSetType.class,
    PHDataSetType.class,
    PositionDataSetType.class,
    PowerFactorDataSetType.class,
    PressureDataSetType.class,
    ProcessTimerDataSetType.class,
    ResistanceDataSetType.class,
    RotaryVelocityDataSetType.class,
    SoundLevelDataSetType.class,
    SpindleSpeedDataSetType.class,
    StrainDataSetType.class,
    TemperatureDataSetType.class,
    TensionDataSetType.class,
    TiltDataSetType.class,
    TorqueDataSetType.class,
    VelocityDataSetType.class,
    ViscosityDataSetType.class,
    VoltageDataSetType.class,
    VoltAmpereDataSetType.class,
    VoltAmpereReactiveDataSetType.class,
    VolumeFluidDataSetType.class,
    VolumeSpatialDataSetType.class,
    WattageDataSetType.class,
    AmperageDCDataSetType.class,
    AmperageACDataSetType.class,
    VoltageACDataSetType.class,
    VoltageDCDataSetType.class,
    XDimensionDataSetType.class,
    YDimensionDataSetType.class,
    ZDimensionDataSetType.class,
    DiameterDataSetType.class,
    OrientationDataSetType.class,
    HumidityRelativeDataSetType.class,
    HumidityAbsoluteDataSetType.class,
    HumiditySpecificDataSetType.class,
    PressurizationRateDataSetType.class,
    DecelerationDataSetType.class,
    AssetUpdateRateDataSetType.class,
    AngularDecelerationDataSetType.class,
    ObservationUpdateRateDataSetType.class,
    PressureAbsoluteDataSetType.class,
    OpennessDataSetType.class,
    DewPointDataSetType.class,
    GravitationalForceDataSetType.class,
    GravitationalAccelerationDataSetType.class,
    BatteryCapacityDataSetType.class,
    DischargeRateDataSetType.class,
    ChargeRateDataSetType.class,
    BatteryChargeDataSetType.class,
    SettlingErrorDataSetType.class,
    SettlingErrorLinearDataSetType.class,
    SettlingErrorAngularDataSetType.class,
    FollowingErrorDataSetType.class,
    FollowingErrorAngularDataSetType.class,
    FollowingErrorLinearDataSetType.class,
    DisplacementLinearDataSetType.class,
    DisplacementAngularDataSetType.class,
    PositionCartesianDataSetType.class,
    ActiveAxesTableType.class,
    ActuatorStateTableType.class,
    AssetChangedTableType.class,
    AssetRemovedTableType.class,
    AvailabilityTableType.class,
    AxisCouplingTableType.class,
    AxisFeedrateOverrideTableType.class,
    AxisInterlockTableType.class,
    AxisStateTableType.class,
    BlockTableType.class,
    BlockCountTableType.class,
    ChuckInterlockTableType.class,
    ChuckStateTableType.class,
    CodeTableType.class,
    CompositionStateTableType.class,
    ControllerModeTableType.class,
    ControllerModeOverrideTableType.class,
    CoupledAxesTableType.class,
    DateCodeTableType.class,
    DeviceUuidTableType.class,
    DirectionTableType.class,
    DoorStateTableType.class,
    EmergencyStopTableType.class,
    EndOfBarTableType.class,
    EquipmentModeTableType.class,
    ExecutionTableType.class,
    FunctionalModeTableType.class,
    HardnessTableType.class,
    LineTableType.class,
    LineLabelTableType.class,
    LineNumberTableType.class,
    MaterialTableType.class,
    MaterialLayerTableType.class,
    MessageTableType.class,
    OperatorIdTableType.class,
    PalletIdTableType.class,
    PartCountTableType.class,
    PartDetectTableType.class,
    PartIdTableType.class,
    PartNumberTableType.class,
    PathFeedrateOverrideTableType.class,
    PathModeTableType.class,
    PowerStateTableType.class,
    PowerStatusTableType.class,
    ProcessTimeTableType.class,
    ProgramTableType.class,
    ProgramCommentTableType.class,
    ProgramEditTableType.class,
    ProgramEditNameTableType.class,
    ProgramHeaderTableType.class,
    ProgramLocationTableType.class,
    ProgramLocationTypeTableType.class,
    ProgramNestLevelTableType.class,
    RotaryModeTableType.class,
    RotaryVelocityOverrideTableType.class,
    SerialNumberTableType.class,
    SpindleInterlockTableType.class,
    ToolAssetIdTableType.class,
    ToolGroupTableType.class,
    ToolIdTableType.class,
    ToolNumberTableType.class,
    ToolOffsetTableType.class,
    UserTableType.class,
    VariableTableType.class,
    WaitStateTableType.class,
    WireTableType.class,
    WorkholdingIdTableType.class,
    WorkOffsetTableType.class,
    OperatingSystemTableType.class,
    FirmwareTableType.class,
    ApplicationTableType.class,
    LibraryTableType.class,
    HardwareTableType.class,
    NetworkTableType.class,
    RotationTableType.class,
    TranslationTableType.class,
    ProcessKindIdTableType.class,
    PartStatusTableType.class,
    AlarmLimitTableType.class,
    ProcessAggregateIdTableType.class,
    PartKindIdTableType.class,
    AdapterURITableType.class,
    DeviceRemovedTableType.class,
    DeviceChangedTableType.class,
    SpecificationLimitTableType.class,
    ConnectionStatusTableType.class,
    AdapterSoftwareVersionTableType.class,
    SensorAttachmentTableType.class,
    ControlLimitTableType.class,
    DeviceAddedTableType.class,
    MTConnectVersionTableType.class,
    ProcessOccurrenceIdTableType.class,
    PartGroupIdTableType.class,
    PartUniqueIdTableType.class,
    ActivationCountTableType.class,
    DeactivationCountTableType.class,
    TransferCountTableType.class,
    LoadCountTableType.class,
    PartProcessingStateTableType.class,
    ProcessStateTableType.class,
    ValveStateTableType.class,
    LockStateTableType.class,
    UnloadCountTableType.class,
    CycleCountTableType.class,
    OperatingModeTableType.class,
    AssetCountTableType.class,
    MaintenanceListTableType.class,
    FixtureIdTableType.class,
    PartCountTypeTableType.class,
    ClockTimeTableType.class,
    NetworkPortTableType.class,
    HostNameTableType.class,
    LeakDetectTableType.class,
    BatteryStateTableType.class,
    FeaturePersisitentIdTableType.class,
    SensorStateTableType.class,
    ComponentDataTableType.class,
    WorkOffsetsTableType.class,
    ToolOffsetsTableType.class,
    FeatureMeasurementTableType.class,
    CharacteristicPersistentIdTableType.class,
    MeasurementTypeTableType.class,
    MeasurementValueTableType.class,
    MeasurementUnitsTableType.class,
    CharacteristicStatusTableType.class,
    UncertaintyTypeTableType.class,
    UncertaintyTableType.class,
    MaterialFeedTableType.class,
    MaterialChangeTableType.class,
    MaterialRetractTableType.class,
    MaterialLoadTableType.class,
    MaterialUnloadTableType.class,
    OpenChuckTableType.class,
    OpenDoorTableType.class,
    PartChangeTableType.class,
    CloseDoorTableType.class,
    CloseChuckTableType.class,
    InterfaceStateTableType.class,
    AccelerationTableType.class,
    AccumulatedTimeTableType.class,
    AmperageTableType.class,
    AngleTableType.class,
    AngularAccelerationTableType.class,
    AngularVelocityTableType.class,
    AxisFeedrateTableType.class,
    CapacityFluidTableType.class,
    CapacitySpatialTableType.class,
    ConcentrationTableType.class,
    ConductivityTableType.class,
    CuttingSpeedTableType.class,
    DensityTableType.class,
    DepositionAccelerationVolumetricTableType.class,
    DepositionDensityTableType.class,
    DepositionMassTableType.class,
    DepositionRateVolumetricTableType.class,
    DepositionVolumeTableType.class,
    DisplacementTableType.class,
    ElectricalEnergyTableType.class,
    EquipmentTimerTableType.class,
    FillLevelTableType.class,
    FlowTableType.class,
    FrequencyTableType.class,
    GlobalPositionTableType.class,
    LengthTableType.class,
    LevelTableType.class,
    LinearForceTableType.class,
    LoadTableType.class,
    MassTableType.class,
    PathFeedrateTableType.class,
    PathFeedratePerRevolutionTableType.class,
    PathPositionTableType.class,
    PHTableType.class,
    PositionTableType.class,
    PowerFactorTableType.class,
    PressureTableType.class,
    ProcessTimerTableType.class,
    ResistanceTableType.class,
    RotaryVelocityTableType.class,
    SoundLevelTableType.class,
    SpindleSpeedTableType.class,
    StrainTableType.class,
    TemperatureTableType.class,
    TensionTableType.class,
    TiltTableType.class,
    TorqueTableType.class,
    VelocityTableType.class,
    ViscosityTableType.class,
    VoltageTableType.class,
    VoltAmpereTableType.class,
    VoltAmpereReactiveTableType.class,
    VolumeFluidTableType.class,
    VolumeSpatialTableType.class,
    WattageTableType.class,
    AmperageDCTableType.class,
    AmperageACTableType.class,
    VoltageACTableType.class,
    VoltageDCTableType.class,
    XDimensionTableType.class,
    YDimensionTableType.class,
    ZDimensionTableType.class,
    DiameterTableType.class,
    OrientationTableType.class,
    HumidityRelativeTableType.class,
    HumidityAbsoluteTableType.class,
    HumiditySpecificTableType.class,
    PressurizationRateTableType.class,
    DecelerationTableType.class,
    AssetUpdateRateTableType.class,
    AngularDecelerationTableType.class,
    ObservationUpdateRateTableType.class,
    PressureAbsoluteTableType.class,
    OpennessTableType.class,
    DewPointTableType.class,
    GravitationalForceTableType.class,
    GravitationalAccelerationTableType.class,
    BatteryCapacityTableType.class,
    DischargeRateTableType.class,
    ChargeRateTableType.class,
    BatteryChargeTableType.class,
    SettlingErrorTableType.class,
    SettlingErrorLinearTableType.class,
    SettlingErrorAngularTableType.class,
    FollowingErrorTableType.class,
    FollowingErrorAngularTableType.class,
    FollowingErrorLinearTableType.class,
    DisplacementLinearTableType.class,
    DisplacementAngularTableType.class,
    PositionCartesianTableType.class,
    StringEventType.class,
    InterfaceEventType.class
})
public abstract class EventType {

    /**
     * Description
     * 
     */
    @XmlValue
    protected String content;
    /**
     * An optional indicator that the event or sample was reset
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "resetTriggered")
    @XmlAttribute(name = "resetTriggered")
    protected String resetTriggered;
    /**
     * The events sequence number
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "sequence")
    @XmlAttribute(name = "sequence", required = true)
    protected BigInteger sequence;
    /**
     * The event subtype corresponding to the measurement subtype
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "subType")
    @XmlAttribute(name = "subType")
    protected String subType;
    /**
     * The time the event occurred or recorded
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "timestamp")
    @XmlAttribute(name = "timestamp", required = true)
    protected XMLGregorianCalendar timestamp;
    /**
     * identifier of the maintenance activity.
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "name")
    @XmlAttribute(name = "name")
    protected String name;
    /**
     * The unique identifier of the item being produced
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "dataItemId")
    @XmlAttribute(name = "dataItemId", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String dataItemId;
    /**
     * The identifier of the sub-element this result is in reference to
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "compositionId")
    @XmlAttribute(name = "compositionId")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String compositionId;

    /**
     * Description
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the value of the content property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getContent()
     */
    public void setContent(String value) {
        this.content = value;
    }

    /**
     * An optional indicator that the event or sample was reset
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getResetTriggered() {
        return resetTriggered;
    }

    /**
     * Sets the value of the resetTriggered property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getResetTriggered()
     */
    public void setResetTriggered(String value) {
        this.resetTriggered = value;
    }

    /**
     * The events sequence number
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getSequence() {
        return sequence;
    }

    /**
     * Sets the value of the sequence property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     * @see #getSequence()
     */
    public void setSequence(BigInteger value) {
        this.sequence = value;
    }

    /**
     * The event subtype corresponding to the measurement subtype
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSubType() {
        return subType;
    }

    /**
     * Sets the value of the subType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getSubType()
     */
    public void setSubType(String value) {
        this.subType = value;
    }

    /**
     * The time the event occurred or recorded
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the value of the timestamp property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     * @see #getTimestamp()
     */
    public void setTimestamp(XMLGregorianCalendar value) {
        this.timestamp = value;
    }

    /**
     * identifier of the maintenance activity.
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
     * The unique identifier of the item being produced
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDataItemId() {
        return dataItemId;
    }

    /**
     * Sets the value of the dataItemId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getDataItemId()
     */
    public void setDataItemId(String value) {
        this.dataItemId = value;
    }

    /**
     * The identifier of the sub-element this result is in reference to
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
