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
package com.hivemq.protocols.v2.manager;

import com.hivemq.adapter.sdk.api.v2.factories.ProtocolAdapterFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * The set of protocol-adapter type factories the manager can instantiate, keyed by {@code protocol-id} (design §8,
 * D8). The factory set is <b>constructor-injected</b> and is <b>empty in production wiring</b> — no real adapter is
 * ported in this project; the core-module tests inject the {@code ChaosProtocolAdapterFactory} (a later task), and
 * the manager tests inject a minimal test factory. An adapter whose {@code protocol-id} has no factory here is
 * surfaced as an {@code ERROR} registry handle with no wrapper created (design §8.2).
 */
public final class ProtocolAdapterFactoryRegistry {

    private final @NotNull Map<String, ProtocolAdapterFactory> factoryMap;

    /**
     * @param factorySet the protocol-adapter factories available to this Edge instance; empty in production.
     * @throws IllegalArgumentException if two factories declare the same {@code protocol-id}.
     */
    public ProtocolAdapterFactoryRegistry(final @NotNull Set<ProtocolAdapterFactory> factorySet) {
        final Map<String, ProtocolAdapterFactory> map = new LinkedHashMap<>();
        for (final ProtocolAdapterFactory factory : factorySet) {
            final String protocolId = factory.information().protocolId();
            final ProtocolAdapterFactory duplicatedFactory = map.put(protocolId, factory);
            if (duplicatedFactory != null) {
                throw new IllegalArgumentException(
                        "two protocol adapter factories declare protocol-id [" + protocolId + "]");
            }
        }
        this.factoryMap = Map.copyOf(map);
    }

    /**
     * @param protocolId the protocol id to look up.
     * @return the factory for the type, or empty if no factory declares that {@code protocol-id}.
     */
    public @NotNull Optional<ProtocolAdapterFactory> findByProtocolId(final @NotNull String protocolId) {
        return Optional.ofNullable(factoryMap.get(protocolId));
    }
}
