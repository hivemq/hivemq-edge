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
package com.hivemq.configuration.reader;

import com.google.common.collect.ImmutableList;
import com.hivemq.api.config.ApiJwtConfiguration;
import com.hivemq.api.config.ApiListener;
import com.hivemq.api.config.HttpListener;
import com.hivemq.api.config.HttpsListener;
import com.hivemq.configuration.entity.api.*;
import com.hivemq.configuration.entity.listener.tls.KeystoreEntity;
import com.hivemq.configuration.service.ApiConfigurationService;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.http.core.UsernamePasswordRoles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ApiConfigurator {

    private static final Logger log = LoggerFactory.getLogger(ApiConfigurator.class);

    private final @NotNull ApiConfigurationService apiConfigurationService;

    @Inject
    public ApiConfigurator(
            final @NotNull ApiConfigurationService apiConfigurationService) {
        this.apiConfigurationService = apiConfigurationService;
    }

    //-- Converts XML entity types to bean types

    public void setApiConfig(final @NotNull AdminApiEntity configEntity) {

        if (configEntity == null) {
            return;
        }

        apiConfigurationService.setEnabled(configEntity.isEnabled());

        //Users
        if (configEntity.getUsers() != null && !configEntity.getUsers().isEmpty()) {
            apiConfigurationService.setUserList(configEntity.getUsers()
                    .stream()
                    .map(userEntity -> new UsernamePasswordRoles(userEntity.getUserName(),
                            userEntity.getPassword(),
                            Set.copyOf(userEntity.getRoles())))
                    .collect(Collectors.toList()));
        } else {
            apiConfigurationService.setUserList(List.of(
                    new UsernamePasswordRoles(UsernamePasswordRoles.DEFAULT_USERNAME,
                            UsernamePasswordRoles.DEFAULT_PASSWORD, Set.of("ADMIN"))));
        }



        //JWT
        ApiJwsEntity jwsEntity = configEntity.getJws();
        ApiJwtConfiguration.Builder apiJwtConfigurationBuilder = new ApiJwtConfiguration.Builder();
        if (jwsEntity != null) {
            apiJwtConfigurationBuilder.withAudience(jwsEntity.getAudience())
                    .withIssuer(jwsEntity.getIssuer())
                    .withKeySize(jwsEntity.getKeySize())
                    .withExpiryTimeMinutes(jwsEntity.getExpiryTimeMinutes())
                    .withTokenEarlyEpochThresholdMinutes(jwsEntity.getTokenEarlyEpochThresholdMinutes());
        }
        apiConfigurationService.setApiJwtConfiguration(apiJwtConfigurationBuilder.build());

        if (configEntity.getListeners().isEmpty()) {
            //set default listener
            apiConfigurationService.setListeners(List.of(new HttpListener(8080, "127.0.0.1")));
        } else {
            final ImmutableList.Builder<ApiListener> builder = ImmutableList.builder();
            for (ApiListenerEntity listener : configEntity.getListeners()) {
                if (listener instanceof HttpListenerEntity) {
                    builder.add(new HttpListener(listener.getPort(), listener.getBindAddress()));
                } else if (listener instanceof HttpsListenerEntity) {
                    final ApiTlsEntity tls = ((HttpsListenerEntity) listener).getTls();
                    final KeystoreEntity keystoreEntity = tls.getKeystoreEntity();
                    if (keystoreEntity == null) {
                        log.error("Keystore can not be emtpy for HTTPS listener");
                        throw new UnrecoverableException(false);
                    }
                    builder.add(new HttpsListener(listener.getPort(),
                            listener.getBindAddress(),
                            tls.getProtocols(),
                            tls.getCipherSuites(),
                            keystoreEntity.getPath(),
                            keystoreEntity.getPassword(),
                            keystoreEntity.getPrivateKeyPassword()));
                } else {
                    log.error("Unkown API listener type");
                    throw new UnrecoverableException(false);
                }
            }
            apiConfigurationService.setListeners(builder.build());
        }

    }
}
