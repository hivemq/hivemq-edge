package com.hivemq.bootstrap.factories;

import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.publish.PUBLISH;

public class HandlerResult {

    private final boolean preventPublish;
    private final @Nullable PUBLISH modifiedPublish;

    public HandlerResult(boolean preventPublish, @Nullable PUBLISH modifiedPublish) {
        this.preventPublish = preventPublish;
        this.modifiedPublish = modifiedPublish;
    }

    public boolean isPreventPublish() {
        return preventPublish;
    }

    public @Nullable PUBLISH getModifiedPublish() {
        return modifiedPublish;
    }
}
