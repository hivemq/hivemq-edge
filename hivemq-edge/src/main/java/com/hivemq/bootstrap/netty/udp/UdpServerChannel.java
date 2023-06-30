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
package com.hivemq.bootstrap.netty.udp;

import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.internal.RecyclableArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author Shevchik (https://github.com/Shevchik/UdpServerSocketChannel)
 * @license GNU LGPLv3 - need to confirm this
 * @modified Simon L Johnson - removed EPoll support
 */
public class UdpServerChannel extends AbstractServerChannel {

    private static final Logger log = LoggerFactory.getLogger(UdpServerChannel.class);

    protected final EventLoopGroup group;
    protected final List<Bootstrap> ioBootstraps = new ArrayList<>();
    protected final List<Channel> ioChannels = new ArrayList<>();
    protected final ConcurrentHashMap<InetSocketAddress, UdpChannel>
            userChannels = new ConcurrentHashMap<>();

    protected volatile boolean open = true;

    public UdpServerChannel(final @NotNull EventLoopGroup group) {
        this.group = group;
        Class<? extends DatagramChannel> channel = NioDatagramChannel.class;
        ChannelInitializer<Channel> initializer = new ChannelInitializer<>() {
            final ReadRouteChannelHandler ioReadRoute = new ReadRouteChannelHandler();
            @Override
            protected void initChannel(Channel ioChannel) {
                ioChannel.pipeline().addLast(ioReadRoute);
            }
        };

        Bootstrap ioBootstrap = new Bootstrap().group(group).
                channel(channel).handler(initializer);
        ioBootstraps.add(ioBootstrap);
    }

    protected class ReadRouteChannelHandler extends SimpleChannelInboundHandler<DatagramPacket> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket p) throws Exception {
            UdpChannel channel = userChannels.compute(p.sender(), (lAddr, lChannel) -> ((lChannel == null) || !lChannel.isOpen()) ?
                    new UdpChannel(UdpServerChannel.this, lAddr) : lChannel);
            boolean newChannel = channel.getIsNew();
            if(log.isTraceEnabled()) {
                log.trace("determined {} route for packet {} -> {}", (newChannel ? "NEW" : "EXISTING"), p, channel);
            }
            channel.buffers.add(p.content().retain());
            if (newChannel) {
                ChannelPipeline serverPipeline = UdpServerChannel.this.pipeline();
                serverPipeline.fireChannelRead(channel);
                serverPipeline.fireChannelReadComplete();
            } else {
                if (channel.isRegistered()) {
                    channel.read();
                }
            }
        }
    }

    protected void doWrite(RecyclableArrayList list, InetSocketAddress remote) {
        Channel ioChannel = ioChannels.get(remote.hashCode() & (ioChannels.size() - 1));
        ioChannel.eventLoop().execute(() -> {
            try {
                for (Object buf : list) {
                    ioChannel.write(new DatagramPacket((ByteBuf) buf, remote));
                }
                ioChannel.flush();
            } finally {
                list.recycle();
            }
        });
    }


    protected void doUserChannelRemove(UdpChannel userChannel) {
        this.
        userChannels.compute((InetSocketAddress) userChannel.remoteAddress(), (lAddr, lChannel) -> lChannel == userChannel ? null : lChannel);
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public boolean isActive() {
        return isOpen();
    }

    @Override
    protected void doClose() {
        open = false;
        new ArrayList<>(userChannels.values()).forEach(Channel::close);
        ioChannels.forEach(Channel::close);
        try {
            group.shutdownGracefully(100,
                    InternalConfigurations.EVENT_LOOP_GROUP_SHUTDOWN_TIMEOUT_MILLISEC,
                    TimeUnit.MILLISECONDS).sync();
        } catch (InterruptedException e) {
            log.warn("UDP listener shutdown interrupted", e);
        }
    }

    @Override
    protected SocketAddress localAddress0() {
        return ioChannels.size() > 0 ? ioChannels.get(0).localAddress() : null;
    }

    @Override
    public InetSocketAddress localAddress() {
        return ioChannels.size() > 0 ? (InetSocketAddress) ioChannels.get(0).localAddress() : null;
    }

    @Override
    protected void doBind(SocketAddress local) throws Exception {
        for (Bootstrap bootstrap : ioBootstraps) {
            ioChannels.add(bootstrap.bind(local).sync().channel());
        }
        ioBootstraps.clear();
    }

    protected final DefaultChannelConfig config = new DefaultChannelConfig(this) {

        {
            setRecvByteBufAllocator(new FixedRecvByteBufAllocator(2048));
        }

        @Override
        public boolean isAutoRead() {
            return true;
        }

        @Override
        public ChannelConfig setAutoRead(boolean autoRead) {
            return this;
        }

    };

    @Override
    public DefaultChannelConfig config() {
        return config;
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return null;
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return true;
    }

    @Override
    protected void doBeginRead() throws Exception {

    }
}
