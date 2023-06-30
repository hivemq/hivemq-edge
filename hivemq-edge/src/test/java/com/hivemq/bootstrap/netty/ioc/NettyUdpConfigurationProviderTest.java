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

import com.hivemq.bootstrap.netty.NettyUdpConfiguration;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class NettyUdpConfigurationProviderTest {

    private NettyUdpConfiguration nettyConfiguration;

    @Before
    public void setUp() throws Exception {

        final NettyUdpConfigurationProvider provider = new NettyUdpConfigurationProvider();
        nettyConfiguration = provider.get();
    }

    @After
    public void tearDown() throws Exception {
        nettyConfiguration.getChildEventLoopGroup().shutdownGracefully();
    }

    @Test
    public void test_nio_is_used() {

        assertThat(nettyConfiguration.getChildEventLoopGroup(), instanceOf(NioEventLoopGroup.class));

        assertEquals(NioDatagramChannel.class, nettyConfiguration.getServerSocketChannelClass());
    }

    @Test
    public void test_thread_names_for_nio_are_set() throws Exception {

        final String childThreadName = nettyConfiguration.getChildEventLoopGroup().submit(() -> Thread.currentThread().getName()).get();
        assertTrue(childThreadName.startsWith("hivemq-eventloop-child-"));

    }
}
