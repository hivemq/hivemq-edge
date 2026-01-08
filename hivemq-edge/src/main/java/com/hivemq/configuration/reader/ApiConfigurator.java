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
import com.hivemq.api.auth.provider.impl.ldap.LdapConnectionProperties;
import com.hivemq.api.config.ApiJwtConfiguration;
import com.hivemq.api.config.ApiListener;
import com.hivemq.api.config.HttpListener;
import com.hivemq.api.config.HttpsListener;
import com.hivemq.api.model.components.PreLoginNotice;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.api.AdminApiEntity;
import com.hivemq.configuration.entity.api.ApiJwsEntity;
import com.hivemq.configuration.entity.api.ApiListenerEntity;
import com.hivemq.configuration.entity.api.ApiTlsEntity;
import com.hivemq.configuration.entity.api.HttpListenerEntity;
import com.hivemq.configuration.entity.api.HttpsListenerEntity;
import com.hivemq.configuration.entity.api.PreLoginNoticeEntity;
import com.hivemq.configuration.entity.api.UserEntity;
import com.hivemq.configuration.entity.listener.tls.KeystoreEntity;
import com.hivemq.configuration.service.ApiConfigurationService;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.http.core.UsernamePasswordRoles;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static com.hivemq.api.auth.ApiRoles.ADMIN;
import static com.hivemq.http.core.UsernamePasswordRoles.DEFAULT_PASSWORD;
import static com.hivemq.http.core.UsernamePasswordRoles.DEFAULT_USERNAME;

public class ApiConfigurator implements Configurator<AdminApiEntity> {

    private static final @NotNull List<ApiListener> DEFAULT_LISTENERS = List.of(new HttpListener(8080, "127.0.0.1"));
    private static final @NotNull Logger log = LoggerFactory.getLogger(ApiConfigurator.class);
    private static final @NotNull List<UsernamePasswordRoles> DEFAULT_USERS =
            List.of(new UsernamePasswordRoles(DEFAULT_USERNAME, DEFAULT_PASSWORD.getBytes(StandardCharsets.UTF_8), Set.of(ADMIN)));

    private final @NotNull ApiConfigurationService apiCfgService;
    private volatile @Nullable AdminApiEntity configEntity;

    @Inject
    public ApiConfigurator(final @NotNull ApiConfigurationService apiCfgService) {
        this.apiCfgService = apiCfgService;
    }

    private static @NotNull UsernamePasswordRoles fromModel(final @NotNull UserEntity userEntity) {
        return new UsernamePasswordRoles(userEntity.getUserName(),
                userEntity.getPassword().getBytes(StandardCharsets.UTF_8),
                Set.copyOf(userEntity.getRoles()));
    }

    //-- Converts XML entity types to bean types

    @Override
    public boolean needsRestartWithConfig(final @NotNull HiveMQConfigEntity config) {
        final AdminApiEntity entity = configEntity;
        return entity != null && hasChanged(entity, config.getApiConfig());
    }

    @Override
    public @NotNull ConfigResult applyConfig(final @NotNull HiveMQConfigEntity config) {
        final AdminApiEntity entity = config.getApiConfig();

        configEntity = entity;

        apiCfgService.setEnabled(entity.isEnabled());
        apiCfgService.setEnforceApiAuth(entity.isEnforceApiAuth());

        // Users
        if(entity.getLdap() != null) {
            apiCfgService.setLdapConnectionProperties(LdapConnectionProperties.fromEntity(entity.getLdap()));
        } else {
            final List<UserEntity> users = entity.getUsers();
            if (!users.isEmpty()) {
                log.warn("The <users> element in the <api> configuration is deprecated and will be removed in future versions. " +
                        "Please use the <username-roles-source> element instead.");
                apiCfgService.setUserList(users.stream().map(ApiConfigurator::fromModel).toList());
            } else {
                apiCfgService.setUserList(DEFAULT_USERS);
            }
        }

        // JWT
        final ApiJwsEntity jwsEntity = entity.getJws();
        apiCfgService.setApiJwtConfiguration(new ApiJwtConfiguration.Builder().withAudience(jwsEntity.getAudience())
                .withIssuer(jwsEntity.getIssuer())
                .withKeySize(jwsEntity.getKeySize())
                .withExpiryTimeMinutes(jwsEntity.getExpiryTimeMinutes())
                .withTokenEarlyEpochThresholdMinutes(jwsEntity.getTokenEarlyEpochThresholdMinutes())
                .build());

        if (entity.getListeners().isEmpty()) {
            //set default listener
            apiCfgService.setListeners(DEFAULT_LISTENERS);
        } else {
            final ImmutableList.Builder<@NotNull ApiListener> listenersBld = ImmutableList.builder();
            for (final ApiListenerEntity listener : entity.getListeners()) {
                if (listener instanceof HttpListenerEntity) {
                    listenersBld.add(new HttpListener(listener.getPort(), listener.getBindAddress()));
                } else if (listener instanceof HttpsListenerEntity) {
                    final ApiTlsEntity tls = ((HttpsListenerEntity) listener).getTls();
                    final KeystoreEntity keystoreEntity = tls.getKeystoreEntity();
                    if (keystoreEntity == null) {
                        log.error("Keystore can not be emtpy for HTTPS listener");
                        throw new UnrecoverableException(false);
                    }
                    listenersBld.add(new HttpsListener(listener.getPort(),
                            listener.getBindAddress(),
                            tls.getProtocols(),
                            tls.getCipherSuites(),
                            keystoreEntity.getPath(),
                            keystoreEntity.getPassword(),
                            keystoreEntity.getPrivateKeyPassword()));
                } else {
                    log.error("Unknown API listener type");
                    throw new UnrecoverableException(false);
                }
            }
            apiCfgService.setListeners(listenersBld.build());
        }

        // pre login message
        final PreLoginNoticeEntity pln = entity.getPreLoginNotice();
        apiCfgService.setPreLoginNotice(new PreLoginNotice(pln.isEnabled(),
                pln.getTitle(),
                pln.getMessage(),
                pln.getConsent()));

        return ConfigResult.SUCCESS;
    }
}
