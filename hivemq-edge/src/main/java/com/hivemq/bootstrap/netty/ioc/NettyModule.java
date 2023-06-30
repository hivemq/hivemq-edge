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

import com.hivemq.bootstrap.netty.ChannelInitializerFactory;
import com.hivemq.bootstrap.netty.ChannelInitializerFactoryImpl;
import com.hivemq.bootstrap.netty.NettyTcpConfiguration;
import com.hivemq.bootstrap.netty.NettyUdpConfiguration;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import javax.inject.Singleton;

@Module
public abstract class NettyModule {

    @Provides
    @Singleton
    static @NotNull ChannelGroup channelGroup() {
        return new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    }

    @Provides
    @Singleton
    static @NotNull NettyTcpConfiguration nettyTcpConfiguration(NettyTcpConfigurationProvider nettyTcpConfigurationProvider) {
        return nettyTcpConfigurationProvider.get();
    }

    @Provides
    @Singleton
    static @NotNull NettyUdpConfiguration nettyUdpConfiguration(NettyUdpConfigurationProvider nettyUdpConfigurationProvider) {
        return nettyUdpConfigurationProvider.get();
    }

    @Binds
    public abstract ChannelInitializerFactory channelInitializerFactory(ChannelInitializerFactoryImpl channelInitializerFactory);

}
