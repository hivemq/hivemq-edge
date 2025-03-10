/*
 * Copyright 2023-present HiveMQ GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.hivemq.edge.adapters.mtconnect.schemas;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class MtConnectSchemaPatchTest {
    private static final @NotNull File SCHEMA_FOLDER_FILE = new File("schema");
    private static final @NotNull File PATCH_SCRIPT_FILE = new File(SCHEMA_FOLDER_FILE, "patch-script.sh");
    /*
     * //xs:complexType[@mixed='true']/xs:complexContent/xs:extension[@base='EventType']//xs:element/../../../..
     * //xs:complexType[@mixed='true']/xs:complexContent/xs:extension[@base='EntryType']//xs:element/../../../..
     */
    private static final @NotNull Map<MtConnectSchema, List<MixedTypeNameGroup>> MIXED_SCHEMA_TO_TYPE_NAME_GROUPS_MAP =
            Map.of(MtConnectSchema.Streams_1_5,
                    List.of(new MixedTypeNameGroup("EventType", List.of("VariableDataSetType"))),
                    MtConnectSchema.Streams_1_6,
                    List.of(new MixedTypeNameGroup("EventType",
                                    List.of("VariableDataSetType", "WorkOffsetTableType", "ToolOffsetTableType")),
                            new MixedTypeNameGroup("EntryType",
                                    List.of("TableEntryType", "WorkOffsetTableEntryType", "ToolOffsetTableEntryType"))),
                    MtConnectSchema.Streams_1_7,
                    List.of(new MixedTypeNameGroup("EventType",
                                    List.of("VariableDataSetType", "WorkOffsetTableType", "ToolOffsetTableType")),
                            new MixedTypeNameGroup("EntryType",
                                    List.of("TableEntryType", "WorkOffsetTableEntryType", "ToolOffsetTableEntryType"))),
                    MtConnectSchema.Streams_1_8,
                    List.of(new MixedTypeNameGroup("EventType",
                                    List.of("VariableDataSetType", "WorkOffsetTableType", "ToolOffsetTableType")),
                            new MixedTypeNameGroup("EntryType",
                                    List.of("TableEntryType", "WorkOffsetTableEntryType", "ToolOffsetTableEntryType"))),
                    MtConnectSchema.Streams_2_0,
                    List.of(new MixedTypeNameGroup("EventType",
                                    List.of("ActiveAxesDataSetType",
                                            "ActuatorStateDataSetType",
                                            "AssetChangedDataSetType",
                                            "AssetRemovedDataSetType",
                                            "AvailabilityDataSetType",
                                            "AxisCouplingDataSetType",
                                            "AxisFeedrateOverrideDataSetType",
                                            "AxisInterlockDataSetType",
                                            "AxisStateDataSetType",
                                            "BlockDataSetType",
                                            "BlockCountDataSetType",
                                            "ChuckInterlockDataSetType",
                                            "ChuckStateDataSetType",
                                            "CodeDataSetType",
                                            "CompositionStateDataSetType",
                                            "ControllerModeDataSetType",
                                            "ControllerModeOverrideDataSetType",
                                            "CoupledAxesDataSetType",
                                            "DateCodeDataSetType",
                                            "DeviceUuidDataSetType",
                                            "DirectionDataSetType",
                                            "DoorStateDataSetType",
                                            "EmergencyStopDataSetType",
                                            "EndOfBarDataSetType",
                                            "EquipmentModeDataSetType",
                                            "ExecutionDataSetType",
                                            "FunctionalModeDataSetType",
                                            "HardnessDataSetType",
                                            "LineDataSetType",
                                            "LineLabelDataSetType",
                                            "LineNumberDataSetType",
                                            "MaterialDataSetType",
                                            "MaterialLayerDataSetType",
                                            "MessageDataSetType",
                                            "OperatorIdDataSetType",
                                            "PalletIdDataSetType",
                                            "PartCountDataSetType",
                                            "PartDetectDataSetType",
                                            "PartIdDataSetType",
                                            "PartNumberDataSetType",
                                            "PathFeedrateOverrideDataSetType",
                                            "PathModeDataSetType",
                                            "PowerStateDataSetType",
                                            "PowerStatusDataSetType",
                                            "ProcessTimeDataSetType",
                                            "ProgramDataSetType",
                                            "ProgramCommentDataSetType",
                                            "ProgramEditDataSetType",
                                            "ProgramEditNameDataSetType",
                                            "ProgramHeaderDataSetType",
                                            "ProgramLocationDataSetType",
                                            "ProgramLocationTypeDataSetType",
                                            "ProgramNestLevelDataSetType",
                                            "RotaryModeDataSetType",
                                            "RotaryVelocityOverrideDataSetType",
                                            "SerialNumberDataSetType",
                                            "SpindleInterlockDataSetType",
                                            "ToolAssetIdDataSetType",
                                            "ToolGroupDataSetType",
                                            "ToolIdDataSetType",
                                            "ToolNumberDataSetType",
                                            "ToolOffsetDataSetType",
                                            "UserDataSetType",
                                            "VariableDataSetType",
                                            "WaitStateDataSetType",
                                            "WireDataSetType",
                                            "WorkholdingIdDataSetType",
                                            "WorkOffsetDataSetType",
                                            "OperatingSystemDataSetType",
                                            "FirmwareDataSetType",
                                            "ApplicationDataSetType",
                                            "LibraryDataSetType",
                                            "HardwareDataSetType",
                                            "NetworkDataSetType",
                                            "RotationDataSetType",
                                            "TranslationDataSetType",
                                            "ProcessKindIdDataSetType",
                                            "PartStatusDataSetType",
                                            "AlarmLimitDataSetType",
                                            "ProcessAggregateIdDataSetType",
                                            "PartKindIdDataSetType",
                                            "AdapterURIDataSetType",
                                            "DeviceRemovedDataSetType",
                                            "DeviceChangedDataSetType",
                                            "SpecificationLimitDataSetType",
                                            "ConnectionStatusDataSetType",
                                            "AdapterSoftwareVersionDataSetType",
                                            "SensorAttachmentDataSetType",
                                            "ControlLimitDataSetType",
                                            "DeviceAddedDataSetType",
                                            "MTConnectVersionDataSetType",
                                            "ProcessOccurrenceIdDataSetType",
                                            "PartGroupIdDataSetType",
                                            "PartUniqueIdDataSetType",
                                            "ActivationCountDataSetType",
                                            "DeactivationCountDataSetType",
                                            "TransferCountDataSetType",
                                            "LoadCountDataSetType",
                                            "PartProcessingStateDataSetType",
                                            "ProcessStateDataSetType",
                                            "ValveStateDataSetType",
                                            "LockStateDataSetType",
                                            "UnloadCountDataSetType",
                                            "CycleCountDataSetType",
                                            "OperatingModeDataSetType",
                                            "AssetCountDataSetType",
                                            "MaintenanceListDataSetType",
                                            "FixtureIdDataSetType",
                                            "PartCountTypeDataSetType",
                                            "MaterialFeedDataSetType",
                                            "MaterialChangeDataSetType",
                                            "MaterialRetractDataSetType",
                                            "MaterialLoadDataSetType",
                                            "MaterialUnloadDataSetType",
                                            "OpenChuckDataSetType",
                                            "OpenDoorDataSetType",
                                            "PartChangeDataSetType",
                                            "CloseDoorDataSetType",
                                            "CloseChuckDataSetType",
                                            "InterfaceStateDataSetType",
                                            "AccelerationDataSetType",
                                            "AccumulatedTimeDataSetType",
                                            "AmperageDataSetType",
                                            "AngleDataSetType",
                                            "AngularAccelerationDataSetType",
                                            "AngularVelocityDataSetType",
                                            "AxisFeedrateDataSetType",
                                            "CapacityFluidDataSetType",
                                            "CapacitySpatialDataSetType",
                                            "ConcentrationDataSetType",
                                            "ConductivityDataSetType",
                                            "CuttingSpeedDataSetType",
                                            "DensityDataSetType",
                                            "DepositionAccelerationVolumetricDataSetType",
                                            "DepositionDensityDataSetType",
                                            "DepositionMassDataSetType",
                                            "DepositionRateVolumetricDataSetType",
                                            "DepositionVolumeDataSetType",
                                            "DisplacementDataSetType",
                                            "ElectricalEnergyDataSetType",
                                            "EquipmentTimerDataSetType",
                                            "FillLevelDataSetType",
                                            "FlowDataSetType",
                                            "FrequencyDataSetType",
                                            "GlobalPositionDataSetType",
                                            "LengthDataSetType",
                                            "LevelDataSetType",
                                            "LinearForceDataSetType",
                                            "LoadDataSetType",
                                            "MassDataSetType",
                                            "PathFeedrateDataSetType",
                                            "PathFeedratePerRevolutionDataSetType",
                                            "PathPositionDataSetType",
                                            "PHDataSetType",
                                            "PositionDataSetType",
                                            "PowerFactorDataSetType",
                                            "PressureDataSetType",
                                            "ProcessTimerDataSetType",
                                            "ResistanceDataSetType",
                                            "RotaryVelocityDataSetType",
                                            "SoundLevelDataSetType",
                                            "SpindleSpeedDataSetType",
                                            "StrainDataSetType",
                                            "TemperatureDataSetType",
                                            "TensionDataSetType",
                                            "TiltDataSetType",
                                            "TorqueDataSetType",
                                            "VelocityDataSetType",
                                            "ViscosityDataSetType",
                                            "VoltageDataSetType",
                                            "VoltAmpereDataSetType",
                                            "VoltAmpereReactiveDataSetType",
                                            "VolumeFluidDataSetType",
                                            "VolumeSpatialDataSetType",
                                            "WattageDataSetType",
                                            "AmperageDCDataSetType",
                                            "AmperageACDataSetType",
                                            "VoltageACDataSetType",
                                            "VoltageDCDataSetType",
                                            "XDimensionDataSetType",
                                            "YDimensionDataSetType",
                                            "ZDimensionDataSetType",
                                            "DiameterDataSetType",
                                            "OrientationDataSetType",
                                            "HumidityRelativeDataSetType",
                                            "HumidityAbsoluteDataSetType",
                                            "HumiditySpecificDataSetType",
                                            "PressurizationRateDataSetType",
                                            "DecelerationDataSetType",
                                            "AssetUpdateRateDataSetType",
                                            "AngularDecelerationDataSetType",
                                            "ObservationUpdateRateDataSetType",
                                            "PressureAbsoluteDataSetType",
                                            "OpennessDataSetType",
                                            "ActiveAxesTableType",
                                            "ActuatorStateTableType",
                                            "AssetChangedTableType",
                                            "AssetRemovedTableType",
                                            "AvailabilityTableType",
                                            "AxisCouplingTableType",
                                            "AxisFeedrateOverrideTableType",
                                            "AxisInterlockTableType",
                                            "AxisStateTableType",
                                            "BlockTableType",
                                            "BlockCountTableType",
                                            "ChuckInterlockTableType",
                                            "ChuckStateTableType",
                                            "CodeTableType",
                                            "CompositionStateTableType",
                                            "ControllerModeTableType",
                                            "ControllerModeOverrideTableType",
                                            "CoupledAxesTableType",
                                            "DateCodeTableType",
                                            "DeviceUuidTableType",
                                            "DirectionTableType",
                                            "DoorStateTableType",
                                            "EmergencyStopTableType",
                                            "EndOfBarTableType",
                                            "EquipmentModeTableType",
                                            "ExecutionTableType",
                                            "FunctionalModeTableType",
                                            "HardnessTableType",
                                            "LineTableType",
                                            "LineLabelTableType",
                                            "LineNumberTableType",
                                            "MaterialTableType",
                                            "MaterialLayerTableType",
                                            "MessageTableType",
                                            "OperatorIdTableType",
                                            "PalletIdTableType",
                                            "PartCountTableType",
                                            "PartDetectTableType",
                                            "PartIdTableType",
                                            "PartNumberTableType",
                                            "PathFeedrateOverrideTableType",
                                            "PathModeTableType",
                                            "PowerStateTableType",
                                            "PowerStatusTableType",
                                            "ProcessTimeTableType",
                                            "ProgramTableType",
                                            "ProgramCommentTableType",
                                            "ProgramEditTableType",
                                            "ProgramEditNameTableType",
                                            "ProgramHeaderTableType",
                                            "ProgramLocationTableType",
                                            "ProgramLocationTypeTableType",
                                            "ProgramNestLevelTableType",
                                            "RotaryModeTableType",
                                            "RotaryVelocityOverrideTableType",
                                            "SerialNumberTableType",
                                            "SpindleInterlockTableType",
                                            "ToolAssetIdTableType",
                                            "ToolGroupTableType",
                                            "ToolIdTableType",
                                            "ToolNumberTableType",
                                            "ToolOffsetTableType",
                                            "UserTableType",
                                            "VariableTableType",
                                            "WaitStateTableType",
                                            "WireTableType",
                                            "WorkholdingIdTableType",
                                            "WorkOffsetTableType",
                                            "OperatingSystemTableType",
                                            "FirmwareTableType",
                                            "ApplicationTableType",
                                            "LibraryTableType",
                                            "HardwareTableType",
                                            "NetworkTableType",
                                            "RotationTableType",
                                            "TranslationTableType",
                                            "ProcessKindIdTableType",
                                            "PartStatusTableType",
                                            "AlarmLimitTableType",
                                            "ProcessAggregateIdTableType",
                                            "PartKindIdTableType",
                                            "AdapterURITableType",
                                            "DeviceRemovedTableType",
                                            "DeviceChangedTableType",
                                            "SpecificationLimitTableType",
                                            "ConnectionStatusTableType",
                                            "AdapterSoftwareVersionTableType",
                                            "SensorAttachmentTableType",
                                            "ControlLimitTableType",
                                            "DeviceAddedTableType",
                                            "MTConnectVersionTableType",
                                            "ProcessOccurrenceIdTableType",
                                            "PartGroupIdTableType",
                                            "PartUniqueIdTableType",
                                            "ActivationCountTableType",
                                            "DeactivationCountTableType",
                                            "TransferCountTableType",
                                            "LoadCountTableType",
                                            "PartProcessingStateTableType",
                                            "ProcessStateTableType",
                                            "ValveStateTableType",
                                            "LockStateTableType",
                                            "UnloadCountTableType",
                                            "CycleCountTableType",
                                            "OperatingModeTableType",
                                            "AssetCountTableType",
                                            "MaintenanceListTableType",
                                            "FixtureIdTableType",
                                            "PartCountTypeTableType",
                                            "MaterialFeedTableType",
                                            "MaterialChangeTableType",
                                            "MaterialRetractTableType",
                                            "MaterialLoadTableType",
                                            "MaterialUnloadTableType",
                                            "OpenChuckTableType",
                                            "OpenDoorTableType",
                                            "PartChangeTableType",
                                            "CloseDoorTableType",
                                            "CloseChuckTableType",
                                            "InterfaceStateTableType",
                                            "AccelerationTableType",
                                            "AccumulatedTimeTableType",
                                            "AmperageTableType",
                                            "AngleTableType",
                                            "AngularAccelerationTableType",
                                            "AngularVelocityTableType",
                                            "AxisFeedrateTableType",
                                            "CapacityFluidTableType",
                                            "CapacitySpatialTableType",
                                            "ConcentrationTableType",
                                            "ConductivityTableType",
                                            "CuttingSpeedTableType",
                                            "DensityTableType",
                                            "DepositionAccelerationVolumetricTableType",
                                            "DepositionDensityTableType",
                                            "DepositionMassTableType",
                                            "DepositionRateVolumetricTableType",
                                            "DepositionVolumeTableType",
                                            "DisplacementTableType",
                                            "ElectricalEnergyTableType",
                                            "EquipmentTimerTableType",
                                            "FillLevelTableType",
                                            "FlowTableType",
                                            "FrequencyTableType",
                                            "GlobalPositionTableType",
                                            "LengthTableType",
                                            "LevelTableType",
                                            "LinearForceTableType",
                                            "LoadTableType",
                                            "MassTableType",
                                            "PathFeedrateTableType",
                                            "PathFeedratePerRevolutionTableType",
                                            "PathPositionTableType",
                                            "PHTableType",
                                            "PositionTableType",
                                            "PowerFactorTableType",
                                            "PressureTableType",
                                            "ProcessTimerTableType",
                                            "ResistanceTableType",
                                            "RotaryVelocityTableType",
                                            "SoundLevelTableType",
                                            "SpindleSpeedTableType",
                                            "StrainTableType",
                                            "TemperatureTableType",
                                            "TensionTableType",
                                            "TiltTableType",
                                            "TorqueTableType",
                                            "VelocityTableType",
                                            "ViscosityTableType",
                                            "VoltageTableType",
                                            "VoltAmpereTableType",
                                            "VoltAmpereReactiveTableType",
                                            "VolumeFluidTableType",
                                            "VolumeSpatialTableType",
                                            "WattageTableType",
                                            "AmperageDCTableType",
                                            "AmperageACTableType",
                                            "VoltageACTableType",
                                            "VoltageDCTableType",
                                            "XDimensionTableType",
                                            "YDimensionTableType",
                                            "ZDimensionTableType",
                                            "DiameterTableType",
                                            "OrientationTableType",
                                            "HumidityRelativeTableType",
                                            "HumidityAbsoluteTableType",
                                            "HumiditySpecificTableType",
                                            "PressurizationRateTableType",
                                            "DecelerationTableType",
                                            "AssetUpdateRateTableType",
                                            "AngularDecelerationTableType",
                                            "ObservationUpdateRateTableType",
                                            "PressureAbsoluteTableType",
                                            "OpennessTableType")),
                            new MixedTypeNameGroup("EntryType",
                                    List.of("TableEntryType",
                                            "ActiveAxesTableEntryType",
                                            "ActuatorStateTableEntryType",
                                            "AssetChangedTableEntryType",
                                            "AssetRemovedTableEntryType",
                                            "AvailabilityTableEntryType",
                                            "AxisCouplingTableEntryType",
                                            "AxisFeedrateOverrideTableEntryType",
                                            "AxisInterlockTableEntryType",
                                            "AxisStateTableEntryType",
                                            "BlockTableEntryType",
                                            "BlockCountTableEntryType",
                                            "ChuckInterlockTableEntryType",
                                            "ChuckStateTableEntryType",
                                            "CodeTableEntryType",
                                            "CompositionStateTableEntryType",
                                            "ControllerModeTableEntryType",
                                            "ControllerModeOverrideTableEntryType",
                                            "CoupledAxesTableEntryType",
                                            "DateCodeTableEntryType",
                                            "DeviceUuidTableEntryType",
                                            "DirectionTableEntryType",
                                            "DoorStateTableEntryType",
                                            "EmergencyStopTableEntryType",
                                            "EndOfBarTableEntryType",
                                            "EquipmentModeTableEntryType",
                                            "ExecutionTableEntryType",
                                            "FunctionalModeTableEntryType",
                                            "HardnessTableEntryType",
                                            "LineTableEntryType",
                                            "LineLabelTableEntryType",
                                            "LineNumberTableEntryType",
                                            "MaterialTableEntryType",
                                            "MaterialLayerTableEntryType",
                                            "MessageTableEntryType",
                                            "OperatorIdTableEntryType",
                                            "PalletIdTableEntryType",
                                            "PartCountTableEntryType",
                                            "PartDetectTableEntryType",
                                            "PartIdTableEntryType",
                                            "PartNumberTableEntryType",
                                            "PathFeedrateOverrideTableEntryType",
                                            "PathModeTableEntryType",
                                            "PowerStateTableEntryType",
                                            "PowerStatusTableEntryType",
                                            "ProcessTimeTableEntryType",
                                            "ProgramTableEntryType",
                                            "ProgramCommentTableEntryType",
                                            "ProgramEditTableEntryType",
                                            "ProgramEditNameTableEntryType",
                                            "ProgramHeaderTableEntryType",
                                            "ProgramLocationTableEntryType",
                                            "ProgramLocationTypeTableEntryType",
                                            "ProgramNestLevelTableEntryType",
                                            "RotaryModeTableEntryType",
                                            "RotaryVelocityOverrideTableEntryType",
                                            "SerialNumberTableEntryType",
                                            "SpindleInterlockTableEntryType",
                                            "ToolAssetIdTableEntryType",
                                            "ToolGroupTableEntryType",
                                            "ToolIdTableEntryType",
                                            "ToolNumberTableEntryType",
                                            "ToolOffsetTableEntryType",
                                            "UserTableEntryType",
                                            "VariableTableEntryType",
                                            "WaitStateTableEntryType",
                                            "WireTableEntryType",
                                            "WorkholdingIdTableEntryType",
                                            "WorkOffsetTableEntryType",
                                            "OperatingSystemTableEntryType",
                                            "FirmwareTableEntryType",
                                            "ApplicationTableEntryType",
                                            "LibraryTableEntryType",
                                            "HardwareTableEntryType",
                                            "NetworkTableEntryType",
                                            "RotationTableEntryType",
                                            "TranslationTableEntryType",
                                            "ProcessKindIdTableEntryType",
                                            "PartStatusTableEntryType",
                                            "AlarmLimitTableEntryType",
                                            "ProcessAggregateIdTableEntryType",
                                            "PartKindIdTableEntryType",
                                            "AdapterURITableEntryType",
                                            "DeviceRemovedTableEntryType",
                                            "DeviceChangedTableEntryType",
                                            "SpecificationLimitTableEntryType",
                                            "ConnectionStatusTableEntryType",
                                            "AdapterSoftwareVersionTableEntryType",
                                            "SensorAttachmentTableEntryType",
                                            "ControlLimitTableEntryType",
                                            "DeviceAddedTableEntryType",
                                            "MTConnectVersionTableEntryType",
                                            "ProcessOccurrenceIdTableEntryType",
                                            "PartGroupIdTableEntryType",
                                            "PartUniqueIdTableEntryType",
                                            "ActivationCountTableEntryType",
                                            "DeactivationCountTableEntryType",
                                            "TransferCountTableEntryType",
                                            "LoadCountTableEntryType",
                                            "PartProcessingStateTableEntryType",
                                            "ProcessStateTableEntryType",
                                            "ValveStateTableEntryType",
                                            "LockStateTableEntryType",
                                            "UnloadCountTableEntryType",
                                            "CycleCountTableEntryType",
                                            "OperatingModeTableEntryType",
                                            "AssetCountTableEntryType",
                                            "MaintenanceListTableEntryType",
                                            "FixtureIdTableEntryType",
                                            "PartCountTypeTableEntryType",
                                            "MaterialFeedTableEntryType",
                                            "MaterialChangeTableEntryType",
                                            "MaterialRetractTableEntryType",
                                            "MaterialLoadTableEntryType",
                                            "MaterialUnloadTableEntryType",
                                            "OpenChuckTableEntryType",
                                            "OpenDoorTableEntryType",
                                            "PartChangeTableEntryType",
                                            "CloseDoorTableEntryType",
                                            "CloseChuckTableEntryType",
                                            "InterfaceStateTableEntryType",
                                            "AccelerationTableEntryType",
                                            "AccumulatedTimeTableEntryType",
                                            "AmperageTableEntryType",
                                            "AngleTableEntryType",
                                            "AngularAccelerationTableEntryType",
                                            "AngularVelocityTableEntryType",
                                            "AxisFeedrateTableEntryType",
                                            "CapacityFluidTableEntryType",
                                            "CapacitySpatialTableEntryType",
                                            "ConcentrationTableEntryType",
                                            "ConductivityTableEntryType",
                                            "CuttingSpeedTableEntryType",
                                            "DensityTableEntryType",
                                            "DepositionAccelerationVolumetricTableEntryType",
                                            "DepositionDensityTableEntryType",
                                            "DepositionMassTableEntryType",
                                            "DepositionRateVolumetricTableEntryType",
                                            "DepositionVolumeTableEntryType",
                                            "DisplacementTableEntryType",
                                            "ElectricalEnergyTableEntryType",
                                            "EquipmentTimerTableEntryType",
                                            "FillLevelTableEntryType",
                                            "FlowTableEntryType",
                                            "FrequencyTableEntryType",
                                            "GlobalPositionTableEntryType",
                                            "LengthTableEntryType",
                                            "LevelTableEntryType",
                                            "LinearForceTableEntryType",
                                            "LoadTableEntryType",
                                            "MassTableEntryType",
                                            "PathFeedrateTableEntryType",
                                            "PathFeedratePerRevolutionTableEntryType",
                                            "PathPositionTableEntryType",
                                            "PHTableEntryType",
                                            "PositionTableEntryType",
                                            "PowerFactorTableEntryType",
                                            "PressureTableEntryType",
                                            "ProcessTimerTableEntryType",
                                            "ResistanceTableEntryType",
                                            "RotaryVelocityTableEntryType",
                                            "SoundLevelTableEntryType",
                                            "SpindleSpeedTableEntryType",
                                            "StrainTableEntryType",
                                            "TemperatureTableEntryType",
                                            "TensionTableEntryType",
                                            "TiltTableEntryType",
                                            "TorqueTableEntryType",
                                            "VelocityTableEntryType",
                                            "ViscosityTableEntryType",
                                            "VoltageTableEntryType",
                                            "VoltAmpereTableEntryType",
                                            "VoltAmpereReactiveTableEntryType",
                                            "VolumeFluidTableEntryType",
                                            "VolumeSpatialTableEntryType",
                                            "WattageTableEntryType",
                                            "AmperageDCTableEntryType",
                                            "AmperageACTableEntryType",
                                            "VoltageACTableEntryType",
                                            "VoltageDCTableEntryType",
                                            "XDimensionTableEntryType",
                                            "YDimensionTableEntryType",
                                            "ZDimensionTableEntryType",
                                            "DiameterTableEntryType",
                                            "OrientationTableEntryType",
                                            "HumidityRelativeTableEntryType",
                                            "HumidityAbsoluteTableEntryType",
                                            "HumiditySpecificTableEntryType",
                                            "PressurizationRateTableEntryType",
                                            "DecelerationTableEntryType",
                                            "AssetUpdateRateTableEntryType",
                                            "AngularDecelerationTableEntryType",
                                            "ObservationUpdateRateTableEntryType",
                                            "PressureAbsoluteTableEntryType",
                                            "OpennessTableEntryType"))));
    private final boolean schemaFound;

    public MtConnectSchemaPatchTest() {
        schemaFound = SCHEMA_FOLDER_FILE.exists();
    }

    @BeforeEach
    public void setUp() {
        if (schemaFound) {
            assertThat(SCHEMA_FOLDER_FILE.isDirectory()).isTrue();
            String os = System.getProperty("os.name").toLowerCase();
            if (!os.contains("linux") && !os.contains("mac")) {
                fail("Only Linux and MacOS are supported.");
            }
        }
    }

    protected @NotNull String getPatchCommand(final @NotNull MtConnectSchema schema, final String fileName) {
        StringBuilder sb = new StringBuilder();
        sb.append("../jaxb-ri/bin/xjc.sh ")
                .append("-classpath \"${CLASSPATH}:../xerces-2_12_2-xml-schema-1.1/xml-apis.jar:../xerces-2_12_2-xml-schema-1.1/xercesImpl.jar\" ")
                .append("-d ../src/main/java ")
                .append("-p com.hivemq.edge.adapters.mtconnect.schemas.")
                .append(schema.getType().name().toLowerCase())
                .append(".")
                .append(schema.getType().name().toLowerCase())
                .append("_")
                .append(schema.getMajorVersion())
                .append("_")
                .append(schema.getMinorVersion())
                .append(" ");
        if (schema.getType() == MtConnectSchemaType.Devices) {
            if ((schema.getMajorVersion() == 1 && schema.getMinorVersion() >= 5) || schema.getMajorVersion() == 2) {
                sb.append("-b ").append(getPatchSchemaFileName(schema)).append(" ");
            }
        }
        sb.append(fileName);
        return sb.toString();
    }

    protected @NotNull String getPatchSchemaFileName(final @NotNull MtConnectSchema schema) {
        return "patch-" +
                schema.getType().name().toLowerCase() +
                "-" +
                schema.getMajorVersion() +
                "-" +
                schema.getMinorVersion() +
                ".xml";
    }

    protected @NotNull String getSchemaFileName(final @NotNull MtConnectSchema schema) {
        return "MTConnect" +
                schema.getType().name() +
                "_" +
                schema.getMajorVersion() +
                "." +
                schema.getMinorVersion() +
                ".xsd";
    }

    @Test
    public void generatePatchScript() throws IOException {
        if (!schemaFound) {
            return;
        }
        final String originalContent;
        if (PATCH_SCRIPT_FILE.exists()) {
            assertThat(PATCH_SCRIPT_FILE.isFile()).isTrue();
            assertThat(PATCH_SCRIPT_FILE.canRead()).isTrue();
            assertThat(PATCH_SCRIPT_FILE.canWrite()).isTrue();
            originalContent = Files.readString(PATCH_SCRIPT_FILE.toPath(), StandardCharsets.UTF_8);
        } else {
            originalContent = null;
        }
        final StringBuilder sb = new StringBuilder();
        Stream.of(MtConnectSchema.values()).forEach(schema -> {
            sb.append("# ")
                    .append(schema.getType().name())
                    .append(" ")
                    .append(schema.getMajorVersion())
                    .append(".")
                    .append(schema.getMinorVersion())
                    .append("\n");
            final var typeNameGroups = MIXED_SCHEMA_TO_TYPE_NAME_GROUPS_MAP.get(schema);
            if (typeNameGroups != null) {
                assertThat(typeNameGroups.size()).isGreaterThan(0);
                sb.append(getPatchCommand(schema, "patched-" + getSchemaFileName(schema))).append("\n");
                final String relativePath = "../src/main/java/com/hivemq/edge/adapters/mtconnect/schemas/" +
                        schema.getType().name().toLowerCase() +
                        "/" +
                        schema.getType().name().toLowerCase() +
                        "_" +
                        schema.getMajorVersion() +
                        "_" +
                        schema.getMinorVersion();
                sb.append("mkdir -p ").append(relativePath).append("/backup\n");
                sb.append("cp -f");
                typeNameGroups.stream()
                        .flatMap(typeNameGroup -> typeNameGroup.derivedTypeNames.stream())
                        .forEach(typeName -> sb.append(" ")
                                .append(relativePath)
                                .append("/")
                                .append(typeName)
                                .append(".java"));
                sb.append(" ").append(relativePath).append("/backup\n");
                sb.append(getPatchCommand(schema, getSchemaFileName(schema))).append("\n");
                sb.append("cp -f ").append(relativePath).append("/backup/*.java ").append(relativePath).append("\n");
                sb.append("rm -rf ").append(relativePath).append("/backup\n");
            } else {
                sb.append(getPatchCommand(schema, getSchemaFileName(schema))).append("\n");
            }
        });
        String content = sb.toString();
        if (!Objects.equals(originalContent, content)) {
            Files.writeString(PATCH_SCRIPT_FILE.toPath(), content, StandardCharsets.UTF_8);
            Files.setPosixFilePermissions(PATCH_SCRIPT_FILE.toPath(),
                    Set.of(PosixFilePermission.OWNER_WRITE,
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.GROUP_READ,
                            PosixFilePermission.OTHERS_READ,
                            PosixFilePermission.OWNER_EXECUTE,
                            PosixFilePermission.GROUP_EXECUTE,
                            PosixFilePermission.OTHERS_EXECUTE));
        }
    }

    @Test
    public void generatePatchJaxbForDevices() throws IOException {
        if (!schemaFound) {
            return;
        }
        for (var schema : Stream.of(MtConnectSchema.values())
                .filter(schema -> schema.getType() == MtConnectSchemaType.Devices)
                .filter(schema -> (schema.getMajorVersion() == 1 && schema.getMinorVersion() >= 5) ||
                        schema.getMajorVersion() == 2)
                .toList()) {
            final File patchJaxbFile = new File(SCHEMA_FOLDER_FILE, getPatchSchemaFileName(schema));
            final String originalContent;
            if (patchJaxbFile.exists()) {
                assertThat(patchJaxbFile.isFile()).isTrue();
                assertThat(patchJaxbFile.canRead()).isTrue();
                assertThat(patchJaxbFile.canWrite()).isTrue();
                originalContent = Files.readString(patchJaxbFile.toPath(), StandardCharsets.UTF_8);
            } else {
                originalContent = null;
            }
            final StringBuilder sb = new StringBuilder();
            sb.append("""
                            <?xml version='1.0' encoding='UTF-8'?>
                            <bindings xmlns="http://java.sun.com/xml/ns/jaxb"
                                xmlns:xsi="http://www.w3.org/2000/10/XMLSchema-instance"
                                xmlns:xs="http://www.w3.org/2001/XMLSchema" version="2.1">
                                <bindings schemaLocation="MTConnectDevices_""".trim())
                    .append(schema.getMajorVersion())
                    .append(".")
                    .append(schema.getMinorVersion())
                    .append("""
                                    .xsd" version="1.0">
                                    <bindings node="//xs:complexType[@name='DeviceRelationshipType']">
                                        <bindings node=".//xs:attribute[@ref='xlink:type']">
                                            <property name="typeOfDeviceRelationship"/>
                                        </bindings>
                                    </bindings>
                                </bindings>
                            </bindings>""".trim());
            if (!Objects.equals(originalContent, sb.toString())) {
                Files.writeString(patchJaxbFile.toPath(), sb.toString(), StandardCharsets.UTF_8);
            }
        }
    }

    @Test
    public void generatePatchJaxbForStreams() throws Exception {
        if (!schemaFound) {
            return;
        }
        for (final var entry : MIXED_SCHEMA_TO_TYPE_NAME_GROUPS_MAP.entrySet()) {
            final var schema = entry.getKey();
            final var typeNameGroups = entry.getValue();
            final File schemaFile = new File(SCHEMA_FOLDER_FILE, getSchemaFileName(schema));
            assertThat(schemaFile.exists()).isTrue();
            assertThat(schemaFile.isFile()).isTrue();
            assertThat(schemaFile.canRead()).isTrue();
            assertThat(schemaFile.canWrite()).isTrue();
            final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            final Document document = documentBuilder.parse(schemaFile);
            document.getDocumentElement().normalize();
            final XPathFactory xPathfactory = XPathFactory.newInstance();
            final XPath xPath = xPathfactory.newXPath();
            for (final var typeNameGroup : typeNameGroups) {
                for (final String typeName : typeNameGroup.getAllTypeNames()) {
                    final XPathExpression xPathExpression =
                            xPath.compile("//*[local-name()='complexType' and @mixed='true' and @name='" +
                                    typeName +
                                    "']");
                    final NodeList nodeList = (NodeList) xPathExpression.evaluate(document, XPathConstants.NODESET);
                    assertThat(nodeList).isNotNull();
                    final int length = nodeList.getLength();
                    assertThat(length).isOne();
                    assertThat(nodeList.item(0)).isInstanceOf(Element.class);
                    final Element element = (Element) nodeList.item(0);
                    assertThat(element.getAttribute("mixed")).isEqualTo("true");
                    element.setAttribute("mixed", "false");
                }
            }
            final TransformerFactory transformerFactory = TransformerFactory.newInstance();
            final Transformer transformer = transformerFactory.newTransformer();
            try (final StringWriter stringWriter = new StringWriter()) {
                final StreamResult streamResult = new StreamResult(stringWriter);
                transformer.transform(new DOMSource(document), streamResult);
                final String patchedContent = stringWriter.getBuffer().toString();
                final File patchedSchemaFile = new File(SCHEMA_FOLDER_FILE, "patched-" + getSchemaFileName(schema));
                final String originalContent;
                if (patchedSchemaFile.exists()) {
                    assertThat(patchedSchemaFile.isFile()).isTrue();
                    assertThat(patchedSchemaFile.canRead()).isTrue();
                    assertThat(patchedSchemaFile.canWrite()).isTrue();
                    originalContent = Files.readString(patchedSchemaFile.toPath(), StandardCharsets.UTF_8);
                } else {
                    originalContent = null;
                }
                if (!Objects.equals(originalContent, patchedContent)) {
                    Files.writeString(patchedSchemaFile.toPath(), patchedContent, StandardCharsets.UTF_8);
                }
            }
        }
    }

    protected record MixedTypeNameGroup(String baseTypeName, List<String> derivedTypeNames) {
        public @NotNull List<String> getAllTypeNames() {
            return Stream.concat(Stream.of(baseTypeName), derivedTypeNames.stream()).toList();
        }
    }
}
