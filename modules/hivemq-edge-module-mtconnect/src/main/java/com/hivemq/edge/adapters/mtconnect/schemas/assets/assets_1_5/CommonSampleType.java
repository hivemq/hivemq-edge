//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.assets.assets_1_5;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;


/**
 * A sample with a single floating point value
 * 
 * <p>Java class for CommonSampleType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="CommonSampleType">
 *   <simpleContent>
 *     <restriction base="<urn:mtconnect.org:MTConnectAssets:1.5>SampleType">
 *     </restriction>
 *   </simpleContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@com.fasterxml.jackson.annotation.JsonTypeName(value = "CommonSampleType")
@XmlType(name = "CommonSampleType")
@XmlSeeAlso({
    AccelerationType.class,
    AccumulatedTimeType.class,
    AmperageType.class,
    AngleType.class,
    AngularAccelerationType.class,
    AngularVelocityType.class,
    AxisFeedrateType.class,
    ClockTimeType.class,
    ConcentrationType.class,
    ConductivityType.class,
    DisplacementType.class,
    ElectricalEnergyType.class,
    EquipmentTimerType.class,
    FillLevelType.class,
    FlowType.class,
    FrequencyType.class,
    GlobalPositionType.class,
    LengthType.class,
    LevelType.class,
    LinearForceType.class,
    LoadType.class,
    MassType.class,
    PathFeedrateType.class,
    PHType.class,
    PositionType.class,
    PowerFactorType.class,
    PressureType.class,
    ProcessTimerType.class,
    ResistanceType.class,
    RotaryVelocityType.class,
    SoundLevelType.class,
    SpindleSpeedType.class,
    StrainType.class,
    TemperatureType.class,
    TensionType.class,
    TiltType.class,
    TorqueType.class,
    VelocityType.class,
    ViscosityType.class,
    VoltAmpereType.class,
    VoltAmpereReactiveType.class,
    VoltageType.class,
    WattageType.class,
    VolumeSpatialType.class,
    VolumeFluidType.class,
    CapacitySpatialType.class,
    CapacityFluidType.class,
    DensityType.class,
    DepositionVolumeType.class,
    DepositionRateVolumetricType.class,
    DepositionAccelerationVolumetricType.class,
    DepositionMassType.class,
    DepositionDensityType.class,
    CuttingSpeedType.class,
    PathFeedratePerRevolutionType.class
})
public class CommonSampleType
    extends SampleType
{


}
