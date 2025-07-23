package com.hivemq.mqtt.services;


import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.api.mqtt.PublishReturnCode;
import com.hivemq.mqtt.message.publish.PUBLISH;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ExecutorService;

public interface PrePublishProcessorService {

    @NotNull ListenableFuture<PublishReturnCode> publish(
            @NotNull PUBLISH publish,
            @NotNull ExecutorService executorService,
            @Nullable String sender);


    /**
     * Send a message to all clients and shared subscription groups which have an active subscription
     *
     * @param publish         the message to send
     * @param executorService the executor service in which all callbacks are executed
     * @param sender          client identifier of the client which sent the message
     */
    @NotNull ListenableFuture<PublishReturnCode> applyDataHubAndPublish(
            final @NotNull PUBLISH publish,
            final @NotNull ExecutorService executorService,
            final @NotNull String sender);
}
