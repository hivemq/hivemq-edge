package com.hivemq.bootstrap.factories;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.clientqueue.ClientQueueLocalPersistence;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;

public interface ClientQueueLocalPersistenceFactory {

     @NotNull ClientQueueLocalPersistence buildClientSessionLocalPersistence(
             @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
             @NotNull PublishPayloadPersistence payloadPersistence,
             @NotNull MessageDroppedService messageDroppedService,
             @NotNull PersistenceStartup persistenceStartup);
}
