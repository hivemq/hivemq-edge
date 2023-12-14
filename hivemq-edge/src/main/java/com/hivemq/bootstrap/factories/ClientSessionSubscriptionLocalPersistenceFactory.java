package com.hivemq.bootstrap.factories;

import com.hivemq.configuration.service.PersistenceConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.clientqueue.ClientQueueLocalPersistence;
import com.hivemq.persistence.local.ClientSessionSubscriptionLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;

public interface ClientSessionSubscriptionLocalPersistenceFactory {


    @NotNull ClientSessionSubscriptionLocalPersistence buildClientSessionSubscriptionLocalPersistence(@NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
                                                                                                      @NotNull PublishPayloadPersistence payloadPersistence,
                                                                                                      @NotNull PersistenceStartup persistenceStartup);
}
