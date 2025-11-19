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

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class NettyTcpConfigurationTest {

    private NioEventLoopGroup eventloop;
    @BeforeEach
    public void setUp() throws Exception {
        eventloop = new NioEventLoopGroup(1);
    }
    @AfterEach
    public void tearDown() throws Exception {
        if (eventloop != null) {
            eventloop.shutdownGracefully();
        }
    }

    @Test
    public void test_server_socket_class_npe() {
    
        assertThrows(NullPointerException.class, () -> new NettyTcpConfiguration(null, eventloop, eventloop));
    }

    @Test
    public void test_parent_eventloop_npe() {
    
        assertThrows(NullPointerException.class, () -> new NettyTcpConfiguration(NioServerSocketChannel.class, null, eventloop));
    }

    @Test
    public void test_child_eventloop_npe() {
    
        assertThrows(NullPointerException.class, () -> new NettyTcpConfiguration(NioServerSocketChannel.class, eventloop, null));
    }
}
