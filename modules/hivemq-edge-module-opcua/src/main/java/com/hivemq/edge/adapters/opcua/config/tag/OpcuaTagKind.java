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

/**
 * What kind of thing an OPC-UA tag points at.
 * <p>
 * Distinct from the node's <em>type</em>, which names the structure of its northbound output and drives
 * schema generation. The kind decides how the node is observed at all — a monitored value, a condition's
 * transitions, a query over a notifier's traffic, or the adapter's refresh channel — and therefore which
 * schemas apply:
 * <ul>
 *   <li>{@link #VALUE} — read and write schemas both derived from the device;</li>
 *   <li>{@link #CONDITION} — read schema from the type, write schema the fixed transition command;</li>
 *   <li>{@link #EVENT_SUBSCRIPTION} — read schema from the type, writing forbidden;</li>
 *   <li>{@link #REFRESH} — read schema the base event fields, write schema the fixed refresh command.</li>
 * </ul>
 * The kind is stated explicitly rather than inferred from the server: inference is possible in principle
 * (NodeClass, HasTypeDefinition, the EventNotifier attribute) but the EventNotifier bit in particular is
 * unreliable, because servers under-populate it and the Server object is a notifier by convention regardless.
 * So the intent is declared here and then <em>verified</em> against the device when the tag is subscribed.
 */
public enum OpcuaTagKind {

    /**
     * An ordinary variable: the tag's node is a Variable whose {@code Value} attribute is monitored.
     * This is the default, and what every tag was before the kind existed.
     */
    VALUE,

    /**
     * A single condition (an alarm). The tag's node is the condition object itself; its transitions are
     * received as events, and it can be transitioned by calling a method on it.
     */
    CONDITION,

    /**
     * The {@code ConditionRefresh} bracket, and the means to ask for a refresh.
     * <p>
     * Northbound it delivers {@code RefreshStartEventType}, {@code RefreshEndEventType} and
     * {@code RefreshRequiredEventType} — the events a server sends to say a refresh has begun, has ended, or
     * is advisable. Southbound a write requests one.
     * <p>
     * Its {@code node} carries no information: the subscription is placed on the Server object and the call
     * is made on {@code ConditionType}, both well-known. And its filter cannot select these events — they
     * bypass the where clause by specification — so the item is given a filter admitting nothing, leaving
     * exactly the events that cannot be withheld.
     */
    REFRESH,

    /**
     * A query against a notifier, delivering events from potentially many conditions beneath it.
     * Northbound only — there is no single target to write to.
     */
    EVENT_SUBSCRIPTION
}
