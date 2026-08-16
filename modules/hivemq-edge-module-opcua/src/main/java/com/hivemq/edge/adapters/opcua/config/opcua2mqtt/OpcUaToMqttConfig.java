/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua.config.opcua2mqtt;

import static java.util.Objects.requireNonNullElse;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.NotNull;

/**
 * Boxed components rather than primitives, and that is the whole point of them.
 * <p>
 * Jackson builds this from the canonical constructor, so a property the document does not carry arrives as
 * whatever the parameter type has for "nothing" — and for a primitive {@code int} that is <b>zero</b>, not the
 * declared default. {@code @ModuleConfigField(defaultValue = "64")} is schema and UI metadata; it describes
 * what a form should prefill and what the API documents, and Jackson never reads it. So every configuration
 * written before {@code eventQueueSize} existed — which is every configuration in the field — deserialised as
 * a request for a queue of zero, silently violating the {@code numberMin = 1} the same annotation declares and
 * bypassing the burst protection the field was added for.
 * <p>
 * A boxed component gives the compact constructor something to distinguish. {@code ConnectionOptions} in the
 * neighbouring package has done it this way since it was written; this record is the one that did not.
 * <p>
 * Boxed but <em>not</em> {@code @Nullable}, which is the part worth stating. Null is something only Jackson
 * ever passes, and only for as long as the compact constructor takes to replace it — no reader of a
 * constructed instance can observe one. Annotating the components would push that transient possibility onto
 * every accessor and make each of the dozen or so call sites unbox a nullable value, which is a worse
 * description of the type than the one-line-wide window it documents.
 * <p>
 * All three components, not only the new one. The two older fields have the same shape and the same latent
 * defect — an {@code opcuaToMqtt} element carrying neither of them read back as a publishing interval of zero
 * — and it has simply never fired because generated configurations write both.
 * <p>
 * An explicit out-of-range value is left alone rather than rejected here. {@code numberMin} is the declared
 * validation surface and it is enforced where configuration is authored; turning a value that has always
 * loaded into a startup failure would be a wider change than the defect being fixed, and a wrong-but-explicit
 * number is a different problem from a value nobody wrote.
 */
public record OpcUaToMqttConfig(
        @JsonProperty("serverQueueSize")
        @ModuleConfigField(
                title = "OPC UA server queue size",
                description = "OPC UA queue size for this subscription on the server",
                numberMin = 1,
                defaultValue = "" + DEFAULT_SERVER_QUEUE_SIZE)
        Integer serverQueueSize,

        @JsonProperty("publishingInterval")
        @ModuleConfigField(
                title = "OPC UA publishing interval [ms]",
                description = "OPC UA publishing interval in milliseconds for this subscription on the server",
                numberMin = 1,
                defaultValue = "" + DEFAULT_PUBLISHING_INTERVAL)
        Integer publishingInterval,

        @JsonProperty("eventQueueSize")
        @ModuleConfigField(
                title = "OPC UA event queue size",
                description = "OPC UA queue size for event subscriptions, such as condition tags. Separate from "
                        + "the value queue size because the same field means something different for events: a "
                        + "queue size of 1 asks the server for the smallest event queue it supports, not for a "
                        + "single entry. Events are transition reports, so an entry dropped from the queue is "
                        + "never re-sent.",
                numberMin = 1,
                defaultValue = "" + DEFAULT_EVENT_QUEUE_SIZE)
        Integer eventQueueSize) {

    /** One entry, which for a value item means a single entry — the OPC UA and Milo default. */
    public static final int DEFAULT_SERVER_QUEUE_SIZE = 1;

    /** Milo's own default publishing interval, kept here so the value has a name. */
    public static final int DEFAULT_PUBLISHING_INTERVAL = 1000;

    /** Deep enough to absorb a refresh burst or several alarms in one publishing cycle. */
    public static final int DEFAULT_EVENT_QUEUE_SIZE = 64;

    @JsonCreator
    public OpcUaToMqttConfig {
        serverQueueSize = requireNonNullElse(serverQueueSize, DEFAULT_SERVER_QUEUE_SIZE);
        publishingInterval = requireNonNullElse(publishingInterval, DEFAULT_PUBLISHING_INTERVAL);
        eventQueueSize = requireNonNullElse(eventQueueSize, DEFAULT_EVENT_QUEUE_SIZE);
    }

    /**
     * Retains the two-argument shape from before events existed, so a caller that predates condition tags
     * keeps compiling and gets the default event queue.
     */
    public OpcUaToMqttConfig(final int serverQueueSize, final int publishingInterval) {
        this(serverQueueSize, publishingInterval, DEFAULT_EVENT_QUEUE_SIZE);
    }

    public static @NotNull OpcUaToMqttConfig defaultOpcUaToMqttConfig() {
        return new OpcUaToMqttConfig(DEFAULT_SERVER_QUEUE_SIZE, DEFAULT_PUBLISHING_INTERVAL, DEFAULT_EVENT_QUEUE_SIZE);
    }
}
