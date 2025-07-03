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
package com.hivemq.api.resources.impl;

import com.hivemq.configuration.service.ApiConfigurationService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;
import java.net.URI;

@Path("/")
@Singleton
public class RootResource {

    private final @NotNull ApiConfigurationService apiConfigurationService;

    @Inject
    public RootResource(final @NotNull ApiConfigurationService apiConfigurationService) {
        this.apiConfigurationService = apiConfigurationService;
    }

    @GET
    public Response getRoot() {

        final String redirectPath = apiConfigurationService
                .getProxyContextPath()
                .map(path -> path + "/app/")
                .orElse("/app/");
        
        return Response.temporaryRedirect(URI.create(redirectPath)).build();
    }

}
