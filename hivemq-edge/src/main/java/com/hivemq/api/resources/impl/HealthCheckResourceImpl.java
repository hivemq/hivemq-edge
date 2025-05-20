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


import com.hivemq.api.AbstractApi;
import com.hivemq.edge.api.HealthCheckEndpointApi;
import org.jetbrains.annotations.NotNull;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;

/**
 * Simple health check response that returns status code 200.
 */

@Singleton
public class HealthCheckResourceImpl extends AbstractApi implements HealthCheckEndpointApi {

    @Inject
    public HealthCheckResourceImpl() {
    }

    @Override
    public @NotNull Response liveness() {
        final HealthStatus status = new HealthStatus();
        status.status = "UP";
        return Response.ok(status).build();
    }

    @Override
    public @NotNull Response readiness() {
        final HealthStatus status = new HealthStatus();
        status.status = "UP";
        return Response.ok(status).build();
    }


    public static class HealthStatus {

        public @NotNull String status;

        public HealthStatus() {
        }

        public @NotNull String getStatus() {
            return status;
        }
    }

}
