//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_2_2;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * Constraints for Entry Values
 * 
 * <p>Java class for EntryType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="EntryType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <attribute name="key" use="required" type="{urn:mtconnect.org:MTConnectStreams:2.2}KeyType" />
 *       <attribute name="removed" type="{urn:mtconnect.org:MTConnectStreams:2.2}RemovedType" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "EntryType")
@XmlType(name = "EntryType", propOrder = {
    "content"
})
@XmlSeeAlso({
    ActiveAxesEntryType.class,
    ActuatorStateEntryType.class,
    AssetChangedEntryType.class,
    AssetRemovedEntryType.class,
    AvailabilityEntryType.class,
    AxisCouplingEntryType.class,
    AxisFeedrateOverrideEntryType.class,
    AxisInterlockEntryType.class,
    AxisStateEntryType.class,
    BlockEntryType.class,
    BlockCountEntryType.class,
    ChuckInterlockEntryType.class,
    ChuckStateEntryType.class,
    CodeEntryType.class,
    CompositionStateEntryType.class,
    ControllerModeEntryType.class,
    ControllerModeOverrideEntryType.class,
    CoupledAxesEntryType.class,
    DateCodeEntryType.class,
    DeviceUuidEntryType.class,
    DirectionEntryType.class,
    DoorStateEntryType.class,
    EmergencyStopEntryType.class,
    EndOfBarEntryType.class,
    EquipmentModeEntryType.class,
    ExecutionEntryType.class,
    FunctionalModeEntryType.class,
    HardnessEntryType.class,
    LineEntryType.class,
    LineLabelEntryType.class,
    LineNumberEntryType.class,
    MaterialEntryType.class,
    MaterialLayerEntryType.class,
    MessageEntryType.class,
    OperatorIdEntryType.class,
    PalletIdEntryType.class,
    PartCountEntryType.class,
    PartDetectEntryType.class,
    PartIdEntryType.class,
    PartNumberEntryType.class,
    PathFeedrateOverrideEntryType.class,
    PathModeEntryType.class,
    PowerStateEntryType.class,
    PowerStatusEntryType.class,
    ProcessTimeEntryType.class,
    ProgramEntryType.class,
    ProgramCommentEntryType.class,
    ProgramEditEntryType.class,
    ProgramEditNameEntryType.class,
    ProgramHeaderEntryType.class,
    ProgramLocationEntryType.class,
    ProgramLocationTypeEntryType.class,
    ProgramNestLevelEntryType.class,
    RotaryModeEntryType.class,
    RotaryVelocityOverrideEntryType.class,
    SerialNumberEntryType.class,
    SpindleInterlockEntryType.class,
    ToolAssetIdEntryType.class,
    ToolGroupEntryType.class,
    ToolIdEntryType.class,
    ToolNumberEntryType.class,
    ToolOffsetEntryType.class,
    UserEntryType.class,
    VariableEntryType.class,
    WaitStateEntryType.class,
    WireEntryType.class,
    WorkholdingIdEntryType.class,
    WorkOffsetEntryType.class,
    OperatingSystemEntryType.class,
    FirmwareEntryType.class,
    ApplicationEntryType.class,
    LibraryEntryType.class,
    HardwareEntryType.class,
    NetworkEntryType.class,
    RotationEntryType.class,
    TranslationEntryType.class,
    ProcessKindIdEntryType.class,
    PartStatusEntryType.class,
    AlarmLimitEntryType.class,
    ProcessAggregateIdEntryType.class,
    PartKindIdEntryType.class,
    AdapterURIEntryType.class,
    DeviceRemovedEntryType.class,
    DeviceChangedEntryType.class,
    SpecificationLimitEntryType.class,
    ConnectionStatusEntryType.class,
    AdapterSoftwareVersionEntryType.class,
    SensorAttachmentEntryType.class,
    ControlLimitEntryType.class,
    DeviceAddedEntryType.class,
    MTConnectVersionEntryType.class,
    ProcessOccurrenceIdEntryType.class,
    PartGroupIdEntryType.class,
    PartUniqueIdEntryType.class,
    ActivationCountEntryType.class,
    DeactivationCountEntryType.class,
    TransferCountEntryType.class,
    LoadCountEntryType.class,
    PartProcessingStateEntryType.class,
    ProcessStateEntryType.class,
    ValveStateEntryType.class,
    LockStateEntryType.class,
    UnloadCountEntryType.class,
    CycleCountEntryType.class,
    OperatingModeEntryType.class,
    AssetCountEntryType.class,
    MaintenanceListEntryType.class,
    FixtureIdEntryType.class,
    PartCountTypeEntryType.class,
    ClockTimeEntryType.class,
    NetworkPortEntryType.class,
    HostNameEntryType.class,
    LeakDetectEntryType.class,
    BatteryStateEntryType.class,
    FeaturePersisitentIdEntryType.class,
    SensorStateEntryType.class,
    ComponentDataEntryType.class,
    WorkOffsetsEntryType.class,
    ToolOffsetsEntryType.class,
    FeatureMeasurementEntryType.class,
    CharacteristicPersistentIdEntryType.class,
    MeasurementTypeEntryType.class,
    MeasurementValueEntryType.class,
    MeasurementUnitsEntryType.class,
    CharacteristicStatusEntryType.class,
    UncertaintyTypeEntryType.class,
    UncertaintyEntryType.class,
    MaterialFeedEntryType.class,
    MaterialChangeEntryType.class,
    MaterialRetractEntryType.class,
    MaterialLoadEntryType.class,
    MaterialUnloadEntryType.class,
    OpenChuckEntryType.class,
    OpenDoorEntryType.class,
    PartChangeEntryType.class,
    CloseDoorEntryType.class,
    CloseChuckEntryType.class,
    InterfaceStateEntryType.class,
    AccelerationEntryType.class,
    AccumulatedTimeEntryType.class,
    AmperageEntryType.class,
    AngleEntryType.class,
    AngularAccelerationEntryType.class,
    AngularVelocityEntryType.class,
    AxisFeedrateEntryType.class,
    CapacityFluidEntryType.class,
    CapacitySpatialEntryType.class,
    ConcentrationEntryType.class,
    ConductivityEntryType.class,
    CuttingSpeedEntryType.class,
    DensityEntryType.class,
    DepositionAccelerationVolumetricEntryType.class,
    DepositionDensityEntryType.class,
    DepositionMassEntryType.class,
    DepositionRateVolumetricEntryType.class,
    DepositionVolumeEntryType.class,
    DisplacementEntryType.class,
    ElectricalEnergyEntryType.class,
    EquipmentTimerEntryType.class,
    FillLevelEntryType.class,
    FlowEntryType.class,
    FrequencyEntryType.class,
    GlobalPositionEntryType.class,
    LengthEntryType.class,
    LevelEntryType.class,
    LinearForceEntryType.class,
    LoadEntryType.class,
    MassEntryType.class,
    PathFeedrateEntryType.class,
    PathFeedratePerRevolutionEntryType.class,
    PathPositionEntryType.class,
    PHEntryType.class,
    PositionEntryType.class,
    PowerFactorEntryType.class,
    PressureEntryType.class,
    ProcessTimerEntryType.class,
    ResistanceEntryType.class,
    RotaryVelocityEntryType.class,
    SoundLevelEntryType.class,
    SpindleSpeedEntryType.class,
    StrainEntryType.class,
    TemperatureEntryType.class,
    TensionEntryType.class,
    TiltEntryType.class,
    TorqueEntryType.class,
    VelocityEntryType.class,
    ViscosityEntryType.class,
    VoltageEntryType.class,
    VoltAmpereEntryType.class,
    VoltAmpereReactiveEntryType.class,
    VolumeFluidEntryType.class,
    VolumeSpatialEntryType.class,
    WattageEntryType.class,
    AmperageDCEntryType.class,
    AmperageACEntryType.class,
    VoltageACEntryType.class,
    VoltageDCEntryType.class,
    XDimensionEntryType.class,
    YDimensionEntryType.class,
    ZDimensionEntryType.class,
    DiameterEntryType.class,
    OrientationEntryType.class,
    HumidityRelativeEntryType.class,
    HumidityAbsoluteEntryType.class,
    HumiditySpecificEntryType.class,
    PressurizationRateEntryType.class,
    DecelerationEntryType.class,
    AssetUpdateRateEntryType.class,
    AngularDecelerationEntryType.class,
    ObservationUpdateRateEntryType.class,
    PressureAbsoluteEntryType.class,
    OpennessEntryType.class,
    DewPointEntryType.class,
    GravitationalForceEntryType.class,
    GravitationalAccelerationEntryType.class,
    BatteryCapacityEntryType.class,
    DischargeRateEntryType.class,
    ChargeRateEntryType.class,
    BatteryChargeEntryType.class,
    SettlingErrorEntryType.class,
    SettlingErrorLinearEntryType.class,
    SettlingErrorAngularEntryType.class,
    FollowingErrorEntryType.class,
    FollowingErrorAngularEntryType.class,
    FollowingErrorLinearEntryType.class,
    DisplacementLinearEntryType.class,
    DisplacementAngularEntryType.class,
    PositionCartesianEntryType.class,
    TableEntryType.class,
    ActiveAxesTableEntryType.class,
    ActuatorStateTableEntryType.class,
    AssetChangedTableEntryType.class,
    AssetRemovedTableEntryType.class,
    AvailabilityTableEntryType.class,
    AxisCouplingTableEntryType.class,
    AxisFeedrateOverrideTableEntryType.class,
    AxisInterlockTableEntryType.class,
    AxisStateTableEntryType.class,
    BlockTableEntryType.class,
    BlockCountTableEntryType.class,
    ChuckInterlockTableEntryType.class,
    ChuckStateTableEntryType.class,
    CodeTableEntryType.class,
    CompositionStateTableEntryType.class,
    ControllerModeTableEntryType.class,
    ControllerModeOverrideTableEntryType.class,
    CoupledAxesTableEntryType.class,
    DateCodeTableEntryType.class,
    DeviceUuidTableEntryType.class,
    DirectionTableEntryType.class,
    DoorStateTableEntryType.class,
    EmergencyStopTableEntryType.class,
    EndOfBarTableEntryType.class,
    EquipmentModeTableEntryType.class,
    ExecutionTableEntryType.class,
    FunctionalModeTableEntryType.class,
    HardnessTableEntryType.class,
    LineTableEntryType.class,
    LineLabelTableEntryType.class,
    LineNumberTableEntryType.class,
    MaterialTableEntryType.class,
    MaterialLayerTableEntryType.class,
    MessageTableEntryType.class,
    OperatorIdTableEntryType.class,
    PalletIdTableEntryType.class,
    PartCountTableEntryType.class,
    PartDetectTableEntryType.class,
    PartIdTableEntryType.class,
    PartNumberTableEntryType.class,
    PathFeedrateOverrideTableEntryType.class,
    PathModeTableEntryType.class,
    PowerStateTableEntryType.class,
    PowerStatusTableEntryType.class,
    ProcessTimeTableEntryType.class,
    ProgramTableEntryType.class,
    ProgramCommentTableEntryType.class,
    ProgramEditTableEntryType.class,
    ProgramEditNameTableEntryType.class,
    ProgramHeaderTableEntryType.class,
    ProgramLocationTableEntryType.class,
    ProgramLocationTypeTableEntryType.class,
    ProgramNestLevelTableEntryType.class,
    RotaryModeTableEntryType.class,
    RotaryVelocityOverrideTableEntryType.class,
    SerialNumberTableEntryType.class,
    SpindleInterlockTableEntryType.class,
    ToolAssetIdTableEntryType.class,
    ToolGroupTableEntryType.class,
    ToolIdTableEntryType.class,
    ToolNumberTableEntryType.class,
    ToolOffsetTableEntryType.class,
    UserTableEntryType.class,
    VariableTableEntryType.class,
    WaitStateTableEntryType.class,
    WireTableEntryType.class,
    WorkholdingIdTableEntryType.class,
    WorkOffsetTableEntryType.class,
    OperatingSystemTableEntryType.class,
    FirmwareTableEntryType.class,
    ApplicationTableEntryType.class,
    LibraryTableEntryType.class,
    HardwareTableEntryType.class,
    NetworkTableEntryType.class,
    RotationTableEntryType.class,
    TranslationTableEntryType.class,
    ProcessKindIdTableEntryType.class,
    PartStatusTableEntryType.class,
    AlarmLimitTableEntryType.class,
    ProcessAggregateIdTableEntryType.class,
    PartKindIdTableEntryType.class,
    AdapterURITableEntryType.class,
    DeviceRemovedTableEntryType.class,
    DeviceChangedTableEntryType.class,
    SpecificationLimitTableEntryType.class,
    ConnectionStatusTableEntryType.class,
    AdapterSoftwareVersionTableEntryType.class,
    SensorAttachmentTableEntryType.class,
    ControlLimitTableEntryType.class,
    DeviceAddedTableEntryType.class,
    MTConnectVersionTableEntryType.class,
    ProcessOccurrenceIdTableEntryType.class,
    PartGroupIdTableEntryType.class,
    PartUniqueIdTableEntryType.class,
    ActivationCountTableEntryType.class,
    DeactivationCountTableEntryType.class,
    TransferCountTableEntryType.class,
    LoadCountTableEntryType.class,
    PartProcessingStateTableEntryType.class,
    ProcessStateTableEntryType.class,
    ValveStateTableEntryType.class,
    LockStateTableEntryType.class,
    UnloadCountTableEntryType.class,
    CycleCountTableEntryType.class,
    OperatingModeTableEntryType.class,
    AssetCountTableEntryType.class,
    MaintenanceListTableEntryType.class,
    FixtureIdTableEntryType.class,
    PartCountTypeTableEntryType.class,
    ClockTimeTableEntryType.class,
    NetworkPortTableEntryType.class,
    HostNameTableEntryType.class,
    LeakDetectTableEntryType.class,
    BatteryStateTableEntryType.class,
    FeaturePersisitentIdTableEntryType.class,
    SensorStateTableEntryType.class,
    ComponentDataTableEntryType.class,
    WorkOffsetsTableEntryType.class,
    ToolOffsetsTableEntryType.class,
    FeatureMeasurementTableEntryType.class,
    CharacteristicPersistentIdTableEntryType.class,
    MeasurementTypeTableEntryType.class,
    MeasurementValueTableEntryType.class,
    MeasurementUnitsTableEntryType.class,
    CharacteristicStatusTableEntryType.class,
    UncertaintyTypeTableEntryType.class,
    UncertaintyTableEntryType.class,
    MaterialFeedTableEntryType.class,
    MaterialChangeTableEntryType.class,
    MaterialRetractTableEntryType.class,
    MaterialLoadTableEntryType.class,
    MaterialUnloadTableEntryType.class,
    OpenChuckTableEntryType.class,
    OpenDoorTableEntryType.class,
    PartChangeTableEntryType.class,
    CloseDoorTableEntryType.class,
    CloseChuckTableEntryType.class,
    InterfaceStateTableEntryType.class,
    AccelerationTableEntryType.class,
    AccumulatedTimeTableEntryType.class,
    AmperageTableEntryType.class,
    AngleTableEntryType.class,
    AngularAccelerationTableEntryType.class,
    AngularVelocityTableEntryType.class,
    AxisFeedrateTableEntryType.class,
    CapacityFluidTableEntryType.class,
    CapacitySpatialTableEntryType.class,
    ConcentrationTableEntryType.class,
    ConductivityTableEntryType.class,
    CuttingSpeedTableEntryType.class,
    DensityTableEntryType.class,
    DepositionAccelerationVolumetricTableEntryType.class,
    DepositionDensityTableEntryType.class,
    DepositionMassTableEntryType.class,
    DepositionRateVolumetricTableEntryType.class,
    DepositionVolumeTableEntryType.class,
    DisplacementTableEntryType.class,
    ElectricalEnergyTableEntryType.class,
    EquipmentTimerTableEntryType.class,
    FillLevelTableEntryType.class,
    FlowTableEntryType.class,
    FrequencyTableEntryType.class,
    GlobalPositionTableEntryType.class,
    LengthTableEntryType.class,
    LevelTableEntryType.class,
    LinearForceTableEntryType.class,
    LoadTableEntryType.class,
    MassTableEntryType.class,
    PathFeedrateTableEntryType.class,
    PathFeedratePerRevolutionTableEntryType.class,
    PathPositionTableEntryType.class,
    PHTableEntryType.class,
    PositionTableEntryType.class,
    PowerFactorTableEntryType.class,
    PressureTableEntryType.class,
    ProcessTimerTableEntryType.class,
    ResistanceTableEntryType.class,
    RotaryVelocityTableEntryType.class,
    SoundLevelTableEntryType.class,
    SpindleSpeedTableEntryType.class,
    StrainTableEntryType.class,
    TemperatureTableEntryType.class,
    TensionTableEntryType.class,
    TiltTableEntryType.class,
    TorqueTableEntryType.class,
    VelocityTableEntryType.class,
    ViscosityTableEntryType.class,
    VoltageTableEntryType.class,
    VoltAmpereTableEntryType.class,
    VoltAmpereReactiveTableEntryType.class,
    VolumeFluidTableEntryType.class,
    VolumeSpatialTableEntryType.class,
    WattageTableEntryType.class,
    AmperageDCTableEntryType.class,
    AmperageACTableEntryType.class,
    VoltageACTableEntryType.class,
    VoltageDCTableEntryType.class,
    XDimensionTableEntryType.class,
    YDimensionTableEntryType.class,
    ZDimensionTableEntryType.class,
    DiameterTableEntryType.class,
    OrientationTableEntryType.class,
    HumidityRelativeTableEntryType.class,
    HumidityAbsoluteTableEntryType.class,
    HumiditySpecificTableEntryType.class,
    PressurizationRateTableEntryType.class,
    DecelerationTableEntryType.class,
    AssetUpdateRateTableEntryType.class,
    AngularDecelerationTableEntryType.class,
    ObservationUpdateRateTableEntryType.class,
    PressureAbsoluteTableEntryType.class,
    OpennessTableEntryType.class,
    DewPointTableEntryType.class,
    GravitationalForceTableEntryType.class,
    GravitationalAccelerationTableEntryType.class,
    BatteryCapacityTableEntryType.class,
    DischargeRateTableEntryType.class,
    ChargeRateTableEntryType.class,
    BatteryChargeTableEntryType.class,
    SettlingErrorTableEntryType.class,
    SettlingErrorLinearTableEntryType.class,
    SettlingErrorAngularTableEntryType.class,
    FollowingErrorTableEntryType.class,
    FollowingErrorAngularTableEntryType.class,
    FollowingErrorLinearTableEntryType.class,
    DisplacementLinearTableEntryType.class,
    DisplacementAngularTableEntryType.class,
    PositionCartesianTableEntryType.class
})
public abstract class EntryType {

    /**
     * Constraints for Entry Values
     * 
     */
    @XmlValue
    protected String content;
    /**
     * the key
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "key")
    @XmlAttribute(name = "key", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String key;
    /**
     * an indicatore that the entry has been removed
     * 
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "removed")
    @XmlAttribute(name = "removed")
    protected Boolean removed;

    /**
     * Constraints for Entry Values
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
     * the key
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the value of the key property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     * @see #getKey()
     */
    public void setKey(String value) {
        this.key = value;
    }

    /**
     * an indicatore that the entry has been removed
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isRemoved() {
        return removed;
    }

    /**
     * Sets the value of the removed property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     * @see #isRemoved()
     */
    public void setRemoved(Boolean value) {
        this.removed = value;
    }

}
