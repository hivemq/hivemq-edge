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
package com.hivemq.bootstrap.netty.ioc;

import com.hivemq.bootstrap.netty.NettyTcpConfiguration;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * This Provider creates the configuration for Netty.
 *
 * @author Dominik Obermaier
 */
@Singleton
public class NettyTcpConfigurationProvider extends AbstractNettyConfigurationProvider
        implements Provider<NettyTcpConfiguration> {

    @Inject
    public NettyTcpConfigurationProvider() {
    }

    @NotNull
    @Override
    public NettyTcpConfiguration get() {

        final EventLoopGroup parentGroup = createParentEventLoop();
        final EventLoopGroup childGroup = createChildEventLoop();

        return new NettyTcpConfiguration(NioServerSocketChannel.class, parentGroup, childGroup);
    }
}
