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
package com.hivemq.mqtt.handler.publish;

import com.codahale.metrics.Counter;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.publish.PublishWithFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Krüger
 */
public class PublishFlushHandler extends ChannelInboundHandlerAdapter implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(PublishFlushHandler.class);

    private @Nullable ChannelHandlerContext ctx;
    private final @NotNull Deque<PublishWithFuture> messagesToWrite = new ArrayDeque<>();
    private final @NotNull Counter channelNotWritable;
    private final int maxWritesBeforeFlush;
    private boolean wasWritable = true; // will only ever be updated in the channel's eventloop

    public PublishFlushHandler(final @NotNull MetricsHolder metricsHolder) {
        channelNotWritable = metricsHolder.getChannelNotWritableCounter();
        maxWritesBeforeFlush = InternalConfigurations.COUNT_OF_PUBLISHES_WRITTEN_TO_CHANNEL_TO_TRIGGER_FLUSH.get();
    }

    @Override
    public void handlerAdded(final @NotNull ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void channelWritabilityChanged(final @NotNull ChannelHandlerContext ctx) {
        final Channel channel = ctx.channel();
        if (channel.isWritable() && !wasWritable) {
            wasWritable = true;
            channelNotWritable.dec();

            channel.eventLoop().execute(this);
        }
        ctx.fireChannelWritabilityChanged();
    }

    @Override
    public void channelInactive(final @NotNull ChannelHandlerContext ctx) throws Exception {
        handleChannelInactiveState();
        super.channelInactive(ctx);
    }

    private void handleChannelInactiveState() {
        while (!messagesToWrite.isEmpty()) {
            messagesToWrite.poll().getFuture().set(PublishStatus.NOT_CONNECTED);
        }
    }

    public void sendPublishes(final @NotNull List<PublishWithFuture> publishes) {
        if (ctx == null) {
            // Handler not yet added to pipeline, mark all as not connected
            for (final PublishWithFuture publish : publishes) {
                publish.getFuture().set(PublishStatus.NOT_CONNECTED);
            }
            return;
        }
        final ChannelHandlerContext localCtx = ctx;
        try{
            localCtx.channel().eventLoop().execute(() -> {
                messagesToWrite.addAll(publishes);
                if (localCtx.channel().isActive()) {
                    consumeQueue();
                } else {
                    handleChannelInactiveState();
                }
            });
        } catch (final RejectedExecutionException e) {
            log.warn("Failed to schedule publish flush task for channel {}. Marking publishes as not connected.", localCtx.channel(), e);
            for (final PublishWithFuture publish : publishes) {
                publish.getFuture().set(PublishStatus.NOT_CONNECTED);
            }
        }
    }

    @Override
    public void run() {
        consumeQueue();
    }

    private void consumeQueue() {
        final ChannelHandlerContext localCtx = ctx;
        if (localCtx == null) {
            // Handler not yet added to pipeline
            handleChannelInactiveState();
            return;
        }
        int written = 0;
        while (!messagesToWrite.isEmpty()) {

            if (!localCtx.channel().isWritable()) {
                if (wasWritable) {
                    wasWritable = false;
                    channelNotWritable.inc();
                }
                break;
            }

            final PublishWithFuture publish = messagesToWrite.poll();

            localCtx.write(publish).addListener(new PublishWriteFailedListener(publish.getFuture()));
            written++;
            if (written >= maxWritesBeforeFlush) {
                localCtx.flush();
                written = 0;
            }
        }
        if (written > 0) {
            localCtx.flush();
        }
    }
}
