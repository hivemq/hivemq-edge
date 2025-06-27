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
package com.hivemq.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.InternalConfigurations;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.hivemq.http.config.JaxrsHttpServerConfiguration;
import com.hivemq.http.core.IHttpRequestResponseHandler;
import com.hivemq.http.error.DefaultExceptionMapper;
import com.hivemq.http.filters.CorsFilter;
import com.hivemq.http.handlers.AlternativeClassloadingStaticFileHandler;
import com.hivemq.http.handlers.StaticFileHandler;
import com.hivemq.http.handlers.WebAppHandler;
import com.hivemq.http.sun.SunHttpHandlerProxy;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import org.apache.commons.lang3.tuple.Pair;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.ExceptionMapper;
import java.net.BindException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Super simple light-weight jax-rs implementation that backs onto the sun HttpServer
 *
 * @author Simon L Johnson
 */
public class JaxrsHttpServer {

    public static final int MAX_BINDING_PRIORITY = 1;

    static final @NotNull String MAX_REQ_TIME = "sun.net.httpserver.maxReqTime";
    static final @NotNull String MAX_RESP_TIME = "sun.net.httpserver.maxRspTime";
    static final @NotNull String LOCATION_RELATIVE_RESOLUTION_DISABLED =
            "jersey.config.server.headers.location.relative.resolution.disabled";
    private static final @NotNull Logger logger = LoggerFactory.getLogger(JaxrsHttpServer.class);
    private final @NotNull List<HttpServer> httpServers = new ArrayList<>();
    private final @NotNull ShutdownHooks shutdownHooks;
    private final @NotNull List<JaxrsHttpServerConfiguration> configs;
    private final @Nullable ResourceConfig resourceConfig;
    private @Nullable JaxrsObjectMapperProvider objectMapperProvider;
    private final @NotNull Object mutex = new Object();
    private volatile boolean running = false;

    public JaxrsHttpServer(
            final @NotNull ShutdownHooks shutdownHooks,
            final @NotNull List<JaxrsHttpServerConfiguration> configs,
            final @Nullable ResourceConfig resourceConfig) {
        this.shutdownHooks = shutdownHooks;
        this.configs = configs;
        this.resourceConfig = resourceConfig;
        shutdownHooks.add(new Shutdown());
    }

    public void startServer() {
        if (!running && httpServers.isEmpty()) {
            synchronized (mutex) {
                if (!running && httpServers.isEmpty()) {

                    final long start = System.currentTimeMillis();

                    //timeout connections, default: no timeout
                    System.setProperty(MAX_REQ_TIME, "45");
                    System.setProperty(MAX_RESP_TIME, "45");

                    ResourceConfig resources = resourceConfig == null ? new ResourceConfig() : resourceConfig;
                    // https://github.com/eclipse-ee4j/jersey/issues/2986
                    // This server property tells jersey not to resolve relative location
                    // so that the Location of the response headers is /path/ instead of http://host:port/path/.
                    resources.property(LOCATION_RELATIVE_RESOLUTION_DISABLED, true);

                    for (JaxrsHttpServerConfiguration config : configs) {

                        final long startClasses = System.currentTimeMillis();
                        if (config.getResourceClasses() != null) {
                            resources.registerClasses(config.getResourceClasses());
                        }
                        logger.trace("Registered classes in {}ms", System.currentTimeMillis() - startClasses);
                        final long startResources = System.currentTimeMillis();
                        bootstrapResources(config, resources);
                        logger.trace("Registered resources in {}ms", System.currentTimeMillis() - startResources);

                        logger.debug("Starting WebServer with protocol '{}' on port {}",
                                config.getProtocol(),
                                config.getPort());
                        URI baseUri =
                                UriBuilder.fromUri(String.format("%s://%s/", config.getProtocol(), config.getHost()))
                                        .port(config.getPort())
                                        .build();

                        boolean isSecure = JaxrsHttpServerConfiguration.HTTPS_PROTOCOL.equals(config.getProtocol());

                        final HttpServer httpServer;

                        final long startCreate = System.currentTimeMillis();
                        //-- Create the server instance, setting the executor service if supplied
                        if (isSecure) {
                            Preconditions.checkNotNull(config.getSslContext(),
                                    "When configured with SSL, context must be supplied");
                            try {
                                httpServer = JdkHttpServerFactory.createHttpServer(baseUri,
                                        resources,
                                        config.getSslContext(),
                                        false);
                                if (config.getHttpsConfigurator() != null) {
                                    logger.debug("WebServer configured with SSL [{}] configuration..",
                                            config.getHttpsConfigurator().getSSLContext().getProtocol());
                                    ((HttpsServer) httpServer).setHttpsConfigurator(config.getHttpsConfigurator());
                                }
                            } catch (ProcessingException processingException) {
                                if (processingException.getCause() instanceof BindException) {
                                    logger.error(
                                            "Unable to start the Http Server for uri '{}'. The port '{}' is already in use.",
                                            baseUri,
                                            config.getPort());
                                } else {
                                    logger.error(
                                            "Unable to start the Http Server for uri '{}'. Is another service using the port '{}'?",
                                            baseUri,
                                            config.getPort());
                                }
                                // still throw the exception to mitigate a silent fail
                                throw processingException;
                            }
                        } else {
                            logger.trace("Creating HTTP service {} with {}", baseUri, resources);
                            try {
                                httpServer = JdkHttpServerFactory.createHttpServer(baseUri, resources, false);
                            } catch (ProcessingException processingException) {
                                if (processingException.getCause() instanceof BindException) {
                                    logger.error(
                                            "Unable to start the Https Server for uri '{}'. The port '{}' is already in use.",
                                            baseUri,
                                            config.getPort());
                                } else {
                                    logger.error(
                                            "Unable to start the Https Server for uri '{}'. Is another service using the port '{}'?",
                                            baseUri,
                                            config.getPort());
                                }
                                // still throw the exception to mitigate a silent fail
                                throw processingException;
                            }
                        }
                        logger.trace("Created API server in {}ms", (System.currentTimeMillis() - startCreate));
                        final long startRegister = System.currentTimeMillis();
                        Executor executorService = config.getHttpThreadPoolExecutor();
                        if (executorService != null) {
                            httpServer.setExecutor(executorService);
                        }

                        //-- If a static resource path has been supplied in config, ensure we mount it
                        registerStaticRoot(config, httpServer);

                        registerContext("/app",
                                new WebAppHandler(objectMapperProvider.getMapper(), "httpd"),
                                httpServer);
                        registerContext("/images",
                                new StaticFileHandler(objectMapperProvider.getMapper(), "httpd/images"),
                                httpServer);
                        registerContext("/module/images",
                                new AlternativeClassloadingStaticFileHandler(objectMapperProvider.getMapper(),
                                        "httpd/images",
                                        shutdownHooks),
                                httpServer);


                        httpServers.add(httpServer);
                        logger.trace("Registered API server in {}ms", (System.currentTimeMillis() - startRegister));
                        httpServer.start();
                        logger.info("Started WebServer with protocol '{}' on port {} in {}ms",
                                config.getProtocol(),
                                config.getPort(),
                                (System.currentTimeMillis() - start));
                    }
                    running = true;
                }
            }
        }
    }

