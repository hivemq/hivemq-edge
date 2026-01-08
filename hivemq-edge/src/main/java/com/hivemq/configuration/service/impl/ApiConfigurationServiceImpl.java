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
package com.hivemq.configuration.service.impl;

import com.hivemq.api.auth.provider.impl.ldap.LdapConnectionProperties;
import com.hivemq.api.config.ApiJwtConfiguration;
import com.hivemq.api.config.ApiListener;
import com.hivemq.api.config.ApiStaticResourcePath;
import com.hivemq.api.model.components.PreLoginNotice;
import com.hivemq.configuration.service.ApiConfigurationService;
import com.hivemq.http.core.UsernamePasswordRoles;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Singleton
public class ApiConfigurationServiceImpl implements ApiConfigurationService {

    private boolean enabled = true;
    private @NotNull List<ApiStaticResourcePath> resourcePaths = new CopyOnWriteArrayList<>();
    private @NotNull List<UsernamePasswordRoles> userList = new CopyOnWriteArrayList<>();
    private @NotNull List<ApiListener> listeners = new CopyOnWriteArrayList<>();
    private @Nullable ApiJwtConfiguration apiJwtConfiguration;
    private @NotNull PreLoginNotice preLoginNotice = new PreLoginNotice();
    private @Nullable LdapConnectionProperties ldapConnectionProperties;
    private boolean enforceApiAuth = false;

    @Override
    public @NotNull List<ApiListener> getListeners() {
        return listeners;
    }

    public void setListeners(final @NotNull List<ApiListener> listeners) {
        this.listeners = listeners;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public @NotNull List<ApiStaticResourcePath> getResourcePaths() {
        return resourcePaths;
    }

    public void setResourcePaths(final @NotNull List<ApiStaticResourcePath> resourcePaths) {
        this.resourcePaths = resourcePaths;
    }

    @Override
    public @Nullable ApiJwtConfiguration getApiJwtConfiguration() {
        return apiJwtConfiguration;
    }

    public void setApiJwtConfiguration(final @NotNull ApiJwtConfiguration apiJwtConfiguration) {
        this.apiJwtConfiguration = apiJwtConfiguration;
    }

    @Override
    public @NotNull List<UsernamePasswordRoles> getUserList() {
        return userList;
    }

    public void setUserList(final @NotNull List<UsernamePasswordRoles> userList) {
        this.userList = userList;
    }

    @Override
    public void setLdapConnectionProperties(final @NotNull LdapConnectionProperties connectionProperties) {
        this.ldapConnectionProperties = connectionProperties;
    }

    @Override
    public @Nullable LdapConnectionProperties getLdapConnectionProperties() {
        return ldapConnectionProperties;
    }

    @Override
    public @NotNull PreLoginNotice getPreLoginNotice() {
        return preLoginNotice;
    }

    @Override
    public void setPreLoginNotice(final @NotNull PreLoginNotice preLoginNotice) {
        this.preLoginNotice = preLoginNotice;
    }

    @Override
    public boolean isEnforceApiAuth() {
        return enforceApiAuth;
    }

    @Override
    public void setEnforceApiAuth(final boolean enforceApiAuth) {
        this.enforceApiAuth = enforceApiAuth;
    }
}
