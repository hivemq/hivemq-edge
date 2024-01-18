package com.hivemq.bootstrap.factories;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class HandlerPackage {


    private final @NotNull ChannelInboundHandlerAdapter handler;
    private final @NotNull String handlerName;

    public HandlerPackage(final @NotNull ChannelInboundHandlerAdapter handler, final @NotNull String handlerName) {
        this.handler = handler;
        this.handlerName = handlerName;
    }

    public @NotNull ChannelInboundHandlerAdapter getHandler() {
        return handler;
    }

    public String getHandlerName() {
        return handlerName;
    }
}
