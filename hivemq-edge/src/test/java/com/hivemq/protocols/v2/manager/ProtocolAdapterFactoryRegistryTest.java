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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hivemq.adapter.sdk.api.v2.factories.ProtocolAdapterFactory;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerTestSupport.TestProtocolAdapterFactory;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The factory registry's listable/hidden split (design §10.1): a hidden factory — the chaos test adapter — stays
 * resolvable by {@code protocol-id} so a configured instance is created and run like any other, but is excluded
 * from {@link ProtocolAdapterFactoryRegistry#all()}, the v2 {@code GET /types} source, so it never appears in the
 * frontend's adapter catalog.
 */
class ProtocolAdapterFactoryRegistryTest {

    @Test
    void hiddenFactory_isResolvableByProtocolIdButExcludedFromTheListing() {
        final ProtocolAdapterFactory listable = new TestProtocolAdapterFactory("listable");
        final ProtocolAdapterFactory hidden = new TestProtocolAdapterFactory("chaos");
        final ProtocolAdapterFactoryRegistry registry =
                new ProtocolAdapterFactoryRegistry(Set.of(listable), Set.of(hidden));

        // Resolvable: a configured instance of the hidden type is created exactly like any other.
        assertThat(registry.findByProtocolId("chaos")).contains(hidden);
        assertThat(registry.findByProtocolId("listable")).contains(listable);
        // But excluded from the /types listing — never shown in the frontend.
        assertThat(registry.all()).containsExactly(listable);
    }

    @Test
    void singleArgConstructor_listsEveryFactory() {
        final ProtocolAdapterFactory a = new TestProtocolAdapterFactory("a");
        final ProtocolAdapterFactory b = new TestProtocolAdapterFactory("b");
        final ProtocolAdapterFactoryRegistry registry = new ProtocolAdapterFactoryRegistry(Set.of(a, b));

        assertThat(registry.all()).containsExactlyInAnyOrder(a, b);
        assertThat(registry.findByProtocolId("a")).contains(a);
    }

    @Test
    void unknownProtocolId_resolvesToEmpty() {
        final ProtocolAdapterFactoryRegistry registry = new ProtocolAdapterFactoryRegistry(Set.of());

        assertThat(registry.findByProtocolId("missing")).isEmpty();
        assertThat(registry.all()).isEmpty();
    }

    @Test
    void duplicateProtocolIdAcrossListableAndHidden_isRejected() {
        final ProtocolAdapterFactory listable = new TestProtocolAdapterFactory("dup");
        final ProtocolAdapterFactory hidden = new TestProtocolAdapterFactory("dup");

        assertThatThrownBy(() -> new ProtocolAdapterFactoryRegistry(Set.of(listable), Set.of(hidden)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dup");
    }
}
