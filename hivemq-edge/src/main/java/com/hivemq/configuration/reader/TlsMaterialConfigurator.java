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
package com.hivemq.configuration.reader;

import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.tls.TlsMaterialEntity;
import com.hivemq.security.tls.TLSMaterialProvider;
import com.hivemq.security.tls.TLSMaterialRegistry;
import jakarta.inject.Inject;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Puts the declared TLS material where a surface resolving a reference can find it.
///
/// Runs on every configuration load, so renaming or adding declared material does not need a
/// restart - a surface picks up the change when it next starts, which is the same moment it would
/// pick up a change to the material written in its own configuration.
public class TlsMaterialConfigurator implements Configurator<List<TlsMaterialEntity>> {

    private static final @NotNull Logger log = LoggerFactory.getLogger(TlsMaterialConfigurator.class);

    private final @NotNull TLSMaterialProvider provider;

    @Inject
    public TlsMaterialConfigurator(final @NotNull TLSMaterialProvider provider) {
        this.provider = provider;
    }

    /// Never, whatever changed. A surface reads its material when it starts, so declared material
    /// that changed reaches it the next time it starts; demanding a node restart would be a heavier
    /// answer than this configuration needs.
    @Override
    public boolean needsRestartWithConfig(final @NotNull HiveMQConfigEntity config) {
        return false;
    }

    @Override
    public @NotNull ConfigResult applyConfig(final @NotNull HiveMQConfigEntity config) {
        final List<TlsMaterialEntity> declared = config.getTlsMaterials();
        try {
            provider.replaceWith(TlsMaterialRegistryBuilder.registryOf(declared));
        } catch (final TLSMaterialRegistry.InvalidReferenceException e) {
            // The declaration cannot be indexed, so no reference could be resolved against it
            // unambiguously. Reported as an error rather than applied in part: a registry missing
            // whichever entry collided would attach a surface to material its operator never chose.
            //
            // The previously declared material is left in place. A surface already running is
            // unaffected either way, and one that starts next resolves against the last declaration
            // that made sense rather than against nothing - which would refuse every reference and
            // turn one bad declaration into every surface failing to start.
            log.error("Unable to read the declared TLS material: {}", e.reason());
            return ConfigResult.ERROR;
        }
        if (!declared.isEmpty() && log.isDebugEnabled()) {
            log.debug("Read {} tls-material declaration(s).", declared.size());
        }
        return ConfigResult.SUCCESS;
    }
}
