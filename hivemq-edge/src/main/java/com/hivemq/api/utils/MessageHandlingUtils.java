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
package com.hivemq.api.utils;

import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.edge.api.model.NorthboundMapping;
import com.hivemq.extension.sdk.api.annotations.NotNull;

public class MessageHandlingUtils {


    public static @NotNull MessageHandlingOptions convert(final @NotNull NorthboundMapping.MessageHandlingOptionsEnum messageHandlingOptionsEnum) {
        switch (messageHandlingOptionsEnum) {
            case MQTT_MESSAGE_PER_TAG:
                return MessageHandlingOptions.MQTTMessagePerTag;
            case MQTT_MESSAGE_PER_SUBSCRIPTION:
                return MessageHandlingOptions.MQTTMessagePerSubscription;
        }
        throw new IllegalArgumentException();
    }
}
