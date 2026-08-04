/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.edge.adapters.opcua.config.tag;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The standard OPC UA condition types, and the event fields each one carries.
 * <p>
 * A condition tag declares which of these its node is, and that declaration decides the tag's northbound
 * shape: the read schema and the event filter's select clause are both derived from it, so the two cannot
 * drift apart. The hierarchy is fixed by the specification and strictly additive — a child carries everything
 * its parent does plus its own fields — so the whole table can live here rather than being discovered per
 * device.
 * <p>
 * Declaring a <em>supertype</em> of what the device actually offers is legitimate: every declared field still
 * exists, and the tag simply projects a narrower view. That is why verification checks assignability rather
 * than equality.
 * <p>
 * Generated from the specification's type model; the field lists are the members each type adds, excluding
 * methods and nested objects (a condition's {@code ShelvingState} is an object with its own methods, not an
 * event field).
 */
public enum OpcuaConditionType {
    CONDITION(
            "ConditionType",
            null,
            List.of(
                    "BranchId",
                    "ClientUserId",
                    "Comment",
                    "ConditionClassId",
                    "ConditionClassName",
                    "ConditionName",
                    "ConditionSubClassId",
                    "ConditionSubClassName",
                    "EnabledState",
                    "LastSeverity",
                    "Quality",
                    "Retain",
                    "SupportsFilteredRetain")),
    ACKNOWLEDGEABLE_CONDITION(
            "AcknowledgeableConditionType", "ConditionType", List.of("AckedState", "ConfirmedState", "EnabledState")),
    ALARM_CONDITION(
            "AlarmConditionType",
            "AcknowledgeableConditionType",
            List.of(
                    "ActiveState",
                    "AudibleEnabled",
                    "EnabledState",
                    "FirstInGroupFlag",
                    "InputNode",
                    "LatchedState",
                    "MaxTimeShelved",
                    "OffDelay",
                    "OnDelay",
                    "OutOfServiceState",
                    "ReAlarmRepeatCount",
                    "ReAlarmTime",
                    "SilenceState",
                    "SuppressedOrShelved",
                    "SuppressedState")),
    DISCRETE_ALARM("DiscreteAlarmType", "AlarmConditionType", List.of()),
    OFF_NORMAL_ALARM("OffNormalAlarmType", "DiscreteAlarmType", List.of("NormalState")),
    SYSTEM_OFF_NORMAL_ALARM("SystemOffNormalAlarmType", "OffNormalAlarmType", List.of()),
    CERTIFICATE_EXPIRATION_ALARM(
            "CertificateExpirationAlarmType",
            "SystemOffNormalAlarmType",
            List.of("Certificate", "CertificateType", "ExpirationDate", "ExpirationLimit")),
    DIALOG_CONDITION(
            "DialogConditionType",
            "ConditionType",
            List.of(
                    "CancelResponse",
                    "DefaultResponse",
                    "DialogState",
                    "EnabledState",
                    "LastResponse",
                    "OkResponse",
                    "Prompt",
                    "ResponseOptionSet")),
    DISCREPANCY_ALARM(
            "DiscrepancyAlarmType", "AlarmConditionType", List.of("ExpectedTime", "TargetValueNode", "Tolerance")),
    LIMIT_ALARM(
            "LimitAlarmType",
            "AlarmConditionType",
            List.of(
                    "BaseHighHighLimit",
                    "BaseHighLimit",
                    "BaseLowLimit",
                    "BaseLowLowLimit",
                    "HighDeadband",
                    "HighHighDeadband",
                    "HighHighLimit",
                    "HighLimit",
                    "LowDeadband",
                    "LowLimit",
                    "LowLowDeadband",
                    "LowLowLimit",
                    "SeverityHigh",
                    "SeverityHighHigh",
                    "SeverityLow",
                    "SeverityLowLow")),
    EXCLUSIVE_LIMIT_ALARM("ExclusiveLimitAlarmType", "LimitAlarmType", List.of("ActiveState")),
    EXCLUSIVE_DEVIATION_ALARM(
            "ExclusiveDeviationAlarmType", "ExclusiveLimitAlarmType", List.of("BaseSetpointNode", "SetpointNode")),
    EXCLUSIVE_LEVEL_ALARM("ExclusiveLevelAlarmType", "ExclusiveLimitAlarmType", List.of()),
    EXCLUSIVE_RATE_OF_CHANGE_ALARM(
            "ExclusiveRateOfChangeAlarmType", "ExclusiveLimitAlarmType", List.of("EngineeringUnits")),
    INSTRUMENT_DIAGNOSTIC_ALARM("InstrumentDiagnosticAlarmType", "OffNormalAlarmType", List.of()),
    NON_EXCLUSIVE_LIMIT_ALARM(
            "NonExclusiveLimitAlarmType",
            "LimitAlarmType",
            List.of("ActiveState", "HighHighState", "HighState", "LowLowState", "LowState")),
    NON_EXCLUSIVE_DEVIATION_ALARM(
            "NonExclusiveDeviationAlarmType",
            "NonExclusiveLimitAlarmType",
            List.of("BaseSetpointNode", "SetpointNode")),
    NON_EXCLUSIVE_LEVEL_ALARM("NonExclusiveLevelAlarmType", "NonExclusiveLimitAlarmType", List.of()),
    NON_EXCLUSIVE_RATE_OF_CHANGE_ALARM(
            "NonExclusiveRateOfChangeAlarmType", "NonExclusiveLimitAlarmType", List.of("EngineeringUnits")),
    SYSTEM_DIAGNOSTIC_ALARM("SystemDiagnosticAlarmType", "OffNormalAlarmType", List.of()),
    TRIP_ALARM("TripAlarmType", "OffNormalAlarmType", List.of()),
    TRUST_LIST_OUT_OF_DATE_ALARM(
            "TrustListOutOfDateAlarmType",
            "SystemOffNormalAlarmType",
            List.of("LastUpdateTime", "TrustListId", "UpdateFrequency"));

