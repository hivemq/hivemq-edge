package com.hivemq.bootstrap.factories;

import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extensions.core.HandlerService;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.dropping.IncomingPublishDropper;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;

import javax.inject.Inject;

public class AdapterHandlingProvider {

    private final @NotNull HandlerService handlerService;
    private final @NotNull MqttConnacker mqttConnacker;
    private final @NotNull MqttServerDisconnector mqttServerDisconnector;
    private final @NotNull IncomingPublishDropper incomingPublishDropper;
    private final @NotNull MessageDroppedService messageDroppedService;
    private final @NotNull ConfigurationService configurationService;

    @Inject
    public AdapterHandlingProvider(
            final @NotNull HandlerService handlerService,
            final @NotNull MqttConnacker mqttConnacker,
            final @NotNull MqttServerDisconnector mqttServerDisconnector,
            final @NotNull IncomingPublishDropper incomingPublishDropper,
            final @NotNull MessageDroppedService messageDroppedService,
            final @NotNull ConfigurationService configurationService) {
        this.handlerService = handlerService;
        this.mqttConnacker = mqttConnacker;
        this.mqttServerDisconnector = mqttServerDisconnector;
        this.incomingPublishDropper = incomingPublishDropper;
        this.messageDroppedService = messageDroppedService;
        this.configurationService = configurationService;
    }

    public @Nullable AdapterHandling get() {
        final AdapterHandlingFactory adapterHandlingFactory = handlerService.getAdapterHandlerFactory();
        if (adapterHandlingFactory == null) {
            return null;
        }
        return adapterHandlingFactory.build(mqttConnacker,
                mqttServerDisconnector,
                incomingPublishDropper,
                configurationService,
                messageDroppedService);
    }
}
