package com.hivemq.combining.mapping;

import com.hivemq.combining.model.DataCombining;
import com.hivemq.mqtt.message.publish.PUBLISH;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface DataCombiningTransformationService {

    @NotNull CompletableFuture<Void> applyMappings(
            final @NotNull PUBLISH mergedPublish, final @NotNull DataCombining dataCombining);

}