    /**
     * Fields every event carries, from {@code BaseEventType} — the root above {@code ConditionType}. Selected
     * for every condition regardless of its type, and listed first so the published shape reads in a stable
     * order.
     */
    public static final @NotNull List<String> BASE_EVENT_FIELDS =
            List.of("EventId", "EventType", "SourceNode", "SourceName", "Time", "ReceiveTime", "Message", "Severity");

    /**
     * The fields whose type is {@code TwoStateVariableType}, and which therefore carry a Boolean {@code Id}
     * beside their display text.
     * <p>
     * The {@code Value} of such a field is "a human readable name" (OPC 10000-9 §5.2) — {@code "Enabled"} or
     * {@code "Disabled"}, in whatever locale the session negotiated. Deciding anything from that means string
     * matching against text that varies by locale and by vendor. The {@code Id} property is the machine
     * readable half of the same state, is <b>Mandatory</b> on the type (Table 1), and is reachable only by a
     * two-element browse path — {@code ['ActiveState', 'Id']} — which is why it needs naming here rather than
     * falling out of the field list.
     * <p>
     * Exactly the thirteen the specification defines. {@code ShelvingState} and {@code LimitState} are
     * deliberately absent: those are Objects with their own state machines
     * ({@code ShelvedStateMachineType}, {@code ExclusiveLimitStateMachineType}), not two-state variables, and
     * asking for an {@code Id} beneath them would select nothing.
     */
    public static final @NotNull Set<String> TWO_STATE_FIELDS = Set.of(
            "AckedState",
            "ActiveState",
            "ConfirmedState",
            "DialogState",
            "EnabledState",
            "HighHighState",
            "HighState",
            "LatchedState",
            "LowLowState",
            "LowState",
            "OutOfServiceState",
            "SilenceState",
            "SuppressedState");

    /** The browse name of the Boolean companion of a {@link #TWO_STATE_FIELDS} field. */
    public static final @NotNull String STATE_ID = "Id";

