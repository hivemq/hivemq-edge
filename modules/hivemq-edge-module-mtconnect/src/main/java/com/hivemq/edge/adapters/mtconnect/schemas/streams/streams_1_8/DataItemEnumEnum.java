//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.5 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_8;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * The types of measurements available
 * 
 * <p>Java class for DataItemEnumEnum</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>{@code
 * <simpleType name="DataItemEnumEnum">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="ACCELERATION"/>
 *     <enumeration value="ACCUMULATED_TIME"/>
 *     <enumeration value="AMPERAGE"/>
 *     <enumeration value="ANGLE"/>
 *     <enumeration value="ANGULAR_ACCELERATION"/>
 *     <enumeration value="ANGULAR_VELOCITY"/>
 *     <enumeration value="AXIS_FEEDRATE"/>
 *     <enumeration value="CAPACITY_FLUID"/>
 *     <enumeration value="CAPACITY_SPATIAL"/>
 *     <enumeration value="CLOCK_TIME"/>
 *     <enumeration value="CONCENTRATION"/>
 *     <enumeration value="CONDUCTIVITY"/>
 *     <enumeration value="CUTTING_SPEED"/>
 *     <enumeration value="DENSITY"/>
 *     <enumeration value="DEPOSITION_ACCELERATION_VOLUMETRIC"/>
 *     <enumeration value="DEPOSITION_DENSITY"/>
 *     <enumeration value="DEPOSITION_MASS"/>
 *     <enumeration value="DEPOSITION_RATE_VOLUMETRIC"/>
 *     <enumeration value="DEPOSITION_VOLUME"/>
 *     <enumeration value="DISPLACEMENT"/>
 *     <enumeration value="ELECTRICAL_ENERGY"/>
 *     <enumeration value="EQUIPMENT_TIMER"/>
 *     <enumeration value="FILL_LEVEL"/>
 *     <enumeration value="FLOW"/>
 *     <enumeration value="FREQUENCY"/>
 *     <enumeration value="GLOBAL_POSITION"/>
 *     <enumeration value="LENGTH"/>
 *     <enumeration value="LEVEL"/>
 *     <enumeration value="LINEAR_FORCE"/>
 *     <enumeration value="LOAD"/>
 *     <enumeration value="MASS"/>
 *     <enumeration value="PATH_FEEDRATE"/>
 *     <enumeration value="PATH_FEEDRATE_PER_REVOLUTION"/>
 *     <enumeration value="PATH_POSITION"/>
 *     <enumeration value="PH"/>
 *     <enumeration value="POSITION"/>
 *     <enumeration value="POWER_FACTOR"/>
 *     <enumeration value="PRESSURE"/>
 *     <enumeration value="PROCESS_TIMER"/>
 *     <enumeration value="RESISTANCE"/>
 *     <enumeration value="ROTARY_VELOCITY"/>
 *     <enumeration value="SOUND_LEVEL"/>
 *     <enumeration value="SPINDLE_SPEED"/>
 *     <enumeration value="STRAIN"/>
 *     <enumeration value="TEMPERATURE"/>
 *     <enumeration value="TENSION"/>
 *     <enumeration value="TILT"/>
 *     <enumeration value="TORQUE"/>
 *     <enumeration value="VELOCITY"/>
 *     <enumeration value="VISCOSITY"/>
 *     <enumeration value="VOLTAGE"/>
 *     <enumeration value="VOLT_AMPERE"/>
 *     <enumeration value="VOLT_AMPERE_REACTIVE"/>
 *     <enumeration value="VOLUME_FLUID"/>
 *     <enumeration value="VOLUME_SPATIAL"/>
 *     <enumeration value="WATTAGE"/>
 *     <enumeration value="AMPERAGE_AC"/>
 *     <enumeration value="AMPERAGE_DC"/>
 *     <enumeration value="VOLTAGE_AC"/>
 *     <enumeration value="VOLTAGE_DC"/>
 *     <enumeration value="X_DIMENSION"/>
 *     <enumeration value="Y_DIMENSION"/>
 *     <enumeration value="Z_DIMENSION"/>
 *     <enumeration value="DIAMETER"/>
 *     <enumeration value="ORIENTATION"/>
 *     <enumeration value="HUMIDITY_RELATIVE"/>
 *     <enumeration value="HUMIDITY_ABSOLUTE"/>
 *     <enumeration value="HUMIDITY_SPECIFIC"/>
 *     <enumeration value="OBSERVATION_UPDATE_RATE"/>
 *     <enumeration value="ASSET_UPDATE_RATE"/>
 *     <enumeration value="PRESSURIZATION_RATE"/>
 *     <enumeration value="DECELERATION"/>
 *     <enumeration value="ANGULAR_DECELERATION"/>
 *     <enumeration value="PRESSURE_ABSOLUTE"/>
 *     <enumeration value="ACTIVE_AXES"/>
 *     <enumeration value="ACTUATOR_STATE"/>
 *     <enumeration value="ALARM"/>
 *     <enumeration value="ASSET_CHANGED"/>
 *     <enumeration value="ASSET_REMOVED"/>
 *     <enumeration value="AVAILABILITY"/>
 *     <enumeration value="AXIS_COUPLING"/>
 *     <enumeration value="AXIS_FEEDRATE_OVERRIDE"/>
 *     <enumeration value="AXIS_INTERLOCK"/>
 *     <enumeration value="AXIS_STATE"/>
 *     <enumeration value="BLOCK"/>
 *     <enumeration value="BLOCK_COUNT"/>
 *     <enumeration value="CHUCK_INTERLOCK"/>
 *     <enumeration value="CHUCK_STATE"/>
 *     <enumeration value="CLOSE_CHUCK"/>
 *     <enumeration value="CLOSE_DOOR"/>
 *     <enumeration value="CODE"/>
 *     <enumeration value="COMPOSITION_STATE"/>
 *     <enumeration value="CONTROLLER_MODE"/>
 *     <enumeration value="CONTROLLER_MODE_OVERRIDE"/>
 *     <enumeration value="COUPLED_AXES"/>
 *     <enumeration value="DATE_CODE"/>
 *     <enumeration value="DEVICE_UUID"/>
 *     <enumeration value="DIRECTION"/>
 *     <enumeration value="DOOR_STATE"/>
 *     <enumeration value="EMERGENCY_STOP"/>
 *     <enumeration value="END_OF_BAR"/>
 *     <enumeration value="EQUIPMENT_MODE"/>
 *     <enumeration value="EXECUTION"/>
 *     <enumeration value="FUNCTIONAL_MODE"/>
 *     <enumeration value="HARDNESS"/>
 *     <enumeration value="INTERFACE_STATE"/>
 *     <enumeration value="LINE"/>
 *     <enumeration value="LINE_LABEL"/>
 *     <enumeration value="LINE_NUMBER"/>
 *     <enumeration value="MATERIAL"/>
 *     <enumeration value="MATERIAL_CHANGE"/>
 *     <enumeration value="MATERIAL_FEED"/>
 *     <enumeration value="MATERIAL_LAYER"/>
 *     <enumeration value="MATERIAL_LOAD"/>
 *     <enumeration value="MATERIAL_RETRACT"/>
 *     <enumeration value="MATERIAL_UNLOAD"/>
 *     <enumeration value="MESSAGE"/>
 *     <enumeration value="OPEN_CHUCK"/>
 *     <enumeration value="OPEN_DOOR"/>
 *     <enumeration value="OPERATOR_ID"/>
 *     <enumeration value="PALLET_ID"/>
 *     <enumeration value="PART_CHANGE"/>
 *     <enumeration value="PART_COUNT"/>
 *     <enumeration value="PART_DETECT"/>
 *     <enumeration value="PART_ID"/>
 *     <enumeration value="PART_NUMBER"/>
 *     <enumeration value="PATH_FEEDRATE_OVERRIDE"/>
 *     <enumeration value="PATH_MODE"/>
 *     <enumeration value="POWER_STATE"/>
 *     <enumeration value="POWER_STATUS"/>
 *     <enumeration value="PROCESS_TIME"/>
 *     <enumeration value="PROGRAM"/>
 *     <enumeration value="PROGRAM_COMMENT"/>
 *     <enumeration value="PROGRAM_EDIT"/>
 *     <enumeration value="PROGRAM_EDIT_NAME"/>
 *     <enumeration value="PROGRAM_HEADER"/>
 *     <enumeration value="PROGRAM_LOCATION"/>
 *     <enumeration value="PROGRAM_LOCATION_TYPE"/>
 *     <enumeration value="PROGRAM_NEST_LEVEL"/>
 *     <enumeration value="ROTARY_MODE"/>
 *     <enumeration value="ROTARY_VELOCITY_OVERRIDE"/>
 *     <enumeration value="SERIAL_NUMBER"/>
 *     <enumeration value="SPINDLE_INTERLOCK"/>
 *     <enumeration value="TOOL_ASSET_ID"/>
 *     <enumeration value="TOOL_GROUP"/>
 *     <enumeration value="TOOL_ID"/>
 *     <enumeration value="TOOL_NUMBER"/>
 *     <enumeration value="TOOL_OFFSET"/>
 *     <enumeration value="USER"/>
 *     <enumeration value="VARIABLE"/>
 *     <enumeration value="WAIT_STATE"/>
 *     <enumeration value="WIRE"/>
 *     <enumeration value="WORKHOLDING_ID"/>
 *     <enumeration value="WORK_OFFSET"/>
 *     <enumeration value="OPERATING_SYSTEM"/>
 *     <enumeration value="FIRMWARE"/>
 *     <enumeration value="APPLICATION"/>
 *     <enumeration value="LIBRARY"/>
 *     <enumeration value="HARDWARE"/>
 *     <enumeration value="NETWORK"/>
 *     <enumeration value="ROTATION"/>
 *     <enumeration value="TRANSLATION"/>
 *     <enumeration value="DEVICE_ADDED"/>
 *     <enumeration value="DEVICE_REMOVED"/>
 *     <enumeration value="DEVICE_CHANGED"/>
 *     <enumeration value="CONNECTION_STATUS"/>
 *     <enumeration value="ADAPTER_SOFTWARE_VERSION"/>
 *     <enumeration value="ADAPTER_URI"/>
 *     <enumeration value="MTCONNECT_VERSION"/>
 *     <enumeration value="SENSOR_ATTACHMENT"/>
 *     <enumeration value="PART_STATUS"/>
 *     <enumeration value="PROCESS_OCCURRENCE_ID"/>
 *     <enumeration value="PROCESS_AGGREGATE_ID"/>
 *     <enumeration value="PROCESS_KIND_ID"/>
 *     <enumeration value="PART_GROUP_ID"/>
 *     <enumeration value="PART_KIND_ID"/>
 *     <enumeration value="PART_UNIQUE_ID"/>
 *     <enumeration value="CONTROL_LIMIT"/>
 *     <enumeration value="SPECIFICATION_LIMIT"/>
 *     <enumeration value="ALARM_LIMIT"/>
 *     <enumeration value="LOAD_COUNT"/>
 *     <enumeration value="UNLOAD_COUNT"/>
 *     <enumeration value="TRANSFER_COUNT"/>
 *     <enumeration value="ACTIVATION_COUNT"/>
 *     <enumeration value="DEACTIVATION_COUNT"/>
 *     <enumeration value="CYCLE_COUNT"/>
 *     <enumeration value="VALVE_STATE"/>
 *     <enumeration value="LOCK_STATE"/>
 *     <enumeration value="PROCESS_STATE"/>
 *     <enumeration value="PART_PROCESSING_STATE"/>
 *     <enumeration value="COMMUNICATIONS"/>
 *     <enumeration value="DATA_RANGE"/>
 *     <enumeration value="LOGIC_PROGRAM"/>
 *     <enumeration value="MOTION_PROGRAM"/>
 *     <enumeration value="SYSTEM"/>
 *     <enumeration value="ACTUATOR"/>
 *     <enumeration value="VARIABLE"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@com.fasterxml.jackson.annotation.JsonTypeName(value = "DataItemEnumEnum")
@XmlType(name = "DataItemEnumEnum")
@XmlEnum
public enum DataItemEnumEnum {


    /**
     * Positive rate of change of velocity.
     * 
     */
    ACCELERATION,

    /**
     * The measurement of accumulated time for an activity or event.
     * 
     */
    ACCUMULATED_TIME,

    /**
     * **DEPRECATED** in *Version 1.6*. Replaced by `AMPERAGE_AC` and
     *             `AMPERAGE_DC`. The measurement of electrical current.
     * 
     */
    AMPERAGE,

    /**
     * The measurement of angular position.
     * 
     */
    ANGLE,

    /**
     * Positive rate of change of angular velocity.
     * 
     */
    ANGULAR_ACCELERATION,

    /**
     * The measurement of the rate of change of angular position.
     * 
     */
    ANGULAR_VELOCITY,

    /**
     * The measurement of the feedrate of a linear axis.
     * 
     */
    AXIS_FEEDRATE,

    /**
     * The fluid capacity of an object or container.
     * 
     */
    CAPACITY_FLUID,

    /**
     * The geometric capacity of an object or container.
     * 
     */
    CAPACITY_SPATIAL,

    /**
     * The value provided by a timing device at a specific point in time.
     * 
     */
    CLOCK_TIME,

    /**
     * The measurement of the percentage of one component within a mixture
     *             of components
     * 
     */
    CONCENTRATION,

    /**
     * The measurement of the ability of a material to conduct electricity.
     * 
     */
    CONDUCTIVITY,

    /**
     * The speed difference (relative velocity) between the cutting
     *             mechanism and the surface of the workpiece it is operating on.
     * 
     */
    CUTTING_SPEED,

    /**
     * The volumetric mass of a material per unit volume of that material.
     * 
     */
    DENSITY,

    /**
     * The rate of change in spatial volume of material deposited in an
     *             additive manufacturing process.
     * 
     */
    DEPOSITION_ACCELERATION_VOLUMETRIC,

    /**
     * The density of the material deposited in an additive manufacturing
     *             process per unit of volume.
     * 
     */
    DEPOSITION_DENSITY,

    /**
     * The mass of the material deposited in an additive manufacturing
     *             process.
     * 
     */
    DEPOSITION_MASS,

    /**
     * The rate at which a spatial volume of material is deposited in an
     *             additive manufacturing process.
     * 
     */
    DEPOSITION_RATE_VOLUMETRIC,

    /**
     * The spatial volume of material to be deposited in an additive
     *             manufacturing process.
     * 
     */
    DEPOSITION_VOLUME,

    /**
     * The measurement of the change in position of an object.
     * 
     */
    DISPLACEMENT,

    /**
     * The value of the {{block(Wattage)}} used or generated by a component
     *             over an interval of time.
     * 
     */
    ELECTRICAL_ENERGY,

    /**
     * The measurement of the amount of time a piece of equipment or a
     *             sub-part of a piece of equipment has performed specific activities.
     * 
     */
    EQUIPMENT_TIMER,

    /**
     * The measurement of the amount of a substance remaining compared to
     *             the planned maximum amount of that substance.
     * 
     */
    FILL_LEVEL,

    /**
     * The measurement of the rate of flow of a fluid.
     * 
     */
    FLOW,

    /**
     * The measurement of the number of occurrences of a repeating event
     *             per unit time.
     * 
     */
    FREQUENCY,

    /**
     * **DEPRECATED** in Version 1.1
     * 
     */
    GLOBAL_POSITION,

    /**
     * The measurement of the length of an object.
     * 
     */
    LENGTH,

    /**
     * **DEPRECATED** in *Version 1.2*. See `FILL_LEVEL`. Represents the
     *             level of a resource.
     * 
     */
    LEVEL,

    /**
     * A {{term(Force)}} applied to a mass in one direction only.
     * 
     */
    LINEAR_FORCE,

    /**
     * The measurement of the actual versus the standard rating of a piece
     *             of equipment.
     * 
     */
    LOAD,

    /**
     * The measurement of the mass of an object(s) or an amount of
     *             material.
     * 
     */
    MASS,

    /**
     * The measurement of the feedrate for the axes, or a single axis,
     *             associated with a {{block(Path)}} component-a vector.
     * 
     */
    PATH_FEEDRATE,

    /**
     * The feedrate for the axes, or a single axis.
     * 
     */
    PATH_FEEDRATE_PER_REVOLUTION,

    /**
     * A measured or calculated position of a control point associated with
     *             a {{block(Controller)}} element, or {{block(Path)}} element if
     *             provided, of a piece of equipment.
     * 
     */
    PATH_POSITION,

    /**
     * A measure of the acidity or alkalinity of a solution.
     * 
     */
    PH,

    /**
     * A measured or calculated position of a {{block(Component)}} element
     *             as reported by a piece of equipment.
     * 
     */
    POSITION,

    /**
     * The measurement of the ratio of real power flowing to a load to the
     *             apparent power in that AC circuit.
     * 
     */
    POWER_FACTOR,

    /**
     * The force per unit area measured relative to atmospheric pressure.
     *             Commonly referred to as gauge pressure.
     * 
     */
    PRESSURE,

    /**
     * The measurement of the amount of time a piece of equipment has
     *             performed different types of activities associated with the process
     *             being performed at that piece of equipment.
     * 
     */
    PROCESS_TIMER,

    /**
     * The measurement of the degree to which a substance opposes the
     *             passage of an electric current.
     * 
     */
    RESISTANCE,

    /**
     * The measurement of the rotational speed of a rotary axis.
     * 
     */
    ROTARY_VELOCITY,

    /**
     * The measurement of a sound level or sound pressure level relative to
     *             atmospheric pressure.
     * 
     */
    SOUND_LEVEL,

    /**
     * **DEPRECATED** in *Version 1.2*. Replaced by `ROTARY_VELOCITY`. The
     *             rotational speed of the rotary axis.
     * 
     */
    SPINDLE_SPEED,

    /**
     * The measurement of the amount of deformation per unit length of an
     *             object when a load is applied.
     * 
     */
    STRAIN,

    /**
     * The measurement of temperature.
     * 
     */
    TEMPERATURE,

    /**
     * The measurement of a force that stretches or elongates an object.
     * 
     */
    TENSION,

    /**
     * The measurement of angular displacement.
     * 
     */
    TILT,

    /**
     * The measurement of the turning force exerted on an object or by an
     *             object.
     * 
     */
    TORQUE,

    /**
     * The measurement of the rate of change of position of a
     *             {{block(Component)}}.
     * 
     */
    VELOCITY,

    /**
     * The measurement of a fluids resistance to flow.
     * 
     */
    VISCOSITY,

    /**
     * **DEPRECATED** in *Version 1.6*. Replaced by `VOLTAGE_AC` and
     *             `VOLTAGE_DC`. The measurement of electrical potential between two
     *             points.
     * 
     */
    VOLTAGE,

    /**
     * The measurement of the apparent power in an electrical circuit,
     *             equal to the product of root-mean-square (RMS) voltage and RMS
     *             current (commonly referred to as VA).
     * 
     */
    VOLT_AMPERE,

    /**
     * The measurement of reactive power in an AC electrical circuit
     *             (commonly referred to as VAR).
     * 
     */
    VOLT_AMPERE_REACTIVE,

    /**
     * The fluid volume of an object or container.
     * 
     */
    VOLUME_FLUID,

    /**
     * The geometric volume of an object or container.
     * 
     */
    VOLUME_SPATIAL,

    /**
     * The measurement of power flowing through or dissipated by an
     *             electrical circuit or piece of equipment.
     * 
     */
    WATTAGE,

    /**
     * The measurement of an electrical current that reverses direction at
     *             regular short intervals.
     * 
     */
    AMPERAGE_AC,

    /**
     * The measurement of an electric current flowing in one direction
     *             only.
     * 
     */
    AMPERAGE_DC,

    /**
     * The measurement of the electrical potential between two points in an
     *             electrical circuit in which the current periodically reverses
     *             direction.
     * 
     */
    VOLTAGE_AC,

    /**
     * The measurement of the electrical potential between two points in an
     *             electrical circuit in which the current is unidirectional.
     * 
     */
    VOLTAGE_DC,

    /**
     * Measured dimension of an entity relative to the X direction of the
     *             referenced coordinate system.
     * 
     */
    X_DIMENSION,

    /**
     * Measured dimension of an entity relative to the Y direction of the
     *             referenced coordinate system.
     * 
     */
    Y_DIMENSION,

    /**
     * Measured dimension of an entity relative to the Z direction of the
     *             referenced coordinate system.
     * 
     */
    Z_DIMENSION,

    /**
     * The measured dimension of a diameter.
     * 
     */
    DIAMETER,

    /**
     * A measured or calculated orientation of a plane or vector relative
     *             to a cartesian coordinate system.
     * 
     */
    ORIENTATION,

    /**
     * The amount of water vapor present expressed as a percent to reach
     *             saturation at the same temperature.
     * 
     */
    HUMIDITY_RELATIVE,

    /**
     * The amount of water vapor expressed in grams per cubic meter.
     * 
     */
    HUMIDITY_ABSOLUTE,

    /**
     * The ratio of the water vapor present over the total weight of the
     *             water vapor and air present expressed as a percent.
     * 
     */
    HUMIDITY_SPECIFIC,

    /**
     * The average rate of change of values for data items in the MTConnect
     *             streams. The average is computed over a rolling window defined by
     *             the implementation.
     * 
     */
    OBSERVATION_UPDATE_RATE,

    /**
     * The average rate of change of values for assets in the MTConnect
     *             streams. The average is computed over a rolling window defined by
     *             the implementation.
     * 
     */
    ASSET_UPDATE_RATE,

    /**
     * The change of pressure per unit time.
     * 
     */
    PRESSURIZATION_RATE,

    /**
     * Negative rate of change of velocity.
     * 
     */
    DECELERATION,

    /**
     * Negative rate of change of angular velocity.
     * 
     */
    ANGULAR_DECELERATION,

    /**
     * The force per unit area measured relative to a vacuum.
     * 
     */
    PRESSURE_ABSOLUTE,

    /**
     * The set of axes currently associated with a {{block(Path)}} or
     *             {{block(Controller)}} {{term(Structural Element)}}.
     * 
     */
    ACTIVE_AXES,

    /**
     * Represents the operational state of an apparatus for moving or
     *             controlling a mechanism or system.
     * 
     */
    ACTUATOR_STATE,

    /**
     * **DEPRECATED:** Replaced with {{block(CONDITION)}} category data
     *             items in Version 1.1.0.
     * 
     */
    ALARM,

    /**
     * The {{block(assetId)}} of the asset that has been added or changed.
     * 
     */
    ASSET_CHANGED,

    /**
     * The {{block(assetId)}} of the asset that has been removed.
     * 
     */
    ASSET_REMOVED,

    /**
     * Represents the {{term(Agent)}}'s ability to communicate with
     *             the data source.
     * 
     */
    AVAILABILITY,

    /**
     * Describes the way the axes will be associated to each other. This is
     *             used in conjunction with {{block(COUPLED_AXES)}} to indicate the way
     *             they are interacting.
     * 
     */
    AXIS_COUPLING,

    /**
     * The value of a signal or calculation issued to adjust the feedrate
     *             of an individual linear type axis.
     * 
     */
    AXIS_FEEDRATE_OVERRIDE,

    /**
     * An indicator of the state of the axis lockout function when power
     *             has been removed and the axis is allowed to move freely.
     * 
     */
    AXIS_INTERLOCK,

    /**
     * An indicator of the controlled state of a {{block(Linear)}} or
     *             {{block(Rotary)}} component representing an axis.
     * 
     */
    AXIS_STATE,

    /**
     * The line of code or command being executed by a
     *             {{block(Controller)}} {{term(Structural Element)}}.
     * 
     */
    BLOCK,

    /**
     * The total count of the number of blocks of program code that have
     *             been executed since execution started.
     * 
     */
    BLOCK_COUNT,

    /**
     * An indication of the operational condition of the interlock function
     *             for an electronically controller chuck.
     * 
     */
    CHUCK_INTERLOCK,

    /**
     * An indication of the operating state of a mechanism that holds a
     *             part or stock material during a manufacturing process. It may also
     *             represent a mechanism that holds any other mechanism in place within
     *             a piece of equipment.
     * 
     */
    CHUCK_STATE,

    /**
     * Service to close a chuck.
     * 
     */
    CLOSE_CHUCK,

    /**
     * Service to close a door.
     * 
     */
    CLOSE_DOOR,

    /**
     * **DEPRECATED** in *Version 1.1*. The programmatic code being
     *             executed.
     * 
     */
    CODE,

    /**
     * An indication of the operating condition of a mechanism represented
     *             by a {{block(Composition)}} type element.
     * 
     */
    COMPOSITION_STATE,

    /**
     * The current operating mode of the {{block(Controller)}} component.
     * 
     */
    CONTROLLER_MODE,

    /**
     * A setting or operator selection that changes the behavior of a piece
     *             of equipment.
     * 
     */
    CONTROLLER_MODE_OVERRIDE,

    /**
     * Refers to the set of associated axes.
     * 
     */
    COUPLED_AXES,

    /**
     * The time and date code associated with a material or other physical
     *             item.
     * 
     */
    DATE_CODE,

    /**
     * The identifier of another piece of equipment that is temporarily
     *             associated with a component of this piece of equipment to perform a
     *             particular function.
     * 
     */
    DEVICE_UUID,

    /**
     * An indication of a fault associated with the direction of motion of
     *             a {{term(Structural Element)}}.
     * 
     */
    DIRECTION,

    /**
     * The operational state of a {{block(Door)}} component or composition
     *             element.
     * 
     */
    DOOR_STATE,

    /**
     * The current state of the emergency stop signal for a piece of
     *             equipment, controller path, or any other component or subsystem of a
     *             piece of equipment.
     * 
     */
    EMERGENCY_STOP,

    /**
     * An indication that the end of a piece of bar stock has been reached.
     * 
     */
    END_OF_BAR,

    /**
     * An indication that a piece of equipment, or a sub-part of a piece of
     *             equipment, is performing specific types of activities.
     * 
     */
    EQUIPMENT_MODE,

    /**
     * The execution status of the {{block(Component)}}.
     * 
     */
    EXECUTION,

    /**
     * The current intended production status of the device or component.
     * 
     */
    FUNCTIONAL_MODE,

    /**
     * The measurement of the hardness of a material.
     * 
     */
    HARDNESS,

    /**
     * An indication of the operation condition of an {{block(Interface)}}
     *             component.
     * 
     */
    INTERFACE_STATE,

    /**
     * **DEPRECATED** in *Version 1.4.0*. The current line of code being
     *             executed.
     * 
     */
    LINE,

    /**
     * An optional identifier for a {{block(Block)}} of code in a
     *             {{block(Program)}}.
     * 
     */
    LINE_LABEL,

    /**
     * A reference to the position of a block of program code within a
     *             control program.
     * 
     */
    LINE_NUMBER,

    /**
     * The identifier of a material used or consumed in the manufacturing
     *             process.
     * 
     */
    MATERIAL,

    /**
     * Service to change the type of material or product being loaded or
     *             fed to a piece of equipment.
     * 
     */
    MATERIAL_CHANGE,

    /**
     * Service to advance material or feed product to a piece of equipment
     *             from a continuous or bulk source.
     * 
     */
    MATERIAL_FEED,

    /**
     * Identifies the layers of material applied to a part or product as
     *             part of an additive manufacturing process.
     * 
     */
    MATERIAL_LAYER,

    /**
     * Service to load a piece of material or product.
     * 
     */
    MATERIAL_LOAD,

    /**
     * Service to remove or retract material or product.
     * 
     */
    MATERIAL_RETRACT,

    /**
     * Service to unload a piece of material or product.
     * 
     */
    MATERIAL_UNLOAD,

    /**
     * Any text string of information to be transferred from a piece of
     *             equipment to a client software application.
     * 
     */
    MESSAGE,

    /**
     * Service to open a chuck.
     * 
     */
    OPEN_CHUCK,

    /**
     * Service to open a door.
     * 
     */
    OPEN_DOOR,

    /**
     * The identifier of the person currently responsible for operating the
     *             piece of equipment.
     * 
     */
    OPERATOR_ID,

    /**
     * The identifier for a pallet.
     * 
     */
    PALLET_ID,

    /**
     * Service to change the part or product associated with a piece of
     *             equipment to a different part or product.
     * 
     */
    PART_CHANGE,

    /**
     * The aggregate count of parts.
     * 
     */
    PART_COUNT,

    /**
     * An indication designating whether a part or work piece has been
     *             detected or is present.
     * 
     */
    PART_DETECT,

    /**
     * An identifier of a part in a manufacturing operation.
     * 
     */
    PART_ID,

    /**
     * **DEPRECATED** in *Version 1.7*. `PART_NUMBER` is now a `subType` of
     *             `PART_KIND_ID`. An identifier of a part or product moving through
     *             the manufacturing process.
     * 
     */
    PART_NUMBER,

    /**
     * The value of a signal or calculation issued to adjust the feedrate
     *             for the axes associated with a {{block(Path)}} component that may
     *             represent a single axis or the coordinated movement of multiple
     *             axes.
     * 
     */
    PATH_FEEDRATE_OVERRIDE,

    /**
     * Describes the operational relationship between a {{block(Path)}}
     *             {{term(Structural Element)}} and another {{block(Path)}}
     *             {{term(Structural Element)}} for pieces of equipment comprised of
     *             multiple logical groupings of controlled axes or other logical
     *             operations.
     * 
     */
    PATH_MODE,

    /**
     * The indication of the status of the source of energy for a
     *             {{term(Structural Element)}} to allow it to perform its intended
     *             function or the state of an enabling signal providing permission for
     *             the {{term(Structural Element)}} to perform its functions.
     * 
     */
    POWER_STATE,

    /**
     * **DEPRECATED** in *Version 1.1.0*. The `ON` or `OFF` status of the
     *             component.
     * 
     */
    POWER_STATUS,

    /**
     * The time and date associated with an activity or event.
     * 
     */
    PROCESS_TIME,

    /**
     * The name of the logic or motion program being executed by the
     *             {{block(Controller)}} component.
     * 
     */
    PROGRAM,

    /**
     * A comment or non-executable statement in the control program.
     * 
     */
    PROGRAM_COMMENT,

    /**
     * An indication of the status of the {{block(Controller)}} components
     *             program editing mode. A program may be edited while another is
     *             executed.
     * 
     */
    PROGRAM_EDIT,

    /**
     * The name of the program being edited. This is used in conjunction
     *             with {{block(ProgramEdit)}} when in `ACTIVE` state.
     * 
     */
    PROGRAM_EDIT_NAME,

    /**
     * The non-executable header section of the control program.
     * 
     */
    PROGRAM_HEADER,

    /**
     * The Uniform Resource Identifier (URI) for the source file associated
     *             with {{block(Program)}}.
     * 
     */
    PROGRAM_LOCATION,

    /**
     * Defines whether the logic or motion program defined by
     *             {{block(Program)}} is being executed from the local memory of the
     *             controller or from an outside source.
     * 
     */
    PROGRAM_LOCATION_TYPE,

    /**
     * An indication of the nesting level within a control program that is
     *             associated with the code or instructions that is currently being
     *             executed.
     * 
     */
    PROGRAM_NEST_LEVEL,

    /**
     * The current operating mode for a {{block(Rotary)}} type axis.
     * 
     */
    ROTARY_MODE,

    /**
     * The percentage change to the velocity of the programmed velocity for
     *             a {{block(Rotary)}} type axis.
     * 
     */
    ROTARY_VELOCITY_OVERRIDE,

    /**
     * The serial number associated with a {{block(Component)}},
     *             {{block(Asset)}}, or {{block(Device)}}.
     * 
     */
    SERIAL_NUMBER,

    /**
     * An indication of the status of the spindle for a piece of equipment
     *             when power has been removed and it is free to rotate.
     * 
     */
    SPINDLE_INTERLOCK,

    /**
     * The identifier of an individual tool asset.
     * 
     */
    TOOL_ASSET_ID,

    /**
     * An identifier for the tool group associated with a specific tool.
     *             Commonly used to designate spare tools.
     * 
     */
    TOOL_GROUP,

    /**
     * **DEPRECATED** in *Version 1.2.0*. See `TOOL_ASSET_ID`. The
     *             identifier of the tool currently in use for a given `Path`.
     * 
     */
    TOOL_ID,

    /**
     * The identifier assigned by the {{block(Controller)}} component to a
     *             cutting tool when in use by a piece of equipment.
     * 
     */
    TOOL_NUMBER,

    /**
     * A reference to the tool offset variables applied to the active
     *             cutting tool associated with a {{block(Path)}} in a
     *             {{block(Controller)}} type component.
     * 
     */
    TOOL_OFFSET,

    /**
     * The identifier of the person currently responsible for operating the
     *             piece of equipment.
     * 
     */
    USER,

    /**
     * A data value whose meaning may change over time due to changes in
     *             the opertion of a piece of equipment or the process being executed
     *             on that piece of equipment.
     * 
     */
    VARIABLE,

    /**
     * An indication of the reason that {{block(Execution)}} is reporting a
     *             value of `WAIT`.
     * 
     */
    WAIT_STATE,

    /**
     * A string like piece or filament of relatively rigid or flexible
     *             material provided in a variety of diameters.
     * 
     */
    WIRE,

    /**
     * The identifier for the current workholding or part clamp in use by a
     *             piece of equipment.
     * 
     */
    WORKHOLDING_ID,

    /**
     * A reference to the offset variables for a work piece or part
     *             associated with a {{block(Path)}} in a {{block(Controller)}} type
     *             component.
     * 
     */
    WORK_OFFSET,

    /**
     * The Operating System of a component.
     * 
     */
    OPERATING_SYSTEM,

    /**
     * The embedded software of a component.
     * 
     */
    FIRMWARE,

    /**
     * The application on a component.
     * 
     */
    APPLICATION,

    /**
     * The software library on a component
     * 
     */
    LIBRARY,

    /**
     * An indication of a fault associated with the hardware subsystem of
     *             the {{term(Structural Element)}}.
     * 
     */
    HARDWARE,

    /**
     * Network details of a component.
     * 
     */
    NETWORK,

    /**
     * A three space angular rotation relative to a coordinate system.
     * 
     */
    ROTATION,

    /**
     * A three space linear translation relative to a coordinate system.
     * 
     */
    TRANSLATION,

    /**
     * An {{block(Event)}} that provides the {{term(UUID)}} of new device
     *             added to an {{term(MTConnect Agent)}}.
     * 
     */
    DEVICE_ADDED,

    /**
     * An {{block(Event)}} that provides the {{term(UUID)}} of a device
     *             removed from an {{term(MTConnect Agent)}}.
     * 
     */
    DEVICE_REMOVED,

    /**
     * An {{block(Event)}} that provides the {{term(UUID)}} of the device
     *             whose {{term(Metadata)}} has changed.
     * 
     */
    DEVICE_CHANGED,

    /**
     * The status of the connection between an {{term(Adapter)}} and an
     *             {{term(Agent)}}.
     * 
     */
    CONNECTION_STATUS,

    /**
     * The originator’s software version of the {{term(Adapter)}}.
     * 
     */
    ADAPTER_SOFTWARE_VERSION,

    /**
     * The {{term(URI)}} of the {{term(Adapter)}}.
     * 
     */
    ADAPTER_URI,

    /**
     * The reference version of the MTConnect Standard supported by the
     *             {{term(Adapter)}}.
     * 
     */
    MTCONNECT_VERSION,

    /**
     * An {{block(Event)}} defining an {{term(Attachment)}} between a
     *             sensor and an entity.
     * 
     */
    SENSOR_ATTACHMENT,

    /**
     * State or condition of a part.
     * 
     */
    PART_STATUS,

    /**
     * An identifier of a process being executed by the device.
     * 
     */
    PROCESS_OCCURRENCE_ID,

    /**
     * Identifier given to link the individual occurrence to a group of
     *             related occurrences, such as a process step in a process plan.
     * 
     */
    PROCESS_AGGREGATE_ID,

    /**
     * Identifier given to link the individual occurrence to a class of
     *             processes or process definition.
     * 
     */
    PROCESS_KIND_ID,

    /**
     * Identifier given to a collection of individual parts.
     * 
     */
    PART_GROUP_ID,

    /**
     * Identifier given to link the individual occurrence to a class of
     *             parts, typically distinguished by a particular part design.
     * 
     */
    PART_KIND_ID,

    /**
     * Identifier given to a distinguishable, individual part.
     * 
     */
    PART_UNIQUE_ID,

    /**
     * A set of limits used to indicate whether a process variable is
     *             stable and in control.
     * 
     */
    CONTROL_LIMIT,

    /**
     * A set of limits defining a range of values designating acceptable
     *             performance for a variable.
     * 
     */
    SPECIFICATION_LIMIT,

    /**
     * A set of limits used to trigger warning or alarm indicators.
     * 
     */
    ALARM_LIMIT,

    /**
     * Accumulation of the number of times an operation has attempted to,
     *             or is planned to attempt to, load materials, parts, or other items.
     * 
     */
    LOAD_COUNT,

    /**
     * Accumulation of the number of times an operation has attempted to,
     *             or is planned to attempt to, unload materials, parts, or other
     *             items.
     * 
     */
    UNLOAD_COUNT,

    /**
     * Accumulation of the number of times an operation has attempted to,
     *             or is planned to attempt to, transfer materials, parts, or other
     *             items from one location to another.
     * 
     */
    TRANSFER_COUNT,

    /**
     * Accumulation of the number of times a function has attempted to, or
     *             is planned to attempt to, activate or be performed.
     * 
     */
    ACTIVATION_COUNT,

    /**
     * Accumulation of the number of times a function has attempted to, or
     *             is planned to attempt to, deactivate or cease.
     * 
     */
    DEACTIVATION_COUNT,

    /**
     * Accumulation of the number of times a cyclic function has attempted
     *             to, or is planned to attempt to execute.
     * 
     */
    CYCLE_COUNT,

    /**
     * The state of a valve is one of open, closed, or transitioning
     *             between the states.
     * 
     */
    VALVE_STATE,

    /**
     * The state or operating mode of a {{block(Lock)}}.
     * 
     */
    LOCK_STATE,

    /**
     * The particular condition of the process occurrence at a specific
     *             time.
     * 
     */
    PROCESS_STATE,

    /**
     * The particular condition of the part occurrence at a specific time.
     * 
     */
    PART_PROCESSING_STATE,

    /**
     * An indication that the piece of equipment has experienced a
     *             communications failure.
     * 
     */
    COMMUNICATIONS,

    /**
     * An indication that the value of the data associated with a measured
     *             value or a calculation is outside of an expected range.
     * 
     */
    DATA_RANGE,

    /**
     * An indication that an error occurred in the logic program or
     *             programmable logic controller (PLC) associated with a piece of
     *             equipment.
     * 
     */
    LOGIC_PROGRAM,

    /**
     * An indication that an error occurred in the motion program
     *             associated with a piece of equipment.
     * 
     */
    MOTION_PROGRAM,

    /**
     * A general purpose indication associated with an electronic component
     *             of a piece of equipment or a controller that represents a fault that
     *             is not associated with the operator, program, or hardware.
     * 
     */
    SYSTEM,

    /**
     * An indication of a fault associated with an actuator.
     * 
     */
    ACTUATOR;

    public String value() {
        return name();
    }

    public static DataItemEnumEnum fromValue(String v) {
        return valueOf(v);
    }

}
