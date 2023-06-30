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
package com.hivemq.api.jwt;

import com.hivemq.api.auth.ApiPrincipal;
import com.hivemq.api.auth.ApiRoles;
import com.hivemq.api.auth.AuthenticationException;
import com.hivemq.api.auth.jwt.JwtAuthenticationProvider;
import com.hivemq.api.config.ApiJwtConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;

/**
 * @author Simon L Johnson
 */
public class ApiJwtTests {

    protected final Logger logger = LoggerFactory.getLogger(ApiJwtTests.class);

    @Test
    @Timeout(5)
    public void testCreateJWT() throws AuthenticationException {
        ApiJwtConfiguration configuration = new ApiJwtConfiguration(2048, "Test-Issuer","Test-Audience", 10, 2);
        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(configuration);
        ApiPrincipal principal = new ApiPrincipal("Test-User", Set.of(ApiRoles.ADMIN));
        String token = provider.generateToken(principal);
        Assert.assertNotNull("JWT Token should exist", token);
    }

    @Test
    @Timeout(5)
    public void testVerifyJWT() throws AuthenticationException {
        ApiJwtConfiguration configuration = new ApiJwtConfiguration(2048, "Test-Issuer","Test-Audience", 10, 2);
        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(configuration);
        ApiPrincipal originalPrincipal = new ApiPrincipal("Test-User", Set.of(ApiRoles.ADMIN));
        String token = provider.generateToken(originalPrincipal);
        Assert.assertNotNull("JWT Token should exist", token);
        Optional<ApiPrincipal> verifiedTokenPrincipal = provider.verify(token);
        Assert.assertTrue("Principal should be returned from token", verifiedTokenPrincipal.isPresent());
        Assert.assertEquals("Principal roles should match", verifiedTokenPrincipal.get().getRoles(), originalPrincipal.getRoles());
        Assert.assertEquals("Principal name should match", verifiedTokenPrincipal.get().getName(), originalPrincipal.getName());
    }

    @Test
    @Timeout(5)
    public void testJWTExpiry() throws AuthenticationException {
        ApiJwtConfiguration configuration = new ApiJwtConfiguration(2048, "Test-Issuer","Test-Audience", -2, 1);
        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(configuration);
        ApiPrincipal originalPrincipal = new ApiPrincipal("Test-User", Set.of(ApiRoles.ADMIN));
        String token = provider.generateToken(originalPrincipal);
        Assert.assertNotNull("JWT Token should exist", token);
        Optional<ApiPrincipal> verifiedTokenPrincipal = provider.verify(token);
        Assert.assertFalse("Principal should NOT be returned from token", verifiedTokenPrincipal.isPresent());
    }

}
