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
package com.hivemq.mqtt.handler.connect;

import org.jetbrains.annotations.NotNull;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.connect.CONNECT;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.NoSuchElementException;

import static com.hivemq.bootstrap.netty.ChannelHandlerNames.NEW_CONNECTION_IDLE_HANDLER;

/**
 * @author Christoph Schäbel
 * @author Silvio Giebl
 */
@Singleton
@ChannelHandler.Sharable
public class NoConnectIdleHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(NoConnectIdleHandler.class);

    private final @NotNull MqttServerDisconnector mqttServerDisconnector;

    @Inject
    public NoConnectIdleHandler(final @NotNull MqttServerDisconnector mqttServerDisconnector) {
        this.mqttServerDisconnector = mqttServerDisconnector;
    }

    @Override
    public void channelRead(final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg) {
        if (msg instanceof CONNECT) {
            try {
                ctx.pipeline().remove(NEW_CONNECTION_IDLE_HANDLER);
                ctx.pipeline().remove(this);
            } catch (final NoSuchElementException ignored) {
                //no problem, because if these handlers are not in the pipeline anyway, we still get the expected result here
                log.trace("Not able to remove no connect idle handler.");
            }
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void userEventTriggered(final @NotNull ChannelHandlerContext ctx, final @NotNull Object evt) {

        if (evt instanceof IdleStateEvent && ((IdleStateEvent) evt).state() == IdleState.READER_IDLE) {
            mqttServerDisconnector.logAndClose(ctx.channel(),
                    "Client with IP {} disconnected. The client was idle for too long without sending a MQTT CONNECT packet.",
                    "No CONNECT sent in time");
        }
        ctx.fireUserEventTriggered(evt);
    }
}
