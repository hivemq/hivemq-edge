package com.hivemq.bootstrap.factories;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.persistence.retained.RetainedMessageLocalPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;

public interface RetainedMessageLocalPersistenceFactory {

    @NotNull RetainedMessageLocalPersistence buildRetainedMessageLocalPersistence(@NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
                                                                                  @NotNull PublishPayloadPersistence payloadPersistence,
                                                                                  @NotNull PersistenceStartup persistenceStartup);
}
