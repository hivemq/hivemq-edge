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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.sun.net.httpserver.HttpsConfigurator;
import org.apache.commons.lang3.tuple.Pair;

import javax.net.ssl.SSLContext;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Configuration for the HTTP & Jaxrs layer with reasonable defaults.
 *
 * @author Simon L Johnson
 */
public class JaxrsHttpServerConfiguration {

    public static int DEFAULT_HTTP_PORT = 8080;
    public static String DEFAULT_HOSTNAME = "localhost";
    public static String HTTP_PROTOCOL = "http";
    public static String HTTPS_PROTOCOL = "https";
    public static String DEFAULT_STATIC_ROOT = "httpd";
    public static String DEFAULT_STATIC_ROOT_CONTEXT = "/static";

    protected int port = DEFAULT_HTTP_PORT;
    protected @NotNull String host = DEFAULT_HOSTNAME;
    protected @NotNull String protocol = HTTP_PROTOCOL;
    protected @NotNull Set<Class<?>> resourceClasses = new HashSet<>();
    protected @NotNull List<Pair<String, String>> staticResources = new ArrayList<>(){{
        add(Pair.of(DEFAULT_STATIC_ROOT_CONTEXT, DEFAULT_STATIC_ROOT));
    }};
    protected int httpThreadPoolShutdownTimeoutSeconds = 2;
    protected int httpThreadPoolSize = 2;
    protected @Nullable ExecutorService httpThreadPoolExecutor;
    protected @Nullable ObjectMapper objectMapper;
    protected @Nullable List<ExceptionMapper> exceptionMappers = new ArrayList<>();
    protected @Nullable SSLContext sslContext;
    protected @Nullable HttpsConfigurator httpsConfigurator;

    public SSLContext getSslContext() {
        return sslContext;
    }

    /**
     * Supply an SSL context to enable the API server to run in secure mode.
     */
    public void setSslContext(final SSLContext sslContext) {
        this.sslContext = sslContext;
        this.setProtocol(HTTPS_PROTOCOL);
    }

    public HttpsConfigurator getHttpsConfigurator() {
        return httpsConfigurator;
    }

    /**
     * Supply a configurator which allows customization of the SSL parameters associated
     * with a request.
     *
     * Use this method to change the Protocol or CipherSuite for the given SSLContext for example:
     * <code>
     *     config.setHttpsConfigurator(new HttpsConfigurator(context){
     *             @Override
     *             public void configure(final HttpsParameters params) {
     *                 SSLParameters parameters = getSSLContext().getDefaultSSLParameters();
     *                 parameters.setProtocols(new String[]{"TLSv1.2"} );
     *                 params.setSSLParameters(parameters);
     *             }
     *         });
     * </code>
     * @param httpsConfigurator
     */
    public void setHttpsConfigurator(final HttpsConfigurator httpsConfigurator) {
        this.httpsConfigurator = httpsConfigurator;
    }

    public int getPort() {
        return port;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(final String protocol) {
        this.protocol = protocol;
    }

    public Set<Class<?>> getResourceClasses() {
        return resourceClasses;
    }

    public void addResourceClasses(Class<?>... clazzz) {
        resourceClasses.addAll(Sets.newHashSet(clazzz));
    }

    public ExecutorService getHttpThreadPoolExecutor() {
        return httpThreadPoolExecutor;
    }

    public void setHttpThreadPoolExecutor(final ExecutorService httpThreadPoolExecutor) {
        this.httpThreadPoolExecutor = httpThreadPoolExecutor;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public void setObjectMapper(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public int getHttpThreadPoolShutdownTimeoutSeconds() {
        return httpThreadPoolShutdownTimeoutSeconds;
    }

    public void setHttpThreadPoolShutdownTimeoutSeconds(final int httpThreadPoolShutdownTimeoutSeconds) {
        this.httpThreadPoolShutdownTimeoutSeconds = httpThreadPoolShutdownTimeoutSeconds;
    }

    public void addStaticResource(final @NotNull Pair<String, String> res){
        staticResources.add(res);
    }

    public List<ExceptionMapper> getExceptionMappers() {
        return exceptionMappers;
    }

    public void addExceptionMappers(ExceptionMapper... mappers){
        exceptionMappers.addAll(Arrays.asList(mappers));
    }

    public int getHttpThreadPoolSize() {
        return httpThreadPoolSize;
    }

    public void setHttpThreadPoolSize(final int httpThreadPoolSize) {
        this.httpThreadPoolSize = httpThreadPoolSize;
    }

    public List<Pair<String, String>> getStaticResources() {
        return staticResources;
    }

    public void setStaticResources(final List<Pair<String, String>> staticResources) {
        this.staticResources = staticResources;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("JaxrsHttpServerConfiguration{");
        sb.append("port=").append(port);
        sb.append(", host='").append(host).append('\'');
        sb.append(", protocol='").append(protocol).append('\'');
        sb.append(", resourceClasses=").append(resourceClasses);
        sb.append(", httpThreadPoolShutdownTimeoutSeconds=").append(httpThreadPoolShutdownTimeoutSeconds);
        sb.append('}');
        return sb.toString();
    }
}
