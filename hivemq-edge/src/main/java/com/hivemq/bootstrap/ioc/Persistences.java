/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.bootstrap.ioc;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.persistence.ScheduledCleanUpService;
import com.hivemq.persistence.clientqueue.ClientQueueLocalPersistence;
import com.hivemq.persistence.connection.ConnectionPersistence;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.persistence.local.ClientSessionSubscriptionLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.persistence.retained.RetainedMessageLocalPersistence;
import com.hivemq.persistence.retained.RetainedMessagePersistence;

import javax.inject.Inject;

public class Persistences {

    private final @NotNull ClientQueueLocalPersistence clientQueueLocalPersistence;
    private final @NotNull ClientSessionLocalPersistence clientSessionLocalPersistence;
    private final @NotNull PublishPayloadPersistence payloadPersistence;
    private final @NotNull ClientSessionSubscriptionLocalPersistence clientSessionSubscriptionLocalPersistence;
    private final @NotNull RetainedMessageLocalPersistence retainedMessageLocalPersistence;
    private final @NotNull RetainedMessagePersistence retainedMessagePersistence;
    private final @NotNull ConnectionPersistence connectionPersistence;
    private final @NotNull ScheduledCleanUpService scheduledCleanUpService;
    private final @NotNull MessageDroppedService messageDroppedService;

    @Inject
    public Persistences(
            final @NotNull ClientQueueLocalPersistence clientQueueLocalPersistence,
            final @NotNull ClientSessionLocalPersistence clientSessionLocalPersistence,
            final @NotNull PublishPayloadPersistence payloadPersistence,
            final @NotNull ClientSessionSubscriptionLocalPersistence clientSessionSubscriptionLocalPersistence,
            final @NotNull RetainedMessageLocalPersistence retainedMessageLocalPersistence,
            final @NotNull RetainedMessagePersistence retainedMessagePersistence,
            final @NotNull ConnectionPersistence connectionPersistence,
            final @NotNull ScheduledCleanUpService scheduledCleanUpService,
            final @NotNull MessageDroppedService messageDroppedService) {
        this.clientQueueLocalPersistence = clientQueueLocalPersistence;
        this.clientSessionLocalPersistence = clientSessionLocalPersistence;
        this.payloadPersistence = payloadPersistence;
        this.clientSessionSubscriptionLocalPersistence = clientSessionSubscriptionLocalPersistence;
        this.retainedMessageLocalPersistence = retainedMessageLocalPersistence;
        this.retainedMessagePersistence = retainedMessagePersistence;
        this.connectionPersistence = connectionPersistence;
        this.scheduledCleanUpService = scheduledCleanUpService;
        this.messageDroppedService = messageDroppedService;
    }

    public @NotNull ClientQueueLocalPersistence queueLocal() {
        return clientQueueLocalPersistence;
    }

    public @NotNull ClientSessionLocalPersistence sessionLocal() {
        return clientSessionLocalPersistence;
    }

    public @NotNull ClientSessionSubscriptionLocalPersistence subscriptionLocal() {
        return clientSessionSubscriptionLocalPersistence;
    }

    public @NotNull RetainedMessageLocalPersistence retainedMessageLocal() {
        return retainedMessageLocalPersistence;
    }

    public @NotNull ConnectionPersistence connectionPersistence() {
        return connectionPersistence;
    }

    public @NotNull ScheduledCleanUpService scheduledCleanUpService() {
        return scheduledCleanUpService;
    }

    public @NotNull MessageDroppedService messageDroppedService() {
        return messageDroppedService;
    }

    public @NotNull PublishPayloadPersistence payloadPersistence() {
        return payloadPersistence;
    }
}
