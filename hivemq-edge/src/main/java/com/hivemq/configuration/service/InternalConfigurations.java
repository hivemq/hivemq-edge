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
package com.hivemq.configuration.service;

import com.hivemq.migration.meta.PersistenceType;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class InternalConfigurations {

    private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
    private static final int AVAILABLE_PROCESSORS_TIMES_TWO = Runtime.getRuntime().availableProcessors() * 2;

    /* ***************
     *  Persistences *
     *****************/

    /**
     * The "persistence shutdown grace period" represents the time span,
     * in which single writer tasks are still processed after the persistence shutdown hook was called.
     */
    public static final AtomicInteger PERSISTENCE_SHUTDOWN_GRACE_PERIOD_MSEC = new AtomicInteger(500);

    /**
     * timeout to wait for persistence shutdown to finish on HiveMQ shutdown
     */
    public static final AtomicInteger PERSISTENCE_SHUTDOWN_TIMEOUT_SEC = new AtomicInteger(300);

    public static final AtomicInteger PERSISTENCE_BUCKET_COUNT = new AtomicInteger(1);
    public static final AtomicInteger SINGLE_WRITER_THREAD_POOL_SIZE = new AtomicInteger(AVAILABLE_PROCESSORS);
    public static final AtomicInteger SINGLE_WRITER_CREDITS_PER_EXECUTION = new AtomicInteger(65);
    public static final AtomicInteger SINGLE_WRITER_INTERVAL_TO_CHECK_PENDING_TASKS_AND_SCHEDULE_MSEC = new AtomicInteger(500);

    public static final AtomicInteger PERSISTENCE_CLOSE_RETRIES = new AtomicInteger(500);
    public static final AtomicInteger PERSISTENCE_CLOSE_RETRY_INTERVAL_MSEC = new AtomicInteger(100);

    /**
     * max amount of subscriptions to pull from the peristence for extension iterate chunk
     */
    public static final int PERSISTENCE_SUBSCRIPTIONS_MAX_CHUNK_SIZE = 2000;

    /**
     * max amount of clients to pull from the peristence for extension iterate chunk
     */
    public static final int PERSISTENCE_CLIENT_SESSIONS_MAX_CHUNK_SIZE = 2000;

    /**
     * max amount of memory for retained messages to pull from the peristence for extension iterate chunk
     */
    public static final int PERSISTENCE_RETAINED_MESSAGES_MAX_CHUNK_MEMORY_BYTES = 10485760; //10 MByte

    /**
     * The threshold at which the topic tree starts to map entries instead of storing them in an array
     */
    public static final AtomicInteger TOPIC_TREE_MAP_CREATION_THRESHOLD = new AtomicInteger(16);

    /**
     * The configuration for qos 0 memory hard limit divisor, must be greater than 0.
     */
    public static final AtomicInteger QOS_0_MEMORY_HARD_LIMIT_DIVISOR = new AtomicInteger(4);

    /**
     * The configuration for qos 0 memory limit per client, must be greater than 0.
     */
    public static final AtomicInteger QOS_0_MEMORY_LIMIT_PER_CLIENT_BYTES = new AtomicInteger(1024 * 1024 * 5);

    /**
     * The amount of qos 0 messages that are queued if the channel is not writable
     */
    public static final AtomicInteger NOT_WRITABLE_QUEUE_SIZE = new AtomicInteger(1000);

    public static AtomicBoolean DEFAULT_USAGE_EVENTS_ENABLED = new AtomicBoolean(true);

    /**
     * The limit of unacknowledged messages that hivemq will handle, regardless of the client receive maximum
     */
    public static int MAX_INFLIGHT_WINDOW_SIZE_MESSAGES = 50;

    /**
     * The maximum allowed size of the passed value for the ConnectionAttributeStore
     */
    public static final int CONNECTION_ATTRIBUTE_STORE_MAX_VALUE_SIZE_BYTES = 10240; //10Kb

    /**
     * The amount of publishes that are polled per batch
     */
    public static int PUBLISH_POLL_BATCH_SIZE = 50;

    /**
     * The amount of bytes that are polled per batch (one publish min)
     */
    public static final int PUBLISH_POLL_BATCH_SIZE_BYTES = 1024 * 1024 * 5; // 5Mb

    /**
     * The amount of qos > 0 retained messages that are queued
     */
    public static final AtomicInteger RETAINED_MESSAGE_QUEUE_SIZE = new AtomicInteger(10_000);

    /**
     * The configuration if rocks db is used instead of xodus for retained messages.
     */
    public static final AtomicReference<PersistenceType> RETAINED_MESSAGE_PERSISTENCE_TYPE = new AtomicReference<>(PersistenceType.IN_MEMORY);


    /* *****************
     *      SSL       *
     *******************/

    public static final boolean SSL_RELOAD_ENABLED = true;
    public static final int SSL_RELOAD_INTERVAL_SEC = 10;

    /* *****************
     *      Metrics     *
     *******************/

    /**
     * register metrics for jmx reporting on startup if enabled
     */
    public static final AtomicBoolean JMX_REPORTER_ENABLED = new AtomicBoolean(true);

    /* *****************
     *      MQTT 5     *
     *******************/

    public static final AtomicInteger TOPIC_ALIAS_GLOBAL_MEMORY_HARD_LIMIT_BYTES = new AtomicInteger(1024 * 1024 * 200); //200Mb
    public static final AtomicInteger TOPIC_ALIAS_GLOBAL_MEMORY_SOFT_LIMIT_BYTES = new AtomicInteger(1024 * 1024 * 50); //50Mb

    public static final AtomicBoolean DISCONNECT_WITH_REASON_CODE_ENABLED = new AtomicBoolean(true);
    public static final AtomicBoolean DISCONNECT_WITH_REASON_STRING_ENABLED = new AtomicBoolean(true);

    public static final AtomicBoolean CONNACK_WITH_REASON_CODE_ENABLED = new AtomicBoolean(true);
    public static final AtomicBoolean CONNACK_WITH_REASON_STRING_ENABLED = new AtomicBoolean(true);

    public static final int USER_PROPERTIES_MAX_SIZE_BYTES = 1024 * 1024 * 5; //5Mb

    public static final boolean LOG_CLIENT_REASON_STRING_ON_DISCONNECT_ENABLED = true;

    /* ***********************
     *    Extension System   *
     *************************/

    public static final AtomicInteger EXTENSION_TASK_QUEUE_EXECUTOR_THREADS_COUNT = new AtomicInteger(AVAILABLE_PROCESSORS);
    public static final AtomicInteger MANAGED_EXTENSION_THREAD_POOL_KEEP_ALIVE_SEC = new AtomicInteger(30);
    public static final AtomicInteger MANAGED_EXTENSION_THREAD_POOL_THREADS_COUNT = new AtomicInteger(AVAILABLE_PROCESSORS);
    public static final AtomicInteger BRIDGE_MESSAGE_FORWARDER_POOL_THREADS_COUNT = new AtomicInteger(Math.max(2, AVAILABLE_PROCESSORS/2));

    /**
     * The amount of time the extension executor shutdown awaits task termination until shutdownNow() is called.
     */
    public static final AtomicInteger MANAGED_EXTENSION_EXECUTOR_SHUTDOWN_TIMEOUT_SEC = new AtomicInteger(180);

    public static final AtomicInteger EXTENSION_SERVICE_CALL_RATE_LIMIT_PER_SEC = new AtomicInteger(0); //unlimited

    /* ********************
     *        Auth        *
     **********************/
    /**
     * Denies bypassing of authentication if no authenticator is registered.
     */
    public static final AtomicBoolean AUTH_DENY_UNAUTHENTICATED_CONNECTIONS = new AtomicBoolean(true);

    public static final AtomicInteger AUTH_PROCESS_TIMEOUT_SEC = new AtomicInteger(30);

    /* *****************
     *     Misc     *
     *******************/

    /**
     * The concurrency level of the shared subscription cache
     */
    public static final AtomicInteger SHARED_SUBSCRIPTION_CACHE_CONCURRENCY_LEVEL = new AtomicInteger(AVAILABLE_PROCESSORS);

    /**
     * The concurrency level of the shared subscriber service cache
     */
    public static final AtomicInteger SHARED_SUBSCRIBER_CACHE_CONCURRENCY_LEVEL = new AtomicInteger(AVAILABLE_PROCESSORS);

    public static final AtomicInteger INTERVAL_BETWEEN_CLEANUP_JOBS_SEC = new AtomicInteger(4);

    public static final AtomicBoolean MQTT_ALLOW_DOLLAR_TOPICS = new AtomicBoolean(false);

    public static final AtomicInteger MQTT_EVENT_EXECUTOR_THREAD_COUNT = new AtomicInteger(AVAILABLE_PROCESSORS_TIMES_TWO);

    /**
     * The amount of cleanup job tasks that are processed at the same time, in each schedule interval
     */
    public static final AtomicBoolean ACKNOWLEDGE_INCOMING_PUBLISH_AFTER_PERSISTING_ENABLED = new AtomicBoolean(true);

    public static final long SHARED_SUBSCRIPTION_CACHE_TIME_TO_LIVE_MSEC = 1000;

    public static final int SHARED_SUBSCRIPTION_CACHE_MAX_SIZE_SUBSCRIPTIONS = 10000;

    public static final AtomicInteger COUNT_OF_PUBLISHES_WRITTEN_TO_CHANNEL_TO_TRIGGER_FLUSH = new AtomicInteger(128);

    public static final long SHARED_SUBSCRIBER_CACHE_TIME_TO_LIVE_MSEC = 1000;

    public static final int SHARED_SUBSCRIBER_CACHE_MAX_SIZE_SUBSCRIBERS = 10000;

    public static final int CLEANUP_JOB_PARALLELISM = 1;

    /**
     * The timeout for a cleanup job task.
     */
    public static final int CLEANUP_JOB_TASK_TIMEOUT_SEC = 300;

    /**
     * set to true to close all client connections at netty-event-loop shutdown
     */
    public static final boolean NETTY_SHUTDOWN_LEGACY = false;
    public static final int NETTY_COUNT_OF_CONNECTIONS_IN_SHUTDOWN_PARTITION = 100;

    public static final double MQTT_CONNECTION_KEEP_ALIVE_FACTOR = 1.5;

    public static final long DISCONNECT_KEEP_ALIVE_BATCH = 100;

    public static final int EVENT_LOOP_GROUP_SHUTDOWN_TIMEOUT_MILLISEC = 500;
    public static final int CONNECTION_PERSISTENCE_SHUTDOWN_TIMEOUT_MILLISEC = 500;

    public static final boolean DROP_MESSAGES_QOS_0_ENABLED = true;

    public static final int WILL_DELAY_CHECK_INTERVAL_SEC = 1;

    public static final int LISTENER_SOCKET_RECEIVE_BUFFER_SIZE_BYTES = -1;
    public static final int LISTENER_SOCKET_SEND_BUFFER_SIZE_BYTES = -1;
    public static final int LISTENER_CLIENT_WRITE_BUFFER_HIGH_THRESHOLD_BYTES = 65536; // 64Kb
    public static final int LISTENER_CLIENT_WRITE_BUFFER_LOW_THRESHOLD_BYTES = 32768;  // 32Kb

    public static final int OUTGOING_BANDWIDTH_THROTTLING_DEFAULT_BYTES_PER_SEC = 0; // unlimited

    public static boolean EXPIRE_INFLIGHT_MESSAGES_ENABLED = false;
    public static boolean EXPIRE_INFLIGHT_PUBRELS_ENABLED = false;

    /**
     * When this amount of in-flight messages is reached, the forwarder stops message polling.
     */
    public static final int FORWARDER_POLL_THRESHOLD_MESSAGES = 100;

    /* ********************
     *       HTTP API     *
     **********************/

    public static final AtomicInteger HTTP_API_THREAD_COUNT = new AtomicInteger(2);
    public static final AtomicInteger HTTP_API_SHUTDOWN_TIME_SECONDS = new AtomicInteger(2);


    /* ********************
     *       EDGE RUNTIME     *
     **********************/

    public static final AtomicInteger ADAPTER_RUNTIME_MAX_APPLICATION_ERROR_BACKOFF = new AtomicInteger(60 * 10 * 1000); //-- 10 minutes
    public static final AtomicInteger ADAPTER_RUNTIME_JOB_EXECUTION_TIMEOUT_MILLIS = new AtomicInteger(60 * 1000);  //-- 60 Seconds
    public static final AtomicInteger ADAPTER_RUNTIME_ALLOWED_TIMEOUT_ERRORS_BEFORE_INTERRUPT = new AtomicInteger(10);
}
