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
package com.hivemq.api.filter;

import com.hivemq.api.auth.ApiPrincipal;
import com.hivemq.api.auth.handler.impl.BearerTokenAuthenticationHandler;
import com.hivemq.api.auth.provider.ITokenGenerator;
import com.hivemq.api.auth.provider.ITokenVerifier;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;

/**
 * @author Simon L Johnson
 */
@Provider
public class JWTReissuanceFilterImpl implements ContainerResponseFilter {

    protected final Logger logger = LoggerFactory.getLogger(JWTReissuanceFilterImpl.class.getName());

    private final @NotNull ITokenGenerator tokenGenerator;
    private final @NotNull ITokenVerifier tokenVerifier;
    private final int MILLIS_BEFORE_EXPIRY_TO_REISSUE = 60 * 5 * 1000;

    public JWTReissuanceFilterImpl(final @NotNull ITokenGenerator tokenGenerator, final @NotNull ITokenVerifier tokenVerifier) {
        this.tokenGenerator = tokenGenerator;
        this.tokenVerifier = tokenVerifier;
    }

    @Override
    public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext)
            throws IOException {
        try {
            String token;
            if((token = (String) requestContext.getProperty(BearerTokenAuthenticationHandler.TOKEN)) != null){
                Optional<Long> millis = tokenVerifier.getExpiryTimeMillis(token);
                long current = System.currentTimeMillis();
                if(millis.isPresent()){
                    long expires = millis.get();
                    if(expires > current){
                        if(current >= (expires - MILLIS_BEFORE_EXPIRY_TO_REISSUE)){
                            String newToken =
                                    tokenGenerator.generateToken((ApiPrincipal) requestContext.getSecurityContext().getUserPrincipal());
                            responseContext.getHeaders().add(BearerTokenAuthenticationHandler.REISSUE, newToken);
                        }
                    }
                }
            }
        } catch(Exception e){
            logger.warn("Error Reissuing JWT Token in Response Header", e);
        }
    }
}
