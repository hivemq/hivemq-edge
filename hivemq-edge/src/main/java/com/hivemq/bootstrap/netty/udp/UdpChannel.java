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

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.RecyclableArrayList;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Shevchik (https://github.com/Shevchik/UdpServerSocketChannel)
 */
public class UdpChannel extends AbstractChannel {

    protected final ChannelMetadata metadata = new ChannelMetadata(false);
    protected final DefaultChannelConfig config = new DefaultChannelConfig(this);

    protected final UdpServerChannel channel;
    protected final InetSocketAddress remote;
    protected final ConcurrentLinkedQueue<ByteBuf>
            buffers = new ConcurrentLinkedQueue<>();
    protected boolean reading = false;
    protected volatile boolean open = true;

    protected UdpChannel(UdpServerChannel channel, InetSocketAddress remote) {
        super(channel);
        this.channel = channel;
        this.remote = remote;
    }

    protected AtomicBoolean isNew = new AtomicBoolean(true);

    protected boolean getIsNew() {
        return isNew.compareAndSet(true, false);
    }

    @Override
    public ChannelMetadata metadata() {
        return metadata;
    }

    @Override
    public ChannelConfig config() {
        return config;
    }

    @Override
    public boolean isActive() {
        return open;
    }

    @Override
    public boolean isOpen() {
        return isActive();
    }

    @Override
    protected void doClose() {
        open = false;
        channel.doUserChannelRemove(this);
    }

    @Override
    protected void doDisconnect() throws Exception {
        doClose();
    }

    @Override
    protected void doBeginRead() throws Exception {
        if (reading) {
            return;
        }
        reading = true;
        try {
            ByteBuf buffer = null;
            while ((buffer = buffers.poll()) != null) {
                pipeline().fireChannelRead(buffer);
            }
            pipeline().fireChannelReadComplete();
        } finally {
            reading = false;
        }
    }

    @Override
    protected void doWrite(ChannelOutboundBuffer buffer) throws Exception {
        final RecyclableArrayList list = RecyclableArrayList.newInstance();
        boolean freeList = true;
        try {
            ByteBuf buf = null;
            while ((buf = (ByteBuf) buffer.current()) != null) {
                list.add(buf.retain());
                buffer.remove();
            }
            freeList = false;
        } finally {
            if (freeList) {
                for (Object obj : list) {
                    ReferenceCountUtil.safeRelease(obj);
                }
                list.recycle();
            }
        }
        channel.doWrite(list, remote);
    }

    @Override
    protected boolean isCompatible(EventLoop eventloop) {
        return true;
    }

    @Override
    protected AbstractUnsafe newUnsafe() {
        return new AbstractUnsafe() {
            @Override
            public void connect(SocketAddress addr1, SocketAddress addr2, ChannelPromise pr) {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    protected SocketAddress localAddress0() {
        return channel.localAddress0();
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return remote;
    }

    @Override
    protected void doBind(SocketAddress addr) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "UdpChannel{" + System.identityHashCode(this) +
                ", remote=" + remote +
                '}';
    }
}
