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
import com.hivemq.configuration.service.ApiConfigurationService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Priority;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
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
    private final ApiConfigurationService apiConfigurationService;

    //TODO(mschoenert) I'm not sure that this is the best place to inject the ApiConfigurationService
    // especially since this we must also inject it in ApiResourceRegistry (where it is registered)
    // but I'm also not sure what would be a better way to have access to the isEnforceApiAuth flag
    @Inject
    public ApiAuthenticationFeature(final @NotNull Set<IAuthenticationHandler> authenticationHandler,
            final @NotNull ApiConfigurationService apiConfigurationService) {
        this.authenticationHandler = authenticationHandler;
        this.apiConfigurationService = apiConfigurationService;
    }

    @Override
    public void configure(final @NotNull ResourceInfo resourceInfo, final @NotNull FeatureContext context) {

        final Method resourceMethod = resourceInfo.getResourceMethod();
        //-- Check if roles are defined on the resource method (these are the ones that are the most specific)
        Optional<RolesAllowed>
                requiredRoles = ApiPermissionUtils.getAnnotationIfExists(RolesAllowed.class, resourceMethod);
        if(requiredRoles.isPresent() && !Set.of(requiredRoles.get().value()).contains("NO_AUTH_REQUIRED")){
            final AuthenticationFilter filter = new AuthenticationFilter(Set.of(requiredRoles.get().value()));
            context.register(filter);
            return;
        } else {
            //-- Check if roles are defined on the resource class
            requiredRoles = ApiPermissionUtils.getAnnotationIfExists(RolesAllowed.class, resourceInfo.getResourceClass());
            if(requiredRoles.isPresent() && !Set.of(requiredRoles.get().value()).contains("NO_AUTH_REQUIRED")){
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

            // shortcut if authentication and authorization is NOT enforced (compatibility behavior)
            if (!apiConfigurationService.isEnforceApiAuth()) {
                return;
            }

            try {

                // first check the authorization and set the security context if authentication was successful
                ChainedAuthenticationHandler handler = new ChainedAuthenticationHandler(authenticationHandler);
                AuthenticationResult result = handler.authenticate(requestContext);
                if(!result.isSuccess()){
                    log.debug("Authentication failed for resource {}", requestContext.getUriInfo().getPath());
                    // a bit counterintuitive but HTTP response codes specify UNAUTHORIZED(401) if authentication fails
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

                // next check the authorization
                if(!requiredPermissions.isEmpty()){
                    if(!handler.authorized(requestContext, requiredPermissions)){
                        log.warn("Not authorized to access resource {} -> {}", requestContext.getUriInfo().getPath(), requestContext.getSecurityContext());
                        // a bit counterintuitive but HTTP response codes specify FORBIDDEN(403) if authorization fails
                        Response.ResponseBuilder builder = Response.status(Response.Status.FORBIDDEN);
                        handler.decorateResponse(result, builder);
                        requestContext.abortWith(builder.build());
                    } else {
                        if(log.isTraceEnabled()){
                            log.trace("Request authorized {} -> {}", requestContext.getUriInfo().getPath(), requestContext.getSecurityContext());
                        }
                    }
                }

                // if the request was not aborted above, that means authentication and authorization were successful

            } catch (final Exception e) {
                log.error("REST API authentication failed, reason: {}", e.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("original exception", e);
                }
            }
        }
    }
}

