package com.hivemq.bootstrap.factories;

import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.dropping.IncomingPublishDropper;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;

public interface AdapterHandlingFactory {

    @NotNull AdapterHandling build(final @NotNull MqttConnacker mqttConnacker,
                                   final @NotNull MqttServerDisconnector mqttServerDisconnector,
                                   final @NotNull IncomingPublishDropper incomingPublishDropper,
                                   final @NotNull ConfigurationService configurationService,
                                   final @NotNull MessageDroppedService messageDroppedService);


}
