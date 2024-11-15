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
package com.hivemq.protocols;

import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterWritingService;
import com.hivemq.adapter.sdk.api.writing.WritingContext;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.persistence.fieldmapping.FieldMappings;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface InternalProtocolAdapterWritingService extends ProtocolAdapterWritingService {



    @NotNull
    CompletableFuture<Void> startWriting(
            @NotNull WritingProtocolAdapter<WritingContext> writingProtocolAdapter,
            @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService, @NotNull List<FieldMappings> fieldMappings);

    @NotNull
    CompletableFuture<Void> stopWriting(@NotNull WritingProtocolAdapter<WritingContext> writingProtocolAdapter);

    void addWritingChangedCallback(@NotNull WritingChangedCallback callback);

    @FunctionalInterface
    interface WritingChangedCallback {
        void onWritingEnabledChanged();
    }
}
