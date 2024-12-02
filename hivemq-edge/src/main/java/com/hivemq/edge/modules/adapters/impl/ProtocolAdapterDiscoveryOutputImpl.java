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
package com.hivemq.edge.modules.adapters.impl;

import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryOutput;
import com.hivemq.exceptions.StackLessProtocolAdapterException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.protocols.params.NodeTreeImpl;

import java.util.concurrent.CompletableFuture;

public class ProtocolAdapterDiscoveryOutputImpl implements ProtocolAdapterDiscoveryOutput {

    private final @NotNull NodeTreeImpl nodeTree = new NodeTreeImpl();
    private final @NotNull CompletableFuture<Void> outputFuture = new CompletableFuture<>();
    private @Nullable String errorMessage = null;


    @Override
    public @NotNull NodeTreeImpl getNodeTree() {
        return nodeTree;
    }

    @Override
    public void finish() {
        outputFuture.complete(null);
    }

    @Override
    public void fail(final @NotNull Throwable t, @Nullable final String errorMessage) {
        this.errorMessage = errorMessage;
        outputFuture.completeExceptionally(t);
    }

    @Override
    public void fail(final @NotNull String errorMessage) {
        this.errorMessage = errorMessage;
        outputFuture.completeExceptionally(new StackLessProtocolAdapterException(errorMessage));
    }

    public @NotNull CompletableFuture<Void> getOutputFuture() {
        return outputFuture;
    }

    public @Nullable String getErrorMessage() {
        return errorMessage;
    }
}
