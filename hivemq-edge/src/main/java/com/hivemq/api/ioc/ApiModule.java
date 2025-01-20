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
package com.hivemq.api.ioc;

import com.google.common.collect.ImmutableList;
import com.hivemq.api.ApiResourceRegistry;
import com.hivemq.api.auth.handler.IAuthenticationHandler;
import com.hivemq.api.auth.handler.impl.BearerTokenAuthenticationHandler;
import com.hivemq.api.auth.jwt.JwtAuthenticationProvider;
import com.hivemq.api.auth.provider.ITokenGenerator;
import com.hivemq.api.auth.provider.ITokenVerifier;
import com.hivemq.api.auth.provider.IUsernamePasswordProvider;
import com.hivemq.api.auth.provider.impl.SimpleUsernamePasswordProviderImpl;
import com.hivemq.api.config.ApiListener;
import com.hivemq.api.resources.impl.AuthenticationResourceImpl;
import com.hivemq.api.resources.impl.BridgeResourceImpl;
import com.hivemq.api.resources.impl.EventResourceImpl;
import com.hivemq.api.resources.impl.FrontendResourceImpl;
import com.hivemq.api.resources.impl.GatewayResourceImpl;
import com.hivemq.api.resources.impl.HealthCheckResourceImpl;
import com.hivemq.api.resources.impl.MetricsResourceImpl;
import com.hivemq.api.resources.impl.ProtocolAdaptersResourceImpl;
import com.hivemq.api.resources.impl.SamplingResourceImpl;
import com.hivemq.api.resources.impl.TopicFilterResourceImpl;
import com.hivemq.api.resources.impl.UnsResourceImpl;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.ApiConfigurationService;
import com.hivemq.edge.api.AuthenticationApi;
import com.hivemq.edge.api.BridgesApi;
import com.hivemq.edge.api.EventsApi;
import com.hivemq.edge.api.FrontendApi;
import com.hivemq.edge.api.GatewayEndpointApi;
import com.hivemq.edge.api.HealthCheckEndpointApi;
import com.hivemq.edge.api.MetricsApi;
import com.hivemq.edge.api.PayloadSamplingApi;
import com.hivemq.edge.api.ProtocolAdaptersApi;
import com.hivemq.edge.api.TopicFiltersApi;
import com.hivemq.edge.api.UnsApi;
import org.jetbrains.annotations.NotNull;
import com.hivemq.http.JaxrsHttpServer;
import com.hivemq.http.config.JaxrsBootstrapFactory;
import com.hivemq.http.config.JaxrsHttpServerConfiguration;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ElementsIntoSet;
import dagger.multibindings.IntoSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Set;

@Module
public abstract class ApiModule {

    private static final Logger log = LoggerFactory.getLogger(ApiModule.class);

    @Binds
    abstract @NotNull AuthenticationApi authenticationApi(@NotNull AuthenticationResourceImpl authenticationResource);
    @Binds
    abstract @NotNull BridgesApi bridgeApi(@NotNull BridgeResourceImpl bridgeResource);
    @Binds
    abstract @NotNull MetricsApi metricsApi(@NotNull MetricsResourceImpl metricsResource);
    @Binds
    abstract @NotNull HealthCheckEndpointApi healthCheckApi(@NotNull HealthCheckResourceImpl healthCheckResource);
    @Binds
    abstract @NotNull ProtocolAdaptersApi protocolAdaptersApi(@NotNull ProtocolAdaptersResourceImpl protocolAdaptersResource);
    @Binds
    abstract @NotNull UnsApi unsApi(@NotNull UnsResourceImpl unsResource);
    @Binds
    abstract @NotNull FrontendApi frontendApi(@NotNull FrontendResourceImpl gatewayResource);
    @Binds
    abstract @NotNull GatewayEndpointApi gatewayApi(@NotNull GatewayResourceImpl gatewayResource);
    @Binds
    abstract @NotNull EventsApi eventApi(@NotNull EventResourceImpl eventResource);
    @Binds
    abstract @NotNull ITokenVerifier tokenVerifier(@NotNull JwtAuthenticationProvider jwtAuthenticationProvider);
    @Binds
    abstract @NotNull ITokenGenerator tokenGenerator(@NotNull JwtAuthenticationProvider jwtAuthenticationProvider);
    @Binds
    abstract @NotNull PayloadSamplingApi samplingResource(@NotNull SamplingResourceImpl samplingResource);
    @Binds
    abstract @NotNull TopicFiltersApi topicFilterApi(@NotNull TopicFilterResourceImpl topicFilterResource);


    @Provides
    @Singleton
    static @NotNull JwtAuthenticationProvider jwtAuthenticationProvider(final ApiConfigurationService apiConfigurationService) {
        return new JwtAuthenticationProvider(apiConfigurationService.getApiJwtConfiguration());
    }

    @Provides
    @IntoSet
    static @NotNull Boolean eagerSingletons(
            final @NotNull ApiResourceRegistry apiResourceRegistry) {
        // this is used to instantiate all the params, similar to guice's asEagerSingleton and returns nothing
        return Boolean.TRUE;
    }

    @Provides
    @ElementsIntoSet
    @Singleton
    static Set<IAuthenticationHandler> provideAuthHandlers(
            /*final @NotNull BasicAuthenticationHandler basicAuthenticationHandler,*/
            final @NotNull BearerTokenAuthenticationHandler bearerTokenAuthenticationHandler) {
        return Set.of(bearerTokenAuthenticationHandler);
    }

    @Provides
    @Singleton
    static IUsernamePasswordProvider usernamePasswordProvider(final @NotNull ApiConfigurationService apiConfigurationService) {
        //Generic Credentials used by Both Authentication Handler
        final SimpleUsernamePasswordProviderImpl provider = new SimpleUsernamePasswordProviderImpl();
        log.trace("Applying {} users to API access list", apiConfigurationService.getUserList().size());
        apiConfigurationService.getUserList().forEach(provider::add);
        return provider;
    }

    @Provides
    @Singleton
    static JaxrsHttpServer jaxrsHttpServer(
            final @NotNull ShutdownHooks shutdownHooks,
            final @NotNull ApiConfigurationService apiConfigurationService,
            final @NotNull ApiResourceRegistry registry) {
        final ImmutableList.Builder<JaxrsHttpServerConfiguration> builder = ImmutableList.builder();

        for (final ApiListener listener : apiConfigurationService.getListeners()) {
            final JaxrsHttpServerConfiguration jaxrsConfiguration =
                    JaxrsBootstrapFactory.createJaxrsConfiguration(apiConfigurationService, listener);
            builder.add(jaxrsConfiguration);
        }

        return new JaxrsHttpServer(shutdownHooks, builder.build(), registry);
    }
}
