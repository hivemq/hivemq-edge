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

import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

/// The TLS material this node currently declares, for whatever resolves a reference against it.
///
/// One instance per node, holding the registry built from the configuration. A surface asks for the
/// registry when it starts, which is what makes declared material reach an adapter that was
/// constructed long before the material was read.
///
/// Mutable on purpose, and the one mutable thing in this package. The configuration is re-read while
/// Edge runs, so what a reference resolves against changes; everything downstream of
/// [#registry()] is immutable, and a surface that already started keeps the registry it resolved
/// against until it is restarted.
@Singleton
public class TLSMaterialProvider {

    /// Empty until the configuration has been read, so a surface starting before then resolves no
    /// reference rather than seeing a half-built registry. Volatile because the configuration is
    /// re-read on a watcher thread while adapters start on others; a registry is immutable once
    /// built, so publishing the new one is the whole of the synchronisation needed.
    private volatile @NotNull TLSMaterialRegistry registry = TLSMaterialRegistry.empty();

    /// Not injectable: the node's one instance is the configuration service's, bound where the
    /// configuration module provides it, so no injection point can be handed an empty one.
    public TLSMaterialProvider() {}

    /// The material currently declared. Never null: a deployment that declares none gets an empty
    /// registry, against which every reference is refused for naming nothing.
    public @NotNull TLSMaterialRegistry registry() {
        return registry;
    }

    /// Replaces the declared material, which the configuration reader calls on every load.
    ///
    /// Surfaces already running are not revisited. A surface holds the material it resolved and read
    /// at start, so changing a declaration takes effect when that surface next starts - the same as
    /// for the material written in a surface's own configuration.
    public void replaceWith(final @NotNull TLSMaterialRegistry registry) {
        this.registry = registry;
    }
}
