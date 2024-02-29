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
package com.hivemq.api.auth.jwt;

import com.google.common.base.Preconditions;
import com.hivemq.api.auth.ApiPrincipal;
import com.hivemq.api.auth.AuthenticationException;
import com.hivemq.api.auth.provider.ITokenGenerator;
import com.hivemq.api.auth.provider.ITokenVerifier;
import com.hivemq.api.config.ApiJwtConfiguration;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * JWT token provider. Use the cobnfiguration to change the key size and the configuration of the produced token.
 * NOTE: this produces JWS's NOT JWE's. To produce JWE's you should encrypt the returned JWS from the
 * generate token method.
 *
 * JWS's are sent in encoded clear text and can be seen (they are integrity protected). If you need to pass
 * secret data, you must encrypt the token using JWE to secure it.
 *
 * @author Simon L Johnson
 */
@Singleton
public class JwtAuthenticationProvider implements ITokenGenerator, ITokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationProvider.class);
    private static final String CLAIM_ROLES = "roles";
    private static final String KEY_ID = "00001";
    private final @NotNull ApiJwtConfiguration configuration;
    private volatile RsaJsonWebKey jwtKey;
    private final Object intializationMonitor = new Object();

    @Inject
    public JwtAuthenticationProvider(final @NotNull ApiJwtConfiguration configuration){
        this.configuration = configuration;
        try {
            initializeKey();
        } catch(AuthenticationException e){
            throw new ExceptionInInitializerError(e);
        }
    }

    protected RsaJsonWebKey getJwtKey() {
        if(jwtKey == null){
            //-- Protected against spurious wakeup
            synchronized (intializationMonitor){
                while(jwtKey == null){
                    try {
                        intializationMonitor.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new ExceptionInInitializerError("interrupted obtaining key");
                    }
                }
            }
        }
        return jwtKey;
    }

    private void initializeKey() throws AuthenticationException {
        Thread initThread = new Thread(() -> {
            synchronized (intializationMonitor){
                initializeKeyInternal();
                intializationMonitor.notifyAll();
            }
        });
        initThread.setPriority(Thread.MIN_PRIORITY);
        initThread.start();
    }


    private void initializeKeyInternal() {
        try {
            log.debug("initializing RSA key");
            if(jwtKey == null){
                long start = System.currentTimeMillis();
                RsaJsonWebKey rsaJsonWebKey = RsaJwkGenerator.generateJwk(configuration.getKeySize());
                rsaJsonWebKey.setKeyId(KEY_ID);
                rsaJsonWebKey.setAlgorithm(AlgorithmIdentifiers.RSA_USING_SHA256);
                jwtKey = rsaJsonWebKey;
                log.debug("finished initializing RSA key in {}ms", (System.currentTimeMillis()-start));
            }
        } catch(Exception e){
            log.warn("Error initializing key store", e);
        }
    }

    @Override
    public String generateToken(final @NotNull ApiPrincipal principal) throws AuthenticationException {

        Preconditions.checkNotNull(principal);

        // -- Generate the Token Claims
        JwtClaims claims = createClaims(principal);

        // -- Sign the Claims
        JsonWebSignature jws = signClaims(claims);
        String token;
        try {
            token = jws.getCompactSerialization();
        } catch (JoseException e) {
            log.warn("Error creating token", e);
            throw new AuthenticationException("error creating <token> from JWS", e);
        }

        if(log.isTraceEnabled()){
            log.trace("Generated JWE {} for principal {}", token, principal);
        }
        return token;
    }

    protected JwtClaims createClaims(@NotNull final ApiPrincipal principal){
        Preconditions.checkNotNull(principal);
        JwtClaims claims = new JwtClaims();
        claims.setGeneratedJwtId();
        claims.setIssuedAtToNow();
        claims.setAudience(configuration.getAudience());
        claims.setIssuer(configuration.getIssuer());
        claims.setExpirationTimeMinutesInTheFuture(configuration.getExpiryTimeMinutes());
        claims.setNotBeforeMinutesInThePast(configuration.getTokenEarlyEpochThresholdMinutes());
        claims.setSubject(principal.getName());
        claims.setStringListClaim(CLAIM_ROLES, List.copyOf(principal.getRoles()));
        return claims;
    }

    protected JsonWebSignature signClaims(@NotNull final JwtClaims claims){
        Preconditions.checkNotNull(claims);
        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setKeyIdHeaderValue(KEY_ID);
        jws.setKey(getJwtKey().getPrivateKey());
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
        return jws;
    }

    protected JwtConsumer buildConsumer(){
        return new JwtConsumerBuilder()
                .setRequireExpirationTime()
                .setAllowedClockSkewInSeconds(30)
                .setRequireSubject()
                .setExpectedIssuer(configuration.getIssuer())
                .setExpectedAudience(configuration.getAudience())
                .setVerificationKey(getJwtKey().getKey())
                .setJwsAlgorithmConstraints(
                        AlgorithmConstraints.ConstraintType.PERMIT, AlgorithmIdentifiers.RSA_USING_SHA256)
                .build();
    }

    @Override
    public Optional<ApiPrincipal> verify(final @NotNull String tokenValue) {

        try {
            Preconditions.checkNotNull(tokenValue);
            JwtClaims claims = buildConsumer().processToClaims(tokenValue);
            String subject = claims.getSubject();
            return Optional.of(new ApiPrincipal(subject,
                    Set.copyOf(claims.getStringListClaimValue(CLAIM_ROLES))));
        }
        catch(MalformedClaimException e){
            log.trace("jwt parse failed, reason {}", e.getMessage());
        }
        catch (InvalidJwtException e){
            if (!e.hasExpired()){
                log.debug("jwt validation failed, reason {}", e.getMessage());
            } else {
                log.trace("jwt expired, reason {}", e.getMessage());
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<Long> getExpiryTimeMillis(final @NotNull String tokenValue) {

        try {
            Preconditions.checkNotNull(tokenValue);
            JwtClaims claims = buildConsumer().processToClaims(tokenValue);
            return Optional.of(claims.getExpirationTime().getValueInMillis());
        }
        catch(MalformedClaimException e){
            log.warn("jwt parse failed, reason {}", e.getMessage());
        }
        catch (InvalidJwtException e){
            if (!e.hasExpired()){
                log.trace("jwt validation failed, reason {}", e.getMessage());
            } else {
                log.trace("jwt validation failed, reason {}", e.getMessage());
            }
        }
        return Optional.empty();
    }
}
