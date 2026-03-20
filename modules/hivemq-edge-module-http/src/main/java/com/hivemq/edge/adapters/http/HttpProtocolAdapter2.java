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
package com.hivemq.edge.adapters.http;

import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapter2;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import org.jetbrains.annotations.NotNull;

/**
 * {@link ProtocolAdapter2} implementation for HttpProtocolAdapter2.
 */
public class HttpProtocolAdapter2 implements ProtocolAdapter2 {

    private final @NotNull ProtocolAdapter legacyAdapter;
    private final @NotNull ModuleServices moduleServices;

    public HttpProtocolAdapter2(final @NotNull ProtocolAdapter delegate, final @NotNull ModuleServices moduleServices) {
        this.legacyAdapter = delegate;
        this.moduleServices = moduleServices;
    }

    @Override
    public @NotNull ProtocolAdapter getLegacyAdapter() {
        return legacyAdapter;
    }

    @Override
    public @NotNull ModuleServices getModuleServices() {
        return moduleServices;
    }

    @Override
    public boolean supportsSouthbound() {
        return false;
    }
}
