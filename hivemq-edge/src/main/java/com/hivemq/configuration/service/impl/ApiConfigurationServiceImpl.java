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

import com.hivemq.api.config.ApiJwtConfiguration;
import com.hivemq.api.config.ApiListener;
import com.hivemq.api.config.ApiStaticResourcePath;
import com.hivemq.configuration.service.ApiConfigurationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.hivemq.http.core.UsernamePasswordRoles;

import jakarta.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Simon L Johnson
 */
@Singleton
public class ApiConfigurationServiceImpl implements ApiConfigurationService {

    private boolean enabled = true;
    private @NotNull List<UsernamePasswordRoles> userList = new CopyOnWriteArrayList<>();
    private @NotNull List<ApiListener> listeners = new CopyOnWriteArrayList<>();
    private @Nullable ApiJwtConfiguration apiJwtConfiguration;
    private @Nullable String proxyContextPath;

    @Override
    public @NotNull List<ApiListener> getListeners() {
        return listeners;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public @Nullable ApiJwtConfiguration getApiJwtConfiguration() {
        return apiJwtConfiguration;
    }

    @Override
    public @NotNull List<UsernamePasswordRoles> getUserList() {
        return userList;
    }


    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public void setUserList(final @NotNull List<UsernamePasswordRoles> userList) {
        this.userList = userList;
    }

    public void setListeners(final @NotNull List<ApiListener> listeners) {
        this.listeners = listeners;
    }

    public void setApiJwtConfiguration(final @NotNull ApiJwtConfiguration apiJwtConfiguration) {
        this.apiJwtConfiguration = apiJwtConfiguration;
    }

    @Override
    public @Nullable Optional<String> getProxyContextPath() {
        if( proxyContextPath == null || proxyContextPath.isEmpty()) {
            return Optional.empty();
        }
        else {
            String normalizedPath = proxyContextPath;
            if (!normalizedPath.startsWith("/")) {
                normalizedPath = "/" + normalizedPath;
            }
            if (normalizedPath.endsWith("/")) {
                normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
            }
            return Optional.of(normalizedPath);
        }
    }

    @Override
    public void setProxyContextPath(final @Nullable String proxyContextPath) {
        this.proxyContextPath = proxyContextPath;
    }
}
