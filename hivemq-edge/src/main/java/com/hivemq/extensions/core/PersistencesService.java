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

    private @Nullable RetainedMessageLocalPersistenceFactory retainedMessageLocalPersistenceFactory;
    private @Nullable ClientSessionLocalPersistenceFactory clientSessionLocalPersistenceFactory;
    private @Nullable ClientQueueLocalPersistenceFactory clientQueueLocalPersistenceFactory;
    private @Nullable PublishPayloadPersistenceFactory publishPayloadPersistenceFactory;
    private @Nullable ClientSessionSubscriptionLocalPersistenceFactory clientSessionSubscriptionLocalPersistenceFactory;

    public void supplyClientSessionLocalPersistenceFactory(final @NotNull ClientSessionLocalPersistenceFactory clientSessionLocalPersistenceFactory) {
        this.clientSessionLocalPersistenceFactory = clientSessionLocalPersistenceFactory;
    }

    public void supplyClientQueueLocalPersistenceFactory(
            final @NotNull ClientQueueLocalPersistenceFactory clientQueueLocalPersistenceFactory) {
        this.clientQueueLocalPersistenceFactory = clientQueueLocalPersistenceFactory;
    }

    public void supplyClientSessionSubscriptionLocalPersistenceFactory(final @NotNull ClientSessionSubscriptionLocalPersistenceFactory clientSessionSubscriptionLocalPersistenceFactory) {
        this.clientSessionSubscriptionLocalPersistenceFactory = clientSessionSubscriptionLocalPersistenceFactory;
    }

    public void supplyPayloadPersistenceFactory(
            final @NotNull PublishPayloadPersistenceFactory publishPayloadPersistenceFactory) {
        this.publishPayloadPersistenceFactory = publishPayloadPersistenceFactory;
    }

    public void supplyRetainedMessageLocalPersistenceFactory(final @NotNull RetainedMessageLocalPersistenceFactory retainedMessageLocalPersistenceFactory) {
         this.retainedMessageLocalPersistenceFactory = retainedMessageLocalPersistenceFactory;
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
}
