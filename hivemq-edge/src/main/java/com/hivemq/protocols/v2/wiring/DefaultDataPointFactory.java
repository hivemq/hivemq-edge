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
package com.hivemq.protocols.v2.wiring;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.edge.modules.adapters.data.DataPointImpl;
import org.jetbrains.annotations.NotNull;

/**
 * The single, shared {@link DataPointFactory} the v2 wrapper factory hands to every protocol adapter it builds
 *: the reused v1 {@link DataPointImpl} value, exactly as the legacy framework's adapter input creates it.
 * <p>
 * Unlike the legacy per-adapter factory it carries no adapter id of its own — the framework re-stamps each
 * adapter-produced value with the owning tag's name and adapter id before it reaches a northbound consumer —
 * so the {@code tagName} doubles as the provisional adapter id here. The factory is stateless and therefore safe to
 * share across every v2 adapter instance.
 */
final class DefaultDataPointFactory implements DataPointFactory {

    @Override
    public @NotNull DataPoint create(final @NotNull String tagName, final @NotNull Object tagValue) {
        return new DataPointImpl(tagName, tagValue, tagName, false);
    }

    @Override
    public @NotNull DataPoint createJsonDataPoint(final @NotNull String tagName, final @NotNull Object tagValue) {
        return new DataPointImpl(tagName, tagValue, tagName, true);
    }
}
