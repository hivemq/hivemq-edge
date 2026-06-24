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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * The set of protocol-adapter type factories the manager can instantiate, keyed by {@code protocol-id} (design §8,
 * D8). The factory set is <b>constructor-injected</b> and is <b>empty in production wiring</b> — no real adapter is
 * ported in this project. The {@code ChaosProtocolAdapterFactory} lives in its own {@code hivemq-edge-module-chaos}
 * module and is injected only by {@code hivemq-edge-test} (and the module's own tests), as a <b>hidden</b> type. An
 * adapter whose {@code protocol-id} has no factory here is surfaced as an {@code ERROR} registry handle with no
 * wrapper created (design §8.2).
 * <p>
 * A factory may be registered as <b>listable</b> or <b>hidden</b>. Both are resolvable by {@code protocol-id}
 * through {@link #findByProtocolId(String)} — so a configured instance of a hidden type is created and run exactly
 * like any other — but a hidden type is excluded from {@link #all()}, the source of the v2 {@code GET /types}
 * listing, so it never appears in the frontend's adapter catalog (design §10.1, §11.1).
 */
public final class ProtocolAdapterFactoryRegistry {

    private final @NotNull Map<String, ProtocolAdapterFactory> factoryMap;
    private final @NotNull List<ProtocolAdapterFactory> listableFactories;

    /**
     * @param factorySet the listable protocol-adapter factories available to this Edge instance; empty in
     *                   production.
     * @throws IllegalArgumentException if two factories declare the same {@code protocol-id}.
     */
    public ProtocolAdapterFactoryRegistry(final @NotNull Set<ProtocolAdapterFactory> factorySet) {
        this(factorySet, Set.of());
    }

    /**
     * @param listableFactorySet the factories listed by {@link #all()} (the v2 {@code GET /types} source).
     * @param hiddenFactorySet   the factories resolvable by {@link #findByProtocolId(String)} but excluded from
     *                           {@link #all()} — e.g. the chaos test adapter, which must never appear in the
     *                           frontend (design §10.1).
     * @throws IllegalArgumentException if two factories (across either set) declare the same {@code protocol-id}.
     */
    public ProtocolAdapterFactoryRegistry(
            final @NotNull Set<ProtocolAdapterFactory> listableFactorySet,
            final @NotNull Set<ProtocolAdapterFactory> hiddenFactorySet) {
        final Map<String, ProtocolAdapterFactory> map = new LinkedHashMap<>();
        final List<ProtocolAdapterFactory> listable = new ArrayList<>();
        register(map, listable, listableFactorySet, true);
        register(map, listable, hiddenFactorySet, false);
        this.factoryMap = Map.copyOf(map);
        this.listableFactories = List.copyOf(listable);
    }

    private static void register(
            final @NotNull Map<String, ProtocolAdapterFactory> map,
            final @NotNull List<ProtocolAdapterFactory> listable,
            final @NotNull Set<ProtocolAdapterFactory> factories,
            final boolean isListable) {
        for (final ProtocolAdapterFactory factory : factories) {
            final String protocolId = factory.information().protocolId();
            final ProtocolAdapterFactory duplicatedFactory = map.put(protocolId, factory);
            if (duplicatedFactory != null) {
                throw new IllegalArgumentException(
                        "two protocol adapter factories declare protocol-id [" + protocolId + "]");
            }
            if (isListable) {
                listable.add(factory);
            }
        }
    }

    /**
     * @param protocolId the protocol id to look up.
     * @return the factory for the type, or empty if no factory declares that {@code protocol-id}.
     */
    public @NotNull Optional<ProtocolAdapterFactory> findByProtocolId(final @NotNull String protocolId) {
        return Optional.ofNullable(factoryMap.get(protocolId));
    }

    /**
     * @return the listable factories, in registration order — the source of the v2 {@code GET /types} listing.
     *         Hidden factories (design §10.1) are excluded, so they never appear in the frontend even though they
     *         remain resolvable by {@link #findByProtocolId(String)}. Empty in production wiring (D8), so
     *         {@code GET /types} is empty until a real adapter type is ported.
     */
    public @NotNull Collection<ProtocolAdapterFactory> all() {
        return List.copyOf(listableFactories);
    }
}