    /**
     * Each type's NodeId in the standard namespace, used to filter events by type.
     * <p>
     * Declared here rather than looked up by name so the compiler checks every entry. Milo's constants live
     * on a package-private superclass of {@code NodeIds}, which makes them unreadable by reflection.
     */
    private static final @NotNull Map<OpcuaConditionType, NodeId> NODE_IDS = Map.ofEntries(
            Map.entry(CONDITION, NodeIds.ConditionType),
            Map.entry(ACKNOWLEDGEABLE_CONDITION, NodeIds.AcknowledgeableConditionType),
            Map.entry(ALARM_CONDITION, NodeIds.AlarmConditionType),
            Map.entry(DISCRETE_ALARM, NodeIds.DiscreteAlarmType),
            Map.entry(OFF_NORMAL_ALARM, NodeIds.OffNormalAlarmType),
            Map.entry(SYSTEM_OFF_NORMAL_ALARM, NodeIds.SystemOffNormalAlarmType),
            Map.entry(CERTIFICATE_EXPIRATION_ALARM, NodeIds.CertificateExpirationAlarmType),
            Map.entry(DIALOG_CONDITION, NodeIds.DialogConditionType),
            Map.entry(DISCREPANCY_ALARM, NodeIds.DiscrepancyAlarmType),
            Map.entry(LIMIT_ALARM, NodeIds.LimitAlarmType),
            Map.entry(EXCLUSIVE_LIMIT_ALARM, NodeIds.ExclusiveLimitAlarmType),
            Map.entry(EXCLUSIVE_DEVIATION_ALARM, NodeIds.ExclusiveDeviationAlarmType),
            Map.entry(EXCLUSIVE_LEVEL_ALARM, NodeIds.ExclusiveLevelAlarmType),
            Map.entry(EXCLUSIVE_RATE_OF_CHANGE_ALARM, NodeIds.ExclusiveRateOfChangeAlarmType),
            Map.entry(INSTRUMENT_DIAGNOSTIC_ALARM, NodeIds.InstrumentDiagnosticAlarmType),
            Map.entry(NON_EXCLUSIVE_LIMIT_ALARM, NodeIds.NonExclusiveLimitAlarmType),
            Map.entry(NON_EXCLUSIVE_DEVIATION_ALARM, NodeIds.NonExclusiveDeviationAlarmType),
            Map.entry(NON_EXCLUSIVE_LEVEL_ALARM, NodeIds.NonExclusiveLevelAlarmType),
            Map.entry(NON_EXCLUSIVE_RATE_OF_CHANGE_ALARM, NodeIds.NonExclusiveRateOfChangeAlarmType),
            Map.entry(SYSTEM_DIAGNOSTIC_ALARM, NodeIds.SystemDiagnosticAlarmType),
            Map.entry(TRIP_ALARM, NodeIds.TripAlarmType),
            Map.entry(TRUST_LIST_OUT_OF_DATE_ALARM, NodeIds.TrustListOutOfDateAlarmType));

