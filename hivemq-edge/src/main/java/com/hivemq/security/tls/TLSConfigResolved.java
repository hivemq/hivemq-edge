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
package com.hivemq.security.tls;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/// A configuration whose references to declared material have been replaced by the material itself.
///
/// The third stage: it takes what a surface's own configuration resolved to and the material a
/// deployment declared, and produces a configuration naming files directly - as if the operator had
/// written the material where it is used. Nothing downstream needs to know a reference was involved.
///
/// A pure function on its two inputs. It reads the material, holds no mutable state and opens no
/// file; reading the files is the stage after this one.
///
/// Resolving happens here rather than where the files are read because a reference resolved in two
/// places is a reference that can be resolved two ways. [TLSPolicy] therefore refuses a reference
/// that reaches it, rather than resolving one itself.
public final class TLSConfigResolved implements TLSConfig {

    private final @NotNull TLSConfig written;
    private final @NotNull Optional<MaterialRef> identity;
    private final @NotNull Optional<MaterialRef> truststore;
    private final @NotNull Optional<MaterialRef> allowList;
    private final @NotNull Optional<MaterialRef> revocationList;

    /// Resolves every reference the given configuration carries.
    ///
    /// @throws TLSMaterialRegistry.InvalidReferenceException if a reference names material that is
    ///     not declared, or material of another kind
    public TLSConfigResolved(final @NotNull TLSConfig written, final @NotNull TLSMaterialRegistry registry)
            throws TLSMaterialRegistry.InvalidReferenceException {
        this.written = written;
        this.identity = resolve(written.identity(), registry, TLSMaterialRegistry.Kind.IDENTITY);
        this.truststore = resolve(written.truststore(), registry, TLSMaterialRegistry.Kind.TRUSTSTORE);
        this.allowList = resolve(written.allowList(), registry, TLSMaterialRegistry.Kind.ALLOW_LIST);
        this.revocationList = resolve(written.revocationList(), registry, TLSMaterialRegistry.Kind.REVOCATION_LIST);
    }

    /// Every reference is resolved, including one under a trust mode that will never read it.
    ///
    /// A reference that names nothing is a mistake whether or not the material would have been
    /// opened, and an operator who changes the trust mode later should not discover it then.
    private static @NotNull Optional<MaterialRef> resolve(
            final @NotNull Optional<MaterialRef> ref,
            final @NotNull TLSMaterialRegistry registry,
            final @NotNull TLSMaterialRegistry.Kind kind)
            throws TLSMaterialRegistry.InvalidReferenceException {
        if (ref.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(registry.resolve(ref.get(), kind));
    }

    // --- the material, resolved -------------------------------------------------------------------

    @Override
    public @NotNull Optional<MaterialRef> identity() {
        return identity;
    }

    @Override
    public @NotNull Optional<MaterialRef> truststore() {
        return truststore;
    }

    @Override
    public @NotNull Optional<MaterialRef> allowList() {
        return allowList;
    }

    @Override
    public @NotNull Optional<MaterialRef> revocationList() {
        return revocationList;
    }

    // --- everything else, unchanged ---------------------------------------------------------------
    // Resolving a reference says nothing about which checks apply, so these pass straight through.

    @Override
    public boolean isEnabled() {
        return written.isEnabled();
    }

    @Override
    public @NotNull TLSConstants.TrustMode trustMode() {
        return written.trustMode();
    }

    @Override
    public @NotNull TLSConstants.SanUriCheck sanUri() {
        return written.sanUri();
    }

    @Override
    public @NotNull TLSConstants.HostnameCheck hostname() {
        return written.hostname();
    }

    @Override
    public @NotNull TLSConstants.ValidityCheck validity() {
        return written.validity();
    }

    @Override
    public @NotNull TLSConstants.RevocationCheck revocation() {
        return written.revocation();
    }

    @Override
    public @NotNull TLSConstants.KeyUsageCheck keyUsage() {
        return written.keyUsage();
    }

    @Override
    public @NotNull List<String> protocols() {
        return written.protocols();
    }

    @Override
    public @NotNull List<String> cipherSuites() {
        return written.cipherSuites();
    }

    @Override
    public @NotNull Duration handshakeTimeout() {
        return written.handshakeTimeout();
    }

    @Override
    public @NotNull String toString() {
        return "TLSConfigResolved[" + written + "]";
    }
}
