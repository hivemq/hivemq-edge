package com.hivemq.extensions.core;

import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;

public interface CoreModuleService {
    @NotNull SystemInformation systemInformation();

    @NotNull PersistencesService persistenceService();
}