    protected void bootstrapResources(
            final @NotNull JaxrsHttpServerConfiguration config, final @NotNull ResourceConfig resources) {

        //-- Provide an Object Mapper either from config (If supplied) or one with reasonable defaults
        objectMapperProvider = createObjectMapperProvider(config);
        resources.register(objectMapperProvider, MAX_BINDING_PRIORITY);

        //-- Add the Custom Filter which optionally adds request debug
//        resources.register(new JaxrsRequestFilter());
        resources.register(new JaxrsResponseFilter());
        resources.register(new CorsFilter());

        //-- Register the injectors via a Bridge (Jersey uses HK2 so need to bridge to Guice)
//        resources.register(new JaxrsInjectorBridge(injector));

        //-- Provide a catch all Exception Mapper to handle fall back when custom mappers arent supplied
        resources.register(new DefaultExceptionMapper(), MAX_BINDING_PRIORITY);

        //-- Register any supplied mappers
        final List<ExceptionMapper> mappers = config.getExceptionMappers();
        if (!mappers.isEmpty()) {
            mappers.stream().forEach(resources::register);
        }
    }

    public void stopServer() {
        if (running) {
            synchronized (mutex) {
                if (running) {
                    try {
                        running = false;
                        if (!httpServers.isEmpty()) {
                            for (HttpServer httpServer : httpServers) {
                                httpServer.stop(InternalConfigurations.HTTP_API_SHUTDOWN_TIME_SECONDS.get());
                                logger.info("Stopped HTTP server {}", httpServer.getAddress());
                            }
                        }
                    } finally {
                        httpServers.clear();
                        //-- If we have a config supplied executor, bring them down gracefully
                        for (JaxrsHttpServerConfiguration config : configs) {
                            if (config.getHttpThreadPoolExecutor() != null) {
                                ExecutorService threadPoolExecutor = config.getHttpThreadPoolExecutor();
                                if (!threadPoolExecutor.isShutdown()) {
                                    threadPoolExecutor.shutdown();
                                    try {
                                        threadPoolExecutor.awaitTermination(InternalConfigurations.HTTP_API_SHUTDOWN_TIME_SECONDS.get(),
                                                TimeUnit.SECONDS);
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                    } finally {
                                        if (!threadPoolExecutor.isShutdown()) {
                                            threadPoolExecutor.shutdownNow();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected JaxrsObjectMapperProvider createObjectMapperProvider(final @NotNull JaxrsHttpServerConfiguration config) {
        ObjectMapper mapper = config.getObjectMapper();
        if (mapper != null) {
            return new JaxrsObjectMapperProvider(mapper);
        } else {
            return new JaxrsObjectMapperProvider();
        }
    }


    private void registerStaticRoot(
            final @NotNull JaxrsHttpServerConfiguration config, final @NotNull HttpServer server) {
        List<Pair<String, String>> staticResources = config.getStaticResources();
        if (staticResources != null && !staticResources.isEmpty()) {
            staticResources.stream()
                    .forEach(s -> registerContext(s.getLeft(),
                            new StaticFileHandler(objectMapperProvider.getMapper(), s.getRight()),
                            server));
        }
    }

    public void registerContext(
            final @NotNull String contextPath,
            final @NotNull IHttpRequestResponseHandler handler,
            final @NotNull HttpServer server) {
        Preconditions.checkNotNull(contextPath);
        Preconditions.checkNotNull(handler);
        if (handler != null && contextPath != null) {
            logger.trace("Registering context on http server at {}", contextPath);
            server.createContext(contextPath, new SunHttpHandlerProxy(handler));
        }
    }

    class Shutdown implements HiveMQShutdownHook {
        @Override
        public @NotNull String name() {
            return "JAX-RS Server Shutdown";
        }

        @Override
        public void run() {
            stopServer();
        }

        @Override
        public @NotNull Priority priority() {
            return Priority.HIGH;
        }
    }
}
