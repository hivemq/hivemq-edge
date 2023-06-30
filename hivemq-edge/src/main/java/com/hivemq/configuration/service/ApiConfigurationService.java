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

import com.hivemq.api.config.ApiJwtConfiguration;
import com.hivemq.api.config.ApiListener;
import com.hivemq.api.config.ApiStaticResourcePath;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.http.core.UsernamePasswordRoles;

import java.util.List;

/**
 * A Configuration service which allows access to API Configuration properties
 */
public interface ApiConfigurationService {

    @NotNull List<ApiListener> getListeners();

    boolean isEnabled();

    @NotNull List<ApiStaticResourcePath> getResourcePaths();

    @Nullable ApiJwtConfiguration getApiJwtConfiguration();

    @NotNull List<UsernamePasswordRoles> getUserList();

    void setEnabled(boolean enabled);

    void setResourcePaths(@NotNull List<ApiStaticResourcePath> resourcePaths);

    void setUserList(@NotNull List<UsernamePasswordRoles> userList);

    void setListeners(@NotNull List<ApiListener> listeners);

    void setApiJwtConfiguration(@NotNull ApiJwtConfiguration apiJwtConfiguration);

}
