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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.jetbrains.annotations.NotNull;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.util.concurrent.ThreadFactory;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractNettyConfigurationProvider {

    /**
     * Creates the Parent Eventloop. Creates either a NIO Eventloop or a native Epoll Eventloop with a preference
     * for native Epoll.
     *
     * @return the Boss EventLoopGroup
     */
    @NotNull
    protected EventLoopGroup createParentEventLoop() {
        return new NioEventLoopGroup(1, createThreadFactory("hivemq-eventloop-parent-%d"));
    }

    /**
     * Creates the Child Eventloop. Creates either a NIO Eventloop or a native Epoll Eventloop with a preference
     * for native Epoll.
     *
     * @return the Boss EventLoopGroup
     */
    @NotNull
    protected EventLoopGroup createChildEventLoop() {
        //Default Netty Threads.
        return new NioEventLoopGroup(0, createThreadFactory("hivemq-eventloop-child-%d"));
    }

    /**
     * Creates a Thread Factory that names Threads with the given format
     *
     * @param nameFormat the format
     * @return a ThreadFactory that names Threads with the given format
     */
    private ThreadFactory createThreadFactory(final @NotNull String nameFormat) {

        checkNotNull(nameFormat, "Thread Factory Name Format must not be null");
        return new ThreadFactoryBuilder().
                setNameFormat(nameFormat).
                build();
    }
}
