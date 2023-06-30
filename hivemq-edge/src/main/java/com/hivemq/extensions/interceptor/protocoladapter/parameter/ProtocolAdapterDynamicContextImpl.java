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
package com.hivemq.extensions.interceptor.protocoladapter.parameter;

import com.google.common.collect.ImmutableMap;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.protocoladapter.parameter.ProtocolAdapterDynamicContext;

import java.util.Optional;
import java.util.Set;

public class ProtocolAdapterDynamicContextImpl implements ProtocolAdapterDynamicContext {

    final @NotNull ImmutableMap<String, String> dynamicContext;

    public ProtocolAdapterDynamicContextImpl(final @NotNull ImmutableMap<String, String> dynamicContext) {
        this.dynamicContext = dynamicContext;
    }

    @Override
    public @NotNull Optional<String> getValue(@NotNull final String key) {
        return Optional.ofNullable(dynamicContext.get(key));
    }

    @Override
    public @NotNull Set<String> getKeys() {
        return dynamicContext.keySet();
    }
}
