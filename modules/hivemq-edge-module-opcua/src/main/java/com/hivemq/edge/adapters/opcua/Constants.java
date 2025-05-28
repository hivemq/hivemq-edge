package com.hivemq.edge.adapters.opcua;

import com.hivemq.edge.adapters.opcua.config.SecPolicy;
import org.jetbrains.annotations.NotNull;

public class Constants {
    public static final String METRIC_SUBSCRIPTION_KEEPALIVE_COUNT = "subscription.keepalive.count";
    public static final String METRIC_SUBSCRIPTION_TRANSFER_FAILED_COUNT = "subscription.transfer.failed.count";
    public static final String METRIC_SUBSCRIPTION_DATA_RECEIVED_COUNT = "subscription.data.received.count";
    public static final String METRIC_SUBSCRIPTION_DATA_ERROR_COUNT = "subscription.data.error.count";
    public static final String METRIC_SUBSCRIPTION_SERVICE_FAULT_COUNT = "subscription.service.fault.count";
    public static final String METRIC_SESSION_INACTIVE_COUNT = "session.inactive.count";
    public static final String METRIC_SESSION_ACTIVE_COUNT = "session.active.count";
    static final int CURRENT_CONFIG_VERSION = 1;

    public static final @NotNull byte[] EMTPY_BYTES = new byte[]{};

    public static final @NotNull String PROTOCOL_ID_OPCUA = "opcua";
    public static final @NotNull String ID_REGEX = "^([a-zA-Z_0-9-_])*$";

    public static final @NotNull String INTEGER_DATA_TYPE = "integer";
    public static final @NotNull String ARRAY_DATA_TYPE = "array";
    public static final @NotNull String ARRAY_ITEMS = "items";
    public static final @NotNull String ARRAY_MAX_TIMES = "maxItems";
    public static final @NotNull String MINIMUM_KEY_WORD = "minimum";
    public static final @NotNull String MAXIMUM_KEY_WORD = "maximum";

    //session details
    public static final @NotNull String OPCUA_APPLICATION_NAME = "HiveMQ Edge";
    public static final @NotNull String OPCUA_APPLICATION_URI = "urn:hivemq:edge:client";
    public static final @NotNull String OPCUA_PRODUCT_URI = "https://github.com/hivemq/hivemq-edge";
    public static final @NotNull String OPCUA_SESSION_NAME_PREFIX = "HiveMQ Edge ";

    public static final @NotNull SecPolicy DEFAULT_SECURITY_POLICY = SecPolicy.NONE;
}
