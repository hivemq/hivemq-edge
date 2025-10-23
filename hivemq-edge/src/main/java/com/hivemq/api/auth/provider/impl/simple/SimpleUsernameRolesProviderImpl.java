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
package com.hivemq.api.auth.provider.impl.simple;

import com.hivemq.api.auth.provider.IUsernameRolesProvider;
import com.hivemq.http.core.UsernamePasswordRoles;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Simon L Johnson
 */
public class SimpleUsernameRolesProviderImpl implements IUsernameRolesProvider {

    private final @NotNull Map<String, UsernamePasswordRoles> usernamePasswordMap;

    public SimpleUsernameRolesProviderImpl() {
        this.usernamePasswordMap = new ConcurrentHashMap<>();
    }

    public SimpleUsernameRolesProviderImpl add(final @NotNull UsernamePasswordRoles usernamePassword){
        checkNotNull(usernamePassword);
        checkArgument(usernamePassword.getUserName() != null && !usernamePassword.getUserName().isBlank(), "Username must not be <null>");
        usernamePasswordMap.put(usernamePassword.getUserName(), usernamePassword);
        return this;
    }

    public SimpleUsernameRolesProviderImpl remove(final @NotNull String userName){
        checkNotNull(userName);
        usernamePasswordMap.remove(userName);
        return this;
    }

    @Override
    public Optional<UsernameRoles> findByUsernameAndPassword(final @NotNull String userName, final byte @NotNull [] password) {
        checkNotNull(userName);
        return Optional
                .ofNullable(usernamePasswordMap.get(userName))
                .map(user -> {
                    if(!Arrays.equals(user.getPassword(), password)) {
                        return null;
                    } else {
                        return new UsernameRoles(user.getUserName(), Set.copyOf(user.getRoles()));
                    }
                });
    }
}
