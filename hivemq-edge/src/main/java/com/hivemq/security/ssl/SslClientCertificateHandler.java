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
import com.hivemq.bootstrap.netty.ChannelHandlerNames;
import com.hivemq.configuration.service.entity.Tls;
import org.jetbrains.annotations.NotNull;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.security.auth.SslClientCertificate;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.security.cert.Certificate;

import static com.hivemq.logging.LoggingUtils.appendListenerToMessage;

/**
 * @author Christoph Schäbel
 */
public class SslClientCertificateHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(SslClientCertificateHandler.class);

    private final @NotNull Tls tls;
    private final @NotNull MqttServerDisconnector mqttServerDisconnector;

    public SslClientCertificateHandler(final @NotNull Tls tls, final @NotNull MqttServerDisconnector mqttServerDisconnector) {
        this.tls = tls;
        this.mqttServerDisconnector = mqttServerDisconnector;
    }

    @Override
    public void userEventTriggered(final @NotNull ChannelHandlerContext ctx, final @NotNull Object evt) throws Exception {

        if (!(evt instanceof SslHandshakeCompletionEvent)) {
            super.userEventTriggered(ctx, evt);
            return;
        }

        final SslHandshakeCompletionEvent sslHandshakeCompletionEvent = (SslHandshakeCompletionEvent) evt;

        if (!sslHandshakeCompletionEvent.isSuccess()) {
            log.trace("Handshake failed", sslHandshakeCompletionEvent.cause());
            return;
        }

        final Channel channel = ctx.channel();

        try {
            final SslHandler sslHandler = (SslHandler) channel.pipeline().get(ChannelHandlerNames.SSL_HANDLER);

            final SSLSession session = sslHandler.engine().getSession();
            final Certificate[] peerCertificates = session.getPeerCertificates();
            final SslClientCertificate sslClientCertificate = new SslClientCertificateImpl(peerCertificates);
            channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setAuthCertificate(sslClientCertificate);

        } catch (final SSLPeerUnverifiedException e) {
            handleSslPeerUnverifiedException(channel, e);

        } catch (final ClassCastException e2) {
            final String eventLogMessage = appendListenerToMessage(channel, "SSL handshake failed");
            mqttServerDisconnector.logAndClose(channel,
                    null, //no logging needed as we rethrow it as a RuntimeException
                    eventLogMessage);
            throw new RuntimeException("Not able to get SslHandler from pipeline", e2);
        }

        channel.pipeline().remove(this);

    }

    private void handleSslPeerUnverifiedException(final @NotNull Channel channel, final SSLPeerUnverifiedException e) {

        // "peer not authenticated" is set by sun.security.ssl.SSLSessionImpl.getPeerCertificates()
        // if the certificate is null
        if ("peer not authenticated".equals(e.getMessage()) || "peer not verified".equals(e.getMessage())) {

            if (Tls.ClientAuthMode.REQUIRED.equals(tls.getClientAuthMode())) {

                //We should never go here when the client authentication is required but the client cert was never sent
                //because the SslHandler of netty checks this for us
                final String eventLogMessage = appendListenerToMessage(channel, "No client certificate provided");
                log.error("Client certificate authentication forced but no client certificate was provided. " +
                        "Disconnecting.", e);
                mqttServerDisconnector.logAndClose(channel,
                        null, //already logged
                        eventLogMessage);

            } else if (Tls.ClientAuthMode.OPTIONAL.equals(tls.getClientAuthMode())) {

                log.debug("Client did not provide SSL certificate for authentication. " +
                        "Could not authenticate at application level");
            }

        } else {
            log.error("An error occurred. Disconnecting client.", e);
            final String eventLogMessage = appendListenerToMessage(channel, "SSL handshake failed");
            mqttServerDisconnector.logAndClose(channel,
                    null, //already logged
                    eventLogMessage);
        }
    }

}
