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
