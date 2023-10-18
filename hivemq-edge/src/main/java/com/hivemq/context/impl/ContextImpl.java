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
package com.hivemq.context.impl;

import com.hivemq.context.HiveMQEdgeContext;
import com.hivemq.context.model.Data;
import com.hivemq.context.model.Result;
import com.hivemq.context.provider.TokenProvider;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * @author Simon L Johnson
 */
public class ContextImpl implements HiveMQEdgeContext {

    private final @NotNull Map<String, String> tokenReplacements;
    private final @NotNull Data input;
    private @NotNull Result result;
    private @NotNull ExecutorService executorService;

    public ContextImpl(final @NotNull Data input) {
        this.input = input;
        this.tokenReplacements = Collections.emptyMap();
    }

    public ContextImpl(final @NotNull Data input,
                       final @NotNull Map<String, String> tokenReplacements) {
        this.input = input;
        this.tokenReplacements = tokenReplacements;
    }

    @Override
    public @NotNull Data getInput() {
        return input;
    }

    @Override
    public @Nullable Result getResult() {
        return result;
    }

    @Override
    public void setResult(final @NotNull Result result) {
        this.result = result;
    }

    @Override
    public TokenProvider getTokenProvider() {
        return context -> tokenReplacements;
    }

    @Override
    public ExecutorService getExecutorService() {
        return executorService;
    }

    @Override
    public void setExecutorService(final ExecutorService executorService) {
        this.executorService = executorService;
    }
}
