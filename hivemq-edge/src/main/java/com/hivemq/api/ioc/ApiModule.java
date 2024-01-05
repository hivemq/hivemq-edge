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
import com.hivemq.api.resources.AuthenticationApi;
import com.hivemq.api.resources.BridgeApi;
import com.hivemq.api.resources.EventApi;
import com.hivemq.api.resources.FrontendApi;
import com.hivemq.api.resources.GatewayApi;
import com.hivemq.api.resources.HealthCheckApi;
import com.hivemq.api.resources.MetricsApi;
import com.hivemq.api.resources.ProtocolAdaptersApi;
import com.hivemq.api.resources.UnsApi;
import com.hivemq.api.resources.impl.AuthenticationResourceImpl;
import com.hivemq.api.resources.impl.BridgeResourceImpl;
import com.hivemq.api.resources.impl.EventResourceImpl;
import com.hivemq.api.resources.impl.FrontendResourceImpl;
import com.hivemq.api.resources.impl.GatewayResourceImpl;
import com.hivemq.api.resources.impl.HealthCheckResourceImpl;
import com.hivemq.api.resources.impl.MetricsResourceImpl;
import com.hivemq.api.resources.impl.ProtocolAdaptersResourceImpl;
import com.hivemq.api.resources.impl.UnsResourceImpl;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.ApiConfigurationService;
import com.hivemq.edge.HiveMQCapabilityService;
import com.hivemq.edge.impl.capability.CapabilityServiceImpl;
import com.hivemq.extension.sdk.api.annotations.NotNull;
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
    abstract @NotNull BridgeApi bridgeApi(@NotNull BridgeResourceImpl bridgeResource);
    @Binds
    abstract @NotNull MetricsApi metricsApi(@NotNull MetricsResourceImpl metricsResource);
    @Binds
    abstract @NotNull HealthCheckApi healthCheckApi(@NotNull HealthCheckResourceImpl healthCheckResource);
    @Binds
    abstract @NotNull ProtocolAdaptersApi protocolAdaptersApi(@NotNull ProtocolAdaptersResourceImpl protocolAdaptersResource);
    @Binds
    abstract @NotNull UnsApi unsApi(@NotNull UnsResourceImpl unsResource);
    @Binds
    abstract @NotNull FrontendApi frontendApi(@NotNull FrontendResourceImpl gatewayResource);
    @Binds
    abstract @NotNull GatewayApi gatewayApi(@NotNull GatewayResourceImpl gatewayResource);
    @Binds
    abstract @NotNull EventApi eventApi(@NotNull EventResourceImpl eventResource);
    @Binds
    abstract @NotNull ITokenVerifier tokenVerifier(@NotNull JwtAuthenticationProvider jwtAuthenticationProvider);
    @Binds
    abstract @NotNull ITokenGenerator tokenGenerator(@NotNull JwtAuthenticationProvider jwtAuthenticationProvider);

    @Provides
    @Singleton
    static @NotNull JwtAuthenticationProvider jwtAuthenticationProvider(ApiConfigurationService apiConfigurationService) {
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
        SimpleUsernamePasswordProviderImpl provider = new SimpleUsernamePasswordProviderImpl();
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

        for (ApiListener listener : apiConfigurationService.getListeners()) {
            final JaxrsHttpServerConfiguration jaxrsConfiguration =
                    JaxrsBootstrapFactory.createJaxrsConfiguration(apiConfigurationService, listener);
            builder.add(jaxrsConfiguration);
        }

        return new JaxrsHttpServer(shutdownHooks, builder.build(), registry);
    }
}
