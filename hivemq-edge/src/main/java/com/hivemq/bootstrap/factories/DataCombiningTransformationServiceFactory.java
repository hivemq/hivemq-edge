package com.hivemq.bootstrap.factories;

import com.hivemq.combining.mapping.DataCombiningTransformationService;
import org.jetbrains.annotations.NotNull;

public interface DataCombiningTransformationServiceFactory {
    @NotNull
    DataCombiningTransformationService build();
}