    private static final @NotNull Map<String, OpcuaConditionType> BY_BROWSE_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(OpcuaConditionType::browseName, Function.identity()));

    private final @NotNull String browseName;
    private final @Nullable String parentBrowseName;
    private final @NotNull List<String> ownFields;

    OpcuaConditionType(
            final @NotNull String browseName,
            final @Nullable String parentBrowseName,
            final @NotNull List<String> ownFields) {
        this.browseName = browseName;
        this.parentBrowseName = parentBrowseName;
        this.ownFields = ownFields;
    }

    /**
     * The type's name in the OPC UA address space, e.g. {@code ExclusiveLevelAlarmType}. This is also the wire
     * form in configuration: a config file names the OPC UA type, not this enum's constant.
     */
    @JsonValue
    public @NotNull String browseName() {
        return browseName;
    }

    /**
     * The type's NodeId in the standard namespace, for filtering events by type.
     * <p>
     * Named constants rather than reflection over Milo's {@code NodeIds}: those constants are declared on a
     * package-private superclass, so reading them reflectively raises {@code IllegalAccessException} even
     * though they are public and inherited. Referring to them directly is both correct and checked by the
     * compiler, which a name-based lookup would not be.
     */
    public @NotNull NodeId nodeId() {
        final NodeId nodeId = NODE_IDS.get(this);
        if (nodeId == null) {
            // Every constant above is present in the map; a miss means one was added without its id, and a
            // filter built on a missing id would silently match nothing.
            throw new IllegalStateException("No OPC UA NodeId is known for condition type '" + browseName + "'");
        }
        return nodeId;
    }

    /** The type this one derives from, or empty for {@code ConditionType}, which is the root here. */
    public @NotNull Optional<OpcuaConditionType> parent() {
        return parentBrowseName == null ? Optional.empty() : Optional.ofNullable(BY_BROWSE_NAME.get(parentBrowseName));
    }

    /**
     * Every field an event of this type carries: the base event fields, then each ancestor's contribution from
     * the root down, then this type's own. Ordered and de-duplicated — a subtype may re-declare a field its
     * parent already has, and it must appear once.
     */
    public @NotNull List<String> allFields() {
        final Set<String> fields = new LinkedHashSet<>(BASE_EVENT_FIELDS);
        final List<OpcuaConditionType> lineage = new ArrayList<>();
        for (OpcuaConditionType type = this; type != null; type = type.parent().orElse(null)) {
            lineage.add(0, type);
        }
        lineage.forEach(type -> fields.addAll(type.ownFields));
        return List.copyOf(fields);
    }

    /**
     * One selected field: the browse path to ask the server for, and the name to publish it under.
     *
     * @param path        the browse path, one element for an ordinary field and two for a state's {@code Id}.
     * @param publishedAs the key in the published JSON. For a two-element path this is the state's own name,
     *                    because the {@code Id} is folded into the state object rather than published beside
     *                    it — {@code {"ActiveState": {"text": "Active", "id": true}}}.
     */
    public record SelectedField(
            @NotNull List<String> path, @NotNull String publishedAs) {

        /** Whether this selects the Boolean {@code Id} beneath a two-state field. */
        public boolean isStateId() {
            return path.size() == 2;
        }
    }

    /**
     * Every field to select, in the order the select clause and the decoder both walk.
     * <p>
     * This is {@link #allFields()} with an extra entry after each two-state field, for that state's
     * {@code Id}. Both are derived here so the two lists cannot drift: the event decoder matches values to
     * fields <em>positionally</em> against the select clause, so an entry added to one and not the other
     * silently shifts every field after it.
     */
    public @NotNull List<SelectedField> selectedFields() {
        final List<SelectedField> selected = new ArrayList<>();
        for (final String field : allFields()) {
            selected.add(new SelectedField(List.of(field), field));
            if (TWO_STATE_FIELDS.contains(field)) {
                selected.add(new SelectedField(List.of(field, STATE_ID), field));
            }
        }
        return List.copyOf(selected);
    }

    /**
     * Whether a condition of {@code other} can be read as one of this type — true when {@code other} is this
     * type or derives from it. This is what makes a supertype declaration valid: an
     * {@code ExclusiveLevelAlarmType} device satisfies a tag declaring {@code AlarmConditionType}.
     */
    public boolean isSatisfiedBy(final @NotNull OpcuaConditionType other) {
        for (OpcuaConditionType type = other; type != null; type = type.parent().orElse(null)) {
            if (type == this) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reads the type from configuration by its address-space name.
     *
     * @throws IllegalArgumentException when the name is not a standard condition type — a typo in the config
     *                                  is reported rather than silently becoming some default.
     */
    @JsonCreator
    public static @NotNull OpcuaConditionType fromConfig(final @Nullable String browseName) {
        return fromBrowseName(browseName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown OPC UA condition type '" + browseName
                        + "'. Known types: "
                        + Arrays.stream(values())
                                .map(OpcuaConditionType::browseName)
                                .collect(Collectors.joining(", "))));
    }

    /** Looks a type up by its address-space name. */
    public static @NotNull Optional<OpcuaConditionType> fromBrowseName(final @Nullable String browseName) {
        return browseName == null ? Optional.empty() : Optional.ofNullable(BY_BROWSE_NAME.get(browseName));
    }
}
