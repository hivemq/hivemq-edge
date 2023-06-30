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
package com.hivemq.mqttsn.services;

import com.google.common.base.Preconditions;
import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.MqttsnConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slj.mqtt.sn.codec.MqttsnCodecs;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 *
 */
@Singleton
public class GatewayBroadcastServiceImpl implements IGatewayBroadcastService {

    private static final Logger logger = LoggerFactory.getLogger(GatewayBroadcastServiceImpl.class);

    private final @NotNull ScheduledExecutorService scheduledExecutorService
            = Executors.newScheduledThreadPool(1);
    final @NotNull MqttsnConfigurationService mqttsnConfigurationService;
    private volatile ScheduledFuture task;
    private volatile Object mutex = new Object();

    @Inject
    public GatewayBroadcastServiceImpl(final @NotNull MqttsnConfigurationService mqttsnConfigurationService,
                                       final @NotNull ShutdownHooks shutdownHooks) {

        this.mqttsnConfigurationService = mqttsnConfigurationService;
        shutdownHooks.add(new HiveMQShutdownHook() {
            @Override
            public @NotNull String name() {
                return "ScheduledGatewayBroadcastService shutdown";
            }

            @Override
            public void run() {
                scheduledExecutorService.shutdown();
            }
        });
    }

    public void startBroadcast(final @NotNull Channel channel) {
        Preconditions.checkNotNull(channel);
        Preconditions.checkState(mqttsnConfigurationService.isDiscoveryEnabled(), "Discovery is not enabled");
        synchronized (mutex) {
            Preconditions.checkState(task == null, "Broadcast already active");
            try {
                int seconds = mqttsnConfigurationService.getDiscoveryBroadcastIntervalSeconds();
                logger.trace("Scheduling MQTT-SN broadcast task to every {}s, starting in {}s", seconds, seconds * 2);
                task = scheduledExecutorService.scheduleAtFixedRate(new BroadcastTask(channel),
                        seconds * 2L,
                        seconds,
                        TimeUnit.SECONDS);
            } catch (final RejectedExecutionException rejectedExecutionException) {
                // can be ignored, can happen during Shutdown
            }
        }
    }

    public void stopBroadcast() {
        synchronized (mutex) {
            try {
                if(task != null){
                    task.cancel(true);
                }
            } finally {
                task = null;
            }
        }
    }

    @Override
    public boolean needsStarting() {
        return task == null && mqttsnConfigurationService.isDiscoveryEnabled();
    }

    public class BroadcastTask implements Runnable {

        final @NotNull Channel channel;

        public BroadcastTask(final @NotNull Channel channel) {
            this.channel = channel;
        }

        @Override
        public void run() {
            try {
                logger.trace("Firing an MQTT-SN broadcast packet to {} address(es)",
                        mqttsnConfigurationService.getDiscoveryBroadcastAddresses().size());

                ChannelFuture future = channel.writeAndFlush(
                        MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2.createMessageFactory().createAdvertise(
                                mqttsnConfigurationService.getGatewayId(),
                                mqttsnConfigurationService.getDiscoveryBroadcastIntervalSeconds()));

            } catch(Exception e){
                logger.warn("An error occurred firing MQTT-SN Broadcast packet", e);
            }
        }
    }

    public static List<InetAddress> getBroadcastAddresses(boolean ignoreLoopBack)
            throws SocketException {
        return NetworkInterface.networkInterfaces()
                .filter(n -> !ignoreLoopBack || notLoopBack(n))
                .map(networkInterface ->
                        networkInterface.getInterfaceAddresses()
                                .stream()
                                .map(InterfaceAddress::getBroadcast)
                                .filter(Objects::nonNull)
                                .findFirst()
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private static boolean notLoopBack(NetworkInterface networkInterface) {
        try {
            return !networkInterface.isLoopback();
        } catch (SocketException e) {
            // should not happen, but if it does: throw RuntimeException
            throw new RuntimeException(e);
        }
    }
}
