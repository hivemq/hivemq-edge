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

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Simon L Johnson
 */
@Path("/test")
public class TestApiResource extends AbstractApi {

    @GET
    @Path("/get")
    public Response testGet() {
        return Response.ok().build();
    }

    @POST
    @Path("/post/entity")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response testPost(TestEntity entity){
        return Response.ok(entity).build();
    }

    @POST
    @Path("/post/formData")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response testFormPost(@FormParam("param1") String value, Form form){
        System.err.println(value);
        return Response.ok(form.asMap()).build();
    }

//    @POST
//    @Path("/post/upload")
//    @Consumes({MediaType.MULTIPART_FORM_DATA})
//    public Response uploadFileWithData(
//            @FormDataParam("file") InputStream fileInputStream,
//            @FormDataParam("file") FormDataContentDisposition cdh,
//            @FormDataParam("emp") TestEntity entity) throws Exception {
//
//        System.out.println(cdh.getName());
//        System.out.println(cdh.getFileName());
//        System.out.println(cdh.getSize());
//        System.out.println(cdh.getType());
//
//        return Response.ok(entity).build();
//    }

    @HEAD
    @Path("/head")
    public Response testHead(){
        return Response.ok().build();
    }

    @PUT
    @Path("/put")
    public Response testPut(TestEntity entity){
        return Response.ok(entity).build();
    }

    @DELETE
    @Path("/delete")
    public Response testDelete(TestEntity entity){
        return Response.ok(entity).build();
    }

    @GET
    @Path("/get/{param}")
    public Response testGetParam(@PathParam("param") String param) {
        return Response.ok(param).build();
    }

    @GET
    @Path("/get/query")
    public Response testGetQuery(@QueryParam("param") String param) {
        return Response.ok(param).build();
    }

    @GET
    @Path("/get/auth/admin")
    @RolesAllowed("ADMIN")
    public Response testGetAdminAuth() {
        return Response.ok().entity(securityContext.getUserPrincipal()).build();
    }

    @GET
    @Path("/get/auth/user")
    @RolesAllowed({"ADMIN", "USER"})
    public Response testGetUserAuth() {
        return Response.ok().entity(securityContext.getUserPrincipal()).build();
    }
}
