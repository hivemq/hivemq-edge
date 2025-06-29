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
package com.hivemq.api;

import com.hivemq.api.auth.ApiAuthenticationFeature;
import com.hivemq.api.auth.handler.IAuthenticationHandler;
import com.hivemq.api.auth.provider.ITokenGenerator;
import com.hivemq.api.auth.provider.ITokenVerifier;
import com.hivemq.api.error.ApiExceptionMapper;
import com.hivemq.api.error.CustomJsonMappingExceptionMapper;
import com.hivemq.api.error.CustomJsonParseExceptionMapper;
import com.hivemq.api.filter.JWTReissuanceFilterImpl;
import com.hivemq.api.resources.GenericAPIHolder;

import com.hivemq.api.resources.impl.RootResource;
import com.hivemq.edge.api.AuthenticationApi;
import com.hivemq.edge.api.BridgesApi;
import com.hivemq.edge.api.CombinersApi;
import com.hivemq.edge.api.EventsApi;
import com.hivemq.edge.api.FrontendApi;
import com.hivemq.edge.api.GatewayEndpointApi;
import com.hivemq.edge.api.HealthCheckEndpointApi;
import com.hivemq.edge.api.MetricsApi;
import com.hivemq.edge.api.PayloadSamplingApi;
import com.hivemq.edge.api.ProtocolAdaptersApi;
import com.hivemq.edge.api.TopicFiltersApi;
import com.hivemq.edge.api.UnsApi;
import io.swagger.annotations.Api;
import org.jetbrains.annotations.NotNull;
import com.hivemq.http.error.DefaultExceptionMapper;
import dagger.Lazy;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static com.hivemq.http.JaxrsHttpServer.MAX_BINDING_PRIORITY;

/**
 * Define a Resource Config that passes managed objects into the JAX-RS context. NOTE: Using instances mean the objects
 * will not be
 * managed by the JAXRS container.
 * <p>
 * NOTE: Since we're injecting interfaces, and the interfaces contain the markup, the container will log about the impls
 * not
 * having markup - this is OK an can be ignored.
 *
 * @author Simon L Johnson
 */
@Singleton
public class ApiResourceRegistry extends ResourceConfig {

    protected final org.slf4j.Logger logger = LoggerFactory.getLogger(ApiResourceRegistry.class);
    private final @NotNull Lazy<MetricsApi> metricsApi;
    private final @NotNull Lazy<HealthCheckEndpointApi> healthCheckApi;
    private final @NotNull Lazy<AuthenticationApi> authenticationApi;
    private final @NotNull Lazy<BridgesApi> bridgeApi;
    private final @NotNull Lazy<MetricsApi> dashboardApi;
    private final @NotNull Lazy<ProtocolAdaptersApi> protocolAdaptersApi;
    private final @NotNull Lazy<UnsApi> unsApi;
    private final @NotNull Lazy<FrontendApi> frontendApi;
    private final @NotNull Lazy<GatewayEndpointApi> gatewayApi;
    private final @NotNull Lazy<EventsApi> eventApi;
    private final @NotNull Lazy<RootResource> rootResource;
    private final @NotNull Lazy<Set<IAuthenticationHandler>> authenticationHandlers;
    private final @NotNull Lazy<ITokenGenerator> tokenGenerator;
    private final @NotNull Lazy<ITokenVerifier> tokenVerifier;
    private final @NotNull Lazy<GenericAPIHolder> genericAPIHolderLazy;
    private final @NotNull Lazy<PayloadSamplingApi> samplingResourceLazy;
    private final @NotNull Lazy<TopicFiltersApi> topicFilterApiLazy;
    private final @NotNull Lazy<CombinersApi> combinersApiLazy;


