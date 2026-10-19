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

    /**
     * How many times a server reported that a monitored item's event queue overflowed — that is, how many
     * times condition transitions were dropped before Edge could read them.
     * <p>
     * Worth a metric of its own rather than folding into the error count: every other counter here reports
     * something Edge failed to do, while this one reports data that no longer exists. A refresh restores the
     * current picture but not the transitions in between, so this is the only number that says the alarm
     * history has a hole in it.
     */
    public static final String METRIC_SUBSCRIPTION_EVENT_QUEUE_OVERFLOW_COUNT =
            "subscription.event.queue.overflow.count";

    public static final @NotNull String ID_REGEX = "^([a-zA-Z_0-9-_])*$";
    public static final @NotNull byte[] EMPTY_BYTES = new byte[] {};

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

    // region Structured builtin field names
    //
    // Four OPC-UA builtin types have no single JSON form, so Edge publishes each as an object and accepts the
    // same object back. These are that object's field names, and they are a **published contract**: they
    // appear in every generated schema a user reads and in every message a user writes, so renaming one is a
    // breaking change rather than a refactor.
    //
    // Named here because the contract is spelled in six places -- OpcUaToJsonConverter and
    // OpcUaEventToJsonConverter write them, JsonSchemaGenerator and ConditionSchemas declare them,
    // JsonToOpcUAConverterUtil reads them on the value-write path, and MethodArguments validates them for a
    // method's arguments. Six independent spellings of one contract, where a typo in any single one fails
    // nothing: the published shape and the accepted shape simply stop agreeing, for one type, silently.
    //
    // Reported by Sam while reviewing EDG-1087, as `code` alongside the magic numbers.

    /// A NodeId's namespace index. Separate from the QualifiedName field of the same spelling: the two types
    /// are independent contracts that happen to agree today, and one constant for both would make a change to
    /// either silently change the other.
    public static final @NotNull String NODE_ID_NAMESPACE_INDEX = "namespaceIndex";

    /// Which of the four identifier kinds a NodeId carries -- 0 numeric, 1 string, 2 guid, 3 opaque, per
    /// OPC 10000-6 section 5.2.2.2. Spelled `idType`; named without the doubled word.
    public static final @NotNull String NODE_ID_TYPE = "idType";

    /// A NodeId's identifier itself, spelled `id`. "Identifier" is the OPC-UA term.
    public static final @NotNull String NODE_ID_IDENTIFIER = "id";

    /// A QualifiedName's namespace index. See the note on {@link #NODE_ID_NAMESPACE_INDEX} for why this is
    /// its own constant.
    public static final @NotNull String QUALIFIED_NAME_NAMESPACE_INDEX = "namespaceIndex";

    /// A QualifiedName's string half.
    public static final @NotNull String QUALIFIED_NAME_NAME = "name";

    /// A StatusCode's numeric value, a UInt32.
    public static final @NotNull String STATUS_CODE_CODE = "code";

    /// A StatusCode's symbolic name, such as `Bad_NotExecutable`. Published for a reader's benefit and
    /// accepted but not authoritative on the way in -- the number decides.
    public static final @NotNull String STATUS_CODE_SYMBOL = "symbol";

    /// A LocalizedText's locale. Absent or null means the server stated none, which is different from the
    /// empty string.
    public static final @NotNull String LOCALIZED_TEXT_LOCALE = "locale";

    /// A LocalizedText's text.
    public static final @NotNull String LOCALIZED_TEXT_TEXT = "text";
    // endregion
}
