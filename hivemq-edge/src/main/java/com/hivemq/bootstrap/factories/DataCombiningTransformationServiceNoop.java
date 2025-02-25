package com.hivemq.bootstrap.factories;

import com.hivemq.combining.mapping.DataCombiningTransformationService;
import com.hivemq.combining.model.DataCombining;
import com.hivemq.mqtt.message.publish.PUBLISH;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class DataCombiningTransformationServiceNoop implements DataCombiningTransformationService{

    @Override
    public @NotNull CompletableFuture<Void> applyMappings(
            final @NotNull PUBLISH mergedPublish,
            final @NotNull DataCombining dataCombining) {
        throw new UnsupportedOperationException();
    }
}