    @Inject
    public ApiResourceRegistry(
            final @NotNull Lazy<MetricsApi> metricsApi,
            final @NotNull Lazy<HealthCheckEndpointApi> healthCheckApi,
            final @NotNull Lazy<AuthenticationApi> authenticationApi,
            final @NotNull Lazy<BridgesApi> bridgeApi,
            final @NotNull Lazy<MetricsApi> dashboardApi,
            final @NotNull Lazy<ProtocolAdaptersApi> protocolAdaptersApi,
            final @NotNull Lazy<UnsApi> unsApi,
            final @NotNull Lazy<FrontendApi> frontendApi,
            final @NotNull Lazy<GatewayEndpointApi> gatewayApi,
            final @NotNull Lazy<EventsApi> eventApi,
            final @NotNull Lazy<RootResource> rootResource,
            final @NotNull Lazy<Set<IAuthenticationHandler>> authenticationHandlers,
            final @NotNull Lazy<ITokenGenerator> tokenGenerator,
            final @NotNull Lazy<ITokenVerifier> tokenVerifier,
            final @NotNull Lazy<GenericAPIHolder> genericAPIHolderLazy,
            final @NotNull Lazy<PayloadSamplingApi> samplingResourceLazy,
            final @NotNull Lazy<TopicFiltersApi> topicFilterApiLazy,
            final @NotNull Lazy<CombinersApi> combinersApiLazy) {
        this.authenticationApi = authenticationApi;
        this.metricsApi = metricsApi;
        this.healthCheckApi = healthCheckApi;
        this.bridgeApi = bridgeApi;
        this.dashboardApi = dashboardApi;
        this.protocolAdaptersApi = protocolAdaptersApi;
        this.unsApi = unsApi;
        this.frontendApi = frontendApi;
        this.gatewayApi = gatewayApi;
        this.eventApi = eventApi;
        this.rootResource = rootResource;
        this.authenticationHandlers = authenticationHandlers;
        this.tokenGenerator = tokenGenerator;
        this.tokenVerifier = tokenVerifier;
        this.genericAPIHolderLazy = genericAPIHolderLazy;
        this.samplingResourceLazy = samplingResourceLazy;
        this.topicFilterApiLazy = topicFilterApiLazy;
        this.combinersApiLazy = combinersApiLazy;
    }

    @Inject //method injection, this gets called once after instantiation
    public void postConstruct() {
        logger.trace("Initializing API resources");
        final long start = System.currentTimeMillis();
        registerJaxrsResources();
        registerMappers();
        registerFeatures();
        registerGenericComponents();
        logger.trace("Initialized API resources in {}ms", (System.currentTimeMillis() - start));
    }

    protected void registerJaxrsResources() {
        register(rootResource.get());
        register(healthCheckApi.get());
        logger.trace("Initialized healthCheckApi API resources");
        register(metricsApi.get());
        logger.trace("Initialized metricsApi API resources");
        register(bridgeApi.get());
        logger.trace("Initialized bridgeApi API resources");
        register(dashboardApi.get());
        logger.trace("Initialized dashboardApi API resources");
        register(authenticationApi.get());
        logger.trace("Initialized authenticationApi API resources");
        register(protocolAdaptersApi.get());
        logger.trace("Initialized protocolAdaptersApi API resources");
        register(unsApi.get());
        logger.trace("Initialized unsApi API resources");
        register(frontendApi.get());
        logger.trace("Initialized frontendApi API resources");
        register(gatewayApi.get());
        logger.trace("Initialized gatewayApi API resources");
        register(eventApi.get());
        logger.trace("Initialized event API resources");
        register(samplingResourceLazy.get());
        logger.trace("Initialized sampling API resources");
        register(topicFilterApiLazy.get());
        logger.trace("Initialized topic filter API resources");
        register(combinersApiLazy.get());
        logger.trace("Initialized combiners API resources");
    }

    protected void registerMappers() {
        register(MarshallingFeature.class);
        register(new CustomJsonMappingExceptionMapper(), MAX_BINDING_PRIORITY);
        register(new CustomJsonParseExceptionMapper(), MAX_BINDING_PRIORITY);
        register(new ApiExceptionMapper(), MAX_BINDING_PRIORITY);
        register(new DefaultExceptionMapper(), MAX_BINDING_PRIORITY);
        if (Boolean.getBoolean("api.wire.logging.enabled")) {
            register(new LoggingFeature(new Logger(getClass().getName(), null) {
                @Override
                public void log(final @NotNull LogRecord record) {
                    logger.info(record.getMessage());
                }
            }, Level.INFO, LoggingFeature.Verbosity.PAYLOAD_ANY, 10000));
        }
    }

    protected void registerFeatures() {
        register(new ApiAuthenticationFeature(authenticationHandlers.get()));
        register(new JWTReissuanceFilterImpl(tokenGenerator.get(), tokenVerifier.get()));
    }

    protected void registerGenericComponents() {
        for (final Object component : genericAPIHolderLazy.get().getComponents()) {
            register(component);
        }
    }
}
