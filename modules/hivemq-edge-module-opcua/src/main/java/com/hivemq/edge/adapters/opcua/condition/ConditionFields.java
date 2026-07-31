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
package com.hivemq.edge.adapters.opcua.condition;

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * The condition/alarm event fields Edge selects and publishes.
 * <p>
 * OPC-UA has no {@code SELECT *} for events: a client must enumerate the fields it wants, by browse path. The
 * set below is the projection Edge uses for a condition tag — the fields of {@code AlarmConditionType} and its
 * ancestors that a downstream system needs in order to display an alarm and act on it. Because the standard
 * type hierarchy is fixed by the specification, this list can be hardcoded rather than discovered per device.
 * <p>
 * Fields that do not exist on a particular event instance come back as {@code null} rather than an error, so
 * selecting the full set is safe even for a condition of a more general type.
 */
public final class ConditionFields {

    private ConditionFields() {}

    /**
     * {@code EventId} identifies one <em>transition</em>, not a state. It is defined on {@code BaseEventType},
     * so every event carries it, and it must be selected: without it a downstream system cannot acknowledge,
     * because {@code Acknowledge} names the transition it applies to.
     */
    public static final @NotNull String EVENT_ID = "EventId";

    /**
     * The browse paths of the fields Edge selects, in the order they are selected. The event callback receives
     * values positionally in exactly this order, so the two must stay in step.
     */
    public static final @NotNull List<String> SELECTED = List.of(
            // --- BaseEventType: present on every event
            EVENT_ID,
            "EventType",
            "SourceNode",
            "SourceName",
            "Time",
            "ReceiveTime",
            "Message",
            "Severity",
            // --- ConditionType
            "ConditionName",
            "BranchId",
            "Retain",
            "EnabledState",
            "Quality",
            "LastSeverity",
            "Comment",
            "ClientUserId",
            // --- AcknowledgeableConditionType
            "AckedState",
            "ConfirmedState",
            // --- AlarmConditionType
            "ActiveState",
            "SuppressedState",
            "ShelvingState");
}
