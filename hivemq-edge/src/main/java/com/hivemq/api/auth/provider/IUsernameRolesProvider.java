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
package com.hivemq.api.auth.provider;

import com.hivemq.api.auth.ApiPrincipal;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;

public interface IUsernameRolesProvider extends ICredentialsProvider {

    record UsernameRoles(String username, Set<String> roles){
        public ApiPrincipal toPrincipal(){
            //decouple the password from the principal for the API
            return new ApiPrincipal(username(), Set.copyOf(roles()));
        }
    }

    Optional<UsernameRoles> findByUsernameAndPassword(final @NotNull String userName, final byte @NotNull [] password);
}
