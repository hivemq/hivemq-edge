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
package com.hivemq.security.ssl;

import com.hivemq.bootstrap.ClientConnection;
import org.jetbrains.annotations.NotNull;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.NotSslRecordException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import static com.hivemq.logging.LoggingUtils.appendListenerToMessage;

/**
 * This Exception handler is responsible for handling SSLExceptions and all other
 * SSL related exceptions.
 * <p>
 * SSLExceptions are fatal most of the time (for a client which wants to connect with SSL :-) ),
 * so we typically can only log.
 */
public class SslExceptionHandler extends ChannelHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(SslExceptionHandler.class);
    private final @NotNull MqttServerDisconnector mqttServerDisconnector;

    @Inject
    public SslExceptionHandler(final @NotNull MqttServerDisconnector mqttServerDisconnector) {
        this.mqttServerDisconnector = mqttServerDisconnector;
    }

    @Override
    public void exceptionCaught(final @NotNull ChannelHandlerContext ctx, final @NotNull Throwable cause) {

        if (ignorableException(cause, ctx)) {
            return;
        }

        //SslHandshakeExceptions are wrapped so we check the cause instead
        if (cause.getCause() != null) {
            if (cause.getCause() instanceof SSLHandshakeException) {
                logSSLHandshakeException(ctx, cause);
                //Just in case the channel wasn't closed already
                final String eventLogMessage = appendListenerToMessage(ctx.channel(), "SSL handshake failed");
                mqttServerDisconnector.logAndClose(ctx.channel(),
                        null, //already logged
                        eventLogMessage);
                return;

            } else if (cause.getCause() instanceof SSLException) {
                logSSLException(ctx, cause);
                final String eventLogMessage = appendListenerToMessage(ctx.channel(), "SSL message transmission failed");
                mqttServerDisconnector.logAndClose(ctx.channel(),
                        null, //already logged
                        eventLogMessage);
                return;
            }
        }

        //Rethrow Exception, we can only handle SSL Exceptions
        ctx.fireExceptionCaught(cause);
    }


    private void logSSLException(final @NotNull ChannelHandlerContext ctx, final @NotNull Throwable cause) {
        if (log.isDebugEnabled()) {

            final Throwable rootCause = ExceptionUtils.getRootCause(cause);

            final ClientConnection clientConnection = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
            final String clientId = clientConnection.getClientId();
            if (clientId != null) {
                log.debug("SSL message transmission for client {} failed: {}", clientId, rootCause.getMessage());
            } else {
                log.debug("SSL message transmission failed for client with IP {}: {}", clientConnection.getChannelIP().orElse("UNKNOWN"), rootCause.getMessage());
            }
            log.trace("Original Exception", rootCause);
        }
    }

    private void logSSLHandshakeException(final @NotNull ChannelHandlerContext ctx, final @NotNull Throwable cause) {
        if (log.isDebugEnabled()) {

            final Throwable rootCause = ExceptionUtils.getRootCause(cause);

            final ClientConnection clientConnection = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
            final String clientId = clientConnection.getClientId();
            if (clientId != null) {
                log.debug("SSL Handshake for client {} failed: {}", clientId, rootCause.getMessage());
            } else {
                log.debug("SSL Handshake failed for client with IP {}: {}", clientConnection.getChannelIP().orElse("UNKNOWN"), rootCause.getMessage());
            }
            log.trace("Original Exception", rootCause);
        }
    }


    private boolean ignorableException(final @NotNull Throwable cause, final @NotNull ChannelHandlerContext ctx) {

        if (cause instanceof NotSslRecordException) {
            if (log.isDebugEnabled()) {
                final ClientConnection clientConnection = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
                log.debug("Client {} sent data which is not SSL/TLS to a SSL/TLS listener. Disconnecting client.", clientConnection.getChannelIP().orElse("UNKNOWN"));
                log.trace("Original Exception:", cause);
            }
            //Just in case the client wasn't disconnected already
            final String eventLogMessage = appendListenerToMessage(ctx.channel(), "SSL handshake failed");
            mqttServerDisconnector.logAndClose(ctx.channel(),
                    null, //already logged
                    eventLogMessage);
            return true;
        }
        return false;
    }
}
