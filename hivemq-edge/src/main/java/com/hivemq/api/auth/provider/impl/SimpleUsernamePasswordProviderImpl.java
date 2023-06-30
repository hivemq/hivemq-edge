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
package com.hivemq.api.auth.provider.impl;

import com.google.common.base.Preconditions;
import com.hivemq.api.auth.provider.IUsernamePasswordProvider;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.http.core.UsernamePasswordRoles;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Simon L Johnson
 */
public class SimpleUsernamePasswordProviderImpl implements IUsernamePasswordProvider {

    private final @NotNull Map<String, UsernamePasswordRoles> usernamePasswordMap;

    public SimpleUsernamePasswordProviderImpl() {
        this.usernamePasswordMap = Collections.synchronizedMap(new HashMap<>());
    }

    public SimpleUsernamePasswordProviderImpl add(final @NotNull UsernamePasswordRoles usernamePassword){
        Preconditions.checkNotNull(usernamePassword);
        Preconditions.checkArgument(usernamePassword.getUserName() != null, "Username must not be <null>");
        usernamePasswordMap.put(usernamePassword.getUserName(), usernamePassword);
        return this;
    }

    public SimpleUsernamePasswordProviderImpl remove(final @NotNull String userName){
        Preconditions.checkNotNull(userName);
        usernamePasswordMap.remove(userName);
        return this;
    }

    @Override
    public Optional<UsernamePasswordRoles> findByUsername(final @NotNull String userName) {
        Preconditions.checkNotNull(userName);
        UsernamePasswordRoles up = usernamePasswordMap.get(userName);
        return Optional.ofNullable(up);
    }

    public static SimpleUsernamePasswordProviderImpl fromList(List<UsernamePasswordRoles> userList){
        SimpleUsernamePasswordProviderImpl simpleUsernamePasswordProvider = new SimpleUsernamePasswordProviderImpl();
        userList.stream().forEach(simpleUsernamePasswordProvider::add);
        return simpleUsernamePasswordProvider;
    }
}
