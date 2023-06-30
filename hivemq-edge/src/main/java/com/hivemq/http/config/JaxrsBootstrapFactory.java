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
package com.hivemq.http.config;

import com.hivemq.api.config.ApiListener;
import com.hivemq.api.config.HttpListener;
import com.hivemq.api.config.HttpsListener;
import com.hivemq.configuration.service.ApiConfigurationService;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.security.ssl.SslUtil;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import java.util.concurrent.Executors;

/**
 * @author Simon L Johnson
 */
public class JaxrsBootstrapFactory {

    private static final Logger log = LoggerFactory.getLogger(JaxrsBootstrapFactory.class);

    public static @NotNull JaxrsHttpServerConfiguration createJaxrsConfiguration(
            final @NotNull ApiConfigurationService apiConfigurationService, final @NotNull ApiListener listener) {

        JaxrsHttpServerConfiguration jaxrsConfig = new JaxrsHttpServerConfiguration();
        jaxrsConfig.setHost(listener.getBindAddress());
        jaxrsConfig.setPort(listener.getPort());
        final int httpThreadPoolSize = InternalConfigurations.HTTP_API_THREAD_COUNT.get();
        jaxrsConfig.setHttpThreadPoolSize(httpThreadPoolSize);
        jaxrsConfig.setHttpThreadPoolExecutor(Executors.newFixedThreadPool(Math.max(1, httpThreadPoolSize)));
        jaxrsConfig.setHttpThreadPoolShutdownTimeoutSeconds(InternalConfigurations.HTTP_API_SHUTDOWN_TIME_SECONDS.get());


        if (listener instanceof HttpListener) {
            jaxrsConfig.setProtocol(JaxrsHttpServerConfiguration.HTTP_PROTOCOL);
        } else if (listener instanceof HttpsListener) {
            jaxrsConfig.setProtocol(JaxrsHttpServerConfiguration.HTTPS_PROTOCOL);
            final HttpsListener httpsListener = (HttpsListener) listener;

            final KeyManagerFactory keyManagerFactory = SslUtil.createKeyManagerFactory("JKS",
                    httpsListener.getKeystorePath(),
                    httpsListener.getKeystorePassword(),
                    httpsListener.getPrivateKeyPassword());

            try {
                final SSLContext context = ((JdkSslContext) SslContextBuilder.forServer(keyManagerFactory)
                        .sslProvider(SslProvider.JDK)
                        .ciphers(httpsListener.getCipherSuites(), SupportedCipherSuiteFilter.INSTANCE)
                        .build()).context();

                jaxrsConfig.setHttpsConfigurator(new HttpsConfigurator(context) {
                    @Override
                    public void configure(final @NotNull HttpsParameters params) {
                        SSLParameters parameters = getSSLContext().getDefaultSSLParameters();
                        parameters.setProtocols(httpsListener.getProtocols().toArray(new String[0]));
                        parameters.setCipherSuites(httpsListener.getCipherSuites().toArray(new String[0]));
                        params.setSSLParameters(parameters);
                    }
                });
            } catch (SSLException e) {
                log.error("Not able to create HTTPS listener", e);
                throw new UnrecoverableException(false);
            }
        }

        //Static Resource Mouth Points
        apiConfigurationService.getResourcePaths()
                .forEach(s -> jaxrsConfig.addStaticResource(Pair.of(s.getUri(), s.getPath())));
        return jaxrsConfig;
    }
}
