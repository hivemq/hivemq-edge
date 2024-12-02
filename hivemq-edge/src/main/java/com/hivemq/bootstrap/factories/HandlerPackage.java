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
package com.hivemq.bootstrap.factories;

import org.jetbrains.annotations.NotNull;
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
