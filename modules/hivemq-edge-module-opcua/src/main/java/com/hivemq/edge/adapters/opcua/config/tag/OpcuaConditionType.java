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
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
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
 * Declaring a <em>supertype</em> of what the device actually offers is legitimate, and verification therefore
 * checks assignability rather than equality. The reason is <b>not</b> that every declared field must exist:
 * most of them need not. Well over half the roughly fifty non-base fields carry the modelling rule
 * {@code Optional} — all sixteen of {@code LimitAlarmType}'s (Table 92), all four of
 * {@code NonExclusiveLimitAlarmType}'s states (Table 97), fourteen of {@code AlarmConditionType}'s seventeen
 * — so a conformant server may omit them whatever type it claims.
 * <p>
 * What makes it safe is the <em>filter</em>, not the type table: OPC 10000-4 §7.22.3 has an unmatched select
 * clause return null rather than fail the monitored item. So a declared field the device does not implement
 * arrives as null, exactly like one that is merely not applicable to this transition. The consequence worth
 * knowing is that those two cases are indistinguishable downstream — a {@code LimitAlarmType} tag against a
 * server implementing only {@code HighLimit} publishes a shape promising sixteen limits, fifteen of them
 * permanently null.
 * <p>
 * Generated from the specification's type model; the field lists are the members each type adds, excluding
 * methods and nested objects, with one deliberate exception: a condition's {@code ShelvingState} is an
 * Object carrying its own methods, and it is selected anyway — through the state-machine variable one level
 * down — because Edge lets an operator command shelving and they would otherwise have no way to observe the
 * result. See {@link #STATE_MACHINE_FIELDS}.
 */
public enum OpcuaConditionType implements EventFieldSet {
    CONDITION(
            "ConditionType",
            null,
            List.of(
                    // First, so it sits immediately after the base event fields in every condition's shape --
                    // the position it held while it was wrongly one of them, which keeps the published key
                    // order of a condition event unchanged.
                    //
                    // Through Names because an enum constant cannot refer to a static field of its own class
                    // declared below it, and enum constants have to come first. CONDITION_ID is that field,
                    // and it is this same string.
                    Names.CONDITION_ID,
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
                    // OPC 10000-9 §5.8.2 Table 40: AudioDataType, Optional -- and a ByteString subtype, so
                    // it publishes as base64 with contentEncoding declared, like EventId. It was omitted
                    // while AudibleEnabled beside it was present, which left the pair half-usable: a
                    // consumer could learn that a sound is configured but never obtain it.
                    "AudibleSound",
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
                    // OPC 10000-9 §5.8.2 Table 40: ShelvedStateMachineType, Optional. Selected because Edge
                    // exposes all three shelving commands, and SuppressedOrShelved beside it is a single
                    // Boolean that cannot say which of the two it means -- let alone tell TimedShelved from
                    // OneShotShelved. An operator could shelve an alarm and then had no way to see that they
                    // had, or to see it come back.
                    "ShelvingState",
                    "SilenceState",
                    "SuppressedOrShelved",
                    "SuppressedState",
                    // ShelvedStateMachineType's own property (§5.8.17 Table 73), and Mandatory there: when
                    // the machine is TimedShelved this says when it returns to Unshelved. Nested one level
                    // down rather than a member of the alarm, hence NESTED_FIELD_PATHS.
                    "UnshelveTime")),
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
    // OPC 10000-9 §5.8.19.3 Table 96 adds exactly one member, and it is Mandatory: LimitState, which says
    // which limit is violated. ActiveState used to stand here instead — it is inherited from
    // AlarmConditionType, so listing it was redundant, and it made the line look populated while the one
    // field the type actually contributes was missing.
    EXCLUSIVE_LIMIT_ALARM("ExclusiveLimitAlarmType", "LimitAlarmType", List.of("LimitState")),
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
    // The one type defined outside Part 9: OPC 10000-12 §7.8.2.11 (Discovery and Global Services). Checked
    // there rather than assumed -- subtype of SystemOffNormalAlarmType, with all three fields Mandatory.
    TRUST_LIST_OUT_OF_DATE_ALARM(
            "TrustListOutOfDateAlarmType",
            "SystemOffNormalAlarmType",
            List.of("LastUpdateTime", "TrustListId", "UpdateFrequency"));

    /**
     * The field naming <em>which condition</em> an event is about.
     * <p>
     * Not a property beneath the event, unlike every other field: it is the event's own node id, so it is
     * selected with an empty browse path against the {@code NodeId} attribute of {@code ConditionType} —
     * exactly the operand the where clause uses to confine a condition tag to its own condition.
     * <p>
     * That difference is why it was missing. The field table is a list of names, and this field has no name
     * to look up and a different attribute to read, so it had nowhere to sit until {@link SelectedField}
     * could carry an attribute. Meanwhile the filter built the same operand by hand, so the concept was
     * present on the subscribing side and absent on the publishing side.
     * <p>
     * It belongs to every <b>condition</b> event, whichever kind of tag delivered it. The same transition
     * must produce the same message however it was subscribed — a {@code CONDITION} tag and an
     * {@code EVENT_SUBSCRIPTION} tag declaring one type publish one shape — and a message that can only be
     * understood by knowing which tag delivered it is not self-describing. For an event subscription it is
     * decisive rather than merely tidy: that tag carries many conditions by design, and without this field
     * nothing in the payload tells them apart.
     * <p>
     * <b>It is a member of {@code ConditionType}, not of {@code BaseEventType}</b>, and the v01 pass put it
     * in the wrong list. That was invisible while only conditions were published, because every condition
     * type inherits the base fields — but a {@code REFRESH} tag publishes {@code RefreshStart},
     * {@code RefreshEnd}, {@code RefreshRequired} and {@code EventQueueOverflow}, which derive from
     * {@code BaseEventType} directly and are not conditions. Those events were being asked for an operand
     * their type does not define and promised a condition identity they cannot have. Declared here, on the
     * type that defines it, both follow from the field list rather than needing to be remembered.
     */
    public static final @NotNull String CONDITION_ID = Names.CONDITION_ID;

    /**
     * Field names the enum constants above need before this class declares them.
     * <p>
     * An enum constant's initializer cannot read a static field of its own class that is declared after it,
     * and enum constants must come first — so {@code ConditionType} listing {@code ConditionId} among its own
     * members has nowhere to read it from. A nested holder is initialized on first use, which is late enough.
     * The public constants below delegate here, so there is still one spelling of each name.
     */
    private static final class Names {

        private static final @NotNull String CONDITION_ID = "ConditionId";

        private Names() {}
    }

    /**
     * Fields every event carries, from {@code BaseEventType} — the root above {@code ConditionType}. Selected
     * for every condition regardless of its type, and listed first so the published shape reads in a stable
     * order.
     */
    public static final @NotNull List<String> BASE_EVENT_FIELDS = List.of(
            "EventId",
            "EventType",
            "SourceNode",
            "SourceName",
            "Time",
            "ReceiveTime",
            // The one Optional member of BaseEventType worth selecting here. Time is UTC; this says what the
            // clock read where the event was issued -- an offset in minutes plus whether it includes DST.
            //
            // Optional is not a reason to leave it out. Edge already selects Optional fields throughout: all
            // sixteen of LimitAlarmType's limits are Optional, as are fourteen of AlarmConditionType's
            // seventeen members. A server without one returns null (OPC 10000-4 §7.22.3), which is the same
            // cost paid everywhere else. What made this field different is that it was *unreachable*: every
            // other field follows from the declared type, and no type adds LocalTime, because it sits on the
            // base of them all. So no configuration could ask for it.
            "LocalTime",
            "Message",
            "Severity");

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

    /**
     * The browse name of the {@code Id} companion — a {@code Boolean} beneath a {@link #TWO_STATE_FIELDS}
     * field, a {@code NodeId} beneath a state machine's {@link #CURRENT_STATE}.
     */
    public static final @NotNull String STATE_ID = "Id";

    /**
     * Fields that are a state machine rather than a value.
     * <p>
     * Such a field is an Object node. An Object has no {@code Value} attribute at all (OPC 10000-4 Table
     * 129 — a select clause names an attribute, and {@code Value} is the only one that carries data), so
     * selecting the field itself returns nothing. What is readable is the {@code FiniteStateMachineType}
     * variable one level down saying where the machine currently is.
     * <p>
     * Only {@code LimitState} is listed. It is Mandatory on {@code ExclusiveLimitAlarmType} (OPC 10000-9
     * §5.8.19.3 Table 96) and says <em>which</em> limit was violated — without it that type publishes that
     * an alarm is active and nothing about which threshold tripped, while its non-exclusive sibling carries
     * all four limit states.
     * <p>
     * {@code ShelvingState} is the other, and it used to be absent on the grounds that shelving was a
     * separate concern. It is not a separate concern from the write side: Edge exposes {@code UNSHELVE},
     * {@code ONE_SHOT_SHELVE} and {@code TIMED_SHELVE}, so an operator could command a shelving state and
     * then had nothing in any published message to confirm it. {@code SuppressedOrShelved} is no substitute
     * — a single Boolean that says neither which of the two it means nor, when it means shelved, which of
     * the two shelving modes is in force.
     */
    public static final @NotNull Set<String> STATE_MACHINE_FIELDS = Set.of("LimitState", "ShelvingState");

    /**
     * Fields whose browse path is not simply their own name.
     * <p>
     * Almost every field is a property of the event, so its path is one element and falls out of the name.
     * {@code UnshelveTime} is a property of the {@code ShelvingState} machine instead — one level down —
     * and is published under its own key rather than nested, because every other field in the payload is
     * flat and a lone nested object would be a shape of its own to handle.
     * <p>
     * A map rather than a special case, because the select clause has always supported paths of any length
     * (OPC 10000-4 §7.22.3 requires only that each element be an Object or Variable); it was the
     * <em>generation</em> that assumed path equals name. One entry today, and the next field like it needs a
     * line rather than a branch.
     */
    public static final @NotNull Map<String, List<String>> NESTED_FIELD_PATHS =
            Map.of("UnshelveTime", List.of("ShelvingState", "UnshelveTime"));

    /**
     * The browse name of the variable holding a state machine's current state, a {@code LocalizedText}.
     * <p>
     * Inherited from {@code FiniteStateMachineType} (OPC 10000-16), not declared by the alarm state machines
     * themselves — OPC 10000-9 Table 93 lists only the machine's states and transitions, which are Objects.
     */
    public static final @NotNull String CURRENT_STATE = "CurrentState";

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
    @Override
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
    @Override
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
     * What a selected field contributes to the published JSON.
     * <p>
     * This is stated rather than inferred from the browse path's length. It used to be inferred — a
     * two-element path <em>meant</em> a state's {@code Id} — which held only while two-element paths had
     * exactly one purpose. {@code LimitState/CurrentState} is a second purpose, and its {@code Id} is a
     * third element deeper still, so the path can no longer say what the entry is for.
     */
    public enum FieldRole {
        /** Published under its own key, whatever its value turns out to be. */
        VALUE,
        /** Folded into the preceding {@link #VALUE} entry as its {@code id}, not published on its own. */
        ID
    }

    /**
     * One selected field: the browse path to ask the server for, the name to publish it under, what it
     * contributes, and which attribute of the node it reads.
     * <p>
     * A path may be of any length. OPC 10000-4 §7.22.3 requires only that each element be an Object or
     * Variable node, so {@code LimitState/CurrentState/Id} traverses an Object and two Variables and is as
     * legal as a one-element path.
     * <p>
     * <b>An empty path means the event node itself</b>, which is how {@code ConditionId} is selected: it is
     * not a property beneath the event but the event's own node id, so there is no browse path to walk and
     * the attribute read is {@code NodeId} rather than {@code Value}. Every other field is a property and
     * reads its {@code Value}, which is why that is the default.
     *
     * @param path        the browse path to select; empty means the event node itself.
     * @param publishedAs the key in the published JSON. An {@link FieldRole#ID} entry repeats the key of the
     *                    entry it belongs to, because it is folded into that object rather than published
     *                    beside it — {@code {"ActiveState": {"text": "Active", "id": true}}}.
     * @param role        what this entry contributes.
     * @param attribute   the attribute id to read, from {@code AttributeId}. {@code Value} for a property.
     * @param typeDefinitionId the type the operand is written against. {@code BaseEventType} for an ordinary
     *                    field, whose browse path resolves against any event that has it; {@code ConditionType}
     *                    for {@code ConditionId}, which is defined there and nowhere else.
     */
    public record SelectedField(
            @NotNull List<String> path,
            @NotNull String publishedAs,
            @NotNull FieldRole role,
            @NotNull UInteger attribute,
            @NotNull NodeId typeDefinitionId) {

        /**
         * The root of the event type hierarchy, and the right operand type for an ordinary field.
         * <p>
         * Naming it means the browse paths resolve against any event type that has them, and a field a
         * particular event lacks comes back null rather than failing the subscription.
         */
        public static final @NotNull NodeId BASE_EVENT_TYPE = NodeIds.BaseEventType;

        /** A field read as its {@code Value} against {@code BaseEventType} — every field but {@code ConditionId}. */
        public SelectedField(
                final @NotNull List<String> path, final @NotNull String publishedAs, final @NotNull FieldRole role) {
            this(path, publishedAs, role, AttributeId.Value.uid(), BASE_EVENT_TYPE);
        }

        /** Whether this selects an {@code Id} to be folded into the field before it. */
        public boolean isStateId() {
            return role == FieldRole.ID;
        }
    }

    /**
     * Every field to select, in the order the select clause and the decoder both walk.
     * <p>
     * This is {@link #allFields()} with an extra {@link FieldRole#ID} entry after each field that has one.
     * Both are derived here so the two lists cannot drift: the event decoder matches values to fields
     * <em>positionally</em> against the select clause, so an entry added to one and not the other silently
     * shifts every field after it.
     * <p>
     * Two kinds of field carry an {@code Id}, and they differ in where it sits and what it is:
     * <ul>
     *   <li>A {@link #TWO_STATE_FIELDS two-state field} keeps it one level down — {@code ActiveState/Id} —
     *       and it is a {@code Boolean}.</li>
     *   <li>A {@link #STATE_MACHINE_FIELDS state machine} is an Object with no value of its own, so the
     *       readable state is {@code CurrentState} one level down and its {@code Id} one level below that.
     *       That {@code Id} is a {@code NodeId} identifying the active state node, not a Boolean.</li>
     * </ul>
     */
    @Override
    public @NotNull List<SelectedField> selectedFields() {
        return selectClauseFor(allFields());
    }

    /**
     * Builds the select clause for a list of field names.
     * <p>
     * Shared with {@link BaseEventFieldSet}, which publishes a fixed list rather than one derived from a type
     * hierarchy. The {@code Id}-companion rules are a property of the <em>fields</em>, not of the type that
     * happens to declare them, so both go through here and neither can grow a rule the other lacks.
     */
    static @NotNull List<SelectedField> selectClauseFor(final @NotNull List<String> fields) {
        final List<SelectedField> selected = new ArrayList<>();
        for (final String field : fields) {
            if (CONDITION_ID.equals(field)) {
                // The event node itself: no browse path to walk, its NodeId attribute rather than a Value,
                // and -- the part that was wrong -- ConditionType rather than BaseEventType. All three
                // together are what OPC 10000-9 defines this operand to be, and the type is not decoration:
                // BaseEventType does not define ConditionId, so an operand naming it asks a strict server
                // for something the type has never had. The where clause built the same operand by hand and
                // got the type right, so the two halves of one concept disagreed. See CONDITION_ID.
                selected.add(new SelectedField(
                        List.of(), field, FieldRole.VALUE, AttributeId.NodeId.uid(), NodeIds.ConditionType));
                continue;
            }
            if (STATE_MACHINE_FIELDS.contains(field)) {
                selected.add(new SelectedField(List.of(field, CURRENT_STATE), field, FieldRole.VALUE));
                selected.add(new SelectedField(List.of(field, CURRENT_STATE, STATE_ID), field, FieldRole.ID));
                continue;
            }
            selected.add(
                    new SelectedField(NESTED_FIELD_PATHS.getOrDefault(field, List.of(field)), field, FieldRole.VALUE));
            if (TWO_STATE_FIELDS.contains(field)) {
                selected.add(new SelectedField(List.of(field, STATE_ID), field, FieldRole.ID));
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
     * <p>
     * <b>Blank means unset</b>, and yields null so the field's documented default applies — no type predicate
     * at all for {@code filterType}, {@code AlarmConditionType} for {@code type}. That is the same rule
     * {@code OpcuaTagDefinition} already applied to the three narrowing <em>node id</em> fields through its
     * {@code blankToNull}, and the same rule {@code EnumParsing} applies to the security config enums: blank
     * or absent input means the setting resolves to its default.
     * <p>
     * It has to be decided here rather than in the constructor, which is where it looks like it belongs. The
     * fields are typed as this enum, so Jackson has to produce an instance <em>before</em> the constructor is
     * invoked — a {@code blankToNull} beside the other three would never run, and could not be written anyway
     * since it would be handed an enum rather than the string.
     * <p>
     * Getting this wrong lost configuration silently. A blank string is what a UI form submits when a box is
     * cleared and what a config generator emits for an unset optional; the throw failed the conversion of the
     * whole adapter configuration, which leaves the running adapter untouched — so the write was accepted,
     * nothing was reported, and the tag was simply absent when read back. Found by QA as EDG-894 P8.
     *
     * @throws IllegalArgumentException when a non-blank name is not a standard condition type. A typo is still
     *                                  reported rather than silently becoming some default: unset resolves to
     *                                  a usable default, and quietly treating {@code AlarmConditonType} as
     *                                  unset would publish a different shape than was written.
     */
    @JsonCreator
    public static @Nullable OpcuaConditionType fromConfig(final @Nullable String browseName) {
        if (browseName == null || browseName.isBlank()) {
            return null;
        }
        // Trimmed because surrounding whitespace is never part of a browse name, and because Jackson's own
        // enum handling trims before matching -- so the two enums on this tag would otherwise disagree about
        // " VALUE " versus " AlarmConditionType ". Case is not normalised: a browse name is a case-sensitive
        // OPC UA identifier, and accepting a different casing would be a wider claim than this fix makes.
        final String trimmed = browseName.trim();
        return fromBrowseName(trimmed)
                .orElseThrow(() -> new IllegalArgumentException("Unknown OPC UA condition type '" + trimmed
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
