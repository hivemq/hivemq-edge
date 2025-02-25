package com.hivemq.bootstrap.factories;

import com.hivemq.combining.mapping.DataCombiningTransformationService;
import com.hivemq.mqtt.services.InternalPublishService;
import org.jetbrains.annotations.NotNull;

public interface DataCombiningTransformationServiceFactory {
    @NotNull
    DataCombiningTransformationService build(final @NotNull InternalPublishService internalPublishService);
}
