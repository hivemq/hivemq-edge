package com.hivemq.edge.adapters.opcua.config;

import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum SecPolicy {
    NONE(1, SecurityPolicy.None),
    BASIC128RSA15(2, SecurityPolicy.Basic128Rsa15), //deprecated in spec, but may still be in use
    BASIC256(3, SecurityPolicy.Basic256), //deprecated in spec, but may still be in use
    BASIC256SHA256(4, SecurityPolicy.Basic256Sha256),
    AES128_SHA256_RSAOAEP(5, SecurityPolicy.Aes128_Sha256_RsaOaep),
    AES256_SHA256_RSAPSS(6, SecurityPolicy.Aes256_Sha256_RsaPss);

    public static final SecPolicy DEFAULT = NONE;

    //higher is better
    private final int priority;
    private final @NotNull SecurityPolicy securityPolicy;

    SecPolicy(final int priority, final @NotNull SecurityPolicy securityPolicy) {
        this.priority = priority;
        this.securityPolicy = securityPolicy;
    }

    public static @Nullable SecPolicy forUri(final @NotNull String securityPolicyUri) {
        for (final SecPolicy value : values()) {
            if (value.getSecurityPolicy().getUri().equals(securityPolicyUri)) {
                return value;
            }
        }
        return null;
    }

    public int getPriority() {
        return priority;
    }

    public @NotNull SecurityPolicy getSecurityPolicy() {
        return securityPolicy;
    }
}
