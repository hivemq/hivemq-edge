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
import com.hivemq.api.resources.HealthCheckApi;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;

/**
 * Simple health check response that returns status code 200.
 */
@Singleton
public class HealthCheckResourceImpl extends AbstractApi implements HealthCheckApi {

    @Inject
    public HealthCheckResourceImpl() {
    }

    @Override
    public Response liveness() {
        HealthStatus status = new HealthStatus();
        status.status = "UP";
        return Response.status(200).entity(status).build();
    }

    @Override
    public Response readiness() {
        HealthStatus status = new HealthStatus();
        status.status = "UP";
        return Response.status(200).entity(status).build();
    }


    public static class HealthStatus {

        public String status;

        public HealthStatus() {
        }

        public String getStatus() {
            return status;
        }
    }
}
