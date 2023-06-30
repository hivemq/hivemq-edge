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
package com.hivemq.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;

/**
 * This class holds a constant {@link HiveMQMetric} for every metric which is provided by HiveMQ
 */
public class HiveMQMetrics {

    public static final String HIVEMQ_PREFIX = "com.hivemq.edge.";
    public static final String PROTOCOL_ADAPTER_PREFIX = "com.hivemq.edge.protocol-adapters.";
    /**
     * represents a {@link Counter}, which counts every incoming MQTT message
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> INCOMING_MESSAGE_COUNT =
            HiveMQMetric.valueOf(HIVEMQ_PREFIX + "messages.incoming.total.count", Counter.class);

    /**
     * represents a {@link Counter}, which counts every outgoing MQTT message
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> OUTGOING_MESSAGE_COUNT =
            HiveMQMetric.valueOf(HIVEMQ_PREFIX + "messages.outgoing.total.count", Counter.class);

    /**
     * represents a {@link Counter}, which counts every incoming MQTT CONNECT messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> INCOMING_CONNECT_COUNT =
            HiveMQMetric.valueOf(HIVEMQ_PREFIX + "messages.incoming.connect.count", Counter.class);

    /**
     * represents a {@link Counter}, which counts every incoming MQTT PUBLISH messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> INCOMING_PUBLISH_COUNT =
            HiveMQMetric.valueOf(HIVEMQ_PREFIX + "messages.incoming.publish.count", Counter.class);

    /**
     * represents a {@link Counter}, which counts every outgoing MQTT PUBLISH messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> OUTGOING_PUBLISH_COUNT =
            HiveMQMetric.valueOf(HIVEMQ_PREFIX + "messages.outgoing.publish.count", Counter.class);

    /**
     * represents a {@link Counter}, which counts every dropped PUBLISH messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> DROPPED_MESSAGE_COUNT =
            HiveMQMetric.valueOf(HIVEMQ_PREFIX + "messages.dropped.count", Counter.class);

    /**
     * represents a {@link Gauge}, which holds the current amount of retained messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Gauge<Number>> RETAINED_MESSAGES_CURRENT =
            HiveMQMetric.gaugeValue(HIVEMQ_PREFIX + "messages.retained.current");

    /**
     * represents a {@link Gauge}, which holds the total amount of read bytes
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Gauge<Number>> BYTES_READ_TOTAL =
            HiveMQMetric.gaugeValue(HIVEMQ_PREFIX + "networking.bytes.read.total");

    /**
     * represents a {@link Gauge}, which holds total of written bytes
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Gauge<Number>> BYTES_WRITE_TOTAL =
            HiveMQMetric.gaugeValue(HIVEMQ_PREFIX + "networking.bytes.write.total");

    /**
     * represents a {@link Gauge}, which holds the current total number of connections
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Gauge<Number>> CONNECTIONS_OVERALL_CURRENT =
            HiveMQMetric.gaugeValue(HIVEMQ_PREFIX + "networking.connections.current");

    /**
     * represents a {@link Counter}, which is increased every time a network connection is closed
     *
     * @since 3.4
     */
    public static final HiveMQMetric<Counter> CONNECTIONS_CLOSED_COUNT =
            HiveMQMetric.valueOf(HIVEMQ_PREFIX + "networking.connections-closed.total.count", Counter.class);

    /**
     * represents a {@link Counter}, which measures the current count of subscriptions
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> SUBSCRIPTIONS_CURRENT =
            HiveMQMetric.valueOf(HIVEMQ_PREFIX + "subscriptions.overall.current", Counter.class);

    /**
     * represents a {@link Gauge}, which measures the current count of stored sessions
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Gauge<Number>> CLIENT_SESSIONS_CURRENT =
            HiveMQMetric.gaugeValue(HIVEMQ_PREFIX + "sessions.overall.current");

    /**
     * represents a {@link Gauge}, which measures the approximate memory usage of the retained message persistence if
     * the memory persistence is used.
     */
    public static final HiveMQMetric<Gauge<Number>> RETAINED_MESSAGES_MEMORY_PERSISTENCE_TOTAL_SIZE =
            HiveMQMetric.gaugeValue(HIVEMQ_PREFIX + "persistence.retained-messages.in-memory.total-size");

    /**
     * represents a {@link Gauge}, which measures the approximate memory usage of the subscription persistence if
     * the memory persistence is used.
     */
    public static final HiveMQMetric<Gauge<Number>> CLIENT_SESSION_SUBSCRIPTIONS_MEMORY_PERSISTENCE_TOTAL_SIZE =
            HiveMQMetric.gaugeValue(HIVEMQ_PREFIX + "persistence.client-session.subscriptions.in-memory.total-size");

    /**
     * represents a {@link Gauge}, which measures the approximate memory usage of the client session persistence if
     * the memory persistence is used.
     */
    public static final HiveMQMetric<Gauge<Number>> CLIENT_SESSIONS_MEMORY_PERSISTENCE_TOTAL_SIZE =
            HiveMQMetric.gaugeValue(HIVEMQ_PREFIX + "persistence.client-sessions.in-memory.total-size");

    /**
     * represents a {@link Gauge}, which measures the approximate memory usage of the queued message persistence if
     * the memory persistence is used.
     */
    public static final HiveMQMetric<Gauge<Number>> QUEUED_MESSAGES_MEMORY_PERSISTENCE_TOTAL_SIZE =
            HiveMQMetric.gaugeValue(HIVEMQ_PREFIX + "persistence.queued-messages.in-memory.total-size");

    /**
     * represents a {@link Counter} for the number of MQTT client channels which are currently not writable.
     */
    public static final HiveMQMetric<Counter> MQTT_CONNECTION_NOT_WRITABLE_CURRENT =
            HiveMQMetric.valueOf(HIVEMQ_PREFIX + "mqtt.connection.not-writable.current", Counter.class);

    /**
     * Represents a {@link Counter}, which holds the current amount of stored LWT messages.
     *
     * @since 2022.1
     */
    public static final HiveMQMetric<Counter> WILL_MESSAGE_COUNT =
            HiveMQMetric.valueOf(HIVEMQ_PREFIX + "messages.will.count.current", Counter.class);


    /**
     * Represents a {@link Counter}, which holds the total amount of published LWT messages.
     *
     * @since 2022.1
     */
    public static final HiveMQMetric<Counter> WILL_MESSAGE_PUBLISHED_COUNT_TOTAL =
            HiveMQMetric.valueOf(HIVEMQ_PREFIX + "messages.will.published.count.total", Counter.class);


}

