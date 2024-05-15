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
package com.hivemq.adapter;

import com.hivemq.adapter.sdk.api.ProtocolAdapterCategory;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.ProtocolAdapterTag;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.List;

/**
 * @author Simon L Johnson
 */
public class TestAdapterInformation implements ProtocolAdapterInformation {

    public TestAdapterInformation() {
    }

    @Override
    public @NotNull String getProtocolName() {
        return "TestProtocol";
    }

    @Override
    public @NotNull String getProtocolId() {
        return "test-adapter-information";
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Test Adapter Information";
    }

    @Override
    public @NotNull String getDescription() {
        return "This is the test protocol information";
    }

    @Override
    public @NotNull String getUrl() {
        return "null";
    }

    @Override
    public @NotNull String getVersion() {
        return "${edge-version}";
    }

    @Override
    public @NotNull String getLogoUrl() {
        return "null";
    }

    @Override
    public @NotNull String getAuthor() {
        return "HiveMQ";
    }

    @Override
    public @Nullable ProtocolAdapterCategory getCategory() {
        return null;
    }

    @Override
    public @Nullable List<ProtocolAdapterTag> getTags() {
        return null;
    }
}
