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
package com.hivemq.edge.adapters.opcua;

import com.hivemq.edge.adapters.opcua.config.SecPolicy;
import org.jetbrains.annotations.NotNull;

public class Constants {
    public static final int CURRENT_CONFIG_VERSION = 1;

    // session details
    public static final @NotNull String OPCUA_APPLICATION_NAME = "HiveMQ Edge";
    public static final @NotNull String OPCUA_APPLICATION_URI = "urn:hivemq:edge:client";
    public static final @NotNull String OPCUA_PRODUCT_URI = "https://github.com/hivemq/hivemq-edge";
    public static final @NotNull String OPCUA_SESSION_NAME_PREFIX = "HiveMQ Edge ";

    // metrics
    public static final String METRIC_SUBSCRIPTION_KEEPALIVE_COUNT = "subscription.keepalive.count";
    public static final String METRIC_SUBSCRIPTION_TRANSFER_FAILED_COUNT = "subscription.transfer.failed.count";
    public static final String METRIC_SUBSCRIPTION_DATA_RECEIVED_COUNT = "subscription.data.received.count";
    public static final String METRIC_SUBSCRIPTION_DATA_ERROR_COUNT = "subscription.data.error.count";
    public static final String METRIC_SUBSCRIPTION_SERVICE_FAULT_COUNT = "subscription.service.fault.count";
    public static final String METRIC_SESSION_INACTIVE_COUNT = "session.inactive.count";
    public static final String METRIC_SESSION_ACTIVE_COUNT = "session.active.count";

    public static final @NotNull String ID_REGEX = "^([a-zA-Z_0-9-_])*$";
    public static final @NotNull byte[] EMPTY_BYTES = new byte[]{};


    // data types
    public static final @NotNull String OBJECT_DATA_TYPE = "object";
    public static final @NotNull String INTEGER_DATA_TYPE = "integer";
    public static final @NotNull String NUMBER_DATA_TYPE = "number";
    public static final @NotNull String BOOLEAN_DATA_TYPE = "boolean";
    public static final @NotNull String ARRAY_DATA_TYPE = "array";
    public static final @NotNull String STRING_DATA_TYPE = "string";
    public static final @NotNull String DATETIME_DATA_TYPE = "date-time";

    // constants
    public static final @NotNull String PROTOCOL_ID_OPCUA = "opcua";
    public static final @NotNull String TYPE = "type";
    public static final @NotNull String ARRAY_ITEMS = "items";
    public static final @NotNull String ARRAY_MAX_TIMES = "maxItems";
    public static final @NotNull String ARRAY_MIN_TIMES = "minItems";
    public static final @NotNull String MINIMUM_KEY_WORD = "minimum";
    public static final @NotNull String MAXIMUM_KEY_WORD = "maximum";



    public static final @NotNull SecPolicy DEFAULT_SECURITY_POLICY = SecPolicy.NONE;
}
