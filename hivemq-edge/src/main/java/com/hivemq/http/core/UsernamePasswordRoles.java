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
package com.hivemq.http.core;

import com.hivemq.api.auth.ApiPrincipal;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.Set;

public class UsernamePasswordRoles {

    public static final String DEFAULT_USERNAME = "admin";
    public static final String DEFAULT_PASSWORD = "hivemq";

    private String userName;
    private String password;
    private String realm;
    private Set<String> roles = Set.of();

    public UsernamePasswordRoles() {
    }


    public UsernamePasswordRoles(final @NotNull String userName, final @NotNull String password, Set<String> roles) {
        this();
        this.userName = userName;
        this.password = password;
        this.roles = roles;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public ApiPrincipal toPrincipal(){
        //decouple the password from the principal for the API
        return new ApiPrincipal(getUserName(), Set.copyOf(getRoles()));
    }
}
