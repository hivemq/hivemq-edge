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
package com.hivemq.api.auth;

import com.google.common.base.Preconditions;
import com.hivemq.api.auth.handler.AuthenticationResult;
import com.hivemq.api.auth.handler.IAuthenticationHandler;
import com.hivemq.api.auth.handler.impl.ChainedAuthenticationHandler;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;

/**
 * A feature that binds in the Auth lifecycle using a dynamic feature. During bootstrap the Resources
 * that are marked using the @RolesAllowed or @DenyAll will be processed and assigned a Filter with
 * their permissions that have been extracted. An Authentication handler is then called on these protected
 * resources to establish whether a given RequestContext is authorized.
 *
 * @author Simon L Johnson
 */
public class ApiAuthenticationFeature implements DynamicFeature {

    static final Logger log = LoggerFactory.getLogger(ApiAuthenticationFeature.class);

    private final Set<IAuthenticationHandler> authenticationHandler;

    @Inject
    public ApiAuthenticationFeature(final @NotNull Set<IAuthenticationHandler> authenticationHandler) {
        this.authenticationHandler = authenticationHandler;
    }

    @Override
    public void configure(final @NotNull ResourceInfo resourceInfo, final @NotNull FeatureContext context) {

        final Method resourceMethod = resourceInfo.getResourceMethod();
        //-- Check if roles are defined on the resource method (these are the ones that are the most specific)
        Optional<RolesAllowed>
                requiredRoles = ApiPermissionUtils.getAnnotationIfExists(RolesAllowed.class, resourceMethod);
        if(requiredRoles.isPresent()){
            final AuthenticationFilter filter = new AuthenticationFilter(Set.of(requiredRoles.get().value()));
            context.register(filter);
            return;
        } else {
            //-- Check if roles are defined on the resource class
            requiredRoles = ApiPermissionUtils.getAnnotationIfExists(RolesAllowed.class, resourceInfo.getResourceClass());
            if(requiredRoles.isPresent()){
                final AuthenticationFilter filter = new AuthenticationFilter(Set.of(requiredRoles.get().value()));
                context.register(filter);
                return;
            }
        }

        if (ApiPermissionUtils.isAnnotationPresent(PermitAll.class, resourceInfo.getResourceClass())) {
            final AuthenticationFilter filter = new AuthenticationFilter(Set.of());
            context.register(filter);
        }
    }

    protected ApiSecurityContext createSecurityContext(final @NotNull AuthenticationResult result){
        ApiSecurityContext context = new ApiSecurityContext(result.getPrincipal(), result.getAuthenticationMethod(), true);
        return context;
    }

    /**
     * The presence of the AuthenticationFilter implies that the context MUST be secured.. the RolesAllows and PermitAll determine
     * which resources are protected
     */
    @Provider
    @Priority(Priorities.AUTHENTICATION)
    public class AuthenticationFilter implements ContainerRequestFilter {
        private final @NotNull Set<String> requiredPermissions;

        public AuthenticationFilter(final @NotNull Set<String> requiredPermissions) {
            Preconditions.checkNotNull(requiredPermissions);
            this.requiredPermissions = requiredPermissions;
        }

        @Override
        public void filter(final @NotNull ContainerRequestContext requestContext) throws IOException {
            Preconditions.checkNotNull(requestContext);
            try {
                ChainedAuthenticationHandler handler = new ChainedAuthenticationHandler(authenticationHandler);
                AuthenticationResult result = handler.authenticate(requestContext);
                if(!result.isSuccess()){
                    log.debug("Authentication failed for resource {}", requestContext.getUriInfo().getPath());
                    Response.ResponseBuilder builder = Response.status(Response.Status.UNAUTHORIZED);
                    handler.decorateResponse(result, builder);
                    requestContext.abortWith(builder.build());
                    return;
                } else {
                    SecurityContext context = createSecurityContext(result);
                    requestContext.setSecurityContext(context);
                    if(log.isTraceEnabled()){
                        log.trace("Request authenticated {} -> {}", requestContext.getUriInfo().getPath(), requestContext.getSecurityContext());
                    }
                }

                if(!requiredPermissions.isEmpty() && !handler.authorized(requestContext, requiredPermissions)){
                    log.warn("Not authorized to access resource {} -> {}", requestContext.getUriInfo().getPath(), requestContext.getSecurityContext());
                    Response.ResponseBuilder builder = Response.status(Response.Status.FORBIDDEN);
                    handler.decorateResponse(result, builder);
                    requestContext.abortWith(builder.build());
                }
            } catch (final Exception e) {
                log.error("REST API authentication failed, reason: {}", e.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("original exception", e);
                }
            }
        }
    }
}

