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
package com.hivemq.configuration.service;

import com.hivemq.api.auth.provider.impl.ldap.LdapConnectionProperties;
import com.hivemq.api.config.ApiJwtConfiguration;
import com.hivemq.api.config.ApiListener;
import com.hivemq.api.config.ApiStaticResourcePath;
import com.hivemq.api.model.components.PreLoginNotice;
import com.hivemq.http.core.UsernamePasswordRoles;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A Configuration service which allows access to API Configuration properties
 */
public interface ApiConfigurationService {

    @NotNull
    List<ApiListener> getListeners();

    void setListeners(final @NotNull List<ApiListener> listeners);

    boolean isEnabled();

    void setEnabled(boolean enabled);

    @NotNull
    List<ApiStaticResourcePath> getResourcePaths();

    void setResourcePaths(final @NotNull List<ApiStaticResourcePath> resourcePaths);

    @Nullable
    ApiJwtConfiguration getApiJwtConfiguration();

    void setApiJwtConfiguration(final @NotNull ApiJwtConfiguration apiJwtConfiguration);

    @NotNull
    List<UsernamePasswordRoles> getUserList();

    void setUserList(final @NotNull List<UsernamePasswordRoles> userList);

    void setLdapConnectionProperties(final @NotNull LdapConnectionProperties connectionProperties);

    @Nullable
    LdapConnectionProperties getLdapConnectionProperties();

    @NotNull
    PreLoginNotice getPreLoginNotice();

    void setPreLoginNotice(final @NotNull PreLoginNotice preLoginNotice);

    boolean isEnforceApiAuth();

    void setEnforceApiAuth(boolean enforceApiAuth);
}
