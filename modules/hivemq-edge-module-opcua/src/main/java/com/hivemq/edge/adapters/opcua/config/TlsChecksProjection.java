/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua.config;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Projects a {@link Tls} configuration onto the six validation axes.
 *
 * <p>This is a pure function: it reads the configuration and returns the flags that drive the checks.
 * It performs no I/O, logs nothing, and — crucially — mutates nothing. The configuration keeps exactly
 * what the operator wrote, which is what makes a writeback of an unchanged configuration a no-op.
 *
 * <p>The preset table below is the single source of truth for what each named profile means.
 */
public final class TlsChecksProjection {

    /** The preset applied when neither {@code tlsChecks} nor {@code tlsChecksFull} is configured. */
    public static final @NotNull TlsChecks DEFAULT_PRESET = TlsChecks.STANDARD;

    private TlsChecksProjection() {}

    /**
     * Resolves the effective checks for the given TLS configuration.
     *
     * @throws InvalidTlsChecksConfigException if the configuration is contradictory or cannot be
     *     honoured. The message is operator-facing and names the resolution.
     */
    public static @NotNull EffectiveChecks project(final @NotNull Tls tls) throws InvalidTlsChecksConfigException {
        final TlsChecks preset = tls.tlsChecks();
        final TlsChecksFull axes = tls.tlsChecksFull();

        if (preset != null && axes != null) {
            throw new InvalidTlsChecksConfigException("Both 'tlsChecks' (preset " + preset
                    + ") and 'tlsChecksFull' (axes) are configured; they are mutually exclusive. "
                    + "Keep the preset for a named profile, or keep tlsChecksFull for full control, but not both.");
        }

        final EffectiveChecks checks =
                axes != null ? fromAxes(axes) : fromPreset(preset == null ? DEFAULT_PRESET : preset);

        validate(checks, tls);
        return checks;
    }

    /**
     * The preset table. Each preset is an exact combination of the six axes.
     *
     * <p>{@code STANDARD}, {@code ALL}, {@code APPLICATION_URI} and {@code NONE} reproduce, bit for
     * bit, the check sets those values selected before the axes existed. Verified against the Milo
     * check sets in the released implementation; changing a row here is a behavioural change for
     * existing deployments.
     */
    public static @NotNull EffectiveChecks fromPreset(final @NotNull TlsChecks preset) {
        return switch (preset) {
            // chain only - no optional checks at all
            case NONE ->
                new EffectiveChecks(
                        TrustMode.CHAIN,
                        SanUriCheck.NONE,
                        HostnameCheck.NONE,
                        ValidityCheck.NONE,
                        RevocationCheck.NONE,
                        KeyUsageCheck.NONE);
            // chain + ApplicationUri
            case APPLICATION_URI ->
                new EffectiveChecks(
                        TrustMode.CHAIN,
                        SanUriCheck.APPLICATION_URI,
                        HostnameCheck.NONE,
                        ValidityCheck.NONE,
                        RevocationCheck.NONE,
                        KeyUsageCheck.NONE);
            // chain + ApplicationUri + validity + revocation (incl. CRLs)
            case STANDARD ->
                new EffectiveChecks(
                        TrustMode.CHAIN,
                        SanUriCheck.APPLICATION_URI,
                        HostnameCheck.NONE,
                        ValidityCheck.NOT_BEFORE_OR_AFTER,
                        RevocationCheck.REQUIRE_CRLS,
                        KeyUsageCheck.NONE);
            // everything Milo can check
            case ALL ->
                new EffectiveChecks(
                        TrustMode.CHAIN,
                        SanUriCheck.APPLICATION_URI,
                        HostnameCheck.HOSTNAME,
                        ValidityCheck.NOT_BEFORE_OR_AFTER,
                        RevocationCheck.REQUIRE_CRLS,
                        KeyUsageCheck.SERVER_AUTH);
            // fingerprint allow-list + full identity + validity; no CA machinery required
            case SELF_SIGNED ->
                new EffectiveChecks(
                        TrustMode.ALLOW_LIST,
                        SanUriCheck.APPLICATION_URI,
                        HostnameCheck.HOSTNAME,
                        ValidityCheck.NOT_BEFORE_OR_AFTER,
                        RevocationCheck.NONE,
                        KeyUsageCheck.NONE);
            // accept anything
            case NO_VERIFICATION ->
                new EffectiveChecks(
                        TrustMode.ANY_CERT,
                        SanUriCheck.NONE,
                        HostnameCheck.NONE,
                        ValidityCheck.NONE,
                        RevocationCheck.NONE,
                        KeyUsageCheck.NONE);
        };
    }

    /**
     * Resolves the axes, filling every omitted axis with its <b>strictest</b> value. A forgotten entry
     * must fail safe: omitting an axis yields more verification, never less.
     */
    public static @NotNull EffectiveChecks fromAxes(final @NotNull TlsChecksFull axes) {
        return new EffectiveChecks(
                Objects.requireNonNullElse(axes.trustMode(), TrustMode.CHAIN),
                Objects.requireNonNullElse(axes.sanUri(), SanUriCheck.APPLICATION_URI),
                Objects.requireNonNullElse(axes.hostname(), HostnameCheck.HOSTNAME),
                Objects.requireNonNullElse(axes.validity(), ValidityCheck.NOT_BEFORE_OR_AFTER),
                Objects.requireNonNullElse(axes.revocation(), RevocationCheck.REQUIRE_CRLS),
                Objects.requireNonNullElse(axes.keyUsage(), KeyUsageCheck.SERVER_AUTH));
    }

    private static void validate(final @NotNull EffectiveChecks checks, final @NotNull Tls tls)
            throws InvalidTlsChecksConfigException {

        // Revocation is only decidable while a certification path is being built: the underlying stack
        // checks revocation as part of path validation, and exposes no standalone revocation check. A
        // configuration asking for it without a chain cannot be honoured, so it is rejected rather
        // than silently ignored - an axis that quietly does nothing is worse than an error.
        if (checks.isRevocation() && !checks.isChainBuilt()) {
            throw new InvalidTlsChecksConfigException(("Revocation checking (revocation=%s) requires a certification "
                            + "path and cannot be performed with trustMode=%s. Set revocation=NONE explicitly, or use "
                            + "trustMode=CHAIN.")
                    .formatted(checks.revocation(), checks.trustMode()));
        }

        if (checks.trustMode() == TrustMode.ALLOW_LIST && !hasAllowListPath(tls.allowList())) {
            throw new InvalidTlsChecksConfigException("Trust mode ALLOW_LIST requires an allow-list of permitted "
                    + "server-certificate fingerprints, but no 'allowList' path is configured. Add "
                    + "<allowList><path>...</path></allowList> to the TLS configuration.");
        }
    }

    private static boolean hasAllowListPath(final @Nullable AllowList allowList) {
        return allowList != null
                && allowList.path() != null
                && !allowList.path().isBlank();
    }

    /** Signals a TLS check configuration that is contradictory or cannot be honoured. */
    public static class InvalidTlsChecksConfigException extends Exception {

        private static final long serialVersionUID = 1L;

        private final @NotNull String reason;

        public InvalidTlsChecksConfigException(final @NotNull String message) {
            super(message);
            this.reason = message;
        }

        /** The operator-facing explanation; never {@code null}, unlike {@link #getMessage()}. */
        public @NotNull String reason() {
            return reason;
        }
    }
}
