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
 * The type is stated explicitly rather than inferred from the server: inference is possible in principle
 * (NodeClass, HasTypeDefinition, the EventNotifier attribute) but the EventNotifier bit in particular is
 * unreliable, because servers under-populate it and the Server object is a notifier by convention regardless.
 * So the intent is declared here and then <em>verified</em> against the device when the tag is subscribed.
 */
public enum OpcuaTagType {

    /**
     * An ordinary variable: the tag's node is a Variable whose {@code Value} attribute is monitored.
     * This is the default, and what every tag was before the type existed.
     */
    VALUE,

    /**
     * A single condition (an alarm). The tag's node is the condition object itself; its transitions are
     * received as events, and it can be transitioned by calling a method on it.
     */
    CONDITION,

    /**
     * A query against a notifier, delivering events from potentially many conditions beneath it.
     * Northbound only — there is no single target to write to.
     */
    EVENT_SUBSCRIPTION
}
