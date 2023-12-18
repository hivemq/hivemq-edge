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
package com.hivemq.extensions.core;

import com.hivemq.bootstrap.factories.ClientQueueLocalPersistenceFactory;
import com.hivemq.bootstrap.factories.ClientSessionLocalPersistenceFactory;
import com.hivemq.bootstrap.factories.ClientSessionSubscriptionLocalPersistenceFactory;
import com.hivemq.bootstrap.factories.PublishPayloadPersistenceFactory;
import com.hivemq.bootstrap.factories.RetainedMessageLocalPersistenceFactory;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

@SuppressWarnings("unused")
public class PersistencesService {

    private volatile boolean filePersistencesPresent = false;

    private @Nullable RetainedMessageLocalPersistenceFactory retainedMessageLocalPersistenceFactory;
    private @Nullable ClientSessionLocalPersistenceFactory clientSessionLocalPersistenceFactory;
    private @Nullable ClientQueueLocalPersistenceFactory clientQueueLocalPersistenceFactory;
    private @Nullable PublishPayloadPersistenceFactory publishPayloadPersistenceFactory;
    private @Nullable ClientSessionSubscriptionLocalPersistenceFactory clientSessionSubscriptionLocalPersistenceFactory;

    public void supplyClientSessionLocalPersistenceFactory(final @NotNull ClientSessionLocalPersistenceFactory clientSessionLocalPersistenceFactory) {
        this.clientSessionLocalPersistenceFactory = clientSessionLocalPersistenceFactory;
        filePersistencesPresent = true;
    }

    public void supplyClientQueueLocalPersistenceFactory(
            final @NotNull ClientQueueLocalPersistenceFactory clientQueueLocalPersistenceFactory) {
        this.clientQueueLocalPersistenceFactory = clientQueueLocalPersistenceFactory;
        filePersistencesPresent = true;
    }

    public void supplyClientSessionSubscriptionLocalPersistenceFactory(final @NotNull ClientSessionSubscriptionLocalPersistenceFactory clientSessionSubscriptionLocalPersistenceFactory) {
        this.clientSessionSubscriptionLocalPersistenceFactory = clientSessionSubscriptionLocalPersistenceFactory;
        filePersistencesPresent = true;
    }

    public void supplyPayloadPersistenceFactory(
            final @NotNull PublishPayloadPersistenceFactory publishPayloadPersistenceFactory) {
        this.publishPayloadPersistenceFactory = publishPayloadPersistenceFactory;
        filePersistencesPresent = true;
    }

    public void supplyRetainedMessageLocalPersistenceFactory(final @NotNull RetainedMessageLocalPersistenceFactory retainedMessageLocalPersistenceFactory) {
         this.retainedMessageLocalPersistenceFactory = retainedMessageLocalPersistenceFactory;
        filePersistencesPresent = true;
    }


    public @Nullable ClientSessionLocalPersistenceFactory getClientSessionLocalPersistenceFactory() {
        return clientSessionLocalPersistenceFactory;
    }

    public @Nullable ClientQueueLocalPersistenceFactory getClientQueueLocalPersistenceFactory() {
        return clientQueueLocalPersistenceFactory;
    }

    public @Nullable PublishPayloadPersistenceFactory getPublishPayloadPersistenceFactory() {
        return publishPayloadPersistenceFactory;
    }

    public @Nullable ClientSessionSubscriptionLocalPersistenceFactory getClientSessionSubscriptionLocalPersistenceFactory() {
        return clientSessionSubscriptionLocalPersistenceFactory;
    }

    public @Nullable RetainedMessageLocalPersistenceFactory getRetainedMessageLocalPersistenceFactory() {
        return retainedMessageLocalPersistenceFactory;
    }

    public boolean isFilePersistencesPresent() {
        return filePersistencesPresent;
    }
}
