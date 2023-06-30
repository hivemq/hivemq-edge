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

import com.hivemq.api.auth.ApiRoles;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Simon L Johnson
 */
@Path("/test/permitall")
@PermitAll
public class TestPermitAllApiResource {

    @GET
    @Path("/get")
    public Response testGet() {
        return Response.ok().build();
    }

    @GET
    @Path("/get/adminonly")
    @RolesAllowed(ApiRoles.ADMIN)
    public Response testGetAdminOnly() {
        return Response.ok().build();
    }
}
