//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_2_1;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * A cell of a table
 * 
 * <p>Java class for TableCellType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="TableCellType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <attribute name="key" use="required" type="{urn:mtconnect.org:MTConnectStreams:2.1}KeyType" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "TableCellType")
@XmlType(name = "TableCellType", propOrder = {
    "content"
})
@XmlSeeAlso({
    ActiveAxesCellType.class,
    ActuatorStateCellType.class,
    AssetChangedCellType.class,
    AssetRemovedCellType.class,
    AvailabilityCellType.class,
    AxisCouplingCellType.class,
    AxisFeedrateOverrideCellType.class,
    AxisInterlockCellType.class,
    AxisStateCellType.class,
    BlockCellType.class,
    BlockCountCellType.class,
    ChuckInterlockCellType.class,
    ChuckStateCellType.class,
    CodeCellType.class,
    CompositionStateCellType.class,
    ControllerModeCellType.class,
    ControllerModeOverrideCellType.class,
    CoupledAxesCellType.class,
    DateCodeCellType.class,
    DeviceUuidCellType.class,
    DirectionCellType.class,
    DoorStateCellType.class,
    EmergencyStopCellType.class,
    EndOfBarCellType.class,
    EquipmentModeCellType.class,
    ExecutionCellType.class,
    FunctionalModeCellType.class,
    HardnessCellType.class,
    LineCellType.class,
    LineLabelCellType.class,
    LineNumberCellType.class,
    MaterialCellType.class,
    MaterialLayerCellType.class,
    MessageCellType.class,
    OperatorIdCellType.class,
    PalletIdCellType.class,
    PartCountCellType.class,
    PartDetectCellType.class,
    PartIdCellType.class,
    PartNumberCellType.class,
    PathFeedrateOverrideCellType.class,
    PathModeCellType.class,
    PowerStateCellType.class,
    PowerStatusCellType.class,
    ProcessTimeCellType.class,
    ProgramCellType.class,
    ProgramCommentCellType.class,
    ProgramEditCellType.class,
    ProgramEditNameCellType.class,
    ProgramHeaderCellType.class,
    ProgramLocationCellType.class,
    ProgramLocationTypeCellType.class,
    ProgramNestLevelCellType.class,
    RotaryModeCellType.class,
    RotaryVelocityOverrideCellType.class,
    SerialNumberCellType.class,
    SpindleInterlockCellType.class,
    ToolAssetIdCellType.class,
    ToolGroupCellType.class,
    ToolIdCellType.class,
    ToolNumberCellType.class,
    ToolOffsetCellType.class,
    UserCellType.class,
    VariableCellType.class,
    WaitStateCellType.class,
    WireCellType.class,
    WorkholdingIdCellType.class,
    WorkOffsetCellType.class,
    OperatingSystemCellType.class,
    FirmwareCellType.class,
    ApplicationCellType.class,
    LibraryCellType.class,
    HardwareCellType.class,
    NetworkCellType.class,
    RotationCellType.class,
    TranslationCellType.class,
    ProcessKindIdCellType.class,
    PartStatusCellType.class,
    AlarmLimitCellType.class,
    ProcessAggregateIdCellType.class,
    PartKindIdCellType.class,
    AdapterURICellType.class,
    DeviceRemovedCellType.class,
    DeviceChangedCellType.class,
    SpecificationLimitCellType.class,
    ConnectionStatusCellType.class,
    AdapterSoftwareVersionCellType.class,
    SensorAttachmentCellType.class,
    ControlLimitCellType.class,
    DeviceAddedCellType.class,
    MTConnectVersionCellType.class,
    ProcessOccurrenceIdCellType.class,
    PartGroupIdCellType.class,
    PartUniqueIdCellType.class,
    ActivationCountCellType.class,
    DeactivationCountCellType.class,
    TransferCountCellType.class,
    LoadCountCellType.class,
    PartProcessingStateCellType.class,
    ProcessStateCellType.class,
    ValveStateCellType.class,
    LockStateCellType.class,
    UnloadCountCellType.class,
    CycleCountCellType.class,
    OperatingModeCellType.class,
    AssetCountCellType.class,
    MaintenanceListCellType.class,
    FixtureIdCellType.class,
    PartCountTypeCellType.class,
    NetworkPortCellType.class,
    HostNameCellType.class,
    LeakDetectCellType.class,
    BatteryStateCellType.class,
    MaterialFeedCellType.class,
    MaterialChangeCellType.class,
    MaterialRetractCellType.class,
    MaterialLoadCellType.class,
    MaterialUnloadCellType.class,
    OpenChuckCellType.class,
    OpenDoorCellType.class,
    PartChangeCellType.class,
    CloseDoorCellType.class,
    CloseChuckCellType.class,
    InterfaceStateCellType.class,
    AccelerationCellType.class,
    AccumulatedTimeCellType.class,
    AmperageCellType.class,
    AngleCellType.class,
    AngularAccelerationCellType.class,
    AngularVelocityCellType.class,
    AxisFeedrateCellType.class,
    CapacityFluidCellType.class,
    CapacitySpatialCellType.class,
    ConcentrationCellType.class,
    ConductivityCellType.class,
    CuttingSpeedCellType.class,
    DensityCellType.class,
    DepositionAccelerationVolumetricCellType.class,
    DepositionDensityCellType.class,
    DepositionMassCellType.class,
    DepositionRateVolumetricCellType.class,
    DepositionVolumeCellType.class,
    DisplacementCellType.class,
    ElectricalEnergyCellType.class,
    EquipmentTimerCellType.class,
    FillLevelCellType.class,
    FlowCellType.class,
    FrequencyCellType.class,
    GlobalPositionCellType.class,
    LengthCellType.class,
    LevelCellType.class,
    LinearForceCellType.class,
    LoadCellType.class,
    MassCellType.class,
    PathFeedrateCellType.class,
    PathFeedratePerRevolutionCellType.class,
    PathPositionCellType.class,
    PHCellType.class,
    PositionCellType.class,
    PowerFactorCellType.class,
    PressureCellType.class,
    ProcessTimerCellType.class,
    ResistanceCellType.class,
    RotaryVelocityCellType.class,
    SoundLevelCellType.class,
    SpindleSpeedCellType.class,
    StrainCellType.class,
    TemperatureCellType.class,
    TensionCellType.class,
    TiltCellType.class,
    TorqueCellType.class,
    VelocityCellType.class,
    ViscosityCellType.class,
    VoltageCellType.class,
    VoltAmpereCellType.class,
    VoltAmpereReactiveCellType.class,
    VolumeFluidCellType.class,
    VolumeSpatialCellType.class,
    WattageCellType.class,
    AmperageDCCellType.class,
    AmperageACCellType.class,
    VoltageACCellType.class,
    VoltageDCCellType.class,
    XDimensionCellType.class,
    YDimensionCellType.class,
    ZDimensionCellType.class,
    DiameterCellType.class,
    OrientationCellType.class,
    HumidityRelativeCellType.class,
    HumidityAbsoluteCellType.class,
    HumiditySpecificCellType.class,
    PressurizationRateCellType.class,
    DecelerationCellType.class,
    AssetUpdateRateCellType.class,
    AngularDecelerationCellType.class,
    ObservationUpdateRateCellType.class,
    PressureAbsoluteCellType.class,
    OpennessCellType.class,
    DewPointCellType.class,
    GravitationalForceCellType.class,
    GravitationalAccelerationCellType.class,
    BatteryCapacityCellType.class,
    DischargeRateCellType.class,
    ChargeRateCellType.class,
    BatteryChargeCellType.class,
    SettlingErrorCellType.class,
    SettlingErrorLinearCellType.class,
    SettlingErrorAngularCellType.class,
    FollowingErrorCellType.class,
    FollowingErrorAngularCellType.class,
    FollowingErrorLinearCellType.class,
    DisplacementLinearCellType.class,
    DisplacementAngularCellType.class,
    PositionCartesianCellType.class
})
public abstract class TableCellType {

    /**
     * A cell of a table
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
     * A cell of a table
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

}
