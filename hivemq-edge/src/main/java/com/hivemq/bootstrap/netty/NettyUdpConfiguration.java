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
package com.hivemq.bootstrap.netty;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramChannel;

import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The configuration for Netty
 */
@Immutable
@Singleton
public class NettyUdpConfiguration {

    private final Class<? extends DatagramChannel> serverSocketChannelClass;

    private final EventLoopGroup parentEventLoopGroup;
    private final EventLoopGroup childEventLoopGroup;


    public NettyUdpConfiguration(final Class<? extends DatagramChannel> serverSocketChannelClass,
                                 final EventLoopGroup parentEventLoopGroup,
                                 final EventLoopGroup childEventLoopGroup) {

        checkNotNull(serverSocketChannelClass, "Server Socket Channel Class must not be null");
        checkNotNull(parentEventLoopGroup, "Parent Event Loop Group must not be null");
        checkNotNull(childEventLoopGroup, "Child Event Loop Group must not be null");

        this.serverSocketChannelClass = serverSocketChannelClass;
        this.parentEventLoopGroup = parentEventLoopGroup;
        this.childEventLoopGroup = childEventLoopGroup;
    }

    public Class<? extends DatagramChannel> getServerSocketChannelClass() {
        return serverSocketChannelClass;
    }

    public EventLoopGroup getChildEventLoopGroup() {
        return childEventLoopGroup;
    }

    public EventLoopGroup getParentEventLoopGroup() {
        return parentEventLoopGroup;
    }
}
