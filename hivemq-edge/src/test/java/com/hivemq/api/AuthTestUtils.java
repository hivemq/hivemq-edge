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
package com.hivemq.api;

import com.hivemq.api.auth.provider.IUsernameRolesProvider;
import com.hivemq.api.auth.provider.impl.simple.SimpleUsernameRolesProviderImpl;
import com.hivemq.http.core.UsernamePasswordRoles;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static com.hivemq.api.auth.ApiRoles.ADMIN;
import static com.hivemq.api.auth.ApiRoles.USER;

/**
 * @author Simon L Johnson
 */
public class AuthTestUtils {

    public static IUsernameRolesProvider createTestUsernamePasswordProvider(){

        return new SimpleUsernameRolesProviderImpl().
                add(new UsernamePasswordRoles("testadmin", "test".getBytes(StandardCharsets.UTF_8), Set.of(ADMIN))).
                add(new UsernamePasswordRoles("testuser", "test".getBytes(StandardCharsets.UTF_8), Set.of(USER))).
                add(new UsernamePasswordRoles("testnorole", "test".getBytes(StandardCharsets.UTF_8), Set.of()));

    }
}
