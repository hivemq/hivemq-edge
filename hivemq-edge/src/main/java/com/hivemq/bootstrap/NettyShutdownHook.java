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
package com.hivemq.bootstrap;

import com.hivemq.common.shutdown.HiveMQShutdownHook;
import org.jetbrains.annotations.NotNull;
import com.hivemq.persistence.connection.ConnectionPersistence;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class NettyShutdownHook implements HiveMQShutdownHook {

    private static final Logger log = LoggerFactory.getLogger(NettyShutdownHook.class);

    private final @NotNull EventLoopGroup workerGroup;
    private final @NotNull EventLoopGroup bossGroup;
    private final @NotNull EventLoopGroup udpWorkerGroup;
    private final @NotNull EventLoopGroup udpBossGroup;
    private final int eventLoopsShutdownTimeout;
    private final int connectionPersistenceShutdownTimeout;
    private final @NotNull ConnectionPersistence connectionPersistence;

    public NettyShutdownHook(
            final @NotNull EventLoopGroup workerGroup,
            final @NotNull EventLoopGroup bossGroup,
            final @NotNull EventLoopGroup udpWorkerGroup,
            final @NotNull EventLoopGroup udpBossGroup,
            final int eventLoopsShutdownTimeout,
            final int connectionPersistenceShutdownTimeout,
            final @NotNull ConnectionPersistence connectionPersistence) {
        this.workerGroup = workerGroup;
        this.bossGroup = bossGroup;
        this.udpWorkerGroup = udpWorkerGroup;
        this.udpBossGroup = udpBossGroup;
        this.eventLoopsShutdownTimeout = eventLoopsShutdownTimeout;
        this.connectionPersistenceShutdownTimeout = connectionPersistenceShutdownTimeout;
        this.connectionPersistence = connectionPersistence;
    }

    @Override
    public @NotNull String name() {
        return "Netty Shutdown";
    }

    @Override
    public @NotNull Priority priority() {
        return Priority.MEDIUM;
    }

    @Override
    public void run() {
        log.debug("Shutting down listeners and clients");
        try {
            //we need to block the shutdown of the clients before we shut down their executors.
            connectionPersistence.shutDown().get(connectionPersistenceShutdownTimeout, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException | ExecutionException e) {
            log.warn("Client shutdown failed exceptionally: {}", e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("Original Exception: ", e);
            }
        } catch (final TimeoutException e) {
            log.warn("Client shutdown timed out.");
            if (log.isDebugEnabled()) {
                log.debug("Original Exception: ", e);
            }
        } finally {
            connectionPersistence.interruptShutdown();
        }

        log.debug("Shutting down worker and boss threads");
        final Future<?> workerFinished = workerGroup.shutdownGracefully(100, eventLoopsShutdownTimeout, TimeUnit.MILLISECONDS); //TimeUnit effects both parameters!
        final Future<?> bossFinished = bossGroup.shutdownGracefully(100, eventLoopsShutdownTimeout, TimeUnit.MILLISECONDS);
        final Future<?> udpWorkerFinished = udpWorkerGroup.shutdownGracefully(100, eventLoopsShutdownTimeout, TimeUnit.MILLISECONDS);
        final Future<?> udpBossFinished = udpBossGroup.shutdownGracefully(100, eventLoopsShutdownTimeout, TimeUnit.MILLISECONDS);

        log.trace("Waiting for Worker threads to finish");
        workerFinished.syncUninterruptibly();
        log.trace("Waiting for Boss threads to finish");
        bossFinished.syncUninterruptibly();
        log.trace("Waiting for UDP Worker threads to finish");
        udpWorkerFinished.syncUninterruptibly();
        log.trace("Waiting for UDP Boss threads to finish");
        udpBossFinished.syncUninterruptibly();
    }
}
