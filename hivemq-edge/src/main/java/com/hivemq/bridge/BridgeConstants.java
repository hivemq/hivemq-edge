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
package com.hivemq.bridge;

import com.hivemq.extension.sdk.api.annotations.NotNull;

public class BridgeConstants {

    /**
     * Special connect user property to disable the overload protection on bridge clients.
     */
    public static final @NotNull String HMQ_BRIDGE_TOKEN = "hmq-bridge-token";

    /**
     * Special publish user property that holds a counter to prevent loops.
     */
    public static final @NotNull String HMQ_BRIDGE_HOP_COUNT = "hmq-bridge-hop-count";

    public static final @NotNull String BRIDGE_NAME_TOPIC_REPLACEMENT_TOKEN = "bridge.name";
}
