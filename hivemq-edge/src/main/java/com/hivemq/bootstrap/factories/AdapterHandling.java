package com.hivemq.bootstrap.factories;

import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.adapter.sdk.api.adapters.ProtocolAdapter;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.publish.PUBLISH;

public interface AdapterHandling {

    @NotNull ListenableFuture<HandlerResult> apply(final @NotNull PUBLISH originalPublish, final @NotNull ProtocolAdapter adapterId);

}
