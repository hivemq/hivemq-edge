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

import static com.hivemq.api.auth.ApiRoles.ADMIN;
import static com.hivemq.http.core.UsernamePasswordRoles.DEFAULT_PASSWORD;
import static com.hivemq.http.core.UsernamePasswordRoles.DEFAULT_USERNAME;

import com.google.common.collect.ImmutableList;
import com.hivemq.api.auth.provider.impl.ldap.LdapConnectionProperties;
import com.hivemq.api.config.ApiJwtConfiguration;
import com.hivemq.api.config.ApiListener;
import com.hivemq.api.config.AuthMode;
import com.hivemq.api.config.HttpListener;
import com.hivemq.api.config.HttpsListener;
import com.hivemq.api.config.OidcConfiguration;
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
import com.hivemq.configuration.entity.api.UsernameAuthenticationEntity;
import com.hivemq.configuration.entity.api.oidc.OidcAuthenticationEntity;
import com.hivemq.configuration.entity.listener.tls.KeystoreEntity;
import com.hivemq.configuration.service.ApiConfigurationService;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.http.core.UsernamePasswordRoles;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiConfigurator implements Configurator<AdminApiEntity> {

    private static final @NotNull List<ApiListener> DEFAULT_LISTENERS = List.of(new HttpListener(8080, "127.0.0.1"));
    private static final @NotNull Logger log = LoggerFactory.getLogger(ApiConfigurator.class);
    private static final @NotNull List<UsernamePasswordRoles> DEFAULT_USERS = List.of(new UsernamePasswordRoles(
            DEFAULT_USERNAME, DEFAULT_PASSWORD.getBytes(StandardCharsets.UTF_8), Set.of(ADMIN)));

    private final @NotNull ApiConfigurationService apiCfgService;
    private volatile @Nullable AdminApiEntity configEntity;

    @Inject
    public ApiConfigurator(final @NotNull ApiConfigurationService apiCfgService) {
        this.apiCfgService = apiCfgService;
    }

    private static @NotNull UsernamePasswordRoles fromModel(final @NotNull UserEntity userEntity) {
        return new UsernamePasswordRoles(
                Objects.requireNonNull(userEntity.getUserName()),
                Objects.requireNonNull(userEntity.getPassword()).getBytes(StandardCharsets.UTF_8),
                Set.copyOf(userEntity.getRoles()));
    }

    private static boolean isBlank(final @Nullable String value) {
        return value == null || value.isBlank();
    }

    /**
     * Resolves whether local username/password authentication is active. An absent
     * {@code <username-authentication>} defaults to enabled, preserving the behavior of a
     * configuration that predates the element. When present, {@code <enabled>} is required (XSD).
     */
    private static boolean resolveUsernameAuthEnabled(final @NotNull AdminApiEntity entity) {
        final UsernameAuthenticationEntity usernameAuth = entity.getUsernameAuthentication();
        return usernameAuth == null || usernameAuth.isEnabled();
    }

    // -- Converts XML entity types to bean types

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

        // Each authentication mechanism carries its own <enabled> flag. Local username/password defaults
        // to enabled when its stanza is absent; OIDC is off when its stanza is absent.
        final OidcAuthenticationEntity oidcEntity = entity.getOidc();
        final boolean usernameAuthEnabled = resolveUsernameAuthEnabled(entity);
        final boolean oidcEnabled = oidcEntity != null && oidcEntity.isEnabled();

        // Presence rule: an <oidc-authentication> stanza (enabled or not) forces the operator to state
        // <username-authentication> explicitly, so local login is never left on implicitly alongside OIDC.
        if (oidcEntity != null && entity.getUsernameAuthentication() == null) {
            log.error("An <oidc-authentication> stanza is configured but <username-authentication> is absent. "
                    + "Add <username-authentication><enabled>true|false</enabled></username-authentication> to state "
                    + "explicitly whether local login is available.");
            throw new UnrecoverableException(false);
        }

        final EnumSet<AuthMode> authModes = EnumSet.noneOf(AuthMode.class);
        if (usernameAuthEnabled) {
            authModes.add(AuthMode.USERNAME_PASSWORD);
        }
        if (oidcEnabled) {
            authModes.add(AuthMode.OPEN_ID);
        }
        apiCfgService.setAuthModes(authModes);

        // Local users / LDAP — only wired when username/password authentication is enabled.
        if (usernameAuthEnabled) {
            if (entity.getLdap() != null) {
                apiCfgService.setLdapConnectionProperties(LdapConnectionProperties.fromEntity(entity.getLdap()));
            } else {
                // An empty list means no usable local user source: <users> was either absent or present but
                // empty (JAXB collapses both to an empty list). Either way there is nothing to authenticate
                // against, so the branches below fall through to the first-boot default or the no-source error
                // exactly as an absent <users> would. An empty <users> is a legitimate state -- it is not
                // enforced away in the schema, because whether it is acceptable depends on <enabled> and <ldap>.
                final List<UserEntity> users = entity.getUsers();
                if (!users.isEmpty()) {
                    log.warn(
                            "The <users> element in the <api> configuration is deprecated and will be removed in future versions. "
                                    + "Please use the <username-roles-source> element instead.");
                    apiCfgService.setUserList(
                            users.stream().map(ApiConfigurator::fromModel).toList());
                } else if (entity.getUsernameAuthentication() == null) {
                    // Nothing was stated: no <username-authentication>, no <ldap>, no <users> (and — by the
                    // presence rule above — no <oidc-authentication>). This is the first-boot convenience:
                    // the built-in admin account is the only way in. It survives ONLY in this fully-implicit
                    // branch.
                    apiCfgService.setUserList(DEFAULT_USERS);
                } else {
                    // Local login was turned on explicitly but no source was configured. Rather than silently
                    // fall back to the built-in admin account (which would expose default credentials — see
                    // EDG-849), reject the configuration so the operator states a real source.
                    log.error("<username-authentication> is enabled but no user source is configured. "
                            + "Add a <users> element with at least one <user>, or an enabled <ldap>, "
                            + "or set <username-authentication><enabled>false</enabled> to close local login.");
                    throw new UnrecoverableException(false);
                }
            }
        } else {
            // Local login disabled: no local users, no default admin. The endpoint is closed.
            apiCfgService.setUserList(List.of());
        }

        // JWT
        final ApiJwsEntity jwsEntity = entity.getJws();
        apiCfgService.setApiJwtConfiguration(new ApiJwtConfiguration.Builder()
                .withAudience(jwsEntity.getAudience())
                .withIssuer(jwsEntity.getIssuer())
                .withKeySize(jwsEntity.getKeySize())
                .withExpiryTimeMinutes(jwsEntity.getExpiryTimeMinutes())
                .withTokenEarlyEpochThresholdMinutes(jwsEntity.getTokenEarlyEpochThresholdMinutes())
                .build());

        // OIDC (only wired when its stanza is enabled).
        if (oidcEnabled) {
            // oidcEnabled implies oidcEntity != null; the local makes that visible to null analysis.
            final OidcAuthenticationEntity enabledOidc = Objects.requireNonNull(oidcEntity);
            if (isBlank(enabledOidc.getIssuerUri())
                    || isBlank(enabledOidc.getClientId())
                    || isBlank(enabledOidc.getRedirectUri())) {
                log.error("OIDC authentication is configured but incomplete: <issuer-uri>, <client-id> and "
                        + "<redirect-uri> are all required.");
                throw new UnrecoverableException(false);
            }
            try {
                apiCfgService.setOidcConfiguration(OidcConfiguration.fromEntity(enabledOidc));
            } catch (final IllegalArgumentException e) {
                log.error("Invalid OIDC configuration: {}", e.getMessage());
                throw new UnrecoverableException(false);
            }
        }

        if (entity.getListeners().isEmpty()) {
            // set default listener
            apiCfgService.setListeners(DEFAULT_LISTENERS);
        } else {
            final ImmutableList.Builder<@NotNull ApiListener> listenersBld = ImmutableList.builder();
            for (final ApiListenerEntity listener : entity.getListeners()) {
                if (listener instanceof HttpListenerEntity) {
                    listenersBld.add(new HttpListener(listener.getPort(), listener.getBindAddress()));
                } else if (listener instanceof HttpsListenerEntity httpsListenerEntity) {
                    final ApiTlsEntity tls = httpsListenerEntity.getTls();
                    final KeystoreEntity keystoreEntity = tls.getKeystoreEntity();
                    if (keystoreEntity == null) {
                        log.error("Keystore can not be emtpy for HTTPS listener");
                        throw new UnrecoverableException(false);
                    }
                    listenersBld.add(new HttpsListener(
                            listener.getPort(),
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
        apiCfgService.setPreLoginNotice(
                new PreLoginNotice(pln.isEnabled(), pln.getTitle(), pln.getMessage(), pln.getConsent()));

        return ConfigResult.SUCCESS;
    }
}
